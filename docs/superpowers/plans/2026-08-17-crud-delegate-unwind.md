# CRUD Delegate Unwind — Implementation Plan (spec step 6)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dissolve the eight remaining CRUD-only delegates — `data_table_agent`,
`knowledge_base_agent`, `context_store_agent`, `asset_file_agent`, `project_deployment_agent`,
`api_collection_agent`, `task_agent`, `ai_agent_agent` — registering their underlying tools flat on
AI Hub and the management MCP surface. "Do not create an AI agent just to hide the number of CRUD
tools."

**Prerequisites:** catalog + `configureMcpServer` plans landed (`mcp_agent` is already out of this
list). This plan is the least urgent of the six and is deliberately structured one-delegate-per-task
so it can land incrementally or stop partway with no broken state.

## Global Constraints

- Work on `0_732`; user commits in parallel — fresh commits, never amend,
  `git commit -m "..." -- <paths>`.
- Commit messages `732 <description>` / `732 client - <description>`.
- CE Apache 2.0; EE Enterprise + `@version ee`. Java style per CLAUDE.md; log-file verification.
- **The Phase-3 key-family defect guard, mandatory per delegate:** the flat callbacks read one of
  two disjoint tool-context families — `AgentToolInvocationContext` (`bytechef.agentTool.*`) or
  `AutomationToolInvocationContext` (`bytechef.assetFile.*`). The target surface's agent must WRITE
  the family the tools READ, or every call fails at runtime as "no callback found"-adjacent errors
  while compiling clean. For each delegate: read the callbacks' context reads, read the surface's
  context writes, record both in the task report before registering anything.
- **The prompt-invariant audit, mandatory per delegate (the mcp_agent lesson):** a delegate's
  prompt can be load-bearing as an invariant that holds only because the delegate is the sole path
  in. Read each delegate's prompt BEFORE dissolving it and classify every rule as (a) already
  enforced in the facade/service, (b) needs promoting to the facade (do it in the same task, like
  the updateMcpServer enable-guard), or (c) genuinely advisory (moves to the surface prompt or the
  tool description). An unclassified rule blocks the task.
- **Session cleanup:** each dissolved delegate's `agentTypeKey` leaves `AgentTypeRegistry`. Confirm
  what task-delete/purge does with a key that disappears; if stored sessions keyed on it would
  never be deleted, note the leftover explicitly in the commit message (bounded-replay caches, not
  user data — but the decision must be recorded, not silent).

## Per-delegate facts to re-derive (start of each task)

```bash
git grep -n "<delegate_name>" -- 'server/**/*.java' 'server/**/*.txt' 'client/src/**/*.ts*' | grep -v test | grep -v docs/superpowers
```

Known flat-tool sources (from the factory survey): `DataTableToolCallbacksFactory` (11),
`KnowledgeBaseToolCallbacksFactory` (7), `ContextStoreToolCallbacksFactory` (12),
`AssetFileToolCallbacksFactory` (7), `ProjectDeploymentToolCallbacksFactory` (7),
`ApiCollectionToolCallbacksFactory` (3), `AiAgentToolCallbacksFactory` (11), `TaskTools` (8,
read-only `@Tool` class). Each factory already splits read/write lists — hub ASK gets reads, hub
BUILD and MCP get the full list, mirroring how the panel slices consume them today.

## Schema-pressure note (why this is safe to do)

Flattening adds roughly 60 tool schemas to the hub and MCP surfaces. The hub already runs the
pgvector tool-search catalog for exactly this (tier 2 of the AI Hub tool architecture): tools that
would bloat the pinned list go to the searchable catalog, callable after a `searchTool` hit. Each
task below decides pinned-vs-catalog per tool list: high-frequency reads (list/get/query) pinned;
mutations to the catalog with a prompt note "find with searchTool first" — matching how
createWorkflowChat is already handled. MCP has no such pressure (clients page tools/list); register
everything flat there.

---

### Task template (applies to Tasks 1–8, one delegate each)

Order: 1 `task_agent`, 2 `api_collection_agent`, 3 `project_deployment_agent`,
4 `asset_file_agent`, 5 `data_table_agent`, 6 `knowledge_base_agent`, 7 `context_store_agent`,
8 `ai_agent_agent` — smallest and lowest-risk first, so the pattern is proven before the big ones.

- [ ] **Step 1: Facts.** Run the grep; read the delegate's prompt; run the key-family check and the
  prompt-invariant audit (Global Constraints). Any (b)-class rule: implement the facade guard in
  this task, test-first, before dissolving.
- [ ] **Step 2: Register flat.** Add the factory's read list to hub ASK, full list to hub BUILD and
  the MCP contributor config, honoring the pinned-vs-catalog split above. Reuse the factory — do
  not construct callbacks inline.
- [ ] **Step 3: Dissolve.** Remove the delegate's registrations (hub + MCP), delete its callback
  class (or its `SubAgentToolCallback` construction), its ChatClient bean if now unconsumed, its
  prompt file, and its `CopilotAgentType`/`AiHubAgentType` entry. `git grep` the tool name
  afterward: only `docs/superpowers` hits may remain.
- [ ] **Step 4: Prompts.** Hub ASK/BUILD prompts: replace the "delegate X to <name>_agent" passage
  with flat-tool guidance (or a searchTool pointer for catalog-demoted mutations). Closing check:
  every backticked tool name in the touched prompts is registered on that agent.
- [ ] **Step 5: Client.** Check `AiHubRuntimeProvider.tsx` / `toToolResultDataPart` for branches on
  the delegate's name (the `task_agent` fallback-parse path is known to exist) and for
  `McpAppUiDescriptor` bindings — a flat tool result may now hit a viewer binding the delegate's
  prose never did, or vice versa. Update, `npm run check` from `client/`, note pre-existing
  parallel-work failures rather than fixing them.
- [ ] **Step 6: Verify + commit** with pathspec; parity test updated in the same commit whenever
  the expected-name list changes.

### Task 9: Duplicate factory cleanup

`DeploymentToolCallbacksFactory` and `ProjectDeploymentToolCallbacksFactory` carry byte-identical
tool lists (phase-3 leftover). Fold to one, update consumers, delete the other. Verify + commit.

### Task 10: Final sweep

- [ ] `git grep -n "_agent" -- 'server/**/*.txt'` (prompts) and the client tool-name branches: no
  reference to any dissolved delegate remains.
- [ ] Run the full parity test + hub/MCP module checks + `./gradlew compileJava compileTestJava
  --continue` (log, `$?`, FAILED grep).
- [ ] Confirm the intelligent tools are the ONLY delegates left: the catalog's `getNames()` is the
  complete delegate inventory.

## Manual verification (running backend)

1. AI Hub ASK: "what data tables do I have" — flat `listDataTables` call, no delegation hop.
2. AI Hub BUILD: create a data table with columns — flat mutations (or searchTool-then-call if
   catalog-demoted), correct result rendering.
3. MCP external client: tools/list shows the flat domain tools; a `queryDataTable` round-trip works
   (key-family proof).
4. A viewer-bound tool (per `McpAppUiDescriptor`) still opens its viewer from the MCP App.

## Self-review notes

- The template's Step 1 is where this plan earns its keep; Steps 2–6 are mechanical. Budget
  accordingly.
- **Deliberately not migrated:** the generative one-shots (research, data_analyst, image_generator,
  slide_builder) — AI-Hub-only by design, not CRUD, not touched.
