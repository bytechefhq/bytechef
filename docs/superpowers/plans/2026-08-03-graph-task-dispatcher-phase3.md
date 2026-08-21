# Graph Task Dispatcher — Phase 3 (Transition Edges) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Real rendered transition edges for `graph/v1` — forward, backward (cycles), and self-loops — derived from `next` expressions, replacing the phase-2 badges as the primary visualization (badges remain for dynamic/dangling).

**Architecture (plan-level resolution to the spec, record in its decisions log):** transition edges are OVERLAY edges on the stable lane layout — lanes keep declaration-order ELK/dagre columns, transitions render as a distinct ReactFlow edge type anchored to lane header/footer handles, and are EXCLUDED from layout inputs and from every chain/containment walker. This avoids configuring elkjs cycle-breaking (the spec's original sketch): layout stays deterministic, cycles exist only at paint time. The considered alternative (edge-driven cyclic ELK layout) is rejected for destabilizing every frame-geometry invariant pinned in phase 2.

**WORKTREE:** ALL work in `/Volumes/Data/bytechef/bc-graph-wt` (branch `graph-dispatcher`), client only. Global constraints identical to the phase-2 plan (lint-enforced conventions; `npm run format && npm run check` before every commit; `client - ` commit prefix).

---

### Task 1: Tighten extraction + derive the edge model

**Files:**
- Modify: `extractNextTargets.ts` (+test — the documented limitation test FLIPS deliberately)
- Create: `utils/deriveGraphTransitionEdges.ts` (+test)

**Interfaces:**
- `extractNextTargets`: literals now count as targets ONLY in result positions — the whole expression is a bare quoted literal, or literals directly following `?` or `:` (nested ternaries recursively). Comparison operands (`== 'x'`) no longer surface as targets/dangling. Elvis `?: 'x'` counts (result position). Anything non-literal in a result position → `dynamic: true`.
- `deriveGraphTransitionEdges(nodes) → Array<{sourceIndex, targetIndex, kind: 'forward'|'back'|'self', dangling?: string}>` — one entry per (source node, extracted target); `kind` from index comparison; dangling literals produce NO edge (badge only). Pure function, exhaustively unit-tested (cycle pair, self-loop, multi-target ternary, dynamic-only node → no edges).

- [ ] Steps: failing tests for both utils (including the deliberate flip of the phase-2 limitation test, referencing the spec decision) → implement → `npm run check` → commit `"client - Derive graph transition edges from next expressions"`.

---

### Task 2: Render the overlay edges

**Files:**
- Create: the `graphTransition` edge component (distinct visual: curved, arrowhead, muted color; back-edges route around the frame side; self-loops as a small loop at the lane header; a label chip with the target name on hover/select)
- Modify: `createGraphEdges.ts` (append derived transition edges with `type: 'graphTransition'` + handles), the edge-type registry (wherever ReactFlow `edgeTypes` is declared), lane header/footer handle additions in the node components if needed
- Test: edge-set derivation through `createGraphEdges` (transition edges present with right kind/anchors; dagre path EXCLUDED — phase-2 badges remain the dagre visualization, transitions are ELK-only per the spec)

- [ ] Steps: read how existing custom edges register (the labeled edges from T2/T4) → failing tests → implement → check → commit `"client - Render graph transition edges as layout overlays"`.

---

### Task 3: Walker/layout isolation audit

**Files:** `collectDescendantNodes.ts`, `postDagreConstraints.ts`, `elkLayoutUtils.ts`, `useLayout.tsx` — audit + tests only (edits only where a walker would traverse a `graphTransition` edge)

- [ ] Verify every chain/containment walker filters by edge type or source/target-handle convention such that transition edges are never traversed (a back-edge entering a walker = infinite-visit risk the visited-sets currently mask); add an explicit type guard where reliance is implicit; pin with a test: a cyclic two-node graph's walkers terminate and return the same sets as without transitions. Verify ELK layout inputs exclude transition edges (they must not affect ranking). Commit `"client - Isolate graph transition edges from layout and traversal"`.

---

### Task 4: Gates + spec

- [ ] `npm run format && npm run check`; zero server diffs; graph module `:test` regression touch. Spec: phase-3 status, decisions log (overlay resolution + the extraction tightening flip + dagre-keeps-badges). Commit `"Mark graph dispatcher phase 3 implemented"`.
