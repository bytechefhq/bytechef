# Domain Copilot for Context Store, Knowledge Base & Data Tables — Design

**Status:** Draft
**Date:** 2026-07-17
**Author:** Ivica Cardic

## Motivation

The Copilot side panel (`App.tsx`, gated by `ai.copilot.enabled`) is wired into the
workflow editor, its sub-editors, workflow executions, and the Skills detail page — but not
into the **Context Store**, **Knowledge Base**, or **Data Table** detail pages. Users working
on those resources have no in-context assistant, even though the AI Hub already carries a full
set of tools for all three domains.

This design brings the three resource-detail pages to parity with Skills by applying the
**full Skills stack** — a per-source Copilot agent pair (ASK + BUILD), a shared tool library,
and a delegating AI Hub subagent — to each domain. It is a *repeatable pattern* defined once
and applied three times.

## Key finding: the tools already exist

The mutation/query logic for all three domains already exists as flat `*ToolCallback` beans
registered directly on the single `ai_hub` agent (`ai-hub-service/.../tool/`):

- **Context Store** (9): `ListContextSourcesToolCallback`, `SearchContextStoreToolCallback`,
  `SemanticSearchContextStoreToolCallback`, `GetContextStoreRecordToolCallback`,
  `CreateContextStoreSourceToolCallback`, `UpdateContextStoreSourceToolCallback`,
  `DeleteContextStoreSourceToolCallback`, `RefreshContextStoreSourceToolCallback`,
  `SetContextStoreSourceEnabledToolCallback`.
- **Knowledge Base** (6): `ListKnowledgeBasesToolCallback`, `QueryKnowledgeBaseToolCallback`,
  `AddKnowledgeBaseDocumentToolCallback`, `DeleteKnowledgeBaseDocumentToolCallback`,
  `CloneKnowledgeBaseToolCallback`, `OpenKnowledgeBaseTabToolCallback`.
- **Data Table** (11): `ListDataTablesToolCallback`, `QueryDataTableToolCallback`,
  `AggregateDataTableToolCallback`, `CreateDataTableToolCallback`,
  `CreateDataTableFromCsvToolCallback`, `AddDataTableColumnToolCallback`,
  `AddDataTableRowToolCallback`, `UpdateDataTableRowToolCallback`,
  `DeleteDataTableRowToolCallback`, `CloneDataTableToolCallback`,
  `OpenDataTableTabToolCallback`.

So the work is mostly **relocating and re-binding** existing tools plus new prompts and wiring —
not writing tool logic from scratch. The exceptions are two brand-new delete tools (below).

## Scope decisions (from brainstorming)

- **Capability:** Ask + safe actions, i.e. a full ASK/BUILD pair per domain. BUILD reuses the
  existing mutation tools, **including deletes** (consistent with the AI Hub, which already
  exposes them).
- **Placement:** the resource **detail** page only (per domain); list pages are out of scope.
- **Tool sharing:** the Skills full pattern — extract the tools into a shared library and expose
  each domain through a **dedicated subagent** (`context_store_agent`, `knowledge_base_agent`,
  `data_table_agent`) that the AI Hub delegates to, replacing the flat tools on `ai_hub`.
- **AI Hub scope:** consolidate `ai_hub`'s flat tools for each domain behind the new subagent
  (matching the `custom_component_agent` SP-C precedent, `2026-07-17-custom-component-build-subagent-sp-c-design.md`).
- **Delete parity — new tools:** add the three missing top-level delete tools so BUILD has
  full-CRUD parity across all domains:
  - `deleteContextStore` → `WorkspaceContextStoreFacade.deleteWorkspaceContextStore(workspaceId, contextStoreId)`.
  - `dropDataTable` → `WorkspaceDataTableFacade.dropTable(dataTableId, environmentId)` (UI calls this "drop").
  - `deleteKnowledgeBase` → `WorkspaceKnowledgeBaseFacade.deleteWorkspaceKnowledgeBase(knowledgeBaseId)`.

  (Source-level / document-level / row-level deletes already have tools.)
