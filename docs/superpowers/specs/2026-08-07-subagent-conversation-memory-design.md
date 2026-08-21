# Subagent Conversation Memory

Give AI Hub specialist subagents a per-conversation memory so a multi-turn refinement does not
restart from zero on every delegation.

## Problem

Every specialist delegation is a one-shot call. `ManagerSubAgentToolCallback` reduces to:

```java
chatClient.prompt(request).call()
```

No conversation id, no memory advisor, no thread. The same holds for the Copilot specialist
callbacks (`SkillsAgentToolCallback`, `ClusterElementAgentToolCallback`, …) and the AI-hub-owned
ones (`ResearchToolCallback`, …). The specialist's entire input is the `request` string.

From the user's seat the conversation looks continuous — you ask, the specialist answers or asks a
question, the parent relays it, you reply, the parent delegates again. That loop works today and is
not what this spec changes. What is missing is continuity *inside* the specialist: it never sees its
own previous output, and it never sees the tool results it fetched last time.

The parent papers over this by re-packing context into `request`. The tool schema says so outright:

> "The user's request, passed verbatim, plus any ids or user decisions already resolved in this
> conversation that the specialist needs."

That makes the parent a lossy relay. Concretely: `personal_agent_manager` drafts an agent's
instructions; the user says "make it shorter"; the specialist has never seen its own draft and
re-drafts from scratch unless the parent quoted the text back verbatim. Specialists also re-run
`listAiHubPersonalAgents` on every delegation because they do not remember the last listing.

## Decisions

- **Durable, not ephemeral.** Reuse the existing session store rather than an in-memory cache, so
  memory survives a restart mid-conversation and works in distributed EE without a second backend.
- **Bounded via compaction.** History is capped by turn count. See "Compaction is destructive"
  below — this is not a read-side window, and the difference is user-visible.
- **AI Hub chat surface only.** The MCP manager surface has no conversation concept, so it has no
  key to memoize on. It degrades to today's stateless behavior, unchanged.
- **One seam, no delegate edits.** Wrap at the same point in `AiHubConfiguration` where
  `SubAgentGuardrailedChatClient.wrap` already sits.
- **Additive, not load-bearing.** The parent keeps re-packing context into `request`. Memory is
  best-effort and the system must work when it is absent.
- **Suspend/resume is out of scope.** A specialist still cannot pause mid-run to ask the user and
  resume. That is a separate, architectural change — see "Follow-on work".

## Component 1 — `SubAgentSessionMemoryChatClient`

A `ChatClient` decorator in `com.bytechef.ee.ai.hub.memory`, modeled directly on
`SubAgentGuardrailedChatClient`.

### Not a second decorator — a contributor seam

The first draft of this spec called for a standalone `SubAgentSessionMemoryChatClient` composed
alongside the guardrails wrapper. Reading `SubAgentGuardrailedChatClient` killed that: the class is
361 lines, of which roughly 250 are `ChatClientRequestSpec` delegation boilerplate. Only
`attachWorkspaceAdvisorsIfActive()` — twelve lines — actually varies. A second decorator would
duplicate all of it, and every future `ChatClientRequestSpec` method added upstream would have to be
implemented twice.

Instead the existing decorator is generalized once. It already does exactly the right thing
structurally — capture the forwarded `ToolContext`, then attach advisors at `call()`/`stream()` — it
just hardcodes which advisors. That becomes a list:

```java
@FunctionalInterface
public interface SubAgentAdvisorContributor {
    List<Advisor> advisors(@Nullable Map<String, Object> toolContext);
}
```

Guardrails-plus-workspace-prompt becomes one contributor; session memory becomes another. Each is
small and independently testable, which is what the original "keep them separate" instinct wanted —
without paying for the boilerplate twice.

The class keeps its name. Its javadoc already records that a rename was declined as churn, and
renaming across 24 call sites for no functional gain would be a poor trade in the same change that
adds behavior.

### Why resolution is deferred to request time

The delegate `ChatClient` beans are process-wide singletons shared by every workspace *and every
conversation*. The conversation id cannot be baked in at construction. Resolution therefore happens
when the delegate forwards the parent's `ToolContext` via `ChatClientRequestSpec.toolContext(Map)` —
the same mechanism the guardrails wrapper already relies on, which is why no delegate class needs to
change.

### Key

```
<parentThreadId>:<agentType.key()>
```

`parentThreadId` comes from `AgentToolInvocationContext.TOOL_CONTEXT_CONVERSATION_ID_KEY`, already
populated — `AiHubSpringAIAgent.toolContext` sets `conversationId(aiHubContext.threadId())` today.
`agentType` is the shared `AgentType` interface, so one wrapper serves `AiHubAgentType`,
`CopilotAgentType`, and manager types without knowing which enum it holds.

