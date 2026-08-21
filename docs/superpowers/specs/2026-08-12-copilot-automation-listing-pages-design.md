# Copilot on Automation Listing Pages — Design

**Date:** 2026-08-12
**Status:** Approved
**Base branch:** `0_732`

## Goal

Every command available in AI Hub becomes reachable from the in-app Copilot panel on the
Automation listing pages. Each page opens the panel scoped to its own domain sub-agent — the
same pattern the Data Table / Knowledge Base / Context Store detail pages use today. Example
gap being closed: the Projects page has no Copilot, so project-list operations (`listProjects`,
`createProject`, …) are only reachable through AI Hub.

## Background (state on 0_732)

- The Copilot panel is mounted once, globally, in `client/src/App.tsx`, gated by
  `ai.copilot.enabled`. A page needs no mounting work: it calls
  `setContext({source, parameters, mode})` + `setCopilotPanelOpen(true)` and registers a
  post-turn cache invalidation via `useCopilotPostTurnRegistry`.
- Chat routing is a naming convention: the client posts AG-UI requests to
  `/api/platform/internal/ai/chat/{source}`; `CopilotApiController` (EE `ai-copilot-rest`)
  rewrites the id to `<source>_ask` / `<source>_build` from the state's mode and looks up a
  `LocalAgent` bean with that id. Each bean has a hard-coded tool list and prompt file.
- **Domain slice pattern** (`DataTableAgentConfiguration`): one config class producing the
  panel ask/build `SpringAIAgent` beans *and* ask/build sub-agent `ChatClient` beans, all
  sharing one prompt pair and one `*ToolCallbacksFactory`. The sub-agent clients are wrapped
  as `*AgentToolCallback` tools by AI Hub and by the management MCP server
  (`McpServerToolCallbackContributor` SPI). Gate:
  `@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")`.
- **Manager pattern** (`DeploymentManagerConfiguration`, `McpManagerConfiguration`,
  `ApiCollectionManagerConfiguration`, `PersonalAgentManagerConfiguration`): a single
  `ChatClient` with a delegation-tuned prompt and inline tool set, gated on
  `ai.hub.enabled`, exposed to AI Hub and MCP via `ManagerSubAgentToolCallback` (with
  ask-relay on the AI Hub surface). No panel presence, no read/write split.
- **Generative one-shots** (`research`, `data_analyst`, `image_generator`, `slide_builder`):
  EE AI-Hub-only sub-agents that produce an artifact, persist it as an asset file, and return
  a summary. Not on the MCP surface.
- Asset-file CRUD tools already exist in CE `automation-ai-tool`
  (`ListAssetFiles`, `GetAssetFileContent`, `CreateAssetFile`, `CreateBinaryAssetFile`,
  `UpdateAssetFileContent`, `CloneAssetFile`) but are wired only into AI Hub (plus the
  read-only `getAssetFileContent` viewer on MCP).
- No Automation *listing* page has Copilot today — only detail pages
  (`DATA_TABLE`, `KNOWLEDGE_BASE`, `CONTEXT_STORE`, `SKILLS`, the executions sheet).

## Decisions

1. **Scope: 10 listing pages** (table below). Everything AI Hub can do today becomes
   reachable from a listing page.
2. **Approach: extend the per-source pattern** (new `Source` values + `<source>_ask/_build`
   agent beans + prompt pairs), not a shared mega-agent and not an SPI refactor.
3. **Agents are page-scoped.** Exception: the Project agent also receives
   `workflow_editor_agent` and `converter_agent` as sub-agent tools (and
   `code_workflow_agent` when the EE bean is present), because projects contain workflows.
4. **Client gate: `ai.copilot.enabled` only** (matching the newer detail pages; `ff-1570`
   stays untouched on the workflow-editor family). Server gate per slice: the
   copilot-or-hub `@ConditionalOnExpression`.
5. **Manager-to-slice conversion.** `deployment`, `mcp_server`, and `api_collection` stop
   being managers and become domain slices (shared prompt pair, ask/build split, one tool
   factory, three surfaces). **`personal_agent_manager` remains the only manager** — its UI
   home is AI Hub itself.
6. **Public tool names stay stable.** AI Hub and MCP keep exposing `deployment_manager`,
   `mcp_manager`, and `api_collection_manager` as tool names, now backed by the slices'
   BUILD `ChatClient`s. External MCP clients are unaffected.
7. **Asset Files becomes a tri-surface domain slice** (panel + AI Hub + MCP management),
   including a new `createAssetFileFromUrl` tool that downloads server-side so file bytes
   never pass through model context. **Generative one-shots stay inside AI Hub** — they are
   NOT attached to the asset slice.
8. **Non-goals:** Connections, API Clients, A2A Servers, Templates, Approval Tasks, Chats,
   AI Memories, AI Gateway, Tool Invocations, Personal Agents page (see Non-goals section).

