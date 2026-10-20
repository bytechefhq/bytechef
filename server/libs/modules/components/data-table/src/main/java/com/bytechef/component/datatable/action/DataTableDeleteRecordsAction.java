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

import static com.bytechef.component.datatable.constant.DataTableConstants.BY_CONDITIONS;
import static com.bytechef.component.datatable.constant.DataTableConstants.BY_IDS;
import static com.bytechef.component.datatable.constant.DataTableConstants.DELETE_BY;
import static com.bytechef.component.datatable.constant.DataTableConstants.IDS;
import static com.bytechef.component.datatable.constant.DataTableConstants.TABLE;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.datatable.util.DataTableUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.definition.BaseProperty.ResourceType;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Delete Record(s): Delete record(s) from a table
 *
 * @author Ivica Cardic
 */
public class DataTableDeleteRecordsAction {

    private final DataTableService dataTableService;
    private final DataTableRowService dataTableRowService;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(
        DataTableService dataTableService, DataTableRowService dataTableRowService) {

        return new DataTableDeleteRecordsAction(dataTableService, dataTableRowService).build();
    }

    private DataTableDeleteRecordsAction(DataTableService dataTableService, DataTableRowService dataTableRowService) {
        this.dataTableService = dataTableService;
        this.dataTableRowService = dataTableRowService;
    }

    private ModifiableActionDefinition build() {
        return action("deleteRecords")
            .title("Delete Record(s)")
            .description("Delete record(s) from a table")
            .properties(
                string(TABLE)
                    .label("Table")
                    .resourceReference(ResourceType.DATA_TABLE)
                    .required(true)
                    .options(DataTableUtils.getActionTableOptions(dataTableService)),
                string(DELETE_BY)
                    .label("Delete By")
                    .description("Whether to delete the records with the given IDs or every record that matches " +
                        "the conditions.")
                    .options(
                        option("Record IDs", BY_IDS),
                        option("Conditions", BY_CONDITIONS))
                    .defaultValue(BY_IDS),
                array(IDS)
                    .label("Record IDs")
                    .description("IDs of records to delete")
                    .displayCondition("%s == '%s'".formatted(DELETE_BY, BY_IDS))
                    .required(true)
                    .items(integer("id")),
                DataTableUtils.conditionsProperty(dataTableService)
                    .description(
                        "OR Condition array that contains AND Condition arrays. Every record that meets every " +
                            "condition of at least one AND Condition is deleted. Use Clear Table to delete every " +
                            "record.")
                    .displayCondition("%s == '%s'".formatted(DELETE_BY, BY_CONDITIONS))
                    .required(true))
            .output(
                outputSchema(
                    object()
                        .properties(
                            integer("deletedCount")
                                .label("Deleted Count"),
                            array("deletedIds")
                                .items(
                                    integer()))))
            .help("", "https://docs.bytechef.io/reference/components/data-table_v1#delete-records")
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        String baseName = inputParameters.getRequiredString(TABLE);
        long environmentId = Objects.requireNonNull(actionContextAware.getEnvironmentId());

        List<Long> ids;

        if (BY_CONDITIONS.equals(inputParameters.getString(DELETE_BY, BY_IDS))) {
            List<List<DataTableRowFilter>> conditions = DataTableUtils.getConditions(inputParameters);

            if (conditions.isEmpty()) {
                throw new IllegalArgumentException(
                    "Delete by conditions requires at least one condition; use Clear Table to delete every record");
            }

            List<DataTableRow> dataTableRows = dataTableRowService.findRows(
                baseName, conditions, null, Integer.MAX_VALUE, 0, environmentId);

            ids = dataTableRows.stream()
                .map(DataTableRow::id)
                .toList();
        } else {
            ids = Arrays.stream(inputParameters.getRequiredArray(IDS))
                .map(id -> (id instanceof Number number) ? number.longValue() : Long.parseLong(String.valueOf(id)))
                .toList();
        }

        List<Long> deletedIds = new ArrayList<>();

        for (long id : ids) {
            if (dataTableRowService.deleteRow(baseName, id, environmentId)) {
                deletedIds.add(id);
            }
        }

        Map<String, Object> result = new HashMap<>();

        result.put("deletedCount", deletedIds.size());
        result.put("deletedIds", deletedIds);

        return result;
    }
}
