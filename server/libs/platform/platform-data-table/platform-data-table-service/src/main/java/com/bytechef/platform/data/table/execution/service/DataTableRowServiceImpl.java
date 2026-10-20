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

package com.bytechef.platform.data.table.execution.service;

import com.bytechef.commons.util.BooleanUtils;
import com.bytechef.commons.util.DateUtils;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.writer.CsvWriter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
public class DataTableRowServiceImpl implements DataTableRowService {

    private static final Logger log = LoggerFactory.getLogger(DataTableRowServiceImpl.class);

    private static final Pattern EPOCH_PATTERN = Pattern.compile("-?\\d+");

    private final ApplicationEventPublisher applicationEventPublisher;
    private final JdbcTemplate jdbcTemplate;

    private Boolean returningSupported;

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

        DataTableRow deletedDataTableRow;

        if (isReturningSupported()) {
            deletedDataTableRow = deleteRowReturningValues(physicalName, id);
        } else {
            deletedDataTableRow = deleteRowAfterReadingValues(baseName, physicalName, id, environmentId);
        }

        if (deletedDataTableRow == null) {
            return false;
        }

        Map<String, Object> payload = new HashMap<>();

        payload.put("id", deletedDataTableRow.id());
        payload.put("values", deletedDataTableRow.values());

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(baseName, DataTableWebhookType.RECORD_DELETED, payload, environmentId));

        return true;
    }

    /**
     * Deletes every row of a data table matching the given OR groups of AND conditions in one transaction.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because filter columns must match an
     * existing column and all identifiers are validated through {@link #escapeIdentifier(String)} and
     * {@link #validateBaseName(String)}. Filter values use parameterized queries.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    @Transactional
    public List<DataTableRow> deleteRows(
        String baseName, List<List<DataTableRowFilter>> conditions, long environmentId) {

        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> columnNames = getNonIdColumnNames(columnSpecs);

        WhereClause whereClause = buildRequiredWhereClause(columnSpecs, conditions);

        String tableName = escapeIdentifier(physicalName);

        List<DataTableRow> deletedDataTableRows;

        if (isReturningSupported()) {
            String sql = "DELETE FROM " + tableName + whereClause.sql() + " RETURNING " +
                buildSelectColumns(columnNames);

            deletedDataTableRows = jdbcTemplate.query(
                sql, preparedStatement -> setFilterParameters(preparedStatement, 1, whereClause.filterParameters()),
                (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));
        } else {
            deletedDataTableRows = selectRowsForUpdate(tableName, columnNames, whereClause);

            List<Long> ids = deletedDataTableRows.stream()
                .map(DataTableRow::id)
                .toList();

            if (!ids.isEmpty()) {
                jdbcTemplate.update(
                    "DELETE FROM " + tableName + " WHERE \"id\" IN (" + buildPlaceholders(ids.size()) + ")",
                    (PreparedStatementSetter) preparedStatement -> setIds(preparedStatement, 1, ids));
            }
        }

        List<DataTableRow> sortedDataTableRows = sortById(deletedDataTableRows);

        for (DataTableRow dataTableRow : sortedDataTableRows) {
            publishWebhookEvent(baseName, DataTableWebhookType.RECORD_DELETED, dataTableRow, environmentId);
        }

        return sortedDataTableRows;
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

        List<DataTableRow> rows = jdbcTemplate.query(
            sql, preparedStatement -> preparedStatement.setLong(1, id),
            (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));

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
            if (log.isTraceEnabled()) {
                log.trace(exception.getMessage());
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

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> allColumnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .toList();

        List<ColumnValue> columnValues = getColumnValues(columnSpecs, values, false);

        String columnsClause = columnValues.stream()
            .map(columnValue -> {
                ColumnSpec columnSpec = columnValue.columnSpec();

                return escapeIdentifier(columnSpec.name());
            })
            .collect(Collectors.joining(", "));

        String valuesClause = columnValues.isEmpty()
            ? " DEFAULT VALUES"
            : (" (" + columnsClause + ") VALUES (" + buildPlaceholders(columnValues.size()) + ")");

        PreparedStatementSetter preparedStatementSetter =
            preparedStatement -> setColumnValues(preparedStatement, 1, columnValues);

        DataTableRow result;

        if (isReturningSupported()) {
            result = insertRowReturningValues(physicalName, valuesClause, allColumnNames, preparedStatementSetter);
        } else {
            result = insertRowAfterInserting(
                baseName, physicalName, valuesClause, environmentId, preparedStatementSetter);
        }

        if (result == null) {
            throw new IllegalStateException("Failed to insert row");
        }

        // Dispatch event for webhooks
        Map<String, Object> payload = new HashMap<>();

        payload.put("id", result.id());
        payload.put("values", result.values());

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(baseName, DataTableWebhookType.RECORD_CREATED, payload, environmentId));

        return result;
    }

    @Override
    public List<DataTableRow> listRows(String baseName, int limit, int offset, long environmentId) {
        return findRows(baseName, List.of(), null, limit, offset, environmentId);
    }

    /**
     * Finds rows of a data table matching the given OR groups of AND conditions.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because filter and sort columns must
     * match an existing column and all identifiers are validated through {@link #escapeIdentifier(String)} and
     * {@link #validateBaseName(String)}. Filter values use parameterized queries.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<DataTableRow> findRows(
        String baseName, List<List<DataTableRowFilter>> conditions, @Nullable DataTableRowSort sort, int limit,
        int offset, long environmentId) {

        if (limit < 0 || offset < 0) {
            throw new IllegalArgumentException(
                "limit and offset must not be negative, got limit=%d and offset=%d".formatted(limit, offset));
        }

        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> columnNames = getNonIdColumnNames(columnSpecs);

        WhereClause whereClause = buildWhereClause(columnSpecs, conditions);

        String orderByClause = " ORDER BY \"id\"";

        if (sort != null) {
            ColumnSpec sortColumnSpec = getColumnSpec(columnSpecs, sort.column());
            DataTableRowSort.Direction direction = sort.direction();

            orderByClause = " ORDER BY " + escapeIdentifier(sortColumnSpec.name()) + " " + direction.name() +
                " NULLS LAST, \"id\"";
        }

        String sql = "SELECT " + buildSelectColumns(columnNames) + " FROM " + escapeIdentifier(physicalName) +
            whereClause.sql() + orderByClause + " LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql, preparedStatement -> {
            int index = setFilterParameters(preparedStatement, 1, whereClause.filterParameters());

            preparedStatement.setInt(index++, limit);
            preparedStatement.setInt(index, offset);
        }, (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public DataTableRow updateRow(String baseName, long id, Map<String, Object> values, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> allColumnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .toList();

        List<ColumnValue> columnValues = getColumnValues(columnSpecs, values, false);

        if (columnValues.isEmpty()) {
            DataTableRow dataTableRow = getRow(baseName, id, environmentId);

            if (dataTableRow == null) {
                throw new IllegalArgumentException("Row not found: id=" + id);
            }

            return dataTableRow;
        }

        String setClause = buildSetClause(columnValues);

        PreparedStatementSetter preparedStatementSetter = preparedStatement -> {
            int index = setColumnValues(preparedStatement, 1, columnValues);

            preparedStatement.setLong(index, id);
        };

        DataTableRow updatedDataTableRow;

        if (isReturningSupported()) {
            updatedDataTableRow = updateRowReturningValues(
                physicalName, setClause, allColumnNames, preparedStatementSetter);
        } else {
            updatedDataTableRow = updateRowAfterUpdating(
                baseName, physicalName, setClause, id, environmentId, preparedStatementSetter);
        }

        if (updatedDataTableRow == null) {
            throw new IllegalArgumentException("Row not found: id=" + id);
        }

        Map<String, Object> payload = new HashMap<>();

        payload.put("id", updatedDataTableRow.id());
        payload.put("values", updatedDataTableRow.values());

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(baseName, DataTableWebhookType.RECORD_UPDATED, payload, environmentId));

        return updatedDataTableRow;
    }

    /**
     * Updates every row of a data table matching the given OR groups of AND conditions in one transaction.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because filter and updated columns must
     * match an existing column and all identifiers are validated through {@link #escapeIdentifier(String)} and
     * {@link #validateBaseName(String)}. Filter and row values use parameterized queries.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    @Transactional
    public List<DataTableRow> updateRows(
        String baseName, List<List<DataTableRowFilter>> conditions, Map<String, Object> values, long environmentId) {

        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> columnNames = getNonIdColumnNames(columnSpecs);

        WhereClause whereClause = buildRequiredWhereClause(columnSpecs, conditions);

        List<ColumnValue> columnValues = getColumnValues(columnSpecs, values, true);

        if (columnValues.isEmpty()) {
            throw new IllegalArgumentException("Updating rows requires a value for at least one column");
        }

        String tableName = escapeIdentifier(physicalName);
        String setClause = buildSetClause(columnValues);

        List<DataTableRow> updatedDataTableRows;

        if (isReturningSupported()) {
            String sql = "UPDATE " + tableName + " SET " + setClause + whereClause.sql() + " RETURNING " +
                buildSelectColumns(columnNames);

            updatedDataTableRows = jdbcTemplate.query(sql, preparedStatement -> {
                int index = setColumnValues(preparedStatement, 1, columnValues);

                setFilterParameters(preparedStatement, index, whereClause.filterParameters());
            }, (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));
        } else {
            List<Long> ids = selectRowsForUpdate(tableName, columnNames, whereClause).stream()
                .map(DataTableRow::id)
                .toList();

            if (ids.isEmpty()) {
                return List.of();
            }

            String idsClause = " WHERE \"id\" IN (" + buildPlaceholders(ids.size()) + ")";

            jdbcTemplate.update("UPDATE " + tableName + " SET " + setClause + idsClause,
                (PreparedStatementSetter) preparedStatement -> {
                    int index = setColumnValues(preparedStatement, 1, columnValues);

                    setIds(preparedStatement, index, ids);
                });

            updatedDataTableRows = jdbcTemplate.query(
                "SELECT " + buildSelectColumns(columnNames) + " FROM " + tableName + idsClause,
                preparedStatement -> setIds(preparedStatement, 1, ids),
                (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));
        }

        List<DataTableRow> sortedDataTableRows = sortById(updatedDataTableRows);

        for (DataTableRow dataTableRow : sortedDataTableRows) {
            publishWebhookEvent(baseName, DataTableWebhookType.RECORD_UPDATED, dataTableRow, environmentId);
        }

        return sortedDataTableRows;
    }

    private String buildPlaceholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    private String buildSelectColumns(List<String> columnNames) {
        return "\"id\"" + (columnNames.isEmpty() ? "" : ", " + columnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", ")));
    }

    private String buildSetClause(List<ColumnValue> columnValues) {
        return columnValues.stream()
            .map(columnValue -> {
                ColumnSpec columnSpec = columnValue.columnSpec();

                return escapeIdentifier(columnSpec.name()) + " = ?";
            })
            .collect(Collectors.joining(", "));
    }

    private WhereClause buildRequiredWhereClause(
        List<ColumnSpec> columnSpecs, List<List<DataTableRowFilter>> conditions) {

        WhereClause whereClause = buildWhereClause(columnSpecs, conditions);

        String sql = whereClause.sql();

        if (sql.isEmpty()) {
            throw new IllegalArgumentException("At least one condition is required to select the rows to change");
        }

        return whereClause;
    }

    private WhereClause buildWhereClause(List<ColumnSpec> columnSpecs, List<List<DataTableRowFilter>> conditions) {
        List<FilterParameter> filterParameters = new ArrayList<>();
        List<String> orConditions = new ArrayList<>();

        for (List<DataTableRowFilter> andFilters : conditions) {
            if (andFilters.isEmpty()) {
                continue;
            }

            List<String> andConditions = new ArrayList<>();

            for (DataTableRowFilter filter : andFilters) {
                andConditions.add(
                    buildCondition(getColumnSpec(columnSpecs, filter.column()), filter, filterParameters));
            }

            orConditions.add("(" + String.join(" AND ", andConditions) + ")");
        }

        String sql = orConditions.isEmpty() ? "" : " WHERE " + String.join(" OR ", orConditions);

        return new WhereClause(sql, filterParameters);
    }

    private List<ColumnValue> getColumnValues(
        List<ColumnSpec> columnSpecs, Map<String, Object> values, boolean bulkUpdate) {

        List<ColumnValue> columnValues = new ArrayList<>();
        List<String> unknownColumnNames = new ArrayList<>();

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            if ("id".equalsIgnoreCase(columnName)) {
                if (bulkUpdate) {
                    throw new IllegalArgumentException(
                        "The id column cannot be updated; add a condition on id to select the rows to change");
                }

                continue;
            }

            if (bulkUpdate && (value == null || (value instanceof String string && string.isBlank()))) {
                throw new IllegalArgumentException(
                    ("Value for column '%s' is empty; updating rows by conditions does not clear columns, use " +
                        "Update Record to clear a column of a single record").formatted(columnName));
            }

            columnSpecs.stream()
                .filter(columnSpec -> {
                    String name = columnSpec.name();

                    return name.equalsIgnoreCase(columnName);
                })
                .findFirst()
                .ifPresentOrElse(
                    columnSpec -> columnValues.add(new ColumnValue(columnSpec, coerceValueStrictly(columnSpec, value))),
                    () -> unknownColumnNames.add(columnName));
        }

        if (!unknownColumnNames.isEmpty()) {
            throw new IllegalArgumentException("Unknown column(s): " + String.join(", ", unknownColumnNames));
        }

        return columnValues;
    }

    private static List<String> getNonIdColumnNames(List<ColumnSpec> columnSpecs) {
        return columnSpecs.stream()
            .map(ColumnSpec::name)
            .filter(name -> !"id".equalsIgnoreCase(name))
            .toList();
    }

    private void publishWebhookEvent(
        String baseName, DataTableWebhookType dataTableWebhookType, DataTableRow dataTableRow, long environmentId) {

        Map<String, Object> payload = new HashMap<>();

        payload.put("id", dataTableRow.id());
        payload.put("values", dataTableRow.values());

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(baseName, dataTableWebhookType, payload, environmentId));
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<DataTableRow> selectRowsForUpdate(
        String tableName, List<String> columnNames, WhereClause whereClause) {

        String sql = "SELECT " + buildSelectColumns(columnNames) + " FROM " + tableName + whereClause.sql() +
            " ORDER BY \"id\" FOR UPDATE";

        return jdbcTemplate.query(
            sql, preparedStatement -> setFilterParameters(preparedStatement, 1, whereClause.filterParameters()),
            (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));
    }

    private int setColumnValues(PreparedStatement preparedStatement, int index, List<ColumnValue> columnValues)
        throws SQLException {

        int curIndex = index;

        for (ColumnValue columnValue : columnValues) {
            ColumnSpec columnSpec = columnValue.columnSpec();

            setParam(preparedStatement, curIndex++, columnSpec.type(), columnValue.value());
        }

        return curIndex;
    }

    private int
        setFilterParameters(PreparedStatement preparedStatement, int index, List<FilterParameter> filterParameters)
            throws SQLException {

        int curIndex = index;

        for (FilterParameter filterParameter : filterParameters) {
            setParam(preparedStatement, curIndex++, filterParameter.columnType(), filterParameter.value());
        }

        return curIndex;
    }

    private static void setIds(PreparedStatement preparedStatement, int index, List<Long> ids) throws SQLException {
        int curIndex = index;

        for (long id : ids) {
            preparedStatement.setLong(curIndex++, id);
        }
    }

    private static List<DataTableRow> sortById(List<DataTableRow> dataTableRows) {
        return dataTableRows.stream()
            .sorted(Comparator.comparingLong(DataTableRow::id))
            .toList();
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

    private String buildCondition(
        ColumnSpec columnSpec, DataTableRowFilter filter, List<FilterParameter> filterParameters) {

        String column = escapeIdentifier(columnSpec.name());
        ColumnType columnType = columnSpec.type();
        DataTableRowFilter.Operation operation = filter.operation();

        return switch (operation) {
            case EQUALS -> addComparison(column, "=", columnSpec, filter, filterParameters);
            case EQUALS_IGNORE_CASE -> {
                String value = (String) coerceTextFilterValue(columnSpec, filter);

                filterParameters.add(new FilterParameter(ColumnType.STRING, value.toLowerCase(Locale.ROOT)));

                yield "LOWER(" + column + ") = ?";
            }
            case NOT_EQUALS -> addComparison(column, "IS DISTINCT FROM", columnSpec, filter, filterParameters);
            case GREATER -> addComparison(column, ">", columnSpec, filter, filterParameters);
            case GREATER_EQUALS -> addComparison(column, ">=", columnSpec, filter, filterParameters);
            case LESS -> addComparison(column, "<", columnSpec, filter, filterParameters);
            case LESS_EQUALS -> addComparison(column, "<=", columnSpec, filter, filterParameters);
            case CONTAINS -> addLike(column, "%", "%", false, columnSpec, filter, filterParameters);
            case NOT_CONTAINS -> addLike(column, "%", "%", true, columnSpec, filter, filterParameters);
            case STARTS_WITH -> addLike(column, "", "%", false, columnSpec, filter, filterParameters);
            case ENDS_WITH -> addLike(column, "%", "", false, columnSpec, filter, filterParameters);
            case IN -> {
                List<?> values = getFilterValues(filter);

                for (Object value : values) {
                    filterParameters.add(new FilterParameter(columnType, coerceFilterValue(columnSpec, value)));
                }

                yield column + " IN (" + values.stream()
                    .map(value -> "?")
                    .collect(Collectors.joining(", ")) + ")";
            }
            case BETWEEN -> {
                List<?> values = getFilterValues(filter);

                for (Object value : values) {
                    filterParameters.add(new FilterParameter(columnType, coerceFilterValue(columnSpec, value)));
                }

                yield column + " BETWEEN ? AND ?";
            }
            case EMPTY -> columnType == ColumnType.STRING
                ? "(" + column + " IS NULL OR " + column + " = '')" : column + " IS NULL";
            case NOT_EMPTY -> columnType == ColumnType.STRING
                ? "(" + column + " IS NOT NULL AND " + column + " <> '')" : column + " IS NOT NULL";
        };
    }

    private String addComparison(
        String column, String comparisonOperator, ColumnSpec columnSpec, DataTableRowFilter filter,
        List<FilterParameter> filterParameters) {

        filterParameters.add(
            new FilterParameter(columnSpec.type(), coerceFilterValue(columnSpec, filter.value())));

        return column + " " + comparisonOperator + " ?";
    }

    private String addLike(
        String column, String prefix, String suffix, boolean negated, ColumnSpec columnSpec,
        DataTableRowFilter filter, List<FilterParameter> filterParameters) {

        String value = (String) coerceTextFilterValue(columnSpec, filter);

        filterParameters.add(new FilterParameter(ColumnType.STRING, prefix + escapeLikePattern(value) + suffix));

        if (negated) {
            return "(" + column + " IS NULL OR " + column + " NOT LIKE ? ESCAPE '\\')";
        }

        return column + " LIKE ? ESCAPE '\\'";
    }

    private Object coerceTextFilterValue(ColumnSpec columnSpec, DataTableRowFilter filter) {
        if (columnSpec.type() != ColumnType.STRING) {
            throw new IllegalArgumentException(
                "Filter operation %s is only supported on text columns, but column '%s' is %s".formatted(
                    filter.operation(), columnSpec.name(), columnSpec.type()));
        }

        return coerceFilterValue(columnSpec, filter.value());
    }

    private Object coerceFilterValue(ColumnSpec columnSpec, @Nullable Object rawValue) {
        return Objects.requireNonNull(
            coerceValueStrictly(columnSpec, rawValue), "filter values are checked by DataTableRowFilter");
    }

    private @Nullable Object coerceValueStrictly(ColumnSpec columnSpec, @Nullable Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        ColumnType columnType = columnSpec.type();

        if (rawValue instanceof String string && string.isBlank()) {
            return columnType == ColumnType.STRING ? "" : null;
        }

        if (rawValue instanceof Collection<?> || rawValue instanceof Map<?, ?>) {
            throw new IllegalArgumentException(
                "Value for column '%s' must be a single value, not a list or an object".formatted(columnSpec.name()));
        }

        boolean dateColumn = columnType == ColumnType.DATE || columnType == ColumnType.DATE_TIME;

        if (dateColumn && rawValue instanceof String string && EPOCH_PATTERN.matcher(string.trim())
            .matches()) {

            throw new IllegalArgumentException(
                "Value '%s' is not a valid %s for column '%s'; use an ISO format such as %s".formatted(
                    rawValue, columnType == ColumnType.DATE ? "date" : "date_time", columnSpec.name(),
                    columnType == ColumnType.DATE ? "2026-01-31" : "2026-01-31T08:00:00"));
        }

        Object coercedValue = switch (columnType) {
            case BOOLEAN -> toBoolean(rawValue);
            case INTEGER -> toLong(rawValue);
            default -> coerceValue(columnType, rawValue);
        };

        if (coercedValue == null) {
            throw new IllegalArgumentException(
                "Value '%s' is not a valid %s for column '%s'".formatted(
                    rawValue, columnType.name()
                        .toLowerCase(Locale.ROOT),
                    columnSpec.name()));
        }

        return coercedValue;
    }

    private static @Nullable Boolean toBoolean(Object rawValue) {
        if (rawValue instanceof Boolean booleanValue) {
            return booleanValue;
        }

        String value = String.valueOf(rawValue)
            .trim()
            .toLowerCase(Locale.ROOT);

        return switch (value) {
            case "true", "t", "yes", "y", "1" -> Boolean.TRUE;
            case "false", "f", "no", "n", "0" -> Boolean.FALSE;
            default -> null;
        };
    }

    private static @Nullable Long toLong(Object rawValue) {
        try {
            BigDecimal decimal = (rawValue instanceof Number number)
                ? toBigDecimal(number) : new BigDecimal(String.valueOf(rawValue)
                    .trim());

            return decimal.longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            return null;
        }
    }

    private static String escapeLikePattern(String value) {
        return value.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

    private static ColumnSpec getColumnSpec(List<ColumnSpec> columnSpecs, String columnName) {
        return columnSpecs.stream()
            .filter(columnSpec -> {
                String name = columnSpec.name();

                return name.equalsIgnoreCase(columnName);
            })
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown column: " + columnName));
    }

    private static List<?> getFilterValues(DataTableRowFilter filter) {
        return (List<?>) Objects.requireNonNull(filter.value(), "list values are checked by DataTableRowFilter");
    }

    private void checkHasId(String physical) {
        String sql =
            "SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ? AND " +
                "column_name = 'id'";

        List<Integer> exists =
            jdbcTemplate.query(sql, preparedStatement -> preparedStatement.setString(1, physical), (rs, rowNum) -> 1);

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
                case BOOLEAN -> BooleanUtils.parseBoolean(trimmed);
                case INTEGER -> com.bytechef.commons.util.NumberUtils.parseLong(trimmed);
                case NUMBER -> com.bytechef.commons.util.NumberUtils.parseBigDecimal(trimmed);
                case DATE -> DateUtils.parseSqlDate(trimmed);
                case DATE_TIME -> DateUtils.parseSqlTimestamp(trimmed);
            };
        }

        return switch (type) {
            case STRING -> String.valueOf(rawValue);
            case BOOLEAN -> (rawValue instanceof Boolean) ? rawValue
                : BooleanUtils.parseBoolean(String.valueOf(rawValue));
            case INTEGER -> (rawValue instanceof Number n) ? Long.valueOf(n.longValue())
                : com.bytechef.commons.util.NumberUtils.parseLong(String.valueOf(rawValue));
            case NUMBER -> (rawValue instanceof BigDecimal bd)
                ? bd
                : (rawValue instanceof Number n) ? toBigDecimal(n)
                    : com.bytechef.commons.util.NumberUtils.parseBigDecimal(String.valueOf(rawValue));
            case DATE ->
                (rawValue instanceof java.sql.Date date) ? date
                    : (rawValue instanceof Date date2) ? new Date(date2.getTime())
                        : DateUtils.parseSqlDate(String.valueOf(rawValue));
            case DATE_TIME ->
                (rawValue instanceof Timestamp timestamp) ? timestamp
                    : (rawValue instanceof Date date3) ? new Timestamp(date3.getTime())
                        : DateUtils.parseSqlTimestamp(String.valueOf(rawValue));
        };
    }

    private String escapeIdentifier(String identifier) {
        Assert.hasText(identifier, "identifier must not be empty");

        String normalizedName = identifier.toLowerCase(Locale.ROOT);

        Assert.isTrue(normalizedName.matches("[a-z_][a-z0-9_]*"), "Invalid identifier: " + identifier);

        return '"' + normalizedName + '"';
    }

    private static DataTableRow toDataTableRow(ResultSet resultSet, List<String> columnNames) throws SQLException {
        Map<String, Object> values = new HashMap<>();

        for (String columnName : columnNames) {
            values.put(columnName, resultSet.getObject(columnName));
        }

        return new DataTableRow(resultSet.getLong("id"), values);
    }

    private List<ColumnSpec> listColumns(String physicalName) {
        String sql = "SELECT column_name, data_type FROM information_schema.columns " +
            "WHERE table_schema = current_schema() AND table_name = ? ORDER BY ordinal_position";

        return jdbcTemplate.query(sql, preparedStatement -> preparedStatement.setString(1, physicalName),
            (rs, rowNum) -> {
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

    private void setParam(PreparedStatement preparedStatement, int index, ColumnType type, Object value)
        throws SQLException {
        if (value == null) {
            int sqlType = switch (type) {
                case STRING -> Types.VARCHAR;
                case NUMBER -> Types.DECIMAL;
                case INTEGER -> Types.BIGINT;
                case DATE -> Types.DATE;
                case DATE_TIME -> Types.TIMESTAMP;
                case BOOLEAN -> Types.BOOLEAN;
            };

            preparedStatement.setNull(index, sqlType);

            return;
        }

        switch (type) {
            case STRING -> preparedStatement.setString(index, String.valueOf(value));
            case NUMBER ->
                preparedStatement.setBigDecimal(index,
                    (value instanceof BigDecimal bd) ? bd : toBigDecimal((Number) value));
            case INTEGER -> {
                switch (value) {
                    case Integer integer -> preparedStatement.setInt(index, integer);
                    case Long aLong -> preparedStatement.setLong(index, aLong);
                    case Number number -> preparedStatement.setLong(index, number.longValue());
                    default -> preparedStatement.setObject(index, value, Types.BIGINT);
                }
            }
            case DATE -> {
                if (value instanceof java.sql.Date d)
                    preparedStatement.setDate(index, d);
                else if (value instanceof Date d)
                    preparedStatement.setDate(index, new java.sql.Date(d.getTime()));
                else
                    preparedStatement.setDate(index, DateUtils.parseSqlDate(String.valueOf(value)));
            }
            case DATE_TIME -> {
                if (value instanceof Timestamp t)
                    preparedStatement.setTimestamp(index, t);
                else if (value instanceof Date d)
                    preparedStatement.setTimestamp(index, new Timestamp(d.getTime()));
                else
                    preparedStatement.setTimestamp(
                        index,
                        DateUtils.parseSqlTimestamp(String.valueOf(value)));
            }
            case BOOLEAN -> {
                if (value instanceof Boolean b)
                    preparedStatement.setBoolean(index, b);
                else
                    preparedStatement.setBoolean(index, Boolean.parseBoolean(String.valueOf(value)));
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

    @SuppressFBWarnings({
        "SQL_INJECTION_JDBC", "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE"
    })
    private PreparedStatement createInsertPreparedStatement(
        Connection connection, String sql, PreparedStatementSetter preparedStatementSetter) throws SQLException {

        PreparedStatement preparedStatement = connection.prepareStatement(sql, new String[] {
            "id"
        });

        preparedStatementSetter.setValues(preparedStatement);

        return preparedStatement;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRow deleteRowReturningValues(String physicalName, long id) {
        List<String> columnNames = listColumns(physicalName).stream()
            .map(ColumnSpec::name)
            .filter(name -> !"id".equalsIgnoreCase(name))
            .toList();

        String returningColumns = "\"id\"" + (columnNames.isEmpty() ? "" : ", " + columnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", ")));

        String sql = "DELETE FROM " + escapeIdentifier(physicalName) + " WHERE \"id\" = ? RETURNING " +
            returningColumns;

        List<DataTableRow> deletedDataTableRows = jdbcTemplate.query(
            sql, preparedStatement -> preparedStatement.setLong(1, id),
            (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));

        return deletedDataTableRows.isEmpty() ? null : deletedDataTableRows.getFirst();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRow deleteRowAfterReadingValues(
        String baseName, String physicalName, long id, long environmentId) {

        DataTableRow dataTableRow = getRow(baseName, id, environmentId);

        if (dataTableRow == null) {
            return null;
        }

        String sql = "DELETE FROM " + escapeIdentifier(physicalName) + " WHERE \"id\" = ?";

        int deletedRowCount =
            jdbcTemplate.update(sql, (PreparedStatementSetter) preparedStatement -> preparedStatement.setLong(1, id));

        return deletedRowCount == 0 ? null : dataTableRow;
    }

    private boolean isReturningSupported() {
        if (returningSupported == null) {
            returningSupported = resolveReturningSupported();
        }

        return returningSupported;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRow insertRowReturningValues(
        String physicalName, String valuesClause, List<String> allColumnNames,
        PreparedStatementSetter preparedStatementSetter) {

        List<String> columnNames = allColumnNames.stream()
            .filter(columnName -> !"id".equalsIgnoreCase(columnName))
            .toList();

        String returningColumns = "\"id\"" + (columnNames.isEmpty() ? "" : ", " + columnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", ")));

        String sql = "INSERT INTO " + escapeIdentifier(physicalName) + valuesClause + " RETURNING " + returningColumns;

        List<DataTableRow> insertedDataTableRows = jdbcTemplate.query(
            sql, preparedStatementSetter, (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));

        return insertedDataTableRows.isEmpty() ? null : insertedDataTableRows.getFirst();
    }

    private DataTableRow insertRowAfterInserting(
        String baseName, String physicalName, String valuesClause, long environmentId,
        PreparedStatementSetter preparedStatementSetter) {

        String sql = "INSERT INTO " + escapeIdentifier(physicalName) + valuesClause;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
            connection -> createInsertPreparedStatement(connection, sql, preparedStatementSetter), keyHolder);

        Number generatedKey = keyHolder.getKey();

        if (generatedKey == null) {
            return null;
        }

        return getRow(baseName, generatedKey.longValue(), environmentId);
    }

    private boolean resolveReturningSupported() {
        DataSource dataSource = jdbcTemplate.getDataSource();

        if (dataSource == null) {
            return true;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            String databaseProductName = databaseMetaData.getDatabaseProductName();

            databaseProductName = databaseProductName.toLowerCase(Locale.ROOT);

            return !databaseProductName.contains("h2");
        } catch (SQLException sqlException) {
            log.warn("Unable to determine the database product name, assuming RETURNING is supported", sqlException);

            return true;
        }
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRow updateRowAfterUpdating(
        String baseName, String physicalName, String setClause, long id, long environmentId,
        PreparedStatementSetter preparedStatementSetter) {

        String sql = "UPDATE " + escapeIdentifier(physicalName) + " SET " + setClause + " WHERE \"id\" = ?";

        int updatedRowCount = jdbcTemplate.update(sql, preparedStatementSetter);

        if (updatedRowCount == 0) {
            return null;
        }

        return getRow(baseName, id, environmentId);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRow updateRowReturningValues(
        String physicalName, String setClause, List<String> allColumnNames,
        PreparedStatementSetter preparedStatementSetter) {

        List<String> columnNames = allColumnNames.stream()
            .filter(columnName -> !"id".equalsIgnoreCase(columnName))
            .toList();

        String returningColumns = "\"id\"" + (columnNames.isEmpty() ? "" : ", " + columnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", ")));

        String sql = "UPDATE " + escapeIdentifier(physicalName) + " SET " + setClause +
            " WHERE \"id\" = ? RETURNING " + returningColumns;

        List<DataTableRow> updatedDataTableRows = jdbcTemplate.query(
            sql, preparedStatementSetter, (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));

        return updatedDataTableRows.isEmpty() ? null : updatedDataTableRows.getFirst();
    }

    private record ColumnValue(ColumnSpec columnSpec, @Nullable Object value) {
    }

    private record FilterParameter(ColumnType columnType, Object value) {
    }

    private record WhereClause(String sql, List<FilterParameter> filterParameters) {
    }
}
