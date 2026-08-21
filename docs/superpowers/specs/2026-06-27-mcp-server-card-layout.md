# MCP Server components & workflows — card layout (mirror AI Hub Connectors)

Date: 2026-06-27
Ticket: client-side reorganization (no new functionality)

## Background

The AI Hub Connectors page (`client/src/pages/automation/ai-hub/context/AiHubConnectors.tsx`,
`ConnectorRow`) renders each connector as a bordered, rounded **collapsible card**. The card header is
`flex items-center gap-2.5 px-3 py-2.5` (chevron, icon `size-6`, title + description, right-aligned
controls, dropdown menu). When expanded, tools render as **rows** inside a
`flex flex-col gap-1 border-t border-border px-3 py-2 pl-10` section; each tool row is
`flex items-center gap-2 py-0.5` with the title/description on the left (flex-1) and controls on the
right (in Connectors: a toggle + a Configure (Bolt) button).

The MCP Servers page renders servers as a `divide-y` list of collapsible rows
(`mcp-server-list/`); inside a server's "Tools" tab, components and workflow groups are plain
hover-rows (`px-2 py-1`), and their leaf **tools/workflows are flex-wrapped badges**
(`flex flex-wrap gap-2`) — each badge a small pill with inline Configure (Bolt) and Delete (X) icon
buttons (workflows also have Edit). There is no per-tool toggle, which is correct for MCP.

Automation and embedded MCP have **parallel, near-identical copies** of the component-list components
under `pages/automation/mcp-servers/` and `ee/pages/embedded/mcp-servers/` (not a single shared
module), and **separate** workflow components (automation `McpProjectWorkflowList`; embedded
`McpIntegrationInstanceConfigurationWorkflowList`). Both surfaces must be changed.

## Goal

Reorganize the MCP **components** and **workflows** blocks to mirror the AI Hub Connectors card layout:
each component and each workflow group becomes a bordered collapsible card, and its leaf
tools/workflows become full-width **rows** (not badges). **No new functionality** — the same actions
(Configure popover, Edit, Delete, version display, add/remove) are preserved; only the visual
structure changes. **MCP tools must NOT gain a toggle** (unlike AI Hub connector tools).

## Decisions

- **Match the className patterns** from AI Hub `ConnectorRow` — do NOT reuse the `ConnectorRow`
  component itself (it carries connector-only toggle/Connect/enable logic). MCP keeps its own data,
  queries, and actions; only the markup/classes change.
- **Servers stay as the current `divide-y` list** (not converted to cards), so only components and
  workflow groups become cards. This avoids border-in-border nesting since components live inside a
  server's collapsible Tools tab — one level deeper than AI Hub's top-level connectors.
- Apply to **both automation and embedded** (parallel edits to the separate copies).

## Design

### Component card (replaces the current component hover-row + tool badges)

`McpComponentListItem` + `McpComponentToolList` + `McpComponentToolListItem` are restructured so a
component renders as one bordered collapsible card:

- **Card container:** `Collapsible` with `rounded-md border border-border` (matching `ConnectorRow`).
- **Header row:** `flex items-center gap-2.5 px-3 py-2.5` — a `CollapsibleTrigger` chevron, the
  component icon (`size-6`), the title + version badge (flex-1, truncated), and on the right the
  existing dropdown menu (Add Component / Edit / Delete actions — unchanged).
- **Expanded tools section:** `flex flex-col gap-1 border-t border-border px-3 py-2 pl-10`.
- **Tool row** (`McpComponentToolListItem`): `flex items-center gap-2 py-0.5` — tool title +
  description on the left (flex-1, truncated), and on the right the existing **Configure** (Bolt →
  `McpComponentToolPropertiesPopover`) and **Delete** (X) icon buttons. **No toggle.**

### Workflow card (replaces the current workflow/project hover-row + workflow badges)

Same treatment for workflow groups:

- Automation: `McpProjectListItem` + `McpProjectWorkflowList` + `McpProjectWorkflowListItem`.
- Embedded: the integration-instance-configuration equivalents
  (`McpIntegrationInstanceConfigurationWorkflowList` + `…ListItem`) plus the embedded project/group
  header item.
- Group header → bordered collapsible card header (chevron, icon, title + version badge, existing
  Edit Workflows / Change Version / Delete menu).
- **Workflow row:** `flex items-center gap-2 py-0.5` — workflow label on the left (flex-1, truncated),
  and on the right the existing **Configure** (Bolt) + **Edit** (Pencil) + **Delete** (X) icon
  buttons. **No toggle.**

### Containers / spacing

`McpComponentList` and `McpProjectList` (and embedded equivalents) currently wrap items with
`py-1 pl-4` and nest tools in `pl-6`. Adjust to stack the new cards with a small vertical gap
(`flex flex-col gap-1.5`, mirroring AI Hub's connector list) inside the server's Tools tab, removing
the now-redundant `pl-6` badge wrapper. Keep the existing tab/section padding so the cards sit within
the server's expanded area.

## Out of scope

- Any behavioural change: no new actions, no toggle on tools, no data/query changes.
- The server-row (`mcp-server-list/`) layout beyond what's needed to host the new cards.
- The "Connect" tab and connection rendering.
- The `Select Tools` dialog and the fromAi/property work (separate feature).

## Testing

- This is presentational. Verify via `npx tsc --noEmit` + `npx eslint` on changed files, and manual
  visual QA on both the automation and embedded MCP Servers pages: components and workflow groups
  render as bordered collapsible cards; tools/workflows render as rows (not badges) with no toggle;
  Configure / Edit / Delete actions still work; expand/collapse works.
- If any changed component has an existing test (`*.test.tsx`), update it to the new structure and
  keep it green.

## Commit plan

Small, reviewable commits (client convention `<ticket> client - <desc>`; ticket to be supplied by the
user — default to a placeholder until then):
1. Automation component cards (component card + tool rows).
2. Automation workflow cards (project card + workflow rows).
3. Embedded component cards.
4. Embedded workflow cards (integration-instance-configuration).

Each commit verified with tsc + eslint on its changed files (global `npm run check` is currently red
on pre-existing unrelated `AiHubConnectors.tsx` lint errors).
