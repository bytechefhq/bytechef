# Graph task dispatcher (state-machine flows with cycles)

**Date:** 2026-08-02
**Status:** Implemented
**Scope:** CE — new `task-dispatchers/graph` module, `platform-configuration-service` (editor output
resolution), client workflow editor (phases 2-4).

## Problem

The engine can only express cycles as fixed-count `loop` iterations. There is no way to say
"keep refining until the check passes" or "route back to enrichment when validation fails" —
the retry-until-done, review-cycle, and agent-routed shapes users otherwise hack together
with loop+condition nests. A graph (state-machine) container makes them first-class:
named nodes, per-node transition expressions, cycles allowed, bounded by a budget.

Deliberately NOT a free-standing `jump/v1` goto task: a goto forces every completion
handler (Default + all six dispatcher handlers) to re-answer "what is next?", and jumping
across nesting boundaries corrupts context-stack pairing. The container confines jump
semantics to one handler — the same isolation that made the dispatcher return-values
feature three small edits (rejected alternative, recorded).

## Design

### DSL

```yaml
- name: triage_1
  type: graph/v1
  parameters:
    startNode: classify          # optional; defaults to the first declared node
    maxTransitions: 100          # optional; default 100, run fails when exceeded
    nodes:
      - name: classify
        tasks: [ ...ordinary task list... ]
        next: "=${score.value} > 0.8 ? 'approve' : 'review'"
      - name: review
        tasks: [ ... ]
        next: "=${review.verdict} == 'REDO' ? 'classify' : 'approve'"   # back-jump
      - name: approve
        tasks: [ ... ]
        # no `next` -> terminal node, graph completes
```

- Each node body is an ordinary task list — structurally identical to a condition case or a
  fork/join branch. Nothing new below node level; nodes may contain any dispatcher,
  and a graph may sit inside any container (composition like every other dispatcher).
- `next` is an **expression** (decision: expression-based transitions) evaluated against the
  accumulated run context, resolving to a node name. A null/empty result is equivalent to
  no `next` — terminal. An unknown node name fails the run with a clear error naming the
  offending node and value.
- Node names must be unique within one graph (validated at dispatch).
- A node with an EMPTY task list is legal and acts as a pure router: its `next` is evaluated
  immediately (still consuming one transition from the budget).

### Runtime semantics

- **Dispatch**: `GraphTaskDispatcher` resolves the start node and dispatches its first task
  with `parameters.__node = "<name>"` stamped on it — the fork/join `__branch` / map
  `__iteration` pattern. The transition budget is seeded via `CounterService.set(parentId,
  maxTransitions)`.
- **Within a node**: `GraphTaskCompletionHandler` advances `taskNumber` through the node's
  task list — verbatim the condition/branch pattern (`canHandle`: parent is `graph/v1` AND
  parameters carry `__node`).
- **At node exhaustion**: the handler evaluates the node's `next` expression against the
  accumulated context; on a target it decrements the budget (0 reached → fail the run:
  "graph transition budget exhausted"), pushes context, and dispatches the target node's
  first task. On terminal it copies the completed child's output onto the parent (the
  return-values write point) and completes the graph.
- **Output contract**: `${graphName}` = the terminal node's last-task output — consistent
  with condition/branch. An output-less terminal last task → null.
- **Context** (decision: accumulated, last write wins): one context stack; a re-executed
  node sees everything produced so far and `${taskName}` resolves to the most recent
  execution's output. This is the engine's existing context-push behavior — zero new
  machinery, and pinned by tests as the contract.
- **Cycle safety** (decision: total transition budget): `maxTransitions` (default 100) as a
  `CounterService` countdown. Exceeding it fails the run cleanly. No per-node visit limits.
- **Failure**: a failed task inside a node fails the job through `ErrorHandlingTaskDispatcher`
  exactly like every other container — no partial-output contract.
- **Crash/suspend/distributed**: all state is data (`__node` stamp, counter, context stacks
  in `TaskFileStorage`/`CounterService`/`ContextService`) — recovery, resume, and the
  distributed deployment work with zero graph-specific code. Nothing under
  `server/libs/atlas/` changes.

### Design-time output (editor)

The definition factory reuses `TaskListOutputDataSource` (from the return-values feature):
the previewed schema is the **first declared terminal node's** last-task schema (documented
approximation — which terminal executes is undecidable statically; same spirit as
condition's caseTrue approximation). No terminal node with tasks → null.
`WorkflowNodeOutputFacadeImpl.getChildWorkflowTasks` gains a `nodes` arm so tasks nested
inside a graph are excluded from autocompleting the enclosing graph's aggregate.

