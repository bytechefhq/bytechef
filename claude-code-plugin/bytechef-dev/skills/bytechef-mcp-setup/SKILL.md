---
name: bytechef-mcp-setup
description: Use when the user asks to "connect Claude to ByteChef", "set up the ByteChef MCP server", "add ByteChef to Codex/.mcp.json", "add ByteChef as a connector", or otherwise wants Claude, Claude Code, Codex or another coding agent to operate a running ByteChef instance over its management MCP server.
transports:
  required: [local]
  optional: [mcp]
---

# ByteChef MCP Setup

ByteChef ships a management MCP server. Once a client is connected, the agent can create projects,
look up components, write and save workflow definitions, bind connections and publish, all
deterministically and without an AI provider configured on the ByteChef server. This skill covers
connecting each client; the `bytechef-workflow-builder` skill covers what to do once connected.

## Minimum server version

The workflow authoring tools arrived in the first ByteChef release after 0.36.0. Older servers expose
the management MCP server but cannot save workflow definitions or look up components over it. If the
connected tool list has no workflow update or component lookup tools, upgrade the server. A 404 on
`/api/management/mcp` also means the server predates this release.

## The server URL

The management MCP server speaks streamable HTTP at:

```
https://<host>/api/management/mcp
```

It signs in with OAuth: the client opens a browser, the user logs in to ByteChef, and every tool call
then runs as that user, limited to the workspaces the user belongs to.

For a client that cannot do OAuth, send an API key instead: an admin creates one in ByteChef under
Settings, Admin API Keys, and the client sends it on every request as
`Authorization: Bearer <API key>`. Tool calls then run as the user who owns the key. The header works
on `https://<host>/api/management/mcp` and on the secret-key URL a ByteChef admin may hand out,
`https://<host>/api/management/<secretKey>/mcp`; the secret-key URL alone, without the header, is
rejected. Treat both the key and the secret-key URL like passwords: keep them out of committed files
and use the client's user-scope configuration or environment substitution.

## Claude Code

<!-- transport: local -->
Add an HTTP entry to the project's `.mcp.json` (or the user-scope MCP configuration):

```json
{
  "mcpServers": {
    "bytechef": {
      "type": "http",
      "url": "https://<host>/api/management/mcp"
    }
  }
}
```

The same entry can be added from the shell with
`claude mcp add --transport http bytechef https://<host>/api/management/mcp`. Then run `/mcp`, pick
`bytechef` and complete the OAuth sign-in in the browser.

Without OAuth, add the API key header to the same entry instead of signing in:

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

`${BYTECHEF_API_KEY}` is expanded from the environment, so the key stays out of the file.
<!-- /transport -->

## Claude (claude.ai and the desktop app)

<!-- transport: local -->
Open Settings, then Connectors, and add a custom connector with the URL
`https://<host>/api/management/mcp`. Connect it and sign in to ByteChef when the OAuth window opens.
Enable the connector in the conversation where the workflow will be built.
<!-- /transport -->

## Codex and other coding agents

<!-- transport: local -->
Add a remote (streamable HTTP) MCP server entry pointing at `https://<host>/api/management/mcp`.
For Codex, in `~/.codex/config.toml`:

```toml
[mcp_servers.bytechef]
url = "https://<host>/api/management/mcp"
```

Then run `codex mcp login bytechef` to complete the OAuth sign-in. Without OAuth, set
`bearer_token_env_var = "BYTECHEF_API_KEY"` in the same table and export that variable with an Admin
API key; Codex sends it as `Authorization: Bearer <API key>`. Other agents take the same URL in
their own remote MCP configuration; if the agent has no OAuth support, configure it to send the
`Authorization: Bearer <API key>` header described above.
<!-- /transport -->

## Verifying the connection and finding the workspace

<!-- transport: mcp uses: listWorkspaces -->
Call `listWorkspaces` as a smoke test. It returns the workspaces the signed-in user belongs to, each
with its id. Tools that work within a workspace take that id as `workspaceId`; if the user has more
than one workspace, ask which one to use rather than guessing.
<!-- /transport -->

A 401 response means the OAuth sign-in did not complete, the API key is missing or invalid, or the
secret key in a secret-key URL is wrong. Access to a workspace the user does not belong to is
reported by the tool itself, not as an HTTP error.

## Choosing an environment

ByteChef keeps connections, deployments and test configuration per environment: `DEVELOPMENT`,
`STAGING` or `PRODUCTION`. Tools that care take an optional `environment` argument, which defaults to
`DEVELOPMENT`. Build and test in `DEVELOPMENT` unless the user asks otherwise, and use the same
environment for every call in one build, so the connections listed and bound are the ones the
workflow is saved and tested against.

## Creating connections

Connections hold credentials (OAuth tokens, API keys), so they are created by the user in the
ByteChef UI, never over MCP. When a workflow needs a connection that does not exist yet, ask the user
to open ByteChef, go to Connections, create one for the component in the chosen environment, and tell
you when it is done. The agent can then list it and bind it to the workflow.
