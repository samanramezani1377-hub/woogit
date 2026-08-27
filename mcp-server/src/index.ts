import express from 'express';
import { randomUUID } from 'node:crypto';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import { isInitializeRequest } from '@modelcontextprotocol/sdk/types.js';
import { z } from 'zod';

const PORT = Number(process.env.PORT ?? 3978);
const MCP_API_TOKEN = process.env.MCP_API_TOKEN;
const WC_BASE_URL = process.env.WC_BASE_URL?.replace(/\/$/, '');
const WC_CONSUMER_KEY = process.env.WC_CONSUMER_KEY;
const WC_CONSUMER_SECRET = process.env.WC_CONSUMER_SECRET;

if (!MCP_API_TOKEN || !WC_BASE_URL || !WC_CONSUMER_KEY || !WC_CONSUMER_SECRET) {
  throw new Error('MCP_API_TOKEN, WC_BASE_URL, WC_CONSUMER_KEY and WC_CONSUMER_SECRET are required.');
}

const transports = new Map<string, StreamableHTTPServerTransport>();

function auth(req: express.Request, res: express.Response, next: express.NextFunction) {
  const value = req.header('authorization');
  if (value !== `Bearer ${MCP_API_TOKEN}`) return res.status(401).json({ error: 'Unauthorized' });
  next();
}

async function wc(path: string, init: RequestInit = {}) {
  const url = new URL(`${WC_BASE_URL}${path}`);
  url.searchParams.set('consumer_key', WC_CONSUMER_KEY!);
  url.searchParams.set('consumer_secret', WC_CONSUMER_SECRET!);
  const response = await fetch(url, {
    ...init,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json', ...(init.headers ?? {}) }
  });
  const text = await response.text();
  let body: unknown;
  try { body = JSON.parse(text); } catch { body = { message: text }; }
  if (!response.ok) throw new Error(`WooCommerce HTTP ${response.status}: ${JSON.stringify(body)}`);
  return body;
}

