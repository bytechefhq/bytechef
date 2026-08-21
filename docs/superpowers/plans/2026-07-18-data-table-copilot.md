# Data Table Copilot Implementation Plan (Slice 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the Domain Copilot pattern (Slices 1-2 on this branch are the templates) to Data Tables: ASK/BUILD Copilot on the Data Table detail page + a `data_table_agent` AI-Hub subagent. Design: `docs/superpowers/specs/2026-07-17-domain-copilot-context-store-kb-datatable-design.md`.

**Architecture:** Mirror the Knowledge Base slice, with two DT-specific decisions from exploration: (1) **`QueryDataTableToolCallback` (4-arg, CSV-export) STAYS flat on ai_hub** — its CSV branch needs `ArtifactGeneratorRegistry`/`AiHubTaskService` and context fields (`sourceOrdinal`, `lastUserPrompt`) that `AgentToolInvocationContext` lacks; the shared lib gets a 2-arg port (no CSV) for the panel + subagent, so no capability is lost. (2) The four artifact-recording mutation tools (AddColumn/AddRow/UpdateRow/DeleteRow) **reuse Slice 2's `ToolMutationArtifactRecorder`** (+ existing `AiHubToolMutationArtifactRecorder` adapter). `OpenDataTableTabToolCallback` stays flat on ai_hub (needs `AiHubTaskArtifactRecorder`). New top-level `dropDataTable` tool. Environment is the **ordinal** passed straight from context (existing DT-tool convention, `0L` default) — no environment-service lookup.

**Tech Stack:** Java 21 / Spring AI / Gradle; React + TS + vitest.

## Global Constraints

- Worktree root for everything: `/Volumes/Data/bytechef/bytechef/.claude/worktrees/context-store-copilot`.
- Shared tools → `.../automation-ai-tool/.../tool/datatable/` (package `com.bytechef.ee.automation.ai.tool.datatable`); Enterprise headers; `ToolErrors` conventions; `AgentToolInvocationContext` for workspace/env (its **instance** method `resolveEnvironmentOrDefault()` replaces the old static `AiHubToolInvocationContext.resolveEnvironmentOrDefault(ctx)` calls).
- Fresh gradle (`--rerun-tasks`) + checkstyle on touched modules every task; both Source enums stay in sync (`DATA_TABLE`).
- Templates (read the real files): Slice 2's `knowledgebase/` classes, `KnowledgeBaseToolCallbacksFactory`, `KnowledgeBaseSpringAIAgent`, `prompt_knowledge_base_*.txt`, `KnowledgeBaseAgentConfiguration`, `KnowledgeBaseAgentToolCallback`, the `knowledge_base` controller branch, and the KB frontend wiring.

---

## Task 1: `DATA_TABLE` in both Source enums

Mirror S2 Task 1: backend `Source.java` + frontend `useCopilotStore.ts` gain `DATA_TABLE` (after `KNOWLEDGE_BASE`). Verify `ai-copilot-api:compileJava`. Commit `feat(copilot): add DATA_TABLE source enum on both surfaces`.

---

## Task 2: Move 9 DT tool callbacks to the shared lib; port a 2-arg Query

