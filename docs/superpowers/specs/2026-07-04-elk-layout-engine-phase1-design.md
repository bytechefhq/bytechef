# ELK Layout Engine for Workflow Editor — Phase 1 Design

**Date:** 2026-07-04
**Status:** Approved
**Scope:** Client-side only (`client/`)

## Problem

The workflow editor computes node positions with dagre (`getLayoutElements()` in
`client/src/pages/platform/workflow-editor/utils/layoutUtils.tsx`). Dagre is a global,
layered solver: it optimizes relative node placement across the whole graph and does not
preserve consistent local spacing. As workflows grow complex — especially with nested
condition dispatchers — node gaps drift, whitespace balloons, and a 2,859-line
post-processing file (`postDagreConstraints.ts`, 20+ correction functions) manually
patches positions after every layout run.

Goal: node and edge spacing must be **identical in every situation**, at every nesting
depth, in both TB (top-bottom) and LR (left-right) directions. A condition nested five
levels deep must look pixel-identical to one at the top level.

## Decision

Build a parallel layout engine on **elkjs** (ELK `layered` algorithm) selected by a
visible switch button in the workflow editor toolbar. Dagre remains the default and is
untouched. Phase 1 supports:

- Plain task nodes (linear chains), triggers, trigger placeholders.
- The **condition** task dispatcher, nested to arbitrary depth.

All other dispatchers (loop, branch, each, parallel, fork-join, map, on-error) and
AI-agent cluster roots are out of scope; workflows containing them disable the switch.

### Why ELK solves the consistency problem

The ELK graph is built as a **hierarchy**: each condition contributes a synthetic
compound "frame" node whose children (ghosts, placeholders, branch chains, nested
frames) are laid out as an independent sub-graph. With ELK's default
`SEPARATE_CHILDREN` hierarchy handling, every frame interior is solved locally with the
same spacing options — consistency at depth N is structural, not patched in afterwards.
Dagre cannot do this because it only sees one flat graph.

## Architecture

### 1. Engine toggle

**New store** `client/src/pages/platform/workflow-editor/stores/useLayoutEngineStore.ts`,
modeled on `useLayoutDirectionStore.ts`:

- `layoutEngine: 'dagre' | 'elk'`, default `'dagre'`.
- Single **global** value (not per-workflow) — this is a development comparison switch,
  not a user preference. Persisted to localStorage key `bytechef.layout-engine`.

**New toolbar button** in
`client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.tsx`, next to
the existing TB/LR direction toggle, using the same `Tooltip + Button` pattern.

- Enabled: toggles `layoutEngine` between `dagre` and `elk`; tooltip names the active
  engine.
- Disabled (with tooltip "Experimental layout supports only Condition for now") when
  `isElkLayoutSupported(tasks)` is false.

**Support detection** — new helper `isElkLayoutSupported(tasks: WorkflowTask[])`:
recursively walks tasks (including condition `caseTrue`/`caseFalse` subtasks) and
returns false if any task is a non-condition dispatcher or a cluster root. If ELK is
selected but the workflow is (or becomes) unsupported, layout silently runs through the
dagre path and the button renders disabled — no dead-end state.

### 2. Engine selection point

`useLayout.tsx` (currently line ~952) calls `getLayoutElements({...})`. The change is a
single branch:

```ts
const layoutFn = layoutEngine === 'elk' && elkSupported ? getElkLayoutElements : getLayoutElements;

layoutFn({canvasHeight, canvasWidth, direction, edges, nodes, savedPositionCrossAxisShift})
```

`layoutEngine` is added to the layout effect's dependency array so toggling re-lays out
immediately (with the existing position animation for unchanged structures).

Everything upstream — node creation (`createConditionNode`, ghosts, placeholders),
edge creation (`createConditionEdges`), `NodeDataType` — is **unchanged**. Both engines
receive the identical flat node/edge lists and return the identical
`{nodes: Node[], edges: Edge[]}` contract. The read-only renderer
(`useReadOnlyWorkflow` → `useLayout`) gets the ELK path for free.

### 3. ELK layout module

**New file** `client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.ts`
exporting `getElkLayoutElements(props: GetLayoutElementsProps)`.

`elkjs` is added as a dependency and loaded via dynamic `import('elkjs/lib/elk.bundled.js')`,
mirroring how dagre is lazily loaded — zero bundle cost until the switch is used. No web
worker in phase 1.

#### 3a. Hierarchy builder (flat nodes → ELK tree)

Input: the flat ReactFlow node list (tasks, ghosts, placeholders) plus edges — the same
arrays dagre receives.

Grouping rule: a node belongs to condition frame `C` when its
`data.conditionData.conditionId === C`, or it is one of C's auxiliary nodes (top ghost,
bottom ghost, case placeholders — identifiable by their `C`-prefixed IDs and
`taskDispatcherId`). For each condition, the builder emits a synthetic ELK compound node
`` `${conditionId}__frame` `` containing those members; nested conditions recurse — the
inner condition's task node *and* its frame both become children of the outer frame.

**The condition task node stays outside its own frame**, as the frame's sibling:
`…prev → conditionNode → frame → next…`. This keeps every scope a plain chain of boxes
and avoids ELK ports and cross-hierarchy edges entirely (they are ELK's least
deterministic feature). ReactFlow draws the real edges; ELK is used only for positions.

