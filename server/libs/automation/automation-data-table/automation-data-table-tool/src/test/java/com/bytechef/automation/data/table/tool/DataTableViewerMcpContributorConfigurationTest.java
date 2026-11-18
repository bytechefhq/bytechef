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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpAppResources;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

@ExtendWith(ObjectMapperSetupExtension.class)
class DataTableViewerMcpContributorConfigurationTest {

    private final DataTableViewerMcpContributorConfiguration configuration =
        new DataTableViewerMcpContributorConfiguration();

    private final WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    @BeforeEach
    void beforeEach() {
        when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(42L, "Workspace 42")));
    }

    @Test
    void testContributesTheReadToolWorkspaceScoped() {
        List<ToolCallback> toolCallbacks = contributor().getToolCallbacks();

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactly("queryDataTable");
        assertThat(toolCallbacks)
            .allSatisfy(toolCallback -> {
                assertThat(toolCallback).isInstanceOf(WorkspaceScopedFlatToolCallback.class);
                assertThat(toolCallback.getToolDefinition()
                    .inputSchema()).contains("\"workspaceId\"", "\"environment\"");
            });
    }

    @Test
    void testContributesNoToolsWhenTheFacadeIsAbsent() {
        McpServerToolCallbackContributor contributor = configuration.dataTableViewerMcpContributor(
            provider(new StaticListableBeanFactory(), WorkspaceDataTableFacade.class),
            provider(new StaticListableBeanFactory(Map.of("workspaceFacade", workspaceFacade)), WorkspaceFacade.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
        assertThat(contributor.getMcpAppUiDescriptors()).containsOnlyKeys("queryDataTable");
    }

    @Test
    void testQueryDataTableReadsRowsOfATableInTheRequestedWorkspace() {
        when(workspaceDataTableFacade.listTables(42L, 0L)).thenReturn(
            List.of(new DataTableInfo(7L, "leads", null, null, List.of(), Instant.EPOCH)));
        when(workspaceDataTableFacade.listRows(7L, 50, 0, 0L)).thenReturn(
            List.of(new DataTableRow(1L, Map.of("name", "Ada"))));

        String result = findToolCallback("queryDataTable").call("{\"workspaceId\":42,\"dataTableId\":\"7\"}");

        assertThat(result).isEqualTo("[{\"name\":\"Ada\"}]");
    }

    @Test
    void testQueryDataTableRefusesAWorkspaceTheCallerCannotAccess() {
        String result = findToolCallback("queryDataTable").call("{\"workspaceId\":99,\"dataTableId\":\"7\"}");

        assertThat(result).contains("not accessible");

        verify(workspaceDataTableFacade, never()).listTables(anyLong(), anyLong());
    }

    @Test
    void testContributesTheDataTableViewerDescriptor() {
        assertThat(contributor().getMcpAppUiDescriptors()
            .get("queryDataTable")
            .resourceUri()).isEqualTo(McpAppResources.DATA_TABLE_VIEWER_URI);
    }

    @Test
    void testDataTableShaperWrapsRows() {
        Map<String, Object> structuredContent = contributor().getMcpAppUiDescriptors()
            .get("queryDataTable")
            .structuredContentShaper()
            .apply("[{\"col\":\"v1\"},{\"col\":\"v2\"}]");

        assertThat(structuredContent).containsKey("rows");
        assertThat((List<?>) structuredContent.get("rows")).hasSize(2);
    }

    private McpServerToolCallbackContributor contributor() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(
            Map.of("workspaceDataTableFacade", workspaceDataTableFacade, "workspaceFacade", workspaceFacade));

        return configuration.dataTableViewerMcpContributor(
            provider(beanFactory, WorkspaceDataTableFacade.class), provider(beanFactory, WorkspaceFacade.class));
    }

    private ToolCallback findToolCallback(String name) {
        return contributor().getToolCallbacks()
            .stream()
            .filter(toolCallback -> name.equals(toolCallback.getToolDefinition()
                .name()))
            .findFirst()
            .orElseThrow();
    }

    private static <T> ObjectProvider<T> provider(StaticListableBeanFactory beanFactory, Class<T> type) {
        return beanFactory.getBeanProvider(type);
    }
}
