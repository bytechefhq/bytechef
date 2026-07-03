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
 * Signaling-only Spring AI {@link ToolCallback} that lets the AI Hub agent request a workspace file to be opened in the
 * client resource panel. The server-side implementation is a no-op that echoes the arguments back as a JSON result; the
 * AI Hub client subscriber intercepts the tool-call result event and updates the tabs store.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenFileTabToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        Open a workspace asset file in the AI Hub resource panel so the user can see it.
        Call this after creating a file (via createAssetFile) or when referring to an existing
        file. Use the fileId returned from createAssetFile or listAssetFiles - never invent file
        IDs.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "fileId": {"type": "string", "description": "Workspace asset file id"},
                "name": {"type": "string", "description": "Display name for the tab"}
            },
            "required": ["fileId", "name"]
        }""";

    private final JsonMapper jsonMapper = new JsonMapper();

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openFileTab")
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
            OpenFileTabInput input = jsonMapper.readValue(toolInput, OpenFileTabInput.class);

            if (input.fileId() == null || input.fileId()
                .isBlank()) {
                return toolError("fileId is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            return jsonMapper.writeValueAsString(
                new OpenFileTabOutput(true, input.fileId(), input.name()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, OpenFileTabToolCallback.class, "openFileTab", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record OpenFileTabInput(String fileId, String name) {
    }

    public record OpenFileTabOutput(boolean opened, String fileId, String name) {
    }
}
