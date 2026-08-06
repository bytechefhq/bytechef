# bytechef-dev Plugin

Development tools for ByteChef — one plugin, four capabilities:

| Skill | What it does |
|---|---|
| **ByteChef Component Builder** | Scaffold in-repo platform components (Java, `server/libs/modules/components/`) |
| **ByteChef Custom Component Builder** | Author single-file (JS/Python/Ruby) or JAR (Java) custom components and **upload** them to a running instance |
| **ByteChef Code Workflow Builder** | Author whole projects (automation) and integrations (embedded) as code and **deploy** them |
| **ByteChef Management MCP Setup** | Connect Claude to a running instance's Management MCP server so it can build workflows live |

The upload/deploy/MCP skills use a configured instance: set `BYTECHEF_BASE_URL` and `BYTECHEF_API_KEY` (admin API key) in the environment, or the skill will ask.

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

### ByteChef Custom Component Builder

Single-file JS/Python/Ruby components (bare object-literal / `SimpleNamespace` / `Struct` contracts, `name` + Integer `version` + `actions[{name,title,properties,output,tool,perform}]`, plus an optional `connection` — every authorization type including OAuth2 — `icon`, dynamic property `options`, and `POLLING` `triggers`) and Java JARs (SDK `component-api` + `META-INF/services/com.bytechef.component.ComponentHandler`), uploaded via `POST /api/platform/v1/custom-components/deploy` (multipart `componentFile`; extension picks the language; admin-gated; `.jar` requires `java-enabled` on the server). Uploads publish; the UI editor saves a draft.

Ask: "Create a custom ByteChef component in Python and upload it" / "Deploy this .js component to my ByteChef".

### ByteChef Code Workflow Builder

Whole projects/integrations as one artifact. Automation projects key on `name` (locked after first deploy) with `workflows[{name,label,tasks[{name,label,connections,perform}]}]`; embedded integrations key on `componentName`. A task's `perform(context)` invokes other components (`context.component.<name>.<action>`), reads the workflow's inputs and prior task outputs (`context.input()`), reads a wired connection (`context.connection(name)`) and logs; `parallel` / `forkJoin` task groups run work concurrently. Deploys: `POST /api/automation/v1/projects/deploy` (`projectFile` + optional `workspaceId`) and `POST /api/embedded/internal/integrations/deploy` (`integrationFile`). Java via `ProjectHandler`/`IntegrationHandler` SDK JARs.

Ask: "Write a code workflow that pings an API daily and deploy it" / "Create an embedded integration as code".

### ByteChef Management MCP Setup

Configures `.mcp.json` for the instance's Management MCP server (`{PUBLIC_URL}/api/management/{SECRET_KEY}/mcp`, streamable HTTP; SSE variant available; optional `Authorization: Bearer <admin key>` + `X-ENVIRONMENT`). Gives Claude live project/workflow/component tools against the connected ByteChef.

Ask: "Connect Claude to my ByteChef instance" / "Set up the ByteChef MCP server".

## Installation

The plugin is registered in `~/.claude/plugins/installed_plugins.json` under the key `bytechef-dev@local`. To verify it is active, start a new Claude Code session — the skill should appear in the available skills list.
