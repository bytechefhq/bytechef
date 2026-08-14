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
import com.agui.core.message.SystemMessage;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
final class SliceSpringAIAgentTest {

    private static final Long WORKSPACE_ID = 111L;
    private static final Long ENVIRONMENT_ID = 222L;
    private static final Long AUTHENTICATED_USER_ID = 333L;

    private final SliceSpringAIAgent agent = SliceSpringAIAgent.builder()
        .agentId("project_deployment_ask")
        .chatModel(mock(ChatModel.class))
        .systemMessage("system message")
        .state(new State())
        .build();

    SliceSpringAIAgentTest() throws AGUIException {
    }

    @Test
    void testBuilderCreatesAgentWithGivenAgentId() {
        assertThat(agent.getAgentId()).isEqualTo("project_deployment_ask");
    }

    @Test
    void testCreateSystemMessageIncludesSystemMessageAndStateBlock() {
        SystemMessage systemMessage = agent.createSystemMessage(new State(), List.of());

        assertThat(systemMessage.getContent()).contains("system message")
            .contains("State:");
    }

    @Test
    void testToolContextWritesAutomationToolInvocationContextFamily() {
        Map<String, Object> toolContext = agent.toolContext(newInputWithWorkspaceScope());

        AutomationToolInvocationContext automationToolInvocationContext =
            AutomationToolInvocationContext.fromToolContext(new ToolContext(toolContext));

        assertThat(automationToolInvocationContext).isNotNull();
        assertThat(automationToolInvocationContext.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(automationToolInvocationContext.environmentId()).isEqualTo(ENVIRONMENT_ID);
    }

    @Test
    void testToolContextStillWritesCopilotAgentToolFamily() {
        Map<String, Object> toolContext = agent.toolContext(newInputWithWorkspaceScope());

        assertThat(toolContext).containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID);
    }

    /**
     * Regression test for the Critical bug where every asset-file create tool failed on the panel and management MCP
     * surfaces: {@code AssetFile.validate()} requires a non-null {@code generatedByAgentSource} whenever
     * {@code source == AI_GENERATED}, and that value comes from {@code sourceOrdinal} on this tool context. Only a
     * slice whose {@code *AgentConfiguration} calls {@link SliceSpringAIAgent.Builder#sourceOrdinal} — the asset-file
     * slice — should see it populated.
     */
    @Test
    void testToolContextWritesSourceOrdinalWhenAgentConfiguresIt() throws AGUIException {
        SliceSpringAIAgent assetFileAgent = SliceSpringAIAgent.builder()
            .agentId("asset_file_ask")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system message")
            .state(new State())
            .sourceOrdinal(AutomationToolInvocationContext.SOURCE_ORDINAL_FILES)
            .build();

        Map<String, Object> toolContext = assetFileAgent.toolContext(newInputWithWorkspaceScope());

        AutomationToolInvocationContext automationToolInvocationContext =
            AutomationToolInvocationContext.fromToolContext(new ToolContext(toolContext));

        assertThat(automationToolInvocationContext).isNotNull();
        assertThat(automationToolInvocationContext.sourceOrdinal()).isNotNull()
            .isEqualTo(AutomationToolInvocationContext.SOURCE_ORDINAL_FILES);
    }

    /**
     * Negative counterpart to {@link #testToolContextWritesSourceOrdinalWhenAgentConfiguresIt()}: the shared
     * {@code agent} field is built WITHOUT {@link SliceSpringAIAgent.Builder#sourceOrdinal}, exactly as the three other
     * slices sharing this class (deployments, MCP servers, API collections) configure themselves. This is what
     * guarantees those slices stay unaffected by the asset-file slice's source-ordinal wiring.
     */
    @Test
    void testToolContextLeavesSourceOrdinalNullWhenAgentDoesNotConfigureIt() {
        Map<String, Object> toolContext = agent.toolContext(newInputWithWorkspaceScope());

        AutomationToolInvocationContext automationToolInvocationContext =
            AutomationToolInvocationContext.fromToolContext(new ToolContext(toolContext));

        assertThat(automationToolInvocationContext).isNotNull();
        assertThat(automationToolInvocationContext.sourceOrdinal()).isNull();
    }

    private static RunAgentInput newInputWithWorkspaceScope() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_WORKSPACE_ID, WORKSPACE_ID);
        stateMap.put(CopilotConstants.STATE_ENVIRONMENT_ID, ENVIRONMENT_ID);
        stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, AUTHENTICATED_USER_ID);

        return new RunAgentInput(null, null, new State(stateMap), null, null, null, null);
    }
}
