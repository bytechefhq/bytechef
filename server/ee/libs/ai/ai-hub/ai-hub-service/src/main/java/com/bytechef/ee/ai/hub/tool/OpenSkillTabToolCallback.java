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
 * Signaling-only Spring AI {@link ToolCallback} that lets the AI Hub agent request a skill to be opened in the client
 * resource panel. The server-side implementation is a no-op that echoes the arguments back as a JSON result; the AI Hub
 * client subscriber intercepts the tool-call result event and updates the tabs store.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenSkillTabToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(OpenSkillTabToolCallback.class);

    private static final String DESCRIPTION = """
        Open an AI skill in the AI Hub resource panel so the user can see it.
        Call this after creating a skill or when referring to an existing skill.
        Use the skill id returned from createAiSkill or getAiSkills - never invent skill IDs.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "skillId": {"type": "string", "description": "Skill id"},
                "name": {"type": "string", "description": "Display name for the tab"}
            },
            "required": ["skillId", "name"]
        }""";

    private final JsonMapper jsonMapper = new JsonMapper();
    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenSkillTabToolCallback(@Nullable AiHubChatArtifactRecorder artifactRecorder) {
        this.artifactRecorder = artifactRecorder;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openSkillTab")
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
            OpenSkillTabInput input = jsonMapper.readValue(toolInput, OpenSkillTabInput.class);

            if (input.skillId() == null || input.skillId()
                .isBlank()) {
                return toolError("skillId is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            recordArtifact(toolContext, input);

            return jsonMapper.writeValueAsString(new OpenSkillTabOutput(true, input.skillId(), input.name()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, OpenSkillTabToolCallback.class, "openSkillTab", exception);
        }
    }

    private void recordArtifact(@Nullable ToolContext toolContext, OpenSkillTabInput input) {
        if (artifactRecorder == null) {
            return;
        }

        AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.threadId() == null) {
            return;
        }

        try {
            artifactRecorder.recordReference(
                invocationContext.threadId(), invocationContext.userId(), "SKILL_REFERENCED", input.skillId(),
                input.name());
        } catch (RuntimeException exception) {
            log.warn("Failed to record skill artifact for openSkillTab (skillId={})", input.skillId(), exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record OpenSkillTabInput(String skillId, String name) {
    }

    public record OpenSkillTabOutput(boolean opened, String skillId, String name) {
    }
}
