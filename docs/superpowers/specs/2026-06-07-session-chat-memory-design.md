# Session Chat Memory — Design

**Date:** 2026-06-07
**Branch:** `0_732_session_chat_memory`
**Status:** Draft — awaiting review

## Goal

Promote the existing single-module `chat-memory-session` component into a first-class
**cluster-element family** that exposes the full Spring AI *session management*
(`spring-ai-session-management` 0.4.2) configuration surface — compaction, recall
storage, branch isolation — while pluggably selecting its storage backend (built-in
internal datasource, external JDBC, or in-memory), the same way `jdbcChatMemory`
selects a `DATA_SOURCE` child.

The session memory must remain usable wherever the other chat memories are used: it is a
`CHAT_MEMORY`-type cluster element, so the AI Agent runtime injects its advisor (and now
its tools) with **no per-memory special-casing**.

## Architecture

Composition tree as it appears in a workflow agent:

```
AI Agent (cluster root)
└─ CHAT_MEMORY slot
   └─ sessionChatMemory            ← cluster element (CHAT_MEMORY) AND cluster root
      │   holds ALL setup properties: conversationId, defaultUserId,
      │   compaction (strategy + params), recall storage, agent branch
      ├─ SESSION_REPOSITORY slot    ← exactly one backend (new cluster element type)
      │  ├─ builtInSessionChatMemory   (internal app DataSource)
      │  ├─ jdbcSessionChatMemory      (cluster root → DATA_SOURCE child = external DB)
      │  └─ inMemorySessionChatMemory  (InMemorySessionRepository)
      └─ MODEL slot (optional)      ← reuses existing MODEL type; required only when
         compactionStrategy = RECURSIVE_SUMMARIZATION (supplies the summarizer ChatClient)
```

`sessionChatMemory` owns the `SessionMemoryAdvisor` and all its knobs. Its
`SESSION_REPOSITORY` child supplies a `SessionRepository`; `sessionChatMemory` wraps that
in a `DefaultSessionService` and builds the advisor. When the user selects
`RECURSIVE_SUMMARIZATION`, `sessionChatMemory` also resolves its optional `MODEL` child
into a `ChatModel` and builds `ChatClient.builder(chatModel).build()` for
`RecursiveSummarizationCompactionStrategy.builder(chatClient)`.

Both children are resolved with the same parent-resolves-child pattern `jdbcChatMemory`
uses for `DATA_SOURCE` — `ClusterElementMap.of(extensions).getClusterElement(...)` +
`clusterElementDefinitionService.getClusterElement(...)`, with the child's connection
read from the `componentConnections` map `apply()` already receives. No SPI signature
change is required to obtain the model.

## SPI changes (`platform-component-api`)

### 1. New `SessionRepositoryFunction` (mirrors `DataSourceFunction`)

```java
package com.bytechef.platform.component.definition.ai.agent;

@FunctionalInterface
public interface SessionRepositoryFunction {

    ClusterElementType SESSION_REPOSITORY =
        new ClusterElementType("SESSION_REPOSITORY", "sessionRepository", "Session Repository", true);

    SessionRepository apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception;
}
```

Returns `org.springframework.ai.session.SessionRepository`. The three backend components
implement this.

### 2. Extend `ChatMemoryFunction.Result` to carry tools (recall storage)

`SessionEventTools` exposes a `conversation_search` `@Tool`. The agent registers tools on
the `ChatClient` separately from advisors, and `ChatMemoryFunction.Result` currently has
no tools slot. Add an optional field:

```java
record Result(BaseAdvisor advisor, @Nullable ChatMemory chatMemory,
              @Nullable ToolCallback[] toolCallbacks) {

    // back-compat convenience ctor for the existing 7 implementations
    public Result(BaseAdvisor advisor, @Nullable ChatMemory chatMemory) {
        this(advisor, chatMemory, null);
    }
}
```

All existing chat-memory implementations keep compiling via the 2-arg ctor.

> `chatMemory` is left `null` for session memory (it has no `ChatMemory`; it has a
> `SessionService`). Guardrail history-loading already tolerates a null `chatMemory`
> (`Optional.map(...).orElse(List.of())` at `AbstractAiAgentChatAction:398`).

## Agent runtime change (`AbstractAiAgentChatAction`)

One change: memory-contributed tools must reach the `ChatClient`. Today the memory
`Result` is built inside `getAdvisors` (line 395) and only its `advisor()` is consumed.

- Build the memory `Result` **once** in `getChatClientRequestSpec`, pass it into
  `getAdvisors` (advisor attach) and into the `.tools(...)` assembly (merge
  `result.toolCallbacks()` into the array at line 173-177).
