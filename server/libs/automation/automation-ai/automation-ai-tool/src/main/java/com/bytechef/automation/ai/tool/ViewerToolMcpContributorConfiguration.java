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

import com.bytechef.ai.mcp.server.spi.McpAppUiDescriptor;
import com.bytechef.ai.mcp.server.spi.McpAppViewerResources;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.datatable.QueryDataTableToolCallback;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Surfaces the CE read-only viewer-backing tools ({@code getAssetFileContent} → file viewer, {@code queryDataTable} →
 * data-table viewer) as first-class management MCP tools and declares their MCP App UI descriptors. Each backing facade
 * is optional (via {@link ObjectProvider}) so lightweight deployments that lack it simply do not expose that tool.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class ViewerToolMcpContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor viewerToolMcpContributor(
        ObjectProvider<AssetFileFacade> assetFileFacadeProvider,
        ObjectProvider<DataTableRowService> dataTableRowServiceProvider,
        ObjectProvider<DataTableService> dataTableServiceProvider) {

        return new McpServerToolCallbackContributor() {

            @Override
            public List<ToolCallback> getToolCallbacks() {
                List<ToolCallback> toolCallbacks = new ArrayList<>();

                AssetFileFacade assetFileFacade = assetFileFacadeProvider.getIfAvailable();

                if (assetFileFacade != null) {
                    toolCallbacks.add(new GetAssetFileContentToolCallback(assetFileFacade));
                }

                DataTableRowService dataTableRowService = dataTableRowServiceProvider.getIfAvailable();
                DataTableService dataTableService = dataTableServiceProvider.getIfAvailable();

                if (dataTableRowService != null && dataTableService != null) {
                    toolCallbacks.add(new QueryDataTableToolCallback(dataTableRowService, dataTableService));
                }

                return toolCallbacks;
            }

            @Override
            public Map<String, McpAppUiDescriptor> getMcpAppUiDescriptors() {
                return Map.of(
                    "getAssetFileContent",
                    new McpAppUiDescriptor(
                        McpAppViewerResources.FILE_VIEWER_URI, ViewerToolMcpContributorConfiguration::shapeFile),
                    "queryDataTable",
                    new McpAppUiDescriptor(
                        McpAppViewerResources.DATA_TABLE_VIEWER_URI,
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
