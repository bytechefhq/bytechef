# ELK Layout Engine Phase 2 — Loop Dispatcher

**Date:** 2026-07-06
**Status:** Approved
**Extends:** `2026-07-04-elk-layout-engine-phase1-design.md`
**Scope:** Client-side only; loop dispatcher (each/map deferred to phase 3)

## Decision

Add the `loop` task dispatcher to the ELK layout engine, composable in both
directions: any supported dispatcher (condition, loop) nests inside a loop body,
and loops nest inside condition branches and other loops, to arbitrary depth.
Childless dispatchers (`loopBreak`, `subflow`, `terminate`) become supported as
plain chain nodes — they carry `taskDispatcher: true` but own no children and
lay out like ordinary tasks.

## Loop frame shape

A loop is a two-chain frame, structurally a condition with a skinny left branch:

- Members: top ghost (`<id>-loop-top-ghost`), **left rail ghost**
  (`<id>-taskDispatcher-left-ghost`, type `taskDispatcherLeftGhostNode` — the
  loop-back visual), the content chain (or `<id>-loop-placeholder-0` when
  empty), bottom ghost (`<id>-loop-bottom-ghost`).
- Interior edges already exist in the real edge set: `top→leftGhost→bottom`
  (rail) parallel to `top→children→bottom` (content). Model order keeps the
  rail left of the content chain.
- Data quirk: the loop bottom ghost carries only `taskDispatcherId`, no
  `loopId` — ownership detection must not rely on the per-dispatcher id field.

## Changes to the ELK module

1. **Generic ownership** — `getOwningConditionId` becomes
   `getOwningDispatcherId`: an aux node belongs to `taskDispatcherId` when that
   differs from the node's own id; child tasks belong via
   `conditionData.conditionId || loopData.loopId`. Cycle guards and the
   orphan→root fail-safe carry over unchanged.
2. **Frame builder** — frames are created for `taskDispatcher` nodes whose
   `componentName` ∈ {condition, loop}. Case ranking is unchanged (loop members
   rank neutral; a loop node inside a condition branch inherits its branch rank
   via `conditionData`, so TRUE/FALSE ordering still holds).
3. **Ghost id resolution** — fixups derive ghost ids per dispatcher kind:
   `<id>-<componentName>-top|bottom-ghost`.
4. **Entry-axis centering** — rail targets (ids ending
   `-taskDispatcher-left-ghost`) are excluded, so a loop frame centers on its
   content chain: content sits directly under the loop node, rail hangs left.
5. **Sizing** — left rail ghost gets a narrow cross-axis footprint (72px column
   instead of the 240px task default) and its rendered DOM size in
   `getRenderedNodeSize`; exact values read from `TaskDispatcherLeftGhostNode`.
6. **Top-bar pull is condition-only** — the 28px `TOP_BAR_LABEL_PULL` exists to
   attach TRUE/FALSE labels; loops have none, so loop boxes keep symmetric
   66/66 box gaps.
7. **Mid-frame vertical centering generalizes** — any placeholder or left rail
   ghost owned by the dispatcher (via `taskDispatcherId`) centers midway
   between the frame's bars, replacing the condition-case-only rule.
8. **Trailing "+" pin generalizes** — bottom-ghost detection matches any
   `-bottom-ghost` id suffix, not only `-condition-bottom-ghost`.
9. **Support detection** — `isElkLayoutSupported` accepts condition, loop, and
   the childless dispatcher names; everything else still disables the toggle
   with the existing tooltip (copy updates to "Condition and Loop").

## Out of scope

each, map, branch, parallel, fork-join, on-error, AI-agent cluster roots;
engine default change; dagre-path changes.

## Testing

Real-elkjs unit tests alongside the phase-1 suite: empty loop (66/66 gaps,
placeholder mid-frame, rail left of content), populated loop (content chain
centered under the loop node, CHAIN_STEP inside the body), loop inside a
condition branch (branch side preserved), condition inside a loop, loop in loop
(three deep, gap invariance), loopBreak as plain chain node, widened support
detection, loop→enclosing merge stub at the box gap.
