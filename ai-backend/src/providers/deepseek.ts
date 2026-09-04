import type { AiProvider, ChatRequest, ChatResult, ProviderInfo } from "../domain.js";

const BASE_URL = "https://api.deepseek.com";

export class DeepSeekProvider implements AiProvider {
  readonly id = "deepseek";
  private readonly apiKey: string;

  constructor(apiKey = process.env.DEEPSEEK_API_KEY ?? "") {
    if (!apiKey) throw new Error("DEEPSEEK_API_KEY is not configured");
    this.apiKey = apiKey;
  }

  async models(): Promise<ProviderInfo> {
    return {
      id: this.id,
      models: ["deepseek-v4-flash", "deepseek-v4-pro", "deepseek-v4-flash-vision-exp"],
    };
  }

  async chat(request: ChatRequest): Promise<ChatResult> {
    const response = await this.request(request, false);
    const data = await response.json() as any;
    const choice = data.choices?.[0];
    if (!choice) throw new Error("DeepSeek returned no choices");
    return {
      id: data.id,
      provider: this.id,
      model: data.model ?? request.model,
      content: choice.message?.content ?? "",
      reasoningContent: choice.message?.reasoning_content ?? undefined,
      usage: data.usage ? {
        promptTokens: data.usage.prompt_tokens,
        completionTokens: data.usage.completion_tokens,
        totalTokens: data.usage.total_tokens,
      } : undefined,
    };
  }

  async stream(request: ChatRequest, onDelta: (text: string) => void): Promise<void> {
    const response = await this.request({ ...request, stream: true }, true);
    if (!response.body) throw new Error("DeepSeek returned an empty stream");
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() ?? "";
        for (const line of lines) {
          const data = line.trim();
          if (!data.startsWith("data:")) continue;
          const payload = data.slice(5).trim();
          if (payload === "[DONE]") return;
          const chunk = JSON.parse(payload);
          const text = chunk.choices?.[0]?.delta?.content;
          if (typeof text === "string" && text) onDelta(text);
        }
      }
    } finally {
      reader.releaseLock();
    }
  }

  private async request(request: ChatRequest, streaming: boolean): Promise<Response> {
    const body: Record<string, unknown> = {
      model: request.model,
      messages: request.messages,
      stream: streaming,
    };
    if (request.thinking) body.thinking = { type: request.thinking };
    if (request.reasoningEffort) body.reasoning_effort = request.reasoningEffort;
    if (request.maxTokens) body.max_tokens = request.maxTokens;

    const response = await fetch(`${BASE_URL}/chat/completions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${this.apiKey}`,
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const detail = await response.text();
      throw new Error(`DeepSeek API ${response.status}: ${detail.slice(0, 500)}`);
    }
    return response;
  }
}
