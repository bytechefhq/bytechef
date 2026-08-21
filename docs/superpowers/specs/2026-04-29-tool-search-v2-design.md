# Tool Search Tool — V2 (Full Implementation) Design Spec

**Status**: Draft (depends on V1)
**Date**: 2026-04-29
**Branch**: `0_732`
**Audience**: AI Hub / Copilot / Agentic-AI module owners + client-side eng for the composer plus-button work.

## Goal

Build on the V1 walking skeleton (ASK agent + dynamic tool discovery via `spring-ai-tool-search-tool`) to ship the full user-facing experience:

1. **BUILD agent integration** with safety hooks for write-mutating tools.
2. **Per-conversation tool persistence** — pre-configured tools with bound connections that survive across turns.
3. **Composer plus-button menu** — point-and-click discovery of components and their tools.
4. **Tool detail panel** — reuses the existing MCP tool-config UI to set parameters and connection.
5. **Conversation attached-tools list** — tools appear as conversation artifacts alongside attached files.

V2 inherits all of V1's architecture; nothing in V1 gets refactored. V2 is purely additive.

## Why this is a separate phase

V1 proves dynamic discovery + invocation works end-to-end. Until that's green, building UI on top is premature — the discovery layer is the foundational primitive everything else attaches to. Splitting also lets V1 ship as a low-stakes ASK-only change while V2 takes the time it needs to land write-mutation safety properly.

## Architecture overview

V1 is the **stateless discovery layer**. V2 adds two layers on top:

1. **Persistence layer** — `ai_hub_task_component` + `ai_hub_task_tool` tables. User-attached tools with pre-set parameters and bound connections; survive across turns.
2. **UI layer** — composer plus-button menu, tool detail panel, conversation attached-tools chips.

The LLM's tool surface in V2 = `searchTools` (from V1) + per-conversation pre-configured tools (synthesized as `ToolCallback`s at request time). Discovered-via-search tools remain stateless one-shots; explicitly-attached tools persist.

## V2.1 — BUILD agent integration

### What's different from ASK

The BUILD agent already issues write-mutating tool calls today (e.g., `addDataTableRow`, `createWorkflow`). Wiring `searchTools` into BUILD means the LLM can now discover and invoke arbitrary write-mutating component tools too — `slack/sendMessage`, `github/createIssue`, `jira/transitionIssue`, etc.

This is intended behavior, but two safety considerations:

1. **Confirm-before-destructive**: tools whose cluster element metadata flags them as destructive (e.g. delete, drop, overwrite) should route through a stage-and-confirm path, similar to how P3.2's `RollbackProjectDeploymentToolCallback` returns a confirmation envelope before mutating. Use a new `destructive` flag on the tool metadata returned by the search advisor; the invocation handler stages destructive calls instead of running them, returning a confirmation envelope the chat surfaces as an "Apply / Cancel" pair.
2. **Audit trail**: every BUILD-agent-discovered tool invocation must record to `ai_hub_tool_usage` with the discovered-via-search marker so we can audit which tools were dynamically discovered vs explicitly attached.

### Wiring

Same as V1: add the library's Advisor to `aiHubBuildSpringAIAgent` in `CopilotConfiguration`. The handler from V1 is reused; V2 adds a thin `DestructiveToolGuard` wrapper around it.

### Acceptance

- BUILD agent can discover and invoke tools via `searchTools`.
- Destructive tools stage a confirmation envelope (verified by test).
- Audit row recorded with `discoveredViaSearch=true` flag.

## V2.2 — Per-conversation tool persistence

### Schema

Two new tables, mirroring the existing `McpComponent` + `McpTool` shape but scoped to a conversation:

```sql
CREATE TABLE ai_hub_task_component (
    id                  BIGSERIAL PRIMARY KEY,
    conversation_id     BIGINT       NOT NULL,
    component_name      VARCHAR(64)  NOT NULL,
    component_version   INTEGER      NOT NULL,
    connection_id       BIGINT       NULL,        -- nullable: tools that don't need a connection
    environment         INTEGER      NOT NULL,
    created_by          VARCHAR(50),
    created_date        TIMESTAMP    NOT NULL,
    last_modified_by    VARCHAR(50),
    last_modified_date  TIMESTAMP    NOT NULL,
    version             INTEGER      NOT NULL,
    CONSTRAINT fk_ccc_conversation FOREIGN KEY (conversation_id)
        REFERENCES ai_hub_task(id) ON DELETE CASCADE,
    CONSTRAINT fk_ccc_connection   FOREIGN KEY (connection_id)
        REFERENCES connection(id) ON DELETE SET NULL,
    UNIQUE (conversation_id, component_name, component_version, connection_id, environment)
);

CREATE TABLE ai_hub_task_tool (
    id                            BIGSERIAL PRIMARY KEY,
    conversation_component_id     BIGINT       NOT NULL,
    name                          VARCHAR(64)  NOT NULL,   -- cluster element name
    parameters                    JSONB        NOT NULL DEFAULT '{}',
    created_by                    VARCHAR(50),
    created_date                  TIMESTAMP    NOT NULL,
    last_modified_by              VARCHAR(50),
    last_modified_date            TIMESTAMP    NOT NULL,
    version                       INTEGER      NOT NULL,
    CONSTRAINT fk_cct_component FOREIGN KEY (conversation_component_id)
        REFERENCES ai_hub_task_component(id) ON DELETE CASCADE,
    UNIQUE (conversation_component_id, name)
);
```