## Phases (sequential, each independently shippable)

**Phases 1-4 — implemented (2026-08-02/03).**

### Phase 1 — Engine (server only)

`task-dispatchers/graph` module: dispatcher, completion handler, constants, definition
factory (properties + `.output(...)` + `taskProperties`), snapshot JSON, unit + IntTests
(forward flow, back-jump cycle with budget, budget exhaustion failure, router node,
unknown target failure, terminal output, `${graphName}` downstream resolution, nested
dispatcher in a node). Server-side editor plumbing: facade exclusion arm + snapshot.
Usable immediately via code workflows, the API, and AI-copilot-generated definitions —
the palette entry appears automatically (definitions are server-driven), rendering as a
plain node until phase 2.

### Phase 2 — Editor structural support (client)

Everything needed to author a graph on the canvas WITHOUT rendered transition edges:
- Registry entries: `TASK_DISPATCHER_CONFIG` (getSubtasks/updateTaskParameters/
  initializeParameters/extractContextFromPlaceholder keyed by node name),
  `TASK_DISPATCHER_NAMES`, `TASK_DISPATCHER_SUBTASK_COLLECTIONS` (`nodes`),
  `TASK_DISPATCHER_DATA_KEY_MAP` (`graphData`) + `NodeDataType`.
- The hardcoded per-type sites (researched inventory): `useLayout.tsx` node+edge chains,
  `createGraphNode.ts` / `createGraphEdges.ts` (new pair — one sub-lane per node like
  fork/join columns, per-node placeholder + trailing add-node placeholder),
  `elkLayoutUtils.ts` (`ELK_FRAME_DISPATCHER_COMPONENT_NAMES`, a `graph` arm in
  `getMemberCaseRank` — the condition-binary fallback is wrong for N nodes, ghost-segment
  tables), `taskTraversalUtils.ts`, `getTask.ts`, `saveTaskDispatcherSubtaskFieldChange.ts`,
  `pasteNode.ts` (deep-clone `nodes`), `handleDeleteTask.ts`.
- Node NAME editing: inline-editable chip per node lane — the `BranchCaseLabel` precedent
  (validated: unique, non-empty).
- `next` EXPRESSION editing: selecting the graph container shows a states list in the
  properties panel, one `PropertyMentionsInput` per node — new UI, closest structural
  cousin is the dispatcher's own top-level parameter fields.
- Transitions surface as **badges/panel text only** in this phase (e.g. "→ review | approve"
  derived from node-name literals found in the expression); no routed edges.
- Data pills need no work (output surfacing is server-driven via `outputDefined`).

### Phase 3 — Transition edges (cyclic rendering)

The first genuine edge concept in the app — no existing pattern extends to it:
- Derive candidate edges by extracting node-name literals from each `next` expression
  (best-effort static analysis; a fully dynamic expression renders as a "dynamic" badge on
  the node instead of edges).
- ELK: enable/configure elkjs's cycle-breaking for the graph frame; back-edge routing
  geometry (the frame code currently assumes clean top-entry/bottom-exit with strictly
  downstream chains).
- Audit `collectDescendantNodes`/chain-walker semantics for cycle-legal meaning (the
  `visited`-set guards prevent freezes but "successors of X" needs a defined answer once X
  can loop back).
- dagre fallback: graphs keep phase-2 (edge-less) rendering under dagre; cyclic edges are
  ELK-only.

### Phase 4 — Agent routing polish + run observability

- Expression-based `next` already permits dynamic targets (`=${routerAgent.nextNode}`);
  this phase makes it first-class: editor autocomplete offering the graph's node names
  inside `next` expressions, validation warnings for expressions that can't be statically
  tied to any node, docs + a worked example of an LLM-routed graph (AI Agent node whose
  structured output picks the next state).
- Execution view: transition timeline for a graph run (visited nodes in order, visit
  counts, budget consumed) — repeated executions of one node currently render as flat
  repeated rows; group them per node.
- User docs page for the graph dispatcher (released-version convention applies).

## Rejected alternatives

- **Free-standing `jump/v1` goto** — leaks "what is next?" into every completion handler,
  breaks nesting/context-stack pairing; recorded above.
