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
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
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
 * Adds a building-block element (model, tool, skill, sub-agent, knowledge base, chat memory, or an HITL approval
 * primitive) to an AI Agent.
 *
 * @author Ivica Cardic
 */
public class AddAiAgentElementToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "addAiAgentElement";

    private static final String DESCRIPTION =
        """
            Add a building-block element to an AI Agent. kind is one of:
            - MODEL: the LLM the agent runs on. Singleton — required before the agent can be published.
              parameters carries the model's own configured properties (provider/model name etc.).
            - TOOL: a component action the agent's LLM can call. referenceId is not used; parameters
              identifies the component action plus an optional boolean requiresApproval — when true, the
              tool is nested inside a generated human-in-the-loop approval gate (needs at least one
              APPROVAL_CHANNEL element to publish).
            - SKILL: referenceId is the skill's id. Multiple SKILL elements are aggregated into one
              skills tool (gated by settings.builtInTools.skills).
            - SUB_AGENT: referenceId is another AiAgent's id, callable as a tool from this agent. Cycles
              (including self-reference) are rejected.
            - KNOWLEDGE_BASE: referenceId is the knowledge base id, wired in as RAG. Singleton.
            - CHAT_MEMORY: gives the agent persistent per-conversation memory. Singleton, no parameters.
            - APPROVAL_GATE: singleton; configures the generated approval gate wrapping every
              requiresApproval TOOL (optional parameters approvalExpiresIn integer + approvalExpiresInUnit
              "HOURS"/"DAYS").
            - APPROVAL_CHANNEL: repeatable; parameters requires componentName + componentVersion
              (e.g. "chat"/1, "slack"/1) and optional elementName, describing where approval requests are
              delivered.
            - APPROVAL_TOOL: singleton, no parameters; gives the LLM its own "ask a human" tool, separate
              from the gate.
            Returns the new element's id and kind.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "agentId": {"type": "integer", "description": "AI Agent id"},
                    "kind": {
                        "type": "string",
                        "enum": ["MODEL", "TOOL", "SKILL", "SUB_AGENT", "KNOWLEDGE_BASE", "CHAT_MEMORY", "APPROVAL_GATE", "APPROVAL_CHANNEL", "APPROVAL_TOOL"],
                        "description": "Element kind to add"
                    },
                    "referenceId": {"type": "integer", "description": "Referenced entity id (skill id for SKILL, agent id for SUB_AGENT, knowledge base id for KNOWLEDGE_BASE)"},
                    "parameters": {"type": "object", "description": "Element-specific configuration"},
                    "connectionId": {"type": "integer", "description": "Connection id, when the element needs one"}
                },
                "required": ["agentId", "kind"]
            }""";

    private final AiAgentFacade aiAgentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AddAiAgentElementToolCallback(AiAgentFacade aiAgentFacade) {
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
            AddAiAgentElementInput input = jsonMapper.readValue(toolInput, AddAiAgentElementInput.class);

            if (input.agentId() == null) {
                return toolError("agentId is required");
            }

            if (input.kind() == null || input.kind()
                .isBlank()) {

                return toolError("kind is required");
            }

            AiAgentElement element = aiAgentFacade.addAgentElement(
                input.agentId(), input.kind(), input.referenceId(), input.parameters(), input.connectionId());

            return jsonMapper.writeValueAsString(
                Map.of("created", true, "id", element.getId(), "kind", element.getKind()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, AddAiAgentElementToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record AddAiAgentElementInput(
        Long agentId, String kind, @Nullable Long referenceId, @Nullable Map<String, Object> parameters,
        @Nullable Long connectionId) {
    }
}
