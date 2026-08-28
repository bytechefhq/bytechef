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

package com.bytechef.automation.ai.tool.datatable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Ivica Cardic
 */
class ListDataTablesToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesListDataTablesName() {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        ListDataTablesToolCallback callback = new ListDataTablesToolCallback(
            workspaceDataTableFacade);

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("listDataTables");
        assertThat(definition.description()).isNotBlank();
    }

    @Test
    void testCallReturnsDataTableListFromWorkspace() throws Exception {
        long workspaceId = 1L;

        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        List<ColumnSpec> columns = List.of(
            new ColumnSpec("name", ColumnType.STRING),
            new ColumnSpec("age", ColumnType.INTEGER));
        DataTableInfo dataTableInfo = new DataTableInfo(42L, "contacts", "Contacts table", columns, null, null);

        when(workspaceDataTableFacade.listTables(workspaceId, 0L)).thenReturn(List.of(dataTableInfo));

        ListDataTablesToolCallback callback = new ListDataTablesToolCallback(
            workspaceDataTableFacade);

        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .environmentId(0L)
                .build()
                .toToolContext());

        String result = callback.call("{}", toolContext);

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();
        assertThat(arrayNode).hasSize(1);

        JsonNode first = arrayNode.get(0);

        assertThat(first.get("id")
            .asLong()).isEqualTo(42L);
        assertThat(first.get("name")
            .asText()).isEqualTo("contacts");

        JsonNode columnsNode = first.get("columns");

        assertThat(columnsNode.isArray()).isTrue();
        assertThat(columnsNode).hasSize(2);
        assertThat(columnsNode.get(0)
            .get("name")
            .asText()).isEqualTo("name");
        assertThat(columnsNode.get(0)
            .get("type")
            .asText()).isEqualTo("STRING");
    }

    @Test
    void testCallReturnsErrorWhenWorkspaceIdMissing() throws Exception {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        ListDataTablesToolCallback callback = new ListDataTablesToolCallback(
            workspaceDataTableFacade);

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsEmptyArrayWhenNoDataTables() throws Exception {
        long workspaceId = 1L;

        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        when(workspaceDataTableFacade.listTables(workspaceId, 0L)).thenReturn(List.of());

        ListDataTablesToolCallback callback = new ListDataTablesToolCallback(
            workspaceDataTableFacade);

        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .environmentId(0L)
                .build()
                .toToolContext());

        String result = callback.call("{}", toolContext);

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();
        assertThat(arrayNode).isEmpty();
    }

    @Test
    void testCallPassesEnvironmentIdToService() throws Exception {
        long workspaceId = 1L;
        long environmentId = 2L;

        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        when(workspaceDataTableFacade.listTables(workspaceId, environmentId)).thenReturn(List.of());

        ListDataTablesToolCallback callback = new ListDataTablesToolCallback(
            workspaceDataTableFacade);

        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .environmentId(environmentId)
                .build()
                .toToolContext());

        callback.call("{}", toolContext);

        verify(workspaceDataTableFacade).listTables(workspaceId, environmentId);
    }
}
