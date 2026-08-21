# Resumable AI Agent Tool Calls Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make tools that call `context.suspend()` actually suspend the AI agent's workflow task and, on resume, re-enter the LLM tool-calling loop with the human's answer injected as the pending tool call's result — for both sync and streaming agents.

**Architecture:** A per-invocation `ToolCallingManager` decorator (`SuspendableToolCallingManager`) wraps the default manager. After each tool batch it checks the agent `ActionContext` for a suspend; if present it serializes the conversation into the suspend's `continueParameters` and returns `ToolExecutionResult.returnDirect(true)` to halt Spring AI's `ToolCallAdvisor` loop. A real `resumePerform` deserializes the conversation, patches the suspended tool's sentinel result with the human's answer, and re-runs the chat client so the loop continues.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI 2.0.0-M6 (`ToolCallingManager`, `ToolCallAdvisor`, `FunctionToolCallback`), JUnit 5, Mockito.

**Spec:** `docs/superpowers/specs/2026-05-20-resumable-agent-tool-calls-design.md` · **Bug:** #5056

> **Spring AI 2.0.0-M6 / Jackson API notes** (discovered during Task 2 — apply throughout):
> - `AssistantMessage` and `ToolResponseMessage` multi-arg constructors are `protected`. Construct them with the builders: `AssistantMessage.builder().content(text).toolCalls(toolCalls).build()` and `ToolResponseMessage.builder().responses(responses).build()`. The `AssistantMessage.ToolCall` and `ToolResponseMessage.ToolResponse` records are public — construct those with `new`. Code blocks below that show `new AssistantMessage(...)` / `new ToolResponseMessage(...)` must use the builder form instead.
> - The project is on Jackson 3.x — use `tools.jackson.databind.ObjectMapper`, not `com.fasterxml.jackson.databind.ObjectMapper`. Jackson 3.x deserializes records via the canonical constructor with no extra annotations.

---

## File structure

| File | Responsibility |
|---|---|
| `platform-ai-api/.../constant/ToolSuspendConstants.java` | **New.** Sentinel string + `continueParameters` keys, shared by agent + tools. |
| `ai/agent/.../tool/ConversationState.java` | **New.** Serializable representation of the `List<Message>` conversation. |
| `ai/agent/.../tool/SuspendableToolCallingManager.java` | **New.** `ToolCallingManager` decorator: detect suspend, capture conversation, halt loop. |
| `ai/agent/.../action/AbstractAiAgentChatAction.java` | **Modify.** Wire the decorator; add explicit-messages overload; shared resume logic. |
| `ai/agent/.../action/AiAgentChatAction.java` | **Modify.** Real `resumePerform`. |
| `ai/agent/.../action/AiAgentStreamChatAction.java` | **Modify.** Real `resumePerform`. |
| `ai/agent/.../facade/AiAgentToolFacade.java` | **Modify.** Build callbacks as `BiFunction` so tools receive `ToolContext`. |
| `platform-component-api/.../service/ClusterElementDefinitionService.java` (+`...service/ClusterElementDefinitionServiceImpl.java`) | **Modify.** New `executeTool` overload that supplies the agent `ActionContext`. |
| `platform-component-api/.../definition/SuspendAwareSseEmitterHandler.java` | **New.** `SseEmitterHandler` wrapper carrying `ActionContextAware`. |
| `component-api/.../definition/ActionDefinition.java` (+ `ComponentDsl`) | **Modify (Task 5a).** Add `BaseResumePerformFunction` marker; `.resumePerform` accepts it. |
| `platform-component-api/.../definition/MultipleConnectionsResumePerformFunction.java` | **New (Task 5a).** Multi-connection resume function (full connections map + `extensions`). |
| `platform-component-service/.../service/ActionDefinitionServiceImpl.java` | **Modify.** Dispatch multi-connection resume (Task 5a); streaming branch wraps the handler (Task 11). |
| `platform-worker/.../task/SseStreamTaskExecutionPostOutputProcessor.java` | **Modify.** Post-stream suspend check. |
| `platform-job-sync/.../executor/SseStreamTaskExecutionPostOutputProcessor.java` | **Modify.** Post-stream suspend check. |
| `platform-webhook-rest-impl/.../web/rest/JobResumeController.java` | **Modify.** Return `text/event-stream` for streaming-agent resumes. |
| `approval/.../cluster/tool/ApprovalRequestApprovalTool.java` | **Modify.** Suspend the agent context; return sentinel. |
| `ai/agent/utils/.../cluster/AiAgentUtilsAskUserQuestionTool.java` | **Modify.** Return sentinel instead of placeholder answers. |

Phases are sequential: **Phase 1** (core sync loop, Tasks 1–6 — Task 5 is split into platform task **5a** and agent task **5b**) → **Phase 2** (tool-context access, Task 7) → **Phase 3** (tool migrations, Tasks 8–9) → **Phase 4** (streaming, Tasks 10–14).

> **Execution status (2026-05-21): COMPLETE.** All tasks implemented and committed on `0_732`; the combined `check` (compile + tests + checkstyle + PMD + SpotBugs) across all 11 touched modules is green. Commits: `389a5e3e167` (1), `f91325692e0` (2), `04455ef70ae` (3), `43b2ccb965d` (4), `d9616c53e18` (5a), `db1583a17df` (5b), `069b81121bd` (6), `d116099dd9d` (7), `6f70e002a22` (8), `55e98a5b5ec` (9), `819272a270d` (10), `2bdaeeb9365` (11), `81fe79b0f97` (12), `2e7cf06a024` (13), `b68c8aea4c8` (13b), `b5da6f08177` (14). Sync + streaming suspend/resume are covered by `AiAgentChatActionResumeIntTest` and `AiAgentStreamChatActionResumeIntTest`. Remaining: the manual smoke test (Final verification, Step 3) against a running instance.

Path prefix abbreviation used below: `AGENT = server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent`.

---

## Phase 1 — Core sync loop

### Task 1: `ToolSuspendConstants`

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-api/src/main/java/com/bytechef/platform/ai/constant/ToolSuspendConstants.java`

- [ ] **Step 1: Create the constants class**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.constant;

/**
 * Shared constants for the agent tool-suspend protocol: the sentinel a suspending tool returns as its result, and the
 * keys under which the resumable-loop state is stored in {@code Suspend.continueParameters}.
 *
 * @author Ivica Cardic
 */
public final class ToolSuspendConstants {

    /**
     * Returned by a tool as its result when it suspends. The {@code SuspendableToolCallingManager} locates the matching
     * {@code ToolResponse} by this exact value to identify the pending tool call.
     */
    public static final String SUSPENDED_SENTINEL = "__bytechef_tool_suspended__";

    /** {@code continueParameters} key holding the serialized {@code ConversationState}. */
    public static final String CONVERSATION_STATE = "__bytechef_conversation_state__";

    /** {@code continueParameters} key holding the pending tool call id (a {@code String}). */
    public static final String PENDING_TOOL_CALL_ID = "__bytechef_pending_tool_call_id__";

    private ToolSuspendConstants() {
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :server:libs:platform:platform-ai:platform-ai-api:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-ai/platform-ai-api/src/main/java/com/bytechef/platform/ai/constant/ToolSuspendConstants.java
git commit -m "732 Add ToolSuspendConstants for agent tool-suspend protocol"
```

---

### Task 2: `ConversationState`

`ConversationState` is a serializable mirror of Spring AI's `List<Message>`. Only the four message kinds the agent loop produces are supported: `SystemMessage`, `UserMessage`, `AssistantMessage` (with tool calls), `ToolResponseMessage`.

