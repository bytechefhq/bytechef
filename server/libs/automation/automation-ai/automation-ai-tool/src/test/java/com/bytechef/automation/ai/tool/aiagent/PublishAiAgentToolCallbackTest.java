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

import com.bytechef.automation.ai.agent.exception.AiAgentErrorType;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.exception.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class PublishAiAgentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesPublishAiAgentName() {
        PublishAiAgentToolCallback callback = new PublishAiAgentToolCallback(mock(AiAgentFacade.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("publishAiAgent");
        assertThat(definition.description()).isNotBlank();
    }

    @Test
    void testCallReturnsPublishedVersionOnSuccess() throws Exception {
        AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);

        when(aiAgentFacade.publishAgent(42L, null)).thenReturn(3);

        PublishAiAgentToolCallback callback = new PublishAiAgentToolCallback(aiAgentFacade);

        String result = callback.call("{\"id\": 42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("published")
            .asBoolean()).isTrue();
        assertThat(node.get("nextDraftVersion")
            .asInt()).isEqualTo(3);
    }

    @Test
    void testCallConvertsConfigurationExceptionToToolError() throws Exception {
        AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);

        when(aiAgentFacade.publishAgent(42L, null))
            .thenThrow(
                new ConfigurationException(
                    "Agent 42 has no MODEL element", AiAgentErrorType.MODEL_MISSING));

        PublishAiAgentToolCallback callback = new PublishAiAgentToolCallback(aiAgentFacade);

        String result = callback.call("{\"id\": 42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("publishAiAgent failed");
    }

    @Test
    void testCallReturnsErrorWhenIdMissing() throws Exception {
        PublishAiAgentToolCallback callback = new PublishAiAgentToolCallback(mock(AiAgentFacade.class));

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
