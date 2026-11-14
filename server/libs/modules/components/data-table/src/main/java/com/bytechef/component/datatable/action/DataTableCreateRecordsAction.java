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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import static com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT;

import com.bytechef.component.datatable.util.DataTableUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.definition.BaseProperty.ResourceType;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Create Record(s): Insert one or more new records to a table
 *
 * @author Ivica Cardic
 */
public class DataTableCreateRecordsAction {

    private final DataTableService dataTableService;
    private final DataTableRowService dataTableRowService;
    private final DataTableWorkspaceResolver dataTableWorkspaceResolver;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(
        DataTableService dataTableService, DataTableRowService dataTableRowService,
        DataTableWorkspaceResolver dataTableWorkspaceResolver) {

        return new DataTableCreateRecordsAction(dataTableService, dataTableRowService, dataTableWorkspaceResolver)
            .build();
    }

    private DataTableCreateRecordsAction(DataTableService dataTableService, DataTableRowService dataTableRowService,
        DataTableWorkspaceResolver dataTableWorkspaceResolver) {
        this.dataTableService = dataTableService;
        this.dataTableRowService = dataTableRowService;
        this.dataTableWorkspaceResolver = dataTableWorkspaceResolver;
    }

    private ModifiableActionDefinition build() {
        return action("createRecords")
            .title("Create Record(s)")
            .description(
                "Insert one or more new records to a table. The records are inserted together: a record that is not " +
                    "valid stops the step and no record is inserted.")
            .properties(
                string(TABLE)
                    .label("Table")
                    .resourceReference(ResourceType.DATA_TABLE)
                    .required(true)
                    .options(DataTableUtils.getActionTableOptions(dataTableService, dataTableWorkspaceResolver)),
                dynamicProperties(RECORDS,
                    DataTableUtils.createDynamicProperties(dataTableService, dataTableWorkspaceResolver, false))
                        .propertiesLookupDependsOn(TABLE)
                        .required(true))
            .output(this::output)
            .help("", "https://docs.bytechef.io/reference/components/data-table_v1#create-records")
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        String name = inputParameters.getRequiredString(TABLE);

        long workspaceId = DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, actionContext);

        DataTableInfo dataTableInfo = DataTableUtils.resolveDataTable(
            dataTableService, workspaceId, name, DEVELOPMENT.ordinal());

        var rowSchema = DataTableUtils.rowObjectSchema(dataTableInfo);

        List<DataTableRow> rows = dataTableRowService.listRows(dataTableInfo.id(), 1, 0, DEVELOPMENT.ordinal());

        if (rows.isEmpty()) {
            return OutputResponse.of(array().items((Property.ValueProperty<?>) rowSchema));
        }

        DataTableRow firstRow = rows.getFirst();

        Map<String, Object> sampleOutput = DataTableUtils.createSampleOutput(
            dataTableInfo, firstRow.id(), firstRow.values());

        return OutputResponse.of(array().items((Property.ValueProperty<?>) rowSchema), List.of(sampleOutput));
    }

    @SuppressWarnings({
        "unchecked", "PMD.UnusedFormalParameter"
    })
    private Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        String name = inputParameters.getRequiredString(TABLE);

        long dataTableId = DataTableUtils.resolveDataTableId(
            dataTableService, dataTableWorkspaceResolver, actionContext, name);

        Map<String, Object> recordsMap = inputParameters.getRequiredMap(RECORDS, Object.class);
        Object valuesObj = recordsMap.get(VALUES);

        if (!(valuesObj instanceof List<?> valuesList)) {
            throw new IllegalArgumentException("Records must be a list of records; no record was inserted");
        }

        List<Map<String, Object>> rowValues = new ArrayList<>();

        for (int index = 0; index < valuesList.size(); index++) {
            if (!(valuesList.get(index) instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException(
                    "Record %d must be an object of column values; no record was inserted".formatted(index + 1));
            }

            rowValues.add((Map<String, Object>) map);
        }

        List<DataTableRow> createdDataTableRows = dataTableRowService.insertRows(
            dataTableId, rowValues, Objects.requireNonNull(actionContextAware.getEnvironmentId()));

        return DataTableUtils.flattenRows(createdDataTableRows);
    }
}
