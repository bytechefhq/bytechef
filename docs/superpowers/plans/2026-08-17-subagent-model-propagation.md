# Subagent Model Propagation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When a user picks a model in the AI Hub or a Copilot panel, the nine intelligent delegate
tools run on that model instead of the application default. Management MCP keeps the default.

**Architecture:** The delegate's `ChatClient` stops being a value handed in at registration and
becomes an `IntelligentToolChatClientFactory` invoked per delegation. The picked (provider, model)
rides the tool context the delegates already forward; an optional CE `SubAgentChatModelResolver` SPI
turns it into a `ChatModel`; the callback asks the factory for a client over that model. The
per-surface `chatClientDecorator` composes *inside* the factory, so guardrails, the workspace system
prompt and session memory still wrap the re-targeted client.

**Tech Stack:** Java 25 / Spring Boot 4, Spring AI, JUnit 5 + Mockito + AssertJ.

**Spec:** `docs/superpowers/specs/2026-08-17-subagent-model-propagation-design.md` — read it first,
especially "Why the obvious mechanisms don't work" (three plausible approaches are ruled out there,
with reasons) and "Failure handling" (the table is the requirements list for Task 4).

**Builds on:** the intelligent tool catalog
(`docs/superpowers/specs/2026-08-17-uniform-tool-surface-design.md` step 1), already landed on this
branch. Read `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/catalog/`
before starting: `IntelligentToolCatalog`, `IntelligentToolDefinition`, `IntelligentToolContributor`,
`SimpleIntelligentToolDefinition`, `IntelligentToolScope`, `IntelligentToolVariant`.

## Global Constraints

- Work on the current branch. **The user commits to `0_732` in parallel** — always fresh commits,
  never amend, and always `git commit -m "..." -- <paths>` (message flag BEFORE the `--` pathspec).
  Never `git commit -a`.
- Commit messages: `732 <description>` (server), `732 client - <description>` (client). This plan
  touches no client code.
- CE files (`server/libs/**`): Apache 2.0 header copied from a neighbouring file in the same module,
  `@author Ivica Cardic`, NO `@version ee`. EE files (`server/ee/**`): the ByteChef Enterprise header
  copied from a neighbour in the same module, AND a `@version ee` Javadoc tag.
- Java style: exactly one blank line before control statements; one blank line between a variable
  modification and the next statement using it; no trailing blank line before a class's closing brace;
  descriptive names (`definition`, `toolCallback`, `chatClient`, `chatModel` — never `def`, `cb`,
  `cm`); no `TODO:` comments; no empty blocks.
- Test method names camelCase with NO underscores. Unit test classes end in `Test`, never `IntTest`.
- **A model preference must never fail a turn.** Every resolution failure falls back to the default
  client. This is the spec's failure table and it is binding on Task 4.
- **No cache.** The spec's "No cache" section is deliberate — rebuilding a `ChatClient` per delegation
  is what `converterBuildSubAgentChatClientSupplier` already does today. Do not add one.
- Verify with `./gradlew spotlessApply`, then module-scoped `check` redirected to a log file, with
  `echo "exit=$?"` on its own line, then `grep -E '^> Task .* FAILED' <log>`. Never judge a Gradle run
  by a piped tail; never grep for `error:` (it matches module paths like `:server:libs:core:error:`).
- Do NOT edit anything under `docs/superpowers/**`. Do NOT edit any prompt `.txt` file — this plan
  changes no tool names and no agent instructions.

## The evidence this plan rests on

Re-derive before implementing; the branch moves.

