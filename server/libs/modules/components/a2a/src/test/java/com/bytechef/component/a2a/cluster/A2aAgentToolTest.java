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

package com.bytechef.component.a2a.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class A2aAgentToolTest {

    private static final String AGENT_CARD_URL =
        "http://localhost:9555/api/automation/a2a/secret/.well-known/agent-card.json";

    @ParameterizedTest
    @ValueSource(
        strings = {
            "http://localhost:9555/api/automation/a2a/secret",
            "http://localhost:9555/api/automation/a2a/secret/",
            " http://localhost:9555/api/automation/a2a/secret ",
            "http://localhost:9555/api/automation/a2a/secret/.well-known/agent-card.json"
        })
    void testGetAgentCardBaseUrlKeepsThePathTheAgentIsServedUnder(String baseUrl) {
        String agentCardBaseUrl = A2aAgentTool.getAgentCardBaseUrl(baseUrl);

        URI agentCardUri = URI.create(agentCardBaseUrl)
            .resolve(".well-known/agent-card.json");

        assertThat(agentCardUri).hasToString(AGENT_CARD_URL);
    }

    @Test
    void testGetAgentCardBaseUrlForAnAgentServedAtTheRoot() {
        String agentCardBaseUrl = A2aAgentTool.getAgentCardBaseUrl("https://agent.example.com");

        URI agentCardUri = URI.create(agentCardBaseUrl)
            .resolve(".well-known/agent-card.json");

        assertThat(agentCardUri).hasToString("https://agent.example.com/.well-known/agent-card.json");
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "http://localhost:9555/api/automation/a2a/secret",
            "http://localhost:9555/api/automation/a2a/secret/",
            "http://localhost:9555/api/automation/a2a/secret/.well-known/agent-card.json"
        })
    void testGetEndpointUrlDropsTheTrailingSlashAndAgentCardPath(String baseUrl) {
        assertThat(A2aAgentTool.getEndpointUrl(baseUrl))
            .isEqualTo("http://localhost:9555/api/automation/a2a/secret");
    }

    @Test
    void testWithEndpointUrlPointsEveryInterfaceAtTheConfiguredUrl() {
        String publicUrl = "http://127.0.0.1:5173/api/automation/a2a/secret";
        String endpointUrl = "http://localhost:9555/api/automation/a2a/secret";

        AgentCard agentCard = new AgentCard.Builder()
            .additionalInterfaces(List.of(new AgentInterface("JSONRPC", publicUrl)))
            .capabilities(new AgentCapabilities(true, false, false, List.of()))
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .description("Java release expert")
            .name("Java Release Expert")
            .skills(List.of())
            .url(publicUrl)
            .version("1.0.0")
            .build();

        AgentCard endpointAgentCard = A2aAgentTool.withEndpointUrl(agentCard, endpointUrl);

        assertThat(endpointAgentCard.url()).isEqualTo(endpointUrl);
        assertThat(endpointAgentCard.additionalInterfaces())
            .containsExactly(new AgentInterface("JSONRPC", endpointUrl));
        assertThat(endpointAgentCard.name()).isEqualTo("Java Release Expert");
    }

    @Test
    void testApplyUsesTheToolNameAndDescriptionOfTheNode() {
        ToolDefinition toolDefinition = getToolDefinition(
            Map.of(
                "toolDescription", "Ask the Java Release Expert about Java and Spring releases.",
                "toolName", "askJavaReleaseExpert"));

        assertThat(toolDefinition.name()).isEqualTo("askJavaReleaseExpert");
        assertThat(toolDefinition.description()).isEqualTo(
            "Ask the Java Release Expert about Java and Spring releases.");
    }

    @Test
    void testApplyFallsBackToTheDefaultToolNameAndDescription() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", " ", "toolName", ""));

        assertThat(toolDefinition.name()).isEqualTo("sendTaskToRemoteAgent");
        assertThat(toolDefinition.description()).startsWith("Delegate a task to the configured remote A2A agent");
    }

    @Test
    void testGetAuthorizationHeadersSendsTheTokenAsABearerHeader() {
        Map<String, String> headers = A2aAgentTool.getAuthorizationHeaders(
            ParametersFactory.create(Map.of("token", "api-key")));

        assertThat(headers).containsExactly(Map.entry("Authorization", "Bearer api-key"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", " "
    })
    void testGetAuthorizationHeadersIsEmptyWithoutAToken(String token) {
        assertThat(A2aAgentTool.getAuthorizationHeaders(ParametersFactory.create(Map.of("token", token)))).isEmpty();
        assertThat(A2aAgentTool.getAuthorizationHeaders(ParametersFactory.create(Map.of()))).isEmpty();
    }

    private static ToolDefinition getToolDefinition(Map<String, ?> inputParameters) {
        Map<String, Object> toolInputParameters = new HashMap<>(inputParameters);

        toolInputParameters.put("agentUrl", "http://localhost:9555/api/automation/a2a/secret");

        ToolCallback[] toolCallbacks = A2aAgentTool.apply(
            ParametersFactory.create(toolInputParameters), ParametersFactory.create(Map.of()), mock(Context.class))
            .getToolCallbacks();

        assertThat(toolCallbacks).hasSize(1);

        return toolCallbacks[0].getToolDefinition();
    }
}
