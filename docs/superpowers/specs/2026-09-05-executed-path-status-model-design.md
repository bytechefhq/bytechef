# Workflow Editor — One Executed-Path Status Model

**Date:** 2026-09-05
**Status:** Proposed
**Scope:** Client only, `client/src/pages/platform/workflow-editor/`. Pure refactor — no pixel changes.

## Problem

After a test run the canvas paints the executed path: edges and task dispatcher ghost bars turn green
or red to match the nodes they connect. The question "is this edge on the executed path?" is currently
answered in three places:

- `edges/getExecutedEdgeStatus.ts` — the node-state rule, for every ordinary edge.
- `getExecutedGraphTransitionStatus` in the same file — the routing rule, for `graph/v1` transitions.
- `nodes/resolveGhostBarSideStatuses.ts` — per ghost bar half, by calling the first rule again per side edge.

Alongside them sits a fourth rule, `resolveTestStateNodeName`, which makes a structural ghost node borrow
its owning dispatcher's status so the dispatcher's colour flows through its plumbing.

Those rules can disagree, and did. A condition whose nested child fails is itself reported `FAILED`; its
ghosts borrowed that `FAILED`; but the node-state rule required a source of exactly `COMPLETED`. Every edge
leaving a failed dispatcher therefore went gray, breaking the red trail between two red nodes
(`condition_1 -> top ghost -> condition_2`). The fix was one character of logic in one of the three places.

The important part is why no test caught it: **every unit test passed**. Each resolver is individually
correct. The defect lived in their composition, and there is no seam at which the composition can be
asserted — the only place the three meet is inside mounted React Flow components.

There is a secondary cost. Per streamed node event the canvas does `O(E·N + G·(N+E))` work: each edge
component runs two `nodes.find` scans, and each ghost bar rebuilds a full node `Map` plus two `edges.filter`
passes. `LabeledBranchCaseEdge` runs four `nodes.find` calls per render — two for geometry and two more
inside the status hook.

## Goals

- Exactly one place answers "is this edge on the executed path?", for every edge type.
- That place is a pure function taking the whole canvas, so composition is directly testable.
- Ghost bar halves read the same answer rather than re-deriving it.
- Work per streamed node event drops to a single `O(N + E)` pass.
- **Identical rendered output.** Every colour on the canvas before and after must match.

## Non-goals

- Node border and badge colouring (`WorkflowNode`) stays as it is, reading node states directly.
- The SVG-stroke / DOM-box seams where edges butt against ghost bars. Separate problem, separate fix.
- Removing ghost placeholder nodes. They remain load-bearing for layout ranking and join semantics.
- The cluster element editor, which has its own `edgeTypes` and does not paint executed status.
- Any change to the rules themselves. The known graph-transition ambiguity (two transitions sharing a
  `(from, to)` pair are indistinguishable) is carried forward unchanged.

## Design

### The pure function

`edges/computeExecutedPathStatuses.ts`:

```ts
export interface ExecutedPathStatusesI {
    edgeStatuses: ReadonlyMap<string, ExecutedStatusType>;
    ghostBarSideStatuses: ReadonlyMap<string, GhostBarSideStatusesI>;
}

export default function computeExecutedPathStatuses({
    edges,
    nodes,
    workflowTestExecution,
    workflowTestNodeStates,
}: ComputeExecutedPathStatusesProps): ExecutedPathStatusesI;
```

`ExecutedStatusType` is `'COMPLETED' | 'FAILED'`. An edge absent from `edgeStatuses` is neutral — the map
carries only decided statuses, so consumers keep their existing "undefined means gray" reading.

The pass, in order:

1. Index nodes by id once (`Map<string, Node>`).
2. Collect graph dispatch history **once per graph**, not once per transition edge: gather the distinct
   `data.graphId` values across `graphTransition` edges and call `collectGraphNodeExecutions` for each.
   Today every transition edge memoizes its own copy of the same walk.
3. Walk `edges` once. For each edge:
   - A `graphTransition` edge (carrying `data.graphId` and `data.to`) is decided by
     `getExecutedGraphTransitionStatus` against that graph's cached dispatch list. Its identity comes from
     `edge.source` plus `edge.data` — the component no longer has to assemble and hand it in.
   - Every other edge is decided by the existing node-state rule against the node index.
   - While iterating, bucket the edge's decided status by ghost handle id: `sourceHandle` for a top ghost,
     `targetHandle` for a bottom ghost. Handle ids are already `${ghostNodeId}-${side}`, so this is a map
     insert, not a search.
4. Walk the ghost nodes once. For each side, reduce its bucket — `FAILED` wins over `COMPLETED`, matching
   today's `resolveGhostBarSideStatuses`. A side with no edges at all falls back to the owning dispatcher's
   own status, read via `data.taskDispatcherId`, exactly as `useTaskDispatcherGhostStatus` does now.

Steps 3 and 4 are each linear, so the whole pass is `O(N + E)`.

The two existing rules — the node-state rule and the graph-transition rule — are **not rewritten**. They
move behind this pass unchanged and keep their current unit tests. Only their callers change.

