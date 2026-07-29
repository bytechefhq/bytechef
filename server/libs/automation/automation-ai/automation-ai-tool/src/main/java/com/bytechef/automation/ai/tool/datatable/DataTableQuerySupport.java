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

package com.bytechef.automation.ai.tool.datatable;

import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Shared inline row-query logic for the {@code queryDataTable} tool: resolve a table's physical base name, fetch rows
 * for an environment, and apply the optional simple-equals {@code where} filter. Both the CE
 * {@link QueryDataTableToolCallback} and the AI-Hub superset variant (which adds an EE-only {@code exportToCsv} branch)
 * call into this, so the query/filter semantics live in exactly one place. Context resolution (workspace / environment)
 * differs per surface and stays with each caller; this helper works on the already-resolved {@code baseName} and
 * {@code environmentId}.
 *
 * @author Ivica Cardic
 */
public final class DataTableQuerySupport {

    public static final int MAX_LIMIT = 50;

    private DataTableQuerySupport() {
    }

    /**
     * Caps a requested row limit to {@link #MAX_LIMIT}, defaulting to the max when unset or non-positive.
     */
    public static int resolveLimit(@Nullable Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return MAX_LIMIT;
        }

        return Math.min(requestedLimit, MAX_LIMIT);
    }

    /**
     * Resolves the physical base name for a data table id, throwing {@link DataTableNotFoundException} when the table
     * does not exist. Callers need the base name both for the row query and (AI-Hub) for the CSV export filename.
     */
    public static String resolveBaseName(DataTableService dataTableService, long dataTableId)
        throws DataTableNotFoundException {

        String baseName = dataTableService.getBaseNameById(dataTableId);

        if (baseName == null || baseName.isBlank()) {
            throw new DataTableNotFoundException();
        }

        return baseName;
    }

    /**
     * Fetches up to {@code fetchLimit} rows for {@code baseName} in {@code environmentId} and applies the optional
     * simple-equals {@code where} filter (e.g. {@code "status = 'qualified'"}), returning the row value maps.
     */
    public static List<Map<String, Object>> queryRowMaps(
        DataTableRowService dataTableRowService, String baseName, @Nullable String where, int fetchLimit,
        long environmentId) throws WhereParseException {

        List<DataTableRow> rows = dataTableRowService.listRows(baseName, fetchLimit, 0, environmentId);

        if (where != null && !where.isBlank()) {
            WhereClause whereClause = parseWhere(where);

            return rows.stream()
                .map(DataTableRow::values)
                .filter(values -> matchesWhereClause(values, whereClause))
                .limit(fetchLimit)
                .toList();
        }

        return rows.stream()
            .map(DataTableRow::values)
            .toList();
    }

    private static WhereClause parseWhere(String where) throws WhereParseException {
        int equalsIndex = where.indexOf('=');

        if (equalsIndex < 0) {
            throw new WhereParseException("Missing '=' operator in: " + where);
        }

        String columnName = where.substring(0, equalsIndex)
            .trim();
        String rawValue = where.substring(equalsIndex + 1)
            .trim();

        if (columnName.isEmpty()) {
            throw new WhereParseException("Column name is empty in: " + where);
        }

        String value = rawValue;

        if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
            value = value.substring(1, value.length() - 1);
        }

        return new WhereClause(columnName, value);
    }

    private static boolean matchesWhereClause(Map<String, Object> values, WhereClause whereClause) {
        Object actualValue = values.get(whereClause.columnName());

        if (actualValue == null) {
            return whereClause.value()
                .isEmpty();
        }

        return Objects.equals(whereClause.value(), actualValue.toString());
    }

    private record WhereClause(String columnName, String value) {
    }

    /**
     * Thrown by {@link #resolveBaseName} when no data table exists for the given id. Callers translate it into a
     * tool-boundary error carrying the caller's original (string) id.
     */
    public static final class DataTableNotFoundException extends Exception {

        DataTableNotFoundException() {
            super("Data table not found");
        }
    }

    /**
     * Thrown by {@link #queryRowMaps} when the {@code where} expression is not a simple equals clause.
     */
    public static final class WhereParseException extends Exception {

        WhereParseException(String message) {
            super(message);
        }
    }
}
