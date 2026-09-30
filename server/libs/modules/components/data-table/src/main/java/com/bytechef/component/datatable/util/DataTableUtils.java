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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.TriggerContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import org.jspecify.annotations.Nullable;

/**
 * Utility to construct output schemas for Data Table actions using table metadata.
 *
 * @author Ivica Cardic
 */
public final class DataTableUtils {

    private static final String UNRESOLVED_WORKSPACE = "Unable to determine the workspace of this workflow";

    private DataTableUtils() {
    }

    public static ActionDefinition.OptionsFunction<String> getActionTableOptions(
        DataTableService dataTableService, DataTableWorkspaceResolver dataTableWorkspaceResolver) {

        return (inputParameters, connectionParameters, dependencyPaths, searchText, context) -> getTableOptions(
            searchText, dataTableService, resolveWorkspaceId(dataTableWorkspaceResolver, context));
    }

    public static TriggerDefinition.OptionsFunction<String> getTriggerTableOptions(
        DataTableService dataTableService, DataTableWorkspaceResolver dataTableWorkspaceResolver) {

        return (inputParameters, connectionParameters, dependencyPaths, searchText, context) -> getTableOptions(
            searchText, dataTableService, resolveWorkspaceId(dataTableWorkspaceResolver, context));
    }

    /**
     * A named table together with the physical table it resolved to and that table's column metadata. The three travel
     * together because every caller that needs one needs the others, and resolving them separately risked the metadata
     * describing a different table from the one the rows come out of.
     */
    public record ResolvedDataTable(DataTableRef dataTableRef, DataTableInfo dataTableInfo) {
    }

    public static long resolveWorkspaceId(
        DataTableWorkspaceResolver dataTableWorkspaceResolver, ActionContext actionContext) {

        if (!(actionContext instanceof ActionContextAware actionContextAware)) {
            throw new IllegalStateException(UNRESOLVED_WORKSPACE);
        }

        OptionalLong workspaceId = resolveByWorkflowId(dataTableWorkspaceResolver, actionContextAware.getWorkflowId());

        if (workspaceId.isPresent()) {
            return workspaceId.getAsLong();
        }

        Long jobPrincipalId = actionContextAware.getJobPrincipalId();
        PlatformType platformType = actionContextAware.getPlatformType();

        if (jobPrincipalId != null && platformType != null) {
            workspaceId = dataTableWorkspaceResolver.resolveByJobPrincipalId(jobPrincipalId, platformType);

            if (workspaceId.isPresent()) {
                return workspaceId.getAsLong();
            }
        }

        throw new IllegalStateException(UNRESOLVED_WORKSPACE);
    }

    public static long resolveWorkspaceId(
        DataTableWorkspaceResolver dataTableWorkspaceResolver, TriggerContext triggerContext) {

        if (!(triggerContext instanceof TriggerContextAware triggerContextAware)) {
            throw new IllegalStateException(UNRESOLVED_WORKSPACE);
        }

        return resolveByWorkflowId(dataTableWorkspaceResolver, triggerContextAware.getWorkflowId())
            .orElseThrow(() -> new IllegalStateException(UNRESOLVED_WORKSPACE));
    }

    public static long resolveWorkspaceId(
        DataTableWorkspaceResolver dataTableWorkspaceResolver, String workflowExecutionId) {

        WorkflowExecutionId parsedWorkflowExecutionId = WorkflowExecutionId.parse(workflowExecutionId);

        long jobPrincipalId = parsedWorkflowExecutionId.getJobPrincipalId();

        OptionalLong workspaceId = jobPrincipalId > 0
            ? dataTableWorkspaceResolver.resolveByJobPrincipalId(jobPrincipalId, parsedWorkflowExecutionId.getType())
            : dataTableWorkspaceResolver.resolveByWorkflowUuid(parsedWorkflowExecutionId.getWorkflowUuid());

        return workspaceId.orElseThrow(() -> new IllegalStateException(UNRESOLVED_WORKSPACE));
    }

