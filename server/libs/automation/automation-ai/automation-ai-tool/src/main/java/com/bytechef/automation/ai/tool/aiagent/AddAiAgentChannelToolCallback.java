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
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adds an inbound channel (trigger) to an AI Agent.
 *
 * @author Ivica Cardic
 */
public class AddAiAgentChannelToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "addAiAgentChannel";

    private static final String DESCRIPTION = """
        Add an inbound channel (trigger) to an AI Agent. Every new agent already has the two permanent
        channels chat and workflowCall — adding either again is rejected. Addable channelType values:
        schedule, slack, telegram, rocketchat (whatsapp is a known-disabled channel; do not add it).
        slack/telegram/rocketchat need a connectionId; schedule does not. parameters carries the
        channel's own configured input (e.g. schedule's cron expression) — leave it empty and ask the
        user, or use sensible defaults, when unsure. Returns the new channel's id and channelType.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "agentId": {"type": "integer", "description": "AI Agent id"},
                    "channelType": {
                        "type": "string",
                        "enum": ["schedule", "slack", "telegram", "rocketchat"],
                        "description": "Channel type to add"
                    },
                    "parameters": {"type": "object", "description": "Channel-specific input parameters"},
                    "connectionId": {"type": "integer", "description": "Connection id, required for slack/telegram/rocketchat"}
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
