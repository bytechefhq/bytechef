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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class DataTableSearchAssetProviderTest {

    @Mock
    private DataTableService dataTableService;

    @InjectMocks
    private DataTableSearchAssetProvider dataTableSearchAssetProvider;

    @Test
    void testReturnsOnlyCallerWorkspaceDataTablesUpToLimit() {
        when(dataTableService.listAllTables(Environment.DEVELOPMENT.ordinal())).thenReturn(
            List.of(
                createDataTableInfo(1L, "orders_foreign", 20L), createDataTableInfo(2L, "orders_foreign_2", 20L),
                createDataTableInfo(3L, "orders_own", 10L), createDataTableInfo(4L, "orders_own_2", 10L)));

        List<DataTableSearchResult> results = dataTableSearchAssetProvider.search("ORDERS", 1, Set.of(10L));

        assertThat(results).containsExactly(new DataTableSearchResult(3L, "orders_own"));
    }

    @Test
    void testSkipsTablesWithoutAWorkspace() {
        when(dataTableService.listAllTables(Environment.DEVELOPMENT.ordinal())).thenReturn(
            List.of(createDataTableInfo(1051L, "orders", null), createDataTableInfo(1052L, "orders", 10L)));

        List<DataTableSearchResult> results = dataTableSearchAssetProvider.search("orders", 10, Set.of(10L));

        assertThat(results).containsExactly(new DataTableSearchResult(1052L, "orders"));
    }

    @Test
    void testReturnsNothingWithoutAccessibleWorkspaces() {
        List<DataTableSearchResult> results = dataTableSearchAssetProvider.search("orders", 10, Set.of());

        assertThat(results).isEmpty();

        verifyNoInteractions(dataTableService);
    }

    private static DataTableInfo createDataTableInfo(long id, String name, Long workspaceId) {
        return new DataTableInfo(id, name, workspaceId, null, List.of(), Instant.EPOCH);
    }
}
