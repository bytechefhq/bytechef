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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableSearchAssetProviderTest {

    private final DataTableService dataTableService = mock(DataTableService.class);

    private final DataTableSearchAssetProvider dataTableSearchAssetProvider =
        new DataTableSearchAssetProvider(dataTableService);

    @Test
    void testReturnsOnlyCallerWorkspaceDataTablesUpToLimit() {
        when(dataTableService.getWorkspaceDataTables(10L)).thenReturn(
            List.of(new DataTable(3L, "orders_own"), new DataTable(4L, "orders_own_2"), new DataTable(5L, "users")));

        List<DataTableSearchResult> results = dataTableSearchAssetProvider.search("orders", 1, Set.of(10L));

        assertThat(results).extracting(DataTableSearchResult::id)
            .containsExactly(3L);

        verify(dataTableService, never()).getWorkspaceDataTables(20L);
    }

    @Test
    void testMatchesNamesIgnoringCaseAcrossEveryCallerWorkspace() {
        when(dataTableService.getWorkspaceDataTables(10L)).thenReturn(List.of(new DataTable(3L, "Orders")));
        when(dataTableService.getWorkspaceDataTables(20L)).thenReturn(List.of(new DataTable(7L, "orders")));

        List<DataTableSearchResult> results = dataTableSearchAssetProvider.search("ORD", 10, Set.of(10L, 20L));

        assertThat(results).extracting(DataTableSearchResult::id, DataTableSearchResult::name)
            .containsExactly(
                tuple(3L, "Orders"), tuple(7L, "orders"));
    }

    @Test
    void testReturnsNothingWithoutCallerWorkspaces() {
        assertThat(dataTableSearchAssetProvider.search("orders", 10, Set.of())).isEmpty();
    }
}