- This avoids constructing the stateful `SessionService` twice.

No new cluster-element-type handling is needed — session memory is `CHAT_MEMORY`, so
advisor injection, `conversationId` resolution (line 146), and `getConversationAdvisor`
(`SESSION_ID_CONTEXT_KEY = ChatMemory.CONVERSATION_ID`) all work unchanged.

## Modules

| Module | Component | Cluster element | Backend |
|---|---|---|---|
| `chat-memory-session` (reworked) | `sessionChatMemory` | `SessionChatMemory` (CHAT_MEMORY + root) | resolves SESSION_REPOSITORY child |
| `chat-memory-builtin-session` | `builtInSessionChatMemory` | `BuiltInSessionChatMemory` (SESSION_REPOSITORY) | internal app `DataSource` (`JdbcSessionRepository`), `InMemorySessionRepository` fallback |
| `chat-memory-jdbc-session` | `jdbcSessionChatMemory` | `JdbcSessionChatMemory` (SESSION_REPOSITORY + root) | external DB via `DATA_SOURCE` child |
| `chat-memory-in-memory-session` | `inMemorySessionChatMemory` | `InMemorySessionChatMemory` (SESSION_REPOSITORY) | `InMemorySessionRepository` |

Shared schema-init + repository-build helpers live in the jdbc-session module and are
reused by builtin-session (the same way `chat-memory-builtin` reuses `chat-memory-jdbc`).

## `sessionChatMemory` properties (the full knob set)

| Property | Type | Notes |
|---|---|---|
| `conversationId` | string, required | session id; flows to `SESSION_ID_CONTEXT_KEY` |
| `defaultUserId` | string, default `bytechef` | `SessionMemoryAdvisor.defaultUserId` |
| `compactionStrategy` | select: `NONE` / `SLIDING_WINDOW` / `TURN_WINDOW` / `TOKEN_COUNT` / `RECURSIVE_SUMMARIZATION` | drives conditional sub-props |
| `maxEvents` | int, default 20 | shown for SLIDING_WINDOW |
| `maxTurns` | int, default 10 | shown for TURN_WINDOW (also paired `TurnCountTrigger`) |
| `maxTokens` | int, default 4000 | shown for TOKEN_COUNT (also paired `TokenCountTrigger`) |
| `maxEventsToKeep` / `overlapSize` | int | shown for RECURSIVE_SUMMARIZATION; also requires a `MODEL` child |
| `enableConversationSearch` | bool, default false | contributes `SessionEventTools` |
| `searchPageSize` | int, default 10 | `SessionEventTools.pageSize` |
| `agentBranch` | string, optional | `EventFilter.forBranch(...)` |

Trigger/strategy are paired to satisfy the advisor's "both-or-neither" `build()`
constraint: window/token strategies pair with a matching count/token trigger;
`NONE` sets neither.

## RecursiveSummarization — resolved via a `MODEL` child

`RecursiveSummarizationCompactionStrategy.builder(chatClient)` needs an LLM. Rather than
change the `ChatMemoryFunction.apply()` signature, `sessionChatMemory` hosts an **optional
`MODEL` child** (the existing `MODEL` cluster element type, the same one the agent uses).

When `compactionStrategy = RECURSIVE_SUMMARIZATION`, `sessionChatMemory.apply()`:
1. resolves the `MODEL` child — `ClusterElementMap.of(extensions).getClusterElement(MODEL)`
   → `clusterElementDefinitionService.getClusterElement(...)` → `ModelFunction.apply(params,
   connParams, false)` (cast to `ChatModel`), reading the child connection from
   `componentConnections.get(modelElement.getWorkflowNodeName())`;
2. builds `ChatClient.builder(chatModel).build()`;
3. passes it to `RecursiveSummarizationCompactionStrategy.builder(chatClient)`.

If `RECURSIVE_SUMMARIZATION` is selected but no `MODEL` child is configured, `apply()`
throws a clear configuration error. All four compaction strategies ship in this PR; no
SPI signature change is required.

## Testing

- Component definition snapshot tests for each new component (auto-generated
  `definition/*.json`); delete stale `build/resources/test/definition/` first.
- Unit tests: `SessionChatMemory` builds the right advisor for each compaction selection
  (incl. RECURSIVE_SUMMARIZATION resolving its `MODEL` child into a `ChatClient`, and a
  clear error when the model child is missing); recall toggle contributes tools; branch
  sets the event filter; backend resolution picks the SESSION_REPOSITORY child.
