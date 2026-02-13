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

package com.bytechef.automation.data.table.execution.service;

import com.bytechef.automation.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.automation.data.table.domain.ColumnSpec;
import com.bytechef.automation.data.table.domain.ColumnType;
import com.bytechef.automation.data.table.execution.domain.DataTableRow;
import com.bytechef.automation.data.table.execution.event.DataTableWebhookEvent;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.writer.CsvWriter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
public class DataTableRowServiceImpl implements DataTableRowService {

    private static final Logger logger = LoggerFactory.getLogger(DataTableRowServiceImpl.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableRowServiceImpl(ApplicationEventPublisher applicationEventPublisher, JdbcTemplate jdbcTemplate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Deletes a row from a data table by ID.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public boolean deleteRow(String baseName, long id, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(physicalName);

        String sql = "DELETE FROM " + escapeIdentifier(physicalName) + " WHERE \"id\" = ?";

        int count = jdbcTemplate.update(sql, ps -> ps.setLong(1, id));

        if (count > 0) {
            Map<String, Object> payload = new HashMap<>();

            payload.put("id", id);

            applicationEventPublisher.publishEvent(
                new DataTableWebhookEvent(baseName, DataTableWebhookType.RECORD_DELETED, payload, environmentId));

            return true;
        }

        return false;
    }

    /**
     * Gets a single row from a data table by ID.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public DataTableRow getRow(String baseName, long id, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(physicalName);

        List<String> columnNames = listColumns(physicalName).stream()
            .map(ColumnSpec::name)
            .filter(name -> !"id".equalsIgnoreCase(name))
            .toList();

        String selectColumns = "\"id\"" + (columnNames.isEmpty() ? "" : ", " + columnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", ")));

        String sql = "SELECT " + selectColumns + " FROM " + escapeIdentifier(physicalName) + " WHERE \"id\" = ?";

        List<DataTableRow> rows = jdbcTemplate.query(sql, ps -> ps.setLong(1, id), (resultSet, rowNum) -> {
            long rowId = resultSet.getLong("id");
            Map<String, Object> values = new HashMap<>();

            for (String columnName : columnNames) {
                values.put(columnName, resultSet.getObject(columnName));
            }

            return new DataTableRow(rowId, values);
        });

        return rows.isEmpty() ? null : rows.getFirst();
    }

    @Override
    public String exportCsv(String baseName, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);
        List<String> columnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .filter(n -> !"id".equalsIgnoreCase(n))
            .toList();

        StringWriter stringWriter = new StringWriter();

        CsvWriter csvWriter = CsvWriter.builder()
            .build(stringWriter);

        // Header
        csvWriter.writeRow(columnNames);

        List<DataTableRow> dataTableRows = listRows(baseName, Integer.MAX_VALUE, 0, environmentId);

        for (DataTableRow dataTableRow : dataTableRows) {
            List<String> curValues = new ArrayList<>();

            for (String columnName : columnNames) {
                Map<String, Object> values = dataTableRow.values();

                Object value = values.get(columnName);

                curValues.add(value == null ? "" : String.valueOf(value));
            }

            csvWriter.writeRow(curValues);
        }

        try {
            csvWriter.close();
        } catch (IOException exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage());
            }
        }

        return stringWriter.toString();
    }

    @Override
    public void importCsv(String baseName, String csv, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(physicalName);

        if (csv == null) {
            return;
        }

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> columnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .toList();

        CsvReader.CsvReaderBuilder csvReaderBuilder = CsvReader.builder();

        try (CsvReader csvReader = csvReaderBuilder.build(new StringReader(csv))) {
            List<String> headers = null;
            List<String> mappedColumnNames = null;

            for (CsvRow csvRow : csvReader) {
                List<String> fields = csvRow.getFields();

                Stream<String> fieldStream = fields.stream();

                if (headers == null) {
                    headers = fieldStream.map(s -> s == null ? "" : s.trim())
                        .toList();

                    // Build mapping from file index -> actual column name (skip unknown and id)
                    mappedColumnNames = new ArrayList<>(headers.size());

                    for (String header : headers) {
                        if (header.equalsIgnoreCase("id")) {
                            mappedColumnNames.add(null);

                            continue;
                        }

                        String columnName = columnNames.stream()
                            .filter(curColumnName -> curColumnName.equalsIgnoreCase(header))
                            .findFirst()
                            .orElse(null);

                        mappedColumnNames.add(columnName);
                    }

                    continue;
                }

                if (fieldStream.allMatch(field -> field == null || field.isBlank())) {
                    continue;
                }

                Map<String, Object> values = new HashMap<>();

                int limit = Math.min(mappedColumnNames.size(), fields.size());

                for (int col = 0; col < limit; col++) {
                    String curColumnName = mappedColumnNames.get(col);

                    if (curColumnName == null) {
                        continue;
                    }

                    String field = fields.get(col);

                    values.put(curColumnName, (field == null || field.isEmpty()) ? null : field);
                }

                insertRow(baseName, values, environmentId);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to import CSV", exception);
        }
    }

    /**
     * Inserts a new row into a data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection. User-provided row values use parameterized queries.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public DataTableRow insertRow(String baseName, Map<String, Object> values, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(physicalName);

        List<String> allColumnNames = listColumns(physicalName).stream()
            .map(ColumnSpec::name)
            .toList();

        List<String> insertableColumnNames = values.keySet()
            .stream()
            .filter(columnName -> allColumnNames.stream()
                .anyMatch(column -> column.equalsIgnoreCase(columnName)))
            .filter(k -> !"id".equalsIgnoreCase(k))
            .map(columnName -> allColumnNames.stream()
                .filter(c -> c.equalsIgnoreCase(columnName))
                .findFirst()
                .orElse(columnName))
            .toList();

        String columnsClause = insertableColumnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", "));
        String placeholders = insertableColumnNames.stream()
            .map(k -> "?")
            .collect(Collectors.joining(", "));

        List<String> returningColumnNames = new ArrayList<>();

        returningColumnNames.add("id");
        returningColumnNames.addAll(allColumnNames.stream()
            .filter(columnName -> !"id".equalsIgnoreCase(columnName))
            .toList());

        String returningClause = returningColumnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", "));

        String valuesClause = insertableColumnNames.isEmpty()
            ? " DEFAULT VALUES" : (" (" + columnsClause + ") VALUES (" + placeholders + ")");

        String sql =
            "INSERT INTO " + escapeIdentifier(physicalName) + valuesClause + " RETURNING " + returningClause;

        Map<String, ColumnType> typeMap = columnTypeMap(physicalName);

        DataTableRow result = jdbcTemplate.query(sql, ps -> {
            int i = 1;

            for (String columnName : insertableColumnNames) {
                ColumnType columnType = typeMap.getOrDefault(columnName.toLowerCase(Locale.ROOT), ColumnType.STRING);
                Object rawValue = getValueCaseInsensitive(values, columnName);

                Object coercedValue = coerceValue(columnType, rawValue);

                setParam(ps, i++, columnType, coercedValue);
            }
        }, resultSet -> {
            if (resultSet.next()) {
                long id = resultSet.getLong("id");
                Map<String, Object> map = new HashMap<>();

                for (String columnName : returningColumnNames) {
                    if (!"id".equalsIgnoreCase(columnName)) {
                        map.put(columnName, resultSet.getObject(columnName));
                    }
                }

                return new DataTableRow(id, map);
            }
            throw new IllegalStateException("Failed to insert row");
        });

        // Dispatch event for webhooks
        Map<String, Object> payload = new HashMap<>();

        payload.put("id", result.id());
        payload.put("values", result.values());

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(baseName, DataTableWebhookType.RECORD_CREATED, payload, environmentId));

        return result;
    }

    /**
     * Lists rows from a data table with pagination.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection. LIMIT/OFFSET values are parameterized.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<DataTableRow> listRows(String baseName, int limit, int offset, long environmentId) {
        validateBaseName(baseName);

        String buildPhysicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(buildPhysicalName);

        List<String> columnNames = listColumns(buildPhysicalName).stream()
            .map(ColumnSpec::name)
            .filter(n -> !"id".equalsIgnoreCase(n))
            .toList();

        String selectColumns = "\"id\"" + (columnNames.isEmpty() ? "" : ", " + columnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", ")));

        String sql =
            "SELECT " + selectColumns + " FROM " + escapeIdentifier(buildPhysicalName) +
                " ORDER BY \"id\" LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql, ps -> {
            ps.setInt(1, Math.max(0, limit));
            ps.setInt(2, Math.max(0, offset));
        }, (rs, rowNum) -> {
            long id = rs.getLong("id");
            Map<String, Object> values = new HashMap<>();

            for (String column : columnNames) {
                values.put(column, rs.getObject(column));
            }

            return new DataTableRow(id, values);
        });
    }

    /**
     * Updates an existing row in a data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection. User-provided row values use parameterized queries.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public DataTableRow updateRow(String baseName, long id, Map<String, Object> values, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(physicalName);

        List<String> allColumnNames = listColumns(physicalName).stream()
            .map(ColumnSpec::name)
            .toList();

        List<String> updatableColumnNames = values.keySet()
            .stream()
            .filter(columnName -> allColumnNames.stream()
                .anyMatch(column -> column.equalsIgnoreCase(columnName)))
            .filter(k -> !"id".equalsIgnoreCase(k))
            .map(columnName -> allColumnNames.stream()
                .filter(c -> c.equalsIgnoreCase(columnName))
                .findFirst()
                .orElse(columnName))
            .toList();

        if (updatableColumnNames.isEmpty()) {
            // nothing to update, return current row
            List<DataTableRow> dataTableRows = listRows(baseName, 1, 0, environmentId).stream()
                .filter(dataTableRow -> dataTableRow.id() == id)
                .toList();

            if (!dataTableRows.isEmpty()) {
                return dataTableRows.getFirst();
            }

            throw new IllegalArgumentException("Row not found: id=" + id);
        }

        String setClause = updatableColumnNames.stream()
            .map(c -> escapeIdentifier(c) + " = ?")
            .collect(Collectors.joining(", "));

        List<String> returningColumnNames = new ArrayList<>();

        returningColumnNames.add("id");
        returningColumnNames.addAll(allColumnNames.stream()
            .filter(c -> !"id".equalsIgnoreCase(c))
            .toList());

        String returningClause = returningColumnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", "));

        String sql =
            "UPDATE " + escapeIdentifier(physicalName) + " SET " + setClause + " WHERE \"id\" = ? RETURNING " +
                returningClause;

        Map<String, ColumnType> columnTypeMap = columnTypeMap(physicalName);

        DataTableRow updatedDataTableRow = jdbcTemplate.query(sql, ps -> {
            int i = 1;

            for (String columnName : updatableColumnNames) {
                ColumnType columnType = columnTypeMap.getOrDefault(
                    columnName.toLowerCase(Locale.ROOT), ColumnType.STRING);

                Object rawValue = getValueCaseInsensitive(values, columnName);

                Object coercedValue = coerceValue(columnType, rawValue);

                setParam(ps, i++, columnType, coercedValue);
            }

            ps.setLong(i, id);
        }, rs -> {
            if (rs.next()) {
                long curId = rs.getLong("id");
                Map<String, Object> map = new HashMap<>();

                for (String columnName : returningColumnNames) {
                    if (!"id".equalsIgnoreCase(columnName)) {
                        map.put(columnName, rs.getObject(columnName));
                    }
                }

                return new DataTableRow(curId, map);
            }

            throw new IllegalArgumentException("Row not found: id=" + id);
        });

        Map<String, Object> payload = new HashMap<>();

        payload.put("id", updatedDataTableRow.id());
        payload.put("values", updatedDataTableRow.values());

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(baseName, DataTableWebhookType.RECORD_UPDATED, payload, environmentId));

        return updatedDataTableRow;
    }

    private void validateBaseName(String baseName) {
        Assert.hasText(baseName, "baseName must not be empty");

        String normalizedName = baseName.toLowerCase(Locale.ROOT);

        Assert.isTrue(!normalizedName.startsWith("dt_"), "baseName must not start with 'dt_'");
        Assert.isTrue(normalizedName.matches("[a-z_][a-z0-9_]*"), "Invalid base name: " + baseName);
    }

    private String buildPhysicalName(long environmentId, String baseName) {
        String normalizedName = baseName.toLowerCase(Locale.ROOT);

        return "dt_" + environmentId + "_" + normalizedName;
    }

    private void checkHasId(String physical) {
        String sql =
            "SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ? AND " +
                "column_name = 'id'";

        List<Integer> exists = jdbcTemplate.query(sql, ps -> ps.setString(1, physical), (rs, rowNum) -> 1);

        if (exists.isEmpty()) {
            throw new IllegalStateException("Table does not have primary key column 'id': " + physical);
        }
    }

    private Object coerceValue(ColumnType type, Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        if (rawValue instanceof String string) {
            String trimmed = string.trim();

            if (trimmed.isEmpty()) {
                return type == ColumnType.STRING ? "" : null;
            }

            return switch (type) {
                case STRING -> trimmed;
                case BOOLEAN -> com.bytechef.commons.util.StringUtils.parseBoolean(trimmed);
                case INTEGER -> com.bytechef.commons.util.StringUtils.parseLong(trimmed);
                case NUMBER -> com.bytechef.commons.util.StringUtils.parseBigDecimal(trimmed);
                case DATE -> com.bytechef.commons.util.StringUtils.parseSqlDate(trimmed);
                case DATE_TIME -> com.bytechef.commons.util.StringUtils.parseSqlTimestamp(trimmed);
            };
        }

        return switch (type) {
            case STRING -> String.valueOf(rawValue);
            case BOOLEAN -> (rawValue instanceof Boolean) ? rawValue
                : com.bytechef.commons.util.StringUtils.parseBoolean(String.valueOf(rawValue));
            case INTEGER -> (rawValue instanceof Number n) ? Long.valueOf(n.longValue())
                : com.bytechef.commons.util.StringUtils.parseLong(String.valueOf(rawValue));
            case NUMBER -> (rawValue instanceof BigDecimal bd)
                ? bd
                : (rawValue instanceof Number n) ? toBigDecimal(n)
                    : com.bytechef.commons.util.StringUtils.parseBigDecimal(String.valueOf(rawValue));
            case DATE ->
                (rawValue instanceof java.sql.Date date) ? date
                    : (rawValue instanceof Date date2) ? new Date(date2.getTime())
                        : com.bytechef.commons.util.StringUtils.parseSqlDate(String.valueOf(rawValue));
            case DATE_TIME ->
                (rawValue instanceof Timestamp timestamp) ? timestamp
                    : (rawValue instanceof Date date3) ? new Timestamp(date3.getTime())
                        : com.bytechef.commons.util.StringUtils.parseSqlTimestamp(String.valueOf(rawValue));
        };
    }

    private Map<String, ColumnType> columnTypeMap(String physicalName) {
        Map<String, ColumnType> columnTypeMap = new HashMap<>();

        for (ColumnSpec columnSpec : listColumns(physicalName)) {
            columnTypeMap.put(StringUtils.lowerCase(columnSpec.name()), columnSpec.type());
        }

        return columnTypeMap;
    }

    private String escapeIdentifier(String identifier) {
        Assert.hasText(identifier, "identifier must not be empty");

        String normalizedName = identifier.toLowerCase(Locale.ROOT);

        Assert.isTrue(normalizedName.matches("[a-z_][a-z0-9_]*"), "Invalid identifier: " + identifier);

        return '"' + normalizedName + '"';
    }

    private Object getValueCaseInsensitive(Map<String, Object> values, String columnName) {
        if (values.containsKey(columnName)) {
            return values.get(columnName);
        }

        String lowerCaseColumnName = columnName.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();

            if (lowerCaseColumnName.equals(key.toLowerCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }

        return null;
    }

    private List<ColumnSpec> listColumns(String physicalName) {
        String sql = "SELECT column_name, data_type FROM information_schema.columns " +
            "WHERE table_schema = current_schema() AND table_name = ? ORDER BY ordinal_position";

        return jdbcTemplate.query(sql, ps -> ps.setString(1, physicalName), (rs, rowNum) -> {
            String name = rs.getString("column_name");
            String dataType = rs.getString("data_type");

            return new ColumnSpec(name, mapType(dataType));
        });
    }

    private ColumnType mapType(String pgType) {
        String type = StringUtils.lowerCase(String.valueOf(pgType));

        if (type.startsWith("timestamp")) {
            return ColumnType.DATE_TIME;
        }

        if (type.equals("boolean") || type.equals("bool")) {
            return ColumnType.BOOLEAN;
        }

        switch (type) {
            case "integer", "int4", "smallint", "int2", "bigint", "int8", "serial", "bigserial" -> {

                return ColumnType.INTEGER;
            }
            case "numeric", "decimal", "double precision", "real" -> {

                return ColumnType.NUMBER;
            }
            case "date" -> {
                return ColumnType.DATE;
            }
            default -> {
                return ColumnType.STRING;
            }
        }
    }

    private void setParam(PreparedStatement ps, int index, ColumnType type, Object value) throws SQLException {
        if (value == null) {
            int sqlType = switch (type) {
                case STRING -> Types.VARCHAR;
                case NUMBER -> Types.DECIMAL;
                case INTEGER -> Types.BIGINT;
                case DATE -> Types.DATE;
                case DATE_TIME -> Types.TIMESTAMP;
                case BOOLEAN -> Types.BOOLEAN;
            };

            ps.setNull(index, sqlType);

            return;
        }

        switch (type) {
            case STRING -> ps.setString(index, String.valueOf(value));
            case NUMBER ->
                ps.setBigDecimal(index, (value instanceof BigDecimal bd) ? bd : toBigDecimal((Number) value));
            case INTEGER -> {
                switch (value) {
                    case Integer integer -> ps.setInt(index, integer);
                    case Long aLong -> ps.setLong(index, aLong);
                    case Number number -> ps.setLong(index, number.longValue());
                    default -> ps.setObject(index, value, Types.BIGINT);
                }
            }
            case DATE -> {
                if (value instanceof java.sql.Date d)
                    ps.setDate(index, d);
                else if (value instanceof Date d)
                    ps.setDate(index, new java.sql.Date(d.getTime()));
                else
                    ps.setDate(index, com.bytechef.commons.util.StringUtils.parseSqlDate(String.valueOf(value)));
            }
            case DATE_TIME -> {
                if (value instanceof Timestamp t)
                    ps.setTimestamp(index, t);
                else if (value instanceof Date d)
                    ps.setTimestamp(index, new Timestamp(d.getTime()));
                else
                    ps.setTimestamp(
                        index,
                        com.bytechef.commons.util.StringUtils.parseSqlTimestamp(String.valueOf(value)));
            }
            case BOOLEAN -> {
                if (value instanceof Boolean b)
                    ps.setBoolean(index, b);
                else
                    ps.setBoolean(index, Boolean.parseBoolean(String.valueOf(value)));
            }
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal bd) {
            return bd;
        }

        if (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte) {
            return BigDecimal.valueOf(number.longValue());
        }

        return new BigDecimal(String.valueOf(number));
    }
}