### Where the shared result lives

A React context, `edges/ExecutedPathStatusesContext.tsx`, providing the computed object, with a `useMemo`
keyed on `nodes`, `edges`, `workflowTestNodeStates` and `workflowTestExecution`.

The provider goes in `components/WorkflowEditor.tsx`, wrapping `<ReactFlow>`. That one site covers every
canvas that paints executed status today:

- the authoring editor, via `WorkflowEditorLayout`;
- the embedded builder, via `EmbeddableWorkflowEditor` → `WorkflowEditorLayout`;
- the read-only sheet (`ReadOnlyWorkflowSheet`) and both execution sheets, which mount the same
  `WorkflowEditor` component with `readOnlyWorkflow` set.

`ClusterElementsWorkflowEditor` is the only other `<ReactFlow>` on the client and consumes no executed
status, so it needs no provider.

Context rather than a module-level cache, deliberately: no global mutable state to reset between tests, no
cache thrash if two canvases are ever mounted at once, and the "computed once per render pass" property
falls out of React rather than out of a hand-written identity comparison. The context default is an empty
result, so any component rendered outside a provider degrades to neutral rather than throwing.

This adds no re-renders. `WorkflowEditor` already subscribes to `nodes` and `edges`, so it already
re-renders on exactly the changes that invalidate the memo.

### Consumers

| File | Change |
|---|---|
| `edges/useExecutedEdgeStatus.ts` | Rewritten: reads the context map by edge id. Loses its `graphTransition` parameter — the model derives transition identity itself. |
| `edges/WorkflowEdge.tsx` | Swaps its direct `getExecutedEdgeStatus(...)` call for `useExecutedEdgeStatus(id)`. |
| `edges/RoundedSmoothStepEdge.tsx`, `edges/LabeledBranchCaseEdge.tsx` | No call-site change; the hook's implementation changes underneath them. |
| `edges/GraphTransitionEdge.tsx` | Drops the second argument it assembles today. |
| `nodes/useTaskDispatcherGhostBarStatuses.ts` | Rewritten: reads the context map by ghost node id. Its `data` and `isBottomGhost` arguments become unnecessary — the model already knows both. |
| `nodes/resolveGhostBarSideStatuses.ts` | Deleted; folded into the pass. |
| `nodes/useTaskDispatcherGhostStatus.ts` | Kept — `TaskDispatcherLeftGhostNode` (map, forkJoin) still uses it directly. |

Presentation rules stay in components. `TaskDispatcherTopGhostNode`'s `isGraphBar` check, which drops the
ink on a graph's bar, is a rendering decision and does not move into the model.

## Testing

The composition test is the point of the exercise, and it is written **first, against the current code**,
so it is a genuine safety net rather than a description of the new implementation:

- `computeExecutedPathStatuses.test.ts` builds whole-canvas fixtures — nodes, edges and node states
  together — and asserts the complete status map. Fixtures to cover:
  - a condition with a failed nested condition in its TRUE branch (today's regression), asserting the
    dispatcher's own edge, the ghost's edge into the child, and both bar halves;
  - a condition with one branch taken and one untaken, asserting the untaken side stays neutral;
  - a completed child followed by a failed sibling in the same branch;
  - a `graph/v1` conditional fan-out, asserting untaken sibling transitions stay neutral;
  - a nested dispatcher joining its parent's bottom ghost.
- `getExecutedEdgeStatus.test.ts` stays as is — the rules are unchanged.
- `resolveGhostBarSideStatuses.test.ts` is retired, its cases absorbed into the fixtures above.
- `TaskDispatcherGhostBarStatus.test.tsx` and any other mounted-component test gain a provider wrapper.

Before/after equivalence is established by running the new fixture suite against the current
implementation, then against the new one, with no expectation edits in between.

## Sequencing

Small commits, each independently green:

1. Add `computeExecutedPathStatuses.test.ts` fixtures driving the **current** three resolvers. Safety net.
2. Add `computeExecutedPathStatuses.ts`; point the fixtures at it. Not yet wired to any component.
3. Add the context and provider; wire into `WorkflowEditor`.
4. Migrate the edge consumers.
5. Migrate the ghost bars; delete `resolveGhostBarSideStatuses.ts`.
6. Drop the now-unused `graphTransition` parameter from `useExecutedEdgeStatus`.

## Risks

- **A consumer left outside the provider silently loses colour.** The provider site was verified to cover
  all five canvases above, and the context default is neutral, so the failure mode is missing colour rather
  than a crash. Step 3 should be checked against the execution sheets specifically, since those are the
  surfaces where run colour matters most and are easiest to forget.
- **Behavioural drift during the move.** Mitigated by step 1: the fixtures must pass unchanged across the
  whole sequence.
- **Ghost fallback semantics.** `resolveGhostBarSideStatuses` returns the dispatcher's own status only when
  a side has *no* edges. Preserving "no edges" versus "edges that all resolved neutral" is the one place the
  single pass could quietly differ; the untaken-branch fixture pins it.
