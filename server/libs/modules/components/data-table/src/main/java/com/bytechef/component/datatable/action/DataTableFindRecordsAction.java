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

import static com.bytechef.component.datatable.constant.DataTableConstants.DIRECTION;
import static com.bytechef.component.datatable.constant.DataTableConstants.FIELD;
import static com.bytechef.component.datatable.constant.DataTableConstants.FILTERS;
import static com.bytechef.component.datatable.constant.DataTableConstants.LIMIT;
import static com.bytechef.component.datatable.constant.DataTableConstants.OFFSET;
import static com.bytechef.component.datatable.constant.DataTableConstants.OPERATOR;
import static com.bytechef.component.datatable.constant.DataTableConstants.SORTS;
import static com.bytechef.component.datatable.constant.DataTableConstants.TABLE;
import static com.bytechef.component.datatable.constant.DataTableConstants.VALUE;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import static com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT;

import com.bytechef.component.datatable.util.DataTableUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.owner.OwnerResolution;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowOwnerFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.owner.OwnerResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Find Records: Find records in a table with pagination
 *
 * @author Ivica Cardic
 */
public class DataTableFindRecordsAction {

    private final DataTableService dataTableService;
    private final DataTableRowService dataTableRowService;
    private final ObjectProvider<OwnerResolver> ownerResolverProvider;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(
        DataTableService dataTableService, DataTableRowService dataTableRowService,
        ObjectProvider<OwnerResolver> ownerResolverProvider) {

        return new DataTableFindRecordsAction(dataTableService, dataTableRowService, ownerResolverProvider).build();
    }

    private DataTableFindRecordsAction(
        DataTableService dataTableService, DataTableRowService dataTableRowService,
        ObjectProvider<OwnerResolver> ownerResolverProvider) {

        this.dataTableService = dataTableService;
        this.dataTableRowService = dataTableRowService;
        this.ownerResolverProvider = ownerResolverProvider;
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
                array(FILTERS)
                    .label("Filters")
                    .description(
                        "Conditions a record must meet. All conditions must match. Leave empty to return every " +
                            "record.")
                    .items(
                        object()
                            .properties(
                                string(FIELD)
                                    .label("Column")
                                    .description("The column to test.")
                                    .required(true),
                                string(OPERATOR)
                                    .label("Operator")
                                    .defaultValue("EQ")
                                    .options(
                                        option("Equals", "EQ"),
                                        option("Not Equals", "NEQ"),
                                        option("Contains", "CONTAINS"),
                                        option("Starts With", "STARTS_WITH"),
                                        option("Greater Than", "GT"),
                                        option("Greater Than Or Equal", "GTE"),
                                        option("Less Than", "LT"),
                                        option("Less Than Or Equal", "LTE"),
                                        option("In", "IN"),
                                        option("Between", "BETWEEN"))
                                    .required(true),
                                string(VALUE)
                                    .label("Value")
                                    .description(
                                        "The value to compare against. For In and Between, a comma-separated list.")
                                    .required(false)))
                    .required(false),
                array(SORTS)
                    .label("Sort")
                    .description(
                        "How to order the records. Sorting by Id descending returns the newest first without " +
                            "needing a timestamp column. Records are ordered by Id when this is empty.")
                    .items(
                        object()
                            .properties(
                                string(FIELD)
                                    .label("Column")
                                    .description("The column to order by.")
                                    .required(true),
                                string(DIRECTION)
                                    .label("Direction")
                                    .defaultValue("ASC")
                                    .options(
                                        option("Ascending", "ASC"),
                                        option("Descending", "DESC"))
                                    .required(true)))
                    .required(false),
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

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        String baseName = inputParameters.getRequiredString(TABLE);

        var rowSchema = DataTableUtils.rowObjectSchema(dataTableService, DEVELOPMENT, baseName);

        RowOwnerFilter rowOwnerFilter = RowOwnerFilter.from(
            OwnerResolution.resolve(actionContextAware, ownerResolverProvider));

        List<DataTableRow> rows = dataTableRowService.listRows(
            baseName, 1, 0, DEVELOPMENT.ordinal(), rowOwnerFilter);

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

        RowOwnerFilter rowOwnerFilter = RowOwnerFilter.from(
            OwnerResolution.resolve(actionContextAware, ownerResolverProvider));

        List<RowFilter> rowFilters = toRowFilters(
            inputParameters.getList(FILTERS, new TypeReference<Map<String, Object>>() {}, List.of()));
        List<RowSort> rowSorts = toRowSorts(
            inputParameters.getList(SORTS, new TypeReference<Map<String, Object>>() {}, List.of()));

        return dataTableRowService.listRows(
            baseName, limit, offset, Objects.requireNonNull(actionContextAware.getEnvironmentId()), rowOwnerFilter,
            rowFilters, rowSorts);
    }

    static List<RowSort> toRowSorts(List<Map<String, Object>> sortEntries) {
        List<RowSort> rowSorts = new ArrayList<>();

        for (Map<String, Object> sortEntry : sortEntries) {
            Object field = sortEntry.get(FIELD);

            if (field == null || String.valueOf(field)
                .isBlank()) {
                continue;
            }

            Object direction = sortEntry.get(DIRECTION);

            rowSorts.add(
                new RowSort(
                    String.valueOf(field),
                    direction == null
                        ? RowSort.Direction.ASC
                        : RowSort.Direction.valueOf(String.valueOf(direction))));
        }

        return rowSorts;
    }

    /**
     * An entry with no column names nothing and is dropped rather than failing the step -- the editor leaves a blank
     * row behind whenever someone adds a filter and changes their mind.
     */
    static List<RowFilter> toRowFilters(List<Map<String, Object>> filterEntries) {
        List<RowFilter> rowFilters = new ArrayList<>();

        for (Map<String, Object> filterEntry : filterEntries) {
            Object field = filterEntry.get(FIELD);

            if (field == null || String.valueOf(field)
                .isBlank()) {
                continue;
            }

            Object operator = filterEntry.get(OPERATOR);
            RowFilter.Operator rowFilterOperator = operator == null
                ? RowFilter.Operator.EQ
                : RowFilter.Operator.valueOf(String.valueOf(operator));

            rowFilters.add(
                new RowFilter(String.valueOf(field), rowFilterOperator, value(rowFilterOperator, filterEntry)));
        }

        return rowFilters;
    }

    /**
     * In and Between read their single text field as a comma-separated list, which is the one place this grammar is
     * lossy: a value containing a comma cannot be expressed.
     */
    private static @Nullable Object value(RowFilter.Operator operator, Map<String, Object> filterEntry) {
        Object value = filterEntry.get(VALUE);

        if (operator != RowFilter.Operator.IN && operator != RowFilter.Operator.BETWEEN) {
            return value;
        }

        return Arrays.stream(String.valueOf(value)
            .split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .toList();
    }
}
