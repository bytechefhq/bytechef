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

import static com.bytechef.component.datatable.constant.DataTableConstants.COLUMN;
import static com.bytechef.component.datatable.constant.DataTableConstants.CONDITIONS;
import static com.bytechef.component.datatable.constant.DataTableConstants.OPERATION;
import static com.bytechef.component.datatable.constant.DataTableConstants.TABLE;
import static com.bytechef.component.datatable.constant.DataTableConstants.VALUE;
import static com.bytechef.component.datatable.constant.DataTableConstants.VALUES;
import static com.bytechef.component.datatable.constant.DataTableConstants.VALUE_TO;
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

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation.Arity;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Shared property, option, output schema and parsing helpers for Data Table actions.
 *
 * @author Ivica Cardic
 */
public final class DataTableUtils {

    private static final List<String> CONDITION_KEYS = List.of(COLUMN, OPERATION, VALUE, VALUE_TO, VALUES);

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

    /**
     * Returns an OptionsFunction listing the columns, including id, of the table selected in the table property.
     *
     * @param dataTableService the data table service
     * @return an OptionsFunction that provides column options
     */
    public static ActionDefinition.OptionsFunction<String> getActionColumnOptions(DataTableService dataTableService) {
        return (inputParameters, connectionParameters, dependencyPaths, searchText, context) -> {
            String baseName = inputParameters.getString(TABLE);

            if (baseName == null || baseName.isBlank()) {
                return List.of();
            }

            DataTableInfo dataTableInfo = getDataTableInfo(dataTableService, baseName, DEVELOPMENT.ordinal());

            List<String> columnNames = new ArrayList<>();

            columnNames.add("id");

            if (dataTableInfo != null && dataTableInfo.columns() != null) {
                for (ColumnSpec columnSpec : dataTableInfo.columns()) {
                    columnNames.add(columnSpec.name());
                }
            }

            return columnNames.stream()
                .filter(columnName -> searchText == null || columnName.toLowerCase(Locale.ROOT)
                    .contains(searchText.toLowerCase(Locale.ROOT)))
                .<Option<String>>map(columnName -> option(columnName, columnName))
                .toList();
        };
    }

    /**
     * Creates the OR Conditions property shared by the actions that select records by condition.
     *
     * @param dataTableService the data table service
     * @return the conditions property
     */
    public static ModifiableArrayProperty conditionsProperty(DataTableService dataTableService) {
        return array(CONDITIONS)
            .label("OR Conditions")
            .placeholder("Add OR Condition")
            .items(
                array()
                    .label("AND Condition")
                    .placeholder("Add AND Condition")
                    .description("AND Condition array that contains the conditions a record must all meet")
                    .items(
                        object()
                            .properties(
                                string(COLUMN)
                                    .label("Column")
                                    .description("The column to compare.")
                                    .options(getActionColumnOptions(dataTableService))
                                    .optionsLookupDependsOn(TABLE)
                                    .required(true),
                                string(OPERATION)
                                    .label("Operation")
                                    .description("How to compare the column with the value.")
                                    .options(
                                        option("Equals", Operation.EQUALS.name()),
                                        option("Equals Ignore Case", Operation.EQUALS_IGNORE_CASE.name()),
                                        option("Not Equals", Operation.NOT_EQUALS.name()),
                                        option("Contains", Operation.CONTAINS.name()),
                                        option("Not Contains", Operation.NOT_CONTAINS.name()),
                                        option("Starts With", Operation.STARTS_WITH.name()),
                                        option("Ends With", Operation.ENDS_WITH.name()),
                                        option("Greater", Operation.GREATER.name()),
                                        option("Greater or Equals", Operation.GREATER_EQUALS.name()),
                                        option("Less", Operation.LESS.name()),
                                        option("Less or Equals", Operation.LESS_EQUALS.name()),
                                        option("In", Operation.IN.name()),
                                        option("Between", Operation.BETWEEN.name()),
                                        option("Empty", Operation.EMPTY.name()),
                                        option("Not Empty", Operation.NOT_EMPTY.name()))
                                    .defaultValue(Operation.EQUALS.name())
                                    .required(true),
                                string(VALUE)
                                    .label("Value")
                                    .description("The value to compare with. For Between, the lower bound.")
                                    .displayCondition(
                                        "!contains({%s}, conditions[index][index].%s)".formatted(
                                            getOperationNames(Arity.LIST, Arity.NONE), OPERATION)),
                                string(VALUE_TO)
                                    .label("To Value")
                                    .description("The upper bound. Both bounds are included.")
                                    .displayCondition(
                                        "contains({%s}, conditions[index][index].%s)".formatted(
                                            getOperationNames(Arity.PAIR), OPERATION)),
                                array(VALUES)
                                    .label("Values")
                                    .description("The values the column must be one of.")
                                    .items(string())
                                    .displayCondition(
                                        "contains({%s}, conditions[index][index].%s)".formatted(
                                            getOperationNames(Arity.LIST), OPERATION)))));
    }

