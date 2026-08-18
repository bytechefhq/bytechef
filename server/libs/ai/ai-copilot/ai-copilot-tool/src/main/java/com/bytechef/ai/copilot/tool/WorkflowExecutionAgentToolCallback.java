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
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Workflow Execution Copilot subagent to the parent ai_hub
 * agent.
 *
 * <p>
 * When the parent LLM invokes this tool it passes a JSON object with a {@code request} field. The callback delegates to
 * a pre-configured {@link ChatClient} that carries the Workflow Execution system prompt and the Copilot specialist's
 * tool catalog (execution-inspection tools plus the read/write workflow tools per mode). The isolated chat client
 * context means the parent never sees the discovery / mutation transcript — only the synthesised result.
 *
 * @author Ivica Cardic
 */
public class WorkflowExecutionAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionAgentToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a user request about a workflow execution (a past run) to a specialised Workflow Execution
            subagent. Use this to inspect or diagnose a run — why it failed, which task errored, what a task's
            input/output was — and, in BUILD mode, to fix the underlying workflow. Pass the user request verbatim;
            the subagent resolves the execution and does its own analysis. Returns the synthesised analysis (ASK) or
            the applied fix plus rationale (BUILD). Include the workflow execution ID if known; otherwise describe
            the run and the subagent resolves it by listing recent executions.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "request": {
                        "type": "string",
                        "description": "The user request in natural language. Pass through verbatim — the subagent does its own task decomposition."
                    }
                },
                "required": ["request"]
            }""";

    private final IntelligentToolChatClientFactory chatClientFactory;
    private final @Nullable SubAgentChatModelResolver chatModelResolver;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowExecutionAgentToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver) {

        this.chatClientFactory = chatClientFactory;
        this.chatModelResolver = chatModelResolver;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("debugWorkflowExecution")
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
            WorkflowExecutionAgentInput input = JsonUtils.read(toolInput, WorkflowExecutionAgentInput.class);

            if (input.request() == null || input.request()
                .isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> parentContext = toolContext == null ? Map.of() : toolContext.getContext();

            ChatClient workflowExecutionChatClient =
                chatClientFactory.get(SubAgentChatModelResolution.resolve(chatModelResolver, parentContext));

            String result = CurrentAgentContext.callWith(CopilotAgentType.DEBUG_WORKFLOW_EXECUTION, parentAgent,
                () -> workflowExecutionChatClient.prompt(input.request())
                    .toolContext(parentContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn(
                    "workflow_execution subagent returned null for request='{}'",
                    input.request());

                return toolError("workflow_execution subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "debugWorkflowExecution rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                WorkflowExecutionAgentToolCallback.class, "debugWorkflowExecution", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record WorkflowExecutionAgentInput(String request) {
    }
}
