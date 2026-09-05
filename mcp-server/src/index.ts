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

type JsonRecord = Record<string, any>;

function normalizeProduct(product: JsonRecord): JsonRecord {
  const variations = Array.isArray(product.variations) ? product.variations : [];
  return {
    id: product.id ?? null,
    name: product.name ?? '',
    slug: product.slug ?? null,
    sku: product.sku ?? null,
    type: product.type ?? null,
    status: product.status ?? null,
    catalog_visibility: product.catalog_visibility ?? null,
    description: product.description ?? '',
    short_description: product.short_description ?? '',
    pricing: {
      price: product.price ?? null,
      regular_price: product.regular_price ?? null,
      sale_price: product.sale_price ?? null,
      on_sale: product.on_sale ?? false,
      tax_status: product.tax_status ?? null,
      tax_class: product.tax_class ?? null
    },
    inventory: {
      manage_stock: product.manage_stock ?? null,
      quantity: product.stock_quantity ?? null,
      status: product.stock_status ?? null,
      backorders: product.backorders ?? null,
      backorders_allowed: product.backorders_allowed ?? null,
      backordered: product.backordered ?? null
    },
    catalog: { categories: product.categories ?? [], tags: product.tags ?? [] },
    shipping: {
      weight: product.weight ?? null,
      dimensions: product.dimensions ?? null,
      shipping_class: product.shipping_class ?? null,
      shipping_required: product.shipping_required ?? null,
      shipping_taxable: product.shipping_taxable ?? null
    },
    images: product.images ?? [],
    attributes: product.attributes ?? [],
    default_attributes: product.default_attributes ?? [],
    related_ids: product.related_ids ?? [],
    upsell_ids: product.upsell_ids ?? [],
    cross_sell_ids: product.cross_sell_ids ?? [],
    purchase_note: product.purchase_note ?? null,
    permalink: product.permalink ?? null,
    variations: variations.map((v: JsonRecord) => ({
      id: v.id ?? null,
      sku: v.sku ?? null,
      description: v.description ?? '',
      attributes: v.attributes ?? [],
      pricing: {
        price: v.price ?? null,
        regular_price: v.regular_price ?? null,
        sale_price: v.sale_price ?? null,
        on_sale: v.on_sale ?? false
      },
      inventory: {
        manage_stock: v.manage_stock ?? null,
        quantity: v.stock_quantity ?? null,
        status: v.stock_status ?? null,
        backorders: v.backorders ?? null,
        backorders_allowed: v.backorders_allowed ?? null,
        backordered: v.backordered ?? null
      },
      image: v.image ?? null,
      weight: v.weight ?? null,
      dimensions: v.dimensions ?? null
    })),
    raw: product
  };
}

function normalizeOrder(order: JsonRecord): JsonRecord {
  return {
    id: order.id ?? null,
    number: order.number ?? null,
    status: order.status ?? null,
    currency: order.currency ?? null,
    date_created: order.date_created ?? null,
    date_modified: order.date_modified ?? null,
    total: order.total ?? null,
    total_tax: order.total_tax ?? null,
    shipping_total: order.shipping_total ?? null,
    discount_total: order.discount_total ?? null,
    payment_method: order.payment_method ?? null,
    payment_method_title: order.payment_method_title ?? null,
    transaction_id: order.transaction_id ?? null,
    customer: { id: order.customer_id ?? null, billing: order.billing ?? null, shipping: order.shipping ?? null },
    items: Array.isArray(order.line_items) ? order.line_items.map((item: JsonRecord) => ({
      id: item.id ?? null,
      product_id: item.product_id ?? null,
      variation_id: item.variation_id ?? null,
      name: item.name ?? '',
      quantity: item.quantity ?? null,
      sku: item.sku ?? null,
      price: item.price ?? null,
      subtotal: item.subtotal ?? null,
      total: item.total ?? null,
      total_tax: item.total_tax ?? null,
      variation: item.variation ?? []
    })) : [],
    shipping_lines: order.shipping_lines ?? [],
    fee_lines: order.fee_lines ?? [],
    coupon_lines: order.coupon_lines ?? [],
    refunds: order.refunds ?? [],
    raw: order
  };
}

function normalizeCategory(category: JsonRecord): JsonRecord {
  return {
    id: category.id ?? null,
    name: category.name ?? '',
    slug: category.slug ?? null,
    parent: category.parent ?? 0,
    description: category.description ?? '',
    display: category.display ?? null,
    image: category.image ?? null,
    menu_order: category.menu_order ?? null,
    count: category.count ?? null,
    raw: category
  };
}

function normalizeCollection(items: unknown, normalizer: (item: JsonRecord) => JsonRecord) {
  return Array.isArray(items) ? items.map(item => normalizer((item ?? {}) as JsonRecord)) : [];
}

