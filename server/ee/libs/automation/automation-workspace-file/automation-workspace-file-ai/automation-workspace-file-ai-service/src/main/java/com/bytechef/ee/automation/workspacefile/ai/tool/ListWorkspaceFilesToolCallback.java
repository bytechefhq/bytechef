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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Spring AI {@link ToolCallback} that returns a summary list of the current workspace's files (max 50 entries) so the
 * copilot can decide which files to read.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ListWorkspaceFilesToolCallback implements ToolCallback {

    private static final int MAX_RESULTS = 50;

    private static final String DESCRIPTION = """
        List the files currently saved in the user's workspace. Returns up to 50 summary entries
        (id, name, mimeType, sizeBytes, createdDate). Use this before reading a file to find its id.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {}
        }""";

    private final WorkspaceFileFacade facade;
    private final WorkspaceContextProvider context;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListWorkspaceFilesToolCallback(WorkspaceFileFacade facade, WorkspaceContextProvider context) {
        this.facade = facade;
        this.context = context;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("listWorkspaceFiles")
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
            Long workspaceId = resolveWorkspaceId(toolContext);

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the Files panel of a workspace.");
            }

            List<WorkspaceFile> files = facade.findAllByWorkspaceId(workspaceId, null);

            List<WorkspaceFileSummary> summaries = files.stream()
                .limit(MAX_RESULTS)
                .map(file -> new WorkspaceFileSummary(
                    file.getId(), file.getName(), file.getMimeType(), file.getSizeBytes(), file.getCreatedDate()))
                .toList();

            return objectMapper.writeValueAsString(summaries);
        } catch (JsonProcessingException exception) {
            return toolError("Serialization failure: " + exception.getMessage());
        }
    }

    private Long resolveWorkspaceId(@Nullable ToolContext toolContext) {
        WorkspaceInvocationContext fromToolContext = WorkspaceInvocationContext.fromToolContext(toolContext);

        if (fromToolContext != null && fromToolContext.workspaceId() != null) {
            return fromToolContext.workspaceId();
        }

        return context.currentWorkspaceId();
    }

    private String toolError(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", message));
        } catch (JsonProcessingException exception) {
            return "{\"error\":\"serialization failure\"}";
        }
    }

    public record WorkspaceFileSummary(Long id, String name, String mimeType, long sizeBytes, Instant createdDate) {
    }
}
