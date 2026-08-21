# Return values for the condition, branch and fork/join task dispatchers

**Date:** 2026-07-31
**Status:** Implemented
**Scope:** CE — `task-dispatchers/{condition,branch,fork-join}`, `platform-workflow-task-dispatcher-api`, `platform-configuration-service` (editor output resolution).

## Problem

The map task dispatcher returns a value (a `List<Object>` of each item's last-iteratee-task
output), so downstream tasks can reference `${mapNodeName}`. The condition, branch, and
fork/join dispatchers return nothing: their completion handlers never call
`taskExecution.setOutput(...)`, their definition factories declare no `.output(...)`, and the
editor shows no output entry for them. A workflow that branches has no way to hand the chosen
branch's result to the next task without an extra explicit variable step.

## Design

### Runtime semantics

- **Condition** and **branch**: the dispatcher task's output is the output of the **last task
  in the executed case's sequence**. The completion handler already detects sequence
  exhaustion (`taskExecution.getTaskNumber() >= subWorkflowTasks.size()`); at that point it
  copies the completed child's output onto the parent
  (`storeTaskExecutionOutput(jobId, parentId, readTaskExecutionOutput(child.getOutput()))`)
  before handing the parent to the outer `taskCompletionHandler` — the exact point where map
  writes its aggregate today.
- **Fork/join**: the output is a **`Map<String, Object>` keyed `branch_0`, `branch_1`, …** in
  branch declaration order, each value being that branch's last task's output. **Decision
  (user, 2026-07-31): synthetic index keys only** — the `branches` DSL stays the current
  array-of-task-arrays; no name field is added. Reordering branches therefore changes the
  keys; documented, accepted. Aggregation mirrors map's read-modify-write of the parent's
  stored output at each branch's last-node completion (the existing `CounterService`
  countdown already serializes join detection; the map is written under the same
  file-storage pattern map uses for its list, with absent branches simply missing from the
  map until they complete).

### Edge semantics

- **Empty case** (zero tasks): output stays unset (`null`) — the existing fast-path
  completion is unchanged. This applies to condition/branch only: fork/join REJECTS empty
  branches at dispatch (pre-existing `Validate.isTrue`), so a `branch_i: null` entry can
  only arise from a branch whose last task produces no output, never from an empty branch.
- **Branch's literal-`value` case shape**: when the selected case carries `value` instead of
  `tasks`, the existing behavior (output = the literal) is already what this spec wants —
  unchanged, now documented as part of the contract instead of an oddity.
- **No matching case**: condition's `caseFalse` and branch's `default` are ordinary cases —
  "no match" is not a distinct state; the output is the fallback case's last-task output.
- **Failure**: a failed task fails the job through `ErrorHandlingTaskDispatcher` before the
  dispatcher completes — no partial output contract. Whatever partial fork/join map was
  written before the failure remains on the task execution for observability only.
- **Nested dispatchers as last node**: the child dispatcher's own output (this feature, or
  map's list) propagates — outputs compose.

### Editor / design-time output

Each factory gains `.output(...)` via a shared SPI mirroring `MapDataSource`
(`getLastIterateeTaskOutput`): a `TaskListOutputDataSource` that resolves the declared output
of the last task of a given task list through the same fallback chain
(`WorkflowNodeTestOutput` sample → static output → live computation).

- **Condition**: previews the `caseTrue` sequence's last-task schema (documented
  approximation — design time cannot know which case runs).
- **Branch**: previews the first case's last-task schema; the `default` case when no cases.
- **Fork/join**: `object()` with one property per branch, `branch_i` typed by that branch's
  last-task schema.
- `WorkflowNodeOutputFacadeImpl`'s dispatcher special-case list gains the three types with
  the same self-reference exclusion loop/each/map use (a task nested inside a branch must not
  autocomplete its own enclosing dispatcher's aggregate).
- Definition snapshot JSONs (`condition_v1.json`, `branch_v1.json`, `fork-join_v1.json`)
  regenerate per the documented delete-both-copies workflow.

### Backward compatibility

Purely additive at the storage layer — nothing reads these outputs today, and every
completion handler already null-guards absent outputs. The one visible behavior change:
`${conditionNode}`-style expressions that previously resolved to `null` now resolve to real
values. No DSL change, no migration.

### Out of scope

- `loop` / `each` outputs (deliberately output-less iteration; unchanged).
- Named fork/join branches (rejected for now — would be an additive DSL evolution
  `{name, tasks}` if ever wanted; the synthetic keys don't preclude it).
- Sync vs distributed: no special handling needed — everything goes through
  `TaskFileStorage`/`CounterService`/`ContextService`, which are already distribution-safe.

## Testing

- Completion-handler unit tests per dispatcher: last-node output lands on the parent;
  empty-case null; fork/join map keys + out-of-order branch completion.
- Extend the existing `ConditionTaskDispatcherIntTest` / `ForkJoinTaskDispatcherIntTest`
  (and branch's equivalent) to assert `${dispatcherName}` resolves downstream.
- Snapshot tests pin the new `outputDefinition` in the three definition JSONs.

## Decisions log

- Fork/join keys: **synthetic `branch_0..N` only**, no DSL change (user, 2026-07-31).
- Condition/branch runtime output: last executed node's output; empty → null.
- Branch literal-`value` path: kept as-is, folded into the documented contract.
- Design-time previews approximate (caseTrue / first case) — inherent to branching.
- **Fork/join null-output branch** (implementation-forced, T3): a branch that completes with
  no output stores its `branch_i` key with a `null` value rather than omitting the key —
  matches map's null-tolerant write path (Jackson preserves null map entries; no `NON_NULL`
  inclusion policy anywhere in `server/libs`). Pinned via a raw-map read (`containsKey`),
  since SpEL cannot distinguish "key present with null" from "key absent."
- **Design-time SPI constants alias `MapDataSource`'s injection keys** (implementation-forced,
  T4): the new `TaskListOutputDataSource` SPI's `WORKFLOW_ID`/`ENVIRONMENT_ID`-style constants
  resolve to the literal strings `"__workflowId"`/`"__environmentId"` — the same literals
  `MapDataSource` already writes into the injection map. Bare/new names would never receive a
  value, since `WorkflowNodeOutputFacadeImpl` injects under the existing literals for every
  dispatcher type. `MapDataSourceImpl` now delegates one-line to the shared fallback chain,
  which moved into `TaskListOutputDataSourceImpl` for DRY.
- **`MapTaskDispatcherIntTest` composition pin updated** (implementation-forced, T1 fallout):
  `testDispatch3`'s "nested condition yields no output" assertion no longer holds once
  condition returns a value — the pin was updated to the composed list, with the reviewer
  verifying the exact per-item value (items `>5` → the `"-2"` branch's output, else `"+2"`),
  confirming condition's new output composes correctly as a map iteratee's last task.
- **Fork/join design-time null-schema branch** (implementation-forced, T5): a branch whose
  last-task schema resolves but is itself `null` renders as an untyped object property (no
  `outputSchema` on that `branch_i` entry) rather than omitting the property or propagating a
  null schema onto it — a named object property cannot carry a `null` schema value.
