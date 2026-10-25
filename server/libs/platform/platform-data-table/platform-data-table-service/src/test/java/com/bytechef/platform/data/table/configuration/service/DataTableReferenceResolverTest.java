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

package com.bytechef.platform.data.table.configuration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.definition.BaseProperty.ResourceType;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableReferenceResolverTest {

    private final DataTableRowService dataTableRowService = mock(DataTableRowService.class);
    private final DataTableService dataTableService = mock(DataTableService.class);
    private final DataTableWorkspaceResolver dataTableWorkspaceResolver = mock(DataTableWorkspaceResolver.class);
    private final DataTableReferenceResolver dataTableReferenceResolver = new DataTableReferenceResolver(
        dataTableRowService, dataTableService, dataTableWorkspaceResolver);

    @Test
    void resourceTypeIsDataTable() {
        assertEquals(ResourceType.DATA_TABLE, dataTableReferenceResolver.getResourceType());
    }

    @Test
    void testFindProblemReportsATableFromAnotherWorkspace() {
        when(dataTableWorkspaceResolver.resolveByWorkflowId("workflow-1")).thenReturn(OptionalLong.of(7L));
        when(dataTableService.fetchDataTable(7L, "orders")).thenReturn(Optional.empty());

        assertEquals(
            "Data table 'orders' not found in this workspace",
            dataTableReferenceResolver.findProblem("orders", 0L, "workflow-1"));
    }

    @Test
    void testFindProblemSkipsTheCheckWithoutAWorkflowId() {
        assertNull(dataTableReferenceResolver.findProblem("orders", 0L, null));

        verifyNoInteractions(dataTableWorkspaceResolver, dataTableService, dataTableRowService);
    }

    @Test
    void testFindProblemSkipsTheCheckWhenTheWorkspaceCannotBeResolved() {
        when(dataTableWorkspaceResolver.resolveByWorkflowId("workflow-1")).thenReturn(OptionalLong.empty());

        assertNull(dataTableReferenceResolver.findProblem("orders", 0L, "workflow-1"));

        verifyNoInteractions(dataTableService, dataTableRowService);
    }

    @Test
    void testFindProblemAcceptsAnExistingTable() {
        DataTable dataTable = new DataTable(1051L, "orders");

        when(dataTableWorkspaceResolver.resolveByWorkflowId("workflow-1")).thenReturn(OptionalLong.of(7L));
        when(dataTableService.fetchDataTable(7L, "orders")).thenReturn(Optional.of(dataTable));
        when(dataTableService.fetchDataTableInfo(1051L, 0L)).thenReturn(
            Optional.of(new DataTableInfo(1051L, "orders", 7L, null, List.of(), Instant.EPOCH)));

        assertNull(dataTableReferenceResolver.findProblem("orders", 0L, "workflow-1"));
    }

    @Test
    void testFindProblemReportsRowServiceReason() {
        DataTable dataTable = new DataTable(1051L, "orders");

        when(dataTableWorkspaceResolver.resolveByWorkflowId("workflow-1")).thenReturn(OptionalLong.of(7L));
        when(dataTableService.fetchDataTable(7L, "orders")).thenReturn(Optional.of(dataTable));
        when(dataTableService.fetchDataTableInfo(1051L, 0L)).thenReturn(
            Optional.of(new DataTableInfo(1051L, "orders", 7L, null, List.of(), Instant.EPOCH)));
        when(dataTableRowService.listRows(any(DataTableRef.class), anyInt(), anyInt()))
            .thenThrow(new IllegalStateException("Table does not have primary key column 'id': dt_0_1051"));

        assertEquals(
            "Table does not have primary key column 'id': dt_0_1051",
            dataTableReferenceResolver.findProblem("orders", 0L, "workflow-1"));
    }

    @Test
    void testFindProblemProbesTheTableThroughItsId() {
        DataTable dataTable = new DataTable(1051L, "orders");

        when(dataTableWorkspaceResolver.resolveByWorkflowId("workflow-1")).thenReturn(OptionalLong.of(7L));
        when(dataTableService.fetchDataTable(7L, "orders")).thenReturn(Optional.of(dataTable));
        when(dataTableService.fetchDataTableInfo(1051L, 0L)).thenReturn(
            Optional.of(new DataTableInfo(1051L, "orders", 7L, null, List.of(), Instant.EPOCH)));

        dataTableReferenceResolver.findProblem("orders", 0L, "workflow-1");

        verify(dataTableRowService).listRows(new DataTableRef(1051L, 0L), 1, 0);
    }
}