## Page inventory

| # | Page | Route (`/automation/…`) | `Source` | Server work |
|---|------|--------------------------|----------|-------------|
| 1 | Projects | `projects` | `PROJECT` (new) | new slice |
| 2 | Deployments | `deployments` | `PROJECT_DEPLOYMENT` (new) | manager→slice conversion |
| 3 | Executions | `executions` | `WORKFLOW_EXECUTION` | none (agents exist) |
| 4 | Data Tables | `datatables` | `DATA_TABLE` | none |
| 5 | Knowledge Bases | `knowledge-bases` | `KNOWLEDGE_BASE` | none |
| 6 | Context Stores (EE) | `context-stores` | `CONTEXT_STORE` | none |
| 7 | MCP Servers | `mcp-servers` | `MCP_SERVER` (new) | manager→slice conversion |
| 8 | AI Skills | `ai/skills` | `SKILLS` | none |
| 9 | API Collections (EE) | `api-platform/api-collections` | `API_COLLECTION` (new) | manager→slice conversion (EE) |
| 10 | Asset Files | `asset-files` | `ASSET_FILE` (new) | new tri-surface slice |

Listing pages pass **empty `parameters`** — the agents already operate workspace-wide (that
is how AI Hub invokes them). Detail pages keep passing their entity id and share the same
`Source` as their listing page.

## Server design

### Source values

Add `PROJECT`, `PROJECT_DEPLOYMENT`, `MCP_SERVER`, `API_COLLECTION`, `ASSET_FILE` to
`server/libs/ai/ai-copilot/ai-copilot-api/.../util/Source.java` (singular, matching
`DATA_TABLE`).

### New domain slice configs

Each mirrors `DataTableAgentConfiguration`: ask/build `SpringAIAgent` beans named
`<source>_<mode>`, ask/build sub-agent `ChatClient` beans, one prompt pair, tools wrapped in
`RehydrateContextToolCallback`, class gated copilot-or-hub.

| Config | Module | ASK tools | BUILD tools |
|--------|--------|-----------|-------------|
| `ProjectAgentConfiguration` | CE `ai-copilot-service` | `ReadProjectTools`, `ReadProjectWorkflowTools` | `ProjectTools`, `ProjectWorkflowTools`, sub-agents: `workflow_editor_agent`, `converter_agent`, `code_workflow_agent` (EE-optional) |
| `ProjectDeploymentAgentConfiguration` | CE `ai-copilot-service` | list/read deployment tools | full lifecycle: create/update/delete/rollback/toggle/promote |
| `McpServerAgentConfiguration` | CE `ai-copilot-service` | list/read MCP server tools | full MCP server + tool mapping lifecycle |
| `ApiCollectionAgentConfiguration` | EE `automation-ai-copilot` | list/read collection tools | create/clone/update collections |
| `AssetFileAgentConfiguration` | CE `ai-copilot-service` | `listAssetFiles`, `getAssetFileContent` | + `createAssetFile`, `createBinaryAssetFile`, `updateAssetFileContent`, `cloneAssetFile`, `createAssetFileFromUrl` (new), `FirecrawlTools` (conditional) |

The EE-optional sub-agent attachment on the Project agent uses
`@Qualifier ObjectProvider<ChatClient>` — the same mechanism `AiHubConfiguration` uses for
its conditional specialists.

### Tool factories

Extract read/write-split factories so every surface builds its tool list from one place
(mirroring `DataTableToolCallbacksFactory`):

- `ProjectDeploymentToolCallbacksFactory`, `McpServerToolCallbacksFactory`,
  `AssetFileToolCallbacksFactory` — CE `automation-ai-tool`.
- `ApiCollectionToolCallbacksFactory` — EE `automation-ai-tool`.

### `createAssetFileFromUrl` (new tool)

Downloads a URL server-side and persists it via `AssetFileFacade` — content never enters
model context. Needed because `createBinaryAssetFile` requires base64 content inline, which
its own size guard exists to discourage. Enforce the same MIME/size/quota limits as
`CreateBinaryAssetFileToolCallback`.

### Manager-to-slice conversion

For `project_deployment`, `mcp_server`, `api_collection`:

- The manager `ChatClient` bean and its delegation prompt (`prompt_deployment_manager.txt`,
  …) are **replaced** by the slice's ask/build prompt pair and clients.
- AI Hub and the MCP contributors wrap the slice's BUILD client under the **existing public
  tool names** (`deployment_manager`, `mcp_manager`, `api_collection_manager`).
