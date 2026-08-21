# Tool Search Tool — V1 (Walking Skeleton) Design Spec

**Status**: Draft
**Date**: 2026-04-29
**Branch**: `0_732`
**Audience**: AI Hub / Copilot / Agentic-AI module owners.

## Goal

Integrate [`spring-ai-tool-search-tool`](https://github.com/spring-ai-community/spring-ai-tool-search-tool) into the AI Hub copilot so the LLM can dynamically discover and invoke any of ByteChef's tool-typed cluster elements without us pre-registering thousands of tool definitions in the agent context.

V1 is a **walking skeleton on the ASK agent only** — read-side, lower stakes, validates the integration end-to-end before broadening to BUILD or layering UI on top.

## Why this matters

ByteChef has 180+ components, many with multiple tool-typed cluster elements. Pre-registering all of them as Spring AI `ToolCallback`s would consume 50K+ tokens before any user message arrives, and tool selection accuracy degrades when LLMs face dozens of similarly-named tools. The Tool Search Tool pattern (per the Spring AI Community blog) reports 34–64% token savings on similar workloads.

The library is Spring AI 2.x / Boot 4 compatible — matches ByteChef's stack — and integrates as a Spring AI **Advisor**, the same middleware mechanism we already use.

## Non-goals (deferred to V2)

- BUILD agent integration (write-mutation safety needs separate consideration)
- Plus-button menu / detail panel UI in the composer
- Per-conversation tool persistence (`ai_hub_task_component` + `_tool` tables)
- Chat affordances for explicit tool attachment (Attach/Add tool callbacks)
- Conversation-attached-tools chip list

## Architecture

### Catalog source

Use the existing `ClusterElementDefinitionService.getClusterElementDefinitions(ClusterElementType.TOOL)` API. Component authors have already curated which actions are tool-eligible by typing them as `TOOL` cluster elements; respect that curation. No hand-rolled "is this action tool-eligible" feeder.

For each `ClusterElementDefinition`, extract:

- `componentName`, `componentVersion`, `clusterElementName` (= the tool's invocation key)
- `description` (human-readable; embedding source)
- `properties` → JSON Schema (the tool's input schema)
- Optional metadata: component category, display title

### Embedding + persistence

Reuse the existing pgvector instance (already provisioned for the Knowledge Base) under a **separate collection** — proposed name `ai_hub_tool_search`. Tool embeddings must NOT mix with KB chunks; different access patterns and lifecycles.

Embedding model: the existing OpenAI embedding model already configured for ai-copilot. One-time cost at first boot is ~$0.04 for the full catalog at `text-embedding-3-small` rates; subsequent boots reuse the persisted vectors keyed by `(componentName, componentVersion, clusterElementName)`.

### Refresh policy

Catalog feeder runs at app boot via an `ApplicationReadyEvent` listener (NOT `@PostConstruct` — bean dependency-init ordering matters, and the Spring AI tool registry isn't fully wired until ApplicationReady).

Per tool, compute a hash of `(description + propertiesAsJsonSchema)`. Persist the hash alongside the embedding. On boot:

- New tool (no row by key) → embed + insert
- Hash unchanged → skip (no embedding cost)
- Hash changed → re-embed + update
- Tool removed (in DB but no longer in catalog) → delete

This keeps catalog drift in sync without paying for embeddings on every restart.

### Advisor wiring

The library exposes an `Advisor` that:

1. Adds a single `searchTools(query: String)` tool to the agent's tool callbacks.
2. On invocation, performs vector search against the embedding store, returns top-K matches.
3. Dynamically expands the matched tool definitions into the next agent turn's tool list.
4. Routes the LLM's invocation of an expanded tool to a configured handler.

Wire into `aiHubAskSpringAIAgent` (in `CopilotConfiguration`) via `.advisors(...)` on the underlying `ChatClient`. BUILD agent stays untouched in V1.

### Invocation handler

A new `ClusterElementToolInvocationHandler` receives `(toolMetadata, llmJsonArgs, ToolContext)` and:

1. Extracts `componentName`, `componentVersion`, `clusterElementName` from the tool metadata.
2. Resolves `WorkspaceInvocationContext` from the `ToolContext` (existing pattern from P3.2).
3. Looks up a `ComponentConnection` via `ComponentConnectionFacade` for `(workspaceId, componentName, environmentId)`.
4. If a connection is required (per the cluster element's connection-required flag) and missing, returns a structured `connectionRequired` envelope keyed by `componentName` — the existing `RequestConnectionToolCallback` flow surfaces this as a button in chat.
5. Otherwise calls `ClusterElementDefinitionService.executeTool(componentName, componentVersion, clusterElementName, parameters, componentConnection, editorEnvironment=false)`.
6. Returns the result, or a typed tool-error if the call throws (same `ToolErrors.runtimeFailure` pattern as P3.2 callbacks).

`executeTool` is the canonical invocation entry point and already handles parameter coercion, output handling, and connection injection — the same wiring agent cluster elements use today.

## Schema

No new tables for V1.

**Liquibase changeset**: a single migration in `ai/copilot_execution/` (the directory we already created for FK-into-execution-tier dependencies, since the pgvector collection lives in the execution-tier vector store):

```
20260430000001_ai_copilot_ai_hub_tool_search_init.xml
```

Provisions the `ai_hub_tool_search` pgvector collection with metadata columns for `(componentName, componentVersion, clusterElementName, contentHash)`. Exact DDL depends on the library's expected schema — finalize during implementation.

## Module placement

- New code lives in `server/ee/libs/ai/ai-copilot/ai-copilot-service` — same module as the existing tool callbacks, so no new Gradle module.
- Dependencies (`org.springaicommunity:tool-search-tool` + `tool-searcher-vectorstore`) added to that module's `build.gradle.kts`.
- EE license header + `@version ee` Javadoc on all new files.

## Connection resolution detail

Cluster elements are typed as `TOOL` independently of whether they need a connection. Most do (slack, github, gmail, hubspot, etc.); some don't (logic, dateTime, math).

For tools needing a connection:

1. Look up by `(workspaceId, componentName, environmentId)` — if exactly one match, use it.
2. If multiple matches (user has e.g. two Slack workspaces connected) → return `connectionAmbiguous` envelope listing the connection IDs/names; the LLM asks the user which to use.
3. If zero matches → return `connectionRequired` envelope. The existing `RequestConnectionToolCallback` flow handles the chat-side button.

For tools NOT needing a connection: skip resolution, pass `null` to `executeTool`.

## Acceptance criteria

V1 is **done** when all of the following pass:

1. Library deps integrate; ai-copilot-service builds cleanly with the new pom entries.
2. `ToolSearchCatalogFeederIntTest` (or unit + integration combo) verifies that on app boot, the feeder enumerates tool-typed cluster elements and persists embeddings to the new collection.
3. Hash-based refresh works: a second boot with no catalog changes does NOT re-embed (assert via mock embedding model interaction count).
4. `ClusterElementToolInvocationHandlerTest` verifies:
   - Successful invocation calls `clusterElementDefinitionService.executeTool` with correct args.
   - Missing connection returns `connectionRequired` envelope (not NPE, not generic error).
   - `executeTool` throwing a `RuntimeException` returns a typed tool-error envelope, not a propagating exception.
5. ASK agent integration test: a chat turn asking "find me a tool to send a Slack message" successfully calls `searchTools`, returns results, and the LLM-issued follow-up call to the discovered tool reaches the invocation handler with the right args. Stub the actual Slack API call.
6. Smoke test against 5 representative chat tasks (manual, not automated) — see "Smoke test plan" below.
7. `./gradlew :ai-copilot-service:check` is fully green: 498+ unit tests + integration tests + spotbugs/pmd/checkstyle.

## Smoke test plan

Five representative tasks exercised manually against the ASK agent post-deploy:

| # | Task | Expected behavior |
|---|---|---|
| 1 | "What tools do I have for working with GitHub?" | LLM calls `searchTools("github")`, returns top-5 matches, renders summary. |
| 2 | "Send a Slack message to #engineering saying 'deploy starting'" | Discovers `slack/sendMessage`, asks for connection if missing or invokes if available. |
| 3 | "Open a GitHub issue titled 'Test' in repo X" | Discovers `github/createIssue`, prompts for the GitHub connection if missing. |
| 4 | "Find me a tool to compute SHA-256 of a string" | Discovers a hash/crypto tool (no-connection path). |
| 5 | "I want to query a Postgres database" | Discovers the relevant DB component's tool, prompts for the connection (likely missing in default workspace). |

Pass criteria: ≥4/5 produce sensible discovery + invocation. The one allowed miss helps surface where catalog enrichment (synonyms, category tags) might be needed for V2.

## Risks

1. **Library maturity** — labeled "incubating" in Spring AI Community docs; v2.x line is recent. Mitigation: pin exact version in lib catalog; verify CI builds against that pin.
2. **Search quality on terse descriptions** — many ByteChef action descriptions are short verb phrases. If smoke test #1 or #4 misses, plan a V2 catalog enrichment pass (synonyms, category, common-task labels) before promoting to BUILD agent.
3. **Embedding cost regression** — if the hash-based refresh is broken, every restart re-embeds. Mitigation: assertion in IntTest pinning the no-change-no-embed contract.
4. **Cross-collection bleed** — putting tool embeddings in the same pgvector instance as KB chunks risks accidental cross-search. Mitigation: separate collection name, separate query path, integration test asserting KB queries don't return tool docs and vice versa.
5. **Concurrent boot** — if two ai-copilot-service instances boot simultaneously, both may try to re-embed the same tool. Mitigation: per-tool advisory lock during embed, or accept the duplicate write and dedup-on-read. V1 picks the "accept and dedup" path because the instance count in dev/test is 1.

## Effort

~3–4 days:

- Day 1: Maven dep + library sanity check + Liquibase migration + collection provisioning.
- Day 2: `ToolSearchCatalogFeeder` + hash-based refresh + unit tests.
- Day 3: `ClusterElementToolInvocationHandler` + connection resolution + unit tests.
- Day 4: Advisor wiring into ASK agent + integration test + smoke test pass.

## Open questions for V2 to inherit

- Tool name namespacing in the LLM-visible callback. V1 default: `componentName_clusterElementName`. V2 might allow user aliases.
- Connection-ambiguous resolution UX (multiple matching connections). V1 returns a structured envelope; V2 may add a "pick connection" chat affordance.
- BUILD agent integration safety (e.g. confirm-before-mutate for destructive tools). V2 problem.
- Tool catalog re-embedding cadence — V1 is boot-only; V2 may need a hot-reload trigger when component versions get bumped at runtime.

## Out of scope, full stop

- Visualization of which tools are "available" in the workspace (UI). V2.
- Per-user vs per-workspace tool surfacing (RBAC). V2.
- Cost attribution per tool invocation. Already handled by the existing `MeteredToolCallback` wrapping.
- Audit of which tools the LLM searched/invoked. Already handled by `ai_hub_tool_usage`.

---

## Sequencing

V1 ships, smoke-tests on staging-equivalent, then V2 begins. V2 spec lives at `2026-04-29-tool-search-v2-design.md` and depends on V1 being green.
