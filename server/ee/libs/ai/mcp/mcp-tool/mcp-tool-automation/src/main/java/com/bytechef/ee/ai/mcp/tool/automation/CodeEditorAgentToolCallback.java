/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.mcp.tool.automation;

import com.bytechef.ee.ai.mcp.tool.usage.Agent;
import com.bytechef.ee.ai.mcp.tool.usage.CurrentAgentContext;
import com.bytechef.ee.ai.mcp.tool.usage.CurrentAgentContext.AgentBinding;
import com.bytechef.ee.ai.mcp.tool.util.ToolErrors;
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
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Code Editor Copilot subagent to the parent ai_hub agent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeEditorAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CodeEditorAgentToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a user request about embedded script code to a specialised Code Editor subagent.
            Use this for requests that write, edit, debug, or explain JavaScript / Python / Ruby script
            embedded inside a workflow task. The subagent owns the canonical behaviour for this domain;
            prefer calling it over generating script code directly. Returns the updated script (BUILD)
            or an explanation (ASK).""";

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

    private final ChatClient codeEditorChatClient;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CodeEditorAgentToolCallback(ChatClient codeEditorChatClient) {
        this.codeEditorChatClient = codeEditorChatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("code_editor_agent")
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
            CodeEditorAgentInput input = jsonMapper.readValue(toolInput, CodeEditorAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            Agent parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> forwardedContext = toolContext == null ? Map.of() : toolContext.getContext();

            String result = CurrentAgentContext.callWith(Agent.CODE_EDITOR_AGENT, parentAgent,
                () -> codeEditorChatClient.prompt(request)
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("code_editor subagent returned null for request='{}'", request);

                return ToolErrors.toolError(jsonMapper, "code_editor subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "code_editor_agent rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, CodeEditorAgentToolCallback.class, "code_editor_agent", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CodeEditorAgentInput(String request) {
    }
}