    public static ResolvedDataTable resolveDataTable(
        DataTableService dataTableService, long workspaceId, String name, long environmentId) {

        DataTable dataTable = dataTableService.fetchDataTable(workspaceId, name)
            .orElseThrow(() -> notFound(name));

        DataTableInfo dataTableInfo = dataTableService.fetchDataTableInfo(dataTable.getId(), environmentId)
            .orElseThrow(() -> notFound(name));

        return new ResolvedDataTable(new DataTableRef(dataTable.getId(), environmentId), dataTableInfo);
    }

    @Nullable
    public static DataTableInfo getDataTableInfo(
        DataTableService dataTableService, long workspaceId, String name, long environmentId) {

        return dataTableService.fetchDataTable(workspaceId, name)
            .flatMap(dataTable -> dataTableService.fetchDataTableInfo(dataTable.getId(), environmentId))
            .orElse(null);
    }

    public static List<Option<String>> getTableOptions(
        @Nullable String searchText, DataTableService dataTableService, long workspaceId) {

        return dataTableService.listTables(workspaceId, DEVELOPMENT.ordinal())
            .stream()
            .filter(dataTableInfo -> searchText == null || dataTableInfo.name()
                .contains(searchText.toLowerCase(Locale.ROOT)))
            .<Option<String>>map(
                dataTableInfo -> option(dataTableInfo.name(), dataTableInfo.name(), dataTableInfo.description()))
            .toList();
    }

    public static long registerWebhook(
        DataTableService dataTableService, DataTableWebhookService dataTableWebhookService, long workspaceId,
        String name, String webhookUrl, DataTableWebhookType type, long environmentId) {

        ResolvedDataTable resolvedDataTable = resolveDataTable(dataTableService, workspaceId, name, environmentId);

        return dataTableWebhookService.addWebhook(resolvedDataTable.dataTableRef(), webhookUrl, type);
    }

    public static OutputResponse createTriggerOutputResponse(
        DataTableRowService dataTableRowService, DataTableService dataTableService, long workspaceId, String name) {

        ResolvedDataTable resolvedDataTable = resolveDataTable(
            dataTableService, workspaceId, name, DEVELOPMENT.ordinal());

        DataTableInfo dataTableInfo = resolvedDataTable.dataTableInfo();

        BaseValueProperty<?> rowSchema = rowObjectSchema(dataTableInfo);

        List<DataTableRow> rows = dataTableRowService.listRows(resolvedDataTable.dataTableRef(), 1, 0);

        if (rows.isEmpty()) {
            return OutputResponse.of(rowSchema);
        }

        DataTableRow firstRow = rows.getFirst();

        return OutputResponse.of(rowSchema, createSampleOutput(dataTableInfo, firstRow.id(), firstRow.values()));
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
        DataTableService dataTableService, DataTableWorkspaceResolver dataTableWorkspaceResolver,
        boolean singleRecord) {

        return (inputParameters, connectionParameters, dependencyPaths, context) -> {
            String name = inputParameters.getString(TABLE);

            if (name == null || name.isBlank()) {
                return List.of();
            }

            DataTableInfo dataTableInfo = getDataTableInfo(
                dataTableService, resolveWorkspaceId(dataTableWorkspaceResolver, context), name, DEVELOPMENT.ordinal());

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

    private static OptionalLong resolveByWorkflowId(
        DataTableWorkspaceResolver dataTableWorkspaceResolver, @Nullable String workflowId) {

        if (workflowId == null) {
            return OptionalLong.empty();
        }

        return dataTableWorkspaceResolver.resolveByWorkflowId(workflowId);
    }

    private static DataTableException notFound(String name) {
        return new DataTableException(
            "Data table '" + name + "' not found in this workspace", DataTableErrorType.DATA_TABLE_NOT_FOUND);
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