Both tables live in `ai/copilot_execution/` (FK into `connection` requires the execution-tier dependency, same constraint that landed during the integration-test fix).

### Domain + service

Mirror the EE MCP service layout exactly:

| New artifact | Mirrors |
|---|---|
| `AiHubTaskComponent` (domain record) | `McpComponent` |
| `AiHubTaskTool` (domain record) | `McpTool` |
| `AiHubTaskComponentRepository` | `McpComponentRepository` |
| `AiHubTaskToolRepository` | `McpToolRepository` |
| `AiHubTaskComponentService` + impl | `McpComponentService` |
| `AiHubTaskToolService` + impl | `McpToolService` |
| `AiHubTaskToolFacade` | combines: `attachComponent` / `addTool` / `updateToolParameters` / `removeTool` / `listConversationTools` |
| `AiHubTaskToolGraphQlController` | `McpToolGraphQlController` |

### Dynamic tool-callback registration

Per-request, the agent factory resolves the conversation's pre-configured tools and synthesizes a `ConversationToolCallback` for each. These callbacks live alongside the `searchTools` callback in the agent's tool list. The LLM sees them as ordinary tools; their `inputSchema` is the cluster element's input schema with the pre-configured parameters merged in (they become defaults; the LLM can override).

Implementation: extend `aiHubBuildSpringAIAgent` (and ASK) with a `ToolCallbackResolver` lambda that:

1. Reads `WorkspaceInvocationContext.threadId` from the chat request.
2. Finds the conversation's id via `ConversationService.findByThreadId`.
3. Calls `AiHubTaskToolFacade.listConversationTools(conversationId)`.
4. For each row, builds a `ConversationToolCallback` whose `call(...)` delegates to the same `ClusterElementToolInvocationHandler` from V1 (params are merged: callback's pre-config + LLM-supplied overrides).

### Conflict with stateless discovery

If the conversation has `slack/sendMessage` attached AND the LLM separately discovers `slack/sendMessage` via `searchTools`, both routes are available. Disambiguate by tool name: the attached one is named `<componentName>_<clusterElementName>`; the search-discovered one is named `searchedTool_<id>` (or whatever the library uses).

The LLM picks based on description — the attached one's description should explicitly say "preconfigured: connection X, channel Y" so the LLM prefers it for repeat use.

## V2.3 — Chat affordances

Three new tool callbacks, mirroring P3.2's pattern:

| Callback | Wraps |
|---|---|
| `AttachComponentToConversationToolCallback` | `AiHubTaskToolFacade.attachComponent` — chat says "add the Slack component to this conversation" |
| `AddConversationToolToolCallback` | `AiHubTaskToolFacade.addTool` — chat says "and add the sendMessage tool with channel #engineering" |
| `RemoveConversationToolToolCallback` | `AiHubTaskToolFacade.removeTool` — chat says "stop using the Slack tool here" |

`RequestConnectionToolCallback` already exists (built earlier in P3.2 work) — reused for the connection-needed step. When `AttachComponentToConversationToolCallback` discovers a missing connection, it returns the same `connectionRequired` envelope the existing flow knows how to render.

## V2.4 — Composer plus-button menu

### UI surface

Extend the composer's plus-button menu (currently in `AiHubChatComposer.tsx`) with a new section: **Tools**.

Structure:

```
+ button → menu opens
  ├── Files (existing)
  ├── Knowledge Base (existing)
  ├── Workflows (existing)
  └── Tools (NEW)
        ├── component A (e.g. Slack)
        │   ├── tool 1 (sendMessage)
        │   ├── tool 2 (createChannel)
        │   └── ...
        ├── component B (GitHub)
        │   └── ...
        └── search box (filters component list)
```

Click on a component → expand its tool list. Click on a tool → opens the **detail panel**.

### Detail panel

Reuses the existing MCP tool-config UI components — they already render an action's input-property form:

- `McpComponentToolPropertiesPopover.tsx` — property-form rendering
- `McpComponentToolListItem.tsx` — list row
- `McpComponentDialogToolSelectionStep.tsx` — selection flow

These get factored out of their MCP-specific GraphQL bindings into a shared shape, then re-used here with conversation-tool GraphQL mutations behind them. The existing MCP usage continues to work.

### Connection step

If the component needs a connection and the workspace has none of that type, the detail panel shows a "Connect" button that opens `ConnectionDialog`. After successful connection, the panel re-renders with the new connection bound.

If the workspace has multiple connections of that type, show a connection picker.

### Save flow

User fills out parameters → clicks "Attach" → GraphQL mutation creates the `ai_hub_task_component` (idempotent if exists) + `ai_hub_task_tool`. The chip appears in the conversation's attached-tools list immediately (subscription-based update or optimistic insert + refetch).

## V2.5 — Conversation attached-tools list

Conversations already render attached files (from `ai_hub_task_asset_file`) as chips. Add a sibling section for attached tools:

```
Attached:
  📎 quarterly-report.pdf
  📎 sales-deck.pptx
  🔧 slack/sendMessage (connected to Acme Workspace)
  🔧 github/createIssue (connected to acme-org)
```

Click a tool chip → open the detail panel pre-filled (edit parameters / change connection / remove).

## Schema location

All V2 Liquibase migrations land in `ai/copilot_execution/`:

```
20260501000001_ai_copilot_ai_hub_conversation_component_init.xml
20260501000002_ai_copilot_ai_hub_conversation_tool_init.xml
```

Sequenced 2026-05-01 to land cleanly after V1's tool-search migration.

## Open questions

1. **Tool name collision**: two conversations attaching `slack/sendMessage` get distinct rows. The LLM-visible name needs disambiguation. Proposal: `slack_sendMessage` if unique within conversation; else `slack_sendMessage_2`, etc. Reject the second attach with a UI error and let the user rename.
2. **Connection lifecycle on delete**: FK `ON DELETE SET NULL` means a deleted connection leaves the tool attached but unusable. Surface "Reconnect" chip; offer to remove the tool instead.
3. **Conversation forking**: when a user duplicates a conversation, do attached tools clone or stay shared? Default: clone (each conversation owns its tool-config rows; deletion cascades correctly).
4. **MCP integration overlap**: should `ai_hub_task_component` be unified with `mcp_component` via a shared `parent_kind` discriminator? **Decision: NO** — the lifecycles, auth models, and audit requirements are different. The schemas mirror because the *shape* of "configured component-action" is universal, not because they should share storage.
5. **Destructive tool confirmation UX**: how does the user accept/reject a staged destructive call? V2 design: Apply / Cancel buttons rendered inline in the chat turn, mirroring the staged-changes pattern from `AddDataTableRowToolCallback`.

## Acceptance criteria

V2 is **done** when all of the following pass:

1. BUILD agent can discover and invoke tools via `searchTools`.
2. Destructive tools (per their metadata flag) are staged-and-confirmed, not run directly.
3. `ai_hub_task_component` + `_tool` tables exist; full service + facade + GraphQL surface; integration tests cover CRUD + cascade-on-delete.
4. Three new chat callbacks (Attach / Add / Remove conversation tool) are unit-tested and registered.
5. Plus-button menu surfaces components-with-tools; clicking a tool opens the detail panel; saving creates the persistence rows.
6. Conversation attached-tools list renders chips; clicking opens the detail panel pre-filled.
7. Connection-bound tool invocation goes through pre-set connection without prompting; connection-missing tool prompts via the existing `RequestConnection` flow.
8. `./gradlew :ai-copilot-service:check` is fully green.
9. Smoke test: chat-driven attachment ("add Slack to this conversation, channel #eng") + plus-button-driven attachment + chip-driven removal all work end-to-end.

## Effort

V2 is ~5–7 days after V1 lands:

- Day 1: V2.2 schema + Liquibase + domain + repos + service.
- Day 2: V2.2 facade + GraphQL controller + integration tests.
- Day 3: V2.3 chat callbacks + V2.1 BUILD agent wiring + destructive-tool guard.
- Day 4–5: V2.4 plus-button menu + detail panel + GraphQL client integration.
- Day 6: V2.5 conversation attached-tools list + edit/remove flows.
- Day 7: Smoke test + bug-bash + documentation.

## What V2 does NOT include

These wait for a future spec, on actual user demand:

- Tool aliases (user-renamable callbacks)
- Per-tool parameter validation rules (beyond what cluster element schema enforces)
- Tool sharing across conversations (a "favorites" library)
- Cross-workspace tool surfacing
- Visual tool-call traces in the conversation (debugging aid)

---

## Sequencing

V1 ships first, smoke-tests pass on staging-equivalent. Then V2.1 (BUILD agent) → V2.2 (persistence) → V2.3 (chat) → V2.4 (UI) → V2.5 (chips). V2.4 + V2.5 can land in parallel with V2.1–V2.3 if a second engineer picks up client work.