- **Doc structure:** one pattern spec (this doc) + three sequenced slices; Context Store first as
  the tracer bullet.

## Non-goals

- No change to the existing Copilot surfaces (workflow editor, code editor, cluster element,
  skills, etc.).
- No new AI Hub task kinds; routing stays a tool call inside an AI Hub turn.
- No list-page Copilot entry points.
- No changes to the underlying facades/services beyond calling existing delete methods.

## Current-state reference (how Skills does it)

Skills is the canonical full stack. Every piece below has a Skills exemplar we copy.

**Read/write tool split.** `ReadSkillsTools` (queries) feeds ASK; `SkillsTools` (mutations) feeds
BUILD. Both live in the shared lib
`libs/automation/automation-ai/automation-ai-tool/.../com/bytechef/automation/ai/tool/` as
`@Component` classes with `@Tool`-annotated methods calling `AiSkillFacade`.

**Panel source agents** (stateful, `ChatMemory`, keyed `SKILLS_ASK` / `SKILLS_BUILD`) —
`skillsAskSpringAIAgent` / `skillsBuildSpringAIAgent` in `CopilotConfiguration`, each with a
classpath system-prompt resource. The panel selects the agent from `context.source` + `context.mode`.

**Subagent chat clients** (stateless, no memory) — `skillsAskSubAgentChatClient` /
`skillsBuildSubAgentChatClient` in `CopilotConfiguration`.

**Subagent tool callback** — `SkillsAgentToolCallback` wraps the BUILD subagent chat client and is
registered in `ToolCallbackContributorConfiguration` (a `McpServerToolCallbackContributor`) so the
AI Hub agent gets one `skills_agent` tool instead of the raw primitives.

**`CopilotAgentType`** carries an enum entry per agent
(`ai-copilot-tool/.../CopilotAgentType.java`).

**Frontend** — `Source` enum in
`client/src/shared/components/copilot/stores/useCopilotStore.ts`; a `CopilotButton` on the detail
page calling `setContext({source, mode, parameters})` (see `AiSkillDetail.tsx:184`); post-turn
query invalidation so BUILD mutations refresh the page.

## The Domain Copilot pattern

For a domain `X`, the full stack is:

### Backend

1. **Shared tool library** (EE): `ReadXTools` (queries) + `XTools` (mutations) as `@Component`
   `@Tool` classes in `ee/libs/automation/automation-ai/automation-ai-tool/.../tool/`, ported from
   the existing `ai-hub-service` `*ToolCallback` bodies, calling the same facades/services. Read
   vs write split:
   - **Read (ASK):** list / search / semantic-search / query / aggregate / get-record.
   - **Write (BUILD):** create / update / delete / refresh / enable / add-column / add-row /
     update-row / delete-row / clone / create-from-csv, plus the new top-level delete.
2. **Two panel source agents** in `CopilotConfiguration`: `xAskSpringAIAgent` (`X_ASK`, ASK
   toolset + `ChatMemory`) and `xBuildSpringAIAgent` (`X_BUILD`, full toolset), each with a
   system-prompt resource.
3. **Two stateless subagent chat clients**: `xAskSubAgentChatClient`, `xBuildSubAgentChatClient`.
4. **Subagent tool callback** `XAgentToolCallback` (wrapping the BUILD subagent), registered in
   `ToolCallbackContributorConfiguration`; **remove** X's flat tool registrations from
   `AiHubConfiguration` (keep only signaling tools like `openXTab`).
5. **`CopilotAgentType`** entries.
6. **System-prompt resources** `prompt_x_ask.txt` / `prompt_x_build.txt` in
   `ai-copilot-service/src/main/resources/`.

### Frontend

7. **`Source` enum** entry in `useCopilotStore.ts`.
8. **`CopilotButton`** in the detail page header, `setContext({source: Source.X, mode,
   parameters: {xId, environmentId, workspaceId}})`.
