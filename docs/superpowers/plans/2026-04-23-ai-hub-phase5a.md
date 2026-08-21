# AI Hub Phase 5A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans.

**Goal:** Mutations go through a server-side staging layer; user sees a Pending Changes bar and explicitly Applies or Discards. No undo (Phase 5A.2).

**Architecture:** New `ai_hub_pending_mutation` table, new `MutationStagingService`, new `MutationApplier` interface with one impl per mutation tool, 3 new REST endpoints, refactored Phase 4 callbacks, new client store + pending-bar component + subscriber interception.

**Reference spec:** [docs/superpowers/specs/2026-04-23-ai-hub-phase5a-design.md](../specs/2026-04-23-ai-hub-phase5a-design.md).

**Depends on:** All phases 1-4 and 4.5 merged.

---

## File structure

### Server (EE)

| Action | Path | Responsibility |
|---|---|---|
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/pending/PendingMutation.java` | Immutable model (record or simple class) for the table. |
| Create | `…/pending/PendingMutationStatus.java` | Enum: PENDING / APPLIED / DISCARDED / EXPIRED. |
| Create | `…/pending/MutationStagingService.java` + `Impl` | Stage / load / markApplied / markDiscarded / listForUser / gcExpired. |
| Create | `…/pending/MutationApplier.java` | SPI interface `{ String toolName(); Object apply(PendingMutation pending); }`. |
| Create | `…/pending/MutationApplierRegistry.java` | Maps tool name → applier bean; `apply(pending)` dispatches. |
| Create | One `MutationApplier` impl per tool under `…/pending/applier/` | 8 impls: UpdateWorkflow, CreateWorkflow, AddDataTableRow, UpdateDataTableRow, DeleteDataTableRow, AddDataTableColumn, AddKnowledgeBaseDocument, DeleteKnowledgeBaseDocument. Each wraps the service method the original callback called pre-5A. |
| Create | `…/pending/repository/PendingMutationRepository.java` | Spring Data JDBC repository. |
| Create | Liquibase changeset | Creates the table, indexes, + FKs if appropriate. Register in the existing master changelog. |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/PendingMutationApiController.java` | 3 endpoints: list/apply/discard. |
| Create | `…/pending/PendingMutationExpiryScheduler.java` | Daily GC of EXPIRED rows, `@Scheduled` annotation. |
| Modify | Each Phase 4 mutation callback (8 files) | Replace direct-call with `mutationStagingService.stage(...)` returning `{staged, pendingId, diffSummary}`. Add `computeDiffSummary()` helper. |
| Modify | Each mutation callback's test (8 files) | Assert `.call(...)` stages via mock MutationStagingService; does NOT call the underlying service. |
| Modify | `CopilotConfiguration.aiHubBuildSpringAIAgent` bean method | Inject `MutationStagingService` and pass to each inline callback construction. |
| Modify | `prompt_ai_hub_build.txt` | Add a paragraph explaining that mutations are staged and the user must apply. |
| Modify | `AiHubSpringAIAgent.java` | Optionally: in `createSystemMessage`, add a new `Context` block `"Pending Mutations"` listing any pending ones so the agent knows not to re-stage the same change. |

### Client

| Action | Path | Responsibility |
|---|---|---|
| Create | `client/src/pages/automation/ai-hub/pending/stores/useAiHubPendingMutationsStore.ts` | Zustand store: `pending[]`, add/remove/clear/hydrate. |
| Create | matching `tests/` | Store unit tests. |
| Create | `client/src/pages/automation/ai-hub/pending/AiHubPendingChangesBar.tsx` | Banner above the resource panel with Apply all / Discard all + per-item × . |
| Create | matching `tests/` | Component unit tests. |
| Create | `client/src/pages/automation/ai-hub/pending/api/pendingMutations.api.ts` | Fetch wrappers for the 3 new endpoints + `usePendingMutationsQuery` React Query hook. |
| Modify | `AiHubRuntimeProvider.tsx` subscriber | Intercept staged tool results; add to pending store. |
| Modify | `AiHubResourcePanel.tsx` | Render `<AiHubPendingChangesBar />` above the tab strip. |
| Modify | `AiHubPanel.tsx` | Hydrate pending mutations on mount (via the React Query hook). |

### Commit convention

`CC5A …` / `CC5A client - …`.

---

## Task list

### Task 1: DB table + domain + repository

**Files:** `PendingMutation.java`, `PendingMutationStatus.java`, `PendingMutationRepository.java`, Liquibase changeset, + test for the repository (Spring Data JDBC + Testcontainer Postgres).

