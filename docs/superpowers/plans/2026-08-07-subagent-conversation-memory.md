# Subagent Conversation Memory Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give each AI Hub specialist subagent a durable, per-conversation memory so a multi-turn refinement stops restarting from zero on every delegation.

**Architecture:** Generalize the existing `SubAgentGuardrailedChatClient` decorator from two hardcoded advisors to a list of `SubAgentAdvisorContributor`s. Guardrails-plus-workspace-prompt becomes one contributor; a new session-memory contributor becomes another, attaching a `SessionMemoryAdvisor` keyed `<parentThreadId>:<agentType>` over the same session store the parent uses. All resolution happens per request from the forwarded `ToolContext`, because the delegate `ChatClient` beans are process-wide singletons.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI, `org.springaicommunity:spring-ai-session` 0.7.0, JUnit 5, Mockito, AssertJ.

**Spec:** `docs/superpowers/specs/2026-08-07-subagent-conversation-memory-design.md`

## Global Constraints

- All files under `server/ee/` use the **ByteChef Enterprise license header**, not Apache 2.0, and carry a `@version ee` Javadoc tag. Spotless selects the header by that tag's presence in the file content, not by path.
- Unit test classes end in `Test`; integration tests end in `IntTest`. Test method names are camelCase with **no underscores** (Checkstyle enforces this on every method in test sources, including private helpers).
- No `TODO:` comments — Checkstyle's `TodoComment` rule rejects them.
- Insert exactly one blank line before control statements (`if`, `for`, `try`, …), except immediately after an opening `{`.
- Insert one blank line between a variable modification and the next statement that uses that variable.
- No trailing blank line before a class's closing `}`.
- Descriptive variable names — no single letters, no cryptic abbreviations, including lambda parameters.
- Run `./gradlew spotlessApply` before every commit.
- Never judge a Gradle run piped into `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep for `^> Task .* FAILED`.
- Commit messages on this branch use the form `0 <description>` for server-side changes.

---

### Task 1: Extract the advisor-contributor seam

Pure refactor. Behavior must not change, and the existing `SubAgentGuardrailedChatClientTest` must stay green untouched — that is the proof.

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/subagent/SubAgentAdvisorContributor.java`
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/subagent/WorkspaceAdvisorContributor.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/guardrails/SubAgentGuardrailedChatClient.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent/WorkspaceAdvisorContributorTest.java`

**Interfaces:**
- Produces: `SubAgentAdvisorContributor.contribute(ChatClientRequestSpec, Map<String, Object>) -> ChatClientRequestSpec`; `WorkspaceAdvisorContributor(AiGuardrails, AiGuardrailMetrics, WorkspaceSystemPrompts)`; `SubAgentGuardrailedChatClient.wrap(ChatClient, List<SubAgentAdvisorContributor>) -> ChatClient`. The pre-existing 4-arg `wrap(ChatClient, AiGuardrails, AiGuardrailMetrics, WorkspaceSystemPrompts)` keeps working with identical behavior.

The contributor returns the spec rather than a `List<Advisor>` because a contributor may need to set request params, not only attach advisors — Task 3 depends on this. It mirrors the decorator's existing `delegateSpec = delegateSpec.advisors(...)` reassignment idiom, which exists because nothing in the `ChatClientRequestSpec` contract guarantees the fluent methods return `this`.

- [ ] **Step 1: Write the contributor interface**

```java
package com.bytechef.ee.ai.hub.subagent;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