**The working precedent for the whole mechanism** — `converterBuildSubAgentChatClientSupplier` in
`server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java`
already resolves a `ChatModel` at call time and rebuilds its `ChatClient` with the same prompt and
tools. Read it first — Task 5 generalises exactly this shape to the other delegates, and Task 5 also
*fixes* it (it currently asks for the environment **default** model via `resolveDefaultChatModel` and
reads the environment from the `EnvironmentContext` ThreadLocal, so it never sees the user's pick).

**`resolveChatModel` already exists**, private, in
`server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/main/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogChatClientResolverImpl.java`.
`resolve(...)` is implemented as `resolveChatModel(...)` then `ChatClient.builder(chatModel).build()`.
Task 1 promotes it to the interface; it does not implement it.

**The two populate sites are single functions:**

```bash
git grep -n "protected Map<String, Object> toolContext" -- 'server/**/*.java'
```

Every Copilot panel agent's `toolContext(RunAgentInput)` returns
`CopilotToolContextUtils.toToolContext(input.state())`, so the panel half is one utility.
`AiHubSpringAIAgent#toolContext(RunAgentInput)` is the hub's.

**The eight delegate callback classes** (nine tools —
`ProjectWorkflowAgentToolCallback` backs both `project_workflow_agent` and
`integration_workflow_agent` through its four-argument constructor):

```bash
git grep -ln "class \(ProjectWorkflowAgent\|Converter\|ClusterElementAgent\|CodeEditorAgent\|SkillsAgent\|WorkflowExecutionAgent\|CustomComponentAgent\|CodeWorkflowAgent\)ToolCallback"
```

Six are CE (`server/libs/ai/ai-copilot/ai-copilot-tool/.../tool/`); `CustomComponentAgentToolCallback`
and `CodeWorkflowAgentToolCallback` are EE
(`server/ee/libs/automation/automation-ai/automation-ai-copilot/.../copilot/tool/`).

Each uses its `ChatClient` field exactly once, inside `call(String, ToolContext)`, in the shape
`chatClient.prompt(request).toolContext(forwardedContext).call().content()`. That single call site is
what changes.

---

### Task 1: The model-resolution plumbing

**Files:**
- Modify: `server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-api/src/main/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogChatClientResolver.java`
- Modify: `server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/main/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogChatClientResolverImpl.java`
- Create: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/catalog/SubAgentChatModelResolver.java`
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/CatalogSubAgentChatModelResolver.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/agent/CatalogSubAgentChatModelResolverTest.java`

**Interfaces:**
- Produces (Tasks 2, 4, 6 depend on these exact shapes):

```java
// CE, com.bytechef.ai.copilot.tool.catalog
public interface SubAgentChatModelResolver {

    /**
     * Resolves the {@link ChatModel} the caller picked, from the tool context a delegate forwards.
     * Returns {@code null} whenever no pick is present or it cannot be honoured — the caller then
     * uses its default client. Never throws.
     */
    @Nullable
    ChatModel resolve(Map<String, Object> toolContext);
}
```

```java
// EE, promoted onto the existing interface
@Nullable
ChatModel resolveChatModel(String providerKey, String model, int environment);
```

- [ ] **Step 1: Promote `resolveChatModel` to the interface.** In `CatalogChatClientResolver`, add the
  declaration above with Javadoc matching the style of its neighbours (`resolve`,
  `resolveDefaultChatModel`): say it mirrors `resolve` but returns the model unwrapped so callers can
  attach their own system prompt and tools, and that out-of-range environments fail closed to `null`.
  In `CatalogChatClientResolverImpl`, change the existing private `resolveChatModel` to `public` and
  add `@Override`. Do not change its body.

- [ ] **Step 2: Write the failing test** for the EE resolver at
  `CatalogSubAgentChatModelResolverTest`. Mock `CatalogChatClientResolver`. Cover every row of the
  spec's failure table:

```java
@Test
void testResolvesThePickedModel() {
    ChatModel chatModel = mock(ChatModel.class);

    when(catalogChatClientResolver.resolveChatModel("anthropic", "claude-opus-4", 1))
        .thenReturn(chatModel);

    Map<String, Object> toolContext = Map.of(
        AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic",
        AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY, "claude-opus-4",
        AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);

    assertThat(resolver.resolve(toolContext)).isSameAs(chatModel);
}

@Test
void testReturnsNullWhenNoPickIsPresent() {
    assertThat(resolver.resolve(Map.of())).isNull();

    verifyNoInteractions(catalogChatClientResolver);
}

@Test
void testReturnsNullWhenOnlyTheProviderIsPresent() {
    Map<String, Object> toolContext = Map.of(
        AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic",
        AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);

    assertThat(resolver.resolve(toolContext)).isNull();

    verifyNoInteractions(catalogChatClientResolver);
}

@Test
void testReturnsNullWhenTheEnvironmentIsMissing() {
    Map<String, Object> toolContext = Map.of(
        AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic",
        AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY, "claude-opus-4");

    assertThat(resolver.resolve(toolContext)).isNull();

    verifyNoInteractions(catalogChatClientResolver);
}

@Test
void testReturnsNullWhenTheCatalogCannotResolveThePair() {
    when(catalogChatClientResolver.resolveChatModel(anyString(), anyString(), anyInt()))
        .thenReturn(null);

    assertThat(resolver.resolve(fullToolContext())).isNull();
}

@Test
void testReturnsNullWhenTheCatalogThrows() {
    when(catalogChatClientResolver.resolveChatModel(anyString(), anyString(), anyInt()))
        .thenThrow(new IllegalStateException("provider exploded"));

    assertThat(resolver.resolve(fullToolContext())).isNull();
}
```

  `TOOL_CONTEXT_LLM_PROVIDER_KEY` and `TOOL_CONTEXT_LLM_MODEL_KEY` do not exist yet — Task 2 adds
  them. **Add them in this task as well** (the two `public static final String` constants only), so
  this task compiles and Task 2 only has to populate them. Their values are
  `"bytechef.agentTool.llmProvider"` and `"bytechef.agentTool.llmModel"`.

- [ ] **Step 3: Run it to verify it fails.**

```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests '*CatalogSubAgentChatModelResolverTest' > /tmp/mp1.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp1.log
```
  Expected: FAIL — `CatalogSubAgentChatModelResolver` does not exist.

- [ ] **Step 4: Implement `CatalogSubAgentChatModelResolver`.** A `@Component @ConditionalOnEEVersion`
  in `com.bytechef.ee.ai.copilot.agent` (beside `CopilotChatClientResolver`, which already injects
  `CatalogChatClientResolver`). It reads the three keys, returns `null` unless all three are present,
  and wraps the catalog call in a `try`/`catch (RuntimeException)` that logs at warn and returns
  `null`.

  Two distinct warn cases, both required by the spec's failure table — do not collapse them into one
  silent `null`:
  - the catalog **throws** → warn with the exception;
  - the catalog **returns null** while all three keys were present → warn naming the provider, model
    and environment. This is the "the caller resolved a model and the delegate could not" case; it is
    otherwise invisible and produces exactly the confusing outcome this design removes. Absent keys
    (no pick, or management MCP) must NOT warn — that is the common path.

  Reuse `com.bytechef.commons.util.NumberUtils#asLong` and
  `com.bytechef.commons.util.StringUtils#asString` for the coercions, as `CopilotChatClientResolver`
  does. Do NOT gate it on `bytechef.ai.copilot.enabled` — the AI Hub needs it too, and the hub's gate
  is independent.

- [ ] **Step 5: Verify.**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:check :server:libs:ai:ai-copilot:ai-copilot-tool:check --continue > /tmp/mp1c.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp1c.log
```

- [ ] **Step 6: Commit.**

```bash
git commit -m "732 Add the subagent chat model resolver and promote resolveChatModel to the catalog interface" -- server/libs/ai/ai-copilot/ai-copilot-tool/src server/ee/libs/ai/ai-copilot/ai-copilot-service/src server/ee/libs/platform/platform-ai/platform-ai-agent
```

---

### Task 2: The carrier — populate the picked model into the tool context

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/context/AgentToolInvocationContext.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/util/CopilotToolContextUtils.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/AiHubSpringAIAgent.java`
- Test: `server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/util/CopilotToolContextUtilsTest.java` (create if absent)
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/agent/AiHubToolContextModelPropagationTest.java`

**Interfaces:**
- Consumes: Task 1's two key constants on `AgentToolInvocationContext`.
- Produces: those keys are populated on both surfaces, and NOT on management MCP.

- [ ] **Step 1: Extend `AgentToolInvocationContext`.** It is a record-plus-builder carrying
  `workspaceId`, `userId`, `environmentId`, `conversationId`, `tenantId`, `authentication`,
  `skipAutomationAuthorization`. Add `llmProvider` and `llmModel` as nullable `String` components,
  with matching builder methods, `toToolContext()` entries (guarded by the same null check the
  existing fields use), and reads in whatever `fromToolContext`-style factory the class already has.
  Follow the file's existing shape exactly — do not restructure it.

- [ ] **Step 2: Write the failing panel test.** In `CopilotToolContextUtilsTest`:

```java
@Test
void testPropagatesTheUserSelectedModel() {
    State state = new State();

    state.set(CopilotConstants.STATE_ENVIRONMENT_ID, 1L);
    state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, "anthropic");
    state.set(CopilotConstants.STATE_USER_SELECTED_LLM_MODEL, "claude-opus-4");

    Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(state);

    assertThat(toolContext)
        .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic")
        .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY, "claude-opus-4");
}

