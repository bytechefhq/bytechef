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
import static com.bytechef.platform.ai.constant.AiAgentToolContextKey.ACTION_CONTEXT;
import static com.bytechef.platform.ai.constant.AiAgentToolContextKey.SSE_BUFFERED_EVENTS;
import static com.bytechef.platform.ai.constant.AiAgentToolContextKey.SSE_EMITTER_REFERENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.bytechef.platform.component.definition.ActionContextAware;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

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
        ToolContext toolContext = new ToolContext(Map.of(ACTION_CONTEXT, actionContext));

        String result = toolCallback.call(TOOL_INPUT, toolContext);

        assertEquals(AiAgentUtilsAskUserQuestionTool.NO_EVENT_TRANSPORT_RESULT, result);
        verify(actionContext, never()).suspend(any());
        verify((ActionContextAware) actionContext, never()).getResumeUrl();
    }

    @Test
    void testCallWithResumeUrlSendsQuestionAndSuspends() {
        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(RESUME_URL);

        Queue<Map<String, Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

        Instant before = Instant.now();

        toolCallback.call(TOOL_INPUT, createStreamingToolContext(bufferedEvents));

        Map<String, Object> event = bufferedEvents.poll();

        assertNotNull(event);
        assertEquals(ASK_USER_QUESTION, event.get(EVENT_TYPE));
        assertEquals(RESUME_URL, event.get("resumeUrl"));
        assertEquals(EXPECTED_QUESTIONS, event.get("questions"));

        verify(actionContext, times(1)).suspend(any(Suspend.class));

        Suspend suspend = suspendReference.get();

        assertNotNull(suspend);
        assertEquals(1, ((List<?>) suspend.continueParameters()
            .get("questions")).size());

        Instant expectedExpiresAt = before.plus(AiAgentUtilsAskUserQuestionTool.SUSPEND_TIMEOUT);
        Duration drift = Duration.between(expectedExpiresAt, suspend.expiresAt());

        assertFalse(drift.isNegative());
        assertTrue(drift.compareTo(Duration.ofMinutes(1)) < 0);
    }

    @Test
    void testCallWithoutResumeUrlSendsQuestionAndTellsModelToEndTurn() {
        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(null);

        Queue<Map<String, Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

        String result = toolCallback.call(TOOL_INPUT, createStreamingToolContext(bufferedEvents));

        assertEquals(AiAgentUtilsAskUserQuestionTool.NO_RESUME_URL_RESULT, result);

        Map<String, Object> event = bufferedEvents.poll();

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
        Queue<Map<String, Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

        toolCallback.call(TOOL_INPUT, createStreamingToolContext(bufferedEvents, new AtomicReference<>(sseEmitter)));

        verify(sseEmitter, times(1)).send(any(Map.class));
        assertTrue(bufferedEvents.isEmpty());
    }

    @Test
    void testCallReportsUndeliveredQuestionsWithoutSuspendingWhenEmitterSendFails() {
        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(RESUME_URL);

        SseEmitter sseEmitter = mock(SseEmitter.class);

        doThrow(new IllegalStateException("client disconnected")).when(sseEmitter)
            .send(any());

        Queue<Map<String, Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

        String result = toolCallback.call(
            TOOL_INPUT, createStreamingToolContext(bufferedEvents, new AtomicReference<>(sseEmitter)));

        assertEquals(AiAgentUtilsAskUserQuestionTool.QUESTIONS_NOT_DELIVERED_RESULT, result);
        assertTrue(bufferedEvents.isEmpty());
        verify(actionContext, never()).suspend(any());
    }

    @Test
    void testCallReportsUndeliveredQuestionsWhenNoEmitterAndNoBuffer() {
        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(RESUME_URL);

        ToolContext toolContext = new ToolContext(
            Map.of(ACTION_CONTEXT, actionContext, SSE_EMITTER_REFERENCE, new AtomicReference<>()));

        String result = toolCallback.call(TOOL_INPUT, toolContext);

        assertEquals(AiAgentUtilsAskUserQuestionTool.QUESTIONS_NOT_DELIVERED_RESULT, result);
        verify(actionContext, never()).suspend(any());
    }

    @Test
    void testCallFailsWhenActionContextMissingAndDoesNotLeakToolContext() {
        Queue<Map<String, Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

        ToolContext toolContextWithoutActionContext = new ToolContext(Map.of(SSE_BUFFERED_EVENTS, bufferedEvents));

        RuntimeException exception = assertThrows(
            RuntimeException.class, () -> toolCallback.call(TOOL_INPUT, toolContextWithoutActionContext));

        assertTrue(getRootCauseMessage(exception).contains("ActionContext not available"));
        assertTrue(bufferedEvents.isEmpty());

        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(null);

        String result = toolCallback.call(TOOL_INPUT, createStreamingToolContext(bufferedEvents));

        assertEquals(AiAgentUtilsAskUserQuestionTool.NO_RESUME_URL_RESULT, result);
    }

    private ToolContext createStreamingToolContext(Queue<Map<String, Object>> bufferedEvents) {
        return createStreamingToolContext(bufferedEvents, new AtomicReference<>());
    }

    private ToolContext createStreamingToolContext(
        Queue<Map<String, Object>> bufferedEvents, AtomicReference<SseEmitter> emitterReference) {

        return new ToolContext(
            Map.of(
                ACTION_CONTEXT, actionContext,
                SSE_BUFFERED_EVENTS, bufferedEvents,
                SSE_EMITTER_REFERENCE, emitterReference));
    }

    private static String getRootCauseMessage(Throwable throwable) {
        Throwable rootCause = throwable;

        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        return String.valueOf(rootCause.getMessage());
    }
}