/**
 * Contributes advisors (and, where needed, advisor params) to a subagent delegate's request, given the
 * {@code ToolContext} the delegate forwarded from the parent agent.
 *
 * <p>
 * Returns the spec rather than a list of advisors because a contributor may need to set request params — the session
 * memory contributor must publish the conversation key the memory advisor resolves its session from. Implementations
 * MUST return the spec produced by their own calls rather than the argument: the Spring AI implementation happens to
 * mutate and return {@code this}, but the contract does not guarantee it.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface SubAgentAdvisorContributor {

    ChatClientRequestSpec contribute(
        ChatClientRequestSpec chatClientRequestSpec, @Nullable Map<String, Object> toolContext);
}
```

- [ ] **Step 2: Write the failing test for the extracted workspace contributor**

```java
package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.platform.ai.guardrails.AiGuardrails;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

class WorkspaceAdvisorContributorTest {

    @Test
    void testContributeSkipsAdvisorWhenGuardrailsInactive() {
        AiGuardrails aiGuardrails = mock(AiGuardrails.class);
        AiGuardrailMetrics aiGuardrailMetrics = mock(AiGuardrailMetrics.class);
        ChatClientRequestSpec chatClientRequestSpec = mock(ChatClientRequestSpec.class);

        when(aiGuardrails.isActive(any())).thenReturn(false);

        WorkspaceAdvisorContributor contributor = new WorkspaceAdvisorContributor(
            aiGuardrails, aiGuardrailMetrics, null);

        ChatClientRequestSpec result = contributor.contribute(chatClientRequestSpec, Map.of());

        assertThat(result).isSameAs(chatClientRequestSpec);

        verify(chatClientRequestSpec, never()).advisors(any(org.springframework.ai.chat.client.advisor.api.Advisor[].class));
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*WorkspaceAdvisorContributorTest*' > /tmp/red1.log 2>&1; echo $?`

Expected: non-zero exit, compilation failure — `WorkspaceAdvisorContributor` does not exist.

- [ ] **Step 4: Implement `WorkspaceAdvisorContributor` by moving the existing logic**

Move the body of `SubAgentGuardrailedChatClient.attachWorkspaceAdvisorsIfActive()` verbatim. Do not change the conditions — `isActive(workspaceId)` for guardrails, `fetchPrompt(workspaceId) != null` for the prompt — and keep the null-workspace tenant-default fallback.

```java
package com.bytechef.ee.ai.hub.subagent;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.platform.ai.guardrails.AiGuardrails;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Attaches the calling workspace's guardrails advisor and system-prompt advisor to a subagent delegate request.
 * Extracted verbatim from {@code SubAgentGuardrailedChatClient} when that decorator was generalized to a list of
 * contributors; the conditions are unchanged, including the null-workspace tenant-default fallback.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class WorkspaceAdvisorContributor implements SubAgentAdvisorContributor {

    private final @Nullable AiGuardrails aiGuardrails;
    private final @Nullable AiGuardrailMetrics aiGuardrailMetrics;
    private final @Nullable WorkspaceSystemPrompts workspaceSystemPrompts;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceAdvisorContributor(
        @Nullable AiGuardrails aiGuardrails, @Nullable AiGuardrailMetrics aiGuardrailMetrics,
        @Nullable WorkspaceSystemPrompts workspaceSystemPrompts) {

        this.aiGuardrails = aiGuardrails;
        this.aiGuardrailMetrics = aiGuardrailMetrics;
        this.workspaceSystemPrompts = workspaceSystemPrompts;
    }

    @Override
    public ChatClientRequestSpec contribute(
        ChatClientRequestSpec chatClientRequestSpec, @Nullable Map<String, Object> toolContext) {

        Long workspaceId = resolveWorkspaceId(toolContext);

        ChatClientRequestSpec resultSpec = chatClientRequestSpec;

        if (aiGuardrails != null && aiGuardrailMetrics != null && aiGuardrails.isActive(workspaceId)) {
            resultSpec = resultSpec.advisors(new AiGuardrailsAdvisor(aiGuardrails, workspaceId, aiGuardrailMetrics));
        }

        if (workspaceSystemPrompts != null && workspaceSystemPrompts.fetchPrompt(workspaceId) != null) {
            resultSpec = resultSpec.advisors(new WorkspaceSystemPromptAdvisor(workspaceSystemPrompts, workspaceId));
        }

        return resultSpec;
    }

    private static @Nullable Long resolveWorkspaceId(@Nullable Map<String, Object> toolContext) {
        if (toolContext == null || toolContext.isEmpty()) {
            return null;
        }

        AgentToolInvocationContext context = AgentToolInvocationContext.fromToolContext(new ToolContext(toolContext));

        return context == null ? null : context.workspaceId();
    }
}
```

Import `WorkspaceSystemPrompts` and `WorkspaceSystemPromptAdvisor` from wherever `SubAgentGuardrailedChatClient` currently imports them — copy those import lines exactly rather than guessing the package.

- [ ] **Step 5: Rewrite the decorator to dispatch contributors**

In `SubAgentGuardrailedChatClient`: replace the three advisor fields with `private final List<SubAgentAdvisorContributor> contributors;`, delete `attachWorkspaceAdvisorsIfActive()` and `resolveWorkspaceId(...)`, and replace both call sites with a loop.

```java
    public static ChatClient wrap(ChatClient chatClient, List<SubAgentAdvisorContributor> contributors) {
        if (contributors.isEmpty()) {
            return chatClient;
        }

        return new SubAgentGuardrailedChatClient(chatClient, List.copyOf(contributors));
    }

    public static ChatClient wrap(
        ChatClient chatClient, @Nullable AiGuardrails aiGuardrails, @Nullable AiGuardrailMetrics aiGuardrailMetrics,
        @Nullable WorkspaceSystemPrompts workspaceSystemPrompts) {

        boolean guardrailsPresent = aiGuardrails != null && aiGuardrailMetrics != null;

        if (!guardrailsPresent && workspaceSystemPrompts == null) {
            return chatClient;
        }

        return wrap(
            chatClient,
            List.of(
                new WorkspaceAdvisorContributor(
                    guardrailsPresent ? aiGuardrails : null, guardrailsPresent ? aiGuardrailMetrics : null,
                    workspaceSystemPrompts)));
    }
```

And in the inner spec class:

```java
        private void attachContributedAdvisors() {
            for (SubAgentAdvisorContributor contributor : contributors) {
                delegateSpec = contributor.contribute(delegateSpec, capturedToolContext);
            }
        }
```

Call `attachContributedAdvisors()` from `call()` and `stream()` where `attachWorkspaceAdvisorsIfActive()` was called.

- [ ] **Step 6: Run the new test and the whole existing decorator suite**

Run:
```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test \
  --tests '*WorkspaceAdvisorContributorTest*' \
  --tests '*SubAgentGuardrailedChatClientTest*' > /tmp/green1.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/green1.log
```

Expected: exit 0, no FAILED tasks. All ten pre-existing `SubAgentGuardrailedChatClientTest` methods must pass **without modification** — if any needed editing, the refactor changed behavior and must be corrected rather than the test.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/subagent \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/guardrails/SubAgentGuardrailedChatClient.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent
git commit -m "0 Extract a contributor seam from the subagent chat client"
```

---

### Task 2: Expose registered agent-type keys

**Files:**
- Modify: `server/libs/ai/ai-api/src/main/java/com/bytechef/ai/agent/tool/AgentTypeRegistry.java`
- Test: `server/libs/ai/ai-api/src/test/java/com/bytechef/ai/agent/tool/AgentTypeRegistryTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `AgentTypeRegistry.keys() -> Set<String>`, used by Task 5 to construct the specialist session keys to purge.

This is CE code (Apache header, no `@version ee` tag).

- [ ] **Step 1: Write the failing test**

```java
    @Test
    void testKeysIncludeProviderContributedTypes() {
        Set<String> keys = AgentTypeRegistry.keys();

        assertThat(keys).contains(CoreAgentType.UNKNOWN.key());
        assertThat(keys).allSatisfy(key -> assertThat(AgentTypeRegistry.fromKey(key)).isNotNull());
    }
```

- [ ] **Step 2: Run it and verify it fails**

Run: `./gradlew :server:libs:ai:ai-api:test --tests '*AgentTypeRegistryTest*' > /tmp/red2.log 2>&1; echo $?`

Expected: non-zero — `keys()` does not exist.

- [ ] **Step 3: Add the accessor**

```java
    /**
     * The keys of every registered agent type. Used to construct the deterministic per-specialist session keys that a
     * task delete must purge; {@code SessionRepository} has no prefix listing, so the keys are constructed rather than
     * discovered.
     */
    public static Set<String> keys() {
        return Set.copyOf(AGENT_TYPES_BY_KEY.keySet());
    }
