/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
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
 * Signaling-only Spring AI {@link ToolCallback} that lets the AI Hub agent request an AI Agent to be opened in the
 * client resource panel. The server checks the agent is visible to the caller and echoes the arguments back as a JSON
 * result; the AI Hub client subscriber intercepts the tool-call result event and updates the tabs store.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenAiAgentTabToolCallback implements ToolCallback {

    static final String AI_AGENT_REFERENCED = "AI_AGENT_REFERENCED";

    private static final Logger log = LoggerFactory.getLogger(OpenAiAgentTabToolCallback.class);

    private static final String DESCRIPTION = """
        Open an AI Agent in the AI Hub resource panel so the user can see and edit its configuration.
        Call this after creating an agent or when referring to an existing one. Use the id returned from
        createAiAgent, listAiAgents or getAiAgent - never invent agent IDs.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "aiAgentId": {"type": "string", "description": "AI Agent id"},
                "name": {"type": "string", "description": "Display name for the tab"}
            },
            "required": ["aiAgentId", "name"]
        }""";

    private final AiAgentFacade aiAgentFacade;
    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenAiAgentTabToolCallback(
        AiAgentFacade aiAgentFacade, @Nullable AiHubChatArtifactRecorder artifactRecorder) {

        this.aiAgentFacade = aiAgentFacade;
        this.artifactRecorder = artifactRecorder;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openAiAgentTab")
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
            OpenAiAgentTabInput input = jsonMapper.readValue(toolInput, OpenAiAgentTabInput.class);

            if (input.aiAgentId() == null || input.aiAgentId()
                .isBlank()) {
                return toolError("aiAgentId is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            if (!isVisible(input.aiAgentId())) {
                return toolError("AI Agent " + input.aiAgentId() + " was not found. Check the agent id.");
            }

            recordArtifact(toolContext, input);

            return jsonMapper.writeValueAsString(new OpenAiAgentTabOutput(true, input.aiAgentId(), input.name()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, OpenAiAgentTabToolCallback.class, "openAiAgentTab", exception);
        }
    }

    private boolean isVisible(String aiAgentId) {
        try {
            aiAgentFacade.getAgent(Long.parseLong(aiAgentId));

            return true;
        } catch (AccessDeniedException | IllegalArgumentException exception) {
            return false;
        }
    }

    private void recordArtifact(@Nullable ToolContext toolContext, OpenAiAgentTabInput input) {
        if (artifactRecorder == null) {
            return;
        }

        AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.threadId() == null) {
            return;
        }

        try {
            artifactRecorder.recordReference(
                invocationContext.threadId(), invocationContext.userId(), AI_AGENT_REFERENCED, input.aiAgentId(),
                input.name());
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to record AI Agent artifact for openAiAgentTab (aiAgentId={})", input.aiAgentId(), exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record OpenAiAgentTabInput(String aiAgentId, String name) {
    }

    public record OpenAiAgentTabOutput(boolean opened, String aiAgentId, String name) {
    }
}