@Test
void testOmitsTheModelKeysWhenHalfSet() {
    State state = new State();

    state.set(CopilotConstants.STATE_ENVIRONMENT_ID, 1L);
    state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, "anthropic");

    Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(state);

    assertThat(toolContext)
        .doesNotContainKey(AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY)
        .doesNotContainKey(AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY);
}
```

  Check `State`'s real setter name before writing this (it may be `set`, `put`, or constructor-based)
  and match it; the assertions are what matter.

- [ ] **Step 3: Write the failing hub test** at `AiHubToolContextModelPropagationTest`. The hub's
  precedence is **user-selected wins over task**, and it must match `AiHubChatClientResolver` exactly.
  Assert three cases: user-selected only → propagated; task only → propagated; both set →
  user-selected wins. Extract the hub's derivation into a package-private static helper on
  `AiHubSpringAIAgent` with this exact shape, so the test can call it without constructing an agent
  and the precedence lives in one readable place:

```java
record SelectedLlm(String provider, String model) {
}

static @Nullable SelectedLlm resolveSelectedLlm(@Nullable State state)
```

- [ ] **Step 4: Run both to verify they fail.**

```bash
./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests '*CopilotToolContextUtilsTest' :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*AiHubToolContextModelPropagationTest' --continue > /tmp/mp2.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp2.log
```

- [ ] **Step 5: Implement both populate sites.**
  - `CopilotToolContextUtils.toToolContext`: read
    `CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER` / `_MODEL` via `StringUtils.asString`, and
    pass them to the existing `AgentToolInvocationContext.builder()` chain via the new builder
    methods — but only when BOTH are non-null. Half-set logs a warn once and propagates neither,
    matching `CopilotChatClientResolver`'s existing treatment.
  - `AiHubSpringAIAgent.toolContext`: same, through the new `resolveSelectedLlm(State)` helper, whose
    body mirrors `AiHubChatClientResolver.resolve`'s precedence — user-selected
    (`AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY` / `USER_SELECTED_LLM_MODEL_KEY`) first, falling
    through to task (`TASK_LLM_PROVIDER_KEY` / `TASK_LLM_MODEL_KEY`) when the user-selected pair is
    not fully set.
  - Add a Javadoc line on both saying the keys must stay consistent with the surface's
    `OverrideChatClientResolver`, or a delegate will run on a different model than its caller.

- [ ] **Step 6: Verify.**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:check :server:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:ai:ai-hub:ai-hub-service:check --continue > /tmp/mp2c.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp2c.log
```