function createServer() {
  const server = new McpServer({ name: 'WooGit', version: '1.0.0' });

  server.registerTool('products.list', {
    title: 'List WooCommerce products',
    description: 'Read products from the connected WooCommerce store. Returns structured product identity, pricing, inventory, catalog, shipping, images, attributes and variations. For inventory questions use inventory.quantity and inventory.status. A null quantity means quantity is not managed/available, not necessarily zero. Variable products expose variation-level inventory.',
    inputSchema: { page: z.number().int().min(1).optional(), per_page: z.number().int().min(1).max(100).optional(), search: z.string().optional() },
    annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true }
  }, async ({ page = 1, per_page = 20, search }) => {
    const products = await wc(`/wp-json/wc/v3/products?page=${page}&per_page=${per_page}${search ? `&search=${encodeURIComponent(search)}` : ''}`);
    return { content: [{ type: 'text', text: JSON.stringify({ items: normalizeCollection(products, normalizeProduct), page, per_page }) }] };
  });

  server.registerTool('products.get', {
    title: 'Get WooCommerce product',
    description: 'Read one product by ID with complete structured product data, including inventory and variation inventory.',
    inputSchema: { id: z.number().int().positive() },
    annotations: { readOnlyHint: true, destructiveHint: false }
  }, async ({ id }) => {
    const product = await wc(`/wp-json/wc/v3/products/${id}`);
    return { content: [{ type: 'text', text: JSON.stringify(normalizeProduct(product as JsonRecord)) }] };
  });

  server.registerTool('products.create', {
    title: 'Create WooCommerce product', description: 'Create a new WooCommerce product. This changes store data and should require user confirmation in the client.',
    inputSchema: { name: z.string().min(1), type: z.string().optional(), status: z.string().optional(), regular_price: z.string().optional(), description: z.string().optional(), short_description: z.string().optional(), sku: z.string().optional(), stock_quantity: z.number().int().nonnegative().optional(), categories: z.array(z.object({ id: z.number().int().positive() })).optional(), images: z.array(z.object({ id: z.number().int().positive().optional(), src: z.string().url().optional() })).optional() },
    annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false, openWorldHint: true }
  }, async (input) => {
    const product = await wc('/wp-json/wc/v3/products', { method: 'POST', body: JSON.stringify(input) });
    return { content: [{ type: 'text', text: JSON.stringify(normalizeProduct(product as JsonRecord)) }] };
  });

  server.registerTool('products.update', {
    title: 'Update WooCommerce product', description: 'Update an existing product. This changes store data and should require user confirmation in the client.',
    inputSchema: { id: z.number().int().positive(), fields: z.record(z.string(), z.unknown()) },
    annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: true, openWorldHint: true }
  }, async ({ id, fields }) => {
    const product = await wc(`/wp-json/wc/v3/products/${id}`, { method: 'PUT', body: JSON.stringify(fields) });
    return { content: [{ type: 'text', text: JSON.stringify(normalizeProduct(product as JsonRecord)) }] };
  });

  server.registerTool('products.delete', {
    title: 'Delete WooCommerce product', description: 'Permanently delete a product. This is destructive and should require explicit user confirmation.',
    inputSchema: { id: z.number().int().positive(), force: z.boolean().default(true) },
    annotations: { readOnlyHint: false, destructiveHint: true, idempotentHint: true }
  }, async ({ id, force }) => ({ content: [{ type: 'text', text: JSON.stringify(await wc(`/wp-json/wc/v3/products/${id}?force=${force}`, { method: 'DELETE' })) }] }));

  server.registerTool('categories.list', {
    title: 'List product categories', description: 'Read WooCommerce product categories as structured data including hierarchy, image and product count.',
    inputSchema: { page: z.number().int().min(1).optional(), per_page: z.number().int().min(1).max(100).optional(), search: z.string().optional() }, annotations: { readOnlyHint: true, destructiveHint: false }
  }, async ({ page = 1, per_page = 100, search }) => {
    const categories = await wc(`/wp-json/wc/v3/products/categories?page=${page}&per_page=${per_page}${search ? `&search=${encodeURIComponent(search)}` : ''}`);
    return { content: [{ type: 'text', text: JSON.stringify({ items: normalizeCollection(categories, normalizeCategory), page, per_page }) }] };
  });

  server.registerTool('orders.list', {
    title: 'List WooCommerce orders', description: 'Read WooCommerce orders as structured data including totals, customer, line items, payment, shipping, discounts and refunds.',
    inputSchema: { page: z.number().int().min(1).optional(), per_page: z.number().int().min(1).max(100).optional(), status: z.string().optional(), search: z.string().optional() }, annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true }
  }, async ({ page = 1, per_page = 20, status, search }) => {
    const orders = await wc(`/wp-json/wc/v3/orders?page=${page}&per_page=${per_page}${status ? `&status=${encodeURIComponent(status)}` : ''}${search ? `&search=${encodeURIComponent(search)}` : ''}`);
    return { content: [{ type: 'text', text: JSON.stringify({ items: normalizeCollection(orders, normalizeOrder), page, per_page }) }] };
  });

  server.registerTool('orders.get', {
    title: 'Get WooCommerce order', description: 'Read one WooCommerce order with complete structured order data.', inputSchema: { id: z.number().int().positive() }, annotations: { readOnlyHint: true, destructiveHint: false }
  }, async ({ id }) => {
    const order = await wc(`/wp-json/wc/v3/orders/${id}`);
    return { content: [{ type: 'text', text: JSON.stringify(normalizeOrder(order as JsonRecord)) }] };
  });

  server.registerTool('orders.update', {
    title: 'Update WooCommerce order', description: 'Update an order. This changes store data and should require user confirmation.', inputSchema: { id: z.number().int().positive(), fields: z.record(z.string(), z.unknown()) }, annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: true }
  }, async ({ id, fields }) => {
    const order = await wc(`/wp-json/wc/v3/orders/${id}`, { method: 'PUT', body: JSON.stringify(fields) });
    return { content: [{ type: 'text', text: JSON.stringify(normalizeOrder(order as JsonRecord)) }] };
  });

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