```

Add `import java.util.Set;`.

- [ ] **Step 4: Run and verify it passes**

Run: `./gradlew :server:libs:ai:ai-api:test --tests '*AgentTypeRegistryTest*' > /tmp/green2.log 2>&1; echo $?`

Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/ai/ai-api/src/main/java/com/bytechef/ai/agent/tool/AgentTypeRegistry.java \
        server/libs/ai/ai-api/src/test/java/com/bytechef/ai/agent/tool/AgentTypeRegistryTest.java
git commit -m "0 Expose registered agent type keys from AgentTypeRegistry"
```

---

### Task 3: The session-memory contributor

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/subagent/SubAgentSessionMemoryContributor.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent/SubAgentSessionMemoryContributorTest.java`

**Interfaces:**
- Consumes: `SubAgentAdvisorContributor` (Task 1).
- Produces: `SubAgentSessionMemoryContributor(AiHubSessionMemory, String agentTypeKey)` and the public constant `SubAgentSessionMemoryContributor.MAX_EVENTS`. Task 4 constructs it; Task 5 reuses its key format via `sessionKey(String threadId, String agentTypeKey)`.

The key derivation is `public static` precisely so Task 5's purge and this contributor cannot drift apart.

- [ ] **Step 1: Write the failing tests**

```java
    @Test
    void testSessionKeyCombinesThreadIdAndAgentType() {
        assertThat(SubAgentSessionMemoryContributor.sessionKey("thread-1", "personal_agent_manager"))
            .isEqualTo("thread-1:personal_agent_manager");
    }

    @Test
    void testDifferentAgentTypesOnSameThreadGetDifferentKeys() {
        String firstKey = SubAgentSessionMemoryContributor.sessionKey("thread-1", "personal_agent_manager");
        String secondKey = SubAgentSessionMemoryContributor.sessionKey("thread-1", "mcp_manager");

        assertThat(firstKey).isNotEqualTo(secondKey);
    }

    @Test
    void testContributeSkipsWhenNoConversationId() {
        ChatClientRequestSpec chatClientRequestSpec = mock(ChatClientRequestSpec.class);

        SubAgentSessionMemoryContributor contributor = new SubAgentSessionMemoryContributor(
            sessionMemory, "personal_agent_manager");

        ChatClientRequestSpec result = contributor.contribute(chatClientRequestSpec, Map.of());

        assertThat(result).isSameAs(chatClientRequestSpec);
    }