    /**
     * Reads the OR Conditions property as OR groups of AND conditions; an empty property matches every row.
     *
     * @param inputParameters the action input parameters
     * @return the conditions
     * @throws IllegalArgumentException if OR Conditions is not a list of AND Condition lists, an AND Condition list is
     *                                  null or empty, or a condition is null, has an unknown key, has no column, has a
     *                                  blank or unknown operation, or has a value that does not fit the operation; the
     *                                  message names the condition's position
     */
    public static DataTableRowConditions getConditions(Parameters inputParameters) {
        List<List<Map<String, Object>>> orConditions;

        try {
            orConditions = inputParameters.getList(
                CONDITIONS, new TypeReference<List<Map<String, Object>>>() {}, List.of());
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException(
                "OR Conditions must be a list of AND Condition lists, each holding condition objects: " +
                    runtimeException.getMessage(),
                runtimeException);
        }

        List<List<DataTableRowFilter>> conditions = new ArrayList<>();

        for (int groupIndex = 0; groupIndex < orConditions.size(); groupIndex++) {
            List<Map<String, Object>> andConditions = orConditions.get(groupIndex);

            if (andConditions == null || andConditions.isEmpty()) {
                throw new IllegalArgumentException(
                    "OR Condition %d is empty; add a condition to it or remove it".formatted(groupIndex + 1));
            }

            List<DataTableRowFilter> filters = new ArrayList<>();

            for (int conditionIndex = 0; conditionIndex < andConditions.size(); conditionIndex++) {
                Map<String, Object> condition = andConditions.get(conditionIndex);

                if (condition == null) {
                    throw new IllegalArgumentException(
                        "Condition %d of OR Condition %d is empty".formatted(conditionIndex + 1, groupIndex + 1));
                }

                try {
                    filters.add(toDataTableRowFilter(condition));
                } catch (IllegalArgumentException illegalArgumentException) {
                    throw new IllegalArgumentException(
                        "Condition %d of OR Condition %d: %s".formatted(
                            conditionIndex + 1, groupIndex + 1, illegalArgumentException.getMessage()),
                        illegalArgumentException);
                }
            }

            conditions.add(filters);
        }

        return conditions.isEmpty() ? DataTableRowConditions.matchAll() : DataTableRowConditions.anyOf(conditions);
    }

    /**
     * Parses an enum constant ignoring case, falling back to the default when the value is null or blank.
     *
     * @param enumClass    the enum type
     * @param value        the value to parse
     * @param defaultValue the value returned when {@code value} is null or blank
     * @param label        the property label used in the error message
     * @return the parsed enum constant
     * @throws IllegalArgumentException if the value names no constant of the enum
     */
    public static <E extends Enum<E>> E toEnum(
        Class<E> enumClass, @Nullable Object value, E defaultValue, String label) {

        if (value == null || String.valueOf(value)
            .isBlank()) {

            return defaultValue;
        }

        String name = String.valueOf(value)
            .trim()
            .toUpperCase(Locale.ROOT);

        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException exception) {
            String expectedNames = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.joining(", "));

