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

    private static @Nullable Map<String, Object> shapeDataTable(String resultText) {
        List<Object> rows = JsonUtils.readList(resultText, Object.class);

        return Map.of("rows", rows);
    }
}