**Files:**
- Create: `AGENT/tool/ConversationState.java`
- Test: `server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/tool/ConversationStateTest.java`

- [ ] **Step 1: Write the failing round-trip test**

```java
/* Apache 2.0 header */
package com.bytechef.component.ai.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

class ConversationStateTest {

    @Test
    void testRoundTripPreservesAllMessageKinds() {
        List<Message> original = List.of(
            new SystemMessage("you are an agent"),
            new UserMessage("please get approval"),
            new AssistantMessage(
                "", java.util.Map.of(),
                List.of(new AssistantMessage.ToolCall("call_1", "function", "requestApproval", "{}"))),
            new ToolResponseMessage(
                List.of(new ToolResponseMessage.ToolResponse("call_1", "requestApproval", "__bytechef_tool_suspended__"))));

        List<Message> restored = ConversationState.from(original)
            .toMessages();

        assertEquals(4, restored.size());
        assertInstanceOf(SystemMessage.class, restored.get(0));
        assertInstanceOf(UserMessage.class, restored.get(1));

        AssistantMessage assistantMessage = assertInstanceOf(AssistantMessage.class, restored.get(2));

        assertEquals("call_1", assistantMessage.getToolCalls()
            .get(0)
            .id());

        ToolResponseMessage toolResponseMessage = assertInstanceOf(ToolResponseMessage.class, restored.get(3));

        assertEquals(
            "__bytechef_tool_suspended__",
            toolResponseMessage.getResponses()
                .get(0)
                .responseData());
    }
}
```

- [ ] **Step 2: Run it — verify it fails to compile** (`ConversationState` does not exist)

Run: `./gradlew :server:libs:modules:components:ai:agent:compileTestJava`
Expected: FAIL — `cannot find symbol: class ConversationState`.

- [ ] **Step 3: Create `ConversationState`**

```java
/* Apache 2.0 header */
package com.bytechef.component.ai.agent.tool;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Serializable representation of an agent conversation (a Spring AI {@code List<Message>}). Stored in a suspended
 * agent's {@code continueParameters} so the tool-calling loop can be reconstructed on resume. Only the message kinds
 * the agent loop produces are supported.
 *
 * @author Ivica Cardic
 */
public record ConversationState(List<MessageEntry> messages) {

    public record MessageEntry(
        String kind, String text, List<ToolCallEntry> toolCalls, List<ToolResponseEntry> toolResponses) {
    }

    public record ToolCallEntry(String id, String type, String name, String arguments) {
    }

    public record ToolResponseEntry(String id, String name, String responseData) {
    }

    public static ConversationState from(List<Message> messages) {
        List<MessageEntry> entries = new ArrayList<>();

        for (Message message : messages) {
            entries.add(toEntry(message));
        }

        return new ConversationState(entries);
    }

    public List<Message> toMessages() {
        List<Message> result = new ArrayList<>();

        for (MessageEntry entry : messages) {
            result.add(toMessage(entry));
        }

        return result;
    }

    private static MessageEntry toEntry(Message message) {
        if (message instanceof SystemMessage systemMessage) {
            return new MessageEntry("system", systemMessage.getText(), List.of(), List.of());
        }

        if (message instanceof UserMessage userMessage) {
            return new MessageEntry("user", userMessage.getText(), List.of(), List.of());
        }

        if (message instanceof AssistantMessage assistantMessage) {
            List<ToolCallEntry> toolCalls = new ArrayList<>();

            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                toolCalls.add(
                    new ToolCallEntry(toolCall.id(), toolCall.type(), toolCall.name(), toolCall.arguments()));
            }

            return new MessageEntry("assistant", assistantMessage.getText(), toolCalls, List.of());
        }

        if (message instanceof ToolResponseMessage toolResponseMessage) {
            List<ToolResponseEntry> toolResponses = new ArrayList<>();

            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                toolResponses.add(
                    new ToolResponseEntry(toolResponse.id(), toolResponse.name(), toolResponse.responseData()));
            }

            return new MessageEntry("tool", "", List.of(), toolResponses);
        }

        throw new IllegalArgumentException(
            "Unsupported message type for conversation serialization: " + message.getClass());
    }

    private static Message toMessage(MessageEntry entry) {
        return switch (entry.kind()) {
            case "system" -> new SystemMessage(entry.text());
            case "user" -> new UserMessage(entry.text());
            case "assistant" -> {
                List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();

                for (ToolCallEntry toolCall : entry.toolCalls()) {
                    toolCalls.add(
                        new AssistantMessage.ToolCall(
                            toolCall.id(), toolCall.type(), toolCall.name(), toolCall.arguments()));
                }

                yield new AssistantMessage(entry.text(), java.util.Map.of(), toolCalls);
            }
            case "tool" -> {
                List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

                for (ToolResponseEntry toolResponse : entry.toolResponses()) {
                    toolResponses.add(
                        new ToolResponseMessage.ToolResponse(
                            toolResponse.id(), toolResponse.name(), toolResponse.responseData()));
                }

                yield new ToolResponseMessage(toolResponses);
            }
            default -> throw new IllegalArgumentException("Unknown message kind: " + entry.kind());
        };
    }
}
```

- [ ] **Step 4: Run the test — verify it passes**

Run: `./gradlew :server:libs:modules:components:ai:agent:test --tests "com.bytechef.component.ai.agent.tool.ConversationStateTest"`
Expected: `SUCCESS (1 test)`.

- [ ] **Step 5: Add a Jackson round-trip test** (proves it survives the `TaskState` persistence path)

Append to `ConversationStateTest`:

```java
    @Test
    void testSurvivesJacksonRoundTrip() throws Exception {
        ConversationState state = ConversationState.from(
            List.of(new UserMessage("hello"),
                new ToolResponseMessage(
                    List.of(new ToolResponseMessage.ToolResponse("call_1", "tool", "result")))));

        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String json = objectMapper.writeValueAsString(state);
        ConversationState restored = objectMapper.readValue(json, ConversationState.class);

        assertEquals(state, restored);
    }
```

- [ ] **Step 6: Run both tests — verify they pass**

Run: `./gradlew :server:libs:modules:components:ai:agent:test --tests "com.bytechef.component.ai.agent.tool.ConversationStateTest"`
Expected: `SUCCESS (2 tests)`.

- [ ] **Step 7: Commit**

```bash
git add AGENT/tool/ConversationState.java server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/tool/ConversationStateTest.java
git commit -m "732 Add ConversationState for agent conversation serialization"
```

---

### Task 3: `SuspendableToolCallingManager`

**Files:**
- Create: `AGENT/tool/SuspendableToolCallingManager.java`
- Test: `server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/tool/SuspendableToolCallingManagerTest.java`

- [ ] **Step 1: Write the failing test**

