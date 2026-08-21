# AI Hub Phase 5A — Mutation preview + Apply / Discard

**Status**: Draft
**Date**: 2026-04-23
**Builds on**: Phases 1–4 and 4.5 (all merged).
**Scope**: Add a server-side **staging layer** so AI Hub mutations are previewed before they apply. The agent stages the mutation, the user sees a pending-changes bar with a diff summary, and explicitly Applies or Discards. Undo and a mutation-log audit viewer are deferred to Phase 5A.2 / 5A.3.

---

## Goal

Every BUILD-mode mutation today is fire-and-forget: the agent calls `updateWorkflow` or `addDataTableRow`, the change commits, and the user has no chance to review. The biggest real-world risk of BUILD mode is an agent doing something silently wrong — a bad row update, a workflow definition merge that lost a field. Phase 5A closes this gap by making every mutation go through a staging step.

Success in v5A:
- A user asks the agent "set row 42's status to qualified." The agent calls `updateDataTableRow`. Instead of committing, a "1 pending change" bar appears above the resource panel with a human-readable diff. The user clicks Apply; the mutation commits. Or Discard; the mutation never happens.
- Multiple agent mutations in one turn aggregate into one pending-changes set the user reviews as a batch.
- ASK mode is unaffected (no mutations exist there).
- Read/query/open-tab tools and subagent delegations (research, workflow_builder, data_analyst) are **not** staged — they're side-effect-free or produce files (which are themselves a mutation, but a low-risk additive one that gets staged like any other).

## Non-goals (v5A)

- **Undo** after apply. Phase 5A.2.
- **Audit viewer** listing all past applied mutations with who/what/when. Phase 5A.3.
- **Multi-user conflict resolution**. Staged mutations are per-user (keyed by user + workspace); last-write-wins at Apply time same as today.
- **Partial apply** (apply subset of pending changes). v5A is Apply-all or Discard-all per pending set. Subset apply is a later UX polish.
- **Long-lived pending changes**. Pending rows have a TTL (e.g. 24 hours) and are garbage-collected. Designed for in-session use, not persistent drafting.

## Architecture overview

### Staging model

One new DB table: `ai_hub_pending_mutation`.

```sql
ai_hub_pending_mutation (
  id                BIGSERIAL PRIMARY KEY,
  workspace_id      BIGINT    NOT NULL,
  user_id           BIGINT    NOT NULL,
  tool_name         VARCHAR   NOT NULL,  -- e.g. "updateDataTableRow"
  input_json        TEXT      NOT NULL,  -- original tool input, verbatim
  diff_summary      TEXT      NOT NULL,  -- human-readable "+x / -y" summary
  status            VARCHAR   NOT NULL,  -- PENDING | APPLIED | DISCARDED | EXPIRED
  created_at        TIMESTAMP NOT NULL,
  applied_at        TIMESTAMP NULL,
  expires_at        TIMESTAMP NOT NULL   -- created_at + 24h
)
```

Indexed by `(workspace_id, user_id, status)` for efficient pending-set lookup.

### Server-side flow

A new `MutationStagingService` sits between each mutation `ToolCallback` and its target service. Each of the 8 Phase 4 mutation callbacks (plus future mutation callbacks) is refactored to:

1. Validate input (unchanged).
2. Compute a **diff summary** — a short string describing what the mutation will do. Callback-specific: e.g. `updateDataTableRow` computes `{column: old→new}` pairs by reading the current row; `createWorkflow` summarizes "Create workflow 'Leads Sync' in project X with 4 tasks."
3. **Stage** via `MutationStagingService.stage(workspaceId, userId, toolName, inputJson, diffSummary)` → returns the new pending row's id.
4. Return `{staged: true, pendingId, diffSummary}` as the tool-call JSON result. **Does not commit.**

Two new REST endpoints (EE, workspace-scoped):
- `POST /api/automation/internal/ai-hub/pending-mutations/{id}/apply` — atomically: load the pending row, dispatch to a `MutationApplier` registry keyed by `tool_name`, mark the pending row `APPLIED`, return the tool's original success payload.
- `POST /api/automation/internal/ai-hub/pending-mutations/{id}/discard` — mark row `DISCARDED`; return 204.

`MutationApplier` is a Spring interface with one impl per tool name. Each impl deserializes `input_json` and calls the pre-existing service (same code the old non-staging path called). Eight impls in v5A (one per Phase 4 mutation callback):
- `UpdateWorkflowMutationApplier`
- `CreateWorkflowMutationApplier`
- `AddDataTableRowMutationApplier`
- `UpdateDataTableRowMutationApplier`
- `DeleteDataTableRowMutationApplier`
- `AddDataTableColumnMutationApplier`
- `AddKnowledgeBaseDocumentMutationApplier`
- `DeleteKnowledgeBaseDocumentMutationApplier`

`CreateAssetFileToolCallback` (Phase 1, used by research + workflow_builder + data_analyst to save reports) is **excluded** from staging — it's additive, low-risk, and staging would break the subagent flows. Documented as an exception.

A third endpoint for listing:
- `GET /api/automation/internal/ai-hub/pending-mutations` — returns `[{id, toolName, diffSummary, createdAt}]` for the current user + workspace. Used by the client to render the pending bar on page load (in case the user had pending changes from a previous session within TTL).

A scheduled task (existing scheduler infra) GC's `EXPIRED` rows daily.

### Client-side flow

One new component + one new store + one subscriber extension.

- `useAiHubPendingMutationsStore` — zustand store with:
  - `pending: Array<{id, toolName, diffSummary, createdAt}>`
  - `addPending`, `removePending`, `clearAll`, `hydrateFromServer`
