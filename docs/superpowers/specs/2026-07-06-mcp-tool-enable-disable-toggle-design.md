# MCP Tool Enable/Disable Toggle — Design

**Date:** 2026-07-06
**Status:** Approved (design)

## Problem

In the MCP Servers management pages (both the **automation** page and the **embedded** EE
page), an MCP server exposes components (e.g. Affinity, Airtable), and each component contains
individual tools. Today a whole MCP server can be toggled on/off (`McpServer.enabled`), but
there is no way to enable/disable an *individual tool*. Users want per-tool control so they can
keep a component installed while turning off specific tools it exposes.

## Goal

Add a per-tool enable/disable toggle, visible on the tool rows of both the automation and
embedded MCP-server-definition pages. A disabled tool is **hidden and not callable** — it
disappears from the MCP `tools/list` response and any `tools/call` for it is rejected, exactly
as if the tool did not exist. All existing and newly-added tools default to **enabled**.

## Non-Goals

- Component-level toggle (turning all of a component's tools on/off at once). Out of scope; the
  toggle is per individual tool only.
- Changing the existing per-customer-instance embedded toggle (`McpIntegrationInstanceTool.enabled`).
  That is a separate, orthogonal concept (see "Two distinct 'enabled' concepts" below) and remains
  as-is. The new definition-level flag applies *in addition* to it.

## Key architectural facts (from codebase investigation)

- Both the automation and embedded MCP-server-definition pages render the **same shared platform
  tables**: `McpServer` → `McpComponent` → `McpTool`, discriminated by `McpServer.type`
  (`AUTOMATION` / `EMBEDDED`). Therefore a single `enabled` flag on the shared `McpTool` entity
  serves both UIs.
- Serve-time tool lists are assembled per request through a `toolFilter` lambda in
  `FilterableMcpAsyncServer` (`toolsListRequestHandler`, `toolsCallRequestHandler`). This is the
  single choke point where a disabled tool must be excluded.
  - **Automation** builds its list in
    `AutomationMcpServerConfiguration.buildToolSpecifications()` via
    `mcpToolService.getMcpComponentMcpTools(mcpComponentId)`.
  - **Embedded** builds its list in
    `EmbeddedMcpToolFacade.getFunctionToolCallback(McpTool, externalUserId, environment, tenantId)`,
    which already returns `null` to exclude a tool.
- The server-level toggle is already implemented end-to-end and is the pattern to mirror:
  `McpServer.enabled` → `mcp_server.enabled` column → `updateMcpServer` mutation →
  `Switch` in `McpServerListItem.tsx` driven by `useMcpServerListItem.ts`.

### Two distinct "enabled" concepts (do not conflate)

1. **Definition-level (new):** `McpTool.enabled` — "is this tool available at all in this MCP
   server definition." Set by the admin/builder on the MCP Servers pages. Shared across automation
   and embedded.
2. **Instance-level (existing, embedded only):** `McpIntegrationInstanceTool.enabled` — "did this
   installed customer instance enable this tool." Per integration instance.

At embedded serve time **both** apply: a tool is served only if it is definition-enabled AND
instance-enabled.

## Chosen approach

**One `enabled` column on the shared `McpTool`.** Rejected alternative: separate flags per context
(automation new field/join table + embedded reusing `McpIntegrationInstanceTool.enabled`) — that
conflates the two concepts above and leaves the embedded *definition* page (which edits `McpTool`,
not integration instances) with no toggle.

## Design

### Domain / schema (platform)

- `McpTool` (`platform-mcp-api`): add `boolean enabled`, initialized to `true` in the constructor;
  add `isEnabled()` / `setEnabled(boolean)`.
- Liquibase: new changelog under
  `platform-mcp-service/.../changelog/platform/mcp/` adding
  `enabled BOOLEAN NOT NULL DEFAULT true` to `mcp_tool`. `DEFAULT true` backfills existing rows so
  all current tools remain active.

### Service (platform)

- `McpToolService` / `McpToolServiceImpl`: add `void updateEnabled(long mcpToolId, boolean enabled)`
  (mirrors the naming of the existing embedded `McpIntegrationInstanceToolServiceImpl.updateEnabled`).
  It fetches the tool, sets `enabled`, and saves. The existing heavyweight `update(...)` is left
  untouched.

### GraphQL (platform, `mcp-tool.graphqls`)

- Add `enabled: Boolean!` to the `McpTool` type.
- Add a dedicated lightweight mutation:
  `updateMcpToolEnabled(id: ID!, enabled: Boolean!): McpTool`, handled by a new
  `@MutationMapping` in `McpToolGraphQlController` delegating to `mcpToolService.updateEnabled(...)`
  and returning the updated tool. (Chosen over extending `updateMcpTool`, whose `McpToolInput`
  would force the client to resend `name`/`parameters`/`version` just to flip a switch.)

### Serve-time filtering

- **Automation** — `AutomationMcpServerConfiguration.buildToolSpecifications()`: skip tools where
  `!mcpTool.isEnabled()`.
- **Embedded** — `EmbeddedMcpToolFacade.getFunctionToolCallback(...)`: add
  `if (!mcpTool.isEnabled()) { return null; }` **before** the existing per-instance
  `isToolEnabled(integrationInstanceId, mcpTool.getId())` check. Net effect: a definition-disabled
  tool is excluded from `tools/list` and cannot be called, on top of the existing per-instance
  logic.

### Client

Both the automation and embedded tool-row components follow the same pattern.

- Automation:
  `client/src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolListItem.tsx`
- Embedded (EE):
  `client/src/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentToolListItem.tsx`

Changes to each:
- Add a `Switch` bound to `tool.enabled`, mirroring `McpServerListItem.tsx`: local
  `isEnablePending` loading state, `onCheckedChange` → mutation → invalidate the component/tool
  query on success.
- New GraphQL operation `updateMcpToolEnabled.graphql`; run codegen.
- Add `enabled` to the tool fields selected by the queries that populate these rows
  (component/tool listing queries), so the switch reflects persisted state.

### EE conventions

The embedded client (`client/src/ee/...`) and server (`server/ee/...`) changes use the ByteChef
Enterprise license header and `@version ee` Javadoc tag on Java classes. Note: the new
`McpTool.enabled` field and the platform serve-time/service/GraphQL changes are **CE** (platform);
only the embedded serve-time filter change (`EmbeddedMcpToolFacade`) and the embedded client tool
row are EE.

## Testing

- **Service:** `McpToolServiceImpl.updateEnabled` sets and persists the flag.
- **Automation filter:** a disabled `McpTool` is excluded from `buildToolSpecifications()`.
- **Embedded filter:** a disabled `McpTool` yields `null` from `getFunctionToolCallback()`, and the
  existing per-instance behavior is unaffected when the tool is definition-enabled.
- **Client:** toggle test on each `McpComponentToolListItem` (automation + embedded) mirroring the
  existing `McpServerListItem` toggle test — flipping the switch calls the mutation with the right
  `id`/`enabled` and invalidates the query.

## Rollout / migration

- Additive column with `DEFAULT true`; no data migration beyond the Liquibase default. Existing
  behavior is preserved (every tool starts enabled).
- No breaking GraphQL changes (new field is non-null with a backfilled default; new mutation is
  additive).
