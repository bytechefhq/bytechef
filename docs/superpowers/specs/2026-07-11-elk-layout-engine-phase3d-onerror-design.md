# ELK Layout Engine Phase 3d: On-Error Dispatcher Support

**Date:** 2026-07-11
**Status:** Approved (user: "implement all missing one by one")
**Depends on:** Phases 1–3c (condition, loop, branch, parallel, fork-join, each, map)

## Goal

Add the `on-error` task dispatcher to the ELK engine as a compound frame. After
this phase, cluster roots (AI agent, data stream, approval) are the only
unsupported shape.

## Ground truth (verified in `createOnErrorNode.ts` / `createOnErrorEdges.ts`)

Condition-shaped, two fixed sides — mainBranch (left, TRY) and onErrorBranch
(right, CATCH):

- **Aux ids use the camelCase `onError` segment**: `${id}-onError-top-ghost`,
  `${id}-onError-bottom-ghost`, placeholders
  `${id}-onError-left-placeholder-0` (mainBranch) and
  `${id}-onError-right-placeholder-0` (onErrorBranch). The engine's
  `GHOST_ID_SEGMENT_BY_COMPONENT_NAME` must gain `'on-error': 'onError'`
  (previously only `fork-join → forkJoin`).
- **No rail** — plain condition-family frame; the ring grammar does not apply.
- **Bottom ghost carries `onErrorId`** (no loop-style quirk), plus
  `taskDispatcherId` — aux scoping works either way.
- **Child data**: `onErrorData: {index, onErrorCase, onErrorId}` with
  `onErrorCase ∈ {'mainBranch', 'onErrorBranch'}`; placeholders carry top-level
  `onErrorCase` + `onErrorId` + `taskDispatcherId`.
- **Edges** use the bars' `-left`/`-right` side handles per branch, exactly like
  condition; `createOnErrorEdges` already maps nested bottom-ghost segments
  correctly (the pattern `createEachEdges` lacks).

## Design (5th member of the frame family)

1. `ELK_FRAME_DISPATCHER_COMPONENT_NAMES` += `'on-error'`; the unsupported set
   shrinks to cluster roots only.
2. `GHOST_ID_SEGMENT_BY_COMPONENT_NAME` += `'on-error': 'onError'`.
3. `getOwningDispatcherId` += `onErrorData?.onErrorId`.
4. `getMemberCaseRank`: explicit on-error scope branch — `mainBranch` → 0,
   `onErrorBranch` → 1 (children via `onErrorData.onErrorCase`, placeholders via
   the top-level field; `NodeDataType` gains optional `onErrorCase`).
5. Everything else is generic: two-entry mean anchoring, top-bar label pull
   (TRY/CATCH labels hang like TRUE/FALSE), case-placeholder mid-centering,
   chain centering, separation/repack.
6. Toolbar "unsupported" fixture migrates from `on-error` to a cluster root.

## Testing

- Populated both sides: mainBranch chain LEFT of onErrorBranch chain, uniform
  gaps (TOP_BOX_GAP / BAR_TO_CHILD_GAP / CHAIN_STEP / BOX_GAP), parent on the
  entry mean, scope-violation-free ELK graph.
- One empty side: placeholder mid-frame on its own side.
- Nested: on-error inside a loop body (ring grammar around it), and a condition
  inside the mainBranch.
- Support gate + toolbar fixture updates.

## Out of scope

- Cluster roots (aiAgent, dataStream, approval) — next and last phase, needs a
  cluster-element abstraction.
