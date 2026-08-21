# Graph dispatcher: free-form canvas and edge-list DSL

- **Date**: 2026-08-17
- **Status**: Approved design; plans: `docs/superpowers/plans/2026-08-17-graph-dispatcher-edge-list-dsl.md`
  (server, Plan A) and `docs/superpowers/plans/2026-08-17-graph-dispatcher-freeform-canvas.md` (client, Plan B)
- **Ticket**: 0_732 (user request: "inside the box I will be able to freely add nodes and do
  connections between them so I can define and see a real graph; as nodes move inside the graph the
  box changes its size and the whole flow reshapes")
- **Supersedes**: the rendering half of `2026-08-02-graph-task-dispatcher-design.md` (phases 2–4)
  and all of `2026-08-06-graph-dispatcher-topological-lane-ordering-design.md`. The engine half of
  the 2026-08-02 spec (container-not-goto, budget, accumulated context, `__node` stamp, nothing under
  `server/libs/atlas/`) stands.

## Problem

`graph/v1` renders one vertical **lane** per declared node, ordered by transition topology, with
transitions as an ELK-only paint-time overlay of dashed arcs. Two rounds of tuning (topological lane
order, arc band routing) left it awkward: a state machine is being forced into a fork/join column
idiom, transitions can only be *read* (derived from string literals inside `next` expressions, never
authored), and the "standard" (dagre) engine shows no transitions at all. The 2026-08-06 spec recorded
"free-form state-machine rendering" as *"a rewrite, not an increment — revisit if topological ordering
proves insufficient."* It has.

`graph/v1` is **unreleased** (absent from `master` and from every release tag up to `v1.1.5`; the docs
mark it *coming soon*), so its shape can be replaced outright without a migration.

## Decisions (user, 2026-08-17)

1. The graph container becomes a **free-form canvas inside a box**: nodes are placed and connected by
   dragging; the box grows/shrinks to fit; the surrounding flow reflows around it.
2. **No task list per node.** A graph node *is* one task.
3. **The DSL changes**: `nodes` becomes a plain task list and transitions become an explicit edge list
   with per-edge conditions (approach 1 below). Conditional edges evaluate in declared order, then the
   unconditional default.
4. Execution stays on Atlas through the existing `task-dispatchers/graph` module; the engine is untouched.

## DSL

```yaml
- name: triage
  type: graph/v1
  parameters:
    startNode: classify            # optional; defaults to the first declared node
    maxTransitions: 100            # optional; default 100
    nodes:                         # a PLAIN task list — every entry is an ordinary task
      - name: classify
        type: aiAgent/v1/chat
        parameters: { ... }
        metadata: { ui: { nodePosition: { x: 40, y: 60 } } }   # box-relative; see "Positions"
      - name: review
        type: ...
      - name: approve
        type: ...
    transitions:                   # the edges drawn on the canvas
      - from: classify
        to: approve
        condition: "=${classify.score} > 0.8"
      - from: classify
        to: review                 # no condition → the node's default transition
      - from: review
        to: classify
        condition: "=${review.verdict} == 'REDO'"
      - from: review
        to: approve
```

- **`nodes`** — an ordinary task list, structurally identical to `caseTrue`. A node may be any task,
  including any dispatcher (a Loop node draws its own frame inside the box; a graph inside a graph
  is legal). Node identity is the task **name**, which is unique workflow-wide, so it doubles as the
  transition vocabulary with no separate state name.
- **`transitions[]`** — `{from, to, condition?}`.
  - `from`: a node name.
  - `to`: a node name (**static** edge) **or** an expression resolving to one (`=${classify.nextNode}`)
    — a **dynamic** edge, the LLM-routing shape. Resolution reuses today's `next` semantics
    (`GraphTaskUtils.resolveNext`): blank/null result → no transition from this edge; a resolved name
    that matches no node fails the run naming the source node and value.
  - `condition`: optional formula-mode expression evaluated against the accumulated context; blank or
    absent means unconditional.
- **Evaluation on node completion**: take the node's outgoing transitions; evaluate the **conditional**
  ones in declared order, first truthy `condition` wins; if none matched take the first
  **unconditional** one; if there is none the node is **terminal**. A dynamic `to` that resolves blank
  counts as "did not match" and evaluation continues to the next candidate. The editor warns when a
  node has more than one unconditional transition (the runtime takes the first declared).
- **Start**: `startNode` if set, else the first declared node.
- **Removed concepts**: per-node `tasks[]`, per-node `next`, router (empty) nodes — conditions live on
  edges now, and a pure decision point is expressed as edges out of the preceding node. An LLM router
  is an AI Agent node with one dynamic edge.
- **Unchanged**: cycles are legal and bounded by `maxTransitions` (`CounterService` countdown, exactly
  N transitions permitted); accumulated last-write-wins context; `${graphName}` = the terminal node's
  output (null if it produced none); a failed node fails the job through `ErrorHandlingTaskDispatcher`;
  all state is data so crash-resume/suspend/distributed need no graph-specific code.

### Design-time output (editor)

The definition factory's `.output(...)` previews the schema of the **first declared node with no
outgoing transitions** (statically: no `transitions[].from == name`); if every node has an outgoing
edge (a graph that only ends via conditions all failing) it returns null. Same "documented
approximation" spirit as before and as condition's `caseTrue`.

## Server changes (`task-dispatchers/graph` only)

- `GraphTaskDispatcherDefinitionFactory`: `taskProperties(array(NODES).items(task()))`;
  `properties(...)` gains `array(TRANSITIONS).items(object().properties(string(FROM).required(),
  string(TO).required(), string(CONDITION).controlType(FORMULA_MODE)))`; `startNode` /
  `maxTransitions` unchanged; output rule above.
- `GraphTaskDispatcherConfiguration`: `DeferredEvaluationParameterKeys.register(GRAPH + "/", NODES,
  TRANSITIONS)` — transitions must not be evaluated at graph dispatch (their expressions reference
  outputs that do not exist yet); the handler evaluates them at transition time. The IntTest harness
  (`DeferredEvaluationParameterKeysLoader` / direct registration) mirrors this.
- `GraphTaskDispatcher.doDispatch`: validate unique node names, resolve the start node, seed the
  budget, dispatch the start node's task stamped `__node = <name>` (`GraphTaskUtils.dispatchNodeTask`
  with `taskNumber = 1`; the taskNumber advance loop goes away). The router-hand-off path
  (`__routerNode`, `dispatchRouterNode`, the `setHandled(true)` opt-out) is deleted with the router
  concept.
