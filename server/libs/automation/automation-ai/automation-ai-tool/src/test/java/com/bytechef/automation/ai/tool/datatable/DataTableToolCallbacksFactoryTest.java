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

import com.bytechef.automation.ai.tool.ToolArtifactRecorder;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.tool.ToolCallback;

/**
 *
 * @author Ivica Cardic
 */
class DataTableToolCallbacksFactoryTest {

    private final DataTableToolCallbacksFactory factory = new DataTableToolCallbacksFactory(
        Mockito.mock(WorkspaceDataTableFacade.class),
        Mockito.mock(DataTableService.class),
        Mockito.mock(DataTableRowService.class),
        Mockito.mock(ToolArtifactRecorder.class));

    @Test
    void readListExcludesMutations() {
        List<String> names = toolNames(factory.readToolCallbacks());

        assertThat(names).contains("listDataTables", "queryDataTable", "aggregateDataTable");
        assertThat(names).doesNotContain("dropDataTable", "createDataTable");
    }

    @Test
    void writeListIncludesReadsAndMutations() {
        List<String> names = toolNames(factory.writeToolCallbacks());

        assertThat(names).contains(
            "listDataTables", "queryDataTable", "aggregateDataTable", "addDataTableRow", "updateDataTableRow",
            "deleteDataTableRow", "addDataTableColumn", "createDataTable", "createDataTableFromCsv",
            "cloneDataTable", "dropDataTable");
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toList());
    }
}
