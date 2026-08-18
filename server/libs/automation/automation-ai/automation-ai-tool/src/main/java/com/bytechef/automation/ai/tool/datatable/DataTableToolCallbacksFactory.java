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

import com.bytechef.automation.ai.tool.ToolArtifactRecorder;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builds the Data Table tool-callback lists shared by the Copilot panel agents, the AI Hub ASK/BUILD agents (which
 * register the three reads flat and the eight mutations via the searchable tool catalog rather than through a delegate
 * — the former {@code data_table_agent} subagent was dissolved, ticket 732, CRUD-delegate-unwind Task 5), and the
 * management MCP server (which registers all eleven flat — no schema-count pressure there). Read list feeds ASK; write
 * list feeds BUILD.
 *
 * @author Ivica Cardic
 */
public class DataTableToolCallbacksFactory {

    private final WorkspaceDataTableFacade workspaceDataTableFacade;
    private final DataTableService dataTableService;
    private final DataTableRowService dataTableRowService;
    private final @Nullable ToolArtifactRecorder artifactRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DataTableToolCallbacksFactory(
        WorkspaceDataTableFacade workspaceDataTableFacade,
        DataTableService dataTableService,
        DataTableRowService dataTableRowService,
        @Nullable ToolArtifactRecorder artifactRecorder) {

        this.workspaceDataTableFacade = workspaceDataTableFacade;
        this.dataTableService = dataTableService;
        this.dataTableRowService = dataTableRowService;
        this.artifactRecorder = artifactRecorder;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListDataTablesToolCallback(workspaceDataTableFacade));
        toolCallbacks.add(new QueryDataTableToolCallback(dataTableRowService, dataTableService));
        toolCallbacks.add(new AggregateDataTableToolCallback(dataTableRowService, dataTableService));

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(
            new AddDataTableRowToolCallback(dataTableRowService, workspaceDataTableFacade, artifactRecorder));
        toolCallbacks.add(
            new UpdateDataTableRowToolCallback(dataTableRowService, workspaceDataTableFacade, artifactRecorder));
        toolCallbacks.add(
            new DeleteDataTableRowToolCallback(dataTableRowService, workspaceDataTableFacade, artifactRecorder));
        toolCallbacks.add(
            new AddDataTableColumnToolCallback(dataTableService, workspaceDataTableFacade, artifactRecorder));
        toolCallbacks.add(new CreateDataTableToolCallback(workspaceDataTableFacade));
        toolCallbacks.add(new CreateDataTableFromCsvToolCallback(dataTableRowService, workspaceDataTableFacade));
        toolCallbacks.add(new CloneDataTableToolCallback(dataTableService, workspaceDataTableFacade));
        toolCallbacks.add(new DropDataTableToolCallback(workspaceDataTableFacade));

        return toolCallbacks;
    }
}
