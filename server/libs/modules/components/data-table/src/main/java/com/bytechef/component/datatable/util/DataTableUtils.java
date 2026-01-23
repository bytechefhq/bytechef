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

package com.bytechef.component.datatable.util;

import static com.bytechef.component.datatable.constant.DataTableConstants.TABLE;
import static com.bytechef.component.datatable.constant.DataTableConstants.VALUES;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import static com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT;

import com.bytechef.automation.data.table.configuration.domain.DataTableInfo;
import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.domain.ColumnSpec;
import com.bytechef.automation.data.table.domain.ColumnType;
import com.bytechef.automation.data.table.execution.domain.DataTableRow;
import com.bytechef.automation.data.table.execution.service.DataTableRowService;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.configuration.domain.Environment;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utility to construct output schemas for Data Table actions using table metadata.
 *
 * @author Ivica Cardic
 */
public final class DataTableUtils {

    private DataTableUtils() {
    }

    /**
     * Returns an OptionsFunction for action table selection dropdowns.
     *
     * @param dataTableService the data table service
     * @return an OptionsFunction that provides table options
     */
    public static ActionDefinition.OptionsFunction<String> getActionTableOptions(DataTableService dataTableService) {
        return (inputParameters, connectionParameters, dependencyPaths, searchText, context) -> getTableOptions(
            searchText, dataTableService);
    }

    /**
     * Returns an OptionsFunction for trigger table selection dropdowns.
     *
     * @param dataTableService the data table service
     * @return an OptionsFunction that provides table options
     */
    public static TriggerDefinition.OptionsFunction<String> getTriggerTableOptions(DataTableService dataTableService) {
        return (inputParameters, connectionParameters, dependencyPaths, searchText, context) -> getTableOptions(
            searchText, dataTableService);
    }

    public static List<Option<String>> getTableOptions(String searchText, DataTableService dataTableService) {
        List<DataTableInfo> dataTableInfos = dataTableService.listTables(DEVELOPMENT.ordinal());

        return dataTableInfos.stream()
            .filter(
                dataTableInfo -> searchText == null || dataTableInfo.baseName()
                    .toLowerCase()
                    .contains(searchText.toLowerCase()))
            .<Option<String>>map(
                dataTableInfo -> option(dataTableInfo.baseName(), dataTableInfo.baseName(),
                    dataTableInfo.description()))
            .toList();
    }

    /**
     * Fetches a DataTableInfo by base name and environment ID.
     *
     * @param dataTableService the data table service
     * @param baseName         the table base name
     * @param environmentId    the environment ID
     * @return the DataTableInfo if found, null otherwise
     */
    @Nullable
    public static DataTableInfo getDataTableInfo(
        DataTableService dataTableService, String baseName, long environmentId) {

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(environmentId);

        return dataTableInfos.stream()
            .filter(dataTableInfo -> {
                String curBaseName = dataTableInfo.baseName();

                return curBaseName.equalsIgnoreCase(baseName);
            })
            .findFirst()
            .orElse(null);
    }

    /**
     * Creates a OutputResponse for a data table trigger, including schema and sample data from the first row.
     *
     * @param dataTableRowService the data table row service
     * @param dataTableService    the data table service
     * @param baseName            the table base name
     * @return an OutputResponse with schema and optional sample data
     */
    public static OutputResponse createTriggerOutputResponse(
        DataTableRowService dataTableRowService, DataTableService dataTableService, String baseName) {

        BaseValueProperty<?> rowSchema = rowObjectSchema(dataTableService, DEVELOPMENT, baseName);

        List<DataTableRow> rows = dataTableRowService.listRows(baseName, 1, 0, DEVELOPMENT.ordinal());

        if (rows.isEmpty()) {
            return OutputResponse.of(rowSchema);
        }

        DataTableRow firstRow = rows.getFirst();

        Map<String, Object> sampleOutput = createSampleOutput(
            dataTableService, DEVELOPMENT, baseName, firstRow.id(), firstRow.values());

        return OutputResponse.of(rowSchema, sampleOutput);
    }

