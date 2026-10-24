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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableClearTableActionTest extends AbstractDataTableActionTest {

    @Test
    void testPerformDeletesAllRowsInOneStep() throws Exception {
        when(dataTableRowService.deleteAllRows("conversations", 1L))
            .thenReturn(List.of(new DataTableRow(1, Map.of()), new DataTableRow(2, Map.of())));

        assertEquals(
            Map.of("deletedCount", 2),
            perform(
                DataTableClearTableAction.of(dataTableService, dataTableRowService), Map.of("table", "conversations")));

        verify(dataTableRowService, never()).listRows(anyString(), anyInt(), anyInt(), anyLong());
        verify(dataTableRowService, never()).deleteRow(anyString(), anyLong(), anyLong());
    }
}
