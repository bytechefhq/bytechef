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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.service.WorkspaceDataTableService;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableTagService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.data.table.execution.service.DataTableStorageService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

/**
 * The row-shaped facade methods -- paging, not-found translation and the unowned automation ref every row statement
 * must go through.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceDataTableFacadeRowsTest {

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

    @InjectMocks
    private WorkspaceDataTableFacadeImpl workspaceDataTableFacade;

    @Test
    void testListRowsBuildsAPageFromRowsAndCount() {
        when(dataTableService.getBaseNameById(7L)).thenReturn("orders");
        when(dataTableRowService.listRows(any(), eq(50), eq(100), eq(List.of()), eq(List.of())))
            .thenReturn(List.of(new DataTableRow(1L, Map.of())));
        when(dataTableRowService.countRows(any(), eq(List.of()))).thenReturn(151L);

        Page<DataTableRow> page = workspaceDataTableFacade.listRows(7L, List.of(), List.of(), 2, 50, ENVIRONMENT_ID);

        assertEquals(151L, page.getTotalElements());
        assertEquals(2, page.getNumber());
        assertEquals(1, page.getContent()
            .size());
    }

    @Test
    void testGetRowTranslatesMissingIntoATypedNotFound() {
        when(dataTableService.getBaseNameById(7L)).thenReturn("orders");
        when(dataTableRowService.getRow(any(), eq(9L))).thenReturn(null);

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> workspaceDataTableFacade.getRow(7L, 9L, ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.ROW_NOT_FOUND.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testGetTableTranslatesMissingIntoATypedNotFound() {
        when(dataTableService.getBaseNameById(7L)).thenReturn("orders");
        when(dataTableService.fetchDataTableInfo("orders", ENVIRONMENT_ID))
            .thenReturn(Optional.empty());

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> workspaceDataTableFacade.getTable(7L, ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.DATA_TABLE_NOT_FOUND.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testGetWorkspaceIdTranslatesMissingIntoATypedNotFound() {
        when(workspaceDataTableService.fetchWorkspaceId(7L)).thenReturn(Optional.empty());

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> workspaceDataTableFacade.getWorkspaceId(7L));

        assertEquals(DataTableErrorType.DATA_TABLE_NOT_FOUND.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testEveryRowMethodUsesAnUnownedAutomationRef() {
        when(dataTableService.getBaseNameById(7L)).thenReturn("orders");

        workspaceDataTableFacade.clearRows(7L, ENVIRONMENT_ID);

        ArgumentCaptor<DataTableRef> captor = ArgumentCaptor.forClass(DataTableRef.class);

        verify(dataTableRowService).clearRows(captor.capture());

    }
}