```

- [ ] **Step 2: Run and verify they fail**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*SubAgentSessionMemoryContributorTest*' > /tmp/red3.log 2>&1; echo $?`

Expected: non-zero — the class does not exist.

- [ ] **Step 3: Implement the contributor**

```java
    public static final int MAX_EVENTS = 10;

    public static String sessionKey(String threadId, String agentTypeKey) {
        return threadId + ":" + agentTypeKey;
    }

    @Override
    public ChatClientRequestSpec contribute(
        ChatClientRequestSpec chatClientRequestSpec, @Nullable Map<String, Object> toolContext) {

        String conversationId = resolveConversationId(toolContext);

        if (conversationId == null || conversationId.isBlank()) {
            return chatClientRequestSpec;
        }

        String sessionId = sessionKey(conversationId, agentTypeKey);

        try {
            SessionMemoryAdvisor sessionMemoryAdvisor = SessionMemoryAdvisor
                .builder(aiHubSessionMemory.sessionService())
                .defaultUserId(AiHubSessionMemory.SESSION_USER_ID)
                .eventFilter(EventFilter.lastN(MAX_EVENTS))
                .build();

            return chatClientRequestSpec
                .advisors(sessionMemoryAdvisor)
                .advisors(advisorSpec -> advisorSpec.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId));
        } catch (RuntimeException exception) {
            log.warn("Failed to attach subagent session memory for sessionId {}; continuing stateless", sessionId,
                exception);

            return chatClientRequestSpec;
        }
    }

    private static @Nullable String resolveConversationId(@Nullable Map<String, Object> toolContext) {
        if (toolContext == null || toolContext.isEmpty()) {
            return null;
        }

        AgentToolInvocationContext context = AgentToolInvocationContext.fromToolContext(new ToolContext(toolContext));

        return context == null ? null : context.conversationId();
    }
```