```java
/* Apache 2.0 header */
package com.bytechef.component.ai.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;

class SuspendableToolCallingManagerTest {

    private final Prompt prompt = new Prompt(List.of());
    private final ChatResponse chatResponse = ChatResponse.builder()
        .generations(List.of())
        .build();

    @Test
    void testNoSuspendReturnsDelegateResultUnchanged() {
        ToolExecutionResult delegateResult = ToolExecutionResult.builder()
            .conversationHistory(List.of())
            .returnDirect(false)
            .build();
        ToolCallingManager delegate = mock(ToolCallingManager.class);

        when(delegate.executeToolCalls(prompt, chatResponse)).thenReturn(delegateResult);

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getSuspend()).thenReturn(null);

        ToolExecutionResult result = new SuspendableToolCallingManager(delegate, context)
            .executeToolCalls(prompt, chatResponse);

        assertEquals(delegateResult, result);
        assertFalse(result.returnDirect());
    }

    @Test
    void testSuspendCapturesConversationAndHaltsLoop() {
        List<Message> conversation = List.of(
            new ToolResponseMessage(
                List.of(
                    new ToolResponseMessage.ToolResponse("call_a", "otherTool", "real result"),
                    new ToolResponseMessage.ToolResponse(
                        "call_b", "requestApproval", ToolSuspendConstants.SUSPENDED_SENTINEL))));
        ToolCallingManager delegate = mock(ToolCallingManager.class);

        when(delegate.executeToolCalls(prompt, chatResponse)).thenReturn(
            ToolExecutionResult.builder()
                .conversationHistory(conversation)
                .returnDirect(false)
                .build());

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getSuspend()).thenReturn(
            new ActionContext.Suspend(Map.of("formUrl", "https://x"), Instant.now()));

        ToolExecutionResult result = new SuspendableToolCallingManager(delegate, context)
            .executeToolCalls(prompt, chatResponse);

        assertTrue(result.returnDirect());

        org.mockito.ArgumentCaptor<ActionContext.Suspend> captor =
            org.mockito.ArgumentCaptor.forClass(ActionContext.Suspend.class);

        org.mockito.Mockito.verify(context)
            .suspend(captor.capture());

        Map<String, ?> continueParameters = captor.getValue()
            .continueParameters();

        assertEquals("call_b", continueParameters.get(ToolSuspendConstants.PENDING_TOOL_CALL_ID));
        assertNotNull(continueParameters.get(ToolSuspendConstants.CONVERSATION_STATE));
        assertEquals("https://x", continueParameters.get("formUrl"));
    }

    @Test
    void testTwoSentinelsInOneBatchThrows() {
        List<Message> conversation = List.of(
            new ToolResponseMessage(
                List.of(
                    new ToolResponseMessage.ToolResponse("call_a", "t1", ToolSuspendConstants.SUSPENDED_SENTINEL),
                    new ToolResponseMessage.ToolResponse("call_b", "t2", ToolSuspendConstants.SUSPENDED_SENTINEL))));
        ToolCallingManager delegate = mock(ToolCallingManager.class);

        when(delegate.executeToolCalls(prompt, chatResponse)).thenReturn(
            ToolExecutionResult.builder()
                .conversationHistory(conversation)
                .returnDirect(false)
                .build());

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getSuspend()).thenReturn(
            new ActionContext.Suspend(Map.of(), Instant.now()));

        SuspendableToolCallingManager manager = new SuspendableToolCallingManager(delegate, context);

        assertThrows(IllegalStateException.class, () -> manager.executeToolCalls(prompt, chatResponse));
    }
}
```

- [ ] **Step 2: Run it — verify it fails to compile** (`SuspendableToolCallingManager` does not exist)

Run: `./gradlew :server:libs:modules:components:ai:agent:compileTestJava`
Expected: FAIL — `cannot find symbol: class SuspendableToolCallingManager`.

- [ ] **Step 3: Implement `SuspendableToolCallingManager`**

```java
/* Apache 2.0 header */
package com.bytechef.component.ai.agent.tool;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * A {@link ToolCallingManager} decorator that makes the agent's tool-calling loop resumable. After delegating tool
 * execution, it inspects the agent {@link ActionContext} for a suspend; if a tool suspended, it captures the
 * conversation into the suspend's {@code continueParameters} and returns {@code returnDirect=true} so Spring AI's
 * {@code ToolCallAdvisor} halts its loop at the suspend point.
 *
 * @author Ivica Cardic
 */
public final class SuspendableToolCallingManager implements ToolCallingManager {

    private final ToolCallingManager delegate;
    private final ActionContextAware actionContext;

    public SuspendableToolCallingManager(ToolCallingManager delegate, ActionContextAware actionContext) {
        this.delegate = delegate;
        this.actionContext = actionContext;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        ToolExecutionResult result = delegate.executeToolCalls(prompt, chatResponse);

        ActionContext.Suspend suspend = actionContext.getSuspend();

        if (suspend == null) {
            return result;
        }

        List<Message> conversation = result.conversationHistory();

        String pendingToolCallId = findSentinelToolResponseId(conversation);

        Map<String, Object> continueParameters = new HashMap<>(suspend.continueParameters());

        continueParameters.put(ToolSuspendConstants.CONVERSATION_STATE, ConversationState.from(conversation));
        continueParameters.put(ToolSuspendConstants.PENDING_TOOL_CALL_ID, pendingToolCallId);

        actionContext.suspend(new ActionContext.Suspend(continueParameters, suspend.expiresAt()));

        return ToolExecutionResult.builder()
            .conversationHistory(conversation)
            .returnDirect(true)
            .build();
    }

    private static String findSentinelToolResponseId(List<Message> conversation) {
        String pendingToolCallId = null;

        for (Message message : conversation) {
            if (!(message instanceof ToolResponseMessage toolResponseMessage)) {
                continue;
            }

            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                if (!ToolSuspendConstants.SUSPENDED_SENTINEL.equals(toolResponse.responseData())) {
                    continue;
                }

                if (pendingToolCallId != null) {
                    throw new IllegalStateException(
                        "More than one tool suspended in a single agent turn, which is not supported. " +
                            "Ensure the model calls at most one suspending tool per turn.");
                }

                pendingToolCallId = toolResponse.id();
            }
        }

        if (pendingToolCallId == null) {
            throw new IllegalStateException(
                "A tool set a suspend on the agent context but no tool response carried the suspend sentinel.");
        }

        return pendingToolCallId;
    }
}
```

- [ ] **Step 4: Run the tests — verify they pass**

Run: `./gradlew :server:libs:modules:components:ai:agent:test --tests "com.bytechef.component.ai.agent.tool.SuspendableToolCallingManagerTest"`
Expected: `SUCCESS (3 tests)`.

- [ ] **Step 5: Commit**

```bash
git add AGENT/tool/SuspendableToolCallingManager.java server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/tool/SuspendableToolCallingManagerTest.java
git commit -m "732 Add SuspendableToolCallingManager decorator"
```

---

### Task 4: Wire the decorator + explicit-messages overload into `AbstractAiAgentChatAction`

`getAdvisors` currently builds `ToolCallAdvisor.builder().toolCallingManager(toolCallingManager)` with the injected bean. It must wrap that bean in `SuspendableToolCallingManager` bound to the current `ActionContext`. `getChatClientRequestSpec` must also accept an explicit message list for resume.

**Files:**
- Modify: `AGENT/action/AbstractAiAgentChatAction.java`

- [ ] **Step 1: Pass the `ActionContext` into `getAdvisors`**

In `getChatClientRequestSpec`, the call `getAdvisors(clusterElementMap, connectionParameters, context)` already passes `context`. In `getAdvisors`, change the `ToolCallAdvisor` wiring (currently lines ~210–212):

```java
        // tool call

        ToolCallAdvisor.Builder<?> toolCallAdvisorBuilder = ToolCallAdvisor.builder()
            .toolCallingManager(new SuspendableToolCallingManager(toolCallingManager, (ActionContextAware) context))
            .advisorOrder(BaseAdvisor.HIGHEST_PRECEDENCE + 300);
```

Add the import: `import com.bytechef.component.ai.agent.tool.SuspendableToolCallingManager;` (`ActionContextAware` is already imported).

- [ ] **Step 2: Add an explicit-messages overload of `getChatClientRequestSpec`**

`getChatClientRequestSpec` currently ends with `.messages(ModelUtils.getMessages(inputParameters, context))`. Extract the message source into a parameter. Add an overload; keep the existing signature delegating to it:

