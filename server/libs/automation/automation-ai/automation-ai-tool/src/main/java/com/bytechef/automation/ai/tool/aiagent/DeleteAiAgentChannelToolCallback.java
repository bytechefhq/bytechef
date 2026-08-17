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
 * Deletes a channel from an AI Agent. The permanent {@code chat}/{@code workflowCall} channels cannot be deleted.
 *
 * @author Ivica Cardic
 */
public class DeleteAiAgentChannelToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "deleteAiAgentChannel";

    private static final String DESCRIPTION = """
        Delete a channel from an AI Agent by channel id (from getAiAgent's channels list). The permanent
        chat and workflowCall channels cannot be deleted and this call is rejected for them. Always
        confirm with the user before calling — the agent stops being reachable through this channel.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "channelId": {"type": "integer", "description": "Channel id to delete"}
            },
            "required": ["channelId"]
        }""";

    private final AiAgentFacade aiAgentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DeleteAiAgentChannelToolCallback(AiAgentFacade aiAgentFacade) {
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
            DeleteAiAgentChannelInput input = jsonMapper.readValue(toolInput, DeleteAiAgentChannelInput.class);

            if (input.channelId() == null) {
                return toolError("channelId is required");
            }

            aiAgentFacade.deleteAgentChannel(input.channelId());

            return jsonMapper.writeValueAsString(Map.of("deleted", true, "channelId", input.channelId()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, DeleteAiAgentChannelToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record DeleteAiAgentChannelInput(Long channelId) {
    }
}
