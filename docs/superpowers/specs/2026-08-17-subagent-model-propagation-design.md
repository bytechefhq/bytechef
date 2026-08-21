# Subagent Model Propagation — Design

**Status:** design
**Date:** 2026-08-17
**Builds on:** `2026-08-17-uniform-tool-surface-design.md` (its step 1, the intelligent tool catalog, is landed)

## The problem

A user picks a model in the chat toolbar. The agent they are talking to honours it. The moment that
agent delegates to a specialist, the pick is silently dropped.

Every delegate `ChatClient` is a Spring bean built as `ChatClient.builder(chatModel)` over the
application-default `ChatModel` — `codeWorkflowBuildSubAgentChatClient` and its nineteen siblings.
The per-conversation pick travels a different road: AG-UI state → `AiHubChatClientResolver` /
`CopilotChatClientResolver` → `CatalogChatClientResolver` → a fully-built override `ChatClient`,
consumed by `AiHubSpringAIAgent#resolveChatClient`. That method is the **main** agent's seam and
nothing else calls it. `AiHubSpringAIAgent`'s own Javadoc already names the gap:

> Subagent one-shot delegate calls do NOT go through this method — each specialist owns its own
> `ChatClient`, constructed once in `AiHubConfiguration` and invoked directly by its hand-rolled
> `ToolCallback`.

That Javadoc was written about guardrails, which were fixed separately. The model pick is the same
gap, still open.

It matters most where it is least visible. `buildWorkflow`-class delegates run multi-minute inner
agent loops — precisely the work a user upgrades their model *for*. They ask for Opus, the
orchestration turn runs on Opus, and the workflow is actually authored by whatever the deployment
defaults to.

## Scope

**Nine delegates, two surfaces.** The intelligent tools the catalog owns — `project_workflow_agent`,
`converter_agent`, `cluster_element_agent`, `code_editor_agent`, `skills_agent`,
`workflow_execution_agent`, `custom_component_agent`, `code_workflow_agent`,
`integration_workflow_agent` — on the AI Hub and the Projects Copilot panel.

**Management MCP keeps the default**, and does so *structurally*: it never puts a model into the tool
context, so the resolver finds nothing and the default client is used. There is no MCP branch to
write and none to forget.

Deliberately out of scope: the five CRUD delegates (cheap listing loops where model choice barely
shows) and the four AI-Hub generative one-shots (research, data_analyst, image_generator,
slide_builder — being reshaped by later plans anyway). Both remain on the default.

## Why the obvious mechanisms don't work

**Swapping the model on a built `ChatClient`.** There is no such operation. `ChatClient.Builder` takes
its `ChatModel` at construction and `mutate()` returns a builder over the same one. A delegate's
prompt, tools and model are fused at bean-creation time.

**Per-request `ChatOptions.model(...)`.** Changes the model *name* on the existing provider. The
pickers select provider **and** model, so a user on Anthropic delegating from a deployment defaulting
to OpenAI would get a model name the provider does not recognise — or worse, silently the wrong one.

**A second `ChatClient` decorator** that routes to a per-model client. Rejected on standing precedent:
`SubAgentGuardrailedChatClient` is ~250 lines of `ChatClientRequestSpec` delegation boilerplate, and
its class Javadoc records that a second decorator was rejected because every method added upstream
would have to be implemented twice.

**A `SubAgentAdvisorContributor`**, riding the seam guardrails and session memory already use. An
advisor can rewrite messages and set params. It cannot change which model the request goes to.

## The shape

The delegate's `ChatClient` stops being a value handed in at registration and becomes a **factory
invoked per delegation**.

```java
// CE, ai-copilot-tool, beside the catalog
@FunctionalInterface
public interface IntelligentToolChatClientFactory {

    /** @param chatModel the resolved override, or null for the contributor's default client */
    ChatClient get(@Nullable ChatModel chatModel);
}
```

`IntelligentToolDefinition` changes one method and one signature:

```java
@Nullable IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant);
ToolCallback create(IntelligentToolChatClientFactory chatClientFactory);
```

The catalog composes the per-surface decorator **inside** the factory, so the surface's wrapping still
applies to whichever client the request ends up on:

```java
IntelligentToolChatClientFactory decorated =
    chatModel -> chatClientDecorator.apply(rawFactory.get(chatModel), definition);

ToolCallback toolCallback = definition.create(decorated);

return callbackDecorator.apply(toolCallback, definition);
```

This preserves the property the catalog was built for: guardrails, the workspace system prompt and
session memory (AI Hub) still wrap the delegate, and now wrap the *re-targeted* delegate. It also
preserves the converter's laziness — the factory is invoked at call time, which is strictly later than
today's supplier resolution, never earlier.

`Supplier<ChatClient>` becomes the degenerate case: `factory.get(null)`.

## The four moving parts

### 1. The carrier — two tool-context keys

`AgentToolInvocationContext` gains `bytechef.agentTool.llmProvider` and `bytechef.agentTool.llmModel`
alongside the environment id it already carries. It is the right home: it is the map every delegate
`ToolCallback` already forwards, and `environmentId` — which the resolver also needs — is already in it.

Two populate sites, both already single functions:

- **AI Hub** — `AiHubSpringAIAgent#toolContext(RunAgentInput)`, from `AiHubStateKeys`'
  `USER_SELECTED_LLM_PROVIDER_KEY` / `USER_SELECTED_LLM_MODEL_KEY`, falling back to
  `TASK_LLM_PROVIDER_KEY` / `TASK_LLM_MODEL_KEY`. **The precedence must match
  `AiHubChatClientResolver` exactly** — user-selected wins over task — or a delegate would run on a
  different model than the agent that called it, which is worse than not propagating at all.
- **Copilot panels** — `CopilotToolContextUtils#toToolContext(State)`, from
  `CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER` / `_MODEL`. Every panel agent's
  `toolContext(...)` delegates to this one utility, so the panel half is a single function.

Half-set state (one of provider/model present) is treated exactly as the existing resolvers treat it:
not propagated, logged once, never a 400. A user mid-pick is a transient client artifact.

### 2. The resolver — a CE SPI with an EE implementation

```java
// CE, ai-copilot-tool
public interface SubAgentChatModelResolver {

    @Nullable
    ChatModel resolve(Map<String, Object> toolContext);
}
```

The EE implementation reads the three keys and delegates to a new method on the existing catalog
resolver:

```java
// EE, platform-ai-agent-api — mirrors resolve(...), returning the bare model
@Nullable
ChatModel resolveChatModel(String providerKey, String model, int environment);
```

This method **already exists** as a private method in `CatalogChatClientResolverImpl` — `resolve(...)`
is implemented as `resolveChatModel(...)` followed by `ChatClient.builder(chatModel).build()`. Adding
it to the interface is a promotion, not an implementation.

`CatalogChatClientResolver` already has the precedent: `resolveDefaultChatModel` exists, its Javadoc
saying it returns the model unwrapped "so callers can attach their own system prompt and tools" —
which is exactly a delegate's requirement.

**No bean, no propagation.** CE has no `SubAgentChatModelResolver` implementation, so CE delegates
run on the default, unchanged. This is the same absent-bean-means-off idiom the guardrails advisor and
`OverrideChatClientResolver` already use.

### 3. The callbacks — resolve once per invocation

The eight delegate callback classes serving the nine tools — `ProjectWorkflowAgentToolCallback` backs
both `project_workflow_agent` and `integration_workflow_agent`, via its four-argument constructor —
hold the factory and an optional resolver, and resolve in `call(String, ToolContext)`, where they
already have the context in hand:

```java
ChatModel chatModel = chatModelResolver == null ? null : chatModelResolver.resolve(contextMap);
ChatClient chatClient = chatClientFactory.get(chatModel);
```

No new decorator, no new seam, no change to how a delegate forwards its context.