#### 3b. Edge scoping

Real edges are remapped per scope: an edge endpoint that lives inside a frame is
represented *by that frame* at the parent scope. Edges whose two representatives are the
same node (fully internal to a deeper scope) are handled at that deeper scope. Duplicate
remapped edges are deduped. Inside a frame, the local edge set is
`topGhost → firstChainNode → … → bottomGhost` per branch (or `topGhost → placeholder →
bottomGhost` for an empty branch), derived from the same remapping.

Result: no edge in the ELK graph ever crosses a hierarchy boundary, so ELK's default
`SEPARATE_CHILDREN` handling applies and each frame interior is an independent solve.

#### 3c. ELK options

| Option | Value | Rationale |
|---|---|---|
| `elk.algorithm` | `layered` | Same family as dagre; suits DAG flows |
| `elk.direction` | `DOWN` (TB) / `RIGHT` (LR) | From `useLayoutDirectionStore` |
| `spacing.nodeNode` | `50` | Today's `nodesep: 50` |
| `layered.spacing.nodeNodeBetweenLayers` | `50` | Today's effective rank gap |
| `elk.padding` (frames) | minimal, uniform | Frames are invisible; padding must not distort gaps |

Applied identically at every hierarchy level — that uniformity is the entire point.

Node sizes come from a shared size function extracted from today's
`getDagreNodeSize()` in `layoutUtils.tsx` so both engines agree on dimensions
(NODE_WIDTH 240, NODE_HEIGHT 100, placeholder 28, ghost minimal, trigger 160 footprint —
all existing constants from `shared/constants.tsx`; **no new spacing values** in
phase 1).

#### 3d. Post-layout mapping

1. Flatten ELK's parent-relative coordinates to absolute canvas coordinates (recursive
   offset accumulation).
2. **Drop the synthetic frame nodes** — they exist only for layout and are never
   rendered.
3. Apply the same canvas-centering used by the dagre path (center the trigger row on the
   canvas cross-axis).
4. Reuse the existing `applySavedPositions()` (imported from `postDagreConstraints.ts`)
   so `metadata.ui.nodePosition` manual drags are honored identically.
5. A small deterministic fixup pass is permitted (target ≤ ~100 lines): e.g. centering a
   condition's top/bottom ghost on the cross-axis midpoint of its branch chains, and
   centering the condition node on its frame. Nothing else from
   `postDagreConstraints.ts` runs on the ELK path.

Sticky notes remain excluded from layout on both paths (decorative, user-positioned).

### 4. Extension pattern for later phases

The hierarchy builder's core abstraction is dispatcher-agnostic: *a dispatcher = a frame
containing top-ghost + one or more parallel chains + bottom-ghost*. Later phases add a
per-dispatcher mapper ("which child-task chains does this dispatcher own"), which
`collectTaskDispatcherData()` already answers:

- Loop / Each / Map → 1 chain
- Branch → N case chains
- Parallel / Fork-Join → N chains
- On-Error → 2 chains

No new layout machinery per phase; `isElkLayoutSupported` widens as mappers land.

## Files

| Action | File |
|---|---|
| Add | `stores/useLayoutEngineStore.ts` |
| Add | `utils/elkLayoutUtils.ts` (hierarchy builder + ELK invocation + mapping) |
| Add | `utils/isElkLayoutSupported.ts` |
| Modify | `components/WorkflowEditorToolbar.tsx` (switch button) |
| Modify | `hooks/useLayout.tsx` (engine branch + effect dep) |
| Modify | `utils/layoutUtils.tsx` (extract shared node-size function) |
| Modify | `client/package.json` (`elkjs` dependency) |

(All paths relative to `client/src/pages/platform/workflow-editor/` unless noted.)

## Error handling

- ELK layout failure (rejected promise / thrown): log via console error, fall back to
  the dagre result for that run so the canvas never renders unpositioned nodes.
- Unsupported workflow while ELK selected: silent dagre fallback + disabled button (see
  §1); the persisted engine choice is retained so a supported workflow uses ELK again.

## Testing

- **Hierarchy builder unit tests** (`elkLayoutUtils.test.ts`): fixtures for linear
  chain; single condition (both branches populated); empty-branch condition
  (placeholders); condition nested 3 deep. Assert the emitted ELK tree shape and edge
  scoping (no cross-hierarchy edges).
- **Spacing invariance tests**: after layout, the main-axis gap between consecutive
  nodes equals the constant at depth 0 and depth 3, in both TB and LR.
- **`isElkLayoutSupported` tests**: plain tasks → true; nested conditions → true; any
  other dispatcher (top-level or nested inside a condition branch) → false.
- **Store test** mirroring `useLayoutDirectionStore.test.ts`.
- **Toolbar test**: button toggles engine; disabled + tooltip when unsupported.
- Standard gates: `npm run check` in `client/`.

## Out of scope (phase 1)

- All dispatchers other than condition; AI-agent cluster roots.
- Removing dagre or `postDagreConstraints.ts`.
- Web-worker ELK execution.
- New spacing values or visual redesign of nodes/edges.
- Server-side changes (none anywhere in this feature).
