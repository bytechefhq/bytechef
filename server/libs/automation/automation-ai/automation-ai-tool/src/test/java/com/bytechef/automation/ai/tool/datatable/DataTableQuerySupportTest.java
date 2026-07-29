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
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.tool.datatable.DataTableQuerySupport.DataTableNotFoundException;
import com.bytechef.automation.ai.tool.datatable.DataTableQuerySupport.WhereParseException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DataTableQuerySupportTest {

    @Test
    void testResolveLimitCapsAndDefaults() {
        assertThat(DataTableQuerySupport.resolveLimit(null)).isEqualTo(DataTableQuerySupport.MAX_LIMIT);
        assertThat(DataTableQuerySupport.resolveLimit(0)).isEqualTo(DataTableQuerySupport.MAX_LIMIT);
        assertThat(DataTableQuerySupport.resolveLimit(-3)).isEqualTo(DataTableQuerySupport.MAX_LIMIT);
        assertThat(DataTableQuerySupport.resolveLimit(10)).isEqualTo(10);
        assertThat(DataTableQuerySupport.resolveLimit(999)).isEqualTo(DataTableQuerySupport.MAX_LIMIT);
    }

    @Test
    void testResolveBaseNameThrowsWhenAbsent() {
        DataTableService dataTableService = mock(DataTableService.class);

        when(dataTableService.getBaseNameById(7L)).thenReturn(null);

        assertThatExceptionOfType(DataTableNotFoundException.class)
            .isThrownBy(() -> DataTableQuerySupport.resolveBaseName(dataTableService, 7L));
    }

    @Test
    void testQueryRowMapsReturnsAllRowsWithoutWhere() throws Exception {
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);

        when(dataTableRowService.listRows("contacts", 50, 0, 0L)).thenReturn(
            List.of(new DataTableRow(1L, Map.of("name", "Alice")), new DataTableRow(2L, Map.of("name", "Bob"))));

        List<Map<String, Object>> rowMaps = DataTableQuerySupport.queryRowMaps(
            dataTableRowService, "contacts", null, 50, 0L);

        assertThat(rowMaps).hasSize(2);
    }

    @Test
    void testQueryRowMapsAppliesEqualsWhereFilter() throws Exception {
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);

        when(dataTableRowService.listRows("contacts", 50, 0, 0L)).thenReturn(
            List.of(
                new DataTableRow(1L, Map.of("status", "qualified")),
                new DataTableRow(2L, Map.of("status", "new"))));

        List<Map<String, Object>> rowMaps = DataTableQuerySupport.queryRowMaps(
            dataTableRowService, "contacts", "status = 'qualified'", 50, 0L);

        assertThat(rowMaps).hasSize(1);
        assertThat(rowMaps.getFirst()).containsEntry("status", "qualified");
    }

    @Test
    void testQueryRowMapsThrowsOnMalformedWhere() {
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);

        when(dataTableRowService.listRows("contacts", 50, 0, 0L)).thenReturn(List.of());

        assertThatExceptionOfType(WhereParseException.class)
            .isThrownBy(
                () -> DataTableQuerySupport.queryRowMaps(dataTableRowService, "contacts", "no operator", 50, 0L));
    }
}
