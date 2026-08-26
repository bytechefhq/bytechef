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
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that fetches the full configuration of one AI Agent — its channels, elements (with
 * kind and parameters), settings, and unpublished-changes state — so the specialist can inspect an agent before
 * modifying it.
 *
 * @author Ivica Cardic
 */
public class GetAiAgentToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "getAiAgent";

    private static final String DESCRIPTION = """
        Get the full configuration of one AI Agent by id: its title/description/instructions, every
        channel (inbound trigger — channelType, parameters, connectionId), every element (building
        block — kind, referenceId, parameters, connectionId; kinds are MODEL, TOOL, SKILL, SUB_AGENT,
        KNOWLEDGE_BASE, CHAT_MEMORY, APPROVAL_GATE, APPROVAL_CHANNEL, APPROVAL_TOOL), the settings
        object (streamResponse plus the builtInTools on/off switches), and
        unpublishedChanges/lastPublishedVersion. Always call
        this before modifying an agent so you know its current channels and elements.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "id": {"type": "integer", "description": "AI Agent id"}
            },
            "required": ["id"]
        }""";

    private final AiAgentFacade aiAgentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public GetAiAgentToolCallback(AiAgentFacade aiAgentFacade) {
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
            GetAiAgentInput input = jsonMapper.readValue(toolInput, GetAiAgentInput.class);

            if (input.id() == null) {
                return toolError("id is required");
            }

            AiAgentDTO agentDTO = aiAgentFacade.getAgent(input.id());

            return jsonMapper.writeValueAsString(toOutput(agentDTO));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, GetAiAgentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static AiAgentOutput toOutput(AiAgentDTO agentDTO) {
        AiAgent agent = agentDTO.agent();

        List<ChannelOutput> channelOutputs = agentDTO.channels()
            .stream()
            .map(GetAiAgentToolCallback::toChannelOutput)
            .toList();

        List<ElementOutput> elementOutputs = agentDTO.elements()
            .stream()
            .map(GetAiAgentToolCallback::toElementOutput)
            .toList();

        return new AiAgentOutput(
            agent.getId(), agent.getName(), agent.getTitle(), agent.getDescription(), agent.getInstructions(),
            channelOutputs, elementOutputs, agentDTO.settings(), agentDTO.unpublishedChanges(),
            agentDTO.lastPublishedVersion());
    }

    private static ChannelOutput toChannelOutput(AiAgentChannel channel) {
        return new ChannelOutput(
            channel.getId(), channel.getChannelType(), channel.getPosition(), channel.getParameters(),
            channel.getConnectionId());
    }

    private static ElementOutput toElementOutput(AiAgentElement element) {
        return new ElementOutput(
            element.getId(), element.getKind(), element.getReferenceId(), element.getParameters(),
            element.getConnectionId(), element.getPosition());
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record GetAiAgentInput(Long id) {
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record AiAgentOutput(
        Long id, String name, String title, @Nullable String description, @Nullable String instructions,
        List<ChannelOutput> channels, List<ElementOutput> elements, Map<String, ?> settings,
        boolean unpublishedChanges, int lastPublishedVersion) {
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record ChannelOutput(
        Long id, String channelType, int position, Map<String, ?> parameters, @Nullable Long connectionId) {
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record ElementOutput(
        Long id, String kind, @Nullable Long referenceId, Map<String, ?> parameters, @Nullable Long connectionId,
        int position) {
    }
}
