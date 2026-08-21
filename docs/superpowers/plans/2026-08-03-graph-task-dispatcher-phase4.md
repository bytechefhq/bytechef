# Graph Task Dispatcher — Phase 4 (Agent Routing Polish + Observability) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make LLM-routed graphs ergonomic and observable: node-name autocomplete in `next` expressions, a per-node grouped transition timeline in the run view, and user docs with an agent-routed worked example. Closes the feature — the spec goes to Status: Implemented.

**WORKTREE:** ALL work in `/Volumes/Data/bytechef/bc-graph-wt` (branch `graph-dispatcher`). Global constraints identical to the phase-2/3 plans (client conventions lint-enforced; `npm run format && npm run check` per client commit; gradle discipline for any server touch; docs follow the released-version Coming Soon convention).

---

### Task 1: Node-name autocomplete in `next`

**Files:** the `GraphStatesPanel` `next` Property rendering + whatever feeds `PropertyMentionsInput` completion items (RESEARCH FIRST: how the mentions input sources its suggestion list — data pills come from prior-node outputs; find whether a per-property custom suggestion source exists or must be minimally added).

- [ ] Offer the graph's declared node names as completion items inside a `next` expression editor (quoted-literal insertion — picking `review` inserts `'review'`). Do NOT alter data-pill behavior for other properties; the mechanism must be scoped to the graph `next` property (a dedicated prop threaded from GraphStatesPanel is acceptable).
- [ ] The dangling/dynamic badges already cover "expression not tied to any node" — verify no additional warning surface is needed (the spec's "static-analysis warning" is satisfied by the existing badges; document this resolution in the report).
- [ ] Tests: suggestion list contains exactly the declared names minus the node's own name? (self-loops are legal — INCLUDE the own name; pin that decision); insertion format quoted.
- [ ] `npm run check`; commit `"client - Offer node names as completions in graph next expressions"`.

---

### Task 2: Run-view transition timeline

**Files:** RESEARCH FIRST: how the workflow execution detail view renders a run's task executions (find the execution sheet/panel component tree) and what the API returns per task execution (parameters incl. the `__node` stamp survive into the row? verify against a real IntTest fixture or the REST DTO).

- [ ] Group a graph's child task executions per node (the `__node` parameter), preserving execution order across visits: render as "node_x (visit 2)" style grouping or an ordered timeline section under the graph's row — follow the execution view's existing visual language (READ how map/loop iterations render today, if they group at all; mirror the closest precedent rather than inventing).
- [ ] Show visit counts and the transition order; budget consumed = transitions taken (derivable client-side from the ordered `__node` sequence — no server change; if the `__node` stamp does NOT survive into the execution rows, STOP, report the gap, and propose the minimal server-side surfacing rather than implementing blind).
- [ ] Tests per the execution-view's existing test patterns.
- [ ] `npm run check`; commit `"client - Group graph runs by node with a transition timeline"`.

---

### Task 3: User docs + agent-routed example

**Files:** `docs/content/docs/automation/build/flow-controls.mdx` (the hand-written flow-controls guide — the dispatcher return-values feature added Outputs notes there; graph gets its own section) + check whether a generated reference page appears automatically.

- [ ] Graph section: nodes/next/transitions/budget/terminal semantics, the outputs contract, Coming Soon treatment per the page's existing convention.
- [ ] The LLM-routed worked example: an AI Agent router node with a structured-output enum picking the next state, fallback + confidence-gating variants (from the spec's design discussion), presented as YAML + prose.
- [ ] `cd docs && npm run types:check`; commit `"Document the graph dispatcher with an agent-routed example"`.

---

### Task 4: Gates + spec close-out

- [ ] Full gates: client format+check; docs types:check; graph module `:test` + `:testIntegration`; zero unexpected server diffs.
- [ ] Spec: overall **Status: Implemented**; phase line "Phases 1-4 — implemented"; decisions log additions from Tasks 1-3 (own-name in completions for self-loops; badges satisfy the static-analysis-warning requirement; timeline derivation source).
- [ ] Commit `"Mark the graph dispatcher feature implemented"`.