- [ ] **Step 7: Commit.**

```bash
git commit -m "732 Carry the selected LLM provider and model in the agent tool context" -- server/libs/ai/ai-copilot server/ee/libs/ai/ai-hub/ai-hub-service/src
```

---

### Task 3: The catalog factory seam (pure refactor)

**Files:**
- Create: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/catalog/IntelligentToolChatClientFactory.java`
- Modify: `.../catalog/IntelligentToolDefinition.java`
- Modify: `.../catalog/SimpleIntelligentToolDefinition.java`
- Modify: `.../catalog/IntelligentToolCatalog.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotIntelligentToolContributor.java`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-copilot/src/main/java/com/bytechef/ee/automation/ai/copilot/config/AutomationIntelligentToolContributor.java`
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/main/java/com/bytechef/ee/embedded/ai/copilot/config/EmbeddedIntelligentToolContributor.java`
- Test: `.../catalog/IntelligentToolCatalogTest.java` (modify)

**Interfaces:**
- Produces (Tasks 4 and 5 depend on these exact shapes):

```java
// CE, com.bytechef.ai.copilot.tool.catalog
@FunctionalInterface
public interface IntelligentToolChatClientFactory {

    /**
     * @param chatModel the model the caller picked, or {@code null} for the contributor's default
     *                  client
     */
    ChatClient get(@Nullable ChatModel chatModel);
}
```

```java
// IntelligentToolDefinition — these two methods REPLACE chatClientSupplier / create(Supplier)
@Nullable
IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant);