```java
    protected ChatClient.ChatClientRequestSpec getChatClientRequestSpec(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        @Nullable ToolExecutionListener toolExecutionListener, ActionContext context) throws Exception {

        return getChatClientRequestSpec(
            inputParameters, connectionParameters, extensions, toolExecutionListener, context,
            ModelUtils.getMessages(inputParameters, context));
    }

    protected ChatClient.ChatClientRequestSpec getChatClientRequestSpec(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        @Nullable ToolExecutionListener toolExecutionListener, ActionContext context,
        List<org.springframework.ai.chat.messages.Message> messages) throws Exception {

        // ... existing body unchanged, except the final builder line uses `messages`:
        //     .messages(messages)
    }
```

(Move the existing body into the new overload; replace its `.messages(ModelUtils.getMessages(inputParameters, context))` with `.messages(messages)`.)

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :server:libs:modules:components:ai:agent:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add AGENT/action/AbstractAiAgentChatAction.java
git commit -m "732 Wire SuspendableToolCallingManager into the AI agent advisor chain"
```

---

### Task 5a: Add `MultipleConnectionsResumePerformFunction` to the component DSL

**Why this task exists:** the agent's `resumePerform` must rebuild the chat client, which needs the full `Map<String,ComponentConnection>` (model + tool + memory connections) **and** `extensions` (the cluster-element map) — exactly what `MultipleConnectionsPerformFunction` gives `perform`. Spring AI's `ActionDefinition.ResumePerformFunction` delivers only a *single* connection and no `extensions`. There is no multi-connection resume variant. This task adds one, mirroring the existing `BasePerformFunction` / `MultipleConnectionsPerformFunction` pattern.

This is design-judgment, multi-file work on core DSL — **use the most capable model**. The steps are investigation-led: replicate the perform-side pattern, do not invent a new shape. If `ActionDefinition` / `ComponentDsl` cannot accommodate the mirror, stop and report `BLOCKED` with the specific obstacle.

**Files:**
- Read first: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ActionDefinition.java` (the `ResumePerformFunction` interface and the `BasePerformFunction` marker), `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/MultipleConnectionsPerformFunction.java`, the `ComponentDsl` builder method that registers `resumePerform`, and `ActionDefinitionServiceImpl.executePerform` / `executeResumePerform` / `executeMultipleConnectionsPerform`.
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/MultipleConnectionsResumePerformFunction.java`
- Modify: `sdks/backend/java/component-api/.../ActionDefinition.java`, `ComponentDsl` (+ `ModifiableActionDefinition`), `server/libs/platform/platform-component/platform-component-service/.../ActionDefinitionServiceImpl.java`

- [ ] **Step 1: Study the perform-side multi-connection pattern.** Confirm: `MultipleConnectionsPerformFunction extends ActionDefinition.BasePerformFunction`; `ActionDefinition.getPerform()` returns `Optional<BasePerformFunction>`; `ActionDefinitionServiceImpl.executePerform` does `instanceof MultipleConnectionsPerformFunction` and passes the full `componentConnections` map + `extensions`. The resume side must mirror this exactly.

- [ ] **Step 2: Add a `BaseResumePerformFunction` marker if absent.** If `ResumePerformFunction` has no base-marker interface, add `ActionDefinition.BaseResumePerformFunction` and make `ResumePerformFunction extends BaseResumePerformFunction`; widen `ActionDefinition.getResumePerform()` (and the corresponding `ModifiableActionDefinition` field) to `Optional<BaseResumePerformFunction>`. Mirror exactly what `BasePerformFunction` / `getPerform()` do.

- [ ] **Step 3: Create `MultipleConnectionsResumePerformFunction`.** A `@FunctionalInterface` in `platform-component-api` extending `ActionDefinition.BaseResumePerformFunction`, mirroring `MultipleConnectionsPerformFunction`:

```java
/* Apache 2.0 header */
package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface MultipleConnectionsResumePerformFunction extends ActionDefinition.BaseResumePerformFunction {

    Object apply(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        Parameters continueParameters, Parameters data, ActionContext context) throws Exception;
}
```

(If `ResumePerformFunction` returns `ResumeResponse` rather than `Object`, match that return type instead — mirror the existing resume function's return type exactly.)

- [ ] **Step 4: DSL.** Make `ComponentDsl.ModifiableActionDefinition.resumePerform(...)` accept a `BaseResumePerformFunction` (widen the existing parameter, or add an overload) so `.resumePerform((MultipleConnectionsResumePerformFunction) this::resumePerform)` compiles. Mirror how `.perform(...)` accepts `BasePerformFunction`.

- [ ] **Step 5: Dispatch in `ActionDefinitionServiceImpl`.** In the resume branch of `executePerform` (the `else` branch where `continueParameters != null` and a resume function is present), branch on the function type: if it is a `MultipleConnectionsResumePerformFunction`, call a new `executeMultipleConnectionsResumePerform` that passes the full `componentConnections` map + `extensions` + `continueParameters` + `resumeData` + `context`; otherwise keep the existing single-connection `executeResumePerform` path unchanged. `executePerform` already has `componentConnections` and `extensions` in scope — thread them in.

- [ ] **Step 6: Verify.** Compile and check:

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava :server:libs:platform:platform-component:platform-component-service:compileJava :server:libs:modules:components:approval:check`
Expected: `BUILD SUCCESSFUL` — the `approval` component already uses `resumePerform`; its tests passing confirms the existing single-connection path is unregressed.

- [ ] **Step 7: Commit**

```bash
git add sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ActionDefinition.java server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/MultipleConnectionsResumePerformFunction.java server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ActionDefinitionServiceImpl.java
git commit -m "732 Add MultipleConnectionsResumePerformFunction to the component DSL"
```

Also stage the `ComponentDsl` file (locate its exact path — it lives under `sdks/backend/java/component-api/`).

---

### Task 5b: Real `resumePerform`

Shared resume logic in `AbstractAiAgentChatAction`; `AiAgentChatAction.resumePerform` delegates to it. Resume: deserialize the conversation, patch the sentinel `ToolResponse` for the pending id with the human's `data`, rebuild the request spec with those messages, re-run `.call()`. The agent's `resumePerform` is registered as the `MultipleConnectionsResumePerformFunction` from Task 5a.

**Files:**
- Modify: `AGENT/action/AbstractAiAgentChatAction.java`, `AGENT/action/AiAgentChatAction.java`
- Test: `server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/tool/ConversationResumeTest.java`

- [ ] **Step 1: Write the failing test for the conversation-patch helper**

```java
/* Apache 2.0 header */
package com.bytechef.component.ai.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

class ConversationResumeTest {

    @Test
    void testPatchReplacesOnlyTheSentinelResponse() {
        List<Message> conversation = List.of(
            new ToolResponseMessage(
                List.of(
                    new ToolResponseMessage.ToolResponse("call_a", "otherTool", "kept"),
                    new ToolResponseMessage.ToolResponse(
                        "call_b", "requestApproval",
                        com.bytechef.platform.ai.constant.ToolSuspendConstants.SUSPENDED_SENTINEL))));

        List<Message> patched = ConversationResume.patchPendingToolResponse(
            conversation, "call_b", "{\"approved\":true}");

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) patched.get(0);

        assertEquals("kept", toolResponseMessage.getResponses()
            .get(0)
            .responseData());
        assertEquals(
            "{\"approved\":true}",
            toolResponseMessage.getResponses()
                .get(1)
                .responseData());
    }
}
```

- [ ] **Step 2: Run it — verify it fails to compile** (`ConversationResume` does not exist)

