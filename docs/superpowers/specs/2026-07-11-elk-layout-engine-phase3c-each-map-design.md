# ELK Layout Engine Phase 3c: Each + Map Dispatcher Support

**Date:** 2026-07-11
**Status:** Draft
**Depends on:** Phase 1 (2026-07-04), Phase 2 loop (2026-07-06), Phase 3a branch (2026-07-07), Phase 3b parallel/fork-join (2026-07-07)

## Goal

Add the `each` and `map` task dispatchers to the experimental ELK layout engine as
compound frames, nestable both ways (each/map inside any supported dispatcher, any
supported dispatcher inside each/map), with the engine's uniform edge-rhythm guarantee
intact in TB and LR. After this phase the only unsupported shapes are `on-error` and
AI-agent cluster roots.

## Ground truth (verified in code)

Both dispatchers are structural clones of loop:

| Aspect | each | map | loop (reference) |
|---|---|---|---|
| Top ghost id | `${id}-each-top-ghost` | `${id}-map-top-ghost` | `${id}-loop-top-ghost` |
| Bottom ghost id | `${id}-each-bottom-ghost` | `${id}-map-bottom-ghost` | `${id}-loop-bottom-ghost` |
| Rail (left ghost) id | `${id}-taskDispatcher-left-ghost` | same | same |
| Rail creation | unconditional | unconditional | unconditional |
| Placeholder | `${id}-each-placeholder-0`, only when empty | `${id}-map-placeholder-0`, only when empty | same pattern |
| Child data | `eachData: {eachId, index: 0}` | `mapData: {index, mapId}` | `loopData: {index, loopId}` |
| Bottom ghost data | `taskDispatcherId` only, **no eachId** | `taskDispatcherId` only, **no mapId** | same quirk (no loopId) |
| Body | **exactly one** iteratee task | sequential chain (iteratee array) | sequential chain |

Key verified facts:

- **No camelCase id segment mapping needed.** Unlike `fork-join` → `forkJoin`, the
  segments are plain `each` / `map` (`bottomGhostIdForDispatcherTask` passes them
  through verbatim; `createEachNode.ts` / `createMapNode.ts` confirm).
- **The rail id is the generic `-taskDispatcher-left-ghost` suffix** shared with loop —
  the engine's `LEFT_GHOST_ID_SUFFIX` and rail fixups already match it. The
  `createLeftGhost` option exists in both node-options types but is dead code; the rail
  is always created.
- **Each is single-child by UI affordance, not by layout contract**: the iteratee is one
  `WorkflowTask` (which MAY be a nested dispatcher — `createEachEdges` explicitly wires
  a dispatcher iteratee's bottom ghost to each's bottom ghost). All each-internal edges
  are `smoothstep` (no "+" insert buttons), while map uses `workflow`-type edges with
  insert buttons like loop. The layout engine lays out whatever chain edges exist and
  does not enforce the limit.
- **Dagre has zero each/map-specific layout rules.** `postDagreConstraints.ts` keys
  every relevant constraint (rail capping, ghost alignment, placeholder centering,
  chain centering) on `node.type` + `taskDispatcherId`; `eachData.eachId` /
  `mapData.mapId` appear only in the parent-derivation chains and
  `collectNestedDispatcherNodes` membership. No sizing special cases.
- **Each's `smoothstep` edge typing is invisible to layout.** The engine reads only
  `edge.source`/`edge.target` (verified: zero reads of `type`/`sourceHandle`/
  `targetHandle` in `elkLayoutUtils.ts`), and `computeEdgeButtonPosition` only runs
  for `workflow`-type edges — so each's button-less interior needs nothing from the
  engine.
- **Pre-existing dagre-side bug found (masked, out of scope here)**: `createEachEdges`
  inlines its nested-dispatcher-child wiring with a naive
  `${childTaskId}-${childTaskId.split('_')[0]}-bottom-ghost` id, which is wrong for
  `fork-join`/`on-error` children (real ghost ids use camelCase `forkJoin`/`onError`
  segments — the pattern `createOnErrorEdges` gets right). The dangling edge is
  silently dropped today and the generic
  `createEdgeFromTaskDispatcherBottomGhostNode` pass supplies the correct
  continuation, so BOTH engines lay out correctly — but that surviving generic edge
  is `workflow`-typed, so a stray "+" button renders on the merge edge inside an
  otherwise button-less each frame. Tracked as a separate fix.

