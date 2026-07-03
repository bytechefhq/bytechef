/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.ee.ai.hub.artifact.ArtifactGeneratorRegistry;
import com.bytechef.ee.ai.hub.artifact.GenerationRequest;
import com.bytechef.ee.ai.hub.artifact.GenerationResult;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Single tool the agent calls to author a file as an artifact. Format-specific behaviour lives in the registered
 * {@link com.bytechef.ee.ai.hub.artifact.ArtifactGenerator} beans; this callback dispatches by the {@code format} field
 * on the input JSON and rewraps the result into a tool-result envelope shaped to drive the existing chat-chip +
 * open-tab UX.
 *
 * <p>
 * Workspace, user, and environment all come from {@link AiHubToolInvocationContext} — never from the tool input. A
 * tampered input that overrode {@code workspaceId} would let the LLM author files into a workspace it cannot reach. The
 * tool input is restricted to the artifact-shaping fields ({@code format}, {@code filename}, {@code content}, optional
 * {@code metadataJson}).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class GenerateArtifactToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "generateArtifact";

    private static final String DESCRIPTION = """
        Author a file artifact (markdown report, code file, csv/json data file, etc.) and persist it
        in the user's workspace. The file appears as a clickable chip in chat and in the Generated tab
        of the Files panel; the user can open it in the file viewer or download it. Use this when the
        user asks for a deliverable they will reference later — not for ephemeral preview text.

        format is one of MARKDOWN, CODE, CSV, JSON, DOCX, PPTX, CHART, IMAGE, HTML. content is the literal
        artifact body for text formats; for binary formats (DOCX/PPTX/CHART/IMAGE) the format-specific
        generator interprets it (typically a JSON spec). metadataJson is optional structured metadata
        the viewer reads back (e.g. chart spec) — leave null when not applicable.

        Use CHART for data visualization. Use HTML only for interactive apps that cannot be expressed as
        a chart, table, or markdown report (e.g. a countdown clock, a small game, a custom interactive
        widget). HTML artifacts must be self-contained: no external script/stylesheet/image references —
        inline everything or use data: URIs.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "format": {
                        "type": "string",
                        "enum": ["MARKDOWN", "CODE", "CSV", "JSON", "DOCX", "PPTX", "CHART", "IMAGE", "HTML"],
                        "description": "Logical format of the artifact"
                    },
                    "filename": {
                        "type": "string",
                        "description": "Display filename. Extension may be auto-appended for some formats."
                    },
                    "content": {
                        "type": "string",
                        "description": "Artifact body. Literal text for MARKDOWN/CODE/CSV/JSON/HTML; format-specific JSON spec for the rest."
                    },
                    "metadataJson": {
                        "type": "string",
                        "description": "Optional structured metadata. Leave null when not applicable."
                    }
                },
                "required": ["format", "filename", "content"]
            }""";

    private final ArtifactGeneratorRegistry artifactGeneratorRegistry;
    private final AiHubTaskService taskService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public GenerateArtifactToolCallback(
        ArtifactGeneratorRegistry artifactGeneratorRegistry, AiHubTaskService taskService) {

        this.artifactGeneratorRegistry = artifactGeneratorRegistry;
        this.taskService = taskService;
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
            GenerateArtifactInput input = jsonMapper.readValue(toolInput, GenerateArtifactInput.class);

            String validationError = validate(input);

            if (validationError != null) {
                return ToolErrors.toolError(jsonMapper, validationError);
            }

            AssetFileFormat format;

            try {
                format = AssetFileFormat.valueOf(input.format());
            } catch (IllegalArgumentException exception) {
                return ToolErrors.toolError(
                    jsonMapper,
                    "Unknown format '" + input.format()
                        + "'. Valid: " + java.util.Arrays.toString(AssetFileFormat.values()));
            }

            AiHubToolInvocationContext invocationContext =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null
                || invocationContext.userId() == null) {

                return ToolErrors.toolError(
                    jsonMapper,
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            Long taskId = resolveTaskId(invocationContext.threadId());

            GenerationRequest request = new GenerationRequest(
                invocationContext.workspaceId(),
                invocationContext.userId(),
                AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext),
                taskId,
                invocationContext.sourceOrdinal() == null ? (short) 0 : invocationContext.sourceOrdinal(),
                invocationContext.lastUserPrompt(),
                input.filename(),
                input.content(),
                input.metadataJson());

            GenerationResult result;

            try {
                result = artifactGeneratorRegistry.generate(format, request);
            } catch (IllegalArgumentException exception) {
                // Two paths land here: (1) no generator registered for the requested format — typically a
                // deployment that didn't pull in the optional generator (e.g. CE missing the EE-only POI ones);
                // (2) the generator's validate() rejected the payload (malformed CSV row, unparseable JSON, etc.).
                // Both surface the same way as a structured tool error so the LLM can recover on the next turn —
                // suggesting a supported format for (1), regenerating the payload for (2).
                return ToolErrors.toolError(jsonMapper, exception.getMessage());
            }

            return jsonMapper.writeValueAsString(new GenerateArtifactOutput(
                result.assetFileId(),
                result.filename(),
                result.format()
                    .name(),
                result.taskLinked()));
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, GenerateArtifactToolCallback.class, TOOL_NAME, exception);
        }
    }

    /**
     * Resolves the task row id for the bound thread. {@code null} is a tolerated state — first-turn races where the
     * apply tool fires before the chat endpoint has created the matching task. The generator still persists the file
     * but skips the AUTHORED join; the caller surfaces this back as {@code taskLinked=false} so the LLM can prompt the
     * user to attach manually if desired.
     */
    private Long resolveTaskId(@Nullable String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return null;
        }

        Optional<AiHubTask> task = taskService.findByThreadId(threadId);

        return task.map(AiHubTask::getId)
            .orElse(null);
    }

    private static String validate(GenerateArtifactInput input) {
        if (input.format() == null || input.format()
            .isBlank()) {
            return "format is required";
        }

        if (input.filename() == null || input.filename()
            .isBlank()) {
            return "filename is required";
        }

        if (input.content() == null) {
            // Empty string is allowed (you can author an empty placeholder); null is not because it indicates the
            // LLM forgot to supply the field rather than chose to leave it blank.
            return "content is required";
        }

        return null;
    }

    public record GenerateArtifactInput(
        String format, String filename, String content, @Nullable String metadataJson) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerateArtifactOutput(
        long assetFileId, String filename, String format, boolean taskLinked) {
    }
}
