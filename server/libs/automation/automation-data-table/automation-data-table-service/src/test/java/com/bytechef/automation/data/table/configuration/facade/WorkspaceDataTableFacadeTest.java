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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.service.WorkspaceDataTableService;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableTagService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.data.table.execution.service.DataTableStorageService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceDataTableFacadeTest {

    private static final long ENVIRONMENT_ID = 0L;

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

    @Mock
    private WorkspaceDataTableService workspaceDataTableService;

    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @BeforeEach
    void setUp() {
        workspaceDataTableFacade = new WorkspaceDataTableFacadeImpl(
            dataTableRowService, dataTableService, dataTableStorageService, dataTableTagService,
            dataTableWebhookService, workspaceDataTableService);
    }

    @Test
    void testCreateTableAssignsTableToWorkspace() {
        List<ColumnSpec> columnSpecs = List.of(new ColumnSpec("name", ColumnType.STRING));

        when(dataTableService.getIdByBaseName("events")).thenReturn(7L);

        workspaceDataTableFacade.createTable("events", "Events", columnSpecs, 3L, ENVIRONMENT_ID);

        InOrder inOrder = inOrder(dataTableService, workspaceDataTableService);

        inOrder.verify(dataTableService)
            .createTable("events", "Events", columnSpecs, ENVIRONMENT_ID);
        inOrder.verify(workspaceDataTableService)
            .assignDataTableToWorkspace(7L, 3L);
    }

    @Test
    void testDuplicateTableAssignsCopyToSourceWorkspace() {
        when(dataTableService.getBaseNameById(7L)).thenReturn("events");
        when(dataTableService.getIdByBaseName("events_copy")).thenReturn(8L);
        when(workspaceDataTableService.getDataTableWorkspaceDataTables(7L))
            .thenReturn(List.of(new WorkspaceDataTable(7L, 3L)));

        workspaceDataTableFacade.duplicateTable(7L, "events_copy", ENVIRONMENT_ID);

        InOrder inOrder = inOrder(dataTableService, workspaceDataTableService);

        inOrder.verify(dataTableService)
            .duplicateTable("events", "events_copy", ENVIRONMENT_ID);
        inOrder.verify(workspaceDataTableService)
            .assignDataTableToWorkspace(8L, 3L);
    }

    @Test
    void testDuplicateTableAssignsCopyToEverySourceWorkspace() {
        when(dataTableService.getBaseNameById(7L)).thenReturn("events");
        when(dataTableService.getIdByBaseName("events_copy")).thenReturn(8L);
        when(workspaceDataTableService.getDataTableWorkspaceDataTables(7L))
            .thenReturn(List.of(new WorkspaceDataTable(7L, 3L), new WorkspaceDataTable(7L, 4L)));

        workspaceDataTableFacade.duplicateTable(7L, "events_copy", ENVIRONMENT_ID);

        verify(workspaceDataTableService).assignDataTableToWorkspace(8L, 3L);
        verify(workspaceDataTableService).assignDataTableToWorkspace(8L, 4L);
    }

    @Test
    void testDuplicateTableWithoutSourceWorkspaceAssignsNothing() {
        when(dataTableService.getBaseNameById(7L)).thenReturn("events");
        when(dataTableService.getIdByBaseName("events_copy")).thenReturn(8L);
        when(workspaceDataTableService.getDataTableWorkspaceDataTables(7L)).thenReturn(List.of());

        workspaceDataTableFacade.duplicateTable(7L, "events_copy", ENVIRONMENT_ID);

        verify(dataTableService).duplicateTable("events", "events_copy", ENVIRONMENT_ID);
        verify(workspaceDataTableService, never()).assignDataTableToWorkspace(anyLong(), anyLong());
    }

    @Test
    void testListTablesReturnsOnlyWorkspaceTables() {
        DataTableInfo events = new DataTableInfo(7L, "events", null, List.of(), null);
        DataTableInfo eventsCopy = new DataTableInfo(8L, "events_copy", null, List.of(), null);

        when(dataTableService.listTables(ENVIRONMENT_ID)).thenReturn(List.of(events, eventsCopy));
        when(workspaceDataTableService.getWorkspaceDataTables(3L)).thenReturn(List.of(new WorkspaceDataTable(7L, 3L)));

        assertThat(workspaceDataTableFacade.listTables(3L, ENVIRONMENT_ID)).containsExactly(events);
    }
}
