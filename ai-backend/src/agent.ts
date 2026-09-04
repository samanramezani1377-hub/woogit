import { createHash } from "node:crypto";
import type { ChatMessage, ChatRequest, ChatResult } from "./domain.js";
import type { ProviderRouter } from "./router.js";
import type { WooCommerceToolRegistry } from "./tools.js";

export type AgentRunResult =
  | { status: "completed"; result: ChatResult; steps: number }
  | { status: "confirmation_required"; confirmationToken: string; toolCallId: string; toolName: string; arguments: unknown; steps: number };

export class WooGitAgent {
  constructor(private readonly router: ProviderRouter, private readonly tools: WooCommerceToolRegistry) {}

  async run(request: ChatRequest, confirmations: string[] = []): Promise<AgentRunResult> {
    let messages: ChatMessage[] = [...request.messages];
    const approved = new Set(confirmations);
    const toolDefinitions = this.tools.list().map((tool) => ({
      type: "function",
      function: { name: tool.name, description: tool.description, parameters: { type: "object", additionalProperties: true } },
    }));

    for (let step = 1; step <= 8; step++) {
      const result = await this.router.chat({ ...request, messages, stream: false, tools: toolDefinitions });
      if (!result.toolCalls?.length) return { status: "completed", result, steps: step };

      messages.push({ role: "assistant", content: result.content || "tool call", tool_calls: result.toolCalls.map((call) => ({ id: call.id, type: "function", function: { name: call.name, arguments: call.arguments } })) });

      for (const call of result.toolCalls) {
        const tool = this.tools.get(call.name);
        if (!tool) throw new Error(`Unknown agent tool: ${call.name}`);
        let args: unknown;
        try { args = JSON.parse(call.arguments); } catch { throw new Error(`Invalid arguments for ${call.name}`); }
        const confirmationToken = tokenFor(call.name, args);
        if (!tool.readOnly && !approved.has(confirmationToken)) {
          return { status: "confirmation_required", confirmationToken, toolCallId: call.id, toolName: call.name, arguments: args, steps: step };
        }
        const output = await tool.execute(args);
        messages.push({ role: "tool", tool_call_id: call.id, content: JSON.stringify(output) });
      }
    }
    throw new Error("Agent exceeded the maximum tool-call steps");
  }
}

function tokenFor(toolName: string, args: unknown): string {
  return createHash("sha256").update(`${toolName}:${JSON.stringify(args)}`).digest("hex").slice(0, 32);
}