- **Static edges + guard conditions** (state-machine edge lists) — more config surface for
  the same power; expression-based `next` was chosen (user, 2026-08-02) and edge lists can
  be layered later without DSL breakage if the editor ever wants to author real edges.
- **Per-node visit limits** — finer-grained than the total budget with unclear blame
  semantics on failure; total budget chosen (user, 2026-08-02).
- **Iteration-scoped context (map-style sub-stacks)** — heavy, unclear downstream
  semantics; accumulated last-write-wins chosen (user, 2026-08-02).

## Testing

- Phase 1: completion-handler unit tests + IntTests per the list above; the budget test
  pins the exact failure message shape; the composition test nests a condition inside a
  node and a graph inside a fork/join branch.
- Phase 2: registry/round-trip tests (insert/delete/paste with `nodes`), name-chip
  validation tests, snapshot of the properties-panel states list.
- Phase 3: edge-derivation unit tests (literal extraction incl. quoting variants,
  dynamic-expression fallback), ELK layout tests per the existing frame-geometry pinning
  pattern.
- Phase 4: run-view grouping tests; docs types:check.

## Decisions log

- Shape: **graph container dispatcher**, not a goto task (user, 2026-08-02).
- Transitions: **expression-based `next`** per node (user, 2026-08-02).
- Cycle safety: **total transition budget**, `maxTransitions` default 100 (user, 2026-08-02).
- Context on re-execution: **accumulated, last write wins** (user, 2026-08-02).
- All four phases will be executed sequentially (user, 2026-08-02).
- Output: terminal node's last-task output, consistent with condition/branch.
- Empty node = router (immediate `next`, consumes budget).
- Phase-2 transitions render as badges; real cyclic edges are phase 3, ELK-only.

### Implementation-forced decisions (Phase 1, 2026-08-02)

- Router hand-offs carry a dedicated `__routerNode` sentinel (never persisted) so nesting
  a graph inside a graph node survives — a plain `__node` overload broke that case (clean
  but silent mis-route on a name collision between the outer and inner graph's node
  names). The completion chain runs ALL matching handlers with `Default` structurally
  appended last (in both `TaskCoordinatorConfiguration` and `JobSyncExecutor`), and the
  graph handler marks router hand-off events `handled` (Suspend precedent) to stop
  `Default` from prematurely completing the job — without it, a router hand-off event
  (`parentId == null`) fell through to `DefaultTaskCompletionHandler` and completed the
  job early.
- Pure-router graphs (zero real tasks) complete cleanly with null output, budget consumed
  per hop — the sentinel is stripped before terminal completion so a degenerate
  all-router graph doesn't recurse to budget exhaustion.
- Budget semantics: exactly `maxTransitions` transitions are permitted; the (N+1)th
  fails. Pinned end-to-end against the real `CounterService`, not a stub.
- `next` has NO default value — terminal-by-omission is load-bearing. Condition's
  `defaultValue("=")` was deliberately not copied onto graph's `next`, so an omitted
  `next` on a node means "this node is terminal," not "fall through to some implicit
  expression."
- Deferred-evaluation registration: `graph/`'s `nodes` key is registered with
  `DeferredEvaluationParameterKeys` so nested task parameters and `next` evaluate lazily
  rather than being frozen at first evaluation. The IntTest harness replicates this
  registration directly, because direct-construction dispatcher IntTest harnesses never
  load module `@Configuration` static blocks — every sibling `*TaskDispatcherIntTest`
  shares this gap, noted as a candidate for a future test-support refactor.

### Implementation-forced decisions (Phase 2, 2026-08-03)

- Editor lane addressing is by node **index**, not name — renames never re-home a lane's
  tasks. This mirrors the id/name split fork/join already has, but graph leans on it
  harder since node names are also the `next`-expression vocabulary.
- Deleting a node's last task keeps the empty node rather than removing the lane — an
  empty node is a legal router (immediate `next`, consumes budget), so the editor must
  keep it addressable. Fork/join's empty-branch shrink-on-delete behavior was deliberately
  **not** copied onto graph for this reason.
