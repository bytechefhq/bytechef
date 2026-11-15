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
import static com.bytechef.component.datatable.constant.DataTableConstants.SORT_COLUMN;
import static com.bytechef.component.datatable.constant.DataTableConstants.SORT_DIRECTION;
import static com.bytechef.component.datatable.constant.DataTableConstants.TABLE;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
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
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort.Direction;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Find Records: Find records in a table matching conditions, with sorting and pagination
 *
 * @author Ivica Cardic
 */
public class DataTableFindRecordsAction {

    private final DataTableService dataTableService;
    private final DataTableRowService dataTableRowService;
    private final DataTableWorkspaceResolver dataTableWorkspaceResolver;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(
        DataTableService dataTableService, DataTableRowService dataTableRowService,
        DataTableWorkspaceResolver dataTableWorkspaceResolver) {

        return new DataTableFindRecordsAction(dataTableService, dataTableRowService, dataTableWorkspaceResolver)
            .build();
    }

    private DataTableFindRecordsAction(DataTableService dataTableService, DataTableRowService dataTableRowService,
        DataTableWorkspaceResolver dataTableWorkspaceResolver) {
        this.dataTableService = dataTableService;
        this.dataTableRowService = dataTableRowService;
        this.dataTableWorkspaceResolver = dataTableWorkspaceResolver;
    }

    private ModifiableActionDefinition build() {
        return action("findRecords")
            .title("Find Records")
            .description("Find records in a table that match conditions")
            .properties(
                string(TABLE)
                    .label("Table")
                    .resourceReference(ResourceType.DATA_TABLE)
                    .required(true)
                    .options(DataTableUtils.getActionTableOptions(dataTableService, dataTableWorkspaceResolver)),
                DataTableUtils.conditionsProperty(dataTableService, dataTableWorkspaceResolver)
                    .description(
                        "OR Condition array that contains AND Condition arrays. A record is returned when it meets " +
                            "every condition of at least one AND Condition. Leave empty to return every record, up " +
                            "to Limit."),
                string(SORT_COLUMN)
                    .label("Sort Column")
                    .description("Column to sort the records by. Records are sorted by id when not set.")
                    .options(DataTableUtils.getActionColumnOptions(dataTableService, dataTableWorkspaceResolver))
                    .optionsLookupDependsOn(TABLE),
                string(SORT_DIRECTION)
                    .label("Sort Direction")
                    .description(
                        "Order of the Sort Column: Ascending or Descending. Has no effect when Sort Column is not set.")
                    .options(
                        option("Ascending", Direction.ASC.name()),
                        option("Descending", Direction.DESC.name()))
                    .defaultValue(Direction.ASC.name()),
                DataTableUtils.expandProperty(dataTableService, dataTableWorkspaceResolver),
                integer(LIMIT)
                    .label("Limit")
                    .description("Maximum number of records to return")
                    .defaultValue(100),
                integer(OFFSET)
                    .label("Offset")
                    .description("Number of records to skip")
                    .defaultValue(0))
            .output(this::output)
            .help("", "https://docs.bytechef.io/reference/components/data-table_v1#find-records")
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        String name = inputParameters.getRequiredString(TABLE);

        long workspaceId = DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, actionContext);

        DataTableInfo dataTableInfo = DataTableUtils.resolveDataTable(
            dataTableService, workspaceId, name, DEVELOPMENT.ordinal());

        List<String> expand = DataTableUtils.getExpand(inputParameters);

        var rowSchema = DataTableUtils.rowObjectSchema(dataTableInfo, expand);

        List<DataTableRow> rows = dataTableRowService.listRows(dataTableInfo.id(), 1, 0, DEVELOPMENT.ordinal());

        if (rows.isEmpty()) {
            return OutputResponse.of(array().items((Property.ValueProperty<?>) rowSchema));
        }

        DataTableRow firstRow = rows.getFirst();

        Map<String, Object> sampleOutput = DataTableUtils.createSampleOutput(
            dataTableInfo, firstRow.id(), firstRow.values(), expand);

        return OutputResponse.of(array().items((Property.ValueProperty<?>) rowSchema), List.of(sampleOutput));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        String name = inputParameters.getRequiredString(TABLE);

        long dataTableId = DataTableUtils.resolveDataTableId(
            dataTableService, dataTableWorkspaceResolver, actionContext, name);
        int limit = inputParameters.getInteger(LIMIT, 100);
        int offset = inputParameters.getInteger(OFFSET, 0);

        if (limit < 0 || offset < 0) {
            throw new IllegalArgumentException("Limit and Offset must not be negative");
        }

        DataTableRowConditions conditions = DataTableUtils.getConditions(inputParameters);

        String sortColumn = inputParameters.getString(SORT_COLUMN);

        List<DataTableRowSort> sorts = List.of();

        if (sortColumn != null && !sortColumn.isBlank()) {
            Direction direction = DataTableUtils.toEnum(
                Direction.class, inputParameters.getString(SORT_DIRECTION), Direction.ASC, "Sort Direction");

            sorts = List.of(new DataTableRowSort(sortColumn, direction));
        }

        List<String> expand = DataTableUtils.getExpand(inputParameters);

        return DataTableUtils.flattenRows(
            dataTableRowService.findRows(
                dataTableId, conditions, sorts, limit, offset, expand,
                Objects.requireNonNull(actionContextAware.getEnvironmentId())));
    }
}