- `AiHubPendingChangesBar` — renders above the resource panel (or as a banner inside it) when `pending.length > 0`. Shows the count + list of diff summaries + two buttons: **Apply all** (fires each `apply` endpoint in sequence; on success, clears the store) and **Discard all** (fires each `discard` endpoint; clears). Individual items have their own small × to discard one.
- `AiHubRuntimeProvider` subscriber extension — intercept `onToolCallResultEvent` for results containing `{staged: true, pendingId, diffSummary}`. Add to the pending-mutations store. Broaden the existing `openXxxTab` interception logic so it doesn't fire on staged mutation results (they don't open tabs).

On page mount, hydrate the pending store from `GET pending-mutations` so the bar reflects any unresolved mutations from a prior session within the 24h TTL.

### Agent-side prompt

Update `prompt_ai_hub_build.txt`:
- Explain that every mutation is staged, not applied immediately.
- Tell the agent: after each mutation tool call, the user will see a pending-changes bar; the mutation is **not yet applied**. In the assistant's chat reply, acknowledge this explicitly ("I've staged 2 changes. Review and apply.") rather than saying "I've updated the table" which would be misleading.

## Refactor pattern per mutation callback

Each of the 8 Phase 4 callbacks follows this shape today (example `UpdateDataTableRowToolCallback`):

```java
public String call(String toolInput, @Nullable ToolContext toolContext) {
    try {
        var input = objectMapper.readValue(toolInput, Input.class);
        // validate...
        var workspaceId = workspaceContextProvider.currentWorkspaceId();
        // verify scope...
        dataTableRowService.updateRow(input.dataTableId(), input.rowId(), input.values(), environmentId);
        return objectMapper.writeValueAsString(new Output(true, input.rowId()));
    } catch (...) { return toolError(...); }
}
```

After Phase 5A (same example):

```java
public String call(String toolInput, @Nullable ToolContext toolContext) {
    try {
        var input = objectMapper.readValue(toolInput, Input.class);
        // validate... verify scope... (unchanged)
        var diff = computeDiffSummary(input);  // new helper
        long pendingId = mutationStagingService.stage(
            workspaceId, userId, "updateDataTableRow", toolInput, diff);
        return objectMapper.writeValueAsString(new StagedOutput(true, pendingId, diff));
    } catch (...) { return toolError(...); }
}
```

`computeDiffSummary` is callback-specific. For row updates it reads the current row and produces `"row <id>: status 'new' → 'qualified', owner (unchanged)"`. For workflow updates it reads the current definition and shows changed top-level fields. For KB doc add: `"add document 'foo.md' (1245 bytes)"`. For data-table column add: `"add column 'priority' (INTEGER)"`.

The `MutationApplier` for each tool takes the opposite path — reads `input_json`, calls the original service method, returns the original success output shape.

## Testing

### Server

- **Unit test per `MutationApplier`** — dispatch to the real service (mocked) with a canned input_json; asserts the correct service method is called with the correct arguments.
- **`MutationStagingService` test** — persists a row, returns the id; enforces TTL on read.
- **Refactored `ToolCallback` tests** — each of the 8 updated callbacks now asserts that:
  - `.call(...)` returns `{staged: true, pendingId, diffSummary}` on happy path.
  - The target service is **not** called directly.
  - Error cases (invalid input, cross-workspace, etc.) still return error JSON without staging.
- **Endpoint tests** — `@WebMvcTest` or similar for the 3 new REST endpoints (apply, discard, list). Cover: happy path, not-found, cross-user access denied, already-applied row returns 409.
- **Liquibase migration smoke** — add a new changeset, verify it applies cleanly on a PostgreSQL Testcontainer.

### Client

- `useAiHubPendingMutationsStore` tests — 4 actions × basic assertions.
- `AiHubPendingChangesBar` test — renders empty / renders with list / Apply button fires mutation per item / Discard fires per item.
- `AiHubRuntimeProvider` subscriber test — staged result adds to pending store; non-staged result is ignored.
- Hydrate test — on mount, calls GET pending-mutations and populates store.

## Risks and open questions

- **Diff computation cost**. For some mutations (workflow update) computing a diff requires reading the current definition — adds a read to every stage call. Acceptable; still bounded.
- **Sub-mutations inside subagents**. `workflow_builder` eventually calls `createWorkflow` through the parent — that call will now stage. Ensure the parent's system prompt + the subagent's return path both understand this (the parent summarizes "I've drafted a workflow — review and apply").
- **Agent confusion about state**. After a staged mutation, the next turn's listWorkflows won't show the new workflow (it's not applied). This is correct behavior but may confuse the LLM if not prompted clearly. The system-prompt update must emphasize "pending mutations are not yet visible to other read tools."
- **Discard of a staged `addKnowledgeBaseDocument`** — the upload has already moved bytes around server-side? No — the staging step only writes to the pending table with the content. On Apply the file is actually ingested. On Discard nothing to clean up. Confirm by inspecting the existing ingestion path.

## Phase 5A.2 preview (out of scope here, documented for continuity)

- Add `ai_hub_mutation_log` table: applied-mutation history with pre-image snapshots for reversible mutations.
- Undo endpoint: reverses the last N mutations for the user within a configurable TTL (e.g. 30 min).
- Client: a "Recently applied (undo available)" section below the pending bar.

## Phase 5A.3 preview

- Audit viewer: a new internal page `/automation/ai-hub/audit` listing all mutations with filter by user, tool, date range. Read-only.

## Commit convention

`CC5A …` / `CC5A client - …`. Follows the established prefix pattern.
