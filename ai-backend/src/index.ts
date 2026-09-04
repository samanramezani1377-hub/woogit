import express from "express";
import { ChatRequestSchema } from "./domain.js";
import { ProviderRouter } from "./router.js";
import { DeepSeekProvider } from "./providers/deepseek.js";
import { WooGitAgent } from "./agent.js";
import { WooCommerceClient, WooCommerceToolRegistry } from "./tools.js";

const app = express();
app.use(express.json({ limit: "1mb" }));

const configuredApiKey = process.env.WOOGIT_AI_API_KEY;
app.use((req, res, next) => {
  if (!configuredApiKey || req.path === "/health") return next();
  if (req.header("Authorization") !== `Bearer ${configuredApiKey}`) {
    return res.status(401).json({ error: { code: "unauthorized", message: "Invalid backend API key" } });
  }
  next();
});

const providerRouter = new ProviderRouter(new Map([["deepseek", new DeepSeekProvider()]]));
const toolRegistry = new WooCommerceToolRegistry(new WooCommerceClient());
const agent = new WooGitAgent(providerRouter, toolRegistry);

app.get("/health", (_req, res) => res.json({ status: "ok" }));

app.get("/v1/providers", async (_req, res) => {
  try {
    res.json({ providers: await providerRouter.providersInfo() });
  } catch (error) {
    res.status(503).json({ error: { code: "provider_unavailable", message: errorMessage(error) } });
  }
});

app.get("/v1/agent/tools", (_req, res) => res.json({ tools: toolRegistry.list() }));

app.post("/v1/agent", async (req, res) => {
  const parsed = ChatRequestSchema.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: { code: "invalid_request", message: parsed.error.message } });
  try {
    const confirmedToolCallIds = Array.isArray(req.body?.confirmedToolCallIds) ? req.body.confirmedToolCallIds.filter((id: unknown): id is string => typeof id === "string") : [];
    return res.json(await agent.run(parsed.data, confirmedToolCallIds));
  } catch (error) {
    return res.status(502).json({ error: { code: "agent_error", message: errorMessage(error) } });
  }
});

app.post("/v1/chat", async (req, res) => {
  const parsed = ChatRequestSchema.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: { code: "invalid_request", message: parsed.error.message } });
  try {
    if (parsed.data.stream) {
      res.status(200);
      res.setHeader("Content-Type", "text/event-stream; charset=utf-8");
      res.setHeader("Cache-Control", "no-cache, no-transform");
      res.setHeader("Connection", "keep-alive");
      await providerRouter.stream(parsed.data, (text) => res.write(`data: ${JSON.stringify({ type: "delta", text })}\n\n`));
      res.write("data: [DONE]\n\n");
      return res.end();
    }
    return res.json(await providerRouter.chat(parsed.data));
  } catch (error) {
    if (res.headersSent) {
      res.write(`data: ${JSON.stringify({ type: "error", message: errorMessage(error) })}\n\n`);
      return res.end();
    }
    return res.status(502).json({ error: { code: "provider_error", message: errorMessage(error) } });
  }
});

const port = Number(process.env.PORT ?? 8787);
app.listen(port, "0.0.0.0", () => console.log(`WooGit AI backend listening on :${port}`));

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Unknown error";
}