### 4. The contributor beans — a factory beside each bean

Each `*SubAgentChatClient` `@Bean` gains a sibling factory that builds the same client over a supplied
`ChatModel`; the `@Bean` becomes that factory called with the default model, so there is exactly one
definition of each delegate's prompt and tool set.

**This is not a new pattern in this codebase — one delegate already does it.**
`converterBuildSubAgentChatClientSupplier` resolves a `ChatModel` at call time and rebuilds its client
with the same prompt and tools, per invocation. It is the design's working precedent, and the reason
this spec is confident the shape holds. What it gets wrong is only *which* model: it asks
`resolveDefaultChatModel(environment)` for the environment default and reads the environment from the
`EnvironmentContext` ThreadLocal, so it never sees the user's pick. This design replaces that source,
not that shape.

**No cache.** The precedent rebuilds per invocation — prompt read included — and that is correct:
assembling a `ChatClient` is object-graph construction, negligible beside the LLM call it precedes.
A cache here would trade a measurable leak (the key grows with distinct user picks behind a
user-facing dropdown) for an unmeasurable saving. The one cost worth removing is re-reading each
prompt `Resource` per delegation, which is a field hoist in the configuration class, not a cache.

## Failure handling

Every failure lands on the default client, and none fails the turn. A model preference is a
preference.

| Condition | Result |
|---|---|
| No provider/model keys in the tool context (Management MCP; no pick made) | Default client |
| Half-set (one key present) | Default client, warn once — matches the existing resolvers |
| No `SubAgentChatModelResolver` bean (CE) | Default client |
| Provider unknown, disabled, or unconfigured in the environment | Default client, warn |
| Resolver throws | Default client, warn — never propagate out of a tool call |

The one thing that must NOT be silent is a *mismatch*: if the parent agent resolved a model and the
delegate could not, that is a real inconsistency and is worth a warn-level log naming both, because it
is otherwise invisible and produces exactly the confusing outcome this design exists to remove.

## Testing

- **Catalog level** — extend `IntelligentToolSurfaceParityTest`: every contributed definition's factory
  must return a distinct client when handed a `ChatModel`, and its default client when handed null. A
  definition that ignores the override is the failure mode this whole design is about, so it is asserted
  at the one place that sees all nine.
- **Resolver level** — unit tests for each row of the failure table, including that a throwing catalog
  resolver yields null rather than propagating.
- **Carrier level** — assert the hub's precedence (user-selected over task) matches
  `AiHubChatClientResolver`'s, pinned in a test that would fail if either side changed alone.
- **MCP** — assert the management MCP tool context carries no model keys, so "MCP defaults" is a
  property of the code rather than a comment.
- **Laziness** — the existing catalog laziness test extends to the factory: the raw factory must not be
  invoked at registration time.

## Risks

- **Precedence drift.** The hub now derives the model in two places — `AiHubChatClientResolver` for
  itself, `toolContext(...)` for its delegates. If they diverge, a delegate runs on a different model
  than its caller. Mitigated by a test that pins them together; a shared helper is the better fix if the
  logic grows past the two-layer fallback it has today.
- **This edits `IntelligentToolDefinition` again**, immediately before the rename plan touches the same
  files. Landing this first is deliberate — the rename is a one-line-per-tool change afterwards, whereas
  the reverse order means renaming, then re-threading a new parameter through every renamed definition.
- **Cache growth** if the ceiling is set carelessly. Bounded LRU, small.
- **Nothing here has run against a live backend.** Neither has the catalog work it builds on. The
  batched visual pass covers both.

## Explicitly not doing

- Propagating to the CRUD delegates or the generative one-shots.
- A per-delegate model *override* (a delegate pinned to its own model regardless of the pick). That is a
  separate capability with its own storage and UI; this design only makes a delegate follow the caller.
- Restoring the commented-out AI Gateway fallback path in either resolver. It stays disabled.
- Changing what the pickers themselves offer.
