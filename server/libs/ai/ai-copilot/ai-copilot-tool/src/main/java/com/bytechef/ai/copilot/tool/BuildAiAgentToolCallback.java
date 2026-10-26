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
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
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
 * @author Ivica Cardic
 */
public class BuildAiAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(BuildAiAgentToolCallback.class);

    private static final String TOOL_NAME = "buildAiAgent";

    private static final String DESCRIPTION =
        """
            Delegate building or editing an AI Agent to a specialised subagent. An AI Agent is a chat-first
            automation that runs on a model instead of a hand-built workflow: instructions, one model with
            its connection, tools, chat memory, sub-agents, knowledge bases, human approval and channels such
            as a schedule. Give a plain-language instruction describing the agent, plus projectId to create a
            new agent in that project or aiAgentId to change an existing agent. The subagent looks up tools
            and connections itself and returns a summary with the agent's id. When it needs a decision it
            returns a numbered question; call this tool again with the original instruction plus the
            answer.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "instruction": {
                        "type": "string",
                        "description": "What the agent should do, in plain language."
                    },
                    "projectId": {
                        "type": "integer",
                        "description": "Id of the project to create a new agent in."
                    },
                    "aiAgentId": {
                        "type": "integer",
                        "description": "Id of an existing agent to change."
                    }
                },
                "required": ["instruction"]
            }""";

    private final IntelligentToolChatClientFactory chatClientFactory;
    private final @Nullable SubAgentChatModelResolver chatModelResolver;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public BuildAiAgentToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver) {

        this.chatClientFactory = chatClientFactory;
        this.chatModelResolver = chatModelResolver;
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
            BuildAiAgentInput input = JsonUtils.read(toolInput, BuildAiAgentInput.class);

            String instruction = input.instruction();

            if (instruction == null || instruction.isBlank()) {
                return ToolErrors.toolError("instruction is required");
            }

            if (input.projectId() != null && input.aiAgentId() != null) {
                return ToolErrors.toolError(
                    "Pass either projectId or aiAgentId: projectId creates a new agent, aiAgentId changes one");
            }

            String request = toRequest(instruction, input.projectId(), input.aiAgentId());

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> parentContext = toolContext == null ? Map.of() : toolContext.getContext();

            ChatClient aiAgentChatClient =
                chatClientFactory.get(SubAgentChatModelResolution.resolve(chatModelResolver, parentContext));

            String result = CurrentAgentContext.callWith(
                CopilotAgentType.BUILD_AI_AGENT, parentAgent,
                () -> aiAgentChatClient.prompt(request)
                    .toolContext(parentContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("buildAiAgent subagent returned null");

                return ToolErrors.toolError("buildAiAgent subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            return ToolErrors.toolError("Invalid tool input: " + exception.getMessage());
        } catch (ChatModelUnavailableException exception) {
            log.warn("buildAiAgent cannot run its sub-agent: {}", exception.getMessage());

            throw exception;
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(BuildAiAgentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static String toRequest(String instruction, @Nullable Long projectId, @Nullable Long aiAgentId) {
        if (aiAgentId != null) {
            return "Update the existing AI Agent with id " + aiAgentId + "; do not create a new one unless asked. " +
                instruction;
        }

        if (projectId != null) {
            return "Create a new AI Agent in project " + projectId + " (pass projectId " + projectId +
                " to createAiAgent). If the project already has an agent with that title, update it instead of " +
                "creating another one: an earlier call may have created it before asking a question. " + instruction;
        }

        return instruction;
    }

    public record BuildAiAgentInput(
        @Nullable String instruction, @Nullable Long projectId, @Nullable Long aiAgentId) {
    }
}
