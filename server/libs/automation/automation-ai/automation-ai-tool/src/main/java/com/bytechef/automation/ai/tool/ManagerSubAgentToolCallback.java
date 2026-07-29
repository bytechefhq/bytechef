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

package com.bytechef.automation.ai.tool;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ai.agent.tool.CurrentAgentContext.AgentBinding;
import com.bytechef.ai.agent.tool.ToolErrors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hand-rolled Spring AI {@link ToolCallback} that exposes an AI-hub-owned "manager" specialist subagent (MCP servers,
 * personal agents, project deployments, API collections) to the parent ai_hub BUILD agent. The parent passes the user's
 * request verbatim in the {@code request} field; the callback delegates to the specialist's pre-configured
 * {@link ChatClient} and returns only the synthesised response — the parent never sees the specialist's tool
 * transcript.
 *
 * <p>
 * The parent's {@link ToolContext} is forwarded into the specialist so its workspace-scoped tools (which resolve
 * {@link AutomationToolInvocationContext} for workspaceId / environment) keep working. Same mechanics as
 * {@link ResearchToolCallback}; hand-rolled for the same reason — no API exists to register a named subagent backed by
 * an externally-constructed {@link ChatClient}.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class ManagerSubAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ManagerSubAgentToolCallback.class);

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "request": {
                        "type": "string",
                        "description": "The user's request, passed verbatim, plus any ids or user decisions already resolved in this conversation that the specialist needs."
                    }
                },
                "required": ["request"]
            }""";

    private final AgentType agentType;
    private final ChatClient chatClient;
    private final String description;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ManagerSubAgentToolCallback(AgentType agentType, ChatClient chatClient, String description) {
        this.agentType = agentType;
        this.chatClient = chatClient;
        this.description = description;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(agentType.key())
            .description(description)
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
            ManagerSubAgentInput input = jsonMapper.readValue(toolInput, ManagerSubAgentInput.class);

            if (input.request() == null || input.request()
                .isBlank()) {
                return ToolErrors.toolError(jsonMapper, "request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            // Forward the parent's tool context so the specialist's workspace-scoped tools can resolve
            // workspaceId / userId / environmentId — without it every tool inside the specialist fails with
            // "Workspace context unavailable".
            Map<String, Object> forwardedContext = toolContext == null ? Map.of() : toolContext.getContext();

            String response = CurrentAgentContext.callWith(agentType, parentAgent,
                () -> chatClient.prompt(input.request())
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            if (response == null) {
                log.warn("{} subagent returned null for request='{}'", agentType.key(), input.request());

                return ToolErrors.toolError(jsonMapper, agentType.key() + " subagent returned null");
            }

            return response;
        } catch (JacksonException exception) {
            log.warn(
                "{} rejected malformed tool input: {} — first 200 chars of input: {}",
                agentType.key(), exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, ManagerSubAgentToolCallback.class, agentType.key(), exception);
        }
    }

    public record ManagerSubAgentInput(@Nullable String request) {
    }
}
