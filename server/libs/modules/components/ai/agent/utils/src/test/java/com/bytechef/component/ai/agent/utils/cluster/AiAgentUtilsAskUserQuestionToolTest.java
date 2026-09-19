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

import static com.bytechef.platform.ai.constant.AiAgentToolContextKey.ACTION_CONTEXT;
import static com.bytechef.platform.ai.constant.AiAgentToolContextKey.SSE_BUFFERED_EVENTS;
import static com.bytechef.platform.ai.constant.AiAgentToolContextKey.SSE_EMITTER_REFERENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
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

    private ActionContext actionContext;
    private ToolCallback toolCallback;

    @BeforeEach
    void setUp() throws Exception {
        actionContext = mock(ActionContext.class, withSettings().extraInterfaces(ActionContextAware.class));

        ToolCallbackProvider toolCallbackProvider = AiAgentUtilsAskUserQuestionTool.CLUSTER_ELEMENT_DEFINITION
            .getElement()
            .apply(mock(Parameters.class), mock(Parameters.class), mock(Context.class));

        toolCallback = toolCallbackProvider.getToolCallbacks()[0];
    }

    @Test
    void testCallWithoutToolContextReturnsGuidance() {
        String result = toolCallback.call(TOOL_INPUT);

        assertEquals(AiAgentUtilsAskUserQuestionTool.NO_EVENT_TRANSPORT_RESULT, result);
        verify(actionContext, never()).suspend(any());
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

        toolCallback.call(TOOL_INPUT, createStreamingToolContext(bufferedEvents));

        Map<String, Object> event = bufferedEvents.poll();

        assertEquals(RESUME_URL, event.get("resumeUrl"));
        assertTrue(event.containsKey("questions"));
        verify(actionContext, times(1)).suspend(any(Suspend.class));
    }

    @Test
    void testCallWithoutResumeUrlSendsQuestionButDoesNotSuspend() {
        when(((ActionContextAware) actionContext).getResumeUrl()).thenReturn(null);

        Queue<Map<String, Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

        toolCallback.call(TOOL_INPUT, createStreamingToolContext(bufferedEvents));

        Map<String, Object> event = bufferedEvents.poll();

        assertFalse(event.containsKey("resumeUrl"));
        assertTrue(event.containsKey("questions"));
        verify(actionContext, never()).suspend(any());
    }

    private ToolContext createStreamingToolContext(Queue<Map<String, Object>> bufferedEvents) {
        return new ToolContext(
            Map.of(
                ACTION_CONTEXT, actionContext,
                SSE_BUFFERED_EVENTS, bufferedEvents,
                SSE_EMITTER_REFERENCE, new AtomicReference<>()));
    }
}
