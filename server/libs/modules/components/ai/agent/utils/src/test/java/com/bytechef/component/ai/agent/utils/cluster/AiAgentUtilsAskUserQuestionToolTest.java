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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.constant.AiAgentToolContextKey;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Pins the sentinel-on-suspend contract: when the agent's {@link ActionContext} has a suspend set after the delegate
 * tool call completes, {@link AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback} must return the
 * {@link ToolSuspendConstants#SUSPENDED_SENTINEL} value so
 * {@code SuspendableToolCallingManager.findSentinelToolResponseId} can locate the pending tool call id; if no suspend
 * was set, the delegate's actual result must pass through unchanged.
 *
 * @author Ivica Cardic
 */
class AiAgentUtilsAskUserQuestionToolTest {

    @Test
    void testReturnsSentinelWhenSuspendObservedOnActionContext() {
        ToolCallback delegate = mock(ToolCallback.class);
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        ToolDefinition toolDefinition = ToolDefinition.builder()
            .name("askUserQuestionTool")
            .description("ask")
            .inputSchema("{}")
            .build();

        when(delegate.getToolDefinition()).thenReturn(toolDefinition);
        when(delegate.call(eq("input"), any(ToolContext.class))).thenReturn("real-delegate-result");
        when(actionContextAware.getSuspend())
            .thenReturn(new ActionContext.Suspend(Map.of("questions", "q1"), null));

        ToolContext toolContext = new ToolContext(Map.of(AiAgentToolContextKey.ACTION_CONTEXT, actionContextAware));

        AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback callback =
            new AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback(delegate);

        String result = callback.call("input", toolContext);

        assertEquals(
            ToolSuspendConstants.SUSPENDED_SENTINEL, result,
            "When the tool sets a suspend on the agent context, the callback must replace the delegate's result with " +
                "the suspend sentinel so the agent loop can locate the pending tool call on resume.");

        verify(delegate, times(1)).call("input", toolContext);
    }

    @Test
    void testPassesThroughDelegateResultWhenNoSuspendObserved() {
        ToolCallback delegate = mock(ToolCallback.class);
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        ToolDefinition toolDefinition = ToolDefinition.builder()
            .name("askUserQuestionTool")
            .description("ask")
            .inputSchema("{}")
            .build();

        when(delegate.getToolDefinition()).thenReturn(toolDefinition);
        when(delegate.call(eq("input"), any(ToolContext.class))).thenReturn("real-delegate-result");
        when(actionContextAware.getSuspend()).thenReturn(null);

        ToolContext toolContext = new ToolContext(Map.of(AiAgentToolContextKey.ACTION_CONTEXT, actionContextAware));

        AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback callback =
            new AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback(delegate);

        String result = callback.call("input", toolContext);

        assertEquals("real-delegate-result", result,
            "Without a suspend, the delegate's real result must pass through.");
    }

    @Test
    void testPassesThroughDelegateResultWhenActionContextMissing() {
        // If a future caller forgets to seed ACTION_CONTEXT into the ToolContext, we should NOT silently substitute
        // the sentinel — that would mask a wiring bug as a successful suspend.
        ToolCallback delegate = mock(ToolCallback.class);

        ToolDefinition toolDefinition = ToolDefinition.builder()
            .name("askUserQuestionTool")
            .description("ask")
            .inputSchema("{}")
            .build();

        when(delegate.getToolDefinition()).thenReturn(toolDefinition);
        when(delegate.call(eq("input"), any(ToolContext.class))).thenReturn("real-delegate-result");

        ToolContext toolContext = new ToolContext(Map.of());

        AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback callback =
            new AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback(delegate);

        String result = callback.call("input", toolContext);

        assertEquals("real-delegate-result", result);
    }

    @Test
    void testGetToolDefinitionDelegates() {
        ToolCallback delegate = mock(ToolCallback.class);

        ToolDefinition toolDefinition = ToolDefinition.builder()
            .name("askUserQuestionTool")
            .description("ask")
            .inputSchema("{}")
            .build();

        when(delegate.getToolDefinition()).thenReturn(toolDefinition);

        AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback callback =
            new AiAgentUtilsAskUserQuestionTool.ToolContextAwareToolCallback(delegate);

        assertEquals(toolDefinition, callback.getToolDefinition());
    }
}
