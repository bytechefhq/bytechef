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
 * Publishes an AI Agent's backing project — the final step of the agent-building playbook. Publishing does NOT deploy
 * the agent; deployment (making a published agent reachable through a real connection/webhook) is a separate,
 * user-driven step on the Agent Deployments page.
 *
 * @author Ivica Cardic
 */
public class PublishAiAgentToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "publishAiAgent";

    private static final String DESCRIPTION =
        """
            Publish an AI Agent. This duplicates the current draft into a new published version and is
            the last step of the build playbook: create -> add a MODEL element -> add channels -> add
            tools/skills/knowledge base -> optionally add HITL approvals -> configure settings -> verify
            behaviour via the test-chat panel -> publish. Publishing is rejected with a specific reason
            when: no MODEL element is present; a channel needing a connection has none; a SUB_AGENT
            element's target agent has never been published itself; a TOOL has requiresApproval=true (or
            an APPROVAL_TOOL element is present) but there is no APPROVAL_CHANNEL element; or
            settings.builtInTools.webSearch is on with webSearchProvider BRAVE or FIRECRAWL and no
            webSearchConnectionId set; or webSearchProvider is NATIVE and the MODEL element's provider
            has no built-in web search (only anthropic does). Always call
            getAiAgent first to check these preconditions and report a clear error to the user rather
            than retrying blindly. Publishing does not deploy the agent — deployment is a separate,
            user-driven action on the Agent Deployments page, outside this tool's scope. Returns
            nextDraftVersion, the version number of the fresh draft created after publish — the
            version that was just published is nextDraftVersion - 1; report that number to the user,
            not nextDraftVersion itself.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "id": {"type": "integer", "description": "AI Agent id"},
                "description": {"type": "string", "description": "Optional description for the published version"}
            },
            "required": ["id"]
        }""";

    private final AiAgentFacade aiAgentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PublishAiAgentToolCallback(AiAgentFacade aiAgentFacade) {
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
            PublishAiAgentInput input = jsonMapper.readValue(toolInput, PublishAiAgentInput.class);

            if (input.id() == null) {
                return toolError("id is required");
            }

            int nextDraftVersion = aiAgentFacade.publishAgent(input.id(), input.description());

            return jsonMapper.writeValueAsString(
                Map.of("published", true, "id", input.id(), "nextDraftVersion", nextDraftVersion));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, PublishAiAgentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record PublishAiAgentInput(Long id, @Nullable String description) {
    }
}
