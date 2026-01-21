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

import static com.bytechef.component.datatable.constant.DataTableConstants.LIMIT;
import static com.bytechef.component.datatable.constant.DataTableConstants.OFFSET;
import static com.bytechef.component.datatable.constant.DataTableConstants.TABLE;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
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
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.definition.ActionContextAware;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Find Records: Find records in a table with pagination
 *
 * @author Ivica Cardic
 */
public class DataTableFindRecordsAction {

    public final ModifiableActionDefinition actionDefinition;

    private final DataTableService dataTableService;
    private final DataTableRowService dataTableRowService;

    @SuppressFBWarnings("EI")
    public DataTableFindRecordsAction(
        DataTableService dataTableService,
        DataTableRowService dataTableRowService) {

        this.dataTableService = dataTableService;
        this.dataTableRowService = dataTableRowService;
        this.actionDefinition = build();
    }

    private ModifiableActionDefinition build() {
        return action("findRecords")
            .title("Find Records")
            .description("Find records in a table with filters")
            .properties(
                string(TABLE)
                    .label("Table")
                    .required(true)
                    .options(DataTableUtils.getActionTableOptions(dataTableService)),
                integer(LIMIT)
                    .label("Limit")
                    .description("Maximum number of records to return")
                    .defaultValue(100),
                integer(OFFSET)
                    .label("Offset")
                    .description("Number of records to skip")
                    .defaultValue(0))
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
            return OutputResponse.of(array().items((Property.ValueProperty<?>) rowSchema));
        }

        DataTableRow firstRow = rows.getFirst();

        Map<String, Object> sampleOutput = DataTableUtils.createSampleOutput(
            dataTableService, DEVELOPMENT, baseName, firstRow.id(), firstRow.values());

        return OutputResponse.of(array().items((Property.ValueProperty<?>) rowSchema), List.of(sampleOutput));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        String baseName = inputParameters.getRequiredString(TABLE);
        int limit = inputParameters.getInteger(LIMIT, 100);
        int offset = inputParameters.getInteger(OFFSET, 0);

        return dataTableRowService.listRows(
            baseName, limit, offset, Objects.requireNonNull(actionContextAware.getEnvironmentId()));
    }
}
