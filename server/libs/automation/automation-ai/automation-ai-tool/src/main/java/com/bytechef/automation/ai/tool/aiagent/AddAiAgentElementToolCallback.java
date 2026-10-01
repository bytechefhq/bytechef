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
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class AddAiAgentElementToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "addAiAgentElement";

    private static final String MODEL_PARAM_MODEL = "model";
    private static final String MODEL_PARAM_PROVIDER = "provider";
    private static final String TOOL_PARAM_ACTION_NAME = "actionName";
    private static final String TOOL_PARAM_COMPONENT_NAME = "componentName";
    private static final String TOOL_PARAM_COMPONENT_VERSION = "componentVersion";

    private static final Set<String> REFERENCE_KINDS = Set.of(
        AiAgentElement.KIND_KNOWLEDGE_BASE, AiAgentElement.KIND_SKILL, AiAgentElement.KIND_SUB_AGENT);

    private static final String DESCRIPTION =
        """
            Add a building-block element to an AI Agent. kind is one of:
            - MODEL: the LLM the agent runs on. Singleton — required before the agent can be published.
              parameters is {"provider": the model component's name such as "anthropic", "model": the model
              id, "parameters": optional model settings such as maxTokens}.
              connectionId is required: the id of a connection to the provider's component (find one with
              listConnectionsForComponent, or create it first); without it the agent cannot run.
            - TOOL: a tool the agent's LLM can call. Find it with searchAiAgentTools first, then pass
              parameters {"componentName", "componentVersion", "actionName"} exactly as it returned them,
              plus an optional "parameters" object of fixed tool inputs and an optional boolean
              "requiresApproval". referenceId is not used; connectionId is the tool component's connection
              when it needs one. A requiresApproval tool runs only after a human approves it, once the agent
              also has an APPROVAL_GATE element; approval requests go out over the agent's own channels.
            - SKILL: referenceId is the skill's id, found by name with searchAiAgentReferences. Multiple
              SKILL elements are aggregated into one skills tool (gated by settings.builtInTools.skills).
            - SUB_AGENT: referenceId is another AiAgent's id from listAiAgents, callable as a tool from this
              agent. Cycles (including self-reference) are rejected.
            - KNOWLEDGE_BASE: referenceId is the knowledge base id, found by name with
              searchAiAgentReferences, wired in as RAG. Repeatable, one per knowledge base.
            - CHAT_MEMORY: gives the agent persistent per-conversation memory. Singleton, no parameters.
            - APPROVAL_GATE: singleton; switches on approval for every requiresApproval TOOL (optional
              parameters approvalExpiresIn integer + approvalExpiresInUnit "HOURS"/"DAYS").
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
                        "enum": ["MODEL", "TOOL", "SKILL", "SUB_AGENT", "KNOWLEDGE_BASE", "CHAT_MEMORY", "APPROVAL_GATE", "APPROVAL_TOOL"],
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

            if (AiAgentElement.KIND_MODEL.equals(input.kind()) && input.connectionId() == null) {
                return toolError(
                    "A MODEL element requires connectionId, the id of a connection to the model provider's " +
                        "component; find one with listConnectionsForComponent or create it first");
            }

            if (AiAgentElement.KIND_MODEL.equals(input.kind()) && !isModelReference(input.parameters())) {
                return toolError(
                    "A MODEL element requires parameters.provider, the model component's name such as " +
                        "\"anthropic\" or \"openAi\", and parameters.model, the model id; put any other model " +
                        "settings in parameters.parameters");
            }

            if (AiAgentElement.KIND_TOOL.equals(input.kind()) && !isToolReference(input.parameters())) {
                return toolError(
                    "A TOOL element requires parameters componentName, componentVersion and actionName; find " +
                        "them with searchAiAgentTools and pass them unchanged");
            }

            if (REFERENCE_KINDS.contains(input.kind()) && input.referenceId() == null) {
                return toolError(
                    "A " + input.kind() + " element requires referenceId; find it with " +
                        (AiAgentElement.KIND_SUB_AGENT.equals(input.kind())
                            ? "listAiAgents" : "searchAiAgentReferences"));
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

    private static boolean isModelReference(@Nullable Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }

        return parameters.get(MODEL_PARAM_PROVIDER) instanceof String provider && !provider.isBlank() &&
            parameters.get(MODEL_PARAM_MODEL) instanceof String model && !model.isBlank();
    }

    private static boolean isToolReference(@Nullable Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }

        return parameters.get(TOOL_PARAM_COMPONENT_NAME) instanceof String componentName && !componentName.isBlank() &&
            parameters.get(TOOL_PARAM_COMPONENT_VERSION) instanceof Number &&
            parameters.get(TOOL_PARAM_ACTION_NAME) instanceof String actionName && !actionName.isBlank();
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