    public static BaseValueProperty<?> rowObjectSchema(
        DataTableService dataTableService, Environment environment, String baseName) {

        DataTableInfo dataTableInfo = getDataTableInfo(dataTableService, baseName, environment.ordinal());

        List<Property.ValueProperty<?>> properties = new ArrayList<>();

        properties.add(integer("id").label("ID"));

        if (dataTableInfo != null && dataTableInfo.columns() != null) {
            for (ColumnSpec columnSpec : dataTableInfo.columns()) {
                properties.add(mapColumn(columnSpec));
            }
        }

        return object().properties(properties.toArray(Property.ValueProperty[]::new));
    }

    /**
     * Creates a sample output map from a row's values, filling in sample values for null columns based on their types.
     *
     * @param dataTableService the data table service
     * @param environment      the environment
     * @param baseName         the table base name
     * @param rowId            the row id
     * @param rowValues        the row values map (may contain null values)
     * @return a map with id and all column values, with sample values for null entries
     */
    public static Map<String, Object> createSampleOutput(
        DataTableService dataTableService, Environment environment, String baseName, long rowId,
        Map<String, Object> rowValues) {

        DataTableInfo dataTableInfo = getDataTableInfo(dataTableService, baseName, environment.ordinal());

        Map<String, Object> sampleOutput = new HashMap<>();

        sampleOutput.put("id", rowId);

        if (dataTableInfo != null && dataTableInfo.columns() != null) {
            for (ColumnSpec columnSpec : dataTableInfo.columns()) {
                String columnName = columnSpec.name();
                Object value = rowValues.get(columnName);

                sampleOutput.put(
                    columnName, Objects.requireNonNullElseGet(value, () -> getSampleValue(columnSpec.type())));
            }
        }

        return sampleOutput;
    }

    /**
     * Creates a PropertiesFunction for dynamic properties lookup based on table columns.
     *
     * @param dataTableService the data table service
     * @return a PropertiesFunction that returns properties based on the selected table
     */
    public static ActionDefinition.PropertiesFunction createDynamicProperties(
        DataTableService dataTableService, boolean singleRecord) {

        return (inputParameters, connectionParameters, dependencyPaths, context) -> {
            String baseName = inputParameters.getString(TABLE);

            if (baseName == null || baseName.isBlank()) {
                return List.of();
            }

            DataTableInfo dataTableInfo = getDataTableInfo(dataTableService, baseName, DEVELOPMENT.ordinal());

            if (dataTableInfo == null || dataTableInfo.columns() == null) {
                return List.of();
            }

            List<Property.ValueProperty<?>> columnProperties = new ArrayList<>();

            for (ColumnSpec columnSpec : dataTableInfo.columns()) {
                columnProperties.add(mapColumn(columnSpec));
            }

            var valuesObject = object(VALUES)
                .label("Values")
                .properties(columnProperties)
                .required(true);

            if (singleRecord) {
                return List.of(valuesObject);
            } else {
                var recordsArray = array(VALUES)
                    .label("Records")
                    .items(valuesObject)
                    .required(true);

                return List.of(recordsArray);
            }
        };
    }

    private static Object getSampleValue(ColumnType columnType) {
        return switch (columnType) {
            case STRING -> "sample value";
            case NUMBER -> 1.0;
            case INTEGER -> 1;
            case DATE -> LocalDate.now();
            case DATE_TIME -> LocalDateTime.now();
            case BOOLEAN -> false;
        };
    }

    private static Property.ValueProperty<?> mapColumn(ColumnSpec columnSpec) {
        String name = columnSpec.name();
        ColumnType type = columnSpec.type();

        return switch (type) {
            case STRING -> string(name);
            case NUMBER -> number(name);
            case INTEGER -> integer(name);
            case DATE -> date(name);
            case DATE_TIME -> dateTime(name);
            case BOOLEAN -> bool(name);
        };
    }
}
