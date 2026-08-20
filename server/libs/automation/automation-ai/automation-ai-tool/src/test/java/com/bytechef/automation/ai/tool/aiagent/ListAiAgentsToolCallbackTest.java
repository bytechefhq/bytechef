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
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class ListAiAgentsToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesListAiAgentsName() {
        ListAiAgentsToolCallback callback = new ListAiAgentsToolCallback(mock(AiAgentFacade.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("listAiAgents");
        assertThat(definition.description()).isNotBlank();
    }

    @Test
    void testCallReturnsAgentListFromWorkspace() throws Exception {
        long workspaceId = 1L;

        AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);

        AiAgent agent = new AiAgent(42L);

        agent.setName("support-bot");
        agent.setTitle("Support Bot");
        agent.setDescription("Handles support tickets");

        AiAgentDTO agentDTO = new AiAgentDTO(
            agent, List.of(), List.of(), false, 2, null, List.of(), ResourceVisibility.WORKSPACE);

        when(aiAgentFacade.getAgents(workspaceId)).thenReturn(List.of(agentDTO));

        ListAiAgentsToolCallback callback = new ListAiAgentsToolCallback(aiAgentFacade);

        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .build()
                .toToolContext());

        String result = callback.call("{}", toolContext);

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();
        assertThat(arrayNode).hasSize(1);

        JsonNode first = arrayNode.get(0);

        assertThat(first.get("id")
            .asLong()).isEqualTo(42L);
        assertThat(first.get("name")
            .asText()).isEqualTo("support-bot");
        assertThat(first.get("title")
            .asText()).isEqualTo("Support Bot");
        assertThat(first.get("published")
            .asBoolean()).isTrue();
        assertThat(first.get("lastPublishedVersion")
            .asInt()).isEqualTo(2);
    }

    @Test
    void testCallReturnsErrorWhenWorkspaceIdMissing() throws Exception {
        ListAiAgentsToolCallback callback = new ListAiAgentsToolCallback(mock(AiAgentFacade.class));

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
