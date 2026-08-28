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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.service.WorkspaceDataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableTagService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.data.table.execution.service.DataTableStorageService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * A duplicated table has to land in the same workspace as the table it was copied from.
 *
 * <p>
 * {@code DataTableServiceImpl.duplicateTable} registers a {@code data_table} row for the copy, but the workspace
 * binding lives in a separate relation that only this facade maintains. Without the assignment the copy exists with no
 * {@code workspace_data_table} row, and {@link WorkspaceDataTableFacadeImpl#listTables} intersects against exactly that
 * relation -- so the duplicate disappears from the automation list while remaining visible to the unscoped
 * {@code listTables(environmentId)} that component option dropdowns and the embedded console read. The knowledge base
 * clone path already assigns its copy; this pins the same contract for data tables.
 * </p>
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceDataTableFacadeDuplicateTest {

    private static final long DATA_TABLE_ID = 1050L;
    private static final long DUPLICATE_ID = 1090L;
    private static final long ENVIRONMENT_ID = 0L;
    private static final long WORKSPACE_ID = 1049L;

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

    @InjectMocks
    private WorkspaceDataTableFacadeImpl workspaceDataTableFacade;

    @Test
    void testTheDuplicateJoinsTheSourceWorkspace() {
        when(dataTableService.getBaseNameById(DATA_TABLE_ID)).thenReturn("table1");
        when(dataTableService.getIdByBaseName("table1_copy")).thenReturn(DUPLICATE_ID);
        when(workspaceDataTableService.fetchWorkspaceId(DATA_TABLE_ID)).thenReturn(Optional.of(WORKSPACE_ID));

        workspaceDataTableFacade.duplicateTable(DATA_TABLE_ID, "table1_copy", ENVIRONMENT_ID);

        verify(dataTableService).duplicateTable("table1", "table1_copy", ENVIRONMENT_ID);
        verify(workspaceDataTableService).assignDataTableToWorkspace(DUPLICATE_ID, WORKSPACE_ID);
    }

    @Test
    void testAnUnboundSourceLeavesTheDuplicateUnbound() {
        when(dataTableService.getBaseNameById(DATA_TABLE_ID)).thenReturn("table1");
        when(workspaceDataTableService.fetchWorkspaceId(DATA_TABLE_ID)).thenReturn(Optional.empty());

        workspaceDataTableFacade.duplicateTable(DATA_TABLE_ID, "table1_copy", ENVIRONMENT_ID);

        verify(dataTableService).duplicateTable("table1", "table1_copy", ENVIRONMENT_ID);

        // Inventing a workspace for a copy of a table that belongs to none would put the duplicate somewhere the
        // original never was; the copy inherits the source's state instead.
        verify(workspaceDataTableService, never()).assignDataTableToWorkspace(anyLong(), eq(WORKSPACE_ID));
    }
}