The param must be set — the advisor resolves its session from `SESSION_ID_CONTEXT_KEY` in the request context, and a delegate's `chatClient.prompt(request)` sets no advisor params of its own. Without it the advisor has no session id and the memory silently does nothing.

Do **not** set `.order(...)`: the default precedence places the advisor outside the tool-calling loop, which is what the spec calls for. Do **not** use `messageFilter` — that governs writes, not replay.

- [ ] **Step 4: Run and verify they pass**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*SubAgentSessionMemoryContributorTest*' > /tmp/green3.log 2>&1; echo $?`

Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/subagent/SubAgentSessionMemoryContributor.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent/SubAgentSessionMemoryContributorTest.java
git commit -m "0 Add the subagent session memory contributor"
```

---

### Task 4: Wire the contributor into the three delegate families

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java`

**Interfaces:**
- Consumes: `SubAgentSessionMemoryContributor` (Task 3), `WorkspaceAdvisorContributor` and the 2-arg `wrap` (Task 1).
- Produces: no new public surface.

There are **24** `SubAgentGuardrailedChatClient.wrap(...)` call sites in this file. Each is inside one of three registration helpers and sits immediately beside the agent-type key its delegate uses — `registerCopilotSubAgentToolCallbacks`, `registerSubAgentToolCallbacks`, and `registerManagerSubAgentToolCallbacks`. The manager sites already carry the literal key as the `ProgressReportingToolCallback` label (for example `"personal_agent_manager"`); use that same key.

Introduce one private helper in this class so the 24 sites stay readable and no site can forget the memory contributor:

```java
    private static ChatClient wrapDelegate(
        ChatClient chatClient, String agentTypeKey, @Nullable AiGuardrails aiGuardrails,
        @Nullable AiGuardrailMetrics aiGuardrailMetrics, @Nullable WorkspaceSystemPrompts workspaceSystemPrompts,
        @Nullable AiHubSessionMemory aiHubSessionMemory) {

        List<SubAgentAdvisorContributor> contributors = new ArrayList<>();

        boolean guardrailsPresent = aiGuardrails != null && aiGuardrailMetrics != null;

        if (guardrailsPresent || workspaceSystemPrompts != null) {
            contributors.add(
                new WorkspaceAdvisorContributor(
                    guardrailsPresent ? aiGuardrails : null, guardrailsPresent ? aiGuardrailMetrics : null,
                    workspaceSystemPrompts));
        }

        if (aiHubSessionMemory != null) {
            contributors.add(new SubAgentSessionMemoryContributor(aiHubSessionMemory, agentTypeKey));
        }

        return SubAgentGuardrailedChatClient.wrap(chatClient, contributors);
    }
```

- [ ] **Step 1: Thread `AiHubSessionMemory` into the three registration helpers**

Each helper already takes `aiGuardrails`, `aiGuardrailMetrics`, and `workspaceSystemPrompts`. Add an `AiHubSessionMemory aiHubSessionMemory` parameter to each of the three, and pass the bean through from the two agent bean methods (`aiHubAskSpringAIAgent` and `aiHubBuildSpringAIAgent`), which already receive it.

- [ ] **Step 2: Replace all 24 call sites**

Replace each `SubAgentGuardrailedChatClient.wrap(someChatClient, aiGuardrails, aiGuardrailMetrics, workspaceSystemPrompts)` with `wrapDelegate(someChatClient, "<agent_type_key>", aiGuardrails, aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory)`.

Use the agent type key already present at each site. For manager delegates that is the `ProgressReportingToolCallback` label; for Copilot specialists and hub-owned subagents it is the key on the corresponding `CopilotAgentType` / `AiHubAgentType` value (for example `AiHubAgentType.RESEARCH.key()`). Prefer the enum's `key()` over a string literal wherever the enum is already imported at that site.

- [ ] **Step 3: Verify the MCP contributors were not touched**

Run:
```bash
grep -rn "wrapDelegate\|SubAgentSessionMemoryContributor" \
  server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubManagerMcpContributorConfiguration.java
```

