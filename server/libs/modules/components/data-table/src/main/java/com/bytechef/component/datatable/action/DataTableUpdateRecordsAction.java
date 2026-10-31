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

import static com.bytechef.component.datatable.constant.DataTableConstants.RECORDS;
import static com.bytechef.component.datatable.constant.DataTableConstants.TABLE;
import static com.bytechef.component.datatable.constant.DataTableConstants.VALUES;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import static com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT;

import com.bytechef.component.datatable.util.DataTableUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.definition.BaseProperty.ResourceType;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Update Records: Update values in every record that matches conditions
 *
 * @author Ivica Cardic
 */
public class DataTableUpdateRecordsAction {

    private static final String UPDATED_COUNT = "updatedCount";

    private final DataTableService dataTableService;
    private final DataTableRowService dataTableRowService;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(
        DataTableService dataTableService, DataTableRowService dataTableRowService) {

        return new DataTableUpdateRecordsAction(dataTableService, dataTableRowService).build();
    }

    private DataTableUpdateRecordsAction(DataTableService dataTableService, DataTableRowService dataTableRowService) {
        this.dataTableService = dataTableService;
        this.dataTableRowService = dataTableRowService;
    }

    private ModifiableActionDefinition build() {
        return action("updateRecords")
            .title("Update Records")
            .description("Update values in every record that matches conditions")
            .properties(
                string(TABLE)
                    .label("Table")
                    .resourceReference(ResourceType.DATA_TABLE)
                    .required(true)
                    .options(DataTableUtils.getActionTableOptions(dataTableService)),
                DataTableUtils.conditionsProperty(dataTableService)
                    .description(
                        "OR Condition array that contains AND Condition arrays. Every record that meets every " +
                            "condition of at least one AND Condition is updated.")
                    .required(true),
                dynamicProperties(VALUES)
                    .propertiesLookupDependsOn(TABLE)
                    .properties(DataTableUtils.createDynamicProperties(dataTableService, true))
                    .required(true))
            .output(this::output)
            .help("", "https://docs.bytechef.io/reference/components/data-table_v1#update-records")
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        String baseName = inputParameters.getRequiredString(TABLE);

        var rowSchema = DataTableUtils.rowObjectSchema(dataTableService, DEVELOPMENT, baseName);

        var outputSchema = object()
            .properties(
                integer(UPDATED_COUNT)
                    .label("Updated Count"),
                array(RECORDS)
                    .label("Records")
                    .items((Property.ValueProperty<?>) rowSchema));

        List<DataTableRow> rows = dataTableRowService.listRows(baseName, 1, 0, DEVELOPMENT.ordinal());

        if (rows.isEmpty()) {
            return OutputResponse.of(outputSchema);
        }

        DataTableRow firstRow = rows.getFirst();

        Map<String, Object> sampleRecord = DataTableUtils.createSampleOutput(
            dataTableService, DEVELOPMENT, baseName, firstRow.id(), firstRow.values());

        return OutputResponse.of(outputSchema, Map.of(UPDATED_COUNT, 1, RECORDS, List.of(sampleRecord)));
    }

    @SuppressWarnings({
        "unchecked", "PMD.UnusedFormalParameter"
    })
    private Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        String baseName = inputParameters.getRequiredString(TABLE);
        long environmentId = Objects.requireNonNull(actionContextAware.getEnvironmentId());
        Map<String, Object> values = (Map<String, Object>) inputParameters.getRequired(VALUES, Map.class);

        DataTableRowConditions conditions = DataTableUtils.getConditions(inputParameters);

        if (conditions.isMatchAll()) {
            throw new IllegalArgumentException("Update Records requires at least one condition");
        }

        if (values.isEmpty()) {
            throw new IllegalArgumentException("Update Records requires a value for at least one column");
        }

        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            baseName, conditions, values, environmentId);

        Map<String, Object> result = new HashMap<>();

        result.put(UPDATED_COUNT, updatedDataTableRows.size());
        result.put(RECORDS, DataTableUtils.flattenRows(updatedDataTableRows));

        return result;
    }
}
