/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenFileTabToolCallback implements ToolCallback {

    static final String FILE_REFERENCED = "FILE_REFERENCED";

    private static final Logger log = LoggerFactory.getLogger(OpenFileTabToolCallback.class);

    private static final String DESCRIPTION = """
        Open a workspace asset file in the AI Hub resource panel so the user can see it.
        Call this after creating a file (via createAssetFile or createBinaryAssetFile) or when referring to an
        existing file. Use the id returned from createAssetFile, createBinaryAssetFile or listAssetFiles - never
        invent file IDs.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "fileId": {"type": "string", "description": "Workspace asset file id"},
                "name": {"type": "string", "description": "Display name for the tab"}
            },
            "required": ["fileId", "name"]
        }""";

    private final AssetFileFacade assetFileFacade;
    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenFileTabToolCallback(
        AssetFileFacade assetFileFacade, @Nullable AiHubChatArtifactRecorder artifactRecorder) {

        this.assetFileFacade = assetFileFacade;
        this.artifactRecorder = artifactRecorder;
    }

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

            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (!isVisible(input.fileId(), invocationContext)) {
                return toolError("File " + input.fileId() + " was not found. Check the file id.");
            }

            recordArtifact(invocationContext, input);

            return writeResult(new OpenFileTabOutput(true, input.fileId(), input.name()));
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, OpenFileTabToolCallback.class, "openFileTab", exception);
        }
    }

    private boolean isVisible(String fileId, @Nullable AiHubToolInvocationContext invocationContext) {
        try {
            long assetFileId = Long.parseLong(fileId);

            if (invocationContext != null && invocationContext.workspaceId() != null) {
                assetFileFacade.findByIdInWorkspace(assetFileId, invocationContext.workspaceId());
            } else {
                assetFileFacade.findById(assetFileId);
            }

            return true;
        } catch (AccessDeniedException | AssetFileNotFoundException | IllegalArgumentException exception) {
            return false;
        }
    }

    private void recordArtifact(@Nullable AiHubToolInvocationContext invocationContext, OpenFileTabInput input) {
        if (artifactRecorder == null || invocationContext == null || invocationContext.threadId() == null) {
            return;
        }

        try {
            artifactRecorder.recordReference(
                invocationContext.threadId(), invocationContext.userId(), FILE_REFERENCED, input.fileId(),
                input.name());
        } catch (RuntimeException exception) {
            log.warn("Failed to record file artifact for openFileTab (fileId={})", input.fileId(), exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    private String writeResult(Object result) {
        try {
            return jsonMapper.writeValueAsString(result);
        } catch (JacksonException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, OpenFileTabToolCallback.class, "openFileTab", exception);
        }
    }

    public record OpenFileTabInput(String fileId, String name) {
    }

    public record OpenFileTabOutput(boolean opened, String fileId, String name) {
    }
}
