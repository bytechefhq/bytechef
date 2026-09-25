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
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import java.time.Instant;
import java.util.List;
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
    void testSearchStampsEachTableWithItsOwnWorkspace() {
        when(dataTableService.listAllTables(Environment.DEVELOPMENT.ordinal())).thenReturn(
            List.of(
                new DataTableInfo(1051L, "orders", 7L, null, List.of(), Instant.EPOCH),
                new DataTableInfo(1052L, "orders", 8L, null, List.of(), Instant.EPOCH)));

        List<DataTableSearchResult> dataTableSearchResults = dataTableSearchAssetProvider.search("ORD", 10);

        assertThat(dataTableSearchResults).containsExactly(
            new DataTableSearchResult(1051L, "orders", 7L), new DataTableSearchResult(1052L, "orders", 8L));
    }

    @Test
    void testSearchSkipsTablesWithoutAWorkspace() {
        when(dataTableService.listAllTables(Environment.DEVELOPMENT.ordinal())).thenReturn(
            List.of(
                new DataTableInfo(1051L, "orders", null, null, List.of(), Instant.EPOCH),
                new DataTableInfo(1052L, "orders", 8L, null, List.of(), Instant.EPOCH)));

        List<DataTableSearchResult> dataTableSearchResults = dataTableSearchAssetProvider.search("orders", 10);

        assertThat(dataTableSearchResults).containsExactly(new DataTableSearchResult(1052L, "orders", 8L));
    }
}