Run: `./gradlew :server:libs:modules:components:ai:agent:compileTestJava`
Expected: FAIL — `cannot find symbol: class ConversationResume`.

- [ ] **Step 3: Create `ConversationResume`**

Create `AGENT/tool/ConversationResume.java`:

```java
/* Apache 2.0 header */
package com.bytechef.component.ai.agent.tool;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * Rebuilds a suspended agent conversation for resume: replaces the suspended tool's sentinel {@code ToolResponse} with
 * the human's real answer so the reconstructed conversation is protocol-correct.
 *
 * @author Ivica Cardic
 */
public final class ConversationResume {

    private ConversationResume() {
    }

    public static List<Message> patchPendingToolResponse(
        List<Message> conversation, String pendingToolCallId, String resumeData) {

        List<Message> result = new ArrayList<>();

        for (Message message : conversation) {
            if (!(message instanceof ToolResponseMessage toolResponseMessage)) {
                result.add(message);

                continue;
            }

            List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();

            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                if (toolResponse.id()
                    .equals(pendingToolCallId)) {

                    responses.add(
                        new ToolResponseMessage.ToolResponse(
                            toolResponse.id(), toolResponse.name(), resumeData));
                } else {
                    responses.add(toolResponse);
                }
            }

            result.add(new ToolResponseMessage(responses));
        }

        return result;
    }
}
```

- [ ] **Step 4: Run the test — verify it passes**

Run: `./gradlew :server:libs:modules:components:ai:agent:test --tests "com.bytechef.component.ai.agent.tool.ConversationResumeTest"`
Expected: `SUCCESS (1 test)`.

- [ ] **Step 5: Add the shared `resumeChat` method to `AbstractAiAgentChatAction`**

```java
    protected Object resumeChat(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        Parameters continueParameters, Parameters data, ActionContext context) throws Exception {

        ConversationState conversationState = continueParameters.get(
            com.bytechef.platform.ai.constant.ToolSuspendConstants.CONVERSATION_STATE, ConversationState.class);
        String pendingToolCallId = continueParameters.getRequiredString(
            com.bytechef.platform.ai.constant.ToolSuspendConstants.PENDING_TOOL_CALL_ID);

        List<org.springframework.ai.chat.messages.Message> conversation = ConversationResume.patchPendingToolResponse(
            conversationState.toMessages(), pendingToolCallId,
            com.bytechef.commons.util.JsonUtils.write(data.toMap()));

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = getChatClientRequestSpec(
            inputParameters, connectionParameters, extensions, null, context, conversation);

        chatClientRequestSpec.toolContext(Map.of(AiAgentToolContextKey.ACTION_CONTEXT, context));

        return ModelUtils.getChatActionResult(chatClientRequestSpec.call(), inputParameters, context)
            .response();
    }
```

Add imports: `com.bytechef.component.ai.agent.tool.ConversationResume`, `com.bytechef.component.ai.agent.tool.ConversationState`, `com.bytechef.platform.ai.constant.AiAgentToolContextKey`.

- [ ] **Step 6: Make `AiAgentChatAction.resumePerform` delegate to it**

Replace the stub body of `AiAgentChatAction.resumePerform` (currently `return ResumeResponse.of(new HashMap<>(data.toMap()));`):

```java
    protected ResumeResponse resumePerform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        Parameters continueParameters, Parameters data, ActionContext context) throws Exception {

        Object response = resumeChat(inputParameters, connectionParameters, extensions, continueParameters, data,
            context);

        return ResumeResponse.of(new HashMap<>(Map.of("response", response)));
    }
```

Register `resumePerform` as the `MultipleConnectionsResumePerformFunction` added in Task 5a — cast at the registration site, e.g. `.resumePerform((MultipleConnectionsResumePerformFunction) this::resumePerform)`. After Task 5a the DSL accepts it and `ActionDefinitionServiceImpl` dispatches it with the full `componentConnections` map + `extensions`. Match `resumePerform`'s return type to whatever Task 5a's `MultipleConnectionsResumePerformFunction.apply` declares.

- [ ] **Step 7: Verify it compiles**

Run: `./gradlew :server:libs:modules:components:ai:agent:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add AGENT/tool/ConversationResume.java AGENT/action/AbstractAiAgentChatAction.java AGENT/action/AiAgentChatAction.java server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/tool/ConversationResumeTest.java
git commit -m "732 Add resumable conversation re-entry for the AI agent"
```

---

### Task 6: Sync end-to-end integration test

Verify a suspending tool suspends `AiAgentChatAction` and resume continues the loop, using a stub `ChatModel` and a stub `ToolCallbackProviderFunction` tool that suspends (Phase 2 is not done yet, so a provider-function tool — which already gets the `ToolContext` — is used here).

**Files:**
- Test: `server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/action/AiAgentChatActionResumeIntTest.java`

- [ ] **Step 1: Write the integration test**

Model it on the existing `AbstractAiAgentChatActionTest` / `AiAgentStreamChatActionTest` (read those first for the established stub-`ChatModel` pattern in this module). The test must:
1. Build a stub `ChatModel` whose first response is a tool call to a stub suspending tool, and whose second response (after the tool result) is final text.
2. Register a stub `ToolCallbackProviderFunction` tool whose callback reads `ACTION_CONTEXT` from the `ToolContext`, calls `actionContext.suspend(new Suspend(Map.of(), Instant.now().plusSeconds(60)))`, and returns `ToolSuspendConstants.SUSPENDED_SENTINEL`.
3. Run `perform` → assert the result is an `ActionContext.Suspend` whose `continueParameters` contains `CONVERSATION_STATE` and `PENDING_TOOL_CALL_ID`.
4. Call `resumePerform` with `data = {"approved": true}` → assert the stub `ChatModel` is re-invoked with a `Prompt` whose last `ToolResponseMessage` holds `{"approved":true}` for the pending id, and the final output is the second stub response.

Write the full test body following the module's existing test conventions. Do not stub `SuspendableToolCallingManager` — exercise the real one through the real advisor chain.

- [ ] **Step 2: Run it — verify it fails first** (before any fix it would not suspend; at this point in the plan it should pass — if it does not, fix the implementation, not the test)

Run: `./gradlew :server:libs:modules:components:ai:agent:test --tests "com.bytechef.component.ai.agent.action.AiAgentChatActionResumeIntTest"`
Expected: `SUCCESS`.

- [ ] **Step 3: Run the full module test + checks**

