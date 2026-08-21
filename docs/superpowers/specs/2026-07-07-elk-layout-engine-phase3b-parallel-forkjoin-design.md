# ELK Layout Engine Phase 3b — Parallel and Fork-Join Dispatchers

**Date:** 2026-07-07
**Status:** Approved
**Extends:** phase 1, phase 2 (loop), phase 3a (branch) specs
**Scope:** Client-side only; parallel + fork-join dispatchers

## The shape-shift, verified

Both dispatchers change geometry by child count (`useLayout`:
`createLeftGhost: !hasSubtasks`):

- **Empty**: loop-shaped — top bar, loop-back rail
  (`<id>-taskDispatcher-left-ghost`), one "+" placeholder, bottom bar.
- **Populated**: branch-shaped — N columns plus an ALWAYS-present trailing "+"
  column (parallel: `<id>-parallel-placeholder-0`, add-a-task; fork-join:
  `<id>-forkJoin-placeholder-<branchCount>`, add-a-branch), no rail. dagre's
  distribution runs over `length + 1` — the trailing placeholder participates
  as the last column, including in middle-case selection.

Column contents differ: a parallel child is a single task (its subtree hangs
below if it is a dispatcher); a fork-join branch is a chain.

## Ordering — intrinsic, no params dependency

Unlike branch, both carry their ordinals on the members: parallel children have
`parallelData.index`; fork-join children AND placeholders have
`forkJoinData.branchIndex` / `data.branchIndex` (the trailing placeholder gets
`branchCount`, ranking last naturally). The parallel "+" placeholder has no
index and ranks last explicitly.

## Id landmine

Fork-join ghost/placeholder ids use the camelCase segment `forkJoin`
(`<id>-forkJoin-top-ghost`), NOT the componentName `fork-join`. The engine's
ghost-id derivation gains a componentName→segment map (`fork-join` → `forkJoin`,
identity otherwise), applied in `getGhostIds`, the flatten entry-centering, and
the rail fixup's bottom-bar lookup.

## Engine changes

1. **Support**: `ELK_FRAME_DISPATCHER_COMPONENT_NAMES` gains `parallel` and
   `fork-join`; `getOwningDispatcherId` child fallback gains
   `parallelData?.parallelId || forkJoinData?.forkJoinId`. The unsupported
   tooltip becomes generic ("Experimental layout does not support this
   workflow yet") — the supported list is now too long to enumerate.
2. **Ranker**: per-scope extension of `getMemberCaseRank` — parallel scope
   ranks by `parallelData.index` (index-less placeholder last); fork-join scope
   ranks by `branchIndex` (member data or placeholder data). Ghost bars/rails
   rank first as always.
3. **Empty-ring generalization**: the square-ring placeholder pin (currently
   loop-only) keys on "this dispatcher has a rail" (a left ghost with matching
   `taskDispatcherId` exists) instead of `componentName === 'loop'` — empty
   parallel/fork-join render the same square ring as empty loops, and populated
   ones (no rail) keep their trailing placeholder as a real ELK column.
4. **Frame anchor**: unchanged median(odd)/mean(even) over entry columns —
   the trailing placeholder counts as a column, mirroring dagre's `+1`
   distribution and middle-handle routing.
5. Everything else (bars, gaps 38/66/80, merge stubs, mid-frame aux centering,
   trailing-"+" pin, alignment, saved positions) applies generically.

## Out of scope

on-error (next), each/map, AI-agent cluster roots, dagre-path changes.

## Testing

- Populated parallel: 3 tasks + trailing "+" = 4 columns, ordered by index with
  the placeholder last; even count → dispatcher on the column mean; standard
  gaps; a dispatcher child's subtree hangs inside its column.
- Empty parallel and empty fork-join: square ring (placeholder mirrored by the
  rail at half the bar-to-bar span), builder excludes the rail, camelCase
  fork-join ghost ids resolve.
- Fork-join: 2 chain branches + trailing placeholder, columns ordered by
  branchIndex, chains keep CHAIN_STEP inside columns; odd column count →
  median column on the dispatcher axis.
- Composability: parallel inside a condition branch or loop body.
- Support detection updates; toolbar fixture switches its "unsupported" sample
  to `each`.
