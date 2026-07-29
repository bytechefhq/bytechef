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
import java.io.IOException;
import java.io.InputStream;
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
 * Spring AI {@link ToolCallback} that reads back a workspace file's text content so the agent can reason over a user's
 * existing document. Refuses to return non-text mimes or files larger than 1 MB.
 *
 *
 * @author Ivica Cardic
 */
public class GetAssetFileContentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(GetAssetFileContentToolCallback.class);

    private static final long MAX_BYTES = 1_048_576L;

    private static final String DESCRIPTION = """
        Read the text content of a workspace file by id. Only text-based mime types are returned;
        binary files and files larger than 1 MB are refused with an error. Use listAssetFiles
        first to find the file id.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "id": {"type": "integer", "description": "Workspace file id"}
            },
            "required": ["id"]
        }""";

    private final AssetFileFacade facade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public GetAssetFileContentToolCallback(AssetFileFacade facade) {
        this.facade = facade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("getAssetFileContent")
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
            GetAssetFileContentInput input = jsonMapper.readValue(toolInput, GetAssetFileContentInput.class);

            if (input.id() == null) {
                return toolError("Missing required field: id");
            }

            AutomationToolInvocationContext invocationContext =
                AutomationToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError("No workspace context available for the current invocation");
            }

            AssetFile file;

            try {
                file = facade.findByIdInWorkspace(input.id(), workspaceId);
            } catch (AssetFileNotFoundException | IllegalArgumentException exception) {
                // Includes both "not found" and "wrong workspace" — the facade collapses these by design so the
                // tool cannot be used as a 404-vs-403 oracle to enumerate cross-workspace file ids. IAE is kept for
                // null/blank-arg validation; AssetFileNotFoundException is thrown for the actual lookup failure.
                return toolError(exception.getMessage());
            }

            if (file.getSizeBytes() > MAX_BYTES) {
                return toolError(
                    "File is too large (%d bytes, limit %d)".formatted(file.getSizeBytes(), MAX_BYTES));
            }

            String mimeType = file.getMimeType();

            if (mimeType == null || !isTextMime(mimeType)) {
                return toolError("Unsupported mime type for text read: %s".formatted(mimeType));
            }

            String content;

            try (InputStream stream = facade.downloadContent(input.id())) {
                content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }

            return jsonMapper.writeValueAsString(
                new GetAssetFileContentOutput(file.getId(), file.getName(), mimeType, content));
        } catch (IOException exception) {
            return toolError("Failed to read workspace file content: " + exception.getMessage());
        } catch (RuntimeException exception) {
            // Catch-all for transient DB outages, NPEs, file-storage S3 5xx, and any other RuntimeException that
            // would otherwise abort the entire agent run. Log at WARN with the full stack trace, return a typed
            // tool error the agent loop can recover from. The message intentionally does not include
            // exception.getMessage() to avoid leaking internal detail to the LLM/user.
            log.warn("getAssetFileContent failed: {}", exception.toString(), exception);

            return toolError("getAssetFileContent failed (" + exception.getClass()
                .getSimpleName() + ")");
        }
    }

    private static boolean isTextMime(String mimeType) {
        return mimeType.startsWith("text/") || mimeType.equals("application/json")
            || mimeType.equals("application/xml");
    }

    private String toolError(String message) {
        try {
            return jsonMapper.writeValueAsString(Map.of("error", message));
        } catch (JacksonException exception) {
            return "{\"error\":\"serialization failure\"}";
        }
    }

    public record GetAssetFileContentInput(Long id) {
    }

    public record GetAssetFileContentOutput(long id, String name, String mimeType, String content) {
    }
}
