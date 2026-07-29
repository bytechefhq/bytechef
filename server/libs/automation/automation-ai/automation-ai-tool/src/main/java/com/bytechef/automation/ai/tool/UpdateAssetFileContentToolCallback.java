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
import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
 * Spring AI {@link ToolCallback} that replaces the content of an existing text asset file in the user's workspace — the
 * edit-in-place counterpart to {@code createAssetFile}. The file's ownership is gated through
 * {@link AssetFileFacade#findByIdInWorkspace(Long, Long)} so a file id from another workspace cannot be written to.
 *
 *
 * @author Ivica Cardic
 */
public class UpdateAssetFileContentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(UpdateAssetFileContentToolCallback.class);

    /**
     * Hard cap on the UTF-8 byte length of the {@code content} field, mirroring
     * {@code CreateAssetFileToolCallback.MAX_CONTENT_BYTES}.
     */
    static final long MAX_CONTENT_BYTES = 64L * 1024L * 1024L;

    private static final int UTF8_MAX_BYTES_PER_CHAR = 4;

    private static final String DESCRIPTION = """
        Replace the full content of an existing text file in the user's workspace. Use this when the
        user asks you to edit, revise, or rewrite a file that already exists (get the id from
        listAssetFiles and the current content from getAssetFileContent first). Supply the complete
        new content — this overwrites the file. For a brand-new file use createAssetFile instead.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "assetFileId": {"type": "number", "description": "Id of the file to update, from listAssetFiles"},
                    "content": {"type": "string", "description": "Complete new file contents (replaces the old contents)"},
                    "mimeType": {"type": "string", "description": "Optional mime type; defaults to the file's current mime type"}
                },
                "required": ["assetFileId", "content"]
            }""";

    private final AssetFileFacade facade;
    private final @Nullable ToolArtifactRecorder artifactRecorder;
    private final long maxContentBytes;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UpdateAssetFileContentToolCallback(
        AssetFileFacade facade, @Nullable ToolArtifactRecorder artifactRecorder) {

        this(facade, artifactRecorder, MAX_CONTENT_BYTES);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    UpdateAssetFileContentToolCallback(
        AssetFileFacade facade, @Nullable ToolArtifactRecorder artifactRecorder, long maxContentBytes) {

        this.facade = facade;
        this.artifactRecorder = artifactRecorder;
        this.maxContentBytes = maxContentBytes;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("updateAssetFileContent")
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
            UpdateAssetFileContentInput input = jsonMapper.readValue(toolInput, UpdateAssetFileContentInput.class);

            if (input.assetFileId() == null) {
                return toolError("assetFileId is required");
            }

            if (input.content() == null) {
                return toolError("content is required");
            }

            // Same pre-encode allocation cap as createAssetFile: reject a runaway multi-GB payload before
            // getBytes(UTF_8) can allocate it.
            long estimatedEncodedBytes = (long) input.content()
                .length() * UTF8_MAX_BYTES_PER_CHAR;

            if (estimatedEncodedBytes > maxContentBytes) {
                return toolError(
                    "File content too large (~%d bytes, limit %d)".formatted(estimatedEncodedBytes, maxContentBytes));
            }

            AutomationToolInvocationContext invocationContext =
                AutomationToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the Files panel of a workspace.");
            }

            AssetFile existingAssetFile;

            try {
                existingAssetFile = facade.findByIdInWorkspace(input.assetFileId(), workspaceId);
            } catch (AssetFileNotFoundException exception) {
                return toolError("File %d not found in the current workspace.".formatted(input.assetFileId()));
            }

            String contentType = input.mimeType() != null && !input.mimeType()
                .isBlank() ? input.mimeType() : existingAssetFile.getMimeType();

            byte[] contentBytes = input.content()
                .getBytes(StandardCharsets.UTF_8);

            AssetFile updated = facade.updateContent(
                input.assetFileId(), contentType, new ByteArrayInputStream(contentBytes));

            if (artifactRecorder != null && invocationContext.threadId() != null) {
                artifactRecorder.record(
                    invocationContext.threadId(), invocationContext.userId(), "FILE_UPDATED",
                    String.valueOf(updated.getId()), updated.getName());
            }

            return jsonMapper.writeValueAsString(
                new UpdateAssetFileContentOutput(updated.getId(), updated.getName(), updated.getSizeBytes()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            // Same recovery contract as createAssetFile: log with the stack, return a typed tool error without
            // leaking internal detail to the LLM/user.
            log.warn("updateAssetFileContent failed: {}", exception.toString(), exception);

            return toolError("updateAssetFileContent failed (" + exception.getClass()
                .getSimpleName() + ")");
        }
    }

    private String toolError(String message) {
        try {
            return jsonMapper.writeValueAsString(Map.of("error", message));
        } catch (JacksonException exception) {
            return "{\"error\":\"serialization failure\"}";
        }
    }

    public record UpdateAssetFileContentInput(Long assetFileId, String content, @Nullable String mimeType) {
    }

    public record UpdateAssetFileContentOutput(long id, String name, long sizeBytes) {
    }
}