- Renames do **not** rewrite other nodes' `next` expressions. A rename can leave another
  node's `next` pointing at a name that no longer exists; the editor surfaces this as a
  dangling-target warning badge instead of silently rewriting expression text (which would
  risk corrupting a dynamic/computed expression it can't safely parse).
- Transition badges are derived from **single-quoted string literals found in the `next`
  expression, unioned with a dynamic marker** when the expression isn't a plain literal set.
  This extraction utility is phase 3's edge-derivation seed and MUST be tightened before
  edges are drawn from it — today it mis-reads comparison operands (e.g. equality checks
  inside a ternary condition) as dangling targets; the phase-2 test suite pins this
  known limitation and phase 3 flips that test deliberately once extraction narrows to
  ternary-result positions only.
- Every node renders a lane, including empty ones (empty = legal router lane, per above);
  there is no left-ghost placeholder. The trailing add-node placeholder is the sole
  empty-state affordance for adding a new node.

### Implementation-forced decisions (Phase 3, 2026-08-03)

- Transition edges are an **overlay**, not real graph edges: they are paint-time-only
  ReactFlow edges (`type: graphTransition`) layered on top of the ELK-computed geometry.
  They are excluded from ELK ranking, from dagre ranking (including the ELK-error
  fallback path), from structural-dedupe collapse, and from every chain/descendant
  walker. Lanes keep their phase-2 declaration-order layout untouched by the overlay.
  This makes cycles exist only at paint time — the layout engines never see a cycle to
  break. The spec's original phase-3 sketch (configure elkjs's own cycle-breaking for
  the graph frame) was investigated and rejected as **not needed**: the overlay
  architecture sidesteps the problem the sketch was written to solve, at lower risk than
  teaching ELK about back-edges.
- Extraction was tightened from phase 2's literal-scan to **ternary-result positions
  only**, as phase 2 planned — the phase-2 documenting test for the known limitation
  (comparison operands misread as dangling targets) was flipped as scheduled. Dangling
  literals (targets that don't resolve to a real node name) remain badge-only; they are
  never promoted to edges.
- dagre keeps phase-2 badges as its **sole** transition visualization — the overlay is
  ELK-only. Read-only mode drops transition edges entirely rather than degrading them:
  read-only node types carry no transition handles, so there is nothing for an overlay
  edge to anchor to.
- The phase-3 isolation audit (conducted before closing the phase, independent of the
  per-task reviews) found two latent pipeline bugs beyond what either task's own tests
  exercised: transition edges were being silently dropped by the structural-dedupe pass,
  and the ELK-error dagre fallback path leaked transition edges into dagre's ranking
  instead of respecting the entry filter. Both were fixed and pinned with regression
  tests during phase 3 rather than deferred.

### Implementation-forced decisions (Phase 4, 2026-08-03)

- Node-name autocomplete suggestions include the node's **own name**, so self-loops
  (`next: 'this_node'`) are a reachable one-click insertion, not something the user has
  to type by hand. Insertion is a quoted-literal full-value replace, not an append —
  matching the seam this phase found: the mention popup's `${...}` insertion is the
  wrong shape for a quoted `next` literal.
- Chips are **disabled** whenever the current `next` expression is dynamic or not a bare
  literal (the silent-clobber guard: an insertion click must never overwrite an
  expression the click can't faithfully represent). A dangling bare literal — a `next`
  that names a node that no longer exists — stays **enabled**, since a full-value
  replace is exactly the one-click repair for that case. `includeInMetadata: true` is
  kept on the save path, matching `PropertyMentionsInputEditor`'s own pre-existing
  `next`-save behavior (verified by reading the code and its blame, not assumed) — a
  reviewer's counter-claim that it should be `false` was investigated and refuted.
- No additional static-analysis-warning surface was added beyond what phase 2/3 already
  render: the existing dangling-target and dynamic-expression badges satisfy the spec's
  phase-4 "validation warnings for expressions that can't be statically tied to any
  node" requirement.
- The run view groups repeated child-task rows **per node visit**, using each visit's
  `taskNumber === 1` as the visit boundary — safe under self-loops, since a self-loop
  produces a fresh visit (and a fresh `taskNumber` sequence) rather than continuing the
  prior one. The grouping key, `__node`, survives to the client unmodified through the
  existing `workflowTask` DTO chain (verified end-to-end: dispatcher stamp → DTO → the
  client's `parameters` map) — no new plumbing was needed to carry it. The summary
  deliberately reports **node visits only**, not a derived transition count: a
  client-computed "N transitions" figure under-reports router hops relative to
  visit-based counting and would collide with the server's own `maxTransitions`
  terminology during budget-exhaustion debugging, so a transition-count reading was
  removed rather than shipped alongside the visit count.
- Docs and in-editor copy name the two layout engines by their **toolbar labels**
  ("experimental" / "standard"), never by their internal library names (ELK / dagre) —
  users never see the library names, only the toolbar picker.
