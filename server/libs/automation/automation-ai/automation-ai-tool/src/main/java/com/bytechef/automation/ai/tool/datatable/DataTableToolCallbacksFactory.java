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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
public class DataTableToolCallbacksFactory {

    private final WorkspaceDataTableFacade workspaceDataTableFacade;
    private final @Nullable ToolArtifactRecorder artifactRecorder;

    public DataTableToolCallbacksFactory(WorkspaceDataTableFacade workspaceDataTableFacade) {
        this(workspaceDataTableFacade, null);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DataTableToolCallbacksFactory(
        WorkspaceDataTableFacade workspaceDataTableFacade, @Nullable ToolArtifactRecorder artifactRecorder) {

        this.workspaceDataTableFacade = workspaceDataTableFacade;
        this.artifactRecorder = artifactRecorder;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListDataTablesToolCallback(workspaceDataTableFacade));
        toolCallbacks.add(new QueryDataTableToolCallback(workspaceDataTableFacade));
        toolCallbacks.add(new AggregateDataTableToolCallback(workspaceDataTableFacade));

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(new AddDataTableRowToolCallback(workspaceDataTableFacade, artifactRecorder));
        toolCallbacks.add(new UpdateDataTableRowToolCallback(workspaceDataTableFacade, artifactRecorder));
        toolCallbacks.add(new DeleteDataTableRowToolCallback(workspaceDataTableFacade, artifactRecorder));
        toolCallbacks.add(new AddDataTableColumnToolCallback(workspaceDataTableFacade, artifactRecorder));
        toolCallbacks.add(new CreateDataTableToolCallback(workspaceDataTableFacade));
        toolCallbacks.add(new CreateDataTableFromCsvToolCallback(workspaceDataTableFacade));
        toolCallbacks.add(new CloneDataTableToolCallback(workspaceDataTableFacade));
        toolCallbacks.add(new DropDataTableToolCallback(workspaceDataTableFacade));

        return toolCallbacks;
    }
}
