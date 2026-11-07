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

package com.bytechef.automation.data.table.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.mcp.server.spi.McpAppResources;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class DataTableViewerMcpContributorConfigurationTest {

    private final DataTableViewerMcpContributorConfiguration configuration =
        new DataTableViewerMcpContributorConfiguration();

    @Test
    void testContributesNoToolsButTheDataTableViewerDescriptor() {
        McpServerToolCallbackContributor contributor = configuration.dataTableViewerMcpContributor();

        assertThat(contributor.getToolCallbacks()).isEmpty();
        assertThat(contributor.getMcpAppUiDescriptors()
            .get("queryDataTable")
            .resourceUri()).isEqualTo(McpAppResources.DATA_TABLE_VIEWER_URI);
    }

    @Test
    void testDataTableShaperWrapsRows() {
        McpServerToolCallbackContributor contributor = configuration.dataTableViewerMcpContributor();

        Map<String, Object> structuredContent = contributor.getMcpAppUiDescriptors()
            .get("queryDataTable")
            .structuredContentShaper()
            .apply("[{\"col\":\"v1\"},{\"col\":\"v2\"}]");

        assertThat(structuredContent).containsKey("rows");
        assertThat((List<?>) structuredContent.get("rows")).hasSize(2);
    }
}
