# bytechef-dev Plugin

Development tools for ByteChef — component scaffolding, patterns, and API reference.

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

## Installation

The plugin is registered in `~/.claude/plugins/installed_plugins.json` under the key `bytechef-dev@local`. To verify it is active, start a new Claude Code session — the skill should appear in the available skills list.
