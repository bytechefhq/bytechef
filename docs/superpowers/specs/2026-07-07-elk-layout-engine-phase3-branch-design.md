# ELK Layout Engine Phase 3a — Branch Dispatcher

**Date:** 2026-07-07
**Status:** Approved
**Extends:** phase 1 (2026-07-04) and phase 2 loop (2026-07-06) specs
**Scope:** Client-side only; branch dispatcher

## Why branch first

Phase 3 sequencing (recorded in project memory): **branch → on-error → fork-join
→ parallel**. Branch is the only N-ary dispatcher that never has a loop-back
rail, so it isolates the one genuinely new problem — N-ary case-order recovery —
and delivers the generalized ranker that fork-join and parallel will reuse,
before their rail-coexistence complications. Fan-out *geometry* is already
solved: entry-mean centering, generic ghost machinery, and layer top-alignment
are N-ary by construction.

## Branch model facts (verified in code)

- Aux nodes (`createBranchNode`): `<id>-branch-top-ghost`,
  `<id>-branch-bottom-ghost`, and one placeholder per empty case:
  `<id>-branch-<caseKey>-placeholder-0` with data
  `{branchId, caseKey, label, taskDispatcherId}` — no ordinal.
- Child tasks carry `branchData {branchId, caseKey, index}` — `index` is the
  position within the case chain, NOT the case ordinal.
- **The case ordinal exists only in the dispatcher's parameters**
  (`createBranchEdges`): canonical left-to-right order is
  `['default', ...parameters.cases.map((c) => c.key)]`. `distributeCases`
  preserves this order; it only selects the middle case for edge-handle
  assignment (odd counts route the middle chain through the bar's center
  handles, sides through left/right handles).
- A completely empty branch gets a synthetic template case `case_0` in the edge
  set; the corresponding placeholder comes from `emptyCaseKeys`.
- Case labels are EDGE-rendered (`LabeledBranchCaseEdge`/`BranchCaseLabel`),
  not node-DOM like the condition's TRUE/FALSE — no new label geometry enters
  the node conversion.

## Engine changes

1. **Support**: `ELK_FRAME_DISPATCHER_COMPONENT_NAMES` gains `'branch'`;
   `getOwningDispatcherId`'s child fallback gains `branchData?.branchId`;
   toolbar tooltip copy becomes "Condition, Loop and Branch".
2. **Generalized ranker — the phase's deliverable.** `getConditionCaseRank`
   becomes a per-scope ranker: when building a scope's members,
   `buildScopeChildren` resolves the scope's dispatcher node
   (`nodesById.get(scope)`) and ranks members by:
   - condition: `caseTrue` → 0, `caseFalse` → 1 (unchanged);
   - branch: index of the member's `caseKey`
     (`data.caseKey || data.branchData?.caseKey`) in the canonical ordinal list
     derived from the dispatcher's `parameters` — the builder's first, and
     deliberately narrow, dependency on dispatcher params;
   - unknown/missing keys rank last, stable (fail-safe for malformed state);
   - aux ghosts rank first as today. Nested dispatcher frames inherit their
     dispatcher node's rank (unchanged mechanism).
   Array-order recovery is explicitly NOT a fallback: empty-case placeholders
   are created before all chain tasks, so mixed empty/populated cases break
   array order in shared layers — the same trap the condition's lone FALSE
   placeholder had.
3. **Geometry**: no rail, so no rail fixup fires. Entry-mean centering,
   per-case placeholder mid-frame centering (via `taskDispatcherId`), the 38px
   top-bar pull, 66px bottom gap, 80px exits, and merge stubs all apply
   unchanged. With symmetric column footprints the entry mean puts an
   odd-count branch's middle chain on the dispatcher axis, matching dagre's
   middle-case handle routing.
4. **Composability**: branch children may be any supported dispatcher and
   branches nest inside condition/loop bodies — ownership and scope machinery
   are already generic.

## Out of scope

on-error, fork-join, parallel, each/map (later phases); AI-agent cluster roots
(radial — needs a new abstraction, not a frame mapper); dagre-path changes.

## Testing

Real-elkjs tests alongside the existing suite:
- Builder: 3-case branch (default + 2 custom, one empty) — frame membership,
  no cross-hierarchy edges, and member emission order follows the canonical
  ordinal (default first), not array order.
- Layout: column centers strictly ascending in canonical order left-to-right;
  the ordering trap fixture (empty first case + populated later case, created
  placeholder-first) stays correctly ordered; odd count → middle chain on the
  dispatcher axis (±1px); entry-mean centering with 3 columns; 38/66/80 gap
  invariants at branch depth; branch-in-loop and loop-in-branch-case.
- Ranker unit cases: unknown caseKey ranks last; missing params ranks stable.
- Support detection and tooltip copy updates.
