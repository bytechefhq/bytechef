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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableUpdateRecordActionTest extends AbstractDataTableActionTest {

    @Test
    void testPerformEmitsFlatRow() throws Exception {
        when(dataTableRowService.updateRow(anyString(), anyLong(), anyMap(), anyLong()))
            .thenReturn(new DataTableRow(7, Map.of("status", "BOT")));

        ModifiableActionDefinition actionDefinition = DataTableUpdateRecordAction.of(
            dataTableService, dataTableRowService);

        assertEquals(
            Map.of("id", 7L, "status", "BOT"),
            perform(
                actionDefinition,
                Map.of("table", "conversations", "id", 7, "values", Map.of("status", "BOT"))));
    }
}
