import { z } from "zod";

export const AgentToolSchema = z.object({
  name: z.string(),
  description: z.string(),
  inputSchema: z.record(z.string(), z.unknown()),
  readOnly: z.boolean(),
  destructive: z.boolean(),
});

export type AgentTool = z.infer<typeof AgentToolSchema> & {
  execute(input: unknown): Promise<unknown>;
};

const IdSchema = z.object({ id: z.number().int().positive() });
const ListSchema = z.object({ page: z.number().int().min(1).default(1), per_page: z.number().int().min(1).max(100).default(20), search: z.string().optional() });

export class WooCommerceToolRegistry {
  private readonly tools: AgentTool[];

  constructor(private readonly wc: WooCommerceClient) {
    this.tools = [
      {
        name: "products.list",
        description: "List products from the connected WooCommerce store.",
        inputSchema: { page: "integer >= 1", per_page: "integer 1..100", search: "optional string" },
        readOnly: true,
        destructive: false,
        execute: async (input) => this.wc.get("/products", ListSchema.parse(input)),
      },
      {
        name: "products.get",
        description: "Get one WooCommerce product by ID.",
        inputSchema: { id: "positive integer" },
        readOnly: true,
        destructive: false,
        execute: async (input) => this.wc.get(`/products/${IdSchema.parse(input).id}`),
      },
      {
        name: "products.update",
        description: "Update a WooCommerce product. Requires explicit user confirmation before execution.",
        inputSchema: { id: "positive integer", fields: "object" },
        readOnly: false,
        destructive: false,
        execute: async (input) => {
          const parsed = z.object({ id: z.number().int().positive(), fields: z.record(z.string(), z.unknown()) }).parse(input);
          return this.wc.request(`/products/${parsed.id}`, "PUT", parsed.fields);
        },
      },
      {
        name: "products.delete",
        description: "Permanently delete a WooCommerce product. Requires explicit user confirmation before execution.",
        inputSchema: { id: "positive integer", force: "boolean" },
        readOnly: false,
        destructive: true,
        execute: async (input) => {
          const parsed = z.object({ id: z.number().int().positive(), force: z.boolean().default(true) }).parse(input);
          return this.wc.request(`/products/${parsed.id}?force=${parsed.force}`, "DELETE");
        },
      },
      {
        name: "categories.list",
        description: "List WooCommerce product categories.",
        inputSchema: { page: "integer >= 1", per_page: "integer 1..100", search: "optional string" },
        readOnly: true,
        destructive: false,
        execute: async (input) => this.wc.get("/products/categories", ListSchema.parse(input)),
      },
      {
        name: "orders.list",
        description: "List WooCommerce orders.",
        inputSchema: { page: "integer >= 1", per_page: "integer 1..100", status: "optional string", search: "optional string" },
        readOnly: true,
        destructive: false,
        execute: async (input) => {
          const parsed = ListSchema.extend({ status: z.string().optional() }).parse(input);
          return this.wc.get("/orders", parsed);
        },
      },
      {
        name: "orders.get",
        description: "Get one WooCommerce order by ID.",
        inputSchema: { id: "positive integer" },
        readOnly: true,
        destructive: false,
        execute: async (input) => this.wc.get(`/orders/${IdSchema.parse(input).id}`),
      },
      {
        name: "orders.update",
        description: "Update a WooCommerce order. Requires explicit user confirmation before execution.",
        inputSchema: { id: "positive integer", fields: "object" },
        readOnly: false,
        destructive: false,
        execute: async (input) => {
          const parsed = z.object({ id: z.number().int().positive(), fields: z.record(z.string(), z.unknown()) }).parse(input);
          return this.wc.request(`/orders/${parsed.id}`, "PUT", parsed.fields);
        },
      },
    ];
  }

  list() {
    return this.tools.map(({ execute: _execute, ...tool }) => tool);
  }

  get(name: string) {
    return this.tools.find((tool) => tool.name === name);
  }
}

export class WooCommerceClient {
  private readonly baseUrl: string;

  constructor(
    baseUrl = process.env.WC_BASE_URL ?? "",
    private readonly consumerKey = process.env.WC_CONSUMER_KEY ?? "",
    private readonly consumerSecret = process.env.WC_CONSUMER_SECRET ?? "",
  ) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
  }

  async get(path: string, query?: Record<string, unknown>) {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(query ?? {})) {
      if (value !== undefined && value !== "") params.set(key, String(value));
    }
    return this.request(`${path}${params.size ? `?${params}` : ""}`, "GET");
  }

  async request(path: string, method: string, body?: unknown) {
    if (!this.baseUrl || !this.consumerKey || !this.consumerSecret) {
      throw new Error("WooCommerce connection is not configured");
    }
    const url = new URL(`${this.baseUrl}/wp-json/wc/v3${path}`);
    url.searchParams.set("consumer_key", this.consumerKey);
    url.searchParams.set("consumer_secret", this.consumerSecret);
    const response = await fetch(url, {
      method,
      headers: { Accept: "application/json", "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const text = await response.text();
    let parsed: unknown;
    try { parsed = JSON.parse(text); } catch { parsed = { message: text }; }
    if (!response.ok) throw new Error(`WooCommerce HTTP ${response.status}`);
    return parsed;
  }
}
