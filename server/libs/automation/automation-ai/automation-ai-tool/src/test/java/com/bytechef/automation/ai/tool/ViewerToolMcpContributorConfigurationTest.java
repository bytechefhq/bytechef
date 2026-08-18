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

import com.bytechef.ai.mcp.server.spi.McpAppResources;
import com.bytechef.ai.mcp.server.spi.McpAppUiDescriptor;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class ViewerToolMcpContributorConfigurationTest {

    private final ViewerToolMcpContributorConfiguration configuration = new ViewerToolMcpContributorConfiguration();

    /**
     * Neither {@code getAssetFileContent} (ticket 732, CRUD-delegate-unwind Task 4) nor {@code queryDataTable} (Task 5)
     * is one of this contributor's own tool callbacks anymore — both callable tools now come from
     * {@code ToolCallbackContributorConfiguration}'s flat-CRUD MCP contributors, correctly workspace-scoped;
     * registering either a second time here (unwrapped, and therefore broken on this ToolContext-less surface) would
     * both duplicate the tool name and resurrect the pre-existing "Workspace context unavailable" bug the flat
     * registrations fixed. {@code ManagementMcpServerConfiguration} matches descriptors to tools by name across every
     * contributor, so both descriptors still apply to tools supplied elsewhere.
     */
    @Test
    void testContributesNoToolsButBothDescriptors() {
        McpServerToolCallbackContributor contributor = configuration.viewerToolMcpContributor();

        assertThat(contributor.getToolCallbacks()).isEmpty();

        Map<String, McpAppUiDescriptor> descriptors = contributor.getMcpAppUiDescriptors();

        assertThat(descriptors.get("getAssetFileContent")
            .resourceUri()).isEqualTo(McpAppResources.FILE_VIEWER_URI);
        assertThat(descriptors.get("queryDataTable")
            .resourceUri()).isEqualTo(McpAppResources.DATA_TABLE_VIEWER_URI);
    }

    @Test
    void testFileShaperExtractsViewerFields() {
        McpServerToolCallbackContributor contributor = configuration.viewerToolMcpContributor();

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
        McpServerToolCallbackContributor contributor = configuration.viewerToolMcpContributor();

        Map<String, Object> structuredContent = contributor.getMcpAppUiDescriptors()
            .get("queryDataTable")
            .structuredContentShaper()
            .apply("[{\"col\":\"v1\"},{\"col\":\"v2\"}]");

        assertThat(structuredContent).containsKey("rows");
        assertThat((List<?>) structuredContent.get("rows")).hasSize(2);
    }
}
