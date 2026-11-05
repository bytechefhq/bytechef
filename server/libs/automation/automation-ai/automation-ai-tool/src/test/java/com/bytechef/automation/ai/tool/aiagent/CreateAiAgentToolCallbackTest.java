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

package com.bytechef.automation.ai.tool.aiagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class CreateAiAgentToolCallbackTest {

    private static final long WORKSPACE_ID = 1L;

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallPassesProjectIdAndReturnsIt() throws Exception {
        AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);

        when(aiAgentFacade.createAgent("Support Bot", null, WORKSPACE_ID, 7L)).thenReturn(agentDTO(7L));

        String result = new CreateAiAgentToolCallback(aiAgentFacade).call(
            "{\"title\": \"Support Bot\", \"projectId\": 7}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("projectId")
            .asLong()).isEqualTo(7L);

        verify(aiAgentFacade).createAgent("Support Bot", null, WORKSPACE_ID, 7L);
    }

    @Test
    void testCallWithoutProjectIdLetsTheFacadeCreateAProject() throws Exception {
        AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);

        when(aiAgentFacade.createAgent("Support Bot", null, WORKSPACE_ID, null)).thenReturn(agentDTO(9L));

        String result = new CreateAiAgentToolCallback(aiAgentFacade).call(
            "{\"title\": \"Support Bot\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("projectId")
            .asLong()).isEqualTo(9L);

        verify(aiAgentFacade).createAgent("Support Bot", null, WORKSPACE_ID, null);
    }

    private static AiAgentDTO agentDTO(long projectId) {
        AiAgent agent = new AiAgent(42L);

        agent.setName("support-bot");
        agent.setTitle("Support Bot");

        return new AiAgentDTO(agent, projectId, List.of(), List.of(), true, 0, null);
    }

    private static ToolContext toolContext() {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(WORKSPACE_ID)
                .build()
                .toToolContext());
    }
}