Expected: **no output.** The MCP surface has no conversation id and must stay stateless — this is the spec's scope boundary.

- [ ] **Step 4: Compile and run the full module suite**

Run:
```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test > /tmp/green4.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/green4.log
```

Expected: exit 0, no FAILED tasks.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java
git commit -m "0 Give hub subagent delegates a per-conversation memory"
```

---

### Task 5: Purge specialist sessions on task delete

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/AiHubTaskServiceImpl.java` (`deleteSessionMessages`)
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/task/AiHubTaskServiceTest.java`

**Interfaces:**
- Consumes: `AgentTypeRegistry.keys()` (Task 2), `SubAgentSessionMemoryContributor.sessionKey(...)` (Task 3).

- [ ] **Step 1: Write the failing test**

Mirror the arrangement of the existing `testDeleteCascadesToChatMemoryAndRemovesRow` in the same class:

```java
    @Test
    void testDeletePurgesSpecialistSessionsAlongsideTheParentSession() {
        AiHubTask task = buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.delete(1L, WORKSPACE_ID, USER_ID);

        verify(sessionService).delete(THREAD_ID);
        verify(sessionService).delete(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, AiHubAgentType.PERSONAL_AGENT_MANAGER.key()));
        verify(taskRepository).delete(task);
    }
```

Two neighbouring tests stay green without edits, and that is worth checking rather than assuming.
`testDeleteCascadesToChatMemoryAndRemovesRow` asserts `verify(sessionService).delete(THREAD_ID)` — Mockito
verifies invocations matching that argument, so the additional per-agent-type deletes do not make it fail.
`testDeleteThrowsNotFoundOnOwnershipMismatch` asserts `verify(sessionService, never()).delete(any())`, which
still holds because the purge runs only after the ownership check passes.

- [ ] **Step 2: Run and verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*AiHubTaskServiceTest*' > /tmp/red5.log 2>&1; echo $?`

Expected: non-zero — only the parent session is deleted today.

- [ ] **Step 3: Extend `deleteSessionMessages`**

```java
    private void deleteSessionMessages(String threadId) {
        AiHubSessionMemory sessionMemory = aiHubSessionMemoryProvider.getIfAvailable();

        if (sessionMemory == null) {
            return;
        }

        deleteSession(sessionMemory, threadId);

        // Specialist subagents keep their own per-conversation sessions keyed <threadId>:<agentType>. SessionRepository
        // has no prefix listing, so the keys are reconstructed from the registry. Registered types include panel agents
        // that never own a specialist session; deleting a key that was never created is a no-op, and the alternative --
        // a hand-maintained delegate list -- would rot silently.
        for (String agentTypeKey : AgentTypeRegistry.keys()) {
            deleteSession(sessionMemory, SubAgentSessionMemoryContributor.sessionKey(threadId, agentTypeKey));
        }
    }

    private void deleteSession(AiHubSessionMemory sessionMemory, String sessionId) {
        try {
            sessionMemory.sessionService()
                .delete(sessionId);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to delete session messages for sessionId {}; leaving orphan messages for background cleanup",
                sessionId, exception);
        }
    }
```

Each delete is individually guarded so one failure does not abandon the rest — the pre-existing best-effort contract, applied per session rather than to the whole sweep.

- [ ] **Step 4: Run and verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*AiHubTaskServiceTest*' > /tmp/green5.log 2>&1; echo $?`

Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/AiHubTaskServiceImpl.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/task/AiHubTaskServiceTest.java
git commit -m "0 Purge subagent sessions when an AI Hub task is deleted"
```

---

### Task 6: Isolation and continuity integration test

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent/SubAgentSessionMemoryIntTest.java`

**Interfaces:**
- Consumes: everything above.

Uses the in-memory session repository (`InMemorySessionRepository`) rather than Testcontainers — the behavior under test is key isolation and replay, which is backend-independent.

- [ ] **Step 1: Write the tests**

Both cases write a turn straight through the `SessionService` and then assert what a *subsequent* delegation would load, using the same `EventFilter` the contributor builds. This tests the key isolation and the read window without standing up a real `ChatModel` — the LLM is not the behavior under test.

