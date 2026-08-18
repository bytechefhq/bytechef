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
 * Signaling-only Spring AI {@link ToolCallback} that lets the AI Hub agent request a code workflow to be opened in the
 * client resource panel. The server-side implementation is a no-op that echoes the arguments back as a JSON result; the
 * AI Hub client subscriber intercepts the tool-call result event and updates the tabs store.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenCodeWorkflowTabToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(OpenCodeWorkflowTabToolCallback.class);

    private static final String DESCRIPTION = """
        Open a code workflow in the AI Hub resource panel so the user can see it.
        Call this after the buildCodeWorkflow reports a built or changed code workflow, or when referring to
        an existing code workflow.
        Use the projectId, language, and name reported by buildCodeWorkflow - never invent code workflow
        project IDs.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "projectId": {"type": "string", "description": "Code workflow project id"},
                "language": {"type": "string", "description": "Code workflow language: JAVASCRIPT, PYTHON, or RUBY"},
                "name": {"type": "string", "description": "Display name for the tab"}
            },
            "required": ["projectId", "language", "name"]
        }""";

    private final JsonMapper jsonMapper = new JsonMapper();
    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenCodeWorkflowTabToolCallback(@Nullable AiHubChatArtifactRecorder artifactRecorder) {
        this.artifactRecorder = artifactRecorder;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openCodeWorkflowTab")
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
            OpenCodeWorkflowTabInput input = jsonMapper.readValue(toolInput, OpenCodeWorkflowTabInput.class);

            if (input.projectId() == null || input.projectId()
                .isBlank()) {
                return toolError("projectId is required");
            }

            if (input.language() == null || input.language()
                .isBlank()) {
                return toolError("language is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            recordArtifact(toolContext, input);

            return jsonMapper.writeValueAsString(
                new OpenCodeWorkflowTabOutput(true, input.projectId(), input.language(), input.name()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, OpenCodeWorkflowTabToolCallback.class, "openCodeWorkflowTab", exception);
        }
    }

    private void recordArtifact(@Nullable ToolContext toolContext, OpenCodeWorkflowTabInput input) {
        if (artifactRecorder == null) {
            return;
        }

        AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.threadId() == null) {
            return;
        }

        try {
            artifactRecorder.recordReference(
                invocationContext.threadId(), invocationContext.userId(), "CODE_WORKFLOW_REFERENCED",
                input.projectId(), input.name());
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to record code workflow artifact for openCodeWorkflowTab (projectId={})",
                input.projectId(), exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record OpenCodeWorkflowTabInput(String projectId, String language, String name) {
    }

    public record OpenCodeWorkflowTabOutput(boolean opened, String projectId, String language, String name) {
    }
}
