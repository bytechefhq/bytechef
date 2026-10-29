/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
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
 * Signaling-only Spring AI {@link ToolCallback} that lets the AI Hub agent request a workflow to be opened in the
 * client resource panel. The server-side implementation is a no-op that echoes the arguments back as a JSON result; the
 * AI Hub client subscriber intercepts the tool-call result event and updates the tabs store.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenWorkflowTabToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(OpenWorkflowTabToolCallback.class);

    private static final String DESCRIPTION = """
        Open a workflow in the AI Hub resource panel so the user can see it.
        Call this after creating a workflow or when referring to an existing workflow.
        Use the workflowId, projectId, and projectWorkflowId returned from createWorkflow or
        listWorkflows - never invent workflow IDs, project IDs, or projectWorkflowIds.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "workflowId": {"type": "string", "description": "Workflow id"},
                "projectId": {"type": "string", "description": "Project id that owns the workflow"},
                "projectWorkflowId": {"type": "number", "description": "Project workflow id (from listWorkflows)"},
                "name": {"type": "string", "description": "Display name for the tab"}
            },
            "required": ["workflowId", "projectId", "projectWorkflowId", "name"]
        }""";

    private final JsonMapper jsonMapper = new JsonMapper();
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenWorkflowTabToolCallback(
        ProjectWorkflowFacade projectWorkflowFacade, @Nullable AiHubChatArtifactRecorder artifactRecorder) {

        this.projectWorkflowFacade = projectWorkflowFacade;
        this.artifactRecorder = artifactRecorder;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openWorkflowTab")
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
            OpenWorkflowTabInput input = jsonMapper.readValue(toolInput, OpenWorkflowTabInput.class);

            if (input.workflowId() == null || input.workflowId()
                .isBlank()) {
                return toolError("workflowId is required");
            }

            if (input.projectId() == null || input.projectId()
                .isBlank()) {
                return toolError("projectId is required");
            }

            if (input.projectWorkflowId() == null) {
                return toolError("projectWorkflowId is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            if (!isVisibleInProject(input)) {
                return toolError("Workflow " + input.workflowId() + " was not found. Check the workflow id.");
            }

            recordArtifact(toolContext, input);

            return jsonMapper.writeValueAsString(
                new OpenWorkflowTabOutput(
                    true, input.workflowId(), input.projectId(), input.projectWorkflowId(), input.name()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, OpenWorkflowTabToolCallback.class, "openWorkflowTab", exception);
        }
    }

    private boolean isVisibleInProject(OpenWorkflowTabInput input) {
        try {
            long workflowProjectId = projectWorkflowFacade.getWorkflowProjectId(input.workflowId());

            return String.valueOf(workflowProjectId)
                .equals(input.projectId());
        } catch (AccessDeniedException | IllegalArgumentException exception) {
            return false;
        }
    }

    private void recordArtifact(@Nullable ToolContext toolContext, OpenWorkflowTabInput input) {
        if (artifactRecorder == null) {
            return;
        }

        AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.threadId() == null) {
            return;
        }

        long projectId;

        try {
            projectId = Long.parseLong(input.projectId());
        } catch (NumberFormatException exception) {
            log.warn(
                "openWorkflowTab projectId '{}' is not numeric — skipping artifact record (workflowId={})",
                input.projectId(), input.workflowId());

            return;
        }

        try {
            artifactRecorder.recordWorkflowReference(
                invocationContext.threadId(), invocationContext.userId(), input.workflowId(), projectId,
                input.projectWorkflowId(), input.name());
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to record workflow artifact for openWorkflowTab (workflowId={})", input.workflowId(),
                exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record OpenWorkflowTabInput(String workflowId, String projectId, Long projectWorkflowId, String name) {
    }

    public record OpenWorkflowTabOutput(
        boolean opened, String workflowId, String projectId, Long projectWorkflowId, String name) {
    }
}