**Summary:** Add the new table via Liquibase; create the Spring Data JDBC record/repository. Test: save → find by id → find by user — basic CRUD smoke.

**Commit:** `CC5A Add ai_hub_pending_mutation table + repository`

### Task 2: `MutationStagingService`

**Files:** `MutationStagingService.java` + `Impl`, test.

**Summary:** Methods: `stage(workspaceId, userId, toolName, inputJson, diffSummary) → Long` (returns new row id); `loadPending(id) → PendingMutation` (enforces still-pending + TTL); `markApplied(id)`; `markDiscarded(id)`; `listForUser(workspaceId, userId) → List<PendingMutation>`; `gcExpired() → int`.

TTL is a constant (e.g. 24h); `expires_at` = `created_at + 24h`.

Test each method with a mocked repository.

**Commit:** `CC5A Add MutationStagingService for pending mutations`

### Task 3: `MutationApplier` SPI + registry

**Files:** `MutationApplier.java`, `MutationApplierRegistry.java`, test.

**Summary:** SPI with `toolName()` + `apply(PendingMutation)`. Registry injects `List<MutationApplier>` and maps by tool name; `apply(pending)` dispatches and returns the underlying applier's result. Unknown tool name → `IllegalStateException`.

Test registry with two stub appliers.

**Commit:** `CC5A Add MutationApplier SPI + registry`

### Task 4: 8 `MutationApplier` impls

Create one `@Component` per tool. Each impl:
- Declares `toolName()` returning the matching string (e.g. `"updateDataTableRow"`).
- In `apply(pending)`, deserializes `pending.inputJson()` back to the tool's `Input` record, calls the underlying service method (the one the old ToolCallback called), returns the original success payload.

Group into 3 commits for readability:
- `CC5A Add workflow MutationApplier impls (update + create)`
- `CC5A Add data-table MutationApplier impls (add/update/delete row + add column)`
- `CC5A Add knowledge-base MutationApplier impls (add + delete document)`

Tests per impl: one happy-path + one error (service throws).

### Task 5: REST endpoints

**Files:** `PendingMutationApiController.java` + integration test.

**Endpoints:**
- `GET  /api/automation/internal/ai-hub/pending-mutations` — workspace-scoped, user-scoped list.
- `POST /api/automation/internal/ai-hub/pending-mutations/{id}/apply` — atomic: load, verify current user + workspace, dispatch to registry, mark APPLIED, return original success payload. 409 if already not-pending.
- `POST /api/automation/internal/ai-hub/pending-mutations/{id}/discard` — mark DISCARDED, return 204.

Use existing auth / workspace-scoping patterns (look at any `*-rest` module under `server/ee/libs/` for reference).

**Commit:** `CC5A Add pending-mutations REST endpoints (list/apply/discard)`

### Task 6: Refactor Phase 4 callbacks to stage

Modify each of the 8 mutation callbacks:
- Constructor gains `MutationStagingService`.
- `call(...)` computes `diffSummary` via a small new `computeDiffSummary(Input)` helper (callback-specific), then calls `mutationStagingService.stage(...)`, returns `{staged:true, pendingId, diffSummary}`.
- The diff summary helpers can be simple string concatenations for v5A — no need for a structured diff model yet.

Update each callback's test to:
- Mock `MutationStagingService`.
- Assert `stage(...)` is called with expected args.
- Assert the original service is NOT called.
- Assert the tool returns the staged JSON.

Commit in the same 3 groups as Task 4:
- `CC5A Route workflow mutation callbacks through staging`
- `CC5A Route data-table mutation callbacks through staging`
- `CC5A Route knowledge-base mutation callbacks through staging`

### Task 7: `MutationStagingService` into CopilotConfiguration

**Files:** `CopilotConfiguration.java`.

**Summary:** Inject `MutationStagingService` into `aiHubBuildSpringAIAgent`. Pass it to every inline mutation-callback construction. Compile + existing tests should continue to pass.

**Commit:** `CC5A Thread MutationStagingService into ai_hub BUILD agent`

### Task 8: Expiry scheduler

**Files:** `PendingMutationExpiryScheduler.java` + test.

**Summary:** `@Component @Scheduled(cron = "0 0 3 * * *")` calls `mutationStagingService.gcExpired()`. Simple. Test verifies method invocation on a mock.

**Commit:** `CC5A Add scheduled GC for expired pending mutations`

### Task 9: System prompt update

**Files:** `prompt_ai_hub_build.txt`.

