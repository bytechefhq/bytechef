# Graph dispatcher free-form canvas — execution record

Closing note for the `graph/v1` rework. The **specs** say what was intended
(`docs/superpowers/specs/2026-08-17-graph-dispatcher-freeform-canvas-design.md`), the **plans** say how
it was to be broken up, and **CLAUDE.md** carries the resulting conventions under "Graph dispatcher
canvas". This file records what only the execution knows: decisions that overrode the plan, defects
found late and why they hid, and what is deliberately still open.

Branch: `claude/graph-dispatcher-canvas-handoff-3dae7e`, 28 commits on `d442e1566bb` (7 server,
21 client). Complete and green — `npm run check` EXIT 0 (611 files / 5526 tests), `:graph:test` and
`:graph:testIntegration` green — and **unmerged** as of 2026-08-19.

## Decisions that overrode the plan

Each of these changed what got built. They are listed because the plan text still says otherwise, and a
reader comparing plan to code will otherwise think something was missed.

**The spec wins on the dynamic badge.** The plan had a dynamic transition's label appear only on
hover/select. The spec calls for a standing `dynamic` badge with the expression revealed on hover — and
`createGraphEdges`' own emission comment, written earlier and independently, describes the same badge.
Two sources against the plan; the badge stands at rest.

**A dangling transition can never paint on the canvas.** `data.dangling` is set exactly when `from` or
`to` names no member — and in precisely those cases the edge's `source`/`target` is a node id that does
not exist, so React Flow drops the edge before rendering. The amber styling on `GraphTransitionEdge` is
therefore unreachable in practice. The flag is kept because the **Transitions panel** reads it: a list
row needs no endpoints. Do not "fix" this by making the canvas draw it.

**Read-only-ness is carried by the component split, not a data field.** `useLayout` rewrites every
`workflow`/`clusterRoot` node to `type: 'readonly'`, so `WorkflowNode`/`AiAgentNode` are never rendered
read-only and `ReadOnlyNode` always is. Two separate plan lines called for a `data.readOnly` field on
task nodes; it does not exist, was not added, and is unnecessary. (An *edge* `readOnly` field is a
different, legitimate thing.)

**Per-node flags beat the global canvas flags.** Verified against React Flow 12's source:
`isDraggable = !!(node.draggable || (nodesDraggable && typeof node.draggable === 'undefined'))`, and the
same shape for `connectable` and per-edge `reconnectable`. So `nodesConnectable={false}`,
`nodesDraggable`, and `edgesReconnectable={false}` were all left alone and the graph opts in per node
and per edge. Flipping any global would make every node in every workflow connectable or draggable.

**But the global `nodesConnectable={false}` does suppress the rubber-band line.**
`ConnectionLineWrapper` gates on the *store's* value, which per-node `connectable` never reaches, and a
custom `connectionLineComponent` does not help because it is rendered *by* that gated wrapper. Rather
than flip the global, the graph draws its own connection line via `ViewportPortal`. If that overlay is
ever removed, drag-to-connect becomes a blind drag.

**Frame auto-size is legitimately engine-dependent.** With a dispatcher member, dagre and ELK produce
different frame heights (464 vs 408 in the pinned case), because the member's internal subtree is laid
out differently and the frame sizes to fit. Member *positions* are engine-independent — they are
persisted. The spec's "renders identically across engines" was written before dispatcher members were
considered. Do not build anything to force parity.

**Dagre parity is no longer a goal, but dagre is still reachable.** `elkLayoutUtils` calls
`getLayoutElements` as its error fallback, so an ELK runtime failure still renders graphs through dagre.
Its `graphFrame` arms must stay correct until that fallback is deliberately removed.

## Defects found late, and why they hid

Worth reading before writing anything that touches these paths.

