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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.tool.datatable.DataTableQuerySupport.DataTableNotFoundException;
import com.bytechef.automation.ai.tool.datatable.DataTableQuerySupport.WhereParseException;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class DataTableQuerySupportTest {

    private static final long CONTACTS_DATA_TABLE_ID = 7L;

    private final WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

    @Test
    void testResolveLimitCapsAndDefaults() {
        assertThat(DataTableQuerySupport.resolveLimit(null)).isEqualTo(DataTableQuerySupport.MAX_LIMIT);
        assertThat(DataTableQuerySupport.resolveLimit(0)).isEqualTo(DataTableQuerySupport.MAX_LIMIT);
        assertThat(DataTableQuerySupport.resolveLimit(-3)).isEqualTo(DataTableQuerySupport.MAX_LIMIT);
        assertThat(DataTableQuerySupport.resolveLimit(10)).isEqualTo(10);
        assertThat(DataTableQuerySupport.resolveLimit(999)).isEqualTo(DataTableQuerySupport.MAX_LIMIT);
    }

    @Test
    void testResolveBaseNameReturnsBaseNameOwnedByWorkspace() throws Exception {
        when(workspaceDataTableFacade.listTables(3L, 0L)).thenReturn(
            List.of(new DataTableInfo(7L, "contacts", null, List.of(), null)));

        assertThat(DataTableQuerySupport.resolveBaseName(workspaceDataTableFacade, 7L, 3L, 0L))
            .isEqualTo("contacts");
    }

    @Test
    void testResolveBaseNameThrowsWhenTableBelongsToAnotherWorkspace() {
        when(workspaceDataTableFacade.listTables(3L, 0L)).thenReturn(
            List.of(new DataTableInfo(9L, "orders", null, List.of(), null)));

        assertThatExceptionOfType(DataTableNotFoundException.class)
            .isThrownBy(() -> DataTableQuerySupport.resolveBaseName(workspaceDataTableFacade, 7L, 3L, 0L));
    }

    @Test
    void testResolveBaseNameThrowsWhenAbsent() {
        when(workspaceDataTableFacade.listTables(3L, 0L)).thenReturn(List.of());

        assertThatExceptionOfType(DataTableNotFoundException.class)
            .isThrownBy(() -> DataTableQuerySupport.resolveBaseName(workspaceDataTableFacade, 7L, 3L, 0L));
    }

    @Test
    void testQueryRowMapsReturnsAllRowsWithoutWhere() throws Exception {

        when(workspaceDataTableFacade.listRows(CONTACTS_DATA_TABLE_ID, 50, 0, 0L)).thenReturn(
            List.of(new DataTableRow(1L, Map.of("name", "Alice")), new DataTableRow(2L, Map.of("name", "Bob"))));

        List<Map<String, Object>> rowMaps = DataTableQuerySupport.queryRowMaps(
            workspaceDataTableFacade, CONTACTS_DATA_TABLE_ID, null, 50, 0L);

        assertThat(rowMaps).hasSize(2);
    }

    @Test
    void testQueryRowMapsAppliesEqualsWhereFilter() throws Exception {

        when(workspaceDataTableFacade.listRows(CONTACTS_DATA_TABLE_ID, DataTableQuerySupport.SCAN_PAGE_SIZE, 0, 0L))
            .thenReturn(
                List.of(
                    new DataTableRow(1L, Map.of("status", "qualified")),
                    new DataTableRow(2L, Map.of("status", "new"))));

        List<Map<String, Object>> rowMaps = DataTableQuerySupport.queryRowMaps(
            workspaceDataTableFacade, CONTACTS_DATA_TABLE_ID, "status = 'qualified'", 50, 0L);

        assertThat(rowMaps).hasSize(1);
        assertThat(rowMaps.getFirst()).containsEntry("status", "qualified");
    }

    @Test
    void testQueryRowMapsThrowsOnMalformedWhere() {

        assertThatExceptionOfType(WhereParseException.class)
            .isThrownBy(
                () -> DataTableQuerySupport.queryRowMaps(workspaceDataTableFacade, CONTACTS_DATA_TABLE_ID,
                    "no operator", 50, 0L));

        verifyNoInteractions(workspaceDataTableFacade);
    }

    @Test
    void testQueryRowMapsFindsMatchesBeyondTheFirstPage() throws Exception {

        List<DataTableRow> firstPage = LongStream.range(0, DataTableQuerySupport.SCAN_PAGE_SIZE)
            .mapToObj(id -> new DataTableRow(id, Map.of("status", "new")))
            .toList();

        when(workspaceDataTableFacade.listRows(CONTACTS_DATA_TABLE_ID, DataTableQuerySupport.SCAN_PAGE_SIZE, 0, 0L))
            .thenReturn(firstPage);
        when(workspaceDataTableFacade.listRows(
            CONTACTS_DATA_TABLE_ID, DataTableQuerySupport.SCAN_PAGE_SIZE, DataTableQuerySupport.SCAN_PAGE_SIZE, 0L))
                .thenReturn(List.of(new DataTableRow(900L, Map.of("status", "qualified"))));

        List<Map<String, Object>> rowMaps = DataTableQuerySupport.queryRowMaps(
            workspaceDataTableFacade, CONTACTS_DATA_TABLE_ID, "status = 'qualified'", 50, 0L);

        assertThat(rowMaps).hasSize(1);
        assertThat(rowMaps.getFirst()).containsEntry("status", "qualified");
    }

    @Test
    void testQueryRowMapsStopsScanningOnceLimitIsReached() throws Exception {

        List<DataTableRow> firstPage = LongStream.range(0, DataTableQuerySupport.SCAN_PAGE_SIZE)
            .mapToObj(id -> new DataTableRow(id, Map.of("status", "qualified")))
            .toList();

        when(workspaceDataTableFacade.listRows(CONTACTS_DATA_TABLE_ID, DataTableQuerySupport.SCAN_PAGE_SIZE, 0, 0L))
            .thenReturn(firstPage);

        List<Map<String, Object>> rowMaps = DataTableQuerySupport.queryRowMaps(
            workspaceDataTableFacade, CONTACTS_DATA_TABLE_ID, "status = 'qualified'", 5, 0L);

        assertThat(rowMaps).hasSize(5);

        verify(workspaceDataTableFacade, never()).listRows(
            CONTACTS_DATA_TABLE_ID, DataTableQuerySupport.SCAN_PAGE_SIZE, DataTableQuerySupport.SCAN_PAGE_SIZE, 0L);
    }
}
