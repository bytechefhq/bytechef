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

import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class SearchAiAgentToolsToolCallbackTest {

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final JsonMapper jsonMapper = new JsonMapper();
    private final SearchAiAgentToolsToolCallback searchAiAgentToolsToolCallback =
        new SearchAiAgentToolsToolCallback(clusterElementDefinitionService);

    @BeforeEach
    void beforeEach() {
        List<ClusterElementDefinition> toolDefinitions = List.of(
            toolDefinition("dateHelper", "getCurrentDate", "Get Current Date", "Returns the current date."),
            toolDefinition("mathHelper", "addition", "Addition", "Adds two numbers."),
            toolDefinition("slack", "sendChannelMessage", "Send Channel Message", "Posts a message to a channel."),
            toolDefinition("aiAgentUtils", "skillsTool", "Skills", "Loads the agent's skills."));

        when(clusterElementDefinitionService.getRootClusterElementDefinitions("aiAgent", 1, "tools"))
            .thenReturn(toolDefinitions);
    }

    @Test
    void testCallReturnsTheToolsMatchingTheQueryWithTheirElementParameters() {
        JsonNode result = jsonMapper.readTree(searchAiAgentToolsToolCallback.call("{\"query\": \"current date\"}"));

        assertThat(result.isArray()).isTrue();
        assertThat(result.get(0)
            .get("componentName")
            .asString()).isEqualTo("dateHelper");
        assertThat(result.get(0)
            .get("componentVersion")
            .asInt()).isEqualTo(1);
        assertThat(result.get(0)
            .get("actionName")
            .asString()).isEqualTo("getCurrentDate");
    }

    @Test
    void testCallMatchesTheComponentName() {
        JsonNode result = jsonMapper.readTree(searchAiAgentToolsToolCallback.call("{\"query\": \"slack\"}"));

        assertThat(componentNames(result)).containsExactly("slack");
    }

    @Test
    void testCallListsEveryToolExceptTheSkillsToolWithoutAQuery() {
        JsonNode result = jsonMapper.readTree(searchAiAgentToolsToolCallback.call("{}"));

        assertThat(componentNames(result)).containsExactlyInAnyOrder("dateHelper", "mathHelper", "slack");
    }

    private static List<String> componentNames(JsonNode result) {
        List<String> componentNames = new ArrayList<>();

        for (JsonNode entry : result) {
            componentNames.add(entry.get("componentName")
                .asString());
        }

        return componentNames;
    }

    private static ClusterElementDefinition toolDefinition(
        String componentName, String name, String title, String description) {

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getComponentName()).thenReturn(componentName);
        when(clusterElementDefinition.getComponentVersion()).thenReturn(1);
        when(clusterElementDefinition.getName()).thenReturn(name);
        when(clusterElementDefinition.getTitle()).thenReturn(title);
        when(clusterElementDefinition.getDescription()).thenReturn(description);

        return clusterElementDefinition;
    }
}
