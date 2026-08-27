# WooGit MCP Server

Remote MCP bridge for exposing WooCommerce operations as ChatGPT tool calls.

## Current capabilities

- Streamable HTTP MCP endpoint: `/mcp`
- Bearer protection with `MCP_API_TOKEN`
- Read tools: `products.list`, `products.get`, `categories.list`, `orders.list`, `orders.get`
- Write tools: `products.create`, `products.update`, `products.delete`, `orders.update`
- Destructive/read-only annotations so MCP clients can apply their confirmation UX
- WooCommerce REST API integration with credentials kept server-side

## Run

```bash
npm install
npm run build
MCP_API_TOKEN='...' WC_BASE_URL='https://store.example' WC_CONSUMER_KEY='ck_...' WC_CONSUMER_SECRET='cs_...' npm start
```

The MCP endpoint is `https://YOUR_PUBLIC_HOST/mcp`.

## Security

Do not put WooCommerce consumer secrets in ChatGPT, prompts, the Android APK, or source control. Keep them as server-side environment/secret-manager values. Put the MCP endpoint behind HTTPS.

The current bridge uses a server bearer token for development/staging. A production ChatGPT deployment should put the MCP endpoint behind a real OAuth 2.1 authorization server and per-user credential mapping; do not treat the development bearer token as the final multi-user authentication model.

## ChatGPT connection

ChatGPT must be able to reach the MCP endpoint over the public internet. Configure the endpoint as a remote MCP connector/app according to the OpenAI MCP integration available to the account. The MCP server exposes tools; the ChatGPT client remains responsible for presenting/handling user confirmation for tools marked as write/destructive.

## Important architecture

```text
ChatGPT
   |
   | MCP / HTTPS
   v
WooGit MCP Server
   |
   | server-side credentials
   v
WooCommerce REST API
```

WooGit Android is not required to be running for the MCP server to execute a WooCommerce operation. The Android app and MCP server can share the same WooCommerce backend without exposing the Android app's local secrets to ChatGPT.