- `GraphTaskCompletionHandler`: `canHandle` = parent is `graph/v1` and parameters carry `__node`;
  `handle` pushes the completed node's output to context and runs the evaluation rule above; on a
  target, decrement budget and dispatch that node's task; on terminal, copy the completed node's
  output onto the graph and complete it (existing `completeGraph`).
- `GraphTaskUtils`: `getNodes` returns `List<WorkflowTask>`; `findNode(name)`; new
  `resolveTransition(evaluator, transitions, fromNodeName, context) -> Optional<String>` implementing
  the rule; `resolveNext` becomes the `to`-resolution helper it already is.
- `WorkflowNodeOutputFacadeImpl.getChildWorkflowTasks`'s `graph` arm reads `nodes` as a task list
  (the nested `tasks` unwrapping goes away).
- Tests: rewrite `GraphTaskDispatcherTest`, `GraphTaskCompletionHandlerTest`,
  `GraphTaskDispatcherIntTest` + YAML fixtures to the new shape: forward chain, conditional fan-out
  (first truthy wins), default edge, dynamic `to` (static + dynamic mixed on one node), back-jump cycle
  within budget, budget exhaustion (exact message pinned), unknown target, terminal output +
  `${graphName}` downstream, nested dispatcher as a node, graph inside a graph, more-than-one
  unconditional edge (first declared wins). Regenerate `graph_v1.json`.
- Docs: `docs/content/docs/platform/automation/build/workflows/flow-controls.mdx` Graph section and the
  generated `reference/flow-controls/graph_v1.mdx` follow the new shape (still marked coming soon).

## Client design

### Rendering

