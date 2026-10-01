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

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class SearchAiAgentToolsToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "searchAiAgentTools";

    private static final String AI_AGENT_COMPONENT_NAME = "aiAgent";
    private static final int AI_AGENT_COMPONENT_VERSION = 1;
    private static final String TOOLS_CLUSTER_ELEMENT_TYPE = "tools";
    private static final String SKILLS_TOOL_NAME = "skillsTool";
    private static final int MAX_RESULTS = 25;

    private static final String DESCRIPTION = """
        Search the tools an AI Agent can be given, such as the current date, arithmetic, Slack, Gmail or
        HTTP requests. Call it before adding a TOOL element: an agent tool is a component's tool cluster
        element, not one of its actions, and only components that declare one can be added. query is a few
        words describing what the agent should be able to do (e.g. "current date", "send slack message");
        omit it to list the first tools. Returns a JSON array of matches, best first, each with
        componentName, componentVersion, actionName, title and description. Pass componentName,
        componentVersion and actionName unchanged as the TOOL element's parameters in addAiAgentElement.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "Words describing what the tool should do"}
            }
        }""";

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SearchAiAgentToolsToolCallback(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            SearchAiAgentToolsInput input = jsonMapper.readValue(toolInput, SearchAiAgentToolsInput.class);

            List<String> queryWords = queryWords(input.query());

            List<ClusterElementDefinition> toolDefinitions =
                clusterElementDefinitionService.getRootClusterElementDefinitions(
                    AI_AGENT_COMPONENT_NAME, AI_AGENT_COMPONENT_VERSION, TOOLS_CLUSTER_ELEMENT_TYPE);

            List<AiAgentToolOption> toolOptions = toolDefinitions.stream()
                .filter(toolDefinition -> !SKILLS_TOOL_NAME.equals(toolDefinition.getName()))
                .map(toolDefinition -> new ScoredToolDefinition(toolDefinition, score(toolDefinition, queryWords)))
                .filter(scoredToolDefinition -> queryWords.isEmpty() || scoredToolDefinition.score() > 0)
                .sorted(Comparator.comparingInt(ScoredToolDefinition::score)
                    .reversed())
                .limit(MAX_RESULTS)
                .map(scoredToolDefinition -> toToolOption(scoredToolDefinition.toolDefinition()))
                .toList();

            return jsonMapper.writeValueAsString(toolOptions);
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, SearchAiAgentToolsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static List<String> queryWords(@Nullable String query) {
        if (query == null) {
            return List.of();
        }

        return Arrays.stream(query.toLowerCase(Locale.ROOT)
            .split("[^a-z0-9]+"))
            .filter(word -> word.length() > 1)
            .toList();
    }

    private static int score(ClusterElementDefinition toolDefinition, List<String> queryWords) {
        String searchableText = String.join(
            " ", toolDefinition.getComponentName(), toolDefinition.getName(),
            Objects.toString(toolDefinition.getTitle(), ""), Objects.toString(toolDefinition.getDescription(), ""))
            .toLowerCase(Locale.ROOT);

        int score = 0;

        for (String queryWord : queryWords) {
            if (searchableText.contains(queryWord)) {
                score++;
            }
        }

        return score;
    }

    private static AiAgentToolOption toToolOption(ClusterElementDefinition toolDefinition) {
        return new AiAgentToolOption(
            toolDefinition.getComponentName(), toolDefinition.getComponentVersion(), toolDefinition.getName(),
            toolDefinition.getTitle(), toolDefinition.getDescription());
    }

    private record ScoredToolDefinition(ClusterElementDefinition toolDefinition, int score) {
    }

    public record AiAgentToolOption(
        String componentName, int componentVersion, String actionName, @Nullable String title,
        @Nullable String description) {
    }

    public record SearchAiAgentToolsInput(@Nullable String query) {
    }
}
