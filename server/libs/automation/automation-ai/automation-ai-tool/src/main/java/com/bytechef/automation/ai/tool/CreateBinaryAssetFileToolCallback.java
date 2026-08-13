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
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lets the agent save a base64-encoded binary asset (image, pptx, or other
 * supported binary format) into the user's workspace files. The base64 payload is decoded server-side and persisted via
 * {@link AssetFileFacade#createBinaryFromAi}. Returns a JSON payload describing the saved file, or an error object when
 * the MIME type is unsupported, the base64 is malformed, or a quota has been exceeded.
 *
 *
 * @author Ivica Cardic
 */
public class CreateBinaryAssetFileToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CreateBinaryAssetFileToolCallback.class);

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/png", "image/jpeg", "image/webp", "image/gif",
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    /**
     * Pre-decode upper bound on a single binary upload, in bytes. Without this check the per-file/per-workspace quota
     * would only run after the base64 string is decoded, letting a hallucinating model exhaust JVM heap.
     */
    static final int MAX_BINARY_BYTES = 64 * 1024 * 1024;

    private static final String DESCRIPTION = """
        Save a binary asset (image, PDF, or PowerPoint presentation) into the user's workspace files.
        Use this when the agent has produced an image, pdf, or pptx document that should be stored
        as a workspace asset. Encode the binary content as base64 before calling this tool.
        Supported MIME types: image/png, image/jpeg, image/webp, image/gif, application/pdf,
        application/vnd.openxmlformats-officedocument.presentationml.presentation.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "filename":      {"type": "string", "description": "Filename with extension"},
                    "mimeType":      {"type": "string", "description": "MIME type — one of image/png, image/jpeg, image/webp, image/gif, application/pdf, application/vnd.openxmlformats-officedocument.presentationml.presentation"},
                    "base64Content": {"type": "string", "description": "Base64-encoded binary content"},
                    "description":   {"type": "string", "description": "Optional short description"}
                },
                "required": ["filename", "mimeType", "base64Content"]
            }""";

    private final AssetFileFacade facade;
    private final ToolArtifactRecorder artifactRecorder;
    private final int maxBinaryBytes;
    private final JsonMapper jsonMapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateBinaryAssetFileToolCallback(AssetFileFacade facade) {
        this(facade, null, new JsonMapper());
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateBinaryAssetFileToolCallback(
        AssetFileFacade facade, ToolArtifactRecorder artifactRecorder) {

        this(facade, artifactRecorder, new JsonMapper());
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateBinaryAssetFileToolCallback(
        AssetFileFacade facade, ToolArtifactRecorder artifactRecorder,
        JsonMapper jsonMapper) {

        this(facade, artifactRecorder, jsonMapper, MAX_BINARY_BYTES);
    }

    /**
     * Test-only constructor that overrides the binary upload bound. Production callers must use the public constructors
     * above; the small-bound override is used to exercise the rejection path without allocating multi-megabyte buffers
     * in unit tests.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    CreateBinaryAssetFileToolCallback(
        AssetFileFacade facade, ToolArtifactRecorder artifactRecorder,
        int maxBinaryBytes) {

        this(facade, artifactRecorder, new JsonMapper(), maxBinaryBytes);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    CreateBinaryAssetFileToolCallback(
        AssetFileFacade facade, ToolArtifactRecorder artifactRecorder,
        JsonMapper jsonMapper, int maxBinaryBytes) {

        this.facade = facade;
        this.artifactRecorder = artifactRecorder;
        this.jsonMapper = jsonMapper;
        this.maxBinaryBytes = maxBinaryBytes;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("createBinaryAssetFile")
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
            CreateBinaryAssetFileInput input =
                jsonMapper.readValue(toolInput, CreateBinaryAssetFileInput.class);

            if (input.mimeType() == null || !ALLOWED_MIME_TYPES.contains(input.mimeType())) {
                return toolError("Unsupported mime type: %s".formatted(input.mimeType()));
            }

            if (input.base64Content() == null) {
                return toolError("base64Content is required");
            }

            // Reject oversize payloads BEFORE allocating a decoded byte[]. Each base64 character encodes 6 bits
            // (3 raw bytes per 4 chars), so the decoded length is at most ceil(N * 3 / 4) where N is the input
            // length. We compare estimatedDecodedBytes directly against maxBinaryBytes (instead of inverting the
            // formula) so the bound stays tight against padding artifacts: a legitimately-encoded payload close
            // to the cap won't be rejected just because of trailing `=` padding chars. The cast to long avoids
            // 32-bit overflow when N approaches Integer.MAX_VALUE.
            long inputLength = input.base64Content()
                .length();
            long estimatedDecodedBytes = (inputLength * 3L + 3L) / 4L;

            if (estimatedDecodedBytes > maxBinaryBytes) {
                return toolError(
                    "Binary payload exceeds the maximum allowed size of " + maxBinaryBytes + " bytes");
            }

            byte[] data;

            try {
                data = Base64.getDecoder()
                    .decode(input.base64Content());
            } catch (IllegalArgumentException exception) {
                return toolError("invalid base64: " + exception.getMessage());
            }

            if (data.length > maxBinaryBytes) {
                return toolError(
                    "Binary payload exceeds the maximum allowed size of " + maxBinaryBytes + " bytes");
            }

            AutomationToolInvocationContext invocationContext =
                AutomationToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the Files panel of a workspace.");
            }

            AssetFile created = facade.createBinaryFromAi(
                workspaceId,
                AutomationToolInvocationContext.resolveEnvironmentOrDefault(invocationContext),
                input.filename(),
                input.mimeType(),
                data,
                null,
                null,
                invocationContext.sourceOrdinal(),
                invocationContext.lastUserPrompt());

            if (artifactRecorder != null && invocationContext.threadId() != null) {
                artifactRecorder.record(
                    invocationContext.threadId(), invocationContext.userId(), "BINARY_FILE_CREATED",
                    String.valueOf(created.getId()), created.getName());
            }

            return jsonMapper.writeValueAsString(
                new CreateBinaryAssetFileOutput(
                    created.getId(),
                    created.getName(),
                    "/api/automation/internal/asset-files/%d/content".formatted(created.getId()),
                    created.getSizeBytes()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (AssetFileQuotaExceededException exception) {
            // Caught ahead of the generic RuntimeException handler below so the model learns the actual limit
            // instead of the opaque "createBinaryAssetFile failed (AssetFileQuotaExceededException)" the
            // catch-all would otherwise produce — mirrors CreateAssetFileFromUrlToolCallback's handling.
            return toolError(
                "File exceeds the allowed file size limit of " + exception.getLimit()
                    + " bytes (attempted " + exception.getAttempted() + " bytes)");
        } catch (RuntimeException exception) {
            // Catch-all for transient DB outages, NPEs, file-storage S3 5xx, and any other RuntimeException that
            // would otherwise abort the entire agent run. Log at WARN with the full stack trace, return a typed
            // tool error the agent loop can recover from. The message intentionally does not include
            // exception.getMessage() to avoid leaking internal detail to the LLM/user.
            log.warn("createBinaryAssetFile failed: {}", exception.toString(), exception);

            return toolError("createBinaryAssetFile failed (" + exception.getClass()
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateBinaryAssetFileInput(
        String filename, String mimeType, String base64Content, String description) {
    }

    public record CreateBinaryAssetFileOutput(long id, String name, String downloadUrl, long sizeBytes) {
    }
}
