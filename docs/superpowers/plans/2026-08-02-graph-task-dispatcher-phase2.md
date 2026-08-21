# Graph Task Dispatcher — Phase 2 (Editor Structural Support) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Author a `graph/v1` container fully on the canvas — add/name/delete nodes, drop tasks into node lanes, edit per-node `next` expressions, see transition badges — WITHOUT rendered transition edges (those are phase 3).

**Architecture:** Extend the client's per-type dispatcher machinery: the `TASK_DISPATCHER_CONFIG` registry + the hardcoded traversal/serialization/paste/delete sites, a new `createGraphNode`/`createGraphEdges` chain-mapper pair rendering one sub-lane per node (fork/join-column style), ELK frame tables, `BranchCaseLabel`-style inline name chips, and a properties-panel states list for `next` expressions.

**Tech Stack:** React 19 / TypeScript, ReactFlow, ELK, Zustand, Vitest.

**Spec:** `docs/superpowers/specs/2026-08-02-graph-task-dispatcher-design.md` (phase 2 section). Server definition already ships `nodes: array(object(name, next[FORMULA_MODE, optional], tasks))` — the runtime shape `parameters.nodes[*].{name,next,tasks}` is the single source of truth; phase 1's `graph_v1.json` snapshot documents it.

**WORKTREE:** ALL work in `/Volumes/Data/bytechef/bc-graph-wt` (branch `graph-dispatcher`), client work in its `client/` (npm install already run — verify `client/node_modules` exists before starting; if absent run `npm install` first). NEVER touch `/Volumes/Data/bytechef/bytechef`.

## Global Constraints

- Client conventions (CLAUDE.md, all enforced by lint): sort-keys alphabetical (NOT auto-fixed), interfaces end `I`/`Props`, named imports sorted, lucide icons `Icon` suffix, `twMerge` not `cn`, hook ordering, `useMemo` not IIFE, `||` for JSX fallbacks, refs end `Ref`, vi.hoisted for factory-injected mocks.
- MANDATORY before every commit: `cd client && npm run format && npm run check` — redirect output, `echo "exit=$?"` on its own line.
- Commit prefix: `client - <description>` (server convention with client marker; no ticket number).
- The research inventory (file:line refs may have drifted — re-locate by symbol): registry `TASK_DISPATCHER_CONFIG` in `src/pages/platform/workflow-editor/utils/taskDispatcherConfig.tsx`; constants `TASK_DISPATCHER_NAMES` / `TASK_DISPATCHER_SUBTASK_COLLECTIONS` / `TASK_DISPATCHER_DATA_KEY_MAP` in `src/shared/constants.tsx`; hardcoded sites `useLayout.tsx` (node+edge chains), `elkLayoutUtils.ts` (`ELK_FRAME_DISPATCHER_COMPONENT_NAMES` via `isElkLayoutSupported.ts`, `getMemberCaseRank`, `GHOST_ID_SEGMENT_BY_COMPONENT_NAME`), `taskTraversalUtils.ts`, `getTask.ts`, `saveTaskDispatcherSubtaskFieldChange.ts`, `pasteNode.ts`, `handleDeleteTask.ts`; precedents `createForkJoinNode.ts`/`createForkJoinEdges.ts`, `useBranchCaseLabel.ts`/`BranchCaseLabel.tsx`.

---

### Task 1: Registry + round-trip plumbing

**Files:**
- Modify: `taskDispatcherConfig.tsx` (full `graph` entry), `src/shared/constants.tsx` (`TASK_DISPATCHER_NAMES` + `TASK_DISPATCHER_SUBTASK_COLLECTIONS['nodes']` + `TASK_DISPATCHER_DATA_KEY_MAP['graphData']`), the `NodeDataType` shape (find it — likely `src/shared/types.ts`), `taskTraversalUtils.ts`, `getTask.ts`, `saveTaskDispatcherSubtaskFieldChange.ts` (a `graphData` switch arm), `pasteNode.ts` (deep-clone `nodes`), `handleDeleteTask.ts` (graph delete path).
- Test: Vitest round-trip tests colocated per existing patterns.

