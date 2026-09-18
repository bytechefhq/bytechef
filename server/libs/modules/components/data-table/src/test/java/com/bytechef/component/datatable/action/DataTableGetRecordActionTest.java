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

package com.bytechef.component.datatable.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableGetRecordActionTest extends AbstractDataTableActionTest {

    @Test
    void testPerformEmitsFlatRow() throws Exception {
        DataTableRef dataTableRef = stubResolvedDataTable();

        when(dataTableRowService.getRow(eq(dataTableRef), anyLong()))
            .thenReturn(new DataTableRow(7, Map.of("staff_reply", "on my way")));

        assertEquals(
            Map.of("id", 7L, "staff_reply", "on my way"),
            perform(createActionDefinition(), Map.of("table", TABLE_NAME, "id", 7)));
    }

    @Test
    void testPerformOfMissingRecordStaysNull() throws Exception {
        DataTableRef dataTableRef = stubResolvedDataTable();

        when(dataTableRowService.getRow(eq(dataTableRef), anyLong())).thenReturn(null);

        assertNull(perform(createActionDefinition(), Map.of("table", TABLE_NAME, "id", 7)));
    }

    @Override
    protected ModifiableActionDefinition createActionDefinition() {
        return DataTableGetRecordAction.of(dataTableService, dataTableRowService, dataTableWorkspaceResolver);
    }

    @Override
    protected Map<String, Object> createInputParameters(String tableName) {
        return Map.of("table", tableName, "id", 7);
    }
}
