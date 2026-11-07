/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.NoSuchElementException;
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
 * @author Ivica Cardic
 */
public class OpenCustomComponentTabToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(OpenCustomComponentTabToolCallback.class);

    private static final String DESCRIPTION = """
        Open a custom component in the AI Hub resource panel so the user can see it.
        Call this after creating a custom component or when referring to an existing custom component.
        Use the custom component id returned from createCustomComponent or listCustomComponents - never invent
        custom component IDs.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "customComponentId": {"type": "string", "description": "Custom component id"},
                "name": {"type": "string", "description": "Display name for the tab"}
            },
            "required": ["customComponentId", "name"]
        }""";

    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;
    private final CustomComponentFacade customComponentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenCustomComponentTabToolCallback(
        CustomComponentFacade customComponentFacade, @Nullable AiHubChatArtifactRecorder artifactRecorder) {

        this.artifactRecorder = artifactRecorder;
        this.customComponentFacade = customComponentFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openCustomComponentTab")
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
            OpenCustomComponentTabInput input = jsonMapper.readValue(toolInput, OpenCustomComponentTabInput.class);

            if (input.customComponentId() == null || input.customComponentId()
                .isBlank()) {
                return toolError("customComponentId is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            if (!isVisible(input.customComponentId())) {
                return toolError(
                    "Custom component " + input.customComponentId() + " was not found. Check the custom component id.");
            }

            recordArtifact(toolContext, input);

            return jsonMapper.writeValueAsString(
                new OpenCustomComponentTabOutput(true, input.customComponentId(), input.name()));
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, OpenCustomComponentTabToolCallback.class, "openCustomComponentTab", exception);
        }
    }

    private boolean isVisible(String customComponentId) {
        try {
            customComponentFacade.getCustomComponent(Long.parseLong(customComponentId));

            return true;
        } catch (AccessDeniedException | IllegalArgumentException | NoSuchElementException exception) {
            return false;
        }
    }

    private void recordArtifact(@Nullable ToolContext toolContext, OpenCustomComponentTabInput input) {
        if (artifactRecorder == null) {
            return;
        }

        AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.threadId() == null) {
            return;
        }

        try {
            artifactRecorder.recordReference(
                invocationContext.threadId(), invocationContext.userId(), "CUSTOM_COMPONENT_REFERENCED",
                input.customComponentId(), input.name());
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to record custom component artifact for openCustomComponentTab (customComponentId={})",
                input.customComponentId(), exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record OpenCustomComponentTabInput(String customComponentId, String name) {
    }

    public record OpenCustomComponentTabOutput(boolean opened, String customComponentId, String name) {
    }
}
