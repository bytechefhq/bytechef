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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.mcp.server.spi.McpAppUiDescriptor;
import com.bytechef.ai.mcp.server.spi.McpAppViewerResources;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(ObjectMapperSetupExtension.class)
class ViewerToolMcpContributorConfigurationTest {

    private final ViewerToolMcpContributorConfiguration configuration = new ViewerToolMcpContributorConfiguration();

    @Test
    void testContributesFileAndDataTableToolsWithDescriptors() {
        McpServerToolCallbackContributor contributor = configuration.viewerToolMcpContributor(
            present(mock(AssetFileFacade.class)), present(mock(DataTableRowService.class)),
            present(mock(DataTableService.class)));

        List<String> toolNames = contributor.getToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).containsExactlyInAnyOrder("getAssetFileContent", "queryDataTable");

        Map<String, McpAppUiDescriptor> descriptors = contributor.getMcpAppUiDescriptors();

        assertThat(descriptors.get("getAssetFileContent")
            .resourceUri()).isEqualTo(McpAppViewerResources.FILE_VIEWER_URI);
        assertThat(descriptors.get("queryDataTable")
            .resourceUri()).isEqualTo(McpAppViewerResources.DATA_TABLE_VIEWER_URI);
    }

    @Test
    void testSkipsToolsWhenFacadesAbsent() {
        McpServerToolCallbackContributor contributor = configuration.viewerToolMcpContributor(
            absent(), absent(), absent());

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void testFileShaperExtractsViewerFields() {
        McpServerToolCallbackContributor contributor = configuration.viewerToolMcpContributor(
            present(mock(AssetFileFacade.class)), absent(), absent());

        Map<String, Object> structuredContent = contributor.getMcpAppUiDescriptors()
            .get("getAssetFileContent")
            .structuredContentShaper()
            .apply("{\"id\":1,\"name\":\"a.txt\",\"mimeType\":\"text/plain\",\"content\":\"hello\"}");

        assertThat(structuredContent).containsEntry("name", "a.txt")
            .containsEntry("mimeType", "text/plain")
            .containsEntry("content", "hello")
            .doesNotContainKey("id");
    }

    @Test
    void testDataTableShaperWrapsRows() {
        McpServerToolCallbackContributor contributor = configuration.viewerToolMcpContributor(
            absent(), present(mock(DataTableRowService.class)), present(mock(DataTableService.class)));

        Map<String, Object> structuredContent = contributor.getMcpAppUiDescriptors()
            .get("queryDataTable")
            .structuredContentShaper()
            .apply("[{\"col\":\"v1\"},{\"col\":\"v2\"}]");

        assertThat(structuredContent).containsKey("rows");
        assertThat((List<?>) structuredContent.get("rows")).hasSize(2);
    }

    private static <T> ObjectProvider<T> present(T value) {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        org.mockito.Mockito.when(provider.getIfAvailable())
            .thenReturn(value);

        return provider;
    }

    private static <T> ObjectProvider<T> absent() {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        return provider;
    }
}
