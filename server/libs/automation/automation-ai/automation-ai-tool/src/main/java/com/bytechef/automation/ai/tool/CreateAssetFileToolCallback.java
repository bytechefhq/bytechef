/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.tool;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.commons.util.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @author Ivica Cardic
 */
public class CreateAssetFileToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CreateAssetFileToolCallback.class);

    static final long MAX_CONTENT_BYTES = 64L * 1024L * 1024L;

    private static final int UTF8_MAX_BYTES_PER_CHAR = 4;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "text/markdown", "text/csv", "text/plain", "application/json",
        "text/javascript", "text/x-python", "text/x-java",
        "text/html", "text/css", "text/yaml");

    private static final String DESCRIPTION = """
        Create a new text file in the user's workspace. Use this when the user asks you to
        write, draft, or generate a document, spec, CSV, JSON, markdown note, or code file.
        The file will appear in their Files panel. Choose a filename with an appropriate
        extension. Supported mime types: text/markdown, text/csv, text/plain,
        application/json, text/javascript, text/x-python, text/x-java, text/html, text/css,
        text/yaml.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "filename": {"type": "string", "description": "Filename with extension, e.g. 'spec.md'"},
                "mimeType": {"type": "string", "description": "Mime type; must match extension"},
                "content": {"type": "string", "description": "Full file contents"},
                "description": {"type": "string", "description": "Optional short description"}
            },
            "required": ["filename", "mimeType", "content"]
        }""";

    private final AssetFileFacade facade;
    private final ToolArtifactRecorder artifactRecorder;
    private final long maxContentBytes;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateAssetFileToolCallback(AssetFileFacade facade) {
        this(facade, null, MAX_CONTENT_BYTES);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateAssetFileToolCallback(
        AssetFileFacade facade, ToolArtifactRecorder artifactRecorder) {

        this(facade, artifactRecorder, MAX_CONTENT_BYTES);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    CreateAssetFileToolCallback(
        AssetFileFacade facade,
        @Nullable ToolArtifactRecorder artifactRecorder, long maxContentBytes) {

        this.facade = facade;
        this.artifactRecorder = artifactRecorder;
        this.maxContentBytes = maxContentBytes;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("createAssetFile")
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
            CreateAssetFileInput input = JsonUtils.read(toolInput, CreateAssetFileInput.class);

            if (input.mimeType() == null || !ALLOWED_MIME_TYPES.contains(input.mimeType())) {
                return toolError("Unsupported mime type: %s".formatted(input.mimeType()));
            }

            if (input.content() != null) {
                long estimatedEncodedBytes = (long) input.content()
                    .length() * UTF8_MAX_BYTES_PER_CHAR;

                if (estimatedEncodedBytes > maxContentBytes) {
                    return toolError(
                        "File content too large (~%d bytes, limit %d)".formatted(
                            estimatedEncodedBytes, maxContentBytes));
                }
            }

            AutomationToolInvocationContext invocationContext =
                AutomationToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the Files panel of a workspace.");
            }

            AssetFile created = facade.createFromAi(
                workspaceId,
                AutomationToolInvocationContext.resolveEnvironmentOrDefault(invocationContext),
                input.filename(),
                input.mimeType(),
                input.content(),
                null,
                null,
                invocationContext.sourceOrdinal(),
                invocationContext.lastUserPrompt());

            if (artifactRecorder != null && invocationContext.threadId() != null) {
                artifactRecorder.record(
                    invocationContext.threadId(), invocationContext.userId(), "FILE_CREATED",
                    String.valueOf(created.getId()), created.getName());
            }

            return JsonUtils.write(
                new CreateAssetFileOutput(
                    created.getId(),
                    created.getName(),
                    "/api/automation/internal/asset-files/%d/content".formatted(created.getId()),
                    created.getSizeBytes()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (AssetFileQuotaExceededException exception) {
            return toolError(
                "File exceeds the allowed file size limit of " + exception.getLimit()
                    + " bytes (attempted " + exception.getAttempted() + " bytes)");
        } catch (RuntimeException exception) {
            log.warn("createAssetFile failed: {}", exception.toString(), exception);

            return toolError("createAssetFile failed (" + exception.getClass()
                .getSimpleName() + ")");
        }
    }

    private String toolError(String message) {
        try {
            return JsonUtils.write(Map.of("error", message));
        } catch (JacksonException exception) {
            return "{\"error\":\"serialization failure\"}";
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateAssetFileInput(String filename, String mimeType, String content, String description) {
    }

    public record CreateAssetFileOutput(long id, String name, String downloadUrl, long sizeBytes) {
    }
}
