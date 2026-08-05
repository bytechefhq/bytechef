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
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that clones an asset file into a different environment within the same workspace,
 * copying the underlying bytes verbatim. The clone is recorded as a USER_UPLOAD regardless of the source's origin — see
 * {@link AssetFileFacade#cloneToEnvironment} for the rationale; the short version is that promoting an AI-generated
 * file means the user has reviewed and is shipping it, so attributing the destination as a user upload is the honest
 * audit trail. AI metadata (generatedByAgentSource, format, metadataJson) is dropped for the same reason.
 *
 * <p>
 * Security: workspace identity comes from the trusted {@link ToolContext} only. The facade additionally re-validates
 * ownership of the source file before cloning, throwing {@link AssetFileNotFoundException} for forged ids.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class CloneAssetFileToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "cloneAssetFile";

    private static final String DESCRIPTION = """
        Clone an asset file into a different environment within the same workspace, copying the underlying bytes.
        Use when the user says "promote my report.pdf to PROD" or "copy that CSV to staging." The clone preserves
        the source's filename (or pass `newName` to override), description, and bytes. The clone is recorded as a
        USER_UPLOAD even when the source was AI-generated — promoting through this tool is an explicit user
        acknowledgement of the file's contents. Returns {id, name, environment}.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "assetFileId": {
                        "type": "integer",
                        "description": "Source asset file id from listAssetFiles. Never invent ids."
                    },
                    "targetEnvironment": {
                        "type": "string",
                        "enum": ["DEVELOPMENT", "STAGING", "PRODUCTION"],
                        "description": "Destination environment for the clone."
                    },
                    "newName": {
                        "type": "string",
                        "description": "Optional filename override; falls back to the source filename when omitted."
                    }
                },
                "required": ["assetFileId", "targetEnvironment"]
            }""";

    private final AssetFileFacade facade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CloneAssetFileToolCallback(AssetFileFacade facade) {
        this.facade = facade;
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
            CloneAssetFileInput input = jsonMapper.readValue(toolInput, CloneAssetFileInput.class);

            Long assetFileId = input.assetFileId();

            if (assetFileId == null) {
                return toolError("assetFileId is required");
            }

            String targetEnvironment = input.targetEnvironment();

            if (targetEnvironment == null || targetEnvironment.isBlank()) {
                return toolError("targetEnvironment is required");
            }

            int targetEnvironmentOrdinal;

            try {
                targetEnvironmentOrdinal = Environment.valueOf(targetEnvironment.toUpperCase())
                    .ordinal();
            } catch (IllegalArgumentException invalidEnv) {
                return toolError(
                    "targetEnvironment must be one of DEVELOPMENT, STAGING, PRODUCTION (got: "
                        + targetEnvironment + ")");
            }

            AutomationToolInvocationContext invocationContext =
                AutomationToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the Files panel of a workspace.");
            }

            try {
                AssetFile clone = facade.cloneToEnvironment(
                    assetFileId, workspaceId, targetEnvironmentOrdinal, input.newName());

                return jsonMapper.writeValueAsString(
                    new CloneAssetFileOutput(
                        clone.getId(), clone.getName(), clone.getEnvironment()
                            .name()));
            } catch (AssetFileNotFoundException notFound) {
                // Same workspace + id couldn't be resolved (forged id from another workspace, or genuinely deleted).
                // The exception message is generic on purpose to avoid a 404-vs-403 oracle; surface it verbatim.
                return toolError(notFound.getMessage());
            } catch (IllegalArgumentException invalid) {
                return toolError(invalid.getMessage());
            }
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return toolError("Unexpected runtime failure during cloneAssetFile: " + exception.getClass()
                .getSimpleName());
        }
    }

    private String toolError(String message) {
        try {
            return jsonMapper.writeValueAsString(new CloneAssetFileError(message));
        } catch (JacksonException jacksonException) {
            // The error payload itself failed to serialise — fall back to a hand-rolled minimal JSON envelope so the
            // LLM still sees a structured error rather than the raw exception bubbling up.
            return "{\"error\":\"cloneAssetFile failed and the error payload could not be serialised\"}";
        }
    }

    public record CloneAssetFileInput(
        @Nullable Long assetFileId, @Nullable String targetEnvironment, @Nullable String newName) {
    }

    public record CloneAssetFileOutput(long id, String name, String environment) {
    }

    public record CloneAssetFileError(String error) {
    }
}
