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
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lists every AI Agent in the current workspace. Returns a compact JSON array of
 * agent summaries (id, name, title, description, published) so the specialist can enumerate existing agents and refer
 * to them by id when calling getAiAgent.
 *
 * @author Ivica Cardic
 */
public class ListAiAgentsToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "listAiAgents";

    private static final String DESCRIPTION = """
        List every AI Agent in the current workspace. An AI Agent is a chat-first automation entity — a
        persona (model, instructions) reachable through one or more channels (chat, workflowCall,
        schedule, slack, telegram, rocketchat) and equipped with elements (tools, skills, sub-agents,
        knowledge bases). Returns a JSON array of summaries: id, name, title, description, published
        (true once the agent has at least one published version), lastPublishedVersion. Call this first
        when the user asks about their agents, then use getAiAgent for the full configuration of one.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {}
        }""";

    private final AiAgentFacade aiAgentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListAiAgentsToolCallback(AiAgentFacade aiAgentFacade) {
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
            AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError("Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            List<AiAgentDTO> agentDTOs = aiAgentFacade.getAgents(workspaceId);

            List<AiAgentSummary> summaries = agentDTOs.stream()
                .map(ListAiAgentsToolCallback::toSummary)
                .toList();

            return jsonMapper.writeValueAsString(summaries);
        } catch (JacksonException exception) {
            return toolError("Serialization error: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, ListAiAgentsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static AiAgentSummary toSummary(AiAgentDTO agentDTO) {
        AiAgent agent = agentDTO.agent();

        return new AiAgentSummary(
            agent.getId(), agent.getName(), agent.getTitle(), agent.getDescription(),
            agentDTO.lastPublishedVersion() > 0, agentDTO.lastPublishedVersion());
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record AiAgentSummary(
        Long id, String name, String title, @Nullable String description, boolean published,
        int lastPublishedVersion) {
    }
}
