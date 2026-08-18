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

import com.bytechef.ai.mcp.server.spi.McpAppResources;
import com.bytechef.ai.mcp.server.spi.McpAppUiDescriptor;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.commons.util.JsonUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the MCP App UI descriptors for {@code getAssetFileContent} (file viewer) and {@code queryDataTable}
 * (data-table viewer). Contributes NO {@link ToolCallback}s of its own — both tools used to be registered here
 * directly, unwrapped, and therefore ALWAYS failed with "Workspace context unavailable" on this surface: the management
 * MCP server carries no {@code ToolContext} of its own, and only
 * {@link WorkspaceScopedFlatToolCallback}/{@link WorkspaceScopedSubAgentToolCallback} inject one.
 *
 * <p>
 * {@code getAssetFileContent}'s callable tool moved to {@code ToolCallbackContributorConfiguration
 * #assetFileFlatCrudMcpContributor} (ticket 732, CRUD-delegate-unwind Task 4), correctly wrapped alongside its six
 * asset-file siblings. {@code queryDataTable}'s callable tool moved to {@code ToolCallbackContributorConfiguration
 * #dataTableFlatCrudMcpContributor} (ticket 732, CRUD-delegate-unwind Task 5) the same way, alongside its ten
 * data-table siblings — registering it a second time here (unwrapped) would both duplicate the tool name on
 * {@code tools/list} and resurrect the same bug the asset-file fix already closed.
 * </p>
 *
 * <p>
 * {@link #getMcpAppUiDescriptors()} still declares both bindings here regardless —
 * {@code ManagementMcpServerConfiguration} matches descriptors to tools by NAME across every contributor's combined map
 * ({@code collectMcpAppUiDescriptors}), so a descriptor does not need to originate from the same contributor as the
 * callable tool.
 * </p>
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class ViewerToolMcpContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor viewerToolMcpContributor() {
        return new McpServerToolCallbackContributor() {

            @Override
            public List<ToolCallback> getToolCallbacks() {
                return List.of();
            }

            @Override
            public Map<String, McpAppUiDescriptor> getMcpAppUiDescriptors() {
                return Map.of(
                    "getAssetFileContent",
                    new McpAppUiDescriptor(
                        McpAppResources.FILE_VIEWER_URI, ViewerToolMcpContributorConfiguration::shapeFile),
                    "queryDataTable",
                    new McpAppUiDescriptor(
                        McpAppResources.DATA_TABLE_VIEWER_URI,
                        ViewerToolMcpContributorConfiguration::shapeDataTable));
            }
        };
    }

    // getAssetFileContent returns {id, name, mimeType, content}; the file viewer reads {name, mimeType, content}.
    private static @Nullable Map<String, Object> shapeFile(String resultText) {
        Map<String, ?> result = JsonUtils.readMap(resultText);

        if (result.containsKey("error") || !result.containsKey("content")) {
            return null;
        }

        Map<String, Object> structuredContent = new LinkedHashMap<>();

        structuredContent.put("name", result.get("name"));
        structuredContent.put("mimeType", result.get("mimeType"));
        structuredContent.put("content", result.get("content"));

        return structuredContent;
    }

    // queryDataTable returns a JSON array of row objects; the data-table viewer reads {rows: [...]}.
    private static @Nullable Map<String, Object> shapeDataTable(String resultText) {
        List<Object> rows = JsonUtils.readList(resultText, Object.class);

        return Map.of("rows", rows);
    }
}
