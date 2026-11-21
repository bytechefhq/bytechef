# ByteChef for Codex and other coding agents

1. Copy the skill folders from `skills/` into the agent's skills directory (for Codex,
   `~/.codex/skills/`), or point the agent at them.
2. Add the ByteChef management MCP server to the agent's MCP configuration.

## Codex

Merge `codex-config.toml.example` into `~/.codex/config.toml`, then run `codex mcp login bytechef`
for OAuth. To use an Admin API key instead, set `bearer_token_env_var` to the name of an environment
variable that holds it.

## Any other agent with remote HTTP MCP support

Use the streamable HTTP URL `https://<host>/api/management/mcp`.

OAuth-capable clients sign in through the browser. For a client without OAuth, send an Admin API key
on every request:

```
Authorization: Bearer <API key>
```

Generic JSON shape, which most clients accept with small variations in the key names:

```json
{
  "mcpServers": {
    "bytechef": {
      "type": "http",
      "url": "https://<host>/api/management/mcp",
      "headers": {
        "Authorization": "Bearer ${BYTECHEF_API_KEY}"
      }
    }
  }
}
```

Keep the API key out of committed files.