- A new `graphFrame` React Flow node type: rounded border, subtle fill, a header row carrying **Add
  node** and **Auto-arrange**. Its `width`/`height` are set explicitly (see layout). It sits in the
  chain between the graph's existing top and bottom ghost anchors, which are kept — the entire
  codebase addresses a container's entry/exit through `<id>-graph-top-ghost` / `-graph-bottom-ghost`
  (continuation edges, `nestedBottomGhostId`, `collectChainSuccessorNodes`, insertion after the box) —
  and rendered flush with the frame's top/bottom edges. The `graph` dispatcher `WorkflowNode` stays
  above the box as the chain node it is today (settings, delete, copy).
- Member nodes are the tasks in `parameters.nodes`, rendered with the existing node components
  (`WorkflowNode`, `AiAgentNode`, dispatcher subtrees with their own ghosts/placeholders/frames), with
  `parentId = <frameId>` and **frame-relative** positions. Nothing new below node level.
- A non-deletable **Start** pill node (`graphStart`) at the frame's content origin, joined by a
  `graphStart` edge to the start node. Re-dragging that edge's target to another node writes
  `startNode`. Removing the start node clears `startNode` (falls back to first declared).
- Transition handles sit on the **cross axis** of the workflow direction (TB → source on the right,
  target on the left; LR → source bottom, target top) so they never collide with a nested dispatcher's
  main-axis chain handles. Handle ids keep today's `-graph-transition-source/-target` suffixes.
- `graphTransition` edges: smoothstep with arrowhead, condition text shown as a label on hover/select,
  and — for a dynamic `to`, which has no static target node — a short **dashed stub** leaving the
  source handle and ending in a "dynamic" badge that shows the expression on hover; self-loop lobe
  reused from `computeGraphTransitionEdgePath`, and a warning style when the target name matches no
  node (only reachable via external edits — the editor never leaves dangling edges).

### Positions

