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

package com.bytechef.automation.data.table.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableOwnershipResolverTest {

    private final DataTableService dataTableService = mock(DataTableService.class);

    private final DataTableOwnershipResolver dataTableOwnershipResolver =
        new DataTableOwnershipResolver(dataTableService);

    @Test
    void testResolveOwnerOfAMissingTableIsUnknown() {
        when(dataTableService.getDataTable(1051L)).thenThrow(
            new DataTableException("Data table not found: id=1051", DataTableErrorType.DATA_TABLE_NOT_FOUND));

        assertThat(dataTableOwnershipResolver.resolveOwner(1051L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerOfATableWithoutAWorkspaceIsUnknown() {
        when(dataTableService.getDataTable(1051L)).thenReturn(new DataTable(1051L, "orders"));

        assertThat(dataTableOwnershipResolver.resolveOwner(1051L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerReturnsTheTableWorkspace() {
        DataTable dataTable = new DataTable(1051L, "orders");

        dataTable.setWorkspaceId(7L);

        when(dataTableService.getDataTable(1051L)).thenReturn(dataTable);

        assertThat(dataTableOwnershipResolver.resolveOwner(1051L)).isEqualTo(ResourceOwner.ofWorkspace(7L));
    }
}
