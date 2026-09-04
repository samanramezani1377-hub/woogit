import type { AiProvider, ChatRequest } from "./domain.js";

export class ProviderRouter {
  constructor(private readonly providers: Map<string, AiProvider>) {}

  provider(id: string): AiProvider {
    const provider = this.providers.get(id);
    if (!provider) throw new Error(`Unsupported AI provider: ${id}`);
    return provider;
  }

  async chat(request: ChatRequest) {
    return this.provider(request.provider).chat(request);
  }

  async stream(request: ChatRequest, onDelta: (text: string) => void) {
    return this.provider(request.provider).stream(request, onDelta);
  }

  async providersInfo() {
    return Promise.all([...this.providers.values()].map((provider) => provider.models()));
  }
}
