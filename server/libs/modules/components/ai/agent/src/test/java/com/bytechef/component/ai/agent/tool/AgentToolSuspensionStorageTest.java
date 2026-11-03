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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Data;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AgentToolSuspensionStorageTest {

    private static final String SUSPENDED_TOOL_RESULT = createSuspendedToolResult();

    private final ActionContextAware actionContextAware = mock(ActionContextAware.class);
    private final Data data = mock(Data.class);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        when(actionContextAware.data(any())).thenAnswer(
            invocation -> ((ContextFunction<Data, ?>) invocation.getArgument(0)).apply(data));
        when(actionContextAware.getEnvironmentId()).thenReturn(1L);
        when(actionContextAware.getJobId()).thenReturn(2L);
        when(actionContextAware.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContextAware.getTaskExecutionId()).thenReturn(3L);
    }

    @Test
    void testPutStoresTheConversationInDataStorageAndKeepsOnlyItsKey() {
        AgentToolSuspension agentToolSuspension = createAgentToolSuspension();
        Map<String, Object> continueParameters = new HashMap<>();

        AgentToolSuspensionStorage.put(continueParameters, agentToolSuspension, actionContextAware);

        assertThat(continueParameters)
            .doesNotContainKey(AgentToolSuspension.CONTINUE_PARAMETER_KEY)
            .containsEntry(
                AgentToolSuspensionStorage.DATA_STORAGE_KEY_CONTINUE_PARAMETER_KEY, "__AGENT_TOOL_SUSPENSION_3");

        verify(data).put(eq(Scope.CURRENT_EXECUTION), eq("__AGENT_TOOL_SUSPENSION_3"), any(Map.class));
    }

    @Test
    void testFetchReadsTheConversationBackFromDataStorage() {
        AgentToolSuspension agentToolSuspension = createAgentToolSuspension();

        when(data.fetch(Scope.CURRENT_EXECUTION, "__AGENT_TOOL_SUSPENSION_3"))
            .thenReturn(Optional.of(ConvertUtils.convertValue(agentToolSuspension, Map.class)));

        AgentToolSuspension fetchedAgentToolSuspension = AgentToolSuspensionStorage.fetch(
            ParametersFactory.create(
                Map.of(
                    AgentToolSuspensionStorage.DATA_STORAGE_KEY_CONTINUE_PARAMETER_KEY, "__AGENT_TOOL_SUSPENSION_3")),
            actionContextAware);

        assertThat(fetchedAgentToolSuspension).isEqualTo(agentToolSuspension);
    }

    @Test
    void testFetchReadsAConversationStoredInTheSuspend() {
        AgentToolSuspension agentToolSuspension = createAgentToolSuspension();

        AgentToolSuspension fetchedAgentToolSuspension = AgentToolSuspensionStorage.fetch(
            ParametersFactory.create(
                Map.of(
                    AgentToolSuspension.CONTINUE_PARAMETER_KEY,
                    ConvertUtils.convertValue(agentToolSuspension, Map.class))),
            actionContextAware);

        assertThat(fetchedAgentToolSuspension).isEqualTo(agentToolSuspension);

        verify(actionContextAware, never()).data(any());
    }

    @Test
    void testPutKeepsTheConversationInTheSuspendInTheEditor() {
        when(actionContextAware.isEditorEnvironment()).thenReturn(true);

        AgentToolSuspension agentToolSuspension = createAgentToolSuspension();
        Map<String, Object> continueParameters = new HashMap<>();

        AgentToolSuspensionStorage.put(continueParameters, agentToolSuspension, actionContextAware);

        assertThat(continueParameters)
            .containsEntry(AgentToolSuspension.CONTINUE_PARAMETER_KEY, agentToolSuspension)
            .doesNotContainKey(AgentToolSuspensionStorage.DATA_STORAGE_KEY_CONTINUE_PARAMETER_KEY);

        verify(actionContextAware, never()).data(any());
    }

    @Test
    void testPutKeepsTheConversationInTheSuspendWhenDataStorageRejectsIt() {
        doThrow(new IllegalArgumentException("Value size exceeds 400KB limit per key"))
            .when(data)
            .put(eq(Scope.CURRENT_EXECUTION), anyString(), any());

        AgentToolSuspension agentToolSuspension = createAgentToolSuspension();
        Map<String, Object> continueParameters = new HashMap<>();

        AgentToolSuspensionStorage.put(continueParameters, agentToolSuspension, actionContextAware);

        assertThat(continueParameters)
            .containsEntry(AgentToolSuspension.CONTINUE_PARAMETER_KEY, agentToolSuspension)
            .doesNotContainKey(AgentToolSuspensionStorage.DATA_STORAGE_KEY_CONTINUE_PARAMETER_KEY);
    }

    @Test
    void testPutReplacesTheConversationOfAPreviousSuspend() {
        AgentToolSuspension agentToolSuspension = createAgentToolSuspension();
        Map<String, Object> continueParameters = new HashMap<>(
            Map.of(AgentToolSuspension.CONTINUE_PARAMETER_KEY, agentToolSuspension));

        AgentToolSuspensionStorage.put(continueParameters, agentToolSuspension, actionContextAware);

        assertThat(continueParameters)
            .doesNotContainKey(AgentToolSuspension.CONTINUE_PARAMETER_KEY)
            .containsKey(AgentToolSuspensionStorage.DATA_STORAGE_KEY_CONTINUE_PARAMETER_KEY);
    }

    private static String createSuspendedToolResult() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new ActionContext.Suspend(Map.of(), Instant.now()));

        return ToolSuspension.suspendedToolResult(actionContext);
    }

    private static AgentToolSuspension createAgentToolSuspension() {
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new AssistantMessage.ToolCall("call_1", "function", "requestApproval", "{}")))
            .build();

        ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
            .responses(
                List.of(new ToolResponseMessage.ToolResponse("call_1", "requestApproval", SUSPENDED_TOOL_RESULT)))
            .build();

        return new AgentToolSuspension(
            ConversationState.from(List.of(new UserMessage("Refund order 42"), assistantMessage, toolResponseMessage)),
            "call_1", SUSPENDED_TOOL_RESULT, null, null);
    }
}