Key derivation gets a dedicated unit test. A bug here crosses conversations, which is the worst
failure this design can produce, and integration coverage would not isolate it.

### Advisor

A `SessionMemoryAdvisor` over the **same** `AiHubSessionMemory.sessionService()` the parent uses,
under `AiHubSessionMemory.SESSION_USER_ID`:

```java
SessionMemoryAdvisor.builder(sessionService)
    .defaultUserId(AiHubSessionMemory.SESSION_USER_ID)
    .eventFilter(EventFilter.lastN(MAX_EVENTS))
    .build()
```

`MAX_EVENTS` is a constant (start at 10, roughly five exchanges), not a configuration property.
There is no evidence yet that operators need to tune it, and a property is easy to add later.

### The window is read-side and non-destructive

`eventFilter` bounds what is *loaded*, not what is kept. `SessionMemoryAdvisor.before` resolves to:

```java
List<SessionEvent> events = this.sessionService.getEvents(sessionId, eventFilter);
```

So `EventFilter.lastN(N)` replays the most recent N events while the full history stays in the
store. Compaction (`TurnCountTrigger` + `TurnWindowCompactionStrategy`) would also bound replay but
**rewrites stored history**, permanently dropping older turns. Since bounding cost is the goal and
losing history is not, the read filter is the better lever; compaction stays available if storage
growth later becomes the binding constraint.

Do not reach for `messageFilter` here — that one governs what is *written*
(`MessageFilter.shouldPersist(Message)`) and cannot bound replay.

The advisor also honours a per-request override via `EVENT_FILTER_CONTEXT_KEY`, merged with the
builder's filter. Not needed for this design, but it is the escape hatch if a specialist ever needs
a different window per call.

### Advisor order

Default precedence (`Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER`), which places the advisor
**outside** the tool-calling loop — deliberately unlike the parent, which uses
`ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER` to capture the full tool transcript.

A specialist's tool traffic is mostly listing calls it re-runs anyway; persisting it would balloon
the memory against the turn budget for little benefit. User and assistant turns are what "remember
your own draft" actually requires.

### Degradation

- **No conversation id in the ToolContext** (MCP manager surface, or any future caller that does not
  forward one) — no advisor attached, behavior identical to today.
- **Session store failure** — fail-open. Log at WARN, run the delegation without memory. Consistent
  with every other persistence path in this area (`persistTurnToChatMemory`, the agent-loop
  checkpointer).

## Component 2 — Wiring

Three call sites in `AiHubConfiguration`, each already wrapping with
`SubAgentGuardrailedChatClient.wrap`:

- `registerCopilotSubAgentToolCallbacks` — skills, context_store, knowledge_base, data_table,
  cluster_element, code_editor, workflow_editor, converter, workflow_execution, custom_component,
  code_workflow
- `registerSubAgentToolCallbacks` — research, data_analyst, image_generator, slide_builder
- `registerManagerSubAgentToolCallbacks` — mcp_manager, personal_agent_manager, deployment_manager,
  api_collection_manager

`AiHubManagerMcpContributorConfiguration` and the other MCP contributors are **not** wrapped. They
construct their own `ManagerSubAgentToolCallback` from the same `ChatClient` beans on a different
`@Bean` method and have no conversation id. This mirrors the existing, deliberate gap where the MCP
surface also skips guardrail wrapping.

## Component 3 — Cleanup on task delete

`AiHubTaskServiceImpl.deleteSessionMessages(threadId)` currently deletes only the parent session.
Specialist sessions keyed off the same thread would orphan.

Extend it to iterate every registered agent type, constructing `<threadId>:<key>` for each and
deleting it. `SessionRepository` has no prefix listing, so keys are constructed rather than
discovered; deleting a key that was never created is a no-op.

`AgentTypeRegistry` already aggregates every `AgentTypeProvider` via ServiceLoader — covering
`AiHubAgentTypeProvider`, `CopilotAgentTypeProvider`, and `ManagerAgentTypeProvider` — but its only
public method today is `fromKey(String)`; the aggregated map is private. This work adds a public
accessor (`keys()`) returning the registered keys. That is a deliberate, if small, widening of a
shared CE API: the alternative, re-running the `ServiceLoader` inside the cleanup path, would
duplicate the registry's own aggregation and drift from it.

