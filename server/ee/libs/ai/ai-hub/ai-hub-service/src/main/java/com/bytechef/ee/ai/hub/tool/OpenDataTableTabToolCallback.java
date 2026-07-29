/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Signaling-only Spring AI {@link ToolCallback} that lets the AI Hub agent request a data table to be opened in the
 * client resource panel. The server-side implementation is a no-op that echoes the arguments back as a JSON result; the
 * AI Hub client subscriber intercepts the tool-call result event and updates the tabs store.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenDataTableTabToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(OpenDataTableTabToolCallback.class);

    private static final String DESCRIPTION = """
        Open a data table in the AI Hub resource panel so the user can see it.
        Call this after creating a data table or when referring to an existing data table.
        Use the dataTableId returned from createDataTable or listDataTables - never invent
        data table IDs.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "dataTableId": {"type": "string", "description": "Data table id"},
                "name": {"type": "string", "description": "Display name for the tab"}
            },
            "required": ["dataTableId", "name"]
        }""";

    private final JsonMapper jsonMapper = new JsonMapper();
    private final @Nullable AiHubTaskArtifactRecorder artifactRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenDataTableTabToolCallback(@Nullable AiHubTaskArtifactRecorder artifactRecorder) {
        this.artifactRecorder = artifactRecorder;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openDataTableTab")
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
            OpenDataTableTabInput input = jsonMapper.readValue(toolInput, OpenDataTableTabInput.class);

            if (input.dataTableId() == null || input.dataTableId()
                .isBlank()) {
                return toolError("dataTableId is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            recordArtifact(toolContext, input);

            return jsonMapper.writeValueAsString(
                new OpenDataTableTabOutput(true, input.dataTableId(), input.name()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, OpenDataTableTabToolCallback.class, "openDataTableTab", exception);
        }
    }

    private void recordArtifact(@Nullable ToolContext toolContext, OpenDataTableTabInput input) {
        if (artifactRecorder == null) {
            return;
        }

        AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.threadId() == null) {
            return;
        }

        try {
            artifactRecorder.recordReference(
                invocationContext.threadId(), invocationContext.userId(), "DATA_TABLE_REFERENCED",
                input.dataTableId(), input.name());
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to record data table artifact for openDataTableTab (dataTableId={})", input.dataTableId(),
                exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record OpenDataTableTabInput(String dataTableId, String name) {
    }

    public record OpenDataTableTabOutput(boolean opened, String dataTableId, String name) {
    }
}
