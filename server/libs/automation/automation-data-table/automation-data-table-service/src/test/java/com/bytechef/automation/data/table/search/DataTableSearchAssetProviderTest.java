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

package com.bytechef.automation.data.table.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.service.WorkspaceDataTableService;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableSearchAssetProviderTest {

    @Test
    void testReturnsOnlyCallerWorkspaceDataTablesUpToLimit() {
        DataTableService dataTableService = mock(DataTableService.class);
        WorkspaceDataTableService workspaceDataTableService = mock(WorkspaceDataTableService.class);

        WorkspaceDataTable ownDataTable = mock(WorkspaceDataTable.class);
        WorkspaceDataTable ownDataTable2 = mock(WorkspaceDataTable.class);

        when(ownDataTable.getDataTableId()).thenReturn(3L);
        when(ownDataTable2.getDataTableId()).thenReturn(4L);
        when(workspaceDataTableService.getWorkspaceDataTables(10L)).thenReturn(List.of(ownDataTable, ownDataTable2));
        when(dataTableService.listTables(1L)).thenReturn(
            List.of(
                createDataTableInfo(1L, "orders_foreign"), createDataTableInfo(2L, "orders_foreign_2"),
                createDataTableInfo(3L, "orders_own"), createDataTableInfo(4L, "orders_own_2")));

        DataTableSearchAssetProvider dataTableSearchAssetProvider = new DataTableSearchAssetProvider(
            dataTableService, workspaceDataTableService);

        List<DataTableSearchResult> results = dataTableSearchAssetProvider.search("orders", 1, Set.of(10L));

        assertThat(results).extracting(DataTableSearchResult::id)
            .containsExactly(3L);
    }

    private static DataTableInfo createDataTableInfo(long id, String baseName) {
        return new DataTableInfo(id, baseName, null, List.of(), null);
    }
}