**Move (git mv)** from ai-hub-service `tool/` to `.../tool/datatable/` (+ tests where they exist — `ListDataTablesToolCallbackTest`, `AggregateDataTableToolCallbackTest`, `CreateDataTableToolCallbackTest`, `CreateDataTableFromCsvToolCallbackTest`):
- Clean (package + context swap only): `ListDataTablesToolCallback`, `AggregateDataTableToolCallback`, `CloneDataTableToolCallback`.
- Static→instance env fix as well: `CreateDataTableToolCallback`, `CreateDataTableFromCsvToolCallback` (`AiHubToolInvocationContext.resolveEnvironmentOrDefault(ctx)` → `ctx == null ? 0L : ctx.resolveEnvironmentOrDefault()` — match how `AgentToolInvocationContext` exposes it; read the class first).
- Recorder rework (exactly like S2's Add/Delete-document tools: `AiHubTaskArtifactService` → `@Nullable ToolMutationArtifactRecorder`, `threadId()` → `conversationId()`, preserve userId guard, kind as enum-name string): `AddDataTableColumnToolCallback`, `AddDataTableRowToolCallback`, `UpdateDataTableRowToolCallback`, `DeleteDataTableRowToolCallback`.

**Do NOT move**: `OpenDataTableTabToolCallback` (stays, untouched) and `QueryDataTableToolCallback` (stays in ai-hub with its 4-arg CSV constructor, **untouched**).

**Create** `.../tool/datatable/QueryDataTableToolCallback.java`: a port of the ai-hub class's NON-CSV path only — 2-arg constructor `(DataTableRowService, DataTableRowService-partner...)` — read the real 2-arg convenience constructor `(DataTableRowService dataTableRowService, DataTableService dataTableService)` and the non-export `call(...)` logic; same TOOL_NAME `queryDataTable`, same input schema minus nothing (keep `exportToCsv` rejected with the existing "not supported" message the 2-arg path already produces — mirror it), context swap applied. (Same class name in a different package is fine; the ai-hub one keeps its EE package.)

**Deps** to add to `automation-ai-tool/build.gradle.kts`:
```kotlin
implementation(project(":server:libs:automation:automation-data-table:automation-data-table-api"))
implementation(project(":server:libs:platform:platform-data-table:platform-data-table-api"))
```
(+ minimal transitive if compilation demands — report it.)

**Repoint `AiHubConfiguration`**: imports of the 9 moved classes → new package; in `registerDataTableMutationToolCallbacks`, pass `new AiHubToolMutationArtifactRecorder(taskArtifactService)` to the 4 recorder tools (constructor args otherwise unchanged); ALL registrations stay (flat removal is Task 8); flat `QueryDataTableToolCallback` + `OpenDataTableTabToolCallback` untouched.

Verify fresh: `automation-ai-tool:test` + `ai-hub-service:compileJava` (`--rerun-tasks`) BUILD SUCCESSFUL; checkstyle (main+test) both modules clean. Commit `refactor(data-table): move copilot tool callbacks to shared automation-ai-tool lib`.

---

## Task 3: New `dropDataTable` tool

Create `.../tool/datatable/DropDataTableToolCallback.java` + test. Mirror `DeleteContextStoreToolCallback`'s shape BUT: facade `WorkspaceDataTableFacade.dropTable(long dataTableId, long environmentId)` (authz via its `@PreAuthorize hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')`); `id` from tool input; `environmentId` from `AgentToolInvocationContext` following the existing DT-tool convention (instance `resolveEnvironmentOrDefault()` with `0L` default when context absent — copy the exact pattern from a moved DT tool). TOOL_NAME `dropDataTable`. DESCRIPTION: drops the entire data table and all its rows in the current environment; irreversible; always confirm with the user first. TDD tests: happy path (`verify(facade).dropTable(42L, 0L)` + `"deleted":true`); missing id; facade `IllegalArgumentException` surfaced. Fresh targeted test + module checkstyle. Commit `feat(data-table): add dropDataTable tool`.

---

## Task 4: `DataTableToolCallbacksFactory`

Mirror `KnowledgeBaseToolCallbacksFactory`(+Test). Deps (verify vs real constructors): `WorkspaceDataTableFacade`, `DataTableService`, `DataTableRowService`, `@Nullable ToolMutationArtifactRecorder`.
- `readToolCallbacks()`: `listDataTables`, `queryDataTable` (shared 2-arg), `aggregateDataTable`.
- `writeToolCallbacks()`: read + `addDataTableRow`, `updateDataTableRow`, `deleteDataTableRow`, `addDataTableColumn`, `createDataTable`, `createDataTableFromCsv`, `cloneDataTable`, `dropDataTable`.
Test asserts memberships (read excludes `dropDataTable`/`createDataTable`; write includes them). Fresh test + checkstyle. Commit `feat(data-table): add tool-list factory for ASK/BUILD`.

---

## Task 5: `DataTableSpringAIAgent` + prompts

Agent class: names-only mirror of `KnowledgeBaseSpringAIAgent`. Prompts:

`prompt_data_table_ask.txt`:
```
You are the Data Table assistant in ByteChef, embedded in the Data Table detail page.

You help the user understand and query Data Tables: what tables exist, their columns, and the rows
inside them. Use `listDataTables` to discover tables and their column schemas, `queryDataTable` to
fetch/filter rows, and `aggregateDataTable` for counts/sums/grouped aggregations.

You are READ-ONLY. Never create, modify, or delete anything. If the user asks to change something,
explain what you would change and tell them to switch to Build mode.

Be concise. Cite table names and ids. If workspace context is unavailable, say so.
```

`prompt_data_table_build.txt`:
```
You are the Data Table builder in ByteChef, embedded in the Data Table detail page.

You can inspect AND modify Data Tables. Available actions: create a table (`createDataTable`),
create from CSV (`createDataTableFromCsv`), add a column (`addDataTableColumn`), add/update/delete
rows (`addDataTableRow`, `updateDataTableRow`, `deleteDataTableRow`), clone a table
(`cloneDataTable`), and drop an entire table (`dropDataTable`).

Always call `listDataTables` (and `queryDataTable` where helpful) first to ground yourself in the
current schema and data. Before any irreversible action (delete row, drop table), state exactly what
will be removed and get the user's explicit confirmation in the conversation before calling the tool.