ToolCallback create(IntelligentToolChatClientFactory chatClientFactory);
```

**This task changes no behaviour.** Every contributor produces a factory that IGNORES its `chatModel`
argument and returns today's client. The parity test must stay green throughout — that is the proof.

- [ ] **Step 1: Write the failing catalog test.** In `IntelligentToolCatalogTest`, add:

```java
@Test
void testTheChatModelReachesTheDefinitionFactory() {
    ChatModel chatModel = mock(ChatModel.class);
    AtomicReference<ChatModel> received = new AtomicReference<>();

    IntelligentToolDefinition definition = definitionWithFactory(
        candidate -> {
            received.set(candidate);

            return mock(ChatClient.class);
        });

    List<ToolCallback> toolCallbacks = catalogOf(definition)
        .getByNames(
            Set.of("test_tool"), IntelligentToolVariant.BUILD, (chatClient, current) -> chatClient,
            (toolCallback, current) -> toolCallback);

    assertThat(toolCallbacks).hasSize(1);

    // the factory is handed to create(...); the callback decides when to invoke it
    assertThat(received.get()).isNull();
}
```

  Keep the existing laziness test and extend it: the raw factory must not be invoked while the catalog
  getter runs.

- [ ] **Step 2: Run it to verify it fails** (compilation — `definitionWithFactory` and the new types
  do not exist):

```bash
./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*IntelligentToolCatalogTest' > /tmp/mp3.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp3.log
```

- [ ] **Step 3: Add `IntelligentToolChatClientFactory`** and change `IntelligentToolDefinition`'s two
  methods to the signatures above. Update the interface Javadoc: state that the factory is invoked
  per delegation, not per registration, and that a definition MUST honour a non-null `chatModel` by
  rebuilding its client with the same system prompt and tools.

- [ ] **Step 4: Update `IntelligentToolCatalog`'s resolution order** to compose the decorator inside
  the factory:

```java
IntelligentToolChatClientFactory rawChatClientFactory = definition.chatClientFactory(variant);

if (rawChatClientFactory == null) {
    continue;
}

IntelligentToolChatClientFactory decoratedChatClientFactory =
    chatModel -> chatClientDecorator.apply(rawChatClientFactory.get(chatModel), definition);

ToolCallback toolCallback = definition.create(decoratedChatClientFactory);

toolCallbacks.add(callbackDecorator.apply(toolCallback, definition));
```

  The per-surface decorator now runs per delegation rather than per registration. Note that in the
  method's Javadoc: it is what keeps guardrails, the workspace system prompt and session memory
  attached to the re-targeted client.

- [ ] **Step 5: Update `SimpleIntelligentToolDefinition`** to carry a
  `Function<IntelligentToolVariant, IntelligentToolChatClientFactory>` in place of its supplier
  function, and a `Function<IntelligentToolChatClientFactory, ToolCallback>` for `create`.

- [ ] **Step 6: Update the three contributors mechanically.** Each existing
  `chatClientSupplier(variant, askProvider, buildProvider)` helper becomes a factory helper that
  returns `chatModel -> <today's client>` — ignoring `chatModel` for now. The converter definition
  keeps passing its `Supplier<ChatClient>` straight through to `ConverterAgentToolCallback` in this
  task; Task 4 changes that. Nothing about which client is chosen may change here.

- [ ] **Step 7: Verify — the parity test is the gate.**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:check :server:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:automation:automation-ai:automation-ai-copilot:check :server:ee:libs:embedded:embedded-ai:embedded-ai-copilot:check :server:ee:libs:ai:ai-hub:ai-hub-service:check --continue > /tmp/mp3c.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp3c.log
```
  `IntelligentToolSurfaceParityTest` must still pass unchanged. If it needed editing to compile, that
  is fine; if it needed editing to PASS, stop and report — behaviour changed.

- [ ] **Step 8: Commit.**

```bash
git commit -m "732 Turn the intelligent tool catalog's chat client into a per-delegation factory" -- server/libs/ai/ai-copilot server/ee/libs/automation/automation-ai/automation-ai-copilot/src server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src
```

---

### Task 4: The callbacks resolve the model per invocation

**Files:**
- Modify (CE, `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/`):
  `ProjectWorkflowAgentToolCallback.java`, `ConverterAgentToolCallback.java`,
  `ClusterElementAgentToolCallback.java`, `CodeEditorAgentToolCallback.java`,
  `SkillsAgentToolCallback.java`, `WorkflowExecutionAgentToolCallback.java`
- Modify (EE, `server/ee/libs/automation/automation-ai/automation-ai-copilot/src/main/java/com/bytechef/ee/automation/ai/copilot/tool/`):
  `CustomComponentAgentToolCallback.java`, `CodeWorkflowAgentToolCallback.java`
- Modify: the three contributors, to pass the resolver through
- Test: `server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/DelegateChatModelResolutionTest.java`

**Interfaces:**
- Consumes: `IntelligentToolChatClientFactory` (Task 3), `SubAgentChatModelResolver` (Task 1).
- Produces: each callback's constructor takes `(IntelligentToolChatClientFactory chatClientFactory,
  @Nullable SubAgentChatModelResolver chatModelResolver, …)` in place of its `ChatClient`.

- [ ] **Step 1: Write the failing test** covering the shared behaviour once, parameterised over the
  callbacks:

```java
@Test
void testUsesTheResolvedChatModelForTheDelegation() {
    ChatModel chatModel = mock(ChatModel.class);
    AtomicReference<ChatModel> received = new AtomicReference<>();

    SubAgentChatModelResolver chatModelResolver = toolContext -> chatModel;

    ToolCallback toolCallback = new ProjectWorkflowAgentToolCallback(
        candidate -> {
            received.set(candidate);

            return stubChatClient("done");
        },
        chatModelResolver);

    toolCallback.call("{\"request\": \"build it\"}", new ToolContext(Map.of()));

    assertThat(received.get()).isSameAs(chatModel);
}