Run: `./gradlew :server:libs:modules:components:ai:agent:check`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/action/AiAgentChatActionResumeIntTest.java
git commit -m "732 Add AI agent sync suspend/resume integration test"
```

---

## Phase 2 — Tool-context access for `MultipleConnectionsToolFunction`

### Task 7: Give `MultipleConnectionsToolFunction` / `ToolFunction` tools the `ToolContext`

> **Depth note:** Step 2 below ("make the cluster-element `context` carry the agent `ActionContext`") is the genuinely hard part — it depends on the internals of `ContextFactory.createClusterElementContext` and `ClusterElementContextAware.toActionContext`, which Step 2 says to inspect. Treat Step 2 as investigation-led: read those classes first, mirror their existing context-creation path, and if the supplied agent `ActionContext` cannot be threaded cleanly, report `BLOCKED` with the obstacle rather than forcing it. Use the most capable model for this task.

`AiAgentToolFacade.getFunctionToolCallback(...)` builds `FunctionToolCallback` callbacks from a plain `Function`. Switch to the verified `FunctionToolCallback.builder(String, BiFunction<I, ToolContext, O>)` overload, read `AiAgentToolContextKey.ACTION_CONTEXT` from the `ToolContext`, and pass the agent `ActionContext` into a new `ClusterElementDefinitionService.executeTool(...)` overload.

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/service/ClusterElementDefinitionService.java`
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceImpl.java`
- Modify: `AGENT/facade/AiAgentToolFacade.java`

- [ ] **Step 1: Add the `executeTool` overload to the interface**

Add to `ClusterElementDefinitionService` an overload of `executeTool` whose final parameter is the agent context: `@Nullable ActionContext agentActionContext`. It mirrors the existing multi-connection `executeTool` plus that parameter.

- [ ] **Step 2: Implement it in `ClusterElementDefinitionServiceImpl`**

In `doExecuteTool`, the `ClusterElementContext` is created by `contextFactory.createClusterElementContext(...)`. The new overload must make the agent `ActionContext` reachable from the tool's `context`. Concretely: when `agentActionContext != null`, the `ClusterElementContext`'s `toActionContext(...)` must return the agent context rather than a fresh one. Inspect `ContextFactory.createClusterElementContext` and `ClusterElementContextAware.toActionContext`; add a creation path that carries the supplied agent `ActionContext`. Keep the existing overloads delegating with `agentActionContext = null`.

- [ ] **Step 3: Switch `AiAgentToolFacade` callbacks to `BiFunction`**

In `getMultipleConnectionsToolCallbackFunction` and `getFromAiToolCallbackFunction`, return a `BiFunction<Map<String,Object>, ToolContext, Object>`:

```java
    private BiFunction<Map<String, Object>, ToolContext, Object> getMultipleConnectionsToolCallbackFunction(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> parameters,
        Map<String, ?> extensions, Map<String, ComponentConnection> componentConnections, boolean editorEnvironment) {

        return (request, toolContext) -> {
            Map<String, Object> resolvedParameters = new HashMap<>();

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                resolvedParameters.put(entry.getKey(), resolveParameterValue(entry.getValue(), request));
            }

            ActionContext agentActionContext = toolContext == null
                ? null
                : (ActionContext) toolContext.getContext()
                    .get(AiAgentToolContextKey.ACTION_CONTEXT);

            return clusterElementDefinitionService.executeTool(
                componentName, componentVersion, clusterElementName, MapUtils.concat(request, resolvedParameters),
                extensions, componentConnections, editorEnvironment, agentActionContext);
        };
    }
```

Change `FunctionToolCallback.builder(...)` calls in both `getFunctionToolCallback` overloads to pass the `BiFunction` (the builder's `BiFunction` overload is verified to exist). Add imports: `java.util.function.BiFunction`, `org.springframework.ai.chat.model.ToolContext`, `com.bytechef.platform.ai.constant.AiAgentToolContextKey`, `com.bytechef.component.definition.ActionContext`.

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :server:libs:modules:components:ai:agent:compileJava :server:libs:platform:platform-component:platform-component-service:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run the agent + component-service module checks**

Run: `./gradlew :server:libs:modules:components:ai:agent:check :server:libs:platform:platform-component:platform-component-service:check`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/service/ClusterElementDefinitionService.java server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceImpl.java AGENT/facade/AiAgentToolFacade.java
git commit -m "732 Thread the agent ActionContext to tool callbacks via ToolContext"
```

---

## Phase 3 — Tool migrations

### Task 8: `ApprovalRequestApprovalTool` suspends the agent context

With Task 7 done, the `MultipleConnectionsToolFunction`'s `context` can yield the agent `ActionContext`. The tool must run the approval logic against it and return the sentinel.

**Files:**
- Modify: `server/libs/modules/components/approval/src/main/java/com/bytechef/component/approval/cluster/tool/ApprovalRequestApprovalTool.java`
- Verify: `server/libs/modules/components/approval/build.gradle.kts` depends on `platform-ai-api` (add it if not).

- [ ] **Step 1: Update the tool's `object()` body**

The `object()` lambda currently does `performFunction.apply(inputParameters, componentConnections, extensions, actionContext)` where `actionContext` came from `toActionContext(...)`. After Task 7, `toActionContext(...)` returns the agent's context, so `performFunction.apply(...)` suspends the right context. After the perform, return the sentinel:

```java
            .object(() -> (inputParameters, connectionParameters, extensions, componentConnections, context) -> {
                ClusterElementContextAware clusterElementContextAware = (ClusterElementContextAware) context;

                ActionContext actionContext = clusterElementContextAware.toActionContext(
                    APPROVAL, 1, "requestApproval", null);

                performFunction.apply(inputParameters, componentConnections, extensions, actionContext);

                return ToolSuspendConstants.SUSPENDED_SENTINEL;
            });
```

Add import `com.bytechef.platform.ai.constant.ToolSuspendConstants`. `ApprovalRequestApprovalAction.perform` already returns `null` after `context.suspend(...)`; the tool replaces that with the sentinel so the decorator can identify the pending call.

- [ ] **Step 2: Regenerate the component definition snapshot**

Delete `server/libs/modules/components/approval/src/test/resources/definition/approval_v1.json` and the matching `build/resources/test/definition/approval_v1.json`, then run the approval component test to regenerate it.

Run: `./gradlew :server:libs:modules:components:approval:test`
Expected: `BUILD SUCCESSFUL` (the `.json` is regenerated).

- [ ] **Step 3: Run the approval module checks**

Run: `./gradlew :server:libs:modules:components:approval:check`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add server/libs/modules/components/approval/src/main/java/com/bytechef/component/approval/cluster/tool/ApprovalRequestApprovalTool.java server/libs/modules/components/approval/src/test/resources/definition/approval_v1.json server/libs/modules/components/approval/build.gradle.kts
git commit -m "732 Make ApprovalRequestApprovalTool suspend the agent context"
```

---

### Task 9: Migrate `AiAgentUtilsAskUserQuestionTool` to the sentinel

`askUserQuestion`'s `questionHandler` currently returns a `Map` of placeholder empty-string answers. Under the new protocol it must return the sentinel string so the decorator can locate its `ToolResponse`.

**Files:**
- Modify: `server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsAskUserQuestionTool.java`
- Verify: the `ai/agent/utils` module depends on `platform-ai-api` (it imports `AiAgentToolContextKey` already, so it does).

- [ ] **Step 1: Replace the placeholder return with the sentinel**

In the `questionHandler` lambda, the trailing block builds `placeholderAnswers` and returns it. Replace:

```java
                actionContext.suspend(new Suspend(continueParameters, expiresAt));

                return com.bytechef.platform.ai.constant.ToolSuspendConstants.SUSPENDED_SENTINEL;
```

Delete the now-unused `placeholderAnswers` construction loop.

`AskUserQuestionTool` from `springaicommunity` expects the handler to return a value assignable to its declared output. If its handler signature requires a `Map`/typed answers rather than a `String`, instead keep the handler returning its expected type but have the wrapping `ToolContextAwareToolCallback.call(...)` return `ToolSuspendConstants.SUSPENDED_SENTINEL` whenever `actionContext.getSuspend()` is non-null after the delegate call. Choose whichever the `AskUserQuestionTool` API allows; verify by compiling.

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run the `ai/agent/utils` checks**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:check`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsAskUserQuestionTool.java
git commit -m "732 Migrate AskUserQuestion tool to the sentinel suspend protocol"
```

---

## Phase 4 — Streaming

### Task 10: `SuspendAwareSseEmitterHandler`

A wrapper that carries the `ActionContextAware` alongside the `SseEmitterHandler` so a post-output processor can check for a suspend after the stream completes.

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/SuspendAwareSseEmitterHandler.java`
- Test: `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/definition/SuspendAwareSseEmitterHandlerTest.java`

