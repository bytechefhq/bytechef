/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import com.bytechef.ee.ai.agent.tool.Agent;
import com.bytechef.ee.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ee.ai.agent.tool.CurrentAgentContext.AgentBinding;
import com.bytechef.ee.ai.agent.tool.ToolErrors;
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
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Workflow Editor Copilot subagent to the parent ai_hub
 * agent.
 *
 * @version ee
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
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowEditorAgentToolCallback(ChatClient workflowEditorChatClient) {
        this.workflowEditorChatClient = workflowEditorChatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("workflow_editor_agent")
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
            WorkflowEditorAgentInput input = jsonMapper.readValue(toolInput, WorkflowEditorAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            Agent parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> forwardedContext = toolContext == null ? Map.of() : toolContext.getContext();

            String result = CurrentAgentContext.callWith(Agent.WORKFLOW_EDITOR_AGENT, parentAgent,
                () -> workflowEditorChatClient.prompt(request)
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("workflow_editor subagent returned null for request='{}'", request);

                return ToolErrors.toolError(jsonMapper, "workflow_editor subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "workflow_editor_agent rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, WorkflowEditorAgentToolCallback.class, "workflow_editor_agent", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record WorkflowEditorAgentInput(String request) {
    }
}