**The Plan A/Plan B seam.** Making `nodes` an ordinary subtask list silently subscribed graph members to
every generic task-handling path in the client — including one that clears a "main axis" position so
dagre can reflow a chain. For a frame child dagre never places, that produced `{x: n, y: undefined}`,
which `containsNodePosition` accepted as pinned (it tests only `!== undefined`), `toFrameChildPosition`
turned into `{x: n, y: NaN}`, and `JSON.stringify` persisted. Both plans were individually correct; the
defect lived in an assumption neither restated.

**Two inheritance failures.** `collectGraphLayoutSignature` knew about `transitions` and positions but
not `startNode`, so re-pointing the Start pill saved correctly and never redrew — the canvas asserted
one entry point while the workflow used another. And Auto-arrange measured each member by its own node
box while the layout pre-pass, in the file beside it, measured the group bounding box — so
auto-arranging a graph containing a loop overlapped the loop's children and then persisted it. Both are
cases of a newer consumer learning only part of an established invariant.

**Structural vs semantic predicates.** The drag-stop persist path gated on "is my parent a frame?"
rather than "am I a graph member?". Those coincide until a member has a subtree, at which point the
subtree's own nodes enter the branch and get positions written that are not part of the model. The
correct predicate (`getGraphMemberOwner`) already existed one file away.

**`<Property>` resolves its target from a store.** `saveProperty` reads `currentNode` from
`useWorkflowNodeDetailsPanelStore`, and `useProperty` reads the displayed value from
`currentNode.parameters`. That is unambiguous on the details panel and false on the canvas, where
selecting an *edge* changes nothing. The transition popover works around it by pointing the store at the
graph while open. The blast radius of doing it properly was measured — 46 `currentNode` reads across a
1790-line hook, 7 render sites, 17 consumers — and a durable note was left in `useProperty.ts` instead.
A half-threading would be worse than either extreme.

**Six tests that could not fail.** Fixtures where both branches were arithmetically identical; a guard
test whose fixture made guarded and unguarded paths land on the same result; a negative assertion
searching for a value the fixture never contained; a parity test whose fixture made the two engines
cancel by construction; one where two fields held the same value so neither pinned which was read; and
one whose fixture omitted the field the code under test clears. None were visible to coverage. Mutation
testing — revert the behaviour, confirm RED — is what caught the later ones, and it is worth the minute
it costs on any assertion that matters.

**Comment rot, in seven files.** Comments describing the retired lane model, naming deleted symbols, or
asserting a future state as present. Three implementers deferred fixing them on file-ownership grounds
and all three deferrals were overridden: file ownership is not the right boundary for a comment that is
now false, and several of them had already misled a later author.

## Deliberately open

1. **Manual verification — 14 behaviours have never been seen rendered.** Every one has automated
   coverage; none has been looked at in a browser. Enumerated in the branch's SDD report. This should
   gate the merge.
2. **The workflow-executions sheet highlights nothing**, for any workflow shape, because it never
   populates `workflowTestNodeStates`/`workflowTestExecution` — it has the execution in props and passes
   only the definition. Pre-existing and wider than graphs; deferred to its own branch. Reversal
   condition: if reviewing historical executions is a primary user journey rather than an occasional
   one, do it next.
3. **Dagre removal** is confirmed as wanted. ELK supports every current workflow shape, so it is
   feasible — but the two dagre-named modules are largely *shared* infrastructure ELK imports from, so it
   is a disentangling job rather than a delete. One decision blocks it: what replaces the silent fallback
   on an ELK runtime error.
4. **Accepted residuals.** The budget-exhausted path leaks its counter row (as do fork-join and map; the
   comment now says so). `parentId === frameId` is load-bearing in two places. `userEvent` dialog specs
   remain load-sensitive, now with 3x timeout headroom.

## One thing that generalises

Twice, measuring a blast radius reversed the decision — 46 reads for `<Property>`, 23 production call
sites for the save queue. Both times the instinct was "this is a small fix." Counting is cheap and it
changed the answer.
