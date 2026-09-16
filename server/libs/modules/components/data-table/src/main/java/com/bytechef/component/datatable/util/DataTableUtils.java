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

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.DataTableResolution;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

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
     * Trigger form of {@link #getActionTableOptions}. Written separately so the two options paths cannot drift.
     *
     * @param dataTableService the data table service
     * @return an OptionsFunction that provides table options
     */
    public static TriggerDefinition.OptionsFunction<String> getTriggerTableOptions(DataTableService dataTableService) {
        return (inputParameters, connectionParameters, dependencyPaths, searchText, context) -> getTableOptions(
            searchText, dataTableService);
    }

    /**
     * A named table together with the physical table it resolved to and that table's column metadata. The three travel
     * together because every caller that needs one needs the others, and resolving them separately risked the metadata
     * describing a different table from the one the rows come out of.
     */
    public record ResolvedDataTable(DataTableRef dataTableRef, DataTableInfo dataTableInfo) {
    }

    /**
     * The table a run names, together with its column metadata.
     *
     * @throws IllegalArgumentException when the environment holds no table of that name
     */
    public static ResolvedDataTable resolveDataTable(
        DataTableService dataTableService, String baseName, long environmentId) {

        return dataTableService.fetchDataTableResolution(baseName, environmentId)
            .flatMap(dataTableResolution -> dataTableInfoOf(dataTableService, dataTableResolution, environmentId))
            .orElseThrow(() -> new IllegalArgumentException("Data table '" + baseName + "' was not found"));
    }

    /**
     * {@link DataTableService#fetchDataTableResolution} settles which physical table a base name addresses, but carries
     * no column metadata. {@link DataTableService#listTables} is still the only source of that, so this recovers the
     * matching {@link DataTableInfo} -- keyed by registry id rather than by name, so the info can only be the resolved
     * table's.
     */
    private static Optional<ResolvedDataTable> dataTableInfoOf(
        DataTableService dataTableService, DataTableResolution dataTableResolution, long environmentId) {

        return dataTableService.listTables(environmentId)
            .stream()
            .filter(dataTableInfo -> Objects.equals(dataTableInfo.id(), dataTableResolution.dataTableId()))
            .findFirst()
            .map(dataTableInfo -> new ResolvedDataTable(dataTableResolution.dataTableRef(), dataTableInfo));
    }

    public static List<Option<String>> getTableOptions(
        String searchText, DataTableService dataTableService) {

        return dataTableService.listTables(DEVELOPMENT.ordinal())
            .stream()
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

        return dataTableService.fetchDataTableResolution(baseName, environmentId)
            .flatMap(dataTableResolution -> dataTableInfoOf(dataTableService, dataTableResolution, environmentId))
            .map(ResolvedDataTable::dataTableInfo)
            .orElse(null);
    }

    /**
     * Registers a trigger's webhook against the table the run resolves for the name it gave.
     *
     * <p>
     * The registration binds to the resolved table rather than to the base name, so registration and delivery meet on
     * the same registry row.
     *
     * @param dataTableService        the data table service
     * @param dataTableWebhookService the webhook registry
     * @param baseName                the table the trigger names
     * @param webhookUrl              the URL to notify
     * @param type                    the row event to subscribe to
     * @param environmentId           the environment the trigger runs in
     * @return the registered webhook id
     */
    public static long registerWebhook(
        DataTableService dataTableService, DataTableWebhookService dataTableWebhookService, String baseName,
        String webhookUrl, DataTableWebhookType type, long environmentId) {

        ResolvedDataTable resolvedDataTable = resolveDataTable(dataTableService, baseName, environmentId);

        return dataTableWebhookService.addWebhook(resolvedDataTable.dataTableRef(), webhookUrl, type);
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

        ResolvedDataTable resolvedDataTable =
            resolveDataTable(dataTableService, baseName, DEVELOPMENT.ordinal());

        DataTableInfo dataTableInfo = resolvedDataTable.dataTableInfo();

        BaseValueProperty<?> rowSchema = rowObjectSchema(dataTableInfo);

        List<DataTableRow> rows = dataTableRowService.listRows(resolvedDataTable.dataTableRef(), 1, 0);

        if (rows.isEmpty()) {
            return OutputResponse.of(rowSchema);
        }

        DataTableRow firstRow = rows.getFirst();

        Map<String, Object> sampleOutput = createSampleOutput(dataTableInfo, firstRow.id(), firstRow.values());

        return OutputResponse.of(rowSchema, sampleOutput);
    }

    /**
     * Takes the already-resolved table rather than looking it up, so that an output refresh scans the catalog once
     * instead of once here and once again in {@link #createSampleOutput}.
     */
    public static BaseValueProperty<?> rowObjectSchema(@Nullable DataTableInfo dataTableInfo) {
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
     * Takes the already-resolved table for the same reason {@link #rowObjectSchema} does.
     *
     * @param dataTableInfo the resolved table, or null when the run could not see one
     * @param rowId         the row id
     * @param rowValues     the row values map (may contain null values)
     * @return a map with id and all column values, with sample values for null entries
     */
    public static Map<String, Object> createSampleOutput(
        @Nullable DataTableInfo dataTableInfo, long rowId, Map<String, Object> rowValues) {

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
     * Creates a PropertiesFunction for dynamic properties lookup based on the columns of the selected table.
     *
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
}
