# Workflow Node Disable/Enable — Design

**Date:** 2026-08-24
**Status:** Approved design, pre-implementation

## Overview

Let users disable individual nodes in a workflow from the editor. Disabled nodes are
skipped during execution: the engine never sees them, downstream references to their
outputs resolve to `null`, and the editor renders them greyed out with an explicit
toggle to re-enable.

## Goals

- Toggle any task node on the canvas: top-level tasks AND tasks nested inside
  condition / loop / branch / each / parallel / fork-join / map dispatchers.
- Skipped nodes leave no trace in execution history ("no trace" semantics); the
  workflow definition itself records why a node did not run.
- Downstream expressions referencing a disabled node resolve to `null` rather than
  failing the workflow.
- Zero impact on existing workflows: absent flag = enabled; no DB migration.

## Non-goals (v1)

- Disabling triggers (overlaps with enabling/disabling the deployed workflow).
- Disabling cluster elements (AI agent tools).
- Blocking validation — reference warnings are advisory only.
- A `SKIPPED` task execution status / audit trail of skipped nodes.

## 1. Definition format & data model

A new optional top-level key on any task object in the workflow definition:

```json
{
    "label": "Send Email",
    "name": "sendEmail_1",
    "type": "google-mail/v1/sendEmail",
    "disabled": true,
    "parameters": {}
}
```

- Absent means enabled. Only `"disabled": true` is ever serialized: the client omits
  the key when re-enabling, and `@JsonInclude(NON_EMPTY)` on `WorkflowTask` drops
  `false` server-side. Existing workflows stay byte-identical.
- `WorkflowTask` gains a `boolean disabled` field parsed in its map constructor via a
  new `WorkflowConstants.DISABLED` key (today the key would fall into the catch-all
  `extensions` map), an `isDisabled()` getter, and `toMap()` inclusion only when
  `true` — the same promotion pattern as `label` / `timeout`.
- Disabling a dispatcher node implicitly skips its entire subtree, because the
  dispatcher itself never runs.
- The flag lives inside the stored workflow definition JSON — no schema/DB change.

## 2. Engine skip semantics

**One generic transform at the engine boundary** — no per-dispatcher changes for how
dispatchers READ their task lists.

> **Amendment:** that claim does not extend to list ARITY. The strip can EMPTY a
> nested task list a dispatcher assumes is non-empty, so dispatchers that index into
> their subtask list need a tolerance guard. See §4.

New utilities in `atlas-configuration`, next to `WorkflowTaskUtils.getTasks` and
reusing its structural detection of nested task lists (a parameter value that is a
list of task-shaped maps, branch `cases[].tasks`, fork/join list-of-lists):

- `WorkflowTaskUtils.removeDisabledTasks(List<WorkflowTask>)` — returns the list
  with disabled tasks removed recursively at all depths; surviving tasks' parameter
  maps are rebuilt with disabled entries removed from nested lists.
- `WorkflowTaskUtils.getDisabledTaskNames(List<WorkflowTask>)` — names of everything
  removed, recursively; a disabled dispatcher contributes its own name and all
  descendant names.

**Application points.** The coordinator applies the strip where it materializes the
workflow for execution: `JobExecutor.execute` (initial + each advance) and
`DefaultTaskCompletionHandler.doHandle` (the `currentTask < tasks.size()` completion
check). The strip is applied at those call sites, NOT inside `WorkflowService` —
the same service feeds the editor, which must keep seeing disabled tasks. The sync
path (`JobSyncExecutor`) reuses these beans and is covered for free.

Consequences:

- `job.currentTask` indexes into the filtered list.
- Dispatchers iterate already-clean nested lists — zero dispatcher changes.
- Executions contain no `TaskExecution` rows for skipped nodes.

**Null resolution.** At job start, the initial job context is seeded with
`name → null` for every disabled task name (including descendants of disabled
dispatchers). `${disabledTask}` resolves to null downstream.

> **Resolved — the documented fallback, after a reverted attempt.** A
> `NullSafePropertyAccessor` (a generic `PropertyAccessor` reading any property of a
> `null` target as `TypedValue.NULL`) was implemented and registered in
> `SpelEvaluator.createEvaluationContext`, then **reverted at the final gate**. A
> context key holding null is indistinguishable from a task that has simply not
> produced its value *yet*: `DefaultTaskCompletionHandler.doHandle` puts
> `name -> null` for any completed task with no output, and the dispatchers rely on
> an unresolvable expression being left as a raw string so it re-resolves per
> iteration. Null-resolving that case broke deferred per-iteration resolution in
> nested loops — `LoopTaskDispatcherIntTest.testDispatch2` produced `2_null, 3_null,
> …` instead of `2_1, 3_1, …`. Making the accessor smarter (a sentinel value, or
> threading the disabled-name set through the shared `Evaluator`) was rejected as
> too invasive for a global evaluator change at the last gate.
>
> **The contract, therefore:** `${disabledTask}` resolves to **null**;
> `${disabledTask.field}` passes through as the **original expression string**
> (SpEL throws `SpelEvaluationException`, which `SpelEvaluator` swallows and returns
> the raw value unchanged). That is exactly what users already see today for any
> reference to a task that produced no output, so it introduces no new behaviour.
> The editor warns about references to disabled nodes at authoring time (§3,
> "Reference warnings"), so this is visible before the workflow is ever run.

