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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.time.Instant;
import java.util.ArrayList;
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
    void testCheckpointerInvokedAfterNonSuspendRound() {
        List<Message> conversation = List.of(
            ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse("call_a", "someTool", "result")))
                .build());
        ToolCallingManager delegate = mock(ToolCallingManager.class);

        when(delegate.executeToolCalls(prompt, chatResponse)).thenReturn(
            ToolExecutionResult.builder()
                .conversationHistory(conversation)
                .returnDirect(false)
                .build());

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getSuspend()).thenReturn(null);

        List<List<Message>> checkpointedConversations = new ArrayList<>();

        new SuspendableToolCallingManager(delegate, context, checkpointedConversations::add)
            .executeToolCalls(prompt, chatResponse);

        assertEquals(List.of(conversation), checkpointedConversations);
    }

    @Test
    void testCheckpointerFailureDoesNotFailTheRound() {
        ToolExecutionResult delegateResult = ToolExecutionResult.builder()
            .conversationHistory(List.of())
            .returnDirect(false)
            .build();
        ToolCallingManager delegate = mock(ToolCallingManager.class);

        when(delegate.executeToolCalls(prompt, chatResponse)).thenReturn(delegateResult);

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getSuspend()).thenReturn(null);

        ToolExecutionResult result = new SuspendableToolCallingManager(
            delegate, context, conversation -> {
                throw new RuntimeException("storage down");
            }).executeToolCalls(prompt, chatResponse);

        assertEquals(delegateResult, result);
    }

    @Test
    void testCheckpointerNotInvokedOnSuspend() {
        List<Message> conversation = List.of(
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse(
                            "call_b", "requestApproval", ToolSuspendConstants.SUSPENDED_SENTINEL)))
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

        List<List<Message>> checkpointedConversations = new ArrayList<>();

        ToolExecutionResult result = new SuspendableToolCallingManager(
            delegate, context, checkpointedConversations::add).executeToolCalls(prompt, chatResponse);

        assertTrue(result.returnDirect());
        assertTrue(checkpointedConversations.isEmpty());
    }

    @Test
    void testSuspendCapturesConversationAndHaltsLoop() {
        List<Message> conversation = List.of(
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse("call_a", "otherTool", "real result"),
                        new ToolResponseMessage.ToolResponse(
                            "call_b", "requestApproval", ToolSuspendConstants.SUSPENDED_SENTINEL)))
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
    void testDelegateExceptionWithOrphanSuspendRethrowsCleanly() {
        // If the delegate throws after a suspending tool has already set a suspend on the context, we re-throw the
        // original exception unchanged. The orphaned suspend is not persisted because the exception unwinds before
        // checkSuspend runs (verified separately via the integration test). This test pins that the wrapper does NOT
        // accidentally swallow or replace the delegate exception.
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
    void testTwoSentinelsInOneBatchThrows() {
        List<Message> conversation = List.of(
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse("call_a", "t1", ToolSuspendConstants.SUSPENDED_SENTINEL),
                        new ToolResponseMessage.ToolResponse("call_b", "t2", ToolSuspendConstants.SUSPENDED_SENTINEL)))
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

        assertThrows(IllegalStateException.class, () -> manager.executeToolCalls(prompt, chatResponse));
    }
}
