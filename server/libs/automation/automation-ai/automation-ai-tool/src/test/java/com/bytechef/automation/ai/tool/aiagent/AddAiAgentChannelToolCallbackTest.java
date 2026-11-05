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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.channel.AiAgentChannelType;
import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel;
import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel.Binding;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class AddAiAgentChannelToolCallbackTest {

    private final AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);
    private final AddAiAgentChannelToolCallback addAiAgentChannelToolCallback =
        new AddAiAgentChannelToolCallback(aiAgentFacade);
    private final JsonMapper jsonMapper = new JsonMapper();

    @BeforeEach
    void beforeEach() {
        when(aiAgentFacade.getAgentChannelDefinitions()).thenReturn(
            List.of(
                channelDefinition(AiAgentChannelType.CHAT, false), channelDefinition("schedule", false),
                channelDefinition("slack", true), channelDefinition("twilio", true)));
    }

    @Test
    void testCallRejectsAnUnknownChannelTypeWithTheAvailableOnes() {
        JsonNode result = jsonMapper.readTree(
            addAiAgentChannelToolCallback.call("{\"agentId\": 1, \"channelType\": \"pager\"}"));

        assertThat(result.get("error")
            .asString())
                .contains("Unknown channelType 'pager'",
                    "schedule, slack (needs connectionId), twilio (needs connectionId)")
                .doesNotContain(AiAgentChannelType.CHAT);

        verify(aiAgentFacade, never()).addAgentChannel(anyLong(), anyString(), anyMap(), any());
    }

    @Test
    void testToolDefinitionDoesNotHardCodeTheChannelTypes() {
        ToolDefinition toolDefinition = addAiAgentChannelToolCallback.getToolDefinition();

        assertThat(toolDefinition.inputSchema()).doesNotContain("\"enum\"");
        assertThat(toolDefinition.description()).doesNotContain("disabled");
    }

    @Test
    void testCallRejectsAScheduleWithoutAPrompt() {
        JsonNode result = jsonMapper.readTree(addAiAgentChannelToolCallback.call(
            "{\"agentId\": 1, \"channelType\": \"schedule\", \"parameters\": {\"expression\": \"0 9 * * ?\"}}"));

        assertThat(result.get("error")
            .asString()).contains("parameters.prompt");

        verify(aiAgentFacade, never()).addAgentChannel(anyLong(), anyString(), anyMap(), any());
    }

    @Test
    void testCallRejectsAScheduleWithoutAnExpression() {
        JsonNode result = jsonMapper.readTree(addAiAgentChannelToolCallback.call(
            "{\"agentId\": 1, \"channelType\": \"schedule\", \"parameters\": {\"prompt\": \"Summarize\"}}"));

        assertThat(result.get("error")
            .asString()).contains("parameters.expression");

        verify(aiAgentFacade, never()).addAgentChannel(anyLong(), anyString(), anyMap(), any());
    }

    @Test
    void testCallAddsAScheduleWithAPromptAndAnExpression() {
        Map<String, Object> parameters = Map.of("expression", "0 9 ? * MON-FRI", "prompt", "Summarize");

        when(aiAgentFacade.addAgentChannel(1L, "schedule", parameters, null)).thenReturn(channel());

        JsonNode result = jsonMapper.readTree(addAiAgentChannelToolCallback.call(
            "{\"agentId\": 1, \"channelType\": \"schedule\", " +
                "\"parameters\": {\"expression\": \"0 9 ? * MON-FRI\", \"prompt\": \"Summarize\"}}"));

        assertThat(result.get("created")
            .asBoolean()).isTrue();

        verify(aiAgentFacade).addAgentChannel(eq(1L), eq("schedule"), eq(parameters), any());
    }

    private static ResolvedAgentChannel channelDefinition(String name, boolean connectionRequired) {
        return new ResolvedAgentChannel(
            name, name, null, null, name + "/v1/newMessage", null, connectionRequired, Map.of(), Map.of(), Set.of(),
            Set.of(), new Binding(null, null, null, null, null, null, Map.of(), Map.of()), null);
    }

    private static AiAgentChannel channel() {
        AiAgentChannel channel = new AiAgentChannel();

        channel.setId(5L);
        channel.setChannelType("schedule");

        return channel;
    }
}