- [ ] **Step 1: Write the failing test**

```java
/* Apache 2.0 header */
package com.bytechef.platform.component.definition;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import org.junit.jupiter.api.Test;

class SuspendAwareSseEmitterHandlerTest {

    @Test
    void testDelegatesHandleAndExposesContext() {
        boolean[] handled = {false};
        SseEmitterHandler delegate = emitter -> handled[0] = true;
        ActionContextAware context = org.mockito.Mockito.mock(ActionContextAware.class);

        SuspendAwareSseEmitterHandler handler = new SuspendAwareSseEmitterHandler(delegate, context);

        handler.handle(null);

        assertSame(context, handler.getActionContext());
        org.junit.jupiter.api.Assertions.assertTrue(handled[0]);
    }
}
```

- [ ] **Step 2: Run it — verify it fails to compile**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:compileTestJava`
Expected: FAIL — `cannot find symbol: class SuspendAwareSseEmitterHandler`.

- [ ] **Step 3: Create the class**

```java
/* Apache 2.0 header */
package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;

/**
 * An {@link SseEmitterHandler} that also carries the {@link ActionContextAware} of the streaming action, so a
 * post-output processor can check {@code getSuspend()} after the stream has been fully consumed — the only point at
 * which a mid-stream tool suspend is observable.
 *
 * @author Ivica Cardic
 */
public final class SuspendAwareSseEmitterHandler implements SseEmitterHandler {

    private final SseEmitterHandler delegate;
    private final ActionContextAware actionContext;

    public SuspendAwareSseEmitterHandler(SseEmitterHandler delegate, ActionContextAware actionContext) {
        this.delegate = delegate;
        this.actionContext = actionContext;
    }

    public ActionContextAware getActionContext() {
        return actionContext;
    }

    @Override
    public void handle(SseEmitter sseEmitter) {
        delegate.handle(sseEmitter);
    }
}
```

- [ ] **Step 4: Run the test — verify it passes**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:test --tests "com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandlerTest"`
Expected: `SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/SuspendAwareSseEmitterHandler.java server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/definition/SuspendAwareSseEmitterHandlerTest.java
git commit -m "732 Add SuspendAwareSseEmitterHandler"
```

---

### Task 11: Streaming branch of `ActionDefinitionServiceImpl` wraps the handler

`executePerform` ends the streaming branch with `return checkSuspend(actionDefinition, actionContext, result)`. For a streaming result, `checkSuspend` runs too early. Instead, wrap the handler so the suspend is checked after the stream.

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ActionDefinitionServiceImpl.java`

- [ ] **Step 1: Wrap the streaming result**

In `executePerform`, after `result` is produced by `executeMultipleConnectionsStreamPerform` / `executeMultipleConnectionsSseStreamResponsePerform`, branch before the final `return`:

```java
            if (result instanceof ActionDefinition.SseEmitterHandler sseEmitterHandler
                && actionContext instanceof ActionContextAware actionContextAware) {

                return new SuspendAwareSseEmitterHandler(sseEmitterHandler, actionContextAware);
            }

            return checkSuspend(actionDefinition, actionContext, result);
```

(Non-streaming results still flow through `checkSuspend` unchanged.) Add import `com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler`.

- [ ] **Step 2: Expose the `checkSuspend` logic for reuse**

The SSE post-output processors (Task 12) need the same resume-URL/`JobResumeId` minting `checkSuspend` does. Extract the body of `checkSuspend` that converts a `Suspend` + `ActionContextAware` into the final `Suspend` (the `jobResumeId` enrichment) into a `public static` helper — e.g. `SuspendUtils.finalizeSuspend(ActionContextAware actionContextAware)` returning `@Nullable ActionContext.Suspend` — in a module both `platform-component-service` and the two post-output-processor modules can depend on. Place `SuspendUtils` in `platform-component-api` (`com.bytechef.platform.component.definition`). Have `checkSuspend` call it.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ActionDefinitionServiceImpl.java server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/SuspendUtils.java
git commit -m "732 Wrap streaming action results for deferred suspend detection"
```

---

### Task 12: Post-stream suspend check in both `SseStreamTaskExecutionPostOutputProcessor`s

Both processors call `sseEmitterHandler.handle(emitter)` then `awaitCompletion(...)`. After completion, if the handler is a `SuspendAwareSseEmitterHandler` and its context has a suspend, return the finalized `Suspend` instead of `null`.

**Files:**
- Modify: `server/libs/platform/platform-worker/src/main/java/com/bytechef/platform/worker/task/SseStreamTaskExecutionPostOutputProcessor.java`
- Modify: `server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/SseStreamTaskExecutionPostOutputProcessor.java`
- Modify: `server/libs/platform/platform-job-sync/.../executor/JobSyncExecutor.java` (post-output processor ordering)

- [ ] **Step 1: Job-sync processor — return `Suspend` after the stream**

In the job-sync `SseStreamTaskExecutionPostOutputProcessor.process(...)`, after `awaitCompletion(emitter, latch)` and before `return null`:

```java
        if (output instanceof SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler) {
            ActionContext.Suspend suspend = SuspendUtils.finalizeSuspend(
                suspendAwareSseEmitterHandler.getActionContext());

            if (suspend != null) {
                return suspend;
            }
        }

        return null;
```

Add imports for `SuspendAwareSseEmitterHandler`, `SuspendUtils`, `com.bytechef.component.definition.ActionContext`.

- [ ] **Step 2: Worker processor — same change**

Apply the identical post-`awaitCompletion` block to the `platform-worker` `SseStreamTaskExecutionPostOutputProcessor`.

- [ ] **Step 3: Ensure the `Suspend` is processed downstream**

The returned `Suspend` must be picked up by `SuspendTaskExecutionPostOutputProcessor`, which must run *after* the SSE processor in the chain. In `JobSyncExecutor`'s `TaskWorker` construction, the post-output processor list order is `CallableResponse, Suspend, WebhookResponse, SseStream, WebSocketStream`. Reorder so `SseStream` (and `WebSocketStream`) precede `Suspend`: `CallableResponse, SseStream, WebSocketStream, Suspend, WebhookResponse`. Apply the equivalent reordering wherever the `platform-worker` processors are registered (find the worker's `TaskWorker`/post-output-processor wiring and reorder identically).

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :server:libs:platform:platform-worker:compileJava :server:libs:platform:platform-job-sync:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run checks for both modules**