```java
package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubAgentSessionMemoryIntTest {

    private static final String THREAD_ID = "thread-1";
    private static final String PERSONAL_AGENT_MANAGER = "personal_agent_manager";
    private static final String MCP_MANAGER = "mcp_manager";

    private AiHubSessionMemory aiHubSessionMemory;

    @BeforeEach
    void setUp() {
        aiHubSessionMemory = new AiHubSessionMemory(
            InMemorySessionRepository.builder()
                .build(),
            null);
    }

    @Test
    void testSecondDelegationSeesTheFirstExchange() {
        String sessionId = SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, PERSONAL_AGENT_MANAGER);

        appendExchange(sessionId, "draft instructions for a PR reviewer", "Draft: reviews pull requests.");

        List<SessionEvent> events = aiHubSessionMemory.sessionService()
            .getEvents(sessionId, EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        assertThat(events).hasSize(2);
        assertThat(events.get(1).getMessage().getText()).contains("reviews pull requests");
    }

    @Test
    void testDifferentSpecialistsOnOneThreadDoNotSeeEachOther() {
        appendExchange(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, PERSONAL_AGENT_MANAGER), "create an agent",
            "Created agent 7.");

        List<SessionEvent> otherSpecialistEvents = aiHubSessionMemory.sessionService()
            .getEvents(
                SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, MCP_MANAGER),
                EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        assertThat(otherSpecialistEvents).isEmpty();
    }

    @Test
    void testReplayIsCappedAtMaxEvents() {
        String sessionId = SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, PERSONAL_AGENT_MANAGER);

        for (int exchangeIndex = 0; exchangeIndex < SubAgentSessionMemoryContributor.MAX_EVENTS; exchangeIndex++) {
            appendExchange(sessionId, "request " + exchangeIndex, "reply " + exchangeIndex);
        }

        List<SessionEvent> events = aiHubSessionMemory.sessionService()
            .getEvents(sessionId, EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        // The window caps what is replayed; every event written stays in the store (see the spec — the read filter
        // is deliberately not compaction).
        assertThat(events).hasSize(SubAgentSessionMemoryContributor.MAX_EVENTS);

        List<SessionEvent> allEvents = aiHubSessionMemory.sessionService()
            .getEvents(sessionId, EventFilter.all());

        assertThat(allEvents).hasSize(SubAgentSessionMemoryContributor.MAX_EVENTS * 2);
    }

    private void appendExchange(String sessionId, String userText, String assistantText) {
        if (aiHubSessionMemory.sessionRepository()
            .findById(sessionId) == null) {

            aiHubSessionMemory.sessionRepository()
                .save(
                    Session.builder()
                        .id(sessionId)
                        .userId(AiHubSessionMemory.SESSION_USER_ID)
                        .build());
        }

        aiHubSessionMemory.sessionRepository()
            .appendEvent(
                SessionEvent.builder()
                    .sessionId(sessionId)
                    .message(new UserMessage(userText))
                    .build());

        aiHubSessionMemory.sessionRepository()
            .appendEvent(
                SessionEvent.builder()
                    .sessionId(sessionId)
                    .message(new AssistantMessage(assistantText))
                    .build());
    }
}
```

If `Session.builder()` requires a `createdAt`, copy the exact builder call from
`WebhookBridgeAgent.persistTurnToChatMemory`, which already constructs sessions this way.

- [ ] **Step 2: Run and verify they pass**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*SubAgentSessionMemoryIntTest*' > /tmp/green6.log 2>&1; echo $?`

Expected: exit 0.

- [ ] **Step 3: Full verification**

Run:
```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:check --continue > /tmp/final.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/final.log
```

Expected: exit 0, no FAILED tasks.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent/SubAgentSessionMemoryIntTest.java
git commit -m "0 Pin subagent memory continuity and cross-specialist isolation"
```

---

## Out of scope

- `truncateMessagesFrom` does not rewind specialist memory. After an edit-and-resend a specialist may still recall a turn the user removed from the visible thread. Deliberate; see the spec.
- Suspend/resume for delegates — a specialist still cannot pause mid-run to ask the user. Separate spec.
- The MCP manager surface stays stateless.
