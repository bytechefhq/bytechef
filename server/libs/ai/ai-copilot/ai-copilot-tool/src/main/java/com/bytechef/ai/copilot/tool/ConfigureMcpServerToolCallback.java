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

package com.bytechef.ai.copilot.tool;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ai.agent.tool.CurrentAgentContext.AgentBinding;
import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolution;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.commons.util.JsonUtils;
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

/**
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the MCP Server tool-mapping subagent to the parent ai_hub
 * agent and the MCP Servers Copilot panel. Given an MCP server id, the subagent reads the workflows already attached to
 * that server and writes each one's tool mapping — {@code toolName}, {@code toolDescription}, and per-input
 * {@code fromAi(...)} expressions — using the two CRUD tools it carries ({@code listMcpProjectWorkflows},
 * {@code updateMcpProjectWorkflowParameters}). It never creates, attaches to, or enables a server.
 *
 * @author Ivica Cardic
 */
public class ConfigureMcpServerToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ConfigureMcpServerToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate synthesizing an MCP server's tool mapping to a specialised subagent. Given an MCP
            server id, it reads every workflow already attached to that server and, for each one, writes
            toolName (short snake_case verb phrase, unique per server), toolDescription (routable by a
            calling LLM), and per-input fromAi(...) expressions synthesized from the workflow's
            inputSchema (a literal value for any input with a fixed, known value). It never creates,
            attaches workflows to, or enables the server — those are separate steps. Pass the numeric
            mcpServerId; instruction is optional guidance on naming or style (e.g. "name them all
            get_*").""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "mcpServerId": {
                        "type": "number",
                        "description": "Numeric id of the MCP server whose attached workflows should be mapped."
                    },
                    "instruction": {
                        "type": "string",
                        "description": "Optional guidance for the mapping (naming convention, style, which workflows to prioritize)."
                    }
                },
                "required": ["mcpServerId"]
            }""";

    private final IntelligentToolChatClientFactory chatClientFactory;
    private final @Nullable SubAgentChatModelResolver chatModelResolver;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ConfigureMcpServerToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver) {

        this.chatClientFactory = chatClientFactory;
        this.chatModelResolver = chatModelResolver;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("configureMcpServer")
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
            ConfigureMcpServerInput input = JsonUtils.read(toolInput, ConfigureMcpServerInput.class);

            Long mcpServerId = input.mcpServerId();

            if (mcpServerId == null) {
                return toolError("mcpServerId is required");
            }

            String request = toRequest(mcpServerId, input.instruction());

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> parentContext = toolContext == null ? Map.of() : toolContext.getContext();

            ChatClient mcpServerChatClient =
                chatClientFactory.get(SubAgentChatModelResolution.resolve(chatModelResolver, parentContext));

            String result = CurrentAgentContext.callWith(
                CopilotAgentType.CONFIGURE_MCP_SERVER, parentAgent,
                () -> mcpServerChatClient.prompt(request)
                    .toolContext(parentContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("configureMcpServer subagent returned null for mcpServerId={}", mcpServerId);

                return ToolErrors.toolError("configureMcpServer subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "configureMcpServer rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                ConfigureMcpServerToolCallback.class, "configureMcpServer", exception);
        }
    }

    private static String toRequest(Long mcpServerId, @Nullable String instruction) {
        String request = "Complete the tool mapping for every workflow already attached to MCP server " +
            mcpServerId + ".";

        if (instruction != null && !instruction.isBlank()) {
            request = request + " " + instruction;
        }

        return request;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record ConfigureMcpServerInput(@Nullable Long mcpServerId, @Nullable String instruction) {
    }
}
