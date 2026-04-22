/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workspacefile.ai.tool;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.service.WorkspaceFileFacade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Spring AI {@link ToolCallback} that reads back a workspace file's text content so the copilot can reason over a
 * user's existing document. Refuses to return non-text mimes or files larger than 1 MB.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class GetWorkspaceFileContentToolCallback implements ToolCallback {

    private static final long MAX_BYTES = 1_048_576L;

    private static final String DESCRIPTION = """
        Read the text content of a workspace file by id. Only text-based mime types are returned;
        binary files and files larger than 1 MB are refused with an error. Use listWorkspaceFiles
        first to find the file id.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "id": {"type": "integer", "description": "Workspace file id"}
            },
            "required": ["id"]
        }""";

    private final WorkspaceFileFacade facade;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public GetWorkspaceFileContentToolCallback(WorkspaceFileFacade facade) {
        this.facade = facade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("getWorkspaceFileContent")
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        try {
            GetWorkspaceFileContentInput input = objectMapper.readValue(toolInput, GetWorkspaceFileContentInput.class);

            if (input.id() == null) {
                return toolError("Missing required field: id");
            }

            WorkspaceFile file = facade.findById(input.id());

            if (file.getSizeBytes() > MAX_BYTES) {
                return toolError(
                    "File is too large (%d bytes, limit %d)".formatted(file.getSizeBytes(), MAX_BYTES));
            }

            String mimeType = file.getMimeType();

            if (mimeType == null || !isTextMime(mimeType)) {
                return toolError("Unsupported mime type for text read: %s".formatted(mimeType));
            }

            String content;

            try (InputStream stream = facade.downloadContent(input.id())) {
                content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }

            return objectMapper.writeValueAsString(
                new GetWorkspaceFileContentOutput(file.getId(), file.getName(), mimeType, content));
        } catch (IOException exception) {
            return toolError("Failed to read workspace file content: " + exception.getMessage());
        }
    }

    private static boolean isTextMime(String mimeType) {
        return mimeType.startsWith("text/") || mimeType.equals("application/json")
            || mimeType.equals("application/xml");
    }

    private String toolError(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", message));
        } catch (JsonProcessingException exception) {
            return "{\"error\":\"serialization failure\"}";
        }
    }

    public record GetWorkspaceFileContentInput(Long id) {
    }

    public record GetWorkspaceFileContentOutput(long id, String name, String mimeType, String content) {
    }
}
