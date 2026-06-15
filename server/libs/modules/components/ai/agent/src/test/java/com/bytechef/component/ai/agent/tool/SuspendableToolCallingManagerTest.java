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

package com.bytechef.component.ai.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;

class SuspendableToolCallingManagerTest {

    private static final String SUSPENDED_TOOL_RESULT = createSuspendedToolResult();

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
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse("call_a", "otherTool", "real result"),
                        new ToolResponseMessage.ToolResponse(
                            "call_b", "requestApproval", SUSPENDED_TOOL_RESULT)))
                .build());
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
        assertTrue(ToolExecutionResult.buildGenerations(result)
            .isEmpty());

        ArgumentCaptor<ActionContext.Suspend> captor = ArgumentCaptor.forClass(ActionContext.Suspend.class);

        verify(context).suspend(captor.capture());

        Map<String, ?> continueParameters = captor.getValue()
            .continueParameters();

        AgentToolSuspension agentToolSuspension =
            (AgentToolSuspension) continueParameters.get(AgentToolSuspension.CONTINUE_PARAMETER_KEY);

        assertNotNull(agentToolSuspension);
        assertEquals("call_b", agentToolSuspension.pendingToolCallId());
        assertEquals(ConversationState.from(conversation), agentToolSuspension.conversation());
        assertEquals("https://x", continueParameters.get("formUrl"));
        assertEquals(2, continueParameters.size());
    }

    @Test
    void testDelegateExceptionWithOrphanSuspendRethrowsCleanly() {
        ToolCallingManager delegate = mock(ToolCallingManager.class);

        RuntimeException delegateException = new RuntimeException("downstream tool blew up");

        when(delegate.executeToolCalls(prompt, chatResponse)).thenThrow(delegateException);

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getSuspend()).thenReturn(
            new ActionContext.Suspend(Map.of("formUrl", "https://x"), Instant.now()));

        SuspendableToolCallingManager manager = new SuspendableToolCallingManager(delegate, context);

        RuntimeException thrown = assertThrows(
            RuntimeException.class, () -> manager.executeToolCalls(prompt, chatResponse));

        assertEquals(delegateException, thrown, "Delegate's exception must propagate unchanged");
    }

    @Test
    void testDelegateExceptionWithoutSuspendRethrowsCleanly() {
        ToolCallingManager delegate = mock(ToolCallingManager.class);

        RuntimeException delegateException = new RuntimeException("downstream tool blew up");

        when(delegate.executeToolCalls(prompt, chatResponse)).thenThrow(delegateException);

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getSuspend()).thenReturn(null);

        SuspendableToolCallingManager manager = new SuspendableToolCallingManager(delegate, context);

        RuntimeException thrown = assertThrows(
            RuntimeException.class, () -> manager.executeToolCalls(prompt, chatResponse));

        assertEquals(delegateException, thrown);
    }

    @Test
    void testTwoSuspendedToolResultsInOneBatchThrows() {
        List<Message> conversation = List.of(
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse("call_a", "t1", SUSPENDED_TOOL_RESULT),
                        new ToolResponseMessage.ToolResponse("call_b", "t2", SUSPENDED_TOOL_RESULT)))
                .build());
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

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> manager.executeToolCalls(prompt, chatResponse));

        assertTrue(
            exception.getMessage()
                .contains("More than one suspended tool response found"));
    }

    @Test
    void testSuspendedToolResultWithoutSuspendThrows() {
        List<Message> conversation = List.of(
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse(
                            "call_a", "requestApproval", SUSPENDED_TOOL_RESULT)))
                .build());
        ToolCallingManager delegate = mock(ToolCallingManager.class);

        when(delegate.executeToolCalls(prompt, chatResponse)).thenReturn(
            ToolExecutionResult.builder()
                .conversationHistory(conversation)
                .returnDirect(false)
                .build());

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getSuspend()).thenReturn(null);

        SuspendableToolCallingManager manager = new SuspendableToolCallingManager(delegate, context);

        assertThrows(IllegalStateException.class, () -> manager.executeToolCalls(prompt, chatResponse));
    }

    @Test
    void testReservedContinueParameterKeyCollisionThrows() {
        List<Message> conversation = List.of(
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse("call_a", "requestApproval", SUSPENDED_TOOL_RESULT)))
                .build());
        ToolCallingManager delegate = mock(ToolCallingManager.class);

        when(delegate.executeToolCalls(prompt, chatResponse)).thenReturn(
            ToolExecutionResult.builder()
                .conversationHistory(conversation)
                .returnDirect(false)
                .build());

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getSuspend()).thenReturn(
            new ActionContext.Suspend(
                Map.of(AgentToolSuspension.CONTINUE_PARAMETER_KEY, "tool value"), Instant.now()));

        SuspendableToolCallingManager manager = new SuspendableToolCallingManager(delegate, context);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> manager.executeToolCalls(prompt, chatResponse));

        assertTrue(
            exception.getMessage()
                .contains(AgentToolSuspension.CONTINUE_PARAMETER_KEY));
        verify(context, never()).suspend(any());
    }

    @Test
    void testSuspendWithoutSuspendedToolResponseThrows() {
        List<Message> conversation = List.of(
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse("call_a", "requestApproval", "approval request failed")))
                .build());
        ToolCallingManager delegate = mock(ToolCallingManager.class);

        when(delegate.executeToolCalls(prompt, chatResponse)).thenReturn(
            ToolExecutionResult.builder()
                .conversationHistory(conversation)
                .returnDirect(false)
                .build());

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getSuspend()).thenReturn(new ActionContext.Suspend(Map.of(), Instant.now()));

        SuspendableToolCallingManager manager = new SuspendableToolCallingManager(delegate, context);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> manager.executeToolCalls(prompt, chatResponse));

        assertTrue(
            exception.getMessage()
                .contains("approval request failed"));
    }

    @Test
    void testToolCallsAfterTheSuspendingToolInTheSameRoundAreNotExecuted() {
        List<String> executedToolNames = new ArrayList<>();
        AtomicReference<ActionContext.Suspend> suspendReference = new AtomicReference<>();

        ActionContextAware context = mockSuspendableContext(suspendReference);
        ToolCallingManager delegate = mockSequentialDelegate(executedToolNames, suspendReference, "requestApproval");

        ToolExecutionResult result = new SuspendableToolCallingManager(delegate, context)
            .executeToolCalls(
                prompt, toolCallResponse(
                    toolCall("call_a", "lookUp"), toolCall("call_b", "requestApproval"),
                    toolCall("call_c", "deleteRecord")));

        assertEquals(List.of("lookUp", "requestApproval"), executedToolNames);
        assertTrue(result.returnDirect());

        ArgumentCaptor<ActionContext.Suspend> captor = ArgumentCaptor.forClass(ActionContext.Suspend.class);

        verify(context).suspend(captor.capture());

        AgentToolSuspension agentToolSuspension = (AgentToolSuspension) captor.getValue()
            .continueParameters()
            .get(AgentToolSuspension.CONTINUE_PARAMETER_KEY);

        assertNotNull(agentToolSuspension);
        assertEquals("call_b", agentToolSuspension.pendingToolCallId());

        List<Message> conversation = agentToolSuspension.conversation()
            .toMessages();

        assertEquals(
            List.of("call_a", "call_b", "call_c"),
            ((AssistantMessage) conversation.get(conversation.size() - 2)).getToolCalls()
                .stream()
                .map(AssistantMessage.ToolCall::id)
                .toList());

        List<ToolResponseMessage.ToolResponse> toolResponses =
            ((ToolResponseMessage) conversation.getLast()).getResponses();

        assertEquals(List.of("call_a", "call_b", "call_c"), toolResponses.stream()
            .map(ToolResponseMessage.ToolResponse::id)
            .toList());
        assertEquals("lookUp result", toolResponses.get(0)
            .responseData());
        assertEquals(SuspendableToolCallingManager.NOT_EXECUTED_TOOL_RESULT, toolResponses.get(2)
            .responseData());
    }

    @Test
    void testSecondSuspendingToolInTheSameRoundIsNotExecuted() {
        List<String> executedToolNames = new ArrayList<>();
        AtomicReference<ActionContext.Suspend> suspendReference = new AtomicReference<>();

        ActionContextAware context = mockSuspendableContext(suspendReference);
        ToolCallingManager delegate = mockSequentialDelegate(
            executedToolNames, suspendReference, "askUserQuestion", "requestApproval");

        ToolExecutionResult result = new SuspendableToolCallingManager(delegate, context)
            .executeToolCalls(
                prompt, toolCallResponse(toolCall("call_a", "askUserQuestion"), toolCall("call_b", "requestApproval")));

        assertEquals(List.of("askUserQuestion"), executedToolNames);
        assertTrue(result.returnDirect());

        ArgumentCaptor<ActionContext.Suspend> captor = ArgumentCaptor.forClass(ActionContext.Suspend.class);

        verify(context).suspend(captor.capture());

        AgentToolSuspension agentToolSuspension = (AgentToolSuspension) captor.getValue()
            .continueParameters()
            .get(AgentToolSuspension.CONTINUE_PARAMETER_KEY);

        assertNotNull(agentToolSuspension);
        assertEquals("call_a", agentToolSuspension.pendingToolCallId());
    }

    @Test
    void testAllToolCallsOfARoundWithoutSuspendAreExecuted() {
        List<String> executedToolNames = new ArrayList<>();
        AtomicReference<ActionContext.Suspend> suspendReference = new AtomicReference<>();

        ActionContextAware context = mockSuspendableContext(suspendReference);
        ToolCallingManager delegate = mockSequentialDelegate(executedToolNames, suspendReference);

        ToolExecutionResult result = new SuspendableToolCallingManager(delegate, context)
            .executeToolCalls(prompt, toolCallResponse(toolCall("call_a", "lookUp"), toolCall("call_b", "search")));

        assertEquals(List.of("lookUp", "search"), executedToolNames);
        assertFalse(result.returnDirect());

        List<Message> conversation = result.conversationHistory();

        assertEquals(2, ((AssistantMessage) conversation.get(conversation.size() - 2)).getToolCalls()
            .size());
        assertEquals(List.of("lookUp result", "search result"), ((ToolResponseMessage) conversation.getLast())
            .getResponses()
            .stream()
            .map(ToolResponseMessage.ToolResponse::responseData)
            .toList());

        verify(context, never()).suspend(any());
    }

    private static ActionContextAware mockSuspendableContext(
        AtomicReference<ActionContext.Suspend> suspendReference) {

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getSuspend()).thenAnswer(invocation -> suspendReference.get());

        return context;
    }

    private static ToolCallingManager mockSequentialDelegate(
        List<String> executedToolNames, AtomicReference<ActionContext.Suspend> suspendReference,
        String... suspendingToolNames) {

        ToolCallingManager delegate = mock(ToolCallingManager.class);

        when(delegate.executeToolCalls(any(), any())).thenAnswer(invocation -> {
            Prompt toolPrompt = invocation.getArgument(0);
            ChatResponse toolChatResponse = invocation.getArgument(1);

            Generation generation = Objects.requireNonNull(toolChatResponse.getResult());

            AssistantMessage assistantMessage = generation.getOutput();

            AssistantMessage.ToolCall toolCall = assistantMessage.getToolCalls()
                .getFirst();

            executedToolNames.add(toolCall.name());

            String responseData = toolCall.name() + " result";

            if (List.of(suspendingToolNames)
                .contains(toolCall.name())) {

                suspendReference.set(new ActionContext.Suspend(Map.of(), Instant.now()));

                responseData = SUSPENDED_TOOL_RESULT;
            }

            List<Message> conversation = new ArrayList<>(toolPrompt.getInstructions());

            conversation.add(assistantMessage);
            conversation.add(
                ToolResponseMessage.builder()
                    .responses(
                        List.of(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), responseData)))
                    .build());

            return ToolExecutionResult.builder()
                .conversationHistory(conversation)
                .returnDirect(false)
                .build();
        });

        return delegate;
    }

    private static AssistantMessage.ToolCall toolCall(String id, String name) {
        return new AssistantMessage.ToolCall(id, "function", name, "{}");
    }

    private static ChatResponse toolCallResponse(AssistantMessage.ToolCall... toolCalls) {
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(toolCalls))
            .build();

        return ChatResponse.builder()
            .generations(List.of(new Generation(assistantMessage)))
            .build();
    }

    private static String createSuspendedToolResult() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new ActionContext.Suspend(Map.of(), Instant.now()));

        return ToolSuspension.suspendedToolResult(actionContext);
    }
}