Run: `./gradlew :server:libs:platform:platform-worker:check :server:libs:platform:platform-job-sync:check`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add server/libs/platform/platform-worker/src/main/java/com/bytechef/platform/worker/task/SseStreamTaskExecutionPostOutputProcessor.java server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/SseStreamTaskExecutionPostOutputProcessor.java server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/JobSyncExecutor.java
git commit -m "732 Detect agent suspend after SSE stream completion"
```

---

### Task 13: `AiAgentStreamChatAction.resumePerform` + streaming integration test

> **Depth note:** this is the streaming analog of Task 5b and likewise requires the `MultipleConnectionsResumePerformFunction` from Task 5a (the streaming `resumePerform` also needs the full connections map + `extensions`). It also depends on Task 11/12's streaming-suspend machinery. Confirm Tasks 5a, 11, 12 are complete before starting. The shared `resumeChat` from Task 5b builds a `.call()` spec; here a `.stream()` variant is needed — factor `resumeChat` so the call/stream terminal step is the only difference.

**Files:**
- Modify: `AGENT/action/AiAgentStreamChatAction.java`
- Test: `server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/action/AiAgentStreamChatActionResumeIntTest.java`

- [ ] **Step 1: Implement `AiAgentStreamChatAction.resumePerform`**

Replace the stub. The resumed turn streams, so `resumePerform` must produce an `SseEmitterHandler`-bearing result, mirroring `perform`. Build the request spec via `getChatClientRequestSpec(..., conversation)` with the patched conversation (reuse the Task 5b `resumeChat` machinery but keep `.stream()` instead of `.call()`), set the tool context (`ACTION_CONTEXT` + `SSE_EMITTER_REFERENCE` + `SSE_BUFFERED_EVENTS`), and return `createSseHandler(...)` exactly as `perform` does. Factor the shared spec-building so `perform` and `resumePerform` do not duplicate it. The streamed continuation is delivered to the client by the resume endpoint extended in Task 14.

- [ ] **Step 2: Write the streaming integration test**

`AiAgentStreamChatActionResumeIntTest`, following the existing `AiAgentStreamChatActionTest` conventions: a suspending tool inside the streamed `Flux`. Assert:
1. `perform` returns a `SuspendAwareSseEmitterHandler`.
2. After the post-output processor consumes the stream, the suspend is observable (`SuspendUtils.finalizeSuspend(handler.getActionContext())` returns a non-null `Suspend`) — i.e. the suspend was *not* lost.
3. An SSE event carrying the resume URL was emitted before stream close.
4. `resumePerform` re-runs the stream and the continued turn reflects the injected answer.

- [ ] **Step 3: Run it — verify it passes**

Run: `./gradlew :server:libs:modules:components:ai:agent:test --tests "com.bytechef.component.ai.agent.action.AiAgentStreamChatActionResumeIntTest"`
Expected: `SUCCESS`.

- [ ] **Step 4: Full agent module check**

Run: `./gradlew :server:libs:modules:components:ai:agent:check`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add AGENT/action/AiAgentStreamChatAction.java server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/action/AiAgentStreamChatActionResumeIntTest.java
git commit -m "732 Add streaming AI agent suspend/resume support"
```

---

### Task 14: Stream the resumed turn over the existing resume endpoint

The continued turn of a resumed streaming agent is delivered over the **existing** `JobResumeController` `POST /job/resume/{id}` endpoint — no new endpoint. Today it returns `204 No Content`; for a streaming-agent resume it returns `text/event-stream`.

**Files:**
- Modify: `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/main/java/com/bytechef/platform/webhook/web/rest/JobResumeController.java`
- Possibly modify: `JobResumeFacade` (+ impl) — if a resume-and-stream entry point is needed beyond `resumeJob(id, data)`.
- Test: `server/libs/platform/platform-webhook/.../web/rest/JobResumeControllerIntTest.java` (follow the module's existing controller-test convention).

- [ ] **Step 1: Trace how a resumed streaming job's SSE events are routed**

Before coding, confirm which executor runs a resumed job and how its `SseStreamTaskExecutionPostOutputProcessor` emits events: the `platform-worker` processor sends via the message broker; the `platform-job-sync` processor forwards to registered `SseStreamBridge`s (`JobSyncExecutor.addSseStreamBridge`). Read `JobResumeFacadeImpl` and `TaskCoordinator.onResumeJobEvent` to determine the executor for a resumed job. Record the finding as a comment in the task branch commit message. The resume controller must register its event sink (an `SseStreamBridge`, or a message-broker subscription) against that executor's mechanism for the resumed job id.

- [ ] **Step 2: Write the failing integration test**

`JobResumeControllerIntTest`: POST to `/job/resume/{id}` for a suspended streaming-agent job with header `Accept: text/event-stream`. Assert the response content type is `text/event-stream` and the body carries the resumed turn's streamed events. Add a second test asserting a POST *without* that `Accept` header still returns `204`.

- [ ] **Step 3: Run it — verify it fails**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:test --tests "*JobResumeControllerIntTest"`
Expected: FAIL — the endpoint returns `204`, not an SSE stream.

- [ ] **Step 4: Add the SSE branch to `JobResumeController`**

Add an overload mapping that produces `text/event-stream`. When the request accepts SSE, return an `SseEmitter`: register an event sink for the job id (per Step 1's finding), trigger the resume, relay each event to the emitter, and `complete()` on stream end / `completeWithError` on failure. When the request does not accept SSE, keep the existing `ResponseEntity<Void>` path. Sketch:

```java
    @RequestMapping(
        method = { RequestMethod.GET, RequestMethod.POST }, value = "/job/resume/{id}",
        produces = "text/event-stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter resumeStream(
        @PathVariable String id, @RequestBody(required = false) Map<String, Object> data) {

        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
            new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(0L);

        // Register an SseStreamBridge for the resumed job id (per Step 1) that forwards
        // onEvent -> emitter.send(...), onComplete -> emitter.complete(),
        // onError  -> emitter.completeWithError(...), THEN trigger the resume.
        jobResumeFacade.resumeJobStreaming(id, data, bridge);

        return emitter;
    }
```

If `JobResumeFacade` has no streaming entry point, add `resumeJobStreaming(String id, Map<String,Object> data, SseStreamBridge bridge)` to the facade and implement it in `JobResumeFacadeImpl` — register the bridge with the executor, then resume. The non-streaming `resume(...)` method keeps `produces` unset so Spring routes by `Accept`.

- [ ] **Step 5: Run the test — verify it passes**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:test --tests "*JobResumeControllerIntTest"`
Expected: `SUCCESS`.

- [ ] **Step 6: Run the module checks**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:check`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/main/java/com/bytechef/platform/webhook/web/rest/JobResumeController.java server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/facade/JobResumeFacade.java server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-service/src/main/java/com/bytechef/platform/workflow/execution/facade/JobResumeFacadeImpl.java server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/test/java/com/bytechef/platform/webhook/web/rest/JobResumeControllerIntTest.java
git commit -m "732 Stream resumed streaming-agent turns over the resume endpoint"
```

---

## Final verification

- [ ] **Step 1: Format**

Run: `./gradlew spotlessApply`

- [ ] **Step 2: Full check across touched modules**

Run: `./gradlew :server:libs:modules:components:ai:agent:check :server:libs:modules:components:ai:agent:utils:check :server:libs:modules:components:approval:check :server:libs:platform:platform-component:platform-component-api:check :server:libs:platform:platform-component:platform-component-service:check :server:libs:platform:platform-worker:check :server:libs:platform:platform-job-sync:check :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:check`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Manual smoke test** — in a running instance, attach `requestApproval` as a tool to an AI agent, prompt the agent to use it, confirm the workflow task suspends; submit the approval form; confirm the agent resumes and the LLM produces a final answer that reflects the approval decision. Repeat with the streaming chat agent.

---

## Open items (carried from the spec)

- **Streaming resume delivery** — handled by Task 14, extending the existing `JobResumeController` to return SSE. Task 14 Step 1 traces which executor runs a resumed job and how its events route before coding; if a resumed streaming job runs under the message-broker worker rather than `JobSyncExecutor`, the controller's event sink is a broker subscription rather than an `SseStreamBridge`.
- **AG-UI invocation path** — confirm AI Hub / Copilot chat invoke `streamChat` as a standard `TaskExecution` so the Task 12 post-output processor fires; if AG-UI bypasses the post-output chain, the suspend hook must be added on that path too.
- **Spring AI version** — verified against `spring-ai` 2.0.0-M6. A version bump must re-verify `ToolCallAdvisor` honoring `ToolExecutionResult.returnDirect()` and the `FunctionToolCallback` `BiFunction` builder.