@Test
void testFallsBackToTheDefaultClientWhenTheResolverReturnsNull() { /* received.get() is null */ }

@Test
void testFallsBackToTheDefaultClientWhenTheResolverThrows() { /* received.get() is null, no exception escapes */ }

@Test
void testFallsBackToTheDefaultClientWhenThereIsNoResolver() { /* resolver == null */ }
```

  `stubChatClient(String)` is a Mockito deep-stub returning the given content from
  `prompt(...).toolContext(...).call().content()`. Write it once as a private helper.

- [ ] **Step 2: Run it to verify it fails.**

```bash
./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*DelegateChatModelResolutionTest' > /tmp/mp4.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp4.log
```

- [ ] **Step 3: Change each of the eight callbacks.** Replace the `ChatClient` field with the factory
  plus a nullable resolver, and change the single call site inside `call(String, ToolContext)`:

```java
Map<String, Object> parentContext = toolContext == null ? Map.of() : toolContext.getContext();

ChatClient chatClient = chatClientFactory.get(resolveChatModel(parentContext));
```

  with one shared private helper per class (or a small package-private static utility — your call,
  but do not duplicate the try/catch eight times if a utility reads better):

```java
private @Nullable ChatModel resolveChatModel(Map<String, Object> parentContext) {
    if (chatModelResolver == null) {
        return null;
    }

    try {
        return chatModelResolver.resolve(parentContext);
    } catch (RuntimeException exception) {
        log.warn("Subagent chat model resolution failed; using the default client", exception);

        return null;
    }
}
```

  `ProjectWorkflowAgentToolCallback`'s four-argument constructor (name, description, agent type) keeps
  those parameters — only its `ChatClient` becomes a factory. `ConverterAgentToolCallback`'s
  `Supplier<ChatClient>` constructor is REPLACED by the factory; the supplier shape is subsumed
  (`factory.get(null)` is the old supplier).

- [ ] **Step 4: Thread the resolver through the three contributors.** Each injects
  `ObjectProvider<SubAgentChatModelResolver>` on its `@Bean` method and passes
  `provider.getIfAvailable()` into each `create`. A null resolver (CE, or the AI Hub with EE copilot
  absent) means "always default", which is the pre-change behaviour.

- [ ] **Step 5: Verify.**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:check :server:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:automation:automation-ai:automation-ai-copilot:check :server:ee:libs:embedded:embedded-ai:embedded-ai-copilot:check :server:ee:libs:ai:ai-hub:ai-hub-service:check --continue > /tmp/mp4c.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp4c.log
```
  Then repo-wide:
```bash
./gradlew compileJava compileTestJava --continue > /tmp/mp4d.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp4d.log
```

- [ ] **Step 6: Commit.**

```bash
git commit -m "732 Resolve the delegate chat model per invocation from the tool context" -- server/libs/ai/ai-copilot server/ee/libs/automation/automation-ai/automation-ai-copilot/src server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src
```

---

### Task 5: The contributor factories honour the model

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotIntelligentToolContributor.java`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-copilot/src/main/java/com/bytechef/ee/automation/ai/copilot/config/AutomationCopilotConfiguration.java`
- Modify: `.../config/AutomationIntelligentToolContributor.java`
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/main/java/com/bytechef/ee/embedded/ai/copilot/config/EmbeddedCopilotConfiguration.java`
- Modify: `.../config/EmbeddedIntelligentToolContributor.java`
- Test: `server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/CopilotIntelligentToolContributorTest.java` (modify)

**This is where behaviour lands.** Until now every factory ignored its `chatModel`.

- [ ] **Step 1: Read `converterBuildSubAgentChatClientSupplier`** in `CopilotConfiguration`. It is the
  template: it already rebuilds a `ChatClient` over a resolved `ChatModel` with the same prompt and
  tools. You are generalising its body and changing where the model comes from.

- [ ] **Step 2: Write the failing contributor test.** In `CopilotIntelligentToolContributorTest`, for
  each of the six definitions:

```java
@Test
void testTheFactoryBuildsADistinctClientForAnOverrideModel() {
    IntelligentToolChatClientFactory factory =
        definitionNamed("project_workflow_agent").chatClientFactory(IntelligentToolVariant.BUILD);

    ChatClient defaultChatClient = factory.get(null);
    ChatClient overriddenChatClient = factory.get(mock(ChatModel.class));

    assertThat(overriddenChatClient).isNotSameAs(defaultChatClient);
}

