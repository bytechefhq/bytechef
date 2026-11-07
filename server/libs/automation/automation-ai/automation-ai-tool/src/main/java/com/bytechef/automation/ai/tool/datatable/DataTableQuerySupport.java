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

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class DataTableQuerySupport {

    public static final int MAX_LIMIT = 50;

    static final int SCAN_PAGE_SIZE = 500;

    private DataTableQuerySupport() {
    }

    public static int resolveLimit(@Nullable Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return MAX_LIMIT;
        }

        return Math.min(requestedLimit, MAX_LIMIT);
    }

    public static String resolveBaseName(
        WorkspaceDataTableFacade workspaceDataTableFacade, long dataTableId, long workspaceId, long environmentId)
        throws DataTableNotFoundException {

        List<DataTableInfo> dataTableInfos = workspaceDataTableFacade.listTables(workspaceId, environmentId);

        for (DataTableInfo dataTableInfo : dataTableInfos) {
            Long id = dataTableInfo.id();

            if (id == null || id != dataTableId) {
                continue;
            }

            String baseName = dataTableInfo.baseName();

            if (baseName != null && !baseName.isBlank()) {
                return baseName;
            }
        }

        throw new DataTableNotFoundException();
    }

    public static List<Map<String, Object>> queryRowMaps(
        WorkspaceDataTableFacade workspaceDataTableFacade, long dataTableId, @Nullable String where, int fetchLimit,
        long environmentId) throws WhereParseException {

        if (where == null || where.isBlank()) {
            return workspaceDataTableFacade.listRows(dataTableId, fetchLimit, 0, environmentId)
                .stream()
                .map(DataTableRow::values)
                .toList();
        }

        WhereClause whereClause = parseWhere(where);

        List<Map<String, Object>> matchingRowMaps = new ArrayList<>();
        int offset = 0;

        while (matchingRowMaps.size() < fetchLimit) {
            List<DataTableRow> rows =
                workspaceDataTableFacade.listRows(dataTableId, SCAN_PAGE_SIZE, offset, environmentId);

            for (DataTableRow row : rows) {
                Map<String, Object> values = row.values();

                if (matchesWhereClause(values, whereClause)) {
                    matchingRowMaps.add(values);

                    if (matchingRowMaps.size() == fetchLimit) {
                        break;
                    }
                }
            }

            if (rows.size() < SCAN_PAGE_SIZE) {
                break;
            }

            offset += SCAN_PAGE_SIZE;
        }

        return matchingRowMaps;
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

    public static final class DataTableNotFoundException extends Exception {

        DataTableNotFoundException() {
            super("Data table not found");
        }
    }

    public static final class WhereParseException extends Exception {

        WhereParseException(String message) {
            super(message);
        }
    }
}
