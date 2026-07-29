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
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that returns a summary list of the current workspace's files (max 50 entries) so the
 * agent can decide which files to read.
 *
 *
 * @author Ivica Cardic
 */
public class ListAssetFilesToolCallback implements ToolCallback {

    private static final int MAX_RESULTS = 50;

    private static final String DESCRIPTION = """
        List the files currently saved in the user's workspace. Returns up to 50 summary entries
        (id, name, mimeType, sizeBytes, createdDate). Use this before reading a file to find its id.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {}
        }""";

    private final AssetFileFacade facade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListAssetFilesToolCallback(AssetFileFacade facade) {
        this.facade = facade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("listAssetFiles")
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
            AutomationToolInvocationContext invocationContext =
                AutomationToolInvocationContext.fromToolContext(toolContext);
            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the Files panel of a workspace.");
            }

            List<AssetFile> files = facade.findAllByWorkspaceIdAndEnvironment(
                workspaceId, AutomationToolInvocationContext.resolveEnvironmentOrDefault(invocationContext), null);

            List<AssetFileSummary> summaries = files.stream()
                .limit(MAX_RESULTS)
                .map(file -> new AssetFileSummary(
                    file.getId(), file.getName(), file.getMimeType(), file.getSizeBytes(), file.getCreatedDate()))
                .toList();

            return jsonMapper.writeValueAsString(summaries);
        } catch (JacksonException exception) {
            return toolError("Serialization failure: " + exception.getMessage());
        }
    }

    private String toolError(String message) {
        try {
            return jsonMapper.writeValueAsString(Map.of("error", message));
        } catch (JacksonException exception) {
            return "{\"error\":\"serialization failure\"}";
        }
    }

    public record AssetFileSummary(Long id, String name, String mimeType, long sizeBytes, Instant createdDate) {
    }
}
