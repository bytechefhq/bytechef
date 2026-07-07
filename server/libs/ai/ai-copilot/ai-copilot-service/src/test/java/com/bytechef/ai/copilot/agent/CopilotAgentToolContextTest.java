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
import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
class CopilotAgentToolContextTest {

    @Test
    void testCodeEditorAgentPublishesCallerIdentity() throws Exception {
        CodeEditorSpringAIAgent agent = CodeEditorSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State(new HashMap<>()))
            .build();

        assertPublishesUserId(agent.toolContext(newInputWithUserId()));
    }

    @Test
    void testClusterElementAgentPublishesCallerIdentity() throws Exception {
        ClusterElementSpringAIAgent agent = ClusterElementSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State(new HashMap<>()))
            .build();

        assertPublishesUserId(agent.toolContext(newInputWithUserId()));
    }

    @Test
    void testSkillsAgentPublishesCallerIdentity() throws Exception {
        SkillsSpringAIAgent agent = SkillsSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State(new HashMap<>()))
            .build();

        assertPublishesUserId(agent.toolContext(newInputWithUserId()));
    }

    @Test
    void testConverterAgentPublishesCallerIdentity() throws Exception {
        ConverterSpringAIAgent agent = ConverterSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State(new HashMap<>()))
            .build();

        assertPublishesUserId(agent.toolContext(newInputWithUserId()));
    }

    private static void assertPublishesUserId(Map<String, Object> toolContext) {
        assertThat(toolContext).containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, 99L);
    }

    private static RunAgentInput newInputWithUserId() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, 99L);

        return newInput(stateMap);
    }

    private static RunAgentInput newInput(Map<String, Object> stateMap) {
        return new RunAgentInput(null, null, new State(stateMap), null, null, null, null);
    }
}
