/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workspacefile.ai.tool;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.exception.WorkspaceFileQuotaExceededException;
import com.bytechef.automation.workspacefile.service.WorkspaceFileFacade;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Spring AI {@link ToolCallback} that lets the copilot save a generated text file into the user's workspace files.
 * Returns a JSON payload describing the saved file (or an error object if the mime type is unsupported or a quota has
 * been exceeded).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CreateWorkspaceFileToolCallback implements ToolCallback {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "text/markdown", "text/csv", "text/plain", "application/json",
        "text/javascript", "text/x-python", "text/x-java",
        "text/html", "text/css", "text/yaml");

    private static final String DESCRIPTION = """
        Create a new text file in the user's workspace. Use this when the user asks you to
        write, draft, or generate a document, spec, CSV, JSON, markdown note, or code file.
        The file will appear in their Files panel. Choose a filename with an appropriate
        extension. Supported mime types: text/markdown, text/csv, text/plain,
        application/json, text/javascript, text/x-python, text/x-java, text/html, text/css,
        text/yaml.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "filename": {"type": "string", "description": "Filename with extension, e.g. 'spec.md'"},
                "mimeType": {"type": "string", "description": "Mime type; must match extension"},
                "content": {"type": "string", "description": "Full file contents"},
                "description": {"type": "string", "description": "Optional short description"}
            },
            "required": ["filename", "mimeType", "content"]
        }""";

    private final WorkspaceFileFacade facade;
    private final WorkspaceContextProvider context;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateWorkspaceFileToolCallback(WorkspaceFileFacade facade, WorkspaceContextProvider context) {
        this.facade = facade;
        this.context = context;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("createWorkspaceFile")
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
            CreateWorkspaceFileInput input = objectMapper.readValue(toolInput, CreateWorkspaceFileInput.class);

            if (input.mimeType() == null || !ALLOWED_MIME_TYPES.contains(input.mimeType())) {
                return toolError("Unsupported mime type: %s".formatted(input.mimeType()));
            }

            WorkspaceInvocationContext invocationContext = resolveContext(toolContext);

            Long workspaceId = invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the Files panel of a workspace.");
            }

            WorkspaceFile created = facade.createFromAi(
                workspaceId,
                input.filename(),
                input.mimeType(),
                input.content(),
                invocationContext.sourceOrdinal(),
                invocationContext.lastUserPrompt());

            return objectMapper.writeValueAsString(
                new CreateWorkspaceFileOutput(
                    created.getId(),
                    created.getName(),
                    "/api/automation/internal/workspace-files/%d/content".formatted(created.getId()),
                    created.getSizeBytes()));
        } catch (WorkspaceFileQuotaExceededException exception) {
            return toolError(exception.getMessage());
        } catch (JsonProcessingException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        }
    }

    private WorkspaceInvocationContext resolveContext(@Nullable ToolContext toolContext) {
        WorkspaceInvocationContext fromToolContext = WorkspaceInvocationContext.fromToolContext(toolContext);

        if (fromToolContext != null) {
            return fromToolContext;
        }

        return new WorkspaceInvocationContext(
            context.currentWorkspaceId(), context.currentSourceOrdinal(), context.lastUserPrompt());
    }

    private String toolError(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", message));
        } catch (JsonProcessingException exception) {
            return "{\"error\":\"serialization failure\"}";
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateWorkspaceFileInput(String filename, String mimeType, String content, String description) {
    }

    public record CreateWorkspaceFileOutput(long id, String name, String downloadUrl, long sizeBytes) {
    }
}
