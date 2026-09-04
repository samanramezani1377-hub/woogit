# WooGit AI Backend

Provider-neutral AI gateway for WooGit. The first real provider is DeepSeek; additional providers should implement the same `AiProvider` contract without changing the HTTP API.

## Run

Requires Node.js 20+.

```bash
cd ai-backend
npm install
DEEPSEEK_API_KEY=your_key npm run dev
```

For production, use `npm run build` and `npm start`.

## API

- `GET /health`
- `GET /v1/providers`
- `POST /v1/chat`

Example request:

```json
{
  "provider": "deepseek",
  "model": "deepseek-v4-flash",
  "messages": [
    { "role": "user", "content": "سلام" }
  ],
  "stream": false,
  "thinking": "disabled"
}
```

Set `stream` to `true` for Server-Sent Events. Provider API keys stay on the backend and are never sent to the Android app.

If `WOOGIT_AI_API_KEY` is configured, client requests must include `Authorization: Bearer <key>`.
