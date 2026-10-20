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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableUpdateRecordsActionTest extends AbstractDataTableActionTest {

    private final ModifiableActionDefinition actionDefinition = DataTableUpdateRecordsAction.of(
        dataTableService, dataTableRowService);

    @Test
    void testPerformUpdatesEveryRecordMatchingConditions() throws Exception {
        DataTableRowConditions conditions = new DataTableRowConditions(
            List.of(
                List.of(new DataTableRowFilter("status", Operation.EQUALS, "BOT")),
                List.of(new DataTableRowFilter("priority", Operation.GREATER, "3"))));
        Map<String, Object> values = Map.of("status", "HUMAN");

        when(dataTableRowService.updateRows("conversations", conditions, values, 1L))
            .thenReturn(
                List.of(
                    new DataTableRow(3, Map.of("status", "HUMAN")), new DataTableRow(4, Map.of("status", "HUMAN"))));

        assertEquals(
            Map.of(
                "updatedCount", 2,
                "records", List.of(Map.of("id", 3L, "status", "HUMAN"), Map.of("id", 4L, "status", "HUMAN"))),
            perform(
                actionDefinition,
                Map.of(
                    "table", "conversations",
                    "conditions", List.of(
                        List.of(Map.of("column", "status", "value", "BOT")),
                        List.of(Map.of("column", "priority", "operation", "GREATER", "value", "3"))),
                    "values", values)));
    }

    @Test
    void testPerformRejectsMissingConditions() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> perform(
                actionDefinition,
                Map.of("table", "conversations", "values", Map.of("status", "HUMAN"))));

        assertEquals("Update Records requires at least one condition", illegalArgumentException.getMessage());

        verifyNoInteractions(dataTableRowService);
    }

    @Test
    void testPerformRejectsMissingValues() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> perform(
                actionDefinition,
                Map.of(
                    "table", "conversations",
                    "conditions", List.of(List.of(Map.of("column", "status", "value", "BOT"))),
                    "values", Map.of())));

        assertEquals("Update Records requires a value for at least one column", illegalArgumentException.getMessage());

        verifyNoInteractions(dataTableRowService);
    }
}
