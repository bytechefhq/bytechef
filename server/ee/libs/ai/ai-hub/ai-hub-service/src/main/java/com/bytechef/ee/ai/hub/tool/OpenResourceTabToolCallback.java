/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Signaling-only Spring AI {@link ToolCallback} that consolidates the per-resource open-tab tools ({@code openFileTab},
 * {@code openWorkflowTab}, {@code openDataTableTab}, {@code openKnowledgeBaseTab}, {@code openSkillTab},
 * {@code openCustomComponentTab}, {@code openCodeWorkflowTab}, {@code openAiAgentTab}) into a single {@code type}-keyed
 * tool, trimming the pinned tool list per agent. The server-side implementation echoes the arguments back as a JSON
 * result carrying the {@code type} discriminator plus the exact legacy field names, so the AI Hub client re-dispatches
 * onto the same per-type validators and tab-store calls the legacy tools used.
 *
 * <p>
 * {@code openWorkflowChatTab} is NOT folded in — its result drives a chat switch (thread + navigation), a different
 * contract. The legacy per-type classes also remain for subagent-internal registrations (e.g. the data_analyst
 * specialist's {@code openDataTableTab}).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenResourceTabToolCallback implements ToolCallback {

    static final String TOOL_NAME = "openResourceTab";

    private static final Logger log = LoggerFactory.getLogger(OpenResourceTabToolCallback.class);

    private static final String DESCRIPTION = """
        Open a workspace resource in the AI Hub resource panel so the user can see it. Call this after
        creating a resource or when referring to an existing one. Supply type, name (tab display name),
        and the type's id fields — always ids returned from listing/create tools, never invented:
        FILE: fileId. WORKFLOW: workflowId, projectId, projectWorkflowId (all three, from the build/list
        result). DATA_TABLE: dataTableId. KNOWLEDGE_BASE: knowledgeBaseId. SKILL: skillId.
        CUSTOM_COMPONENT: customComponentId. CODE_WORKFLOW: projectId, language. AI_AGENT: aiAgentId.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "type": {
                        "type": "string",
                        "enum": ["FILE", "WORKFLOW", "DATA_TABLE", "KNOWLEDGE_BASE", "SKILL", "CUSTOM_COMPONENT", "CODE_WORKFLOW", "AI_AGENT"],
                        "description": "The resource kind to open"
                    },
                    "name": {"type": "string", "description": "Display name for the tab"},
                    "fileId": {"type": "string", "description": "FILE: workspace asset file id"},
                    "workflowId": {"type": "string", "description": "WORKFLOW: workflow id"},
                    "projectId": {"type": "string", "description": "WORKFLOW / CODE_WORKFLOW: owning project id"},
                    "projectWorkflowId": {"type": "number", "description": "WORKFLOW: project workflow id (from listWorkflows)"},
                    "dataTableId": {"type": "string", "description": "DATA_TABLE: data table id"},
                    "knowledgeBaseId": {"type": "string", "description": "KNOWLEDGE_BASE: knowledge base id"},
                    "skillId": {"type": "string", "description": "SKILL: skill id"},
                    "customComponentId": {"type": "string", "description": "CUSTOM_COMPONENT: custom component id"},
                    "language": {"type": "string", "description": "CODE_WORKFLOW: script language"},
                    "aiAgentId": {"type": "string", "description": "AI_AGENT: AI Agent id"}
                },
                "required": ["type", "name"]
            }""";

    private final JsonMapper jsonMapper = new JsonMapper();
    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenResourceTabToolCallback(@Nullable AiHubChatArtifactRecorder artifactRecorder) {
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
            OpenResourceTabInput input = jsonMapper.readValue(toolInput, OpenResourceTabInput.class);

            if (input.type() == null || input.type()
                .isBlank()) {
                return toolError("type is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            return switch (input.type()) {
                case "FILE" -> openSimpleResource(input, "fileId", input.fileId(), null, null);
                case "WORKFLOW" -> openWorkflow(input, toolContext);
                case "DATA_TABLE" -> openSimpleResource(
                    input, "dataTableId", input.dataTableId(), "DATA_TABLE_REFERENCED", toolContext);
                case "KNOWLEDGE_BASE" -> openSimpleResource(
                    input, "knowledgeBaseId", input.knowledgeBaseId(), "KB_REFERENCED", toolContext);
                case "SKILL" -> openSimpleResource(input, "skillId", input.skillId(), "SKILL_REFERENCED", toolContext);
                case "CUSTOM_COMPONENT" -> openSimpleResource(
                    input, "customComponentId", input.customComponentId(), "CUSTOM_COMPONENT_REFERENCED", toolContext);
                case "CODE_WORKFLOW" -> openCodeWorkflow(input, toolContext);
                case "AI_AGENT" -> openSimpleResource(
                    input, "aiAgentId", input.aiAgentId(), "AI_AGENT_REFERENCED", toolContext);
                default -> toolError(
                    "Unknown type '" + input.type() + "'. Supported: FILE, WORKFLOW, DATA_TABLE, KNOWLEDGE_BASE, " +
                        "SKILL, CUSTOM_COMPONENT, CODE_WORKFLOW, AI_AGENT");
            };
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, OpenResourceTabToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String openSimpleResource(
        OpenResourceTabInput input, String idFieldName, @Nullable String idValue, @Nullable String referenceKind,
        @Nullable ToolContext toolContext) {

        if (idValue == null || idValue.isBlank()) {
            return toolError(idFieldName + " is required for type " + input.type());
        }

        if (referenceKind != null) {
            recordReference(toolContext, referenceKind, idValue, input.name());
        }

        Map<String, Object> output = baseOutput(input.type(), input.name());

        output.put(idFieldName, idValue);

        return writeOutput(output);
    }

    private String openWorkflow(OpenResourceTabInput input, @Nullable ToolContext toolContext) {
        String workflowId = input.workflowId();

        if (workflowId == null || workflowId.isBlank()) {
            return toolError("workflowId is required for type WORKFLOW");
        }

        String projectId = input.projectId();

        if (projectId == null || projectId.isBlank()) {
            return toolError("projectId is required for type WORKFLOW");
        }

        if (input.projectWorkflowId() == null) {
            return toolError("projectWorkflowId is required for type WORKFLOW");
        }

        recordWorkflowReference(toolContext, input);

        Map<String, Object> output = baseOutput(input.type(), input.name());

        output.put("workflowId", workflowId);
        output.put("projectId", projectId);
        output.put("projectWorkflowId", input.projectWorkflowId());

        return writeOutput(output);
    }

    private String openCodeWorkflow(OpenResourceTabInput input, @Nullable ToolContext toolContext) {
        String projectId = input.projectId();

        if (projectId == null || projectId.isBlank()) {
            return toolError("projectId is required for type CODE_WORKFLOW");
        }

        String language = input.language();

        if (language == null || language.isBlank()) {
            return toolError("language is required for type CODE_WORKFLOW");
        }

        recordReference(toolContext, "CODE_WORKFLOW_REFERENCED", projectId, input.name());

        Map<String, Object> output = baseOutput(input.type(), input.name());

        output.put("projectId", projectId);
        output.put("language", language);

        return writeOutput(output);
    }

    private Map<String, Object> baseOutput(String type, String name) {
        Map<String, Object> output = new LinkedHashMap<>();

        output.put("opened", true);
        output.put("type", type);
        output.put("name", name);

        return output;
    }

    private void recordReference(
        @Nullable ToolContext toolContext, String referenceKind, String resourceId, String resourceName) {

        if (artifactRecorder == null) {
            return;
        }

        AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.threadId() == null) {
            return;
        }

        try {
            artifactRecorder.recordReference(
                invocationContext.threadId(), invocationContext.userId(), referenceKind, resourceId, resourceName);
        } catch (RuntimeException exception) {
            // Best-effort: the open-tab signal must still succeed even if artifact persistence fails.
            log.warn(
                "Failed to record {} artifact for openResourceTab (resourceId={})", referenceKind, resourceId,
                exception);
        }
    }

    private void recordWorkflowReference(@Nullable ToolContext toolContext, OpenResourceTabInput input) {
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
                "openResourceTab projectId '{}' is not numeric — skipping artifact record (workflowId={})",
                input.projectId(), input.workflowId());

            return;
        }

        try {
            // Route through the dedup-aware recorder so a referenced workflow collapses onto an existing
            // created/updated row for the same (chat, workflowId) instead of producing a duplicate sidebar entry.
            artifactRecorder.recordWorkflowReference(
                invocationContext.threadId(), invocationContext.userId(), input.workflowId(), projectId,
                input.projectWorkflowId(), input.name());
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to record workflow artifact for openResourceTab (workflowId={})", input.workflowId(),
                exception);
        }
    }

    private String writeOutput(Map<String, Object> output) {
        return jsonMapper.writeValueAsString(output);
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record OpenResourceTabInput(
        String type, String name, @Nullable String fileId, @Nullable String workflowId, @Nullable String projectId,
        @Nullable Long projectWorkflowId, @Nullable String dataTableId, @Nullable String knowledgeBaseId,
        @Nullable String skillId, @Nullable String customComponentId, @Nullable String language,
        @Nullable String aiAgentId) {
    }
}
