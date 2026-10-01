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
import com.bytechef.automation.ai.agent.channel.AiAgentChannelType;
import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class AddAiAgentChannelToolCallback implements ToolCallback {

    private static final String SCHEDULE_EXPRESSION = "expression";
    private static final String SCHEDULE_PROMPT = "prompt";
    private static final String TOOL_NAME = "addAiAgentChannel";

    private static final Set<String> PERMANENT_CHANNEL_TYPES = Set.of(
        AiAgentChannelType.CHAT, AiAgentChannelType.WORKFLOW_CALL);

    private static final String DESCRIPTION =
        """
            Add an inbound channel (trigger) to an AI Agent. Every new agent already has the two permanent
            channels chat and workflowCall, and adding either again is rejected. channelType is the name of a
            registered agent channel, for example schedule, slack or telegram. An unknown channelType is rejected
            with the list of channel types this server supports and which of them need a connectionId; schedule
            never needs one. parameters carries the channel's own configured input. A schedule channel requires parameters.prompt (the message the
            agent receives on every run) and parameters.expression (a five-field cron expression: minute hour
            day-of-month month day-of-week, e.g. "0 9 ? * MON-FRI" for weekdays at 9:00), plus an optional
            timezone (default UTC) and name. For the other channels leave parameters empty and ask the user,
            or use sensible defaults, when unsure. Returns the new channel's id and channelType.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "agentId": {"type": "integer", "description": "AI Agent id"},
                    "channelType": {
                        "type": "string",
                        "description": "Name of the registered agent channel to add, for example schedule, slack or telegram"
                    },
                    "parameters": {"type": "object", "description": "Channel-specific input parameters"},
                    "connectionId": {"type": "integer", "description": "Connection id, required by channels that need a connection"}
                },
                "required": ["agentId", "channelType"]
            }""";

    private final AiAgentFacade aiAgentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AddAiAgentChannelToolCallback(AiAgentFacade aiAgentFacade) {
        this.aiAgentFacade = aiAgentFacade;
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
            AddAiAgentChannelInput input = jsonMapper.readValue(toolInput, AddAiAgentChannelInput.class);

            if (input.agentId() == null) {
                return toolError("agentId is required");
            }

            if (input.channelType() == null || input.channelType()
                .isBlank()) {

                return toolError("channelType is required");
            }

            List<ResolvedAgentChannel> addableChannels = aiAgentFacade.getAgentChannelDefinitions()
                .stream()
                .filter(channelDefinition -> !PERMANENT_CHANNEL_TYPES.contains(channelDefinition.name()))
                .toList();

            boolean addable = addableChannels.stream()
                .anyMatch(channelDefinition -> Objects.equals(channelDefinition.name(), input.channelType()));

            if (!addable) {
                return toolError(
                    "Unknown channelType '%s'. Available channel types: %s".formatted(
                        input.channelType(), describeChannels(addableChannels)));
            }

            if (AiAgentChannelType.SCHEDULE.equals(input.channelType())) {
                String scheduleError = validateScheduleParameters(input.parameters());

                if (scheduleError != null) {
                    return toolError(scheduleError);
                }
            }

            AiAgentChannel channel = aiAgentFacade.addAgentChannel(
                input.agentId(), input.channelType(), input.parameters(), input.connectionId());

            return jsonMapper.writeValueAsString(
                Map.of("created", true, "id", channel.getId(), "channelType", channel.getChannelType()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, AddAiAgentChannelToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static String describeChannels(List<ResolvedAgentChannel> channelDefinitions) {
        return channelDefinitions.stream()
            .map(channelDefinition -> channelDefinition.connectionRequired()
                ? channelDefinition.name() + " (needs connectionId)" : channelDefinition.name())
            .collect(Collectors.joining(", "));
    }

    private static @Nullable String validateScheduleParameters(@Nullable Map<String, Object> parameters) {
        if (isBlank(parameters, SCHEDULE_PROMPT)) {
            return "A schedule channel requires parameters.prompt, the message the agent receives on every run";
        }

        if (isBlank(parameters, SCHEDULE_EXPRESSION)) {
            return "A schedule channel requires parameters.expression, a five-field cron expression such as " +
                "\"0 9 ? * MON-FRI\"";
        }

        return null;
    }

    private static boolean isBlank(@Nullable Map<String, Object> parameters, String key) {
        if (parameters == null || !(parameters.get(key) instanceof String value)) {
            return true;
        }

        return value.isBlank();
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record AddAiAgentChannelInput(
        Long agentId, String channelType, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {
    }
}
