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

import static com.bytechef.component.datatable.constant.DataTableConstants.ID;
import static com.bytechef.component.datatable.constant.DataTableConstants.TABLE;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import static com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT;

import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.execution.domain.DataTableRow;
import com.bytechef.automation.data.table.execution.service.DataTableRowService;
import com.bytechef.component.datatable.util.DataTableUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Get Record: Get a single record by its id
 *
 * @author Ivica Cardic
 */
public class DataTableGetRecordAction {

    private final DataTableService dataTableService;
    private final DataTableRowService dataTableRowService;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(
        DataTableService dataTableService, DataTableRowService dataTableRowService) {

        return new DataTableGetRecordAction(dataTableService, dataTableRowService).build();
    }

    private DataTableGetRecordAction(DataTableService dataTableService, DataTableRowService dataTableRowService) {
        this.dataTableService = dataTableService;
        this.dataTableRowService = dataTableRowService;
    }

    private ModifiableActionDefinition build() {
        return action("getRecord")
            .title("Get Record")
            .description("Get single record by its id")
            .properties(
                string(TABLE)
                    .label("Table")
                    .required(true)
                    .options(DataTableUtils.getActionTableOptions(dataTableService)),
                integer(ID)
                    .label("Record ID")
                    .required(true))
            .output(this::output)
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        String baseName = inputParameters.getRequiredString(TABLE);

        var rowSchema = DataTableUtils.rowObjectSchema(dataTableService, DEVELOPMENT, baseName);

        List<DataTableRow> rows = dataTableRowService.listRows(baseName, 1, 0, DEVELOPMENT.ordinal());

        if (rows.isEmpty()) {
            return OutputResponse.of(rowSchema);
        }

        DataTableRow firstRow = rows.getFirst();

        Map<String, Object> sampleOutput = DataTableUtils.createSampleOutput(
            dataTableService, DEVELOPMENT, baseName, firstRow.id(), firstRow.values());

        return OutputResponse.of(rowSchema, sampleOutput);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        String baseName = inputParameters.getRequiredString(TABLE);
        long id = inputParameters.getRequiredLong(ID);

        return dataTableRowService.getRow(
            baseName, id, Objects.requireNonNull(actionContextAware.getEnvironmentId()));
    }
}