Note the coupling this creates: registered agent types include panel agents and mode variants that
are never delegates and therefore never own a specialist session (`ai_hub_ask`, `workflow_editor`,
…). Constructing keys for them is harmless — each is one no-op delete — but the delete cost scales
with the enum, not with the number of specialists actually used in the conversation. Accepted:
deletes are rare and the alternative is a hand-maintained delegate list that would silently rot.

Cleanup stays best-effort, matching the existing behavior where a failed purge logs and leaves
orphans for background cleanup rather than blocking the row delete.

## Known gap — truncation does not rewind specialist memory

`truncateMessagesFrom` ("edit and resend") compacts the parent's session only. After an edit, a
specialist may still recall a turn the user removed from the visible thread.

Deliberately out of scope. Fixing it means mapping a parent message index onto per-specialist turn
boundaries, which have no relationship to each other — the specialist may have been delegated to
zero times or five times between two visible parent messages. Worth doing separately if it proves
confusing in practice.

## Considered and rejected — session branches

`SessionEvent` carries a `branch` column and `EventFilter.forBranch(String)` implements the
MemGPT / Google ADK isolation rule: an event at branch `X` is visible to an agent at branch `Y` when
`X` is null (a root event), equals `Y`, or is a dot-prefix ancestor of `Y`. The library's own javadoc
example is this exact scenario — `.eventFilter(EventFilter.forBranch("orch.researcher"))`.

That model is a strictly better fit on paper. One session per conversation means task delete already
purges everything and the `AgentTypeRegistry.keys()` accessor becomes unnecessary. Better still,
because the parent's existing events are all branch-null (root), a specialist filtering on
`orch.<key>` would see **the parent's actual conversation** rather than only the re-packed `request`
string — and siblings would stay invisible to each other, and the parent would not see specialists.
It is also backward compatible: existing sessions are entirely branch-null, so nothing already stored
becomes unreachable.

It is rejected because `SessionMemoryAdvisor` **filters reads by branch but never stamps one on
write**. The builder has no `branch(...)`, and nothing in the advisor's persist path sets it —
`SessionEvent.Builder.branch(String)` exists, but only a caller writing events by hand can use it.
Specialist turns written through the advisor would therefore land at root, visible to the parent and
to every sibling: precisely the pollution the branch model is meant to prevent.

Adopting branches thus means abandoning the advisor for specialists and hand-writing every turn (the
shape `WebhookBridgeAgent.persistTurnToChatMemory` already uses), trading the advisor's automatic
capture for manual persistence at every delegate. Separate sessions get the same isolation with no
new write path.

Revisit if the library adds a write-side branch on the advisor, or if giving specialists the parent's
full conversation context turns out to be worth hand-rolled persistence.

## Testing

Unit (`SubAgentSessionMemoryChatClientTest`):

- Key composition — `<threadId>:<agentType>`, and that two different agent types on the same thread
  produce different keys.
- No conversation id in the ToolContext — no advisor attached, delegate called unchanged.
- Session store throwing — delegation still runs, warning logged.
- Composition with `SubAgentGuardrailedChatClient` — both advisors present.

Unit (`AiHubTaskServiceTest`):

- Delete purges the parent session *and* one constructed key per registered agent type.
- A purge failure does not block the metadata row delete.

Unit (`AgentTypeRegistryTest`):

- `keys()` returns types from every registered provider, not just the core ones.

Integration:

- Two sequential delegations to the same specialist on one thread: the second sees the first's
  output.
- Two delegations to *different* specialists on one thread do not see each other.

## Verification

- `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test`
- `./gradlew spotlessApply check`

## Risks

- **Persisted specialist transcripts** now fall under retention and guardrail scanning, and add one
  session per specialist per conversation. Note the read window bounds *replay*, not *storage*: a
  long-running conversation keeps every specialist turn on disk while only the last `MAX_EVENTS` are
  ever loaded. That is the deliberate trade for not destroying history, and it is the first thing to
  revisit — via compaction — if storage rather than token cost becomes the binding constraint.
- **Key collision across conversations** would leak one user's specialist context into another's.
  Mitigated by the dedicated key-derivation test; the key is derived from a thread id that is already
  globally unique (`ai_hub_task.thread_id` is `NOT NULL UNIQUE`).
- **Silent no-op.** If a delegate ever stops forwarding the ToolContext, memory disappears with no
  error — the same failure mode the guardrails wrapper already carries.

## Follow-on work

Specialists still cannot suspend mid-run to ask the user and resume; they must finish, return the
question as text, and be re-invoked by the parent. Extending the suspend/resume sentinel protocol to
delegate calls is architectural — `runChatWorkflow`'s SSE/awaitingInput contract is client-coupled to
the MAIN agent's tool-call event — and gets its own spec.