**Mid-flight consistency.** Published workflow definitions are versioned per
deployment, and test runs re-read the definition each run — no extra handling for
toggling while a job is in flight.

## 3. Editor UX

**Toggle.** A "Disable" / "Enable" item in the node's existing three-dots popover
menu (alongside Delete/Duplicate), on top-level and nested task nodes alike.
Toggling writes/removes `disabled: true` in that task's object and saves via the
existing whole-definition update mutation (same persistence pattern as sticky
notes / node positions, but at the task's top level, not `metadata.ui`).

**Visuals.**

- Disabled node: reduced opacity, muted icon, small "Disabled" indicator (ban /
  eye-off glyph in the node header). Edges still render — graph shape is unchanged.
- Descendants of a disabled dispatcher render with the same muted treatment,
  DERIVED from the ancestor flag at render time; nothing is written into their
  JSON, so re-enabling the parent instantly restores them.

**Reference warnings.** Nodes whose parameters reference a disabled task's name
(string scan for `${disabledName...}` across parameter values) get a warning badge
with tooltip: "References disabled node x — it will not run, so this value will not
resolve" (plural form: "References disabled nodes x, y — they will not run, so this
value will not resolve"). The tooltip names the cause rather than a runtime outcome,
because the §2 contract is split: a bare `${x}` resolves to null, while `${x.field}`
passes through as the original expression string. Advisory only; saving and running
are never blocked.

**Interaction details.**

- Disabling does not touch selection, the properties panel, or connections; a
  disabled node stays fully editable and individually testable (single-node test
  runs bypass the job path).
- The execution-detail read-only canvas reuses the same node components, so a
  disabled-at-that-version node shows greyed there — the "why didn't this run"
  explanation despite no-trace executions.

> **Deviation (sanctioned):** the execution-detail canvas does not actually reuse the
> editor's node components — it renders `ReadOnlyNode`/`ReadOnlyPlaceholderNode`
> (see `toReadOnlyLayoutEdges` in `useLayout.tsx`). The muted treatment was extended
> there deliberately: `useLayout.tsx` stamps `isEffectivelyDisabled` (own flag or a
> disabled ancestor via `getEffectivelyDisabledTaskNames`) onto each read-only node's
> data, and `ReadOnlyNode.tsx` applies the same `opacity-50 grayscale` classes and
> `DisabledNodeBadge` as the editor's `WorkflowNode`.

## 4. Edge cases

- **All tasks disabled:** filtered list is empty; the job completes immediately as
  successful with no task executions. `JobExecutor.execute` currently throws
  "No tasks to execute!" on an empty list — that path gets a graceful
  complete-empty-job branch.
- **First task disabled:** filtering shifts indexes; no special handling.
- **Emptied subtask list (invariant):** disabling every task inside a dispatcher's
  subtask list empties that list rather than removing it. Any task dispatcher that
  indexes into its subtask list (`getFirst()`, `Validate.isTrue(!isEmpty())`, or a
  counter sized from branch count) must tolerate an empty list and route to its
  empty-completion path. Verified as already handling it: condition, branch,
  parallel, on-error, graph. Needed guards: each, loop, map and fork-join — the
  latter three fixed in commit `05dc4929c96`.
- **Disabled task inside `finalize` / `pre` / `post`:** same recursive strip applies
  (they are `WorkflowTask` lists too).
- **Duplicating a disabled node:** copies the flag — it is part of the task map.
- **Copilot/AI-generated workflows:** may emit the flag; no special handling — it is
  part of the task schema.

## 5. Testing

- **Server unit (`WorkflowTaskUtilsTest`):** recursive removal across condition /
  branch / loop / fork-join shapes; disabled-name collection includes descendants;
  `WorkflowTask` map ⇄ object round-trip keeps/omits the flag correctly.
- **Server engine test:** 3-task workflow, middle task disabled — exactly 2 task
  executions, job completes, a bare downstream reference to the disabled name
  evaluates to null and a property path off it passes through as the original
  expression string (pins both halves of the §2 contract).
- **All-disabled engine test:** job completes successfully with zero task executions.
- **Client:** toggle menu item writes/removes `disabled`; muted rendering including
  derived ancestor-disabled state; reference-warning scan utility.
