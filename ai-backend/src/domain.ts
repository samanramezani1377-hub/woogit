import { z } from "zod";

export const ChatMessageSchema = z.object({
  role: z.enum(["system", "user", "assistant", "tool"]),
  content: z.string().min(1),
});

export const ChatRequestSchema = z.object({
  provider: z.string().min(1).default("deepseek"),
  model: z.string().min(1).default("deepseek-v4-flash"),
  messages: z.array(ChatMessageSchema).min(1),
  stream: z.boolean().default(false),
  thinking: z.enum(["enabled", "disabled"]).optional(),
  reasoningEffort: z.enum(["low", "high", "max"]).optional(),
  maxTokens: z.number().int().positive().max(384000).optional(),
});

export type ChatRequest = z.infer<typeof ChatRequestSchema>;

export type ProviderInfo = {
  id: string;
  models: string[];
};

export type ChatResult = {
  id: string;
  provider: string;
  model: string;
  content: string;
  reasoningContent?: string;
  usage?: {
    promptTokens: number;
    completionTokens: number;
    totalTokens: number;
  };
};

export interface AiProvider {
  readonly id: string;
  models(): Promise<ProviderInfo>;
  chat(request: ChatRequest): Promise<ChatResult>;
  stream(request: ChatRequest, onDelta: (text: string) => void): Promise<void>;
}
