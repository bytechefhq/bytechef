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

package com.bytechef.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.automation.ai.tool.WorkflowExecutionToolContextKeys;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
class WorkflowExecutionSpringAIAgentTest {

    private static final Long CLIENT_SUPPLIED_WORKSPACE_ID = 999L;
    private static final Long EXECUTION_ENVIRONMENT_ID = 2L;
    private static final Long SESSION_ENVIRONMENT_ID = 1L;
    private static final Long VERIFIED_WORKSPACE_ID = 111L;

    @Test
    void testWorkspaceKeyAliasesTheAutomationToolContextKey() {
        assertThat(WorkflowExecutionToolContextKeys.WORKSPACE_ID)
            .as("the workflow-execution tools read the same map entry the verified copilot workspace is written to; "
                + "if these ever diverge, that entry stops being populated and every listing silently empties")
            .isEqualTo(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY);

        assertThat(WorkflowExecutionToolContextKeys.ENVIRONMENT_ID)
            .isEqualTo(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY);
    }

    @Test
    void testToolContextPublishesTheVerifiedWorkspaceNotTheClientSuppliedOne() throws AGUIException {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_WORKSPACE_ID, VERIFIED_WORKSPACE_ID);
        stateMap.put("parameters", Map.of("workspaceId", CLIENT_SUPPLIED_WORKSPACE_ID));

        assertThat(toolContext(stateMap))
            .as("state.parameters is raw client input; letting it overwrite the workspace the chat facade verified "
                + "let a member of one workspace read and enumerate another workspace's runs")
            .containsEntry(WorkflowExecutionToolContextKeys.WORKSPACE_ID, VERIFIED_WORKSPACE_ID);
    }

    @Test
    void testToolContextPublishesNoWorkspaceWhenNoneWasVerified() throws AGUIException {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("parameters", Map.of("workspaceId", CLIENT_SUPPLIED_WORKSPACE_ID));

        assertThat(toolContext(stateMap))
            .as("an unverified workspace must not reach the tools at all, not merely lose to a verified one")
            .doesNotContainKey(WorkflowExecutionToolContextKeys.WORKSPACE_ID);
    }

    @Test
    void testToolContextNarrowsTheEnvironmentToTheExecutionOnScreen() throws AGUIException {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_ENVIRONMENT_ID, SESSION_ENVIRONMENT_ID);
        stateMap.put("parameters", Map.of("environmentId", EXECUTION_ENVIRONMENT_ID));

        assertThat(toolContext(stateMap))
            .as("the run being diagnosed is legitimately not in the environment the session's selector points at")
            .containsEntry(WorkflowExecutionToolContextKeys.ENVIRONMENT_ID, EXECUTION_ENVIRONMENT_ID);
    }

    @Test
    void testToolContextKeepsTheSessionEnvironmentWhenParametersCarryNone() throws AGUIException {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_ENVIRONMENT_ID, SESSION_ENVIRONMENT_ID);

        assertThat(toolContext(stateMap))
            .containsEntry(WorkflowExecutionToolContextKeys.ENVIRONMENT_ID, SESSION_ENVIRONMENT_ID);
    }

    private static Map<String, Object> toolContext(Map<String, Object> stateMap) throws AGUIException {
        WorkflowExecutionSpringAIAgent agent = WorkflowExecutionSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State(new HashMap<>()))
            .build();

        return agent.toolContext(new RunAgentInput(null, null, new State(stateMap), null, null, null, null));
    }
}
