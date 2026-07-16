---
title: MCP Server
description: Connect AI assistants to ByteChef using the Model Context Protocol
---

# MCP Server

> **Coming soon.** The MCP server is on the upcoming release track and is not yet available in the latest released version of ByteChef.

ByteChef exposes a [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server that lets AI assistants — such as Claude, Cursor, and Windsurf — interact directly with your ByteChef instance to manage projects, workflows, and components.

## Get Your MCP Server URL

1. Open ByteChef and go to **Settings** → **MCP Server**
2. Copy the MCP server URL — it has the form:

   ```
   https://<your-host>/api/management/<secret-key>/mcp
   ```

> **Keep this URL private.** The secret key identifies your MCP server. You can regenerate it at any time from the same settings page.

The URL alone is not enough to connect — every request must also carry a valid credential (see [Authentication](#authentication) below).

---

## Authentication

The URL secret identifies the server. When the server has **Require authentication** enabled, a separate credential also authenticates the caller, and requests without a valid one are rejected with `401`. Whether a credential is required is a setting on the server — see [Require authentication](#require-authentication) below.

### Require authentication

The management MCP server has a **Require authentication** toggle on its settings page:

- **On** — every request must carry a valid admin API key or OAuth2 token; token-less requests get `401`. This is the default for a newly configured server.
- **Off** — the URL secret alone is enough; requests are served without a credential, and any credential that *is* sent is ignored. A server configured before this setting existed defaults to **off**, so it keeps working unchanged.

Keep the URL secret private in both modes — it is the server's only protection when authentication is off.

### API key (default)

Create an admin API key under **Settings** → **Admin API Keys** and send it with every request as a Bearer header:

```
Authorization: Bearer <your-api-key>
```

The connection snippets below include this header. The API key is bound to an environment; requests target the environment given in the `X-ENVIRONMENT` header (`PRODUCTION` when omitted), and the key's environment must match. Because the key is bound to your user account, the AI assistant acts with your permissions — and you can revoke a single client by deleting its key without touching the server URL or other clients.

### OAuth2 (optional)

For MCP clients that support OAuth2 — including those that cannot send custom headers, such as claude.ai web integrations — ByteChef can run its own OAuth2 authorization server:

```
BYTECHEF_OAUTH2_AUTHORIZATION_SERVER_ENABLED=true
```

Once enabled, OAuth2-capable clients need no manual credential setup: on the first unauthenticated request the server advertises its authorization metadata, the client registers itself automatically (Dynamic Client Registration), and you complete a standard authorization-code + PKCE flow using your existing ByteChef login, followed by a consent screen. The client then presents the issued token as a Bearer JWT.

API keys keep working alongside OAuth2 — enabling one does not disable the other.

### External identity provider (Enterprise Edition)

EE deployments can additionally trust JWTs issued by their own identity provider (Okta, Microsoft Entra, Keycloak, …) instead of, or alongside, the embedded authorization server. Trusted issuers and their claim-to-tenant/authority mappings are configured via the `BYTECHEF_OAUTH2_RESOURCE_SERVER_ISSUERS_*` properties — see [Environment Variables](/self-hosting/configuration/environment-variables#oauth2-configuration).

---

## Connect an AI Assistant

### Claude

#### Claude.ai (web)

> Requires a Claude Pro subscription. claude.ai integrations cannot send custom headers, so [enable OAuth2](#oauth2-optional) on your instance first.

1. Go to [claude.ai/settings/integrations](https://claude.ai/settings/integrations)
2. Click **Add More**
3. Paste your MCP server URL and save

#### Claude Desktop

1. Open Claude Desktop
2. Go to **Settings** → **Developer** → **Edit Config** → **Open `claude_desktop_config.json`**
3. Add the following, replacing the URL with yours and `YOUR_API_KEY` with an admin API key:

   ```json
   {
     "mcpServers": {
       "ByteChef": {
         "command": "npx",
         "args": [
           "-y",
           "mcp-remote",
           "https://<your-host>/api/management/<secret-key>/mcp",
           "--header",
           "Authorization: Bearer YOUR_API_KEY"
         ]
       }
     }
   }
   ```

4. Save the file, then quit and restart Claude Desktop

---

### Cursor

1. Open Cursor → **Settings** → **Cursor Settings** → **MCP** → **Add new global MCP server**
2. Paste the following, replacing the URL with yours and `YOUR_API_KEY` with an admin API key:

   ```json
   {
     "mcpServers": {
       "ByteChef": {
         "headers": {
           "Authorization": "Bearer YOUR_API_KEY"
         },
         "url": "https://<your-host>/api/management/<secret-key>/mcp"
       }
     }
   }
   ```

3. Save

---

### Windsurf

1. Open Windsurf → **Settings** → **Advanced** → **Cascade** → **Add Server** → **Add custom server**
2. Paste the following, replacing the URL with yours and `YOUR_API_KEY` with an admin API key:

   ```json
   {
     "mcpServers": {
       "ByteChef": {
         "headers": {
           "Authorization": "Bearer YOUR_API_KEY"
         },
         "url": "https://<your-host>/api/management/<secret-key>/mcp"
       }
     }
   }
   ```

3. Save

---

### Other Clients

Any MCP-compatible client that supports Streamable HTTP transport can connect using the raw server URL:

```
https://<your-host>/api/management/<secret-key>/mcp
```

Send an admin API key with every request as an `Authorization: Bearer` header, or [enable OAuth2](#oauth2-optional) for clients that authenticate via the standard MCP OAuth2 discovery flow.

---

## Available Tools

The MCP server exposes the following tools to connected AI assistants.

### Projects

| Tool | Description |
|---|---|
| `createProject` | Create a new project |
| `updateProject` | Update an existing project |
| `deleteProject` | Delete a project |
| `getProject` | Get project details |
| `listProjects` | List all projects |
| `searchProjects` | Search projects by keyword |
| `publishProject` | Publish a project |
| `getProjectStatus` | Get the current status of a project |

### Workflows

| Tool | Description |
|---|---|
| `createProjectWorkflow` | Create a new workflow in a project |
| `updateWorkflow` | Update an existing workflow |
| `deleteWorkflow` | Delete a workflow |
| `getWorkflow` | Get workflow details |
| `listWorkflows` | List all workflows |
| `searchWorkflows` | Search workflows by keyword |
| `validateWorkflow` | Validate a workflow definition |
| `getWorkflowBuildInstructions` | Get instructions for building a workflow |

### Components & Actions

| Tool | Description |
|---|---|
| `listComponents` | List all available components |
| `getComponent` | Get component details |
| `searchComponents` | Search components by keyword |
| `listActions` | List actions for a component |
| `getAction` | Get action details |
| `searchActions` | Search actions by keyword |
| `getActionDefinition` | Get the full definition of an action |
| `getOutputProperty` | Get output property details |

### Triggers

| Tool | Description |
|---|---|
| `listTriggers` | List triggers for a component |
| `getTrigger` | Get trigger details |
| `searchTriggers` | Search triggers by keyword |
| `getTriggerDefinition` | Get the full definition of a trigger |

### Tasks

| Tool | Description |
|---|---|
| `listTasks` | List available task dispatchers |
| `getTask` | Get task dispatcher details |
| `searchTasks` | Search tasks by keyword |
| `validateTask` | Validate a task configuration |
| `getTaskDefinition` | Get the full definition of a task |
| `getTaskDispatcherBuildInstructions` | Get build instructions for a task dispatcher |
| `getTaskProperties` | Get properties for a task |
| `getTaskOutputProperty` | Get the output property of a task |
| `getProperties` | Get general properties |
