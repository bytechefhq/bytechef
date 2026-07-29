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
import com.bytechef.ai.copilot.tool.util.WorkflowPersistCaptureUtils;
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
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Workflow Editor Copilot subagent to the parent ai_hub
 * agent.
 *
 * @author Ivica Cardic
 */
public class WorkflowEditorAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEditorAgentToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a user request about whole workflows to a specialised Workflow Editor subagent. Use
            this for requests that design, edit, debug, or explain a workflow (orchestration of tasks,
            triggers, conditions, loops). The subagent owns the canonical behaviour for this domain;
            prefer calling it over reasoning about workflow shape directly. ASK mode returns analysis;
            BUILD mode returns the updated workflow JSON plus a change rationale.""";

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

    private final ChatClient workflowEditorChatClient;
    private final String toolName;
    private final String description;
    private final CopilotAgentType agentType;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowEditorAgentToolCallback(ChatClient workflowEditorChatClient) {
        this(workflowEditorChatClient, "workflow_editor_agent", DESCRIPTION, CopilotAgentType.WORKFLOW_EDITOR_AGENT);
    }

    /**
     * Variant constructor for domain-specific workflow editors (e.g. the embedded integration-workflow editor), letting
     * the parent agent expose a distinct tool name/description and bind the correct agent type for the subagent call.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowEditorAgentToolCallback(
        ChatClient workflowEditorChatClient, String toolName, String description, CopilotAgentType agentType) {

        this.workflowEditorChatClient = workflowEditorChatClient;
        this.toolName = toolName;
        this.description = description;
        this.agentType = agentType;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(toolName)
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
            WorkflowEditorAgentInput input = JsonUtils.read(toolInput, WorkflowEditorAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> parentContext = toolContext == null ? Map.of() : toolContext.getContext();

            Map<String, Object> forwardedContext = WorkflowPersistCaptureUtils.withCaptureHolder(parentContext);

            String result = CurrentAgentContext.callWith(
                agentType, parentAgent,
                () -> workflowEditorChatClient.prompt(request)
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("workflow_editor subagent returned null for request='{}'", request);

                return ToolErrors.toolError("workflow_editor subagent returned null");
            }

            String trailer = WorkflowPersistCaptureUtils.renderTrailer(forwardedContext);

            return trailer == null ? result : result + trailer;
        } catch (JacksonException exception) {
            log.warn(
                "workflow_editor_agent rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                WorkflowEditorAgentToolCallback.class, "workflow_editor_agent", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record WorkflowEditorAgentInput(String request) {
    }
}