- `ChatMemoryFunction.Result` 2-arg ctor back-compat (existing impls unchanged).
- Agent runtime: memory `toolCallbacks` reach the `ChatClient` `.tools(...)`.

## Out of scope

- Multi-agent branch *production* (the runtime emitting branched `SessionEvent`s) — we
  only expose the read-side `forBranch` filter.
- Expired-session sweeping (`deleteExpiredSessions`) scheduling.

## Spike findings (2026-06-07)

Both gating assumptions were investigated against the worktree
(`spring-ai = 2.0.0-RC1`, `spring-ai-session-management = 0.4.2`). Verdicts below;
neither blocks the design, but **Assumption 2 requires a concrete runtime fix**.

### Assumption 1 — nesting non-DATA_SOURCE children under a CHAT_MEMORY element

**Verdict: (b) per-element declaration IS required — and it is straightforward.**

Nesting is NOT unrestricted. Which child cluster-element types a cluster *root* may host
is declared explicitly via `getClusterElementTypes()` on
`ClusterRootComponentDefinition`
(`server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ClusterRootComponentDefinition.java:32`).

Evidence the existing `jdbcChatMemory` permission is declared, not implicit:
- `JdbcChatMemoryComponentDefinition extends ClusterRootComponentDefinition` and overrides
  `getClusterElementTypes()` to return `List.of(DATA_SOURCE)`
  (`.../definition/JdbcChatMemoryComponentDefinition.java:30-33`). The cluster-element DSL
  (`JdbcChatMemory.java`) itself declares nothing about children — it only resolves the
  child at runtime via `ClusterElementMap` + `JdbcChatMemoryUtils`.
- The AI Agent declares its slots the same way: `getClusterElementTypes()` →
  `List.of(MODEL, CHAT_MEMORY, RAG, GUARDRAILS, TOOLS)`
  (`.../definition/AiAgentComponentDefinition.java:44-47`).

**Direct precedent for "a CHAT_MEMORY element that is also a cluster root hosting a NEW
child type":** `VectorStoreChatMemoryComponentDefinition` — a CHAT_MEMORY cluster element
whose component definition `extends ClusterRootComponentDefinition` and declares
`getClusterElementTypes()` → `List.of(VECTOR_STORE)`
(`.../definition/VectorStoreChatMemoryComponentDefinition.java:27-32`). So the proposed
`sessionChatMemory` (CHAT_MEMORY + root, hosting SESSION_REPOSITORY + MODEL) is shape-for-shape
identical to an already-shipping component. No platform change is needed to *allow* the shape.

**Editor wiring confirms it reads the nested root's OWN definition.** For a nested cluster
root the client looks up `nestedClusterRootsDefinitions[componentName]` and uses that
definition's `clusterElementTypes`
(`client/src/pages/platform/cluster-element-editor/utils/createClusterElementsNodes.ts:113-118`),
then `getFilteredClusterElementTypes(..., isNestedClusterRoot: true)` additionally narrows by
`clusterElementClusterElementTypes[currentClusterElementsType]`
(`client/src/pages/platform/cluster-element-editor/utils/clusterElementsUtils.ts:289-308`).
That extra map defaults to "no restriction" when absent
(`ClusterRootComponentDefinition.getClusterElementClusterElementTypes()` defaults to
`Map.of()`, line 42-44), so by default all of the nested root's own declared types are
offered.

**Exact declarations required:**

1. New SPI type `SessionRepositoryFunction.SESSION_REPOSITORY` (already in the design's SPI
   section) plus a `MODEL` (existing) — both must be registered as real `ClusterElementType`s.
2. A new platform component-definition interface for `sessionChatMemory`, mirroring
   `VectorStoreChatMemoryComponentDefinition`:

   ```java
   public interface SessionChatMemoryComponentDefinition extends ClusterRootComponentDefinition {
       @Override
       default List<ClusterElementType> getClusterElementTypes() {
           return List.of(SESSION_REPOSITORY, MODEL);
       }
   }
   ```

   `sessionChatMemory`'s `ComponentHandler` must wrap its definition with this interface
   (the same way `JdbcChatMemoryComponentHandler` wraps `JdbcChatMemoryComponentDefinition`).
3. `jdbcSessionChatMemory` (the SESSION_REPOSITORY child that is itself a root hosting an
   external `DATA_SOURCE`) needs a definition interface declaring
   `getClusterElementTypes()` → `List.of(DATA_SOURCE)`, exactly like
   `JdbcChatMemoryComponentDefinition`.