`task.metadata.ui.nodePosition` on each member — the existing field, with two graph-specific rules:
it is **frame-relative** (origin = frame content area, below the header, non-negative), and it is
**always honored** (inside a graph, positions are the model, not a pin override). Members without a
position (definitions authored outside the editor) are auto-placed by ELK layered over the transition
graph, offset clear of the bounding box of positioned members, and **written back on the first user
interaction with that graph** (drag, connect, add) so a graph never reshuffles on the next add. New
nodes get a concrete free spot written immediately. `saveWorkflowNodesPosition.updateTaskPositions`
gains a `nodes` arm; `applySavedPositions` and the dagre constraint passes skip graph members
(`removeWorkflowNodePosition`'s unpin has no meaning inside a graph and the button is hidden there).

### Layout pipeline (engine-agnostic)

A pre-pass, `layoutGraphFrames`, runs **post-order** over graph frames before the active engine:

1. For each member, lay out its own subtree with the active engine (a plain task is just its rendered
   size; a dispatcher member yields its subtree's bounding box; a nested graph has already been
   processed by post-order).
2. Place members from `nodePosition` (auto-place the rest as above).
3. Frame size = union of member boxes + padding + header, with a minimum size.
4. Hand the outer engine the frame as a **sized leaf** node (a `graphFrame` arm in `getElkNodeSize` /
   `getDagreNodeSize`); member nodes and transition edges are removed from the outer arrays and
   re-appended after the engine returns. Because members are frame-relative, the outer result cannot
   disturb them; because the frame is an ordinary node, the outer flow reflows around it via the
   existing relayout + `animateNodePositions` tween — that is the "box changes size and the whole flow
   reshapes" behaviour, identical under ELK and dagre.
5. `getTasksStructuralFingerprint` includes each graph's transitions signature **and** its member
   positions (which are what determine the frame size), so a drop re-runs the outer layout; when the
   box did not change size the outer result is identical and the position tween is a no-op.

Retired: `createGraphNode`'s per-lane/trailing placeholders, `createGraphEdges` lane columns and
handle-group distribution, `orderGraphNodeIndexes`, the `graph` arm of `getMemberCaseRank`, the
`labeledGraphNode` edge + `GraphNodeLabel`/`useGraphNodeLabel`, `deriveGraphTransitionEdges`'s
kind/visual-position machinery, `GraphTransitionEdge`'s `bandY` routing, `extractNextTargets` and
the dynamic/dangling badge derivation, `collectGraphNextExpressions`.

### Interactions

- **Drag a member**: members are always draggable inside the frame regardless of the global drag lock
  (per-node `draggable: true`, like sticky notes), clamped to non-negative frame coordinates. While
  dragging, the frame's size is recomputed locally (cheap `setNodes` on the frame only); on drop the
  position persists and, if the frame size changed, the outer relayout runs (animated). Dragging a
  dispatcher member carries its subtree through the existing collect-descendants path.
- **Connect**: `nodesConnectable` stays off globally; graph member handles are the only connectable
  ones. `onConnect` + `isValidConnection` (both endpoints members of the same graph; no duplicate
  `from/to` pair) append `{from, to}`. Dragging from a source handle and dropping on empty frame space
  opens the component popover, creates the node at the drop point, and connects it.
- **Add node**: header **Add node** or dropping a component from the sidebar into the box creates an
  unconnected member at a free spot (drop point when dropped).
- **Edit a transition**: click an edge → popover with the `condition` mentions input, the `to` field
  (node name picker or expression), and delete. Selecting the graph container shows a **Transitions**
  panel (grouped by source node, reorderable — declared order is conditional priority; a warning row
  when a node has more than one unconditional edge) replacing `GraphStatesPanel` /
  `GraphNextNodeSuggestions` / `GraphTransitionBadges`.
- **Delete an edge**: removes the row. **Delete a node**: removes the task and every transition that
  names it (either direction) — no dangling edges by construction; clears `startNode` if it pointed
  there.
- **Auto-arrange**: ELK layered over the transition graph, writes every member position.
- Copy/paste of a graph deep-clones `nodes` and `transitions` (name remapping applies to both `from`
  and `to`); undo/redo comes from the definition (temporal store), as today.

### Read-only / execution view

Same frame, members, Start pill and transition edges (read-only members are `readonly` nodes that
gain the transition handles, closing today's "overlay handles don't exist in read-only" gap). Visited
nodes and taken transitions are highlighted from the `__node` stamps in the order they executed; the
execution accordion's per-visit grouping simplifies to one row per node visit.

## What deliberately does NOT change

- Everything under `server/libs/atlas/`. The graph remains a coordinator-side dispatcher.
- The graph dispatcher's own chain node above the box, and the ghost anchors as entry/exit.
- Budget, context, output and failure semantics from the 2026-08-02 spec.

## Rejected alternatives

- **UI-only, keep per-node `next` expressions** — drag-connect must author ternaries; a second edge
  from a node needs a condition skeleton the editor cannot invent; deleting an edge inside a ternary is
  a manual edit; N outgoing edges is an N-deep ternary. Workable, noticeably worse, and it would keep
  the heuristic literal-extraction layer that two phases already fought.
- **Strict declared-order evaluation (unconditional edges shadow later ones)** — simpler runtime,
  but a user who draws a default first and then adds a conditional edge gets an edge that can never
  fire; "conditional first, then default" matches the Step-Functions Choice/Default mental model.
- **State cards with task lists** — user decision 2 removed task lists per node.
- **A separate sub-editor per graph (double-click to open)** — contradicts the request that the box
  and the surrounding flow reshape in place.
- **ELK `org.eclipse.elk.fixed` compound for the frame** — elegant on ELK, no dagre equivalent, and
  ELK compound frames are dropped before nodes are emitted today anyway; a sized-leaf frame gives both
  engines identical rendering.

## Testing

- Server: unit + IntTests per the list above; snapshot regenerated.
- Client: `layoutGraphFrames` (frame sizing, post-order nesting, auto-place-then-persist, both engines
  produce identical member geometry); connect/append + delete cascade; transition editor + panel;
  fingerprint reacts to transitions and frame size but not to same-size drags; read-only conversion
  keeps transition edges; paste remaps `from`/`to`; `saveWorkflowNodesPosition` `nodes` arm.

## Non-goals / follow-ups

- Migration of pre-existing `graph/v1` definitions (unreleased; dev fixtures such as
  `graph-topo-repro` are recreated).
- A distinct `default`/`else` marker beyond "the unconditional edge"; per-edge visit limits.
- Free-form placement for other containers (Loop, Condition…) — the frame-as-leaf pre-pass is written
  so it *could* host them later, but only graph is in scope.
- Reconnecting an existing edge's endpoints by dragging (edit the `to` field instead).