@Test
void testTheFactoryReturnsTheBeanClientForANullModel() {
    IntelligentToolChatClientFactory factory =
        definitionNamed("project_workflow_agent").chatClientFactory(IntelligentToolVariant.BUILD);

    assertThat(factory.get(null)).isSameAs(workflowEditorBuildSubAgentChatClient);
}
```

- [ ] **Step 3: Run it to verify it fails** (both currently return the same bean instance):

```bash
./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests '*CopilotIntelligentToolContributorTest' > /tmp/mp5.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp5.log
```

- [ ] **Step 4: Extract a builder per delegate in each configuration class.** For each
  `*SubAgentChatClient` `@Bean`, add a package-private method that takes the `ChatModel` and returns
  the built client, and reduce the `@Bean` to a call to it. Worked example for
  `workflowEditorBuildSubAgentChatClient`:

```java
@Bean
ChatClient workflowEditorBuildSubAgentChatClient(
    ChatModel chatModel, ProjectAuthoringTools projectAuthoringTools, ProjectWorkflowTools projectWorkflowTools,
    TaskTools taskTools, ScriptTools scriptTools, SimulationTools simulationTools) {

    return buildWorkflowEditorBuildSubAgentChatClient(
        chatModel, projectAuthoringTools, projectWorkflowTools, taskTools, scriptTools, simulationTools);
}

ChatClient buildWorkflowEditorBuildSubAgentChatClient(
    ChatModel chatModel, ProjectAuthoringTools projectAuthoringTools, ProjectWorkflowTools projectWorkflowTools,
    TaskTools taskTools, ScriptTools scriptTools, SimulationTools simulationTools) {

    return ChatClient.builder(chatModel)
        .defaultSystem(getSystemPrompt(promptWorkflowEditorBuildResource))
        .defaultTools(
            projectAuthoringTools, projectWorkflowTools, taskTools, scriptTools, simulationTools,
            workflowValidatorTools, workflowInstructionTools)
        // One-shot subagent (backs the management MCP workflow_editor agent + AI Hub delegation): give it
        // lookupPropertyOptions so it fetches real option values for dynamic-option properties and sets a valid
        // one itself. Not the interactive askUserQuestion/select picker — a one-shot subagent can't ask + resume.
        .defaultToolCallbacks(
            new LookupComponentPropertyOptionsToolCallback(
                actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
                propertyOptionsResolver, ToolStateVisibilityMetrics.NOOP))
        .build();
}
```

  Do the same for `workflowEditorAsk…`, `clusterElement{Ask,Build}…`, `codeEditor{Ask,Build}…`,
  `skills{Ask,Build}…`, `workflowExecution{Ask,Build}…` and `converterBuildSubAgentChatClient` in
  `CopilotConfiguration`; `customComponent{Ask,Build}…` and `codeWorkflow{Ask,Build}…` in
  `AutomationCopilotConfiguration`; `workflowEditorEmbeddedBuild…` in `EmbeddedCopilotConfiguration`.
  Copy each existing `@Bean` body verbatim into its builder — do not "improve" prompts, tool lists or
  comments while moving them.

- [ ] **Step 5: Hoist the prompt reads.** `getSystemPrompt(resource)` performs I/O and now runs per
  delegation. Read each prompt once into a `private final String`-style field (or a memoised
  supplier) at configuration init, and have the builders use that. Keep `getSystemPrompt` for any
  remaining callers.

- [ ] **Step 6: Point the contributors' factories at the builders.** Each definition's factory becomes
  `chatModel -> chatModel == null ? <the bean client> : <the builder>(chatModel, …)`.

  **Default choice: expose each builder as an `IntelligentToolChatClientFactory` `@Bean` from the
  configuration class that already owns the collaborators**, and inject those factory beans into the
  contributor by qualifier:

```java
// in CopilotConfiguration, beside the ChatClient @Bean
@Bean
IntelligentToolChatClientFactory workflowEditorBuildSubAgentChatClientFactory(
    ChatModel chatModel, ProjectAuthoringTools projectAuthoringTools, ProjectWorkflowTools projectWorkflowTools,
    TaskTools taskTools, ScriptTools scriptTools, SimulationTools simulationTools) {

    return candidateChatModel -> buildWorkflowEditorBuildSubAgentChatClient(
        candidateChatModel == null ? chatModel : candidateChatModel, projectAuthoringTools, projectWorkflowTools,
        taskTools, scriptTools, simulationTools);
}
```

  This keeps every collaborator list in the class that already declares it, and leaves the contributor
  holding only qualifiers — the shape it has today. Take the alternative (injecting the collaborators
  into the contributor) only if a factory bean proves impossible for some delegate, and say which and
  why in your report.

  Note the consequence for Step 4's `@Bean ChatClient`: it now reads
  `return workflowEditorBuildSubAgentChatClientFactory(...).get(null);` or simply calls the builder
  with the default `chatModel` — either is fine, but there must be exactly one definition of the
  prompt and tool list.

- [ ] **Step 7: Retire the converter's ThreadLocal path.** With the factory receiving the picked model,
  `converterBuildSubAgentChatClientSupplier`'s `EnvironmentContext.fetchCurrentEnvironment()` +
  `resolveDefaultChatModel` branch is superseded. Reduce that bean to the plain default client and let
  the factory do the re-targeting, so there is exactly one model-resolution path. If any other caller
  still consumes the supplier bean, leave the bean and note it in your report.

- [ ] **Step 8: Verify.**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:automation:automation-ai:automation-ai-copilot:check :server:ee:libs:embedded:embedded-ai:embedded-ai-copilot:check :server:ee:libs:ai:ai-hub:ai-hub-service:check --continue > /tmp/mp5c.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp5c.log
```

