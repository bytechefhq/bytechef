# ELK Layout Engine Phase 4: Cluster Root Support

**Date:** 2026-07-11
**Status:** Approved (user: "implement all missing one by one")
**Depends on:** Phases 1–3d (all task dispatchers)

## Goal

Support cluster-root tasks — AI agent, data stream, approval, and any future
component the server flags `clusterRoot: true` — making every workflow shape
ELK-eligible. After this phase `isElkLayoutSupported` always returns true (the
gate stays in place for future shapes).

## Ground truth (verified)

The feared "radial abstraction" does not exist on the main canvas:

- A cluster-root task becomes **exactly one ReactFlow node** (type
  `clusterRoot`, rendered by `AiAgentNode` for all component kinds). Cluster
  ELEMENTS never become main-canvas nodes — they render as an icon row inside
  the root's DOM, and their own layout lives in the separate
  ClusterElementsCanvasDialog (own ReactFlow, own store, untouched here).
- No aux nodes (ghosts/placeholders); the chain enters a single top handle and
  exits a single bottom handle, continuing below like a plain node. Cluster
  roots may sit inside dispatcher frames like any task.
- Two DOM states: unconfigured = 72×72 icon box, handle at left 36; configured
  = ~240-wide button (elements icon row inside), handle at left **120** — the
  center of the rendered button in both states.
- Dagre cross footprints: TB 378 (240 button + 126 label + gap) configured /
  240 unconfigured; LR 292 / 120. Main: TB height 100 (generic); LR rendered
  main 240 / 72.

## Design — a plain chain node with bigger boxes

Because the chain handle sits at the CENTER of the rendered box in both
states, the engine's existing rule — center the rendered box inside the ELK
footprint — puts the handle exactly on the chain axis with no special handle
logic (unlike dagre's −85/−23 compensation offsets spread over 11 passes).

1. `getRenderedNodeSize`: `clusterRoot` branch — TB `{height: 100, width: 240}`
   configured / `{height: 72, width: 72}` unconfigured; LR
   `{height: 72, width: 240}` configured (the −23 dagre offset shows LR chain
   alignment targets the icon band, so cross stays the 72 anchor) /
   `{height: 72, width: 72}`.
2. `getElkNodeSize`: main-axis footprint = rendered main + 2×14 slack (128 TB
   configured, 268 LR configured, 100 otherwise) so gaps around roots keep the
   80px node→node read; cross footprints come from `getDagreNodeSize`
   automatically (378/240, 292/120).
3. `isElkLayoutSupported`: drop the `clusterRoot` rejection.
4. `hasConfiguredClusterElements` imported from `postDagreConstraints`
   (canonical predicate — do not duplicate it a seventh time).
5. Skipped deliberately: dagre's extra rank (`minlen 2`) below configured
   roots — the engine's uniform rhythm is the whole point.

## Testing

- Chain `task → configured root → task`: handle column straight
  (`root.x + 120 == task.x + 36`), 80px gaps both sides of the root's rendered
  box.
- Unconfigured root behaves exactly like a plain 72px task node.
- Configured root inside a condition branch keeps its handle on the branch
  column and the frame gaps uniform.
- Support gate: cluster-root workflows now ELK-eligible; toolbar switch stays
  enabled.
