# Graph dispatcher: topological lane ordering and transition legibility

- **Date**: 2026-08-06 (revised same day after the first attempt was reverted)
- **Status**: Approved, staged implementation
- **Ticket**: 0_732 QA session (user request: "render/present nodes and transitions in a more
  natural way, more like normal graph flows")

## Problem

The `graph/v1` dispatcher renders one vertical lane per declared node, distributed
left/middle/right by declaration order (mirroring fork-join). Transition (`next`) expressions
render as a dashed overlay. Rendering the `graph-topo-repro` workflow (a four-node cycle plus one
dynamic-`next` node) surfaced four distinct defects, not one:

1. **Lane order ignores transition topology.** Lanes render in declaration order
   (`node_0 … node_4`), so a chain whose declaration order disagrees with its transition order —
   the common case, since nodes are appended as they are created — renders mostly as back-arcs.
   On the repro, three of four arcs run right-to-left and the one forward arc skips two lanes.
   The actual chain (`node_1 → node_0 → node_3 → node_2 →` back to `node_1`) is recoverable only
   by tracing arcs backwards across the container.
2. **Arcs cross node labels.** The hidden transition handles sit at the task node's vertical
   centre, and a node's label block sits to the right of its icon at that same height, so
   horizontally-travelling arcs necessarily cut through label text. This is structural, and
   reordering lanes does not fix it.
3. **Arrowheads are ambiguous.** Several arcs terminate within a few pixels of each other at a
   lane's entry point, so which arc lands where is unreadable.
4. **The trailing add-node placeholder straddles the container border**, sitting outside the
   right edge at task-row height rather than inside with the per-lane add buttons. Present under
   both layout engines.

Defect 1 is what the original version of this spec addressed. Defects 2–4 were found by looking at
the rendered canvas and are in scope here.

## Mechanism correction (supersedes the previous "Findings" section)

The first implementation attempt permuted the left/middle/right handle groups in
`createGraphEdges` and re-derived arc `kind` from visual positions. Lanes did not move, and the
result read as *more* broken than before, because the arcs then described an ordering the lanes did
not have.

That attempt's post-mortem concluded lane x-order was set by ELK's
`considerModelOrder: NODES_AND_EDGES` reading the ReactFlow node array, and that fixing it required
emitting lane member nodes in topological order in both engines' node-construction paths. **That
diagnosis is wrong.** `buildScopeChildren` (`elkLayoutUtils.ts`) sorts each container's ELK
children explicitly, by `getMemberCaseRank`; for a graph container that key is

```ts
return memberData.graphData?.nodeIndex ?? memberData.nodeIndex ?? Number.MAX_SAFE_INTEGER;
```

`considerModelOrder` merely preserves the order that sort has already produced. Lane order is
therefore controlled at exactly one seam, and no node-array surgery, contiguous-run detection, or
change to node construction is required. The previously-scheduled remedy would have been both
larger and ineffective.

## Decisions

Order the lanes by **transition topology** instead of declaration order, keeping the lane frames,
chips, overlay arcs, and per-lane add buttons as they are — then fix the two legibility defects the
reordering does not address.

- A stable topological sort (Kahn) over the **statically resolvable** transition targets
  (`extractNextTargets`) produces the visual order. Declaration index breaks ties, so graphs
  without transitions keep today's rendering exactly.
- `startNode`, when it statically resolves to a declared node, is preferred first among ready
  nodes.
- Cycles are broken deterministically: when no node has zero remaining in-degree, the
  **lowest-declaration-index** remaining node is emitted next, and its incoming transitions become
  back-arcs. (`startNode` is preferred among *ready* nodes but is deliberately not used as the
  cycle-break anchor — keeping one rule for the tiebreak keeps the ordering predictable.)
- Nodes with no statically resolvable transitions in either direction rank last.
- Self-loops and dynamic/dangling `next` expressions do not constrain ordering; they stay
  badge/overlay-only.
- Overlay arc `kind` (`forward`/`back`/`self`) derives from **visual positions**, not declaration
  indexes, so arc bowing matches what is on screen.
- **Lane ordering applies to the ELK engine only.** The transition overlay is already ELK-only and
  dagre renders no arcs at all, so reordering dagre's lanes would change its layout with no visible
  benefit. Dagre lane order also comes from a different path (structural edges plus handle groups)
  and would need separate work and separate verification.

## Implementation

Staged, with a visual checkpoint between stages.

**Stage 1 — topological lane order**

- `orderGraphNodeIndexes.ts` (new, pure, unit-tested): `(nodes, startNode?) => number[]`, a
  declaration-index permutation per the rules above.
- `elkLayoutUtils.ts`: `getMemberCaseRank`'s graph branch maps `nodeIndex` through that permutation
  to a visual rank. The trailing add-node placeholder carries `nodeIndex === nodes.length`, which
  stays maximal under any permutation of `0..n-1`, so it keeps ranking last without special-casing.
- `createGraphEdges.ts`: `distributeGraphNodeIndexes` splits the **ordered** index array into the
  left/middle/right handle groups, so handle sides agree with visual order;
  `createGraphTransitionEdges` maps declaration indexes to visual positions before deriving each
  arc's `kind`.
- `deriveGraphTransitionEdges.ts`: accepts an optional `visualPositionByIndex` map (identity by
  default, so existing consumers and tests are unaffected).
- `getGraphNodeSide` resolves first/middle/last from the visual position rather than the
  declaration index.

**Stage 2 — move arcs out of the label band** (scoped after stage 1 is seen on screen)

- Relocate the hidden `graph-transition-source`/`-target` handles from the task node's vertical
  centre to its **top edge** (`WorkflowNode`, `PlaceholderNode`), so arcs travel through the empty
  gap between the lane header row and the task row instead of through label text.
- Adjust `computeGraphTransitionEdgePath` to bow within that band, keeping the existing per-kind
  offset stacking so parallel arcs stay distinguishable.

**Stage 3 — trailing placeholder position**

- Bring the trailing add-node placeholder inside the container's right edge, consistent with the
  per-lane add buttons.

## Verification

The first implementation step is the `getMemberCaseRank` change alone, verified by re-rendering the
`graph-topo-repro` workflow and comparing against the captured before-state. This is the cheapest
possible test of the mechanism claim above, and nothing else is built until it holds — the previous
attempt failed precisely by building on an unverified mechanism.

Unit tests:

- `orderGraphNodeIndexes`: simple chain, cycle (verifying the lowest-index break), isolated nodes,
  no transitions at all (identity permutation), `startNode` preference, and self-loops.
- `createGraphEdges`: handle-group distribution over a permuted order, and arc `kind` derived from
  visual position.
- `deriveGraphTransitionEdges`: identity default preserved; explicit visual-position map honoured.

Each stage ends with a screenshot of the repro workflow.

## What deliberately does NOT change

- Transitions remain a paint-time overlay — never fed into layout, never walked by chain
  traversals. The phase-3 architecture decision stands.
- The overlay stays ELK-only; dagre keeps badges as its transition visualization, and now also
  keeps declaration lane order.
- Lane ids, placeholder ids, transition edge ids, and chip labels all keep using **declaration
  indexes**. Ordering is purely a presentation-time permutation, so no persisted structure and no
  mutation path changes.

## Rejected alternatives

- **Free-form ELK layered layout over the transition graph** (true state-machine rendering):
  conflicts with the lane/frame architecture (per-lane task chains, add placeholders, dagre engine,
  read-only conversion) — a rewrite, not an increment. Revisit if topological ordering proves
  insufficient.
- **Rendering static transitions as solid structural edges**: would feed transitions into layout
  and chain traversal, reversing the phase-3 isolation decision.
- **Permuting lane member nodes in the ReactFlow node array**: unnecessary given the
  `getMemberCaseRank` seam, and materially riskier — empty-lane placeholders and populated-lane
  task runs live in different regions of that array, so keeping their relative order consistent
  would have meant refilling scattered slots or restructuring the ghost block.
- **Anchoring transition arcs to the lane header chips**: the headers are edge labels
  (`GraphNodeLabel`/`useGraphNodeLabel` on the `labeledGraphNode` edge type), not ReactFlow nodes,
  so they expose no handles to anchor to.