            throw new IllegalArgumentException(
                "Unknown %s '%s'; expected one of %s".formatted(label, value, expectedNames), exception);
        }
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

    @Nullable
    public static Map<String, Object> flattenRow(@Nullable DataTableRow dataTableRow) {
        if (dataTableRow == null) {
            return null;
        }

        Map<String, Object> flattenedRow = new HashMap<>();

        flattenedRow.put("id", dataTableRow.id());
        flattenedRow.putAll(dataTableRow.values());

        return flattenedRow;
    }

    public static List<Map<String, Object>> flattenRows(List<DataTableRow> dataTableRows) {
        return dataTableRows.stream()
            .map(DataTableUtils::flattenRow)
            .toList();
    }

    @SuppressWarnings("unchecked")
    public static Object flattenPayload(Object payload) {
        if (!(payload instanceof Map<?, ?> payloadMap) || !(payloadMap.get(VALUES) instanceof Map<?, ?> values)) {
            return payload;
        }

        Map<String, Object> flattenedPayload = new HashMap<>();

        for (Map.Entry<?, ?> entry : payloadMap.entrySet()) {
            if (!Objects.equals(entry.getKey(), VALUES)) {
                flattenedPayload.put((String) entry.getKey(), entry.getValue());
            }
        }

        flattenedPayload.putAll((Map<String, Object>) values);

        return flattenedPayload;
    }

    /**
     * Creates a PropertiesFunction for dynamic properties lookup based on table columns.
     *
     * @param dataTableService the data table service
     * @param singleRecord     true to return one property per column, false to return a Records array of column objects
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

            if (singleRecord) {
                return columnProperties;
            }

            var valuesObject = object(VALUES)
                .label("Values")
                .properties(columnProperties)
                .required(true);

            var recordsArray = array(VALUES)
                .label("Records")
                .items(valuesObject)
                .required(true);

            return List.of(recordsArray);
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

    private static String getOperationNames(Arity... arities) {
        List<Arity> arityList = List.of(arities);

        return Arrays.stream(Operation.values())
            .filter(operation -> arityList.contains(operation.arity()))
            .map(operation -> "'" + operation.name() + "'")
            .collect(Collectors.joining(","));
    }

    private static void requirePairValue(
        Map<String, Object> condition, String key, String label, Operation operation) {

        Object value = condition.get(key);

        if (value == null || (value instanceof String string && string.isBlank())) {
            throw new IllegalArgumentException("Operation %s requires %s".formatted(operation, label));
        }
    }

    private static DataTableRowFilter toDataTableRowFilter(Map<String, Object> condition) {
        List<String> unknownKeys = condition.keySet()
            .stream()
            .filter(key -> !CONDITION_KEYS.contains(key))
            .sorted()
            .toList();

        if (!unknownKeys.isEmpty()) {
            throw new IllegalArgumentException(
                "Unknown condition key(s) %s; expected %s".formatted(
                    String.join(", ", unknownKeys), String.join(", ", CONDITION_KEYS)));
        }

        Object column = condition.get(COLUMN);

        if (!(column instanceof String columnName) || columnName.isBlank()) {
            throw new IllegalArgumentException("Column is required");
        }

        Object operationValue = condition.get(OPERATION);

        if (operationValue != null && String.valueOf(operationValue)
            .isBlank()) {

            throw new IllegalArgumentException("Operation must not be blank");
        }

        Operation operation = toEnum(Operation.class, operationValue, Operation.EQUALS, "Operation");

        if (operation.arity() == Arity.LIST && condition.get(VALUES) == null && condition.get(VALUE) != null) {
            throw new IllegalArgumentException(
                "Operation %s takes its values from Values, not Value".formatted(operation));
        }

        if (operation.arity() == Arity.PAIR) {
            requirePairValue(condition, VALUE, "Value", operation);
            requirePairValue(condition, VALUE_TO, "To Value", operation);
        }

        Object value = switch (operation.arity()) {
            case LIST -> condition.get(VALUES);
            case PAIR -> Arrays.asList(condition.get(VALUE), condition.get(VALUE_TO));
            case NONE, ONE -> condition.get(VALUE);
        };

        return new DataTableRowFilter(columnName, operation, value);
    }
}
