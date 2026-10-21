# bytechef-dev Plugin

Development tools for ByteChef — one plugin, four capabilities:

| Skill | What it does |
|---|---|
| **ByteChef Component Builder** | Scaffold in-repo platform components (Java, `server/libs/modules/components/`) |
| **ByteChef Management MCP Setup** | Connect Claude to a running instance's Management MCP server so it can build workflows live |
| **ByteChef Workflow Builder** | Create/build/import/publish workflows **live**, over the Management MCP server, against a connected instance |
| **ByteChef App Builder** | Create an app from the `create-bytechef-app` template's directory, write its key to `.env.local`, bind connections, and build, deploy and bind its workflows (DEVELOPMENT only) |

The MCP Setup, Workflow Builder and App Builder skills use a configured instance: the Management MCP server's URL and an admin API key, which the MCP Setup skill asks for. The App Builder also uses the `bytechef` CLI with an automation API key.

## Skills

### ByteChef Component Builder

Guides creation of new ByteChef components (integrations) with actions, triggers, connections, and tests.

#### What It Covers

- **10-step workflow**: scaffold directory, register in build system, constants, connection, actions, triggers, component handler, icon, tests, build and verify
- **All component types**: standard (with connection), OpenAPI-based (CLI scaffolded), helper (no connection/triggers), AI-tool-enabled (with cluster elements)
- **Property quick reference**: all DSL property types (`string`, `integer`, `bool`, `date`, `array`, `object`, `fileEntry`, etc.) with their Java types and UI controls
- **Dynamic options pattern**: API-driven dropdowns with `optionsLookupDependsOn` for dependent fields
- **Full API reference** (in `references/component-api-reference.md`): all DSL factory methods, property modifiers, context methods (HTTP, file, JSON, XML, logging, data storage, encoding), authorization types, trigger types, Parameters getters, output definitions
- **Real-world patterns** (in `references/component-patterns.md`): Bearer/API Key/Basic Auth/OAuth2 connections, HTTP GET/POST actions, file uploads, conditional display, static/dynamic/polling triggers, snapshot and unit tests with mocking, integration tests, OpenAPI component extension

#### How to Use

Start a new Claude Code session and ask:

- "Create a ByteChef component for the Notion API"
- "Build a new component with OAuth2 connection"
- "Scaffold a component for Stripe"
- "Add an action to the slack component"
- "Create a polling trigger for new records"
- "Create a connection definition with API key auth"

The skill activates automatically when it detects component-building intent.

### ByteChef Management MCP Setup

Configures `.mcp.json` for the instance's Management MCP server (`{PUBLIC_URL}/api/management/{SECRET_KEY}/mcp`, streamable HTTP; SSE variant available; optional `Authorization: Bearer <admin key>` + `X-ENVIRONMENT`). Gives Claude live project/workflow/component tools against the connected ByteChef.

Ask: "Connect Claude to my ByteChef instance" / "Set up the ByteChef MCP server".

### ByteChef Workflow Builder

Operates an already-connected instance over the Management MCP server: `listProjects`/`createProject`
then `createProjectWorkflow` to get a `workflowId`, `buildWorkflow` (plain-language instruction,
independent per call, may ask a clarifying question) or `importWorkflow` (n8n/Make/Zapier/Workato) to
fill it in, `listComponents`/`searchActions`/`getActionDefinition`/`getProperties` and
`listTriggers`/`searchTriggers`/`getTriggerDefinition` to look up what a step or trigger can do,
`getWorkflow` to inspect a definition, and `publishProject` to go live. Requires the MCP
Setup skill above to already be connected; exposing the result as a tool for *other* MCP clients
(`createMcpServer`/`createMcpProject`/`configureMcpServer`/`updateMcpServer`) is a separate job, not
covered by this skill.

Ask: "Build a workflow in ByteChef that..." / "Create a project and workflow for X" / "Import this
n8n workflow into ByteChef".

### ByteChef App Builder

The coding-agent loop for an app made from the `create-bytechef-app` template, DEVELOPMENT only. It prefers API
Platform endpoints for the app's logic, then Call-workflow bindings (long-running, approval and background runs), then
direct actions or tools, then the proxy, and builds scheduled workflows that write to a data table when the app needs
periodic work. The CLI manages the app (`bytechef automation app create|link|list|status|bind connection|bind
workflow|bind api-collection|unbind connection|unbind workflow|unbind api-collection|key rotate`,
`bytechef automation connection list`), writing the app key straight into `.env.local` so it never passes through the
model. The management MCP server builds workflows (`buildWorkflow` with `kind` `API_ENDPOINT`, `SCHEDULED` or
`APP_CALLABLE`), publishes them (`publishProject`), creates API collections and endpoints (`createAppApiCollection`,
`createAppApiEndpoint`, `updateAppApiCollectionVersion`, `listAppApiCollections`), data tables
(`createAppDataTable`) and deploys (`deployProject`), all EE. Needs an automation API key and an admin API key, both
created in DEVELOPMENT.

Ask: "Create a ByteChef app for this project and bind my HubSpot connection" / "Give my app an endpoint that lists
open orders" / "Refresh this report every hour" / "Give my app a workflow that onboards a customer".

## Installation

The plugin is registered in `~/.claude/plugins/installed_plugins.json` under the key `bytechef-dev@local`. To verify it is active, start a new Claude Code session — the skill should appear in the available skills list.
