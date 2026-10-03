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

package com.bytechef.component.ai.agent.utils.cluster;

import static com.bytechef.platform.ai.constant.AiAgentSseEventType.ASK_USER_QUESTION;
import static com.bytechef.platform.ai.constant.AiAgentSseEventType.EVENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.tool.AiAgentToolContext;
import com.bytechef.platform.ai.tool.AiAgentToolContext.SseTransport;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class AiAgentUtilsAskUserQuestionToolTest {

    private static final String RESUME_URL = "https://example.com/api/job/resume/abc";

    private static final String TOOL_INPUT = """
        {
          "questions": [
            {
              "question": "Which library should we use?",
              "header": "Library",
              "options": [
                {"label": "Alpha", "description": "Use alpha"},
                {"label": "Beta", "description": "Use beta"}
              ],
              "multiSelect": false
            }
          ]
        }
        """;

    private static final List<Map<String, Object>> EXPECTED_QUESTIONS = List.of(
        Map.of(
            "header", "Library",
            "multiSelect", false,
            "options", List.of(
                Map.of("description", "Use alpha", "label", "Alpha"),
                Map.of("description", "Use beta", "label", "Beta")),
            "question", "Which library should we use?"));

    private ActionContext actionContext;
    private final AtomicReference<Suspend> suspendReference = new AtomicReference<>();
    private ToolCallback toolCallback;

    @BeforeEach
    void setUp() throws Exception {
        actionContext = mock(ActionContext.class, withSettings().extraInterfaces(ActionContextAware.class));

        doAnswer(invocation -> {
            suspendReference.set(invocation.getArgument(0));

            return null;
        }).when(actionContext)
            .suspend(any(Suspend.class));

        when(((ActionContextAware) actionContext).getSuspend()).thenAnswer(invocation -> suspendReference.get());

        ToolCallbackProvider toolCallbackProvider = AiAgentUtilsAskUserQuestionTool.CLUSTER_ELEMENT_DEFINITION
            .getElement()
            .apply(mock(Parameters.class), mock(Parameters.class), mock(Context.class));

        toolCallback = toolCallbackProvider.getToolCallbacks()[0];
    }

    @Test
    void testCallWithoutToolContextReturnsGuidance() {
        String result = toolCallback.call(TOOL_INPUT);

        assertEquals(AiAgentUtilsAskUserQuestionTool.NO_EVENT_TRANSPORT_RESULT, result);
    }

    @Test
    void testCallWithoutEventTransportReturnsGuidanceWithoutSuspending() {
        ToolContext toolContext = new ToolContext(new AiAgentToolContext(actionContext).toMap());

        String result = toolCallback.call(TOOL_INPUT, toolContext);

        assertEquals(AiAgentUtilsAskUserQuestionTool.NO_EVENT_TRANSPORT_RESULT, result);
        verify(actionContext, never()).suspend(any());
        verify((ActionContextAware) actionContext, never()).getResumeUrl();
    }

    @Test
    void testCallWithResumeUrlSendsQuestionAndSuspends() {
        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(RESUME_URL);

        SseTransport sseTransport = new SseTransport();

        Instant before = Instant.now();

        String result = toolCallback.call(TOOL_INPUT, createStreamingToolContext(sseTransport));

        assertTrue(ToolSuspension.isSuspendedToolResult(result, suspendReference.get()));

        Map<String, @Nullable Object> event = getFirstBufferedEvent(sseTransport);

        assertNotNull(event);
        assertEquals(ASK_USER_QUESTION, event.get(EVENT_TYPE));
        assertEquals(RESUME_URL, event.get("resumeUrl"));
        assertEquals(EXPECTED_QUESTIONS, event.get("questions"));

        Suspend suspend = suspendReference.get();

        assertNotNull(suspend);
        Map<String, ?> continueParameters = suspend.continueParameters();

        assertEquals(1, ((List<?>) continueParameters.get("questions")).size());
        assertEquals(true, continueParameters.get(MetadataConstants.STREAMING_RESUME));

        Instant expectedExpiresAt = before.plus(AiAgentUtilsAskUserQuestionTool.SUSPEND_TIMEOUT);
        Duration drift = Duration.between(expectedExpiresAt, suspend.expiresAt());

        assertFalse(drift.isNegative());
        assertTrue(drift.compareTo(Duration.ofMinutes(1)) < 0);
    }

    @Test
    void testCallInEditorEnvironmentSendsQuestionWithoutResumeUrlAndDoesNotSuspend() {
        when(((ActionContextAware) actionContext).isEditorEnvironment()).thenReturn(true);
        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(RESUME_URL);

        SseTransport sseTransport = new SseTransport();

        String result = toolCallback.call(TOOL_INPUT, createStreamingToolContext(sseTransport));

        assertEquals(AiAgentUtilsAskUserQuestionTool.NO_RESUME_URL_RESULT, result);

        Map<String, @Nullable Object> event = getFirstBufferedEvent(sseTransport);

        assertNotNull(event);
        assertFalse(event.containsKey("resumeUrl"));
        assertEquals(EXPECTED_QUESTIONS, event.get("questions"));

        verify(actionContext, never()).suspend(any());
    }

    @Test
    void testCallWithoutResumeUrlSendsQuestionAndTellsModelToEndTurn() {
        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(null);

        SseTransport sseTransport = new SseTransport();

        String result = toolCallback.call(TOOL_INPUT, createStreamingToolContext(sseTransport));

        assertEquals(AiAgentUtilsAskUserQuestionTool.NO_RESUME_URL_RESULT, result);

        Map<String, @Nullable Object> event = getFirstBufferedEvent(sseTransport);

        assertNotNull(event);
        assertEquals(ASK_USER_QUESTION, event.get(EVENT_TYPE));
        assertFalse(event.containsKey("resumeUrl"));
        assertEquals(EXPECTED_QUESTIONS, event.get("questions"));
        verify(actionContext, never()).suspend(any());
    }

    @Test
    void testCallSendsQuestionThroughEmitterWhenPresent() {
        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(RESUME_URL);

        SseEmitter sseEmitter = mock(SseEmitter.class);
        SseTransport sseTransport = new SseTransport();

        String result = toolCallback.call(
            TOOL_INPUT, createStreamingToolContext(attach(sseTransport, sseEmitter)));

        assertTrue(ToolSuspension.isSuspendedToolResult(result, suspendReference.get()));
        verify(sseEmitter, times(1)).send(any(Map.class));
    }

    @Test
    void testCallReportsUndeliveredQuestionsWithoutSuspendingWhenEmitterSendFails() {
        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(RESUME_URL);

        SseEmitter sseEmitter = mock(SseEmitter.class);

        doThrow(new IllegalStateException("client disconnected")).when(sseEmitter)
            .send(any());

        SseTransport sseTransport = new SseTransport();

        String result = toolCallback.call(
            TOOL_INPUT, createStreamingToolContext(attach(sseTransport, sseEmitter)));

        assertEquals(AiAgentUtilsAskUserQuestionTool.QUESTIONS_NOT_DELIVERED_RESULT, result);
        verify(actionContext, never()).suspend(any());
    }

    @Test
    void testMalformedInputFailsWithoutSendingAQuestionOrSuspending() {
        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(RESUME_URL);

        SseTransport sseTransport = new SseTransport();

        ToolContext toolContext = createStreamingToolContext(sseTransport);

        assertThrows(RuntimeException.class, () -> toolCallback.call("not json", toolContext));

        assertTrue(getBufferedEvents(sseTransport).isEmpty());
        verify(actionContext, never()).suspend(any());
    }

    @Test
    void testCallWithoutAgentToolContextReturnsGuidanceAndDoesNotLeakToolContext() {
        SseTransport sseTransport = new SseTransport();

        ToolContext toolContextWithoutAgentToolContext = new ToolContext(Map.of("unrelated", "value"));

        String guidance = toolCallback.call(TOOL_INPUT, toolContextWithoutAgentToolContext);

        assertEquals(AiAgentUtilsAskUserQuestionTool.NO_EVENT_TRANSPORT_RESULT, guidance);
        assertTrue(getBufferedEvents(sseTransport).isEmpty());

        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(null);

        String result = toolCallback.call(TOOL_INPUT, createStreamingToolContext(sseTransport));

        assertEquals(AiAgentUtilsAskUserQuestionTool.NO_RESUME_URL_RESULT, result);
    }

    @Test
    void testReturnsSuspendedToolResultWhenSuspendObservedOnActionContext() {
        ToolCallback delegate = mock(ToolCallback.class);
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(delegate.getToolDefinition()).thenReturn(createToolDefinition());
        when(delegate.call(eq("input"), any(ToolContext.class))).thenReturn("real-delegate-result");
        when(actionContextAware.getSuspend())
            .thenReturn(new ActionContext.Suspend(Map.of("questions", "q1"), null));

        ToolContext toolContext = new ToolContext(
            new AiAgentToolContext(actionContextAware, new SseTransport()).toMap());

        AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback callback =
            new AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback(delegate);

        String result = callback.call("input", toolContext);

        ArgumentCaptor<Suspend> suspendCaptor = ArgumentCaptor.forClass(Suspend.class);

        verify(actionContextAware).suspend(suspendCaptor.capture());

        assertTrue(
            ToolSuspension.isSuspendedToolResult(result, suspendCaptor.getValue()),
            "When the tool sets a suspend on the agent context, the callback must replace the delegate's result with " +
                "the suspended tool result so the agent loop can locate the pending tool call on resume.");

        verify(delegate, times(1)).call("input", toolContext);
    }

    @Test
    void testPassesThroughDelegateResultWhenNoSuspendObserved() {
        ToolCallback delegate = mock(ToolCallback.class);
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(delegate.getToolDefinition()).thenReturn(createToolDefinition());
        when(delegate.call(eq("input"), any(ToolContext.class))).thenReturn("real-delegate-result");
        when(actionContextAware.getSuspend()).thenReturn(null);

        ToolContext toolContext = new ToolContext(
            new AiAgentToolContext(actionContextAware, new SseTransport()).toMap());

        AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback callback =
            new AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback(delegate);

        String result = callback.call("input", toolContext);

        assertEquals("real-delegate-result", result,
            "Without a suspend, the delegate's real result must pass through.");
    }

    @Test
    void testPassesThroughDelegateResultWhenTheActionContextCannotSuspend() {
        ToolCallback delegate = mock(ToolCallback.class);

        when(delegate.getToolDefinition()).thenReturn(createToolDefinition());
        when(delegate.call(eq("input"), any(ToolContext.class))).thenReturn("real-delegate-result");

        ToolContext toolContext = new ToolContext(
            new AiAgentToolContext(mock(ActionContext.class), new SseTransport())
                .toMap());

        AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback callback =
            new AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback(delegate);

        String result = callback.call("input", toolContext);

        assertEquals("real-delegate-result", result);
    }

    @Test
    void testGetToolDefinitionDelegates() {
        ToolCallback delegate = mock(ToolCallback.class);

        ToolDefinition toolDefinition = createToolDefinition();

        when(delegate.getToolDefinition()).thenReturn(toolDefinition);

        AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback callback =
            new AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback(delegate);

        assertEquals(toolDefinition, callback.getToolDefinition());
    }

    private ToolContext createStreamingToolContext(SseTransport sseTransport) {
        return new ToolContext(new AiAgentToolContext(actionContext, sseTransport).toMap());
    }

    private static SseTransport attach(SseTransport sseTransport, SseEmitter sseEmitter) {
        sseTransport.attach(sseEmitter, exception -> {
            throw new IllegalStateException(exception);
        });

        return sseTransport;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, @Nullable Object>> getBufferedEvents(SseTransport sseTransport) {
        List<Map<String, @Nullable Object>> events = new ArrayList<>();
        SseEmitter recordingSseEmitter = mock(SseEmitter.class);

        doAnswer(invocation -> events.add(invocation.getArgument(0))).when(recordingSseEmitter)
            .send(any());

        attach(sseTransport, recordingSseEmitter);

        return events;
    }

    private static @Nullable Map<String, @Nullable Object> getFirstBufferedEvent(SseTransport sseTransport) {
        List<Map<String, @Nullable Object>> events = getBufferedEvents(sseTransport);

        return events.isEmpty() ? null : events.getFirst();
    }

    private static ToolDefinition createToolDefinition() {
        return ToolDefinition.builder()
            .name("askUserQuestionTool")
            .description("ask")
            .inputSchema("{}")
            .build();
    }

}