Be concise; report the ids you created or changed.
```

Fresh `ai-copilot-service:compileJava` + `checkstyleMain`. Commit `feat(copilot): add data table agent class + ask/build prompts`.

---

## Task 6: EE `DataTableAgentConfiguration`

Names-substituted mirror of `KnowledgeBaseAgentConfiguration` (same gate, helpers, recorder via `ObjectProvider<AiHubTaskArtifactService>`): factory bean + `dataTableAskSpringAIAgent`/`dataTableBuildSpringAIAgent` (agentIds `data_table_ask`/`data_table_build`) + `dataTableAskSubAgentChatClient`/`dataTableBuildSubAgentChatClient`. Verify the real `DataTableToolCallbacksFactory` constructor. Fresh `ai-hub-service:compileJava` + `checkstyleMain`. Commit `feat(copilot): add EE DataTableAgentConfiguration (source agents + subagent clients)`.

---

## Task 7: `DataTableAgentToolCallback` + `CopilotAgentType`

Mirror `KnowledgeBaseAgentToolCallback`(+Test incl. `ObjectMapperSetupExtension`): tool `data_table_agent`, `CopilotAgentType.DATA_TABLE_AGENT`. DESCRIPTION:
```
Delegate a user request about Data Tables to a specialised Data Table subagent.
A Data Table is a structured, environment-scoped table with typed columns and rows. The subagent
owns listing, querying, aggregating, and (in build mode) creating tables (incl. from CSV), managing
columns and rows, cloning, and dropping tables. Prefer calling it over reasoning about data tables
directly. Returns a synthesised markdown report or a summary of the mutations performed.
```
Enum: `DATA_TABLE_ASK("data_table_ask", false), DATA_TABLE_BUILD("data_table_build", false), DATA_TABLE("data_table", true), DATA_TABLE_AGENT("data_table_agent", false),`. TDD; fresh test + checkstyle. Commit `feat(copilot): add data_table_agent subagent tool callback`.

---

## Task 8: Wire delegation; remove flat DT tools (except Query + OpenTab)

1. Contributor (`ToolCallbackContributorConfiguration`): `@Qualifier("dataTableBuildSubAgentChatClient")` provider + `new DataTableAgentToolCallback(chatClient)` + import.
2. `CopilotApiController`: `data_table` branch after `knowledge_base` (→ `data_table_ask`/`data_table_build`).
3. `AiHubConfiguration` dual registration: `registerCopilotSubAgentToolCallbacks` param `dataTableSubAgentChatClientProvider` + `ProgressReportingToolCallback(new DataTableAgentToolCallback(chatClient), "data_table_agent")`; ASK/BUILD `@Qualifier("dataTableAsk/BuildSubAgentChatClient")` params + call-site args.
4. Remove flat DT tools: `ListDataTablesToolCallback` (both agents); the whole `registerDataTableMutationToolCallbacks` call + method (BUILD). **KEEP** flat `QueryDataTableToolCallback` (4-arg CSV, both agents) and `OpenDataTableTabToolCallback` (both agents). Clean up imports/params that become unused (careful: `dataTableRowService`/`dataTableService` are still needed by the kept flat Query registration).
Fresh 3-module compile + `ai-hub-service:checkstyleMain` (0) + `ai-hub-service:test` (adapt tests only as needed; report). Commit `refactor(ai-hub): delegate data tables to data_table_agent subagent; drop flat mutation tools`.

---

## Task 9: Frontend trigger + invalidation on the DT detail page

Files: `client/src/pages/automation/datatable/DataTable.tsx` (id via `useParams`), `components/DataTableHeader.tsx` (**already has a right slot** — insert the button there via a prop or directly, matching how the header composes its right-side controls; read it first), NEW test `client/src/pages/automation/datatable/tests/DataTable.test.tsx` (no top-level test exists — follow `KnowledgeBase.test.tsx`'s mock-everything pattern).

Mirror the KB wiring: `openCopilot` → `setContext({mode: MODE.ASK, parameters: {dataTableId: id}, source: Source.DATA_TABLE})` + open panel; gate on `ai.copilot.enabled`; post-turn `register(Source.DATA_TABLE, ...)` with cleanup invalidating `['dataTables']` and `['dataTableRowsPage']` (verify these literals against sibling `invalidateQueries` call sites + generated hooks). TDD: failing test (click → setContext args + panel open) → implement → `npm --prefix client test -- DataTable` all pass → tsc clean. Commit `feat(data-table): add copilot trigger + post-turn invalidation to detail page`.

---

## Task 10: Full-slice verification

- Gradle: `automation-ai-tool:test`, `ai-copilot-tool:test`, `ai-copilot-service:compileJava`, `ai-copilot-rest:compileJava`, `ai-hub-service:test`, `ai-hub-service:checkstyleMain` → BUILD SUCCESSFUL.
- `npm --prefix client test -- DataTable` + tsc → clean.
- Ledger notes for anything unverifiable without app boot.

## Self-Review

Coverage: tools+query-port (T2), drop tool (T3), factory (T4), agent+prompts (T5), EE config (T6), subagent callback+enum (T7), dual registration+controller+flat removal preserving Query/OpenTab (T8), frontend (T1, T9), verification (T10). Names consistent: `data_table_ask`/`data_table_build`/`data_table_agent`, `Source.DATA_TABLE`, `dropDataTable`. No placeholders — every "mirror X" names a real file on this branch.