- **Ask-relay is ported:** managers are today the only sub-agents that can pose a question
  to the user mid-run. With shared prompts the ask tool must exist on every surface the
  prompt reaches: the panel registers `AskUserQuestionToolCallback` directly; the AI Hub
  wrapper keeps the `SubAgentAskRelay` managers have today; the MCP surface keeps the
  null-relay behavior (the question is returned as the tool's text result).
- `ManagerAgentType` keys are preserved for metrics/session-memory continuity.

### Controller dispatch cleanup

Replace the ~16 mechanical branches in `CopilotApiController` with one default rule
(`agentId = agentId + "_" + mode`), keeping only `converter` (always `_build`) as a special
case. An id that resolves to no registered `LocalAgent` returns an explicit client error
instead of today's silent fall-through to a `null` agent. New sources then need no
controller change.

### Agent types

Add `PROJECT_ASK/BUILD`, `PROJECT_DEPLOYMENT_ASK/BUILD`, `MCP_SERVER_ASK/BUILD`,
`ASSET_FILE_ASK/BUILD` to `CopilotAgentType`, and `API_COLLECTION_ASK/BUILD` to the EE type
provider, so session memory, guardrails, and metrics partition correctly.

### MCP management surface

A new contributor exposes `asset_file_agent` (wrapping the slice's BUILD client) on the
management MCP server, giving external MCP clients fetch-and-store parity. The read-only
`getAssetFileContent` viewer stays as-is.

**Workspace scoping on MCP is mandatory.** The MCP surface has no AI Hub chat state to
seed the tool context, so an unwrapped delegate forwards `Map.of()` and its
workspace-scoped inner tools fail ("Workspace context unavailable"). The copilot-domain
delegates contributed by `ToolCallbackContributorConfiguration` have exactly this
pre-existing defect today — only the managers work, because
`ManagerMcpContributorConfiguration` wraps them in `WorkspaceScopedManagerToolCallback`
(the name before this work renamed it — see below)
(optional `workspaceId` input, auto-select when the tenant has one workspace, typed
`workspace_required` error listing candidates otherwise). Therefore:

- **Generalize the wrapper**: `WorkspaceScopedManagerToolCallback` is renamed
  `WorkspaceScopedSubAgentToolCallback` and becomes usable for any
  sub-agent delegate (not just managers), keeping its behavior: optional `workspaceId`
  input appended to the schema, auto-select when the tenant has exactly one workspace,
  typed `workspace_required` error listing candidates otherwise.
- **Fix the pre-existing raw delegates**: every sub-agent delegate contributed to the MCP
  management surface gets the workspace-scope wrap — the CE copilot contributor
  (`workflow_editor_agent`, `code_editor_agent`, `cluster_element_agent`, `skills_agent`,
  `workflow_execution_agent`, `converter_agent`, `knowledge_base_agent`,
  `data_table_agent`), the EE automation contributor (`context_store_agent`,
  `custom_component_agent`, `code_workflow_agent`), and the EE embedded contributor
  (`workflow_editor_embedded_agent`). Copilot panel and AI Hub surfaces are untouched —
  they already seed workspace context from chat state.
- The new `asset_file_agent` MCP contribution uses the same wrap from day one.
- The manager-to-slice conversions keep the wrap on the MCP surface — converting the
  backing agent must not regress MCP workspace handling.

### Module placement rule

Shared capability never lives in `ai-hub` modules. Slices, tool factories, and tool
implementations live in the shared modules (CE/EE `automation-ai-tool`, `ai-copilot-*`);
the `ai-hub` modules retain only hub-surface-specific pieces: the routing agents and tool
catalogs, task/artifact tracking, progress/guardrail/metering wrappers,
`personal_agent_manager`, the generative one-shots, and hub UI tools (`openFileTab`).
Concretely for this design:

- `AiHubConfiguration` stops registering asset-file write tools directly on the root
  agent's catalogs and instead wraps the asset slice's BUILD client as `asset_file_agent`
  (same as `data_table_agent` today). The generative one-shots keep their internal
  `createBinaryAssetFile` — that is their artifact pipe, not a shared capability.
- The three converted managers already live outside ai-hub (`automation-ai-tool`), so the
  conversion moves nothing between modules — only their wiring changes.

### Prompt conventions

- New prompt pairs: `prompt_project_ask/build.txt`, `prompt_project_deployment_ask/build.txt`,
  `prompt_mcp_server_ask/build.txt`, `prompt_api_collection_ask/build.txt` (EE),
  `prompt_asset_file_ask/build.txt`.
- BUILD prompts document `askUserQuestion` and require confirm-before-destroy for
  destructive operations (delete project, rollback/delete deployment, delete MCP server).
- Conditional tools drop their prompt section when absent (Firecrawl on the asset agent,
  `code_workflow_agent` on the Project agent) — a documented-but-unregistered tool makes the
  model call it and fail the turn ("No ToolCallback found").

## Client design

- **`Source` enum sync** in `useCopilotStore.ts`: add `PROJECT`, `PROJECT_DEPLOYMENT`, `MCP_SERVER`,
  `API_COLLECTION`, `ASSET_FILE`.
- **Post-turn registry goes multi-callback**: `useCopilotPostTurnRegistry` changes from one
  callback per `Source` to a set; `register` returns an unregister that removes only its own
  entry; `CopilotRuntimeProvider.runFor` runs all callbacks for the source. This lets a
  listing page and a detail page share a `Source` without clobbering each other.
- **`CopilotButton` resurrection**: the currently-orphaned
  `shared/components/copilot/CopilotButton.tsx` becomes the standard listing-page trigger.
  It takes `{source, parameters?, mode?}`, renders the `SparklesIcon` button, performs
  `setContext` + `setCopilotPanelOpen(true)`, and gates on `ai.copilot.enabled` only
  (dropping its current `ff-1570` check — safe, nothing imports it today).
- **Per-page wiring** — each page adds the button to its `Header right` slot plus one
  `useEffect` registering the post-turn invalidation:

| Page | Post-turn invalidates |
|------|-----------------------|
| Projects | projects list + workflows queries |
| Deployments | project-deployments list |
| Executions | executions page query |
| Data Tables | `dataTables` |
| Knowledge Bases | KB list query |
| Context Stores | context stores query |
| MCP Servers | MCP servers GraphQL query |
| AI Skills | skills list |
| API Collections | API collections query |
| Asset Files | asset files list |

- **Deliberately unchanged:** the conversation reset on route change; the executions sheet's
  local panel (the page-level button uses the global panel); no `useCopilotLayoutShifted` on
  listing pages (`LayoutContainer` flexes naturally — verify visually during implementation).

## Error handling

- Unknown agent id after the controller rewrite → explicit 400 naming the resolved id.
- Destructive operations confirm first via `askUserQuestion` (renders as the interactive
  question widget in the panel; text round-trip on AI Hub/MCP).
- Tool failures keep the `ToolErrors` typed-error path.
- A hub-enabled/copilot-disabled server materializes unused panel agent beans — harmless,
  identical to the data-table slice today.

## Testing

- **Server:** bean-wiring tests per new config (agent ids resolve; ask gets read tools,
  build gets write tools); `CopilotApiController` tests for the default-branch rewrite,
  converter special case, and unknown-id error; factory tests for each extracted
  `*ToolCallbacksFactory` read/write split; `createAssetFileFromUrl` tests (happy path,
  size/MIME rejection, unreachable URL).
- **Client:** `useCopilotPostTurnRegistry` multi-callback semantics (two registrations both
  fire; unregister removes only its own); `CopilotButton` gating + props; one listing-page
  test asserting the button renders only when `ai.copilot.enabled`.
- **Manual:** one BUILD round-trip per page against the dev server, verifying the post-turn
  refresh updates the list; AI Hub regression pass on `deployment_manager`/`mcp_manager`/
  `api_collection_manager` after the conversion (names unchanged, ask-relay still works).

## Rollout

- No new flags, no migrations, no GraphQL schema changes.
- Implementation on a fresh branch off `0_732`, landed via rebase + ff-only.
- Commits split server/client per the repo convention.
- Sizing: 5 pages are client-only; the Project slice (new prompts + sub-agent composition)
  is the largest single piece; the 3 manager conversions are mechanical factory extractions
  plus prompt rewrites; the asset slice adds one new tool.

## Non-goals (this iteration)

- **Connections** — no agent exists anywhere (AI Hub cannot do connection ops either);
  would need a new connections agent from scratch.
- **API Clients (EE)** — `api_collection_manager` covers collections only; extend later,
  cheap once the api-collection slice exists.
- **A2A Servers, Templates, Approval Tasks, Chats, AI Memories, AI Gateway, Tool
  Invocations** — no agent, no AI Hub precedent.
- **Personal Agents page** — lives inside AI Hub, which already has the full chat.
- **Attaching generative one-shots to the asset slice** — explicitly deferred; they remain
  AI-Hub-only.
- **Unifying the executions sheet's local panel with the global store** — deferred to a
  dedicated **phase 5** (user-approved 2026-08-12). Five surfaces hand-roll a one-deep
  conversation stack via `saveConversationState`/`restoreConversationState`: the CE and EE
  `useWorkflowExecutionSheet` hooks, `useSampleOutputCopilot`,
  `usePropertyJsonSchemaBuilderCopilot`, and `useWorkflowCodeEditorSheet`. The fix is a real
  push/pop conversation stack in `useCopilotStore`, not moving those surfaces onto the global
  panel — modal dialogs render their panel inside the modal deliberately, because the global
  aside is unreachable behind an overlay. Phase 5 supersedes the one-line guard this phase
  adds in `useWorkflowExecutionSheet`.
- **Copilot source-agent SPI refactor** (Approach C) — revisit if source count keeps
  growing.
