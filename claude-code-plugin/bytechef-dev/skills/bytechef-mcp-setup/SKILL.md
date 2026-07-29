---
name: ByteChef Management MCP Setup
description: This skill should be used when the user asks to "connect Claude to ByteChef", "set up the ByteChef MCP server", "configure the management MCP server", "let Claude build ByteChef workflows", "add ByteChef to .mcp.json", or wants Claude to operate a running ByteChef instance (projects, workflows, components) over MCP.
---

# ByteChef Management MCP Setup

ByteChef ships a **Management MCP server** that gives an MCP client (Claude Code, Claude Desktop, etc.) live tools against a running instance: listing/creating projects and workflows, reading component/task definitions, editing workflow definitions, and more. Connecting it turns Claude into an operator of the ByteChef instance — the recommended way to have Claude *build workflows on* ByteChef (as opposed to authoring code artifacts to upload).

## What the server exposes

Registered tool families (from `ManagementMcpServerConfiguration`): **ProjectTools, ProjectWorkflowTools, ComponentTools, TaskTools, TaskDispatcherTools, ScriptTools, ClusterElementTools** — plus, on EE deployments, contributed Copilot subagent tools. Workflow-editor tools (`getWorkflow`, `createProjectWorkflow`, `updateWorkflow`) carry MCP App UI metadata and return the workflow definition as structured content, so MCP clients with app support render an interactive workflow view. Server capabilities: tools, resources, prompts, logging.

## Server-side prerequisites

- Property `bytechef.ai.mcp.server.enabled` — **on by default**. The HTTP+SSE sub-transport is separately toggled by `bytechef.ai.mcp.server.sse.enabled` (also default on).
- The management MCP server is registered per instance with a **secret key** and an `authenticationRequired` flag (managed from the ByteChef UI / GraphQL; defaults to authentication required). Get the secret key from a ByteChef admin — the UI surfaces the ready-made URL.

## URL shapes and auth

Base pattern (streamable HTTP — preferred):

```
{PUBLIC_URL}/api/management/{SECRET_KEY}/mcp
```

HTTP+SSE variant (for clients that need it): stream `.../sse`, client→server messages `.../message`.

Auth model:
- The `{SECRET_KEY}` path segment is always required — it identifies and (when `authenticationRequired=false`) alone authorizes the connection.
- When `authenticationRequired=true` (the default), ALSO send `Authorization: Bearer <admin API key>` — the key must be an admin platform API key matching the environment.
- Optional `X-ENVIRONMENT` header selects the environment (defaults to `PRODUCTION`).

## Claude Code configuration

`.mcp.json` (project) or user-scope MCP config:

```json
{
  "mcpServers": {
    "bytechef": {
      "type": "http",
      "url": "https://bytechef.example.com/api/management/<SECRET_KEY>/mcp",
      "headers": {
        "Authorization": "Bearer <ADMIN_API_KEY>",
        "X-ENVIRONMENT": "PRODUCTION"
      }
    }
  }
}
```

- Drop the `headers` block entirely if the server was registered with `authenticationRequired=false`.
- For SSE-only clients, use `"type": "sse"` with the `.../sse` URL.
- Never commit real secret keys or API keys — use environment substitution or user-scope config for the values.

## Verifying the connection

After configuring, list the MCP tools in the client (in Claude Code: `/mcp`). You should see the project/workflow/component tool families. A quick smoke test: ask Claude to "list ByteChef projects" — it should call `listProjects` and return live data. 401/403 responses mean a wrong secret key, a non-admin API key, or an environment mismatch (check `X-ENVIRONMENT`).

## When to use MCP vs. code artifacts

- **MCP (this skill):** Claude operates the instance directly — creating projects, editing visual (JSON) workflows step-by-step, inspecting components. Best for interactive building.
- **Code workflows / custom components (sibling skills):** Claude authors an artifact locally and uploads/deploys it. Best for versioned, reviewable, code-first delivery.
Both can be combined: build interactively over MCP, then export/pin as code.
