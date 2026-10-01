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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class AddAiAgentElementToolCallbackTest {

    private final AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);
    private final AddAiAgentElementToolCallback addAiAgentElementToolCallback =
        new AddAiAgentElementToolCallback(aiAgentFacade);
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallRejectsAModelWithoutAConnection() {
        JsonNode result = jsonMapper.readTree(addAiAgentElementToolCallback.call(
            "{\"agentId\": 1, \"kind\": \"MODEL\", " +
                "\"parameters\": {\"provider\": \"anthropic\", \"model\": \"claude-sonnet-4-5\"}}"));

        assertThat(result.get("error")
            .asString()).contains("connectionId");

        verify(aiAgentFacade, never()).addAgentElement(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    void testCallAddsAModelWithAConnection() {
        Map<String, Object> parameters = Map.of("model", "claude-sonnet-4-5", "provider", "anthropic");

        when(aiAgentFacade.addAgentElement(1L, AiAgentElement.KIND_MODEL, null, parameters, 9L))
            .thenReturn(element(AiAgentElement.KIND_MODEL));

        JsonNode result = jsonMapper.readTree(addAiAgentElementToolCallback.call(
            "{\"agentId\": 1, \"kind\": \"MODEL\", \"connectionId\": 9, " +
                "\"parameters\": {\"provider\": \"anthropic\", \"model\": \"claude-sonnet-4-5\"}}"));

        assertThat(result.get("created")
            .asBoolean()).isTrue();

        verify(aiAgentFacade).addAgentElement(1L, AiAgentElement.KIND_MODEL, null, parameters, 9L);
    }

    @Test
    void testCallRejectsAToolWithoutAComponentAndAction() {
        JsonNode result = jsonMapper.readTree(addAiAgentElementToolCallback.call(
            "{\"agentId\": 1, \"kind\": \"TOOL\", " +
                "\"parameters\": {\"componentName\": \"slack\", \"toolName\": \"tools\"}}"));

        assertThat(result.get("error")
            .asString()).contains("actionName")
                .contains("searchAiAgentTools");

        verify(aiAgentFacade, never()).addAgentElement(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    void testCallRejectsAModelWithoutAProvider() {
        JsonNode result = jsonMapper.readTree(addAiAgentElementToolCallback.call(
            "{\"agentId\": 1, \"kind\": \"MODEL\", \"connectionId\": 9, " +
                "\"parameters\": {\"model\": \"claude-sonnet-4-5\", \"maxTokens\": 1024}}"));

        assertThat(result.get("error")
            .asString()).contains("parameters.provider")
                .contains("parameters.model");

        verify(aiAgentFacade, never()).addAgentElement(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    void testCallRejectsASkillWithoutAReference() {
        JsonNode result = jsonMapper.readTree(addAiAgentElementToolCallback.call(
            "{\"agentId\": 1, \"kind\": \"SKILL\"}"));

        assertThat(result.get("error")
            .asString()).contains("referenceId")
                .contains("searchAiAgentReferences");

        verify(aiAgentFacade, never()).addAgentElement(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    void testCallRejectsASubAgentWithoutAReference() {
        JsonNode result = jsonMapper.readTree(addAiAgentElementToolCallback.call(
            "{\"agentId\": 1, \"kind\": \"SUB_AGENT\"}"));

        assertThat(result.get("error")
            .asString()).contains("listAiAgents");
    }

    @Test
    void testCallAddsAToolWithoutAConnection() {
        Map<String, Object> parameters =
            Map.of("actionName", "get", "componentName", "httpClient", "componentVersion", 1);

        when(aiAgentFacade.addAgentElement(1L, "TOOL", null, parameters, null))
            .thenReturn(element("TOOL"));

        JsonNode result = jsonMapper.readTree(addAiAgentElementToolCallback.call(
            "{\"agentId\": 1, \"kind\": \"TOOL\", " +
                "\"parameters\": {\"componentName\": \"httpClient\", \"componentVersion\": 1, \"actionName\": \"get\"}}"));

        assertThat(result.get("created")
            .asBoolean()).isTrue();
    }

    private static AiAgentElement element(String kind) {
        AiAgentElement element = new AiAgentElement(1L, kind);

        element.setId(3L);

        return element;
    }
}
