---
name: ByteChef Management MCP Setup
description: This skill should be used when the user asks to "connect Claude to ByteChef", "set up the ByteChef MCP server", "configure the management MCP server", "let Claude build ByteChef workflows", "add ByteChef to .mcp.json", or wants Claude to operate a running ByteChef instance (projects, workflows, components) over MCP.
transports:
  required: [local]
---

# ByteChef Management MCP Setup

ByteChef ships a **Management MCP server** that gives an MCP client (Claude Code, Claude Desktop, etc.) live tools against a running instance: listing/creating projects and workflows, reading component/task definitions, editing workflow definitions, and more. Connecting it turns Claude into an operator of the ByteChef instance — the recommended way to have Claude *build workflows on* ByteChef.

## What the server exposes

Registered tool families (from `ManagementMcpServerConfiguration`): **ProjectTools, ProjectWorkflowTools, ComponentTools, TaskTools, TaskDispatcherTools, ScriptTools, ClusterElementTools** — plus, on EE deployments, contributed Copilot subagent tools. The workflow-editor tool (`getWorkflow`) carries MCP App UI metadata and returns the workflow definition as structured content, so MCP clients with app support render an interactive workflow view. `createProjectWorkflow` creates an empty workflow; its content comes from `buildWorkflow`. Server capabilities: tools, resources, prompts, logging.

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
- The app loop (the **ByteChef App Builder** skill) needs an admin API key created in DEVELOPMENT and `X-ENVIRONMENT: DEVELOPMENT`.

## Claude Code configuration

`.mcp.json` (project) or user-scope MCP config:

<!-- transport: local -->
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
<!-- /transport -->

- Drop the `headers` block entirely if the server was registered with `authenticationRequired=false`.
- For SSE-only clients, use `"type": "sse"` with the `.../sse` URL.
- Never commit real secret keys or API keys — use environment substitution or user-scope config for the values.

## Verifying the connection

After configuring, list the MCP tools in the client (in Claude Code: `/mcp`). You should see the project/workflow/component tool families. A quick smoke test: ask Claude to "list ByteChef projects" — it should call `listProjects` and return live data. 401/403 responses mean a wrong secret key, a non-admin API key, or an environment mismatch (check `X-ENVIRONMENT`).

## When to use this skill vs. its siblings

- **MCP (this skill):** connects Claude to the instance so it can operate it directly — creating projects, editing visual (JSON) workflows step-by-step, inspecting components.
- **ByteChef Workflow Builder:** builds, imports and publishes workflows over the connection this skill sets up.
- **ByteChef App Builder:** creates an app from the `create-bytechef-app` template and builds, deploys and binds the workflows and API endpoints it calls, over this connection and the `bytechef` CLI.
- **ByteChef Component Builder:** scaffolds a new in-repo platform component when no existing component covers the API.