**Interfaces:**
- Produces: `TASK_DISPATCHER_CONFIG.graph` with `getSubtasks({context}) → nodes[nodeIndex].tasks`, `updateTaskParameters`, `initializeParameters() → {maxTransitions: 100, nodes: []}`, `extractContextFromPlaceholder(placeholderId) → {taskDispatcherId, nodeIndex}` — placeholder id convention `<graphId>-graph-node-<index>-placeholder-<pos>` (mirror fork/join's naming EXACTLY, substituting the segment).
- Graph node addressing is by INDEX into `nodes` (names are display/runtime identity; the editor addresses lanes positionally like fork/join branches — a rename must not re-home tasks).

- [ ] **Step 1: Read the fork/join entry in `taskDispatcherConfig.tsx` top to bottom** plus one insert flow end-to-end (`getTaskDispatcherContext` → `saveWorkflowDefinition` → `insertTaskDispatcherSubtask`). Then write failing round-trip tests: insert first task into empty graph (creates `nodes[0]` shell? NO — a graph node needs a NAME; decide: inserting into the trailing add-node placeholder creates `{name: 'node_<n>', tasks: [task]}` with a generated unique name), insert into existing node, delete last task of a node (node SURVIVES empty — it may be a router; deletion of the NODE itself is Task 4's chip UI), paste a graph (deep-cloned nodes, no shared refs).
- [ ] **Step 2: Implement the registry entry + constants + the five hardcoded sites.** `flattenDefinitionTasks` already handles `{tasks} in item` shapes generically (research finding) — verify with a test rather than re-implementing.
- [ ] **Step 3: `npm run check` green; commit** `"client - Register the graph dispatcher in the editor round-trip plumbing"`.

---

### Task 2: Canvas chain mappers

**Files:**
- Create: `utils/createGraphNode.ts`, `utils/createGraphEdges.ts`
- Modify: `hooks/useLayout.tsx` (both if/else chains)
- Test: mirror the fork/join mapper tests if they exist; otherwise pin via layout-level tests in Task 3.

**Interfaces:**
- Consumes Task 1's placeholder-id convention verbatim.
- Produces: one vertical sub-lane per node (fork/join-column layout), each lane: entry point, the node's task chain, per-lane bottom placeholder; plus one trailing add-node placeholder column; top/bottom ghosts for the container per the fork/join shape.

- [ ] **Step 1: Read `createForkJoinNode.ts` + `createForkJoinEdges.ts` completely.** Copy-adapt: lanes come from `nodes[*].tasks` instead of `branches[*]`; the trailing placeholder creates a NEW node (Task 1's insert semantics).
- [ ] **Step 2: Wire both `useLayout.tsx` chains** (node creation + edge creation).
- [ ] **Step 3: Manual smoke** via `npm run dev` against a dev server if available; otherwise rely on tests + Task 3's layout pins. `npm run check` green; commit `"client - Render graph nodes as canvas lanes with per-node placeholders"`.

---

### Task 3: ELK frame integration

**Files:**
- Modify: `isElkLayoutSupported.ts` (`ELK_FRAME_DISPATCHER_COMPONENT_NAMES` + `graph`), `elkLayoutUtils.ts` (`getMemberCaseRank` graph arm — rank by node INDEX, N-ary; the condition-binary fallback must not apply), ghost-segment + ring tables as the fork/join entries require.
- Test: the ELK frame-geometry pinning tests (find the existing pattern — the ELK work has geometry tests; pin graph frames incl. saved-position behavior).

- [ ] **Step 1: Read `getMemberCaseRank` fully** — understand rank semantics for fork/join's N branches; graph's arm mirrors it keyed on the `__node`-derived lane index (however the client tags lane membership — follow fork/join's mechanism exactly).
- [ ] **Step 2: Implement + pin.** CRITICAL check: `isElkLayoutSupported` — omitting `graph` silently disables ELK for the whole workflow; the test must pin that a graph-containing workflow IS ELK-supported.
- [ ] **Step 3: `npm run check` green; commit** `"client - Add graph frames to the ELK layout engine"`.

---

### Task 4: Node name chips, next-expression panel, transition badges

**Files:**
- Create: graph node-name chip components (adapt `BranchCaseLabel.tsx` + `useBranchCaseLabel.ts` — inline edit, add node, delete node; validation: unique, non-empty; DELETE of a node with tasks prompts like fork/join's branch-delete path)
- Create: the graph states editor in the properties panel — when the graph CONTAINER node is selected, render the node list, each with name (read-only here; chips own renames) + a `PropertyMentionsInput` for `next` (empty = terminal; helper text says so)
- Create: transition badges — a small badge on each lane header showing targets extracted from that node's `next` expression (extract single-quoted string literals matching declared node names; anything else → a "dynamic" badge). Pure display; the extraction util gets its own unit tests (it is phase-3's edge-derivation seed — name it `extractNextTargets.ts` and keep it standalone).
- Test: chip validation, states-list rendering, extraction util, mutation round-trips (rename updates `nodes[i].name` AND any `next` literals referencing the old name? NO — renames do NOT rewrite other nodes' expressions in phase 2; show a warning badge on dangling targets instead. Pin this decision with a test.)

- [ ] **Step 1: Read `BranchCaseLabel`/`useBranchCaseLabel` + how the properties panel decides what to render for a selected dispatcher node.** Write failing tests per above.
- [ ] **Step 2: Implement.** Rename semantics per the pinned decision (dangling `next` targets get the warning badge, not silent rewrite).
- [ ] **Step 3: `npm run check` green; commit** `"client - Add graph node naming, next-expression editing and transition badges"`.

---

### Task 5: Gates + spec

- [ ] **Step 1:** Full client gate: `npm run format && npm run check`; server sanity: graph module `:test` (nothing server-side should have changed — verify with `git status`).
- [ ] **Step 2:** Spec: phase-2 status line + decisions log entries (lane-by-index addressing; empty node survives task deletion; renames don't rewrite `next` — dangling-target warning instead; badge extraction = single-quoted literals ∪ dynamic marker).
- [ ] **Step 3:** Commit `"Mark graph dispatcher phase 2 implemented"`.