## Design

Fourth repetition of the frame-mapper family; the loop mapping applies wholesale.

1. **`isElkLayoutSupported.ts`**: add `'each'` and `'map'` to
   `ELK_FRAME_DISPATCHER_COMPONENT_NAMES` (alphabetical:
   `['branch', 'condition', 'each', 'fork-join', 'loop', 'map', 'parallel']`).
   Refresh the stale doc comment (it still lists branch/fork-join/parallel as
   unsupported); remaining unsupported: `on-error`, cluster roots.
2. **`getOwningDispatcherId`** in `elkLayoutUtils.ts`: add `eachData?.eachId` and
   `mapData?.mapId` to the child-ownership fall-through chain, AFTER the
   `taskDispatcherId !== node.id` aux-node branch (mirroring the existing
   `loopData` entry). **This must ship in the same commit as item 1**: with the
   gate widened but the ownership chain unchanged, each/map children scope to the
   ELK root — buildElkGraph emits a root-scope cycle, the child renders outside
   its frame, and nothing throws, so there is no dagre fallback; the layout is
   silently garbled.
3. **Nothing else in the engine changes.** Verified generic coverage:
   - Ghost ids: `getGhostIdSegment` passes `each`/`map` through unchanged.
   - Rail: presence-driven square-ring logic (`dispatcherHasRail`) and the rail
     position fixup key on node type + `taskDispatcherId`.
   - Empty frame: placeholder mid-centering + square-ring pin are presence-driven.
   - Ranking: single-body frames have one entry; `getMemberCaseRank` order is
     irrelevant, as with loop.
   - Chain centering (2026-07-11 tallest-chain rule), top-bar pull, bottom-bar exit
     extension, entry-axis frame centering: all keyed on frame membership generically.
4. **Toolbar test fixture migration**: the "unsupported dispatcher" fixture in
   `WorkflowEditorToolbar.test.tsx` uses `each_1`; migrate to an `on-error` dispatcher.
5. **`isElkLayoutSupported.test.ts`**: move `each`/`map` from the rejects loop into
   supported cases; rejects keeps `on-error`.

## Testing

Engine tests in `elkLayoutUtils.test.ts` (real elkjs, per suite convention):

- **Map parity with loop**: populated map (2-child chain) reproduces loop's exact gap
  set — BAR_TO_CHILD_GAP entry, CHAIN_STEP between children, BOX_GAP exit, rail
  present, TB and LR.
- **Each single child**: one iteratee task centered on the dispatcher axis with the
  designed gaps; rail present.
- **Empty each / empty map**: square ring per direction (placeholder pinned at
  half-span right, rail mirrored left), reusing the loop empty-ring assertions.
- **Nesting both ways**: each inside a condition branch keeps branch-side placement;
  a loop as each's iteratee (dispatcher-as-iteratee) chains through the nested bottom
  ghost with uniform merge stubs.
- **Fork-join as each's iteratee**: layout stays intact when the edge list contains
  createEachEdges' dangling `-fork-join-bottom-ghost` edge alongside the correct
  generic `-forkJoin-bottom-ghost` continuation (the engine must drop the dangling
  one and walk the real one).
- Support-gate + toolbar tests per items 4–5.

## Out of scope

- `on-error` (next phase; condition-shaped, TRY/CATCH labels).
- Cluster roots — AI agent, data stream, and approval components (any task the
  server flags `clusterRoot`); these need a cluster-element abstraction, not a
  frame mapper. The support gate already rejects them generically by node type.
- Any change to each's single-child UI affordance or its `smoothstep` edge styling.
- The dead `createLeftGhost` options in `createEachNode.ts`/`createMapNode.ts`.
- The `createEachEdges` nested-ghost-id bug and its stray "+" button (separate fix;
  use `createOnErrorEdges`' segment-mapping pattern).