function createServer() {
  const server = new McpServer({ name: 'WooGit', version: '1.0.0' });

  server.registerTool('products.list', {
    title: 'List WooCommerce products',
    description: 'Read products from the connected WooCommerce store.',
    inputSchema: { page: z.number().int().min(1).optional(), per_page: z.number().int().min(1).max(100).optional(), search: z.string().optional() },
    annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true }
  }, async ({ page = 1, per_page = 20, search }) => ({ content: [{ type: 'text', text: JSON.stringify(await wc(`/wp-json/wc/v3/products?page=${page}&per_page=${per_page}${search ? `&search=${encodeURIComponent(search)}` : ''}`)) }] }));

  server.registerTool('products.get', {
    title: 'Get WooCommerce product', description: 'Read one product by ID.',
    inputSchema: { id: z.number().int().positive() }, annotations: { readOnlyHint: true, destructiveHint: false }
  }, async ({ id }) => ({ content: [{ type: 'text', text: JSON.stringify(await wc(`/wp-json/wc/v3/products/${id}`)) }] }));

  server.registerTool('products.create', {
    title: 'Create WooCommerce product', description: 'Create a new WooCommerce product. This changes store data and should require user confirmation in the client.',
    inputSchema: { name: z.string().min(1), type: z.string().optional(), status: z.string().optional(), regular_price: z.string().optional(), description: z.string().optional(), short_description: z.string().optional(), sku: z.string().optional(), stock_quantity: z.number().int().nonnegative().optional(), categories: z.array(z.object({ id: z.number().int().positive() })).optional(), images: z.array(z.object({ id: z.number().int().positive().optional(), src: z.string().url().optional() })).optional() },
    annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false, openWorldHint: true }
  }, async (input) => ({ content: [{ type: 'text', text: JSON.stringify(await wc('/wp-json/wc/v3/products', { method: 'POST', body: JSON.stringify(input) })) }] }));

  server.registerTool('products.update', {
    title: 'Update WooCommerce product', description: 'Update an existing product. This changes store data and should require user confirmation in the client.',
    inputSchema: { id: z.number().int().positive(), fields: z.record(z.string(), z.unknown()) },
    annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: true, openWorldHint: true }
  }, async ({ id, fields }) => ({ content: [{ type: 'text', text: JSON.stringify(await wc(`/wp-json/wc/v3/products/${id}`, { method: 'PUT', body: JSON.stringify(fields) })) }] }));

  server.registerTool('products.delete', {
    title: 'Delete WooCommerce product', description: 'Permanently delete a product. This is destructive and should require explicit user confirmation.',
    inputSchema: { id: z.number().int().positive(), force: z.boolean().default(true) },
    annotations: { readOnlyHint: false, destructiveHint: true, idempotentHint: true, openWorldHint: true }
  }, async ({ id, force }) => ({ content: [{ type: 'text', text: JSON.stringify(await wc(`/wp-json/wc/v3/products/${id}?force=${force}`, { method: 'DELETE' })) }] }));

  server.registerTool('categories.list', {
    title: 'List product categories', description: 'Read WooCommerce product categories.',
    inputSchema: { page: z.number().int().min(1).optional(), per_page: z.number().int().min(1).max(100).optional(), search: z.string().optional() }, annotations: { readOnlyHint: true, destructiveHint: false }
  }, async ({ page = 1, per_page = 100, search }) => ({ content: [{ type: 'text', text: JSON.stringify(await wc(`/wp-json/wc/v3/products/categories?page=${page}&per_page=${per_page}${search ? `&search=${encodeURIComponent(search)}` : ''}`)) }] }));

  server.registerTool('orders.list', {
    title: 'List WooCommerce orders', description: 'Read WooCommerce orders.',
    inputSchema: { page: z.number().int().min(1).optional(), per_page: z.number().int().min(1).max(100).optional(), status: z.string().optional(), search: z.string().optional() }, annotations: { readOnlyHint: true, destructiveHint: false }
  }, async ({ page = 1, per_page = 20, status, search }) => ({ content: [{ type: 'text', text: JSON.stringify(await wc(`/wp-json/wc/v3/orders?page=${page}&per_page=${per_page}${status ? `&status=${encodeURIComponent(status)}` : ''}${search ? `&search=${encodeURIComponent(search)}` : ''}`)) }] }));

  server.registerTool('orders.get', {
    title: 'Get WooCommerce order', description: 'Read one order by ID.', inputSchema: { id: z.number().int().positive() }, annotations: { readOnlyHint: true, destructiveHint: false }
  }, async ({ id }) => ({ content: [{ type: 'text', text: JSON.stringify(await wc(`/wp-json/wc/v3/orders/${id}`)) }] }));

  server.registerTool('orders.update', {
    title: 'Update WooCommerce order', description: 'Update an order. This changes store data and should require user confirmation.', inputSchema: { id: z.number().int().positive(), fields: z.record(z.string(), z.unknown()) }, annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: true }
  }, async ({ id, fields }) => ({ content: [{ type: 'text', text: JSON.stringify(await wc(`/wp-json/wc/v3/orders/${id}`, { method: 'PUT', body: JSON.stringify(fields) })) }] }));

  return server;
}

const app = express();
app.use(express.json({ limit: '1mb' }));
app.get('/health', (_req, res) => res.json({ ok: true, service: 'woogit-mcp' }));
app.all('/mcp', auth, async (req, res) => {
  try {
    const sessionId = req.header('mcp-session-id');
    let transport = sessionId ? transports.get(sessionId) : undefined;
    if (!transport && !sessionId && isInitializeRequest(req.body)) {
      transport = new StreamableHTTPServerTransport({ sessionIdGenerator: () => randomUUID(), onsessioninitialized: id => transports.set(id, transport!) });
      transport.onclose = () => { if (transport?.sessionId) transports.delete(transport.sessionId); };
      await createServer().connect(transport);
    }
    if (!transport) return res.status(400).json({ error: 'Invalid or missing MCP session' });
    await transport.handleRequest(req, res, req.body);
  } catch (error) {
    if (!res.headersSent) res.status(500).json({ error: error instanceof Error ? error.message : 'Internal MCP error' });
  }
});

app.listen(PORT, () => console.log(`WooGit MCP listening on :${PORT}`));
