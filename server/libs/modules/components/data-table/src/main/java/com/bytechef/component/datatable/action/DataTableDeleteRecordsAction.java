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

import static com.bytechef.component.datatable.constant.DataTableConstants.CONDITIONS;
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
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Delete Record(s): Delete records by ID or every record that matches conditions
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
            .description("Delete records from a table by ID or by conditions")
            .properties(
                string(TABLE)
                    .label("Table")
                    .resourceReference(ResourceType.DATA_TABLE)
                    .required(true)
                    .options(DataTableUtils.getActionTableOptions(dataTableService)),
                string(DELETE_BY)
                    .label("Delete By")
                    .description("Whether to delete the records with the given IDs or every record that matches " +
                        "the conditions. The editor asks for Record IDs until Conditions is chosen. In a workflow " +
                        "definition that leaves it out, conditions are used if only OR Conditions is given and " +
                        "Record IDs otherwise; it must be set when both are given.")
                    .options(
                        option("Record IDs", DeleteBy.IDS.name()),
                        option("Conditions", DeleteBy.CONDITIONS.name())),
                array(IDS)
                    .label("Record IDs")
                    .description(
                        "IDs of records to delete; required unless Delete By is Conditions, and ignored when it is.")
                    .displayCondition("%s != '%s'".formatted(DELETE_BY, DeleteBy.CONDITIONS))
                    .required(true)
                    .items(integer("id")),
                DataTableUtils.conditionsProperty(dataTableService)
                    .description(
                        "OR Condition array that contains AND Condition arrays; required when Delete By is " +
                            "Conditions. Every record that meets every " +
                            "condition of at least one AND Condition is deleted. Use Clear Table to delete every " +
                            "record.")
                    .displayCondition("%s == '%s'".formatted(DELETE_BY, DeleteBy.CONDITIONS))
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

        if (getDeleteBy(inputParameters) == DeleteBy.CONDITIONS) {
            DataTableRowConditions conditions = DataTableUtils.getConditions(inputParameters);

            if (conditions.isMatchAll()) {
                throw new IllegalArgumentException(
                    "Delete by conditions requires at least one condition; use Clear Table to delete every record");
            }

            return deleteRows(baseName, conditions, environmentId);
        }

        List<Long> ids = Arrays.stream(inputParameters.getRequiredArray(IDS))
            .map(DataTableDeleteRecordsAction::toId)
            .distinct()
            .toList();

        if (ids.isEmpty()) {
            return toResult(List.of());
        }

        DataTableRowConditions conditions = DataTableRowConditions.of(
            List.of(new DataTableRowFilter("id", Operation.IN, ids)));

        return deleteRows(baseName, conditions, environmentId);
    }

    private Map<String, Object> deleteRows(String baseName, DataTableRowConditions conditions, long environmentId) {
        List<Long> deletedIds = dataTableRowService.deleteRows(baseName, conditions, environmentId)
            .stream()
            .map(DataTableRow::id)
            .toList();

        return toResult(deletedIds);
    }

    private static DeleteBy getDeleteBy(Parameters inputParameters) {
        String deleteBy = inputParameters.getString(DELETE_BY);

        if (deleteBy == null || deleteBy.isBlank()) {
            Object conditions = inputParameters.get(CONDITIONS);

            boolean hasConditions = conditions != null &&
                !(conditions instanceof Collection<?> collection && collection.isEmpty());
            boolean hasIds = inputParameters.containsKey(IDS);

            if (hasConditions && hasIds) {
                throw new IllegalArgumentException(
                    "Delete Records has both Record IDs and OR Conditions; set Delete By to %s or %s".formatted(
                        DeleteBy.IDS, DeleteBy.CONDITIONS));
            }

            return hasConditions ? DeleteBy.CONDITIONS : DeleteBy.IDS;
        }

        return DataTableUtils.toEnum(DeleteBy.class, deleteBy, DeleteBy.IDS, "Delete By");
    }

    private static long toId(Object id) {
        BigDecimal bigDecimal;

        try {
            bigDecimal = new BigDecimal(String.valueOf(id)
                .trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Record ID '%s' is not a number".formatted(id), exception);
        }

        try {
            return bigDecimal.longValueExact();
        } catch (ArithmeticException exception) {
            BigDecimal strippedBigDecimal = bigDecimal.stripTrailingZeros();

            String reason = strippedBigDecimal.scale() > 0 ? "is not a whole number" : "is out of range";

            throw new IllegalArgumentException("Record ID '%s' %s".formatted(id, reason), exception);
        }
    }

    private static Map<String, Object> toResult(List<Long> deletedIds) {
        Map<String, Object> result = new HashMap<>();

        result.put("deletedCount", deletedIds.size());
        result.put("deletedIds", deletedIds);

        return result;
    }

    private enum DeleteBy {

        IDS,
        CONDITIONS
    }
}
