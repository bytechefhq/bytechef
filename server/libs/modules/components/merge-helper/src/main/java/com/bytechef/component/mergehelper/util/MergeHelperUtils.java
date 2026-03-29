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

package com.bytechef.component.mergehelper.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Ivona Pavela
 */
public class MergeHelperUtils {

    private static final Pattern SAFE_IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    public static List<Map<String, Object>> flatten(Object input) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> allKeys = new LinkedHashSet<>();

        if (input == null) {
            return List.of();
        }

        if (input instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                rows.add(flattenToRow(item));
            }
        } else {
            rows.add(flattenToRow(input));
        }

        for (Map<String, Object> row : rows) {
            allKeys.addAll(row.keySet());
        }

        for (Map<String, Object> row : rows) {
            for (String key : allKeys) {
                row.putIfAbsent(key, null);
            }
        }

        return rows;
    }

    @SuppressFBWarnings({
        "SQL_INJECTION_JDBC", "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE"
    })
    public static void createTable(Connection connection, String tableName, List<Map<String, Object>> rows)
        throws SQLException {

        validateIdentifier(tableName);

        if (rows.isEmpty()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE " + tableName + " (dummy INTEGER);");
            }

            return;
        }

        Map<String, Object> sample = rows.getFirst();

        StringBuilder ddl = new StringBuilder("CREATE TABLE " + tableName + " (");

        int i = 0;

        for (Map.Entry<String, Object> entry : sample.entrySet()) {
            validateIdentifier(entry.getKey());

            if (i++ > 0) {
                ddl.append(", ");
            }

            ddl.append(entry.getKey())
                .append(" ")
                .append(mapToDuckDBType(entry.getValue()));
        }

        ddl.append(");");

        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl.toString());
        }
    }

    @SuppressFBWarnings({
        "SQL_INJECTION_JDBC", "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"
    })
    public static void insertData(Connection connection, String tableName, List<Map<String, Object>> rows)
        throws SQLException {

        if (rows.isEmpty())
            return;

        validateIdentifier(tableName);

        Map<String, Object> firstRow = rows.getFirst();

        List<String> columns = new ArrayList<>(firstRow.keySet());

        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));

        String sql = "INSERT INTO " + tableName + " VALUES (" + placeholders + ")";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map<String, Object> row : rows) {
                for (int i = 0; i < columns.size(); i++) {
                    ps.setObject(i + 1, row.get(columns.get(i)));
                }

                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @SuppressFBWarnings("SQL_INJECTION_JDBC")
    public static void validateSQL(Connection conn, String query) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("EXPLAIN " + query);
        }
    }

    @SuppressFBWarnings("SQL_INJECTION_JDBC")
    public static List<Map<String, Object>> executeQuery(Connection conn, String query)
        throws SQLException {

        List<Map<String, Object>> results = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData rsMetaData = rs.getMetaData();

            int columnCount = rsMetaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    row.put(rsMetaData.getColumnName(i), rs.getObject(i));
                }

                results.add(row);
            }
        }

        return results;
    }

    private static Map<String, Object> flattenToRow(Object input) {
        Map<String, Object> row = new LinkedHashMap<>();

        flattenObject(input, row, "");

        return row;
    }

    private static void flattenObject(Object input, Map<String, Object> row, String prefix) {
        if (input instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();

                String newKey = prefix.isEmpty() ? key : prefix + "_" + key;

                if (value instanceof Map<?, ?> nestedMap) {
                    flattenObject(nestedMap, row, newKey);

                } else if (value instanceof Iterable<?> iterable) {
                    row.put(newKey, normalizeList(iterable));
                } else {
                    row.put(newKey, value);
                }
            }

        } else if (input instanceof Iterable<?> iterable) {
            row.put(prefix.isEmpty() ? "value" : prefix, normalizeList(iterable));
        } else {
            row.put(prefix.isEmpty() ? "value" : prefix, input);
        }
    }

    private static List<Object> normalizeList(Iterable<?> iterable) {
        List<Object> list = new ArrayList<>();

        for (Object item : iterable) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> flattened = new LinkedHashMap<>();

                flattenObject(map, flattened, "");

                list.add(flattened);
            } else if (item instanceof Iterable<?> nestedIterable) {
                list.add(normalizeList(nestedIterable));
            } else {
                list.add(item);
            }
        }

        return list;
    }

    private static void validateIdentifier(String identifier) {
        if (!SAFE_IDENTIFIER_PATTERN.matcher(identifier)
            .matches()) {
            throw new IllegalArgumentException(
                "Invalid SQL identifier (only letters, digits, and underscores are allowed): " + identifier);
        }
    }

    private static String mapToDuckDBType(Object value) {
        return switch (value) {
            case Integer _ -> "INTEGER";
            case Long _ -> "BIGINT";
            case Double _ -> "DOUBLE";
            case Float _ -> "DOUBLE";
            case Boolean _ -> "BOOLEAN";
            case List<?> _ -> "LIST";
            default -> "VARCHAR";
        };
    }
}
