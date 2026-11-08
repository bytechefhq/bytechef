/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenDataTableTabToolCallback implements ToolCallback {

    static final String DATA_TABLE_REFERENCED = "DATA_TABLE_REFERENCED";

    private static final String TOOL_NAME = "openDataTableTab";

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

    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenDataTableTabToolCallback(@Nullable AiHubChatArtifactRecorder artifactRecorder) {
        this.artifactRecorder = artifactRecorder;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
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
            OpenDataTableTabInput input = JsonUtils.read(toolInput, OpenDataTableTabInput.class);

            if (input.dataTableId() == null || input.dataTableId()
                .isBlank()) {

                return ToolErrors.toolError("dataTableId is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {

                return ToolErrors.toolError("name is required");
            }

            recordArtifact(AiHubToolInvocationContext.fromToolContext(toolContext), input);

            return JsonUtils.write(new OpenDataTableTabOutput(true, input.dataTableId(), input.name()));
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(OpenDataTableTabToolCallback.class, TOOL_NAME, exception);
        }
    }

    private void recordArtifact(
        @Nullable AiHubToolInvocationContext invocationContext, OpenDataTableTabInput input) {

        if (artifactRecorder == null || invocationContext == null || invocationContext.threadId() == null) {
            return;
        }

        try {
            artifactRecorder.recordReference(
                invocationContext.threadId(), invocationContext.userId(), DATA_TABLE_REFERENCED,
                input.dataTableId(), input.name());
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to record data table artifact for openDataTableTab (dataTableId={})", input.dataTableId(),
                exception);
        }
    }

    public record OpenDataTableTabInput(@Nullable String dataTableId, @Nullable String name) {
    }

    public record OpenDataTableTabOutput(boolean opened, String dataTableId, String name) {
    }
}
