/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Signaling-only Spring AI {@link ToolCallback} that lets the AI Hub agent navigate the user to a workflow-chat task.
 * Server-side this is a no-op that echoes the {@code threadId} back as the result; the client subscriber intercepts the
 * tool-call event and switches the active task to the matching workflow-chat row in the sidebar.
 *
 * <p>
 * Pairs with {@link CreateWorkflowChatToolCallback}: the LLM creates a workflow chat (or finds an existing one), then
 * fires this tool to surface it. Mirrors the {@link OpenWorkflowTabToolCallback} pattern — both are pure UI hints with
 * no persistent server-side effect.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenWorkflowChatTabToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        Navigate the user to a workflow-chat task. Call this AFTER createWorkflowChat to surface
        the new chat in the AI Hub sidebar — pass the threadId returned from createWorkflowChat.
        Never invent a threadId.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "threadId": {
                    "type": "string",
                    "description": "threadId from createWorkflowChat (a UUID)"
                },
                "title": {
                    "type": "string",
                    "description": "Optional display name for the tab/sidebar entry (defaults to existing title)"
                }
            },
            "required": ["threadId"]
        }""";

    private final JsonMapper jsonMapper = new JsonMapper();

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openWorkflowChatTab")
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
            OpenWorkflowChatTabInput input = jsonMapper.readValue(toolInput, OpenWorkflowChatTabInput.class);

            if (input.threadId() == null || input.threadId()
                .isBlank()) {
                return toolError("threadId is required");
            }

            return jsonMapper.writeValueAsString(
                new OpenWorkflowChatTabOutput(true, input.threadId(), input.title()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, OpenWorkflowChatTabToolCallback.class, "openWorkflowChatTab", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record OpenWorkflowChatTabInput(String threadId, @Nullable String title) {
    }

    public record OpenWorkflowChatTabOutput(boolean opened, String threadId, @Nullable String title) {
    }
}