4. Optional polish: if the MODEL child should only be *offered* under `sessionChatMemory`
   when `compactionStrategy = RECURSIVE_SUMMARIZATION`, that is a conditional-property /
   editor concern, not a nesting-permission concern — `getClusterElementTypes()` always
   listing MODEL is fine (it's an optional slot).

No change to the AI Agent's own `clusterElementClusterElementTypes` is required for the
common case; the nested-root filter reads `sessionChatMemory`'s own definition.

### Assumption 2 — conversation id reaching the `@Tool` `ToolContext`

**Verdict: the constant alignment is correct, BUT the conversation id does NOT currently
reach the tool ToolContext. A one-line-per-call-site fix is required.**

Constant alignment (all equal to the string `chat_memory_conversation_id`), verified by
`javap -constants` against the resolved jars:
- `org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID = "chat_memory_conversation_id"`
  (`spring-ai-model-2.0.0-RC1.jar`).
- `org.springframework.ai.session.advisor.SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY =
  "chat_memory_conversation_id"` (`spring-ai-session-management-0.4.2.jar`).
- `org.springframework.ai.session.tool.SessionEventTools.SESSION_ID_CONTEXT_KEY =
  "chat_memory_conversation_id"`; its `conversationSearch(...)` `@Tool` takes a
  `ToolContext` parameter and reads the session id from it.

**Problem — advisor context ≠ tool context.** The agent sets the conversation id only as an
*advisor param*:
`AbstractAiAgentChatAction.getConversationAdvisor` calls
`advisor.param(ChatMemory.CONVERSATION_ID, conversationId)` (and a mirrored
`"chat_memory_session_id"`)
(`server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatAction.java:450-462`),
attached at line 171.

In Spring AI RC1 these are two distinct maps. `DefaultChatClientUtils` builds the `@Tool`
`ToolContext` **only** from `inputRequest.getToolContext()` (the `.toolContext(...)` builder
map) — `tbuilder.toolContext(inputRequest.getToolContext())` — while advisor params go into a
*separate* `ChatClientRequest.context(...)`:
```
/tmp (extracted) DefaultChatClientUtils.java:120-122   // ToolContext  ← .toolContext(...)
/tmp (extracted) DefaultChatClientUtils.java:135        // advisor ctx  ← .getAdvisorParams()
```
The current `.toolContext(...)` calls carry only `ACTION_CONTEXT` (+ SSE keys), never the
conversation id:
- `AiAgentChatAction.java:136` → `Map.of(AiAgentToolContextKey.ACTION_CONTEXT, context)`
- `AiAgentStreamChatAction.java:112-116` and `:168-172`
- resume path `AbstractAiAgentChatAction.java:309`

Therefore `SessionEventTools.conversationSearch` would receive a `ToolContext` **without**
`chat_memory_conversation_id`, and recall search would fail/return empty.

**Required fix (concrete).** Put the conversation id into the *tool* context, not just the
advisor context. Two viable shapes:

- **Preferred (single point):** inside `getChatClientRequestSpec`, where `conversationId` is
  already computed (`AbstractAiAgentChatAction.java:146-160`) and `.tools(...)` is assembled
  (`:173-177`), add a `.toolContext(...)` carrying the id, e.g. (literal kept to avoid
  coupling the core agent module to spring-ai-session, matching the existing comment style at
  line 455-459):
  ```java
  if (conversationId != null) {
      spec.toolContext(Map.of("chat_memory_conversation_id", conversationId));
  }
  ```
  Note: `ChatClientRequestSpec.toolContext(...)` **merges** rather than replaces, so the
  callers' later `.toolContext(Map.of(ACTION_CONTEXT, ...))` will not clobber it. Verify the
  merge semantics when implementing; if it replaces, fold the id into each caller's map
  instead.
- **Alternative (per call-site):** add the key to each of the four `.toolContext(...)` maps
  (`AiAgentChatAction:136`, `AiAgentStreamChatAction:112` & `:168`,
  `AbstractAiAgentChatAction:309`). This requires `conversationId` to be in scope at those
  sites (today it is local to `getChatClientRequestSpec`), so the single-point fix is cleaner.

This fix is small and self-contained; it does **not** invalidate the design. The design's
"no per-memory special-casing" claim still holds for the *advisor* path; only the *tool*
path needs the id propagated, which benefits every memory exposing recall tools, not just
session memory.

### No hard blockers

Neither assumption produced a blocker. Assumption 1 is a known, already-shipping shape
(VectorStoreChatMemory). Assumption 2 needs a small, well-scoped tool-context propagation
fix that should be folded into the "Agent runtime change" task alongside the
`toolCallbacks` plumbing.