**Summary:** Add a paragraph:
```
IMPORTANT: In BUILD mode, every mutation tool (updateWorkflow, createWorkflow,
add/update/deleteDataTableRow, addDataTableColumn, add/deleteKnowledgeBaseDocument)
is STAGED, not applied immediately. Tool results include {staged: true,
pendingId, diffSummary}. The user sees a Pending Changes bar and must click
Apply before the mutation takes effect.

In chat replies, acknowledge this explicitly: say "I've staged <N> changes
— review and apply in the Pending Changes bar" rather than "I've updated X"
(which would be misleading until applied).

Do not re-stage the same change repeatedly. If the user has already staged
a mutation, check the State's Pending Mutations context block before
restaging.
```

**Commit:** `CC5A Update BUILD system prompt for staged mutations`

### Task 10: Client — pending-mutations store

**Files:** `useAiHubPendingMutationsStore.ts` + tests.

**Summary:** Store shape:
```ts
interface PendingMutationI {
    createdAt: string;
    diffSummary: string;
    id: number;
    toolName: string;
}

interface AiHubPendingMutationsStateI {
    addPending: (mutation: PendingMutationI) => void;
    clearAll: () => void;
    hydrateFromServer: (mutations: PendingMutationI[]) => void;
    pending: PendingMutationI[];
    removePending: (id: number) => void;
}
```

Tests: 5 cases covering each action.

**Commit:** `CC5A client - Add pending mutations store`

### Task 11: Client — pendingMutations.api.ts + React Query hook

**Files:** `pending/api/pendingMutations.api.ts` (wraps the 3 REST endpoints) + a `usePendingMutationsQuery` hook.

**Summary:** Typed fetch calls with `credentials: 'include'`, CSRF token (see Phase 1 `AiHubRuntimeProvider` for the cookie pattern).

**Commit:** `CC5A client - Add pending mutations API wrappers + query hook`

### Task 12: Runtime provider subscriber interception

**Files:** `AiHubRuntimeProvider.tsx` + tests.

**Summary:** In `buildAiHubSubscriber.onToolCallResultEvent`, after parsing the JSON result, check for `{staged: true, pendingId, diffSummary}`. If present, call `aiHubPendingMutationsStore.getState().addPending({id, toolName, diffSummary, createdAt: now})`. Keep existing `openXxxTab` interception untouched.

Add 2 tests: staged result populates store; non-staged result still triggers other interceptions.

**Commit:** `CC5A client - Intercept staged tool results and populate pending store`

### Task 13: `AiHubPendingChangesBar`

**Files:** `AiHubPendingChangesBar.tsx` + tests.

**Summary:** Banner rendered above the tab strip when `pending.length > 0`. Shows count + a vertical list of diff summaries with per-item × (calls discard endpoint + `removePending`). Two buttons: **Apply all** (sequentially calls apply for each, removes each on success, clears on completion; one failure stops the sequence and toasts) and **Discard all** (same pattern).

Use ShadCN `Alert` or a simple `div` with clear affordance.

Tests: renders empty (null) / renders with list / Apply fires all apply endpoints / Discard fires all discard endpoints.

**Commit:** `CC5A client - Add Pending Changes bar with Apply all / Discard all`

### Task 14: Wire bar + hydration

**Files:** `AiHubResourcePanel.tsx`, `AiHubPanel.tsx`.

**Summary:**
- Render `<AiHubPendingChangesBar />` in the resource panel's header area (above the tab strip).
- In the `AiHub.tsx` route (or `AiHubPanel.tsx`), on mount call `usePendingMutationsQuery` and hydrate the store from the response.

**Commit:** `CC5A client - Hydrate pending store on mount; render bar in resource panel`

### Task 15: Full server + client check + manual verification

- `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test`
- `cd client && npm run check`
- Manual: log in, open AI Hub, ask agent "add a row to leads: name=Ada, status=qualified" → verify row is NOT added but pending bar shows 1 change → click Apply → row appears → bar empties.
- Verify Discard path.
- Verify hydration: reload page with a pending mutation, bar is populated.

**Commit** (if any formatting fixes): `CC5A Final formatting + lint fixes`.

---

## Out of scope (deferred)

- **Phase 5A.2**: Undo within TTL, mutation log, audit viewer.
- **Phase 5A.3**: Full audit-trail page with filters.
- **Multi-user conflict resolution** on apply.
- **Partial apply** (subset of pending).

## Risks (called out in spec)

- Agent may re-stage the same mutation. Mitigation: system prompt instruction + agent reads a "Pending Mutations" context block (enhancement; may defer).
- Subagent-produced staged mutations (workflow_builder triggers createWorkflow) need careful wording in parent chat so user knows the workflow is pending. Mitigation: prompt update in Task 9.
- Diff summaries require reads; the existing per-callback service usage already does reads for scope checks, so incremental cost is small.