9. **Post-turn query invalidation** registered so BUILD mutations refresh the page's GraphQL cache.

### Data flow

Panel open → `context.source` + `context.mode` pick `X_ASK` or `X_BUILD` SpringAIAgent → agent runs
with its domain toolset scoped by `xId` → in BUILD, mutations hit the facades → post-turn registry
invalidates page queries → the table/list refreshes. In the AI Hub, the same behaviour is reached
through the delegating `x_agent` subagent tool.

## Per-domain application

| Domain | `Source` | Detail page (route) | New delete tool | Signaling tool kept on `ai_hub` |
|---|---|---|---|---|
| Context Store | `CONTEXT_STORE` | `context-store/ContextStoreSources.tsx` (`context-stores/:id`) | `deleteContextStore` (new) | — |
| Knowledge Base | `KNOWLEDGE_BASE` | `knowledge-base/KnowledgeBase.tsx` (`knowledge-bases/:id`) | `deleteKnowledgeBase` (new) | `openKnowledgeBaseTab` |
| Data Table | `DATA_TABLE` | `datatable/DataTable.tsx` (`datatables/:id`) | `dropDataTable` (new) | `openDataTableTab` |

### Slice 1 — Context Store (tracer bullet)

Proves the full loop end to end: extract 9 tools + add `deleteContextStore` → split read/write →
two source agents + two subagent clients → `ContextStoreAgentToolCallback` + remove flat tools from
`AiHubConfiguration` → `CONTEXT_STORE` source + `CopilotButton` on `ContextStoreSources.tsx` →
post-turn invalidation of `contextStores` / `contextStoreSources` queries.

### Slice 2 — Knowledge Base

Same recipe over the 6 KB tools + add `deleteKnowledgeBase`; keep `openKnowledgeBaseTab` on
`ai_hub`. Button on `KnowledgeBase.tsx`.

### Slice 3 — Data Tables

Same recipe over the 11 DT tools + add `dropDataTable`; keep `openDataTableTab` on `ai_hub`. Button
on `DataTable.tsx`.

## Module placement

The shared tool lib is EE (`ee/libs/automation/automation-ai/automation-ai-tool`) because
Context Store is EE-only and the AI Hub is EE. Skills' shared lib is the OSS
`automation-ai-tool`; the EE sibling already exists (SP-B's `CustomComponentTools` lives there),
so the domain tool classes follow that placement. Copilot config (`ai-copilot-service`) and AI Hub
(`ai-hub-service`) both already depend on it — correct dependency direction, no cycle.

## Error handling & authorization

- Tool errors keep the existing `ToolErrors` / `toolError` convention already used by every ported
  callback.
- Authorization is unchanged: the ported tools call the same facades, which enforce the same
  workspace/environment scoping and permission checks the flat tools relied on. No new guard
  surface. Deletes remain allowed (they exist today on `ai_hub`); the two new delete tools call
  existing facade delete methods that already back the UI's delete/drop actions.

## Testing

- **Unit** — one test per new `@Tool` method, ported from the existing `*ToolCallbackTest`
  suites; new tests for `deleteContextStore` and `dropDataTable`.
- **Subagent** — one `XAgentToolCallback` test per domain, mirroring
  `SkillsAgentToolCallbackTest` (blank-request rejection, delegation, result pass-through).
- **Frontend** — a detail-page test per domain asserting the `CopilotButton` sets the right
  `context.source` and that BUILD post-turn invalidation fires, mirroring the Skills detail test.
- **Regression** — verify the AI Hub still performs each domain's operations through the new
  subagent after the flat tools are removed.

## Sequencing

Context Store → Knowledge Base → Data Tables. Each slice is an independently shippable plan sharing
this pattern doc. Build and prove Context Store fully before starting the next.

## Open items

1. **System-prompt authoring** — draft ASK/BUILD prompts per domain modeled on the Skills prompts,
   for product to refine.
