---
title: MCP Server
description: Connect AI assistants to ByteChef using the Model Context Protocol
---

# MCP Server

ByteChef exposes a [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server that lets AI assistants — such as Claude, Cursor, and Windsurf — interact directly with your ByteChef instance to manage projects, workflows, and components.

## Get Your MCP Server URL

1. Open ByteChef and go to **Settings** → **MCP Server**
2. Copy the MCP server URL — it has the form:

   ```
   https://<your-host>/api/management/<secret-key>/mcp
   ```

> **Keep this URL private.** It grants full access to your projects and workflows. Only share it with trusted applications. You can regenerate the secret key at any time from the same settings page.

---

## Connect an AI Assistant

### Claude

#### Claude.ai (web)

> Requires a Claude Pro subscription.

1. Go to [claude.ai/settings/integrations](https://claude.ai/settings/integrations)
2. Click **Add More**
3. Paste your MCP server URL and save

#### Claude Desktop

1. Open Claude Desktop
2. Go to **Settings** → **Developer** → **Edit Config** → **Open `claude_desktop_config.json`**
3. Add the following, replacing the URL with yours:

   ```json
   {
     "mcpServers": {
       "ByteChef": {
         "command": "npx",
         "args": [
           "-y",
           "supergateway",
           "--streamableHttp",
           "https://<your-host>/api/management/<secret-key>/mcp"
         ]
       }
     }
   }
   ```

4. Save the file, then quit and restart Claude Desktop

---

### Cursor

1. Open Cursor → **Settings** → **Cursor Settings** → **MCP** → **Add new global MCP server**
2. Paste the following, replacing the URL with yours:

   ```json
   {
     "mcpServers": {
       "ByteChef": {
         "url": "https://<your-host>/api/management/<secret-key>/mcp"
       }
     }
   }
   ```

3. Save

---

### Windsurf

1. Open Windsurf → **Settings** → **Advanced** → **Cascade** → **Add Server** → **Add custom server**
2. Paste the following, replacing the URL with yours:

   ```json
   {
     "mcpServers": {
       "ByteChef": {
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
