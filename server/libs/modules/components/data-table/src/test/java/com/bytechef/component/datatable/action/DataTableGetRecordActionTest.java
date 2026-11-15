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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableGetRecordActionTest extends AbstractDataTableActionTest {

    @Test
    void testPerformEmitsFlatRow() throws Exception {
        when(dataTableRowService.getRow(anyLong(), anyLong(), any(), anyLong()))
            .thenReturn(new DataTableRow(7, Map.of("staff_reply", "on my way")));

        ModifiableActionDefinition actionDefinition = DataTableGetRecordAction.of(
            dataTableService, dataTableRowService, dataTableWorkspaceResolver);

        assertEquals(
            Map.of("id", 7L, "staff_reply", "on my way"),
            perform(actionDefinition, Map.of("table", "conversations", "id", 7)));
    }

    @Test
    void testGetRecordPassesExpand() throws Exception {
        when(dataTableRowService.getRow(anyLong(), anyLong(), any(), anyLong()))
            .thenReturn(new DataTableRow(7, Map.of("customer", new DataTableRow(5, "ext-5", Map.of("name", "Ada")))));

        ModifiableActionDefinition actionDefinition = DataTableGetRecordAction.of(
            dataTableService, dataTableRowService, dataTableWorkspaceResolver);

        Object result = perform(
            actionDefinition, Map.of("table", "conversations", "id", 7, "expand", List.of("customer")));

        verify(dataTableRowService).getRow(eq(DATA_TABLE_ID), eq(7L), eq(List.of("customer")), eq(ENVIRONMENT_ID));

        assertEquals(
            Map.of("id", 7L, "customer", Map.of("id", 5L, "externalId", "ext-5", "values", Map.of("name", "Ada"))),
            result);
    }

    @Test
    void testPerformOfMissingRecordStaysNull() throws Exception {
        when(dataTableRowService.getRow(anyLong(), anyLong(), any(), anyLong())).thenReturn(null);

        ModifiableActionDefinition actionDefinition = DataTableGetRecordAction.of(
            dataTableService, dataTableRowService, dataTableWorkspaceResolver);

        assertNull(perform(actionDefinition, Map.of("table", "conversations", "id", 7)));
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