- [ ] **Step 9: Commit.**

```bash
git commit -m "732 Rebuild delegate chat clients over the selected model" -- server/libs/ai/ai-copilot server/ee/libs/automation/automation-ai/automation-ai-copilot/src server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src
```

---

### Task 6: Surface-level assertions and docs

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/config/IntelligentToolSurfaceParityTest.java`
- Modify: `CLAUDE.md`

- [ ] **Step 1: Extend the parity test.** For every contributed definition and every variant it
  offers, assert the factory returns a different client for a mock `ChatModel` than for `null`. A
  definition that ignores the override is the one failure this design cannot tolerate, and the parity
  test is the only place that sees all nine.

- [ ] **Step 2: Assert MCP defaults structurally.** Add a test asserting that the management-MCP tool
  context contains neither `TOOL_CONTEXT_LLM_PROVIDER_KEY` nor `TOOL_CONTEXT_LLM_MODEL_KEY` — the
  contexts built by `WorkspaceScopedSubAgentToolCallback`. "MCP keeps the default" then holds because
  of the code, not because of a comment.

- [ ] **Step 3: Document it in `CLAUDE.md`**, in the "AI Hub agent tool architecture (EE)" section:
  the nine catalog delegates follow the caller's picked model; management MCP does not, structurally;
  the resolution path is tool-context keys → `SubAgentChatModelResolver` → factory; and the hub's
  precedence must stay in step with `AiHubChatClientResolver`. Three or four sentences.

- [ ] **Step 4: Full verify.**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew compileJava compileTestJava --continue > /tmp/mp6.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp6.log
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:check --continue > /tmp/mp6c.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/mp6c.log
```

- [ ] **Step 5: Commit** the test and the doc change separately.

---

## Manual verification (requires a running backend)

1. AI Hub, pick a non-default provider+model in the composer, ask for a workflow. The delegation to
   `project_workflow_agent` runs on the picked model — verify via the AI-gateway/LLM-usage rows for
   the turn, which record the model per call.
2. Same request with no pick: delegate runs on the default. No warnings in the log.
3. Projects Copilot panel: repeat 1 and 2.
4. Management MCP: invoke `project_workflow_agent` through an MCP client. It runs on the default and
   logs nothing about model resolution.
5. Pick a provider that is disabled in the environment: the turn completes on the default, with one
   warn naming the provider.

## Self-review notes

- **The riskiest task is 5.** It moves ten-plus `@Bean` bodies. Copy verbatim; a silently reworded
  prompt or a dropped tool would not fail any test in this plan.
- **Task 3 must not change behaviour.** If `IntelligentToolSurfaceParityTest` needs editing to PASS
  (as opposed to compile), something moved that should not have.
- **Not in scope:** the five CRUD delegates, the four AI-Hub generative one-shots, per-delegate model
  overrides, restoring either resolver's commented-out AI Gateway fallback, and anything the
  intelligent-tool renames plan covers.
