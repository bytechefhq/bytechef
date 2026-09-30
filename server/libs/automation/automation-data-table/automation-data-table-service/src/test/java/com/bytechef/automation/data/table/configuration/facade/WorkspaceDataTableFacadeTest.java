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

package com.bytechef.automation.data.table.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableTagService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.data.table.execution.service.DataTableStorageService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceDataTableFacadeTest {

    @Mock
    private DataTableRowService dataTableRowService;

    @Mock
    private DataTableService dataTableService;

    @Mock
    private DataTableStorageService dataTableStorageService;

    @Mock
    private DataTableTagService dataTableTagService;

    @Mock
    private DataTableWebhookService dataTableWebhookService;

    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @BeforeEach
    void setUp() {
        workspaceDataTableFacade = new WorkspaceDataTableFacadeImpl(
            dataTableRowService, dataTableService, dataTableStorageService, dataTableTagService,
            dataTableWebhookService);
    }

    @Test
    void testCreateTablePassesTheWorkspaceAndReturnsTheId() {
        List<ColumnSpec> columnSpecs = List.of(new ColumnSpec("title", ColumnType.STRING));

        when(dataTableService.createTable(7L, "orders", "desc", columnSpecs, 0L)).thenReturn(1051L);

        long dataTableId = workspaceDataTableFacade.createTable("orders", "desc", columnSpecs, 7L, 0L);

        assertThat(dataTableId).isEqualTo(1051L);
    }

    @Test
    void testListTablesListsOnlyTheWorkspace() {
        DataTableInfo dataTableInfo = new DataTableInfo(1051L, "orders", 7L, null, List.of(), Instant.EPOCH);

        when(dataTableService.listTables(7L, 0L)).thenReturn(List.of(dataTableInfo));

        assertThat(workspaceDataTableFacade.listTables(7L, 0L)).containsExactly(dataTableInfo);
    }

    @Test
    void testGetWorkspaceIdReadsTheColumn() {
        DataTable dataTable = new DataTable(1051L, "orders");

        dataTable.setWorkspaceId(7L);

        when(dataTableService.getDataTable(1051L)).thenReturn(dataTable);

        assertThat(workspaceDataTableFacade.getWorkspaceId(1051L)).isEqualTo(7L);
    }

    @Test
    void testListRowsBuildsAnIdRef() {
        workspaceDataTableFacade.listRows(1051L, 10, 0, 0L);

        verify(dataTableRowService).listRows(new DataTableRef(1051L, 0L), 10, 0);
    }
}
