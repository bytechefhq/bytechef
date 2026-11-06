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
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.writer.CsvWriter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.ConnectionCallback;
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

    static final int MAX_ARRAY_PARAMETER_LENGTH = 50_000;

    private static final long MIN_EPOCH_MILLISECONDS = 100_000_000_000L;

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone(ZoneOffset.UTC);

    private static final Pattern EPOCH_PATTERN = Pattern.compile("-?\\d+");

    private static final int MAX_EXTERNAL_ID_LENGTH = 255;

    private static final DateTimeFormatter[] STRICT_DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("uuuu-M-d")
            .withResolverStyle(ResolverStyle.STRICT)
    };

    private static final DateTimeFormatter[] STRICT_DATE_TIME_FORMATTERS = {
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
            .withResolverStyle(ResolverStyle.STRICT)
    };

    private final ApplicationEventPublisher applicationEventPublisher;
    private final JdbcTemplate jdbcTemplate;

    private Boolean returningSupported;

    @SuppressFBWarnings("EI")
    public DataTableRowServiceImpl(ApplicationEventPublisher applicationEventPublisher, JdbcTemplate jdbcTemplate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public long countRows(String baseName, DataTableRowConditions conditions, long environmentId) {
        Objects.requireNonNull(conditions, "conditions");

        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        WhereClause whereClause = buildWhereClause(columnSpecs, conditions);

        String sql = "SELECT COUNT(*) FROM " + escapeIdentifier(physicalName) + whereClause.sql();

        List<Long> counts = jdbcTemplate.query(
            sql, preparedStatement -> setFilterParameters(preparedStatement, 1, whereClause.filterParameters()),
            (resultSet, rowNum) -> resultSet.getLong(1));

        return counts.isEmpty() ? 0L : counts.getFirst();
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

        checkHasId(baseName, physicalName);

        DataTableRow deletedDataTableRow;

        if (isReturningSupported()) {
            deletedDataTableRow = deleteRowReturningValues(physicalName, id);
        } else {
            deletedDataTableRow = deleteRowAfterReadingValues(baseName, physicalName, id, environmentId);
        }

        if (deletedDataTableRow == null) {
            return false;
        }

        publishWebhookEvent(baseName, DataTableWebhookType.RECORD_DELETED, List.of(deletedDataTableRow), environmentId);

        return true;
    }

    @Override
    @Transactional
    public List<DataTableRow> deleteAllRows(String baseName, long environmentId) {
        return deleteMatchingRows(baseName, DataTableRowConditions.matchAll(), environmentId);
    }

    @Override
    @Transactional
    public List<DataTableRow> deleteRows(String baseName, DataTableRowConditions conditions, long environmentId) {
        Objects.requireNonNull(conditions, "conditions");

        conditions.requireAtLeastOneCondition();

        return deleteMatchingRows(baseName, conditions, environmentId);
    }

    /**
     * Deletes every row of a data table matching the given OR groups of AND conditions.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because filter columns must match an
     * existing column and all identifiers are validated through {@link #escapeIdentifier(String)} and
     * {@link #validateBaseName(String)}. Filter values use parameterized queries.
     */
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<DataTableRow> deleteMatchingRows(
        String baseName, DataTableRowConditions conditions, long environmentId) {

        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> columnNames = getNonIdColumnNames(columnSpecs);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        WhereClause whereClause = buildWhereClause(columnSpecs, conditions);

        String tableName = escapeIdentifier(physicalName);

        List<DataTableRow> deletedDataTableRows;

        if (isReturningSupported()) {
            String sql = "DELETE FROM " + tableName + whereClause.sql() + " RETURNING " +
                buildSelectColumns(columnNames, hasExternalIdColumn);

            deletedDataTableRows = jdbcTemplate.query(
                sql, preparedStatement -> setFilterParameters(preparedStatement, 1, whereClause.filterParameters()),
                (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames, hasExternalIdColumn));
        } else {
            deletedDataTableRows = selectRowsForUpdate(tableName, columnNames, hasExternalIdColumn, whereClause);

            List<Long> ids = deletedDataTableRows.stream()
                .map(DataTableRow::id)
                .toList();

            if (!ids.isEmpty()) {
                jdbcTemplate.update(
                    "DELETE FROM " + tableName + " WHERE " + buildAnyCondition("\"id\"", ids.size()),
                    (PreparedStatementSetter) preparedStatement -> setIds(preparedStatement, 1, ids));
            }
        }

        List<DataTableRow> sortedDataTableRows = sortById(deletedDataTableRows);

        publishWebhookEvent(baseName, DataTableWebhookType.RECORD_DELETED, sortedDataTableRows, environmentId);

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
    public @Nullable DataTableRow getRow(String baseName, long id, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> columnNames = getNonIdColumnNames(columnSpecs);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        String sql = "SELECT " + buildSelectColumns(columnNames, hasExternalIdColumn) + " FROM " +
            escapeIdentifier(physicalName) + " WHERE \"id\" = ?";

        List<DataTableRow> rows = jdbcTemplate.query(
            sql, preparedStatement -> preparedStatement.setLong(1, id),
            (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames, hasExternalIdColumn));

        return rows.isEmpty() ? null : rows.getFirst();
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public Optional<DataTableRow> fetchRowByExternalId(String baseName, String externalId, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        if (!hasExternalIdColumn(columnSpecs)) {
            return Optional.empty();
        }

        List<String> columnNames = getNonIdColumnNames(columnSpecs);

        String sql = "SELECT " + buildSelectColumns(columnNames, true) + " FROM " + escapeIdentifier(physicalName) +
            " WHERE " + escapeIdentifier(ReservedColumns.EXTERNAL_ID) + " = ?";

        List<DataTableRow> rows = jdbcTemplate.query(
            sql, preparedStatement -> preparedStatement.setString(1, externalId),
            (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames, true));

        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    @Override
    public String exportCsv(String baseName, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);
        List<String> columnNames = getNonIdColumnNames(columnSpecs);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        List<String> headerNames = new ArrayList<>();

        if (hasExternalIdColumn) {
            headerNames.add(ReservedColumns.EXTERNAL_ID);
        }

        headerNames.addAll(columnNames);

        StringWriter stringWriter = new StringWriter();

        CsvWriter csvWriter = CsvWriter.builder()
            .build(stringWriter);

        // Header
        csvWriter.writeRow(headerNames);

        List<DataTableRow> dataTableRows = listRows(baseName, Integer.MAX_VALUE, 0, environmentId);

        for (DataTableRow dataTableRow : dataTableRows) {
            List<String> curValues = new ArrayList<>();

            if (hasExternalIdColumn) {
                String externalId = dataTableRow.externalId();

                curValues.add(externalId == null ? "" : externalId);
            }

            for (String columnName : columnNames) {
                Map<String, Object> values = dataTableRow.values();

                Object value = values.get(columnName);

                curValues.add(toCsvValue(value));
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
    @Transactional
    public int importCsv(String baseName, String csv, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        if (csv == null) {
            return 0;
        }

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        List<String> columnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .toList();

        CsvReader.CsvReaderBuilder csvReaderBuilder = CsvReader.builder();

        List<DataTableRow> insertedDataTableRows = new ArrayList<>();

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
                        if (hasExternalIdColumn && ReservedColumns.EXTERNAL_ID.equalsIgnoreCase(header)) {
                            mappedColumnNames.add(ReservedColumns.EXTERNAL_ID);

                            continue;
                        }

                        if (ReservedColumns.isReserved(header)) {
                            mappedColumnNames.add(null);

                            continue;
                        }

                        String columnName = columnNames.stream()
                            .filter(curColumnName -> curColumnName.equalsIgnoreCase(header))
                            .findFirst()
                            .orElse(null);

                        mappedColumnNames.add(columnName);
                    }

                    if (mappedColumnNames.stream()
                        .allMatch(Objects::isNull)) {

                        List<String> tableColumnNames = columnNames.stream()
                            .filter(columnName -> !ReservedColumns.isReserved(columnName))
                            .toList();

                        throw new IllegalArgumentException(
                            "CSV header %s matches no column of table '%s'; its columns are %s; no row was imported"
                                .formatted(headers, baseName, tableColumnNames));
                    }

                    continue;
                }

                if (fieldStream.allMatch(field -> field == null || field.isBlank())) {
                    continue;
                }

                Map<String, Object> values = new HashMap<>();
                String externalId = null;

                int limit = Math.min(mappedColumnNames.size(), fields.size());

                for (int col = 0; col < limit; col++) {
                    String curColumnName = mappedColumnNames.get(col);

                    if (curColumnName == null) {
                        continue;
                    }

                    String field = fields.get(col);

                    if (ReservedColumns.EXTERNAL_ID.equals(curColumnName)) {
                        externalId = (field == null || field.isBlank()) ? null : field.trim();

                        if (externalId != null && externalId.length() > MAX_EXTERNAL_ID_LENGTH) {
                            throw new DataTableException(
                                "CSV line %d: external_id must be at most %d characters; no row was imported"
                                    .formatted(csvRow.getOriginalLineNumber(), MAX_EXTERNAL_ID_LENGTH),
                                DataTableErrorType.CSV_INVALID);
                        }

                        continue;
                    }

                    values.put(curColumnName, (field == null || field.isEmpty()) ? null : field);
                }

                try {
                    insertedDataTableRows.add(
                        insertRowWithoutEvent(
                            baseName, physicalName, columnSpecs, values, externalId, environmentId,
                            new HashSet<>()));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(
                        "CSV line %d: %s; no row was imported".formatted(
                            csvRow.getOriginalLineNumber(), exception.getMessage()),
                        exception);
                } catch (DataIntegrityViolationException dataIntegrityViolationException) {
                    throw withRowPosition(
                        "CSV line " + csvRow.getOriginalLineNumber(), "no row was imported",
                        dataIntegrityViolationException);
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to import CSV", exception);
        }

        publishWebhookEvent(baseName, DataTableWebhookType.RECORD_CREATED, insertedDataTableRows, environmentId);

        return insertedDataTableRows.size();
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
        return insertRow(baseName, values, null, environmentId);
    }

    @Override
    public DataTableRow insertRow(
        String baseName, Map<String, Object> values, @Nullable String externalId, long environmentId) {

        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        Set<String> ignoredColumnNames = new LinkedHashSet<>();

        DataTableRow dataTableRow = insertRowWithoutEvent(
            baseName, physicalName, listColumns(physicalName), values, externalId, environmentId, ignoredColumnNames);

        logIgnoredColumnNames(baseName, environmentId, ignoredColumnNames);

        publishWebhookEvent(baseName, DataTableWebhookType.RECORD_CREATED, List.of(dataTableRow), environmentId);

        return dataTableRow;
    }

    @Override
    @Transactional
    public List<DataTableRow> insertRows(String baseName, List<Map<String, Object>> rowValues, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);
        List<DataTableRow> insertedDataTableRows = new ArrayList<>();
        Set<String> ignoredColumnNames = new LinkedHashSet<>();

        for (int index = 0; index < rowValues.size(); index++) {
            try {
                insertedDataTableRows.add(
                    insertRowWithoutEvent(
                        baseName, physicalName, columnSpecs, rowValues.get(index), null, environmentId,
                        ignoredColumnNames));
            } catch (IllegalArgumentException exception) {
                IllegalArgumentException illegalArgumentException = new IllegalArgumentException(
                    "Record %d: %s; no record was inserted".formatted(index + 1, exception.getMessage()));

                illegalArgumentException.addSuppressed(exception);

                throw illegalArgumentException;
            } catch (DataIntegrityViolationException dataIntegrityViolationException) {
                throw withRowPosition("Record " + (index + 1), "no record was inserted",
                    dataIntegrityViolationException);
            }
        }

        logIgnoredColumnNames(baseName, environmentId, ignoredColumnNames);

        publishWebhookEvent(baseName, DataTableWebhookType.RECORD_CREATED, insertedDataTableRows, environmentId);

        return insertedDataTableRows;
    }

    @Override
    @Transactional
    public List<DataTableRow> insertRows(
        String baseName, List<NewRow> newRows, CreateStrategy createStrategy, long environmentId) {

        if (createStrategy == CreateStrategy.UPSERT) {
            boolean missingExternalId = newRows.stream()
                .anyMatch(newRow -> {
                    String externalId = newRow.externalId();

                    return externalId == null || externalId.isBlank();
                });

            if (missingExternalId) {
                throw new DataTableException(
                    "Every row needs an externalId under the UPSERT strategy",
                    DataTableErrorType.ROW_EXTERNAL_ID_REQUIRED);
            }
        }

        if (newRows.isEmpty()) {
            return List.of();
        }

        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<DataTableRow> dataTableRows = new ArrayList<>(newRows.size());
        List<DataTableRow> createdDataTableRows = new ArrayList<>();
        List<DataTableRow> updatedDataTableRows = new ArrayList<>();
        Set<String> ignoredColumnNames = new LinkedHashSet<>();

        for (int index = 0; index < newRows.size(); index++) {
            NewRow newRow = newRows.get(index);

            try {
                if (createStrategy == CreateStrategy.UPSERT) {
                    UpsertResult upsertResult = upsertRowWithoutEvent(
                        baseName, physicalName, columnSpecs, Objects.requireNonNull(newRow.externalId()),
                        newRow.values(), environmentId, ignoredColumnNames);

                    DataTableRow dataTableRow = upsertResult.row();

                    dataTableRows.add(dataTableRow);

                    if (upsertResult.created()) {
                        createdDataTableRows.add(dataTableRow);
                    } else {
                        updatedDataTableRows.add(dataTableRow);
                    }
                } else {
                    DataTableRow dataTableRow = insertRowWithoutEvent(
                        baseName, physicalName, columnSpecs, newRow.values(), newRow.externalId(), environmentId,
                        ignoredColumnNames);

                    dataTableRows.add(dataTableRow);
                    createdDataTableRows.add(dataTableRow);
                }
            } catch (IllegalArgumentException exception) {
                IllegalArgumentException illegalArgumentException = new IllegalArgumentException(
                    "Record %d: %s; no record was inserted".formatted(index + 1, exception.getMessage()));

                illegalArgumentException.addSuppressed(exception);

                throw illegalArgumentException;
            } catch (DataIntegrityViolationException dataIntegrityViolationException) {
                throw withRowPosition(
                    "Record " + (index + 1), "no record was inserted", dataIntegrityViolationException);
            }
        }

        logIgnoredColumnNames(baseName, environmentId, ignoredColumnNames);

        publishWebhookEvent(baseName, DataTableWebhookType.RECORD_CREATED, createdDataTableRows, environmentId);
        publishWebhookEvent(baseName, DataTableWebhookType.RECORD_UPDATED, updatedDataTableRows, environmentId);

        return dataTableRows;
    }

    private static DataIntegrityViolationException withRowPosition(
        String rowPosition, String outcome, DataIntegrityViolationException dataIntegrityViolationException) {

        Throwable mostSpecificCause = dataIntegrityViolationException.getMostSpecificCause();

        String message = mostSpecificCause.getMessage();

        if (message == null || message.isBlank()) {
            Class<? extends Throwable> mostSpecificCauseClass = mostSpecificCause.getClass();

            message = mostSpecificCauseClass.getSimpleName();
        }

        String firstLine = message.lines()
            .findFirst()
            .orElse(message)
            .strip();

        DataIntegrityViolationException positionedDataIntegrityViolationException = new DataIntegrityViolationException(
            "%s: %s; %s".formatted(rowPosition, firstLine, outcome));

        positionedDataIntegrityViolationException.addSuppressed(dataIntegrityViolationException);

        return positionedDataIntegrityViolationException;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRow insertRowWithoutEvent(
        String baseName, String physicalName, List<ColumnSpec> columnSpecs, Map<String, Object> values,
        @Nullable String externalId, long environmentId, Set<String> ignoredColumnNames) {

        List<String> allColumnNames = getColumnNames(columnSpecs);

        List<ColumnValue> columnValues = getColumnValues(
            columnSpecs, values, ValuePolicy.SINGLE_ROW, ignoredColumnNames);

        List<String> insertColumnNames = new ArrayList<>();

        for (ColumnValue columnValue : columnValues) {
            ColumnSpec columnSpec = columnValue.columnSpec();

            insertColumnNames.add(columnSpec.name());
        }

        if (externalId != null) {
            Assert.isTrue(
                hasExternalIdColumn(columnSpecs), "Table " + physicalName + " has no external_id column");

            insertColumnNames.add(ReservedColumns.EXTERNAL_ID);
        }

        String columnsClause = insertColumnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", "));

        String valuesClause = insertColumnNames.isEmpty()
            ? " DEFAULT VALUES"
            : (" (" + columnsClause + ") VALUES (" + buildPlaceholders(insertColumnNames.size()) + ")");

        PreparedStatementSetter preparedStatementSetter = preparedStatement -> {
            int index = setColumnValues(preparedStatement, 1, columnValues);

            if (externalId != null) {
                preparedStatement.setString(index, externalId);
            }
        };

        DataTableRow result;

        try {
            if (isReturningSupported()) {
                result = insertRowReturningValues(physicalName, valuesClause, allColumnNames, preparedStatementSetter);
            } else {
                result = insertRowAfterInserting(
                    baseName, physicalName, valuesClause, environmentId, preparedStatementSetter);
            }
        } catch (DuplicateKeyException duplicateKeyException) {
            throw new DataTableException(
                "A row with external id '" + externalId + "' already exists", duplicateKeyException,
                DataTableErrorType.ROW_EXTERNAL_ID_CONFLICT);
        }

        if (result == null) {
            throw new IllegalStateException("Failed to insert row");
        }

        return result;
    }

    @Override
    public List<DataTableRow> listRows(String baseName, int limit, int offset, long environmentId) {
        return findRows(baseName, DataTableRowConditions.matchAll(), null, limit, offset, environmentId);
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
        String baseName, DataTableRowConditions conditions, @Nullable DataTableRowSort sort, int limit,
        int offset, long environmentId) {

        Objects.requireNonNull(conditions, "conditions");

        if (limit < 0 || offset < 0) {
            throw new IllegalArgumentException(
                "limit and offset must not be negative, got limit=%d and offset=%d".formatted(limit, offset));
        }

        return queryRows(baseName, conditions, sort == null ? List.of() : List.of(sort), limit, offset, environmentId);
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<DataTableRow> queryRows(
        String baseName, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int limit, int offset,
        long environmentId) {

        Objects.requireNonNull(conditions, "conditions");
        Objects.requireNonNull(sorts, "sorts");

        if (limit < 0 || offset < 0) {
            throw new IllegalArgumentException(
                "limit and offset must not be negative, got limit=%d and offset=%d".formatted(limit, offset));
        }

        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> columnNames = getNonIdColumnNames(columnSpecs);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        WhereClause whereClause = buildWhereClause(columnSpecs, conditions);

        List<String> orderByTerms = new ArrayList<>();

        for (DataTableRowSort sort : sorts) {
            ColumnSpec sortColumnSpec = getColumnSpec(columnSpecs, sort.column());
            DataTableRowSort.Direction direction = sort.direction();

            orderByTerms.add(escapeIdentifier(sortColumnSpec.name()) + " " + direction.name() + " NULLS LAST");
        }

        orderByTerms.add("\"id\"");

        String sql = "SELECT " + buildSelectColumns(columnNames, hasExternalIdColumn) + " FROM " +
            escapeIdentifier(physicalName) + whereClause.sql() + " ORDER BY " + String.join(", ", orderByTerms) +
            " LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql, preparedStatement -> {
            int index = setFilterParameters(preparedStatement, 1, whereClause.filterParameters());

            preparedStatement.setInt(index++, limit);
            preparedStatement.setInt(index, offset);
        }, (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames, hasExternalIdColumn));
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public DataTableRow updateRow(String baseName, long id, Map<String, Object> values, long environmentId) {
        return updateRow(baseName, id, values, null, environmentId);
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public DataTableRow updateRow(
        String baseName, long id, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        long environmentId) {

        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> allColumnNames = getColumnNames(columnSpecs);

        Set<String> ignoredColumnNames = new LinkedHashSet<>();

        List<ColumnValue> columnValues = getColumnValues(
            columnSpecs, values, ValuePolicy.SINGLE_ROW, ignoredColumnNames);

        logIgnoredColumnNames(baseName, environmentId, ignoredColumnNames);

        List<String> setClauses = new ArrayList<>();

        if (!columnValues.isEmpty()) {
            setClauses.add(buildSetClause(columnValues));
        }

        if (externalIdPatch != null) {
            Assert.isTrue(
                hasExternalIdColumn(columnSpecs), "Table " + physicalName + " has no external_id column");

            setClauses.add(escapeIdentifier(ReservedColumns.EXTERNAL_ID) + " = ?");
        }

        if (setClauses.isEmpty()) {
            DataTableRow dataTableRow = getRow(baseName, id, environmentId);

            if (dataTableRow == null) {
                throw new DataTableException("Row not found: id=" + id, DataTableErrorType.ROW_NOT_FOUND);
            }

            return dataTableRow;
        }

        String setClause = String.join(", ", setClauses);

        PreparedStatementSetter preparedStatementSetter = preparedStatement -> {
            int index = setColumnValues(preparedStatement, 1, columnValues);

            if (externalIdPatch != null) {
                preparedStatement.setString(index++, externalIdPatch.externalId());
            }

            preparedStatement.setLong(index, id);
        };

        DataTableRow updatedDataTableRow;

        try {
            if (isReturningSupported()) {
                updatedDataTableRow = updateRowReturningValues(
                    physicalName, setClause, allColumnNames, preparedStatementSetter);
            } else {
                updatedDataTableRow = updateRowAfterUpdating(
                    baseName, physicalName, setClause, id, environmentId, preparedStatementSetter);
            }
        } catch (DuplicateKeyException duplicateKeyException) {
            throw new DataTableException(
                "A row with external id '" + (externalIdPatch == null ? null : externalIdPatch.externalId()) +
                    "' already exists",
                duplicateKeyException, DataTableErrorType.ROW_EXTERNAL_ID_CONFLICT);
        }

        if (updatedDataTableRow == null) {
            throw new DataTableException("Row not found: id=" + id, DataTableErrorType.ROW_NOT_FOUND);
        }

        publishWebhookEvent(baseName, DataTableWebhookType.RECORD_UPDATED, List.of(updatedDataTableRow), environmentId);

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
        String baseName, DataTableRowConditions conditions, Map<String, Object> values, long environmentId) {

        Objects.requireNonNull(conditions, "conditions");

        conditions.requireAtLeastOneCondition();

        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> columnNames = getNonIdColumnNames(columnSpecs);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        WhereClause whereClause = buildWhereClause(columnSpecs, conditions);

        List<ColumnValue> columnValues = getColumnValues(columnSpecs, values, ValuePolicy.BULK_UPDATE, Set.of());

        if (columnValues.isEmpty()) {
            throw new IllegalArgumentException("Updating rows requires a value for at least one column");
        }

        String tableName = escapeIdentifier(physicalName);
        String setClause = buildSetClause(columnValues);

        List<DataTableRow> updatedDataTableRows;

        if (isReturningSupported()) {
            String sql = "UPDATE " + tableName + " SET " + setClause + whereClause.sql() + " RETURNING " +
                buildSelectColumns(columnNames, hasExternalIdColumn);

            updatedDataTableRows = jdbcTemplate.query(sql, preparedStatement -> {
                int index = setColumnValues(preparedStatement, 1, columnValues);

                setFilterParameters(preparedStatement, index, whereClause.filterParameters());
            }, (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames, hasExternalIdColumn));
        } else {
            List<Long> ids = selectRowsForUpdate(tableName, columnNames, hasExternalIdColumn, whereClause).stream()
                .map(DataTableRow::id)
                .toList();

            if (ids.isEmpty()) {
                return List.of();
            }

            String idsClause = " WHERE " + buildAnyCondition("\"id\"", ids.size());

            jdbcTemplate.update("UPDATE " + tableName + " SET " + setClause + idsClause,
                (PreparedStatementSetter) preparedStatement -> {
                    int index = setColumnValues(preparedStatement, 1, columnValues);

                    setIds(preparedStatement, index, ids);
                });

            updatedDataTableRows = jdbcTemplate.query(
                "SELECT " + buildSelectColumns(columnNames, hasExternalIdColumn) + " FROM " + tableName + idsClause,
                preparedStatement -> setIds(preparedStatement, 1, ids),
                (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames, hasExternalIdColumn));
        }

        List<DataTableRow> sortedDataTableRows = sortById(updatedDataTableRows);

        publishWebhookEvent(baseName, DataTableWebhookType.RECORD_UPDATED, sortedDataTableRows, environmentId);

        return sortedDataTableRows;
    }

    @Override
    @Transactional
    public UpsertResult upsertRow(
        String baseName, String externalId, Map<String, Object> values, long environmentId) {

        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        checkHasId(baseName, physicalName);

        Set<String> ignoredColumnNames = new LinkedHashSet<>();

        UpsertResult upsertResult = upsertRowWithoutEvent(
            baseName, physicalName, listColumns(physicalName), externalId, values, environmentId, ignoredColumnNames);

        logIgnoredColumnNames(baseName, environmentId, ignoredColumnNames);

        publishWebhookEvent(
            baseName,
            upsertResult.created() ? DataTableWebhookType.RECORD_CREATED : DataTableWebhookType.RECORD_UPDATED,
            List.of(upsertResult.row()), environmentId);

        return upsertResult;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private UpsertResult upsertRowWithoutEvent(
        String baseName, String physicalName, List<ColumnSpec> columnSpecs, String externalId,
        Map<String, Object> values, long environmentId, Set<String> ignoredColumnNames) {

        Assert.hasText(externalId, "externalId must not be empty");
        Assert.isTrue(hasExternalIdColumn(columnSpecs), "Table " + physicalName + " has no external_id column");

        List<ColumnValue> columnValues = getColumnValues(
            columnSpecs, values, ValuePolicy.SINGLE_ROW, ignoredColumnNames);

        if (!isReturningSupported()) {
            return upsertRowAfterReading(
                baseName, physicalName, columnSpecs, externalId, columnValues, environmentId, ignoredColumnNames);
        }

        List<String> columnNames = getNonIdColumnNames(columnSpecs);

        List<String> writtenColumnNames = new ArrayList<>();

        for (ColumnValue columnValue : columnValues) {
            ColumnSpec columnSpec = columnValue.columnSpec();

            writtenColumnNames.add(columnSpec.name());
        }

        List<String> insertColumnNames = new ArrayList<>();

        insertColumnNames.add(ReservedColumns.EXTERNAL_ID);
        insertColumnNames.addAll(writtenColumnNames);

        String columnsClause = insertColumnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", "));

        String updateClause = writtenColumnNames.isEmpty()
            ? escapeIdentifier(ReservedColumns.EXTERNAL_ID) + " = EXCLUDED." +
                escapeIdentifier(ReservedColumns.EXTERNAL_ID)
            : writtenColumnNames.stream()
                .map(columnName -> escapeIdentifier(columnName) + " = EXCLUDED." + escapeIdentifier(columnName))
                .collect(Collectors.joining(", "));

        String sql = "INSERT INTO " + escapeIdentifier(physicalName) + " (" + columnsClause + ") VALUES (" +
            buildPlaceholders(insertColumnNames.size()) + ") ON CONFLICT (" +
            escapeIdentifier(ReservedColumns.EXTERNAL_ID) + ") DO UPDATE SET " + updateClause + " RETURNING " +
            buildSelectColumns(columnNames, true) + ", (xmax = 0) AS \"created\"";

        UpsertResult upsertResult = jdbcTemplate.query(sql, preparedStatement -> {
            preparedStatement.setString(1, externalId);

            setColumnValues(preparedStatement, 2, columnValues);
        }, resultSet -> {
            if (!resultSet.next()) {
                return null;
            }

            return new UpsertResult(toDataTableRow(resultSet, columnNames, true), resultSet.getBoolean("created"));
        });

        if (upsertResult == null) {
            throw new IllegalStateException("Upsert returned no row");
        }

        return upsertResult;
    }

    private UpsertResult upsertRowAfterReading(
        String baseName, String physicalName, List<ColumnSpec> columnSpecs, String externalId,
        List<ColumnValue> columnValues, long environmentId, Set<String> ignoredColumnNames) {

        Optional<DataTableRow> existingDataTableRow = fetchRowByExternalId(baseName, externalId, environmentId);

        Map<String, Object> values = new HashMap<>();

        for (ColumnValue columnValue : columnValues) {
            ColumnSpec columnSpec = columnValue.columnSpec();

            values.put(columnSpec.name(), columnValue.value());
        }

        if (existingDataTableRow.isPresent()) {
            DataTableRow dataTableRow = existingDataTableRow.get();

            if (values.isEmpty()) {
                return new UpsertResult(dataTableRow, false);
            }

            String setClause = buildSetClause(columnValues);

            DataTableRow updatedDataTableRow = updateRowAfterUpdating(
                baseName, physicalName, setClause, dataTableRow.id(), environmentId, preparedStatement -> {
                    int index = setColumnValues(preparedStatement, 1, columnValues);

                    preparedStatement.setLong(index, dataTableRow.id());
                });

            if (updatedDataTableRow == null) {
                throw new IllegalStateException("Upsert returned no row");
            }

            return new UpsertResult(updatedDataTableRow, false);
        }

        return new UpsertResult(
            insertRowWithoutEvent(
                baseName, physicalName, columnSpecs, values, externalId, environmentId, ignoredColumnNames),
            true);
    }

    private static String toCsvValue(@Nullable Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Timestamp timestamp) {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);

            return localDateTime.toString();
        }

        return String.valueOf(value);
    }

    private String buildPlaceholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    private String buildSelectColumns(List<String> columnNames, boolean hasExternalIdColumn) {
        return "\"id\"" + (hasExternalIdColumn ? ", " + escapeIdentifier(ReservedColumns.EXTERNAL_ID) : "") +
            (columnNames.isEmpty() ? "" : ", " + columnNames.stream()
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

    private WhereClause buildWhereClause(List<ColumnSpec> columnSpecs, DataTableRowConditions conditions) {
        List<FilterParameter> filterParameters = new ArrayList<>();
        List<String> orConditions = new ArrayList<>();

        for (List<DataTableRowFilter> andFilters : conditions.groups()) {
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
        List<ColumnSpec> columnSpecs, Map<String, Object> values, ValuePolicy valuePolicy,
        Set<String> ignoredColumnNames) {

        List<ColumnValue> columnValues = new ArrayList<>();
        List<String> unknownColumnNames = new ArrayList<>();
        Set<String> givenColumnNames = new HashSet<>();

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            if ("id".equalsIgnoreCase(columnName)) {
                if (valuePolicy == ValuePolicy.BULK_UPDATE) {
                    throw new IllegalArgumentException(
                        "The id column cannot be updated; add a condition on id to select the rows to change");
                }

                continue;
            }

            if (ReservedColumns.isReserved(columnName)) {
                if (valuePolicy == ValuePolicy.BULK_UPDATE) {
                    throw new IllegalArgumentException(
                        "The %s column cannot be updated by conditions".formatted(ReservedColumns.EXTERNAL_ID));
                }

                continue;
            }

            if (valuePolicy == ValuePolicy.BULK_UPDATE
                && (value == null || (value instanceof String string && string.isBlank()))) {
                throw new IllegalArgumentException(
                    ("Value for column '%s' is empty; updating rows by conditions does not clear columns, use " +
                        "Update Record to clear a column of a single record").formatted(columnName));
            }

            findColumnSpec(columnSpecs, columnName).ifPresentOrElse(
                columnSpec -> {
                    if (!givenColumnNames.add(columnSpec.name())) {
                        throw new IllegalArgumentException(
                            "Column '%s' is given more than once".formatted(columnSpec.name()));
                    }

                    columnValues.add(new ColumnValue(columnSpec, coerceValueStrictly(columnSpec, value, true)));
                },
                () -> unknownColumnNames.add(columnName));
        }

        if (!unknownColumnNames.isEmpty()) {
            if (valuePolicy == ValuePolicy.BULK_UPDATE || columnValues.isEmpty()) {
                throw new IllegalArgumentException(
                    "Unknown column(s): %s; available columns: %s".formatted(
                        String.join(", ", unknownColumnNames), String.join(", ", getNonIdColumnNames(columnSpecs))));
            }

            ignoredColumnNames.addAll(unknownColumnNames);
        }

        return columnValues;
    }

    private static void logIgnoredColumnNames(String baseName, long environmentId, Set<String> ignoredColumnNames) {
        if (!ignoredColumnNames.isEmpty()) {
            log.warn(
                "Ignored values for unknown column(s) {} of data table '{}' in environment {}",
                String.join(", ", ignoredColumnNames), baseName, environmentId);
        }
    }

    private static List<String> getColumnNames(List<ColumnSpec> columnSpecs) {
        return columnSpecs.stream()
            .map(ColumnSpec::name)
            .toList();
    }

    private static List<String> getNonIdColumnNames(List<ColumnSpec> columnSpecs) {
        return columnSpecs.stream()
            .map(ColumnSpec::name)
            .filter(name -> !ReservedColumns.isReserved(name))
            .toList();
    }

    private static boolean hasExternalIdColumn(List<ColumnSpec> columnSpecs) {
        return columnSpecs.stream()
            .anyMatch(columnSpec -> ReservedColumns.EXTERNAL_ID.equalsIgnoreCase(columnSpec.name()));
    }

    private void publishWebhookEvent(
        String baseName, DataTableWebhookType dataTableWebhookType, List<DataTableRow> dataTableRows,
        long environmentId) {

        if (dataTableRows.isEmpty()) {
            return;
        }

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(baseName, dataTableWebhookType, dataTableRows, environmentId));
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<DataTableRow> selectRowsForUpdate(
        String tableName, List<String> columnNames, boolean hasExternalIdColumn, WhereClause whereClause) {

        String sql = "SELECT " + buildSelectColumns(columnNames, hasExternalIdColumn) + " FROM " + tableName +
            whereClause.sql() + " ORDER BY \"id\" FOR UPDATE";

        return jdbcTemplate.query(
            sql, preparedStatement -> setFilterParameters(preparedStatement, 1, whereClause.filterParameters()),
            (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames, hasExternalIdColumn));
    }

    private int setColumnValues(PreparedStatement preparedStatement, int index, List<ColumnValue> columnValues)
        throws SQLException {

        int parameterIndex = index;

        for (ColumnValue columnValue : columnValues) {
            ColumnSpec columnSpec = columnValue.columnSpec();

            setParam(preparedStatement, parameterIndex++, columnSpec.type(), columnValue.value());
        }

        return parameterIndex;
    }

    private int
        setFilterParameters(PreparedStatement preparedStatement, int index, List<FilterParameter> filterParameters)
            throws SQLException {

        int parameterIndex = index;

        for (FilterParameter filterParameter : filterParameters) {
            if (filterParameter.value() instanceof Object[] values) {
                setArrayParam(preparedStatement, parameterIndex++, filterParameter.columnType(), values);
            } else {
                setParam(preparedStatement, parameterIndex++, filterParameter.columnType(), filterParameter.value());
            }
        }

        return parameterIndex;
    }

    private static void setArrayParam(PreparedStatement preparedStatement, int index, ColumnType type, Object[] values)
        throws SQLException {

        Object[] elements = new Object[values.length];

        for (int elementIndex = 0; elementIndex < values.length; elementIndex++) {
            elements[elementIndex] = toArrayElement(type, values[elementIndex]);
        }

        Connection connection = preparedStatement.getConnection();

        Array array = connection.createArrayOf(getArrayElementTypeName(type), elements);

        preparedStatement.setArray(index, array);
    }

    private static void setIds(PreparedStatement preparedStatement, int index, List<Long> ids) throws SQLException {
        int parameterIndex = index;

        for (List<Long> chunk : partition(ids)) {
            setArrayParam(preparedStatement, parameterIndex++, ColumnType.INTEGER, chunk.toArray());
        }
    }

    private static String buildAnyCondition(String column, int valueCount) {
        int chunkCount = Math.max(1, (valueCount + MAX_ARRAY_PARAMETER_LENGTH - 1) / MAX_ARRAY_PARAMETER_LENGTH);

        String condition = String.join(" OR ", Collections.nCopies(chunkCount, column + " = ANY(?)"));

        return chunkCount == 1 ? condition : "(" + condition + ")";
    }

    private static <T> List<List<T>> partition(List<T> values) {
        List<List<T>> chunks = new ArrayList<>();

        for (int start = 0; start < values.size(); start += MAX_ARRAY_PARAMETER_LENGTH) {
            chunks.add(values.subList(start, Math.min(start + MAX_ARRAY_PARAMETER_LENGTH, values.size())));
        }

        return chunks;
    }

    private static String getArrayElementTypeName(ColumnType type) {
        return switch (type) {
            case STRING -> "varchar";
            case NUMBER -> "numeric";
            case INTEGER -> "bigint";
            case DATE -> "date";
            case DATE_TIME -> "timestamp";
            case BOOLEAN -> "boolean";
        };
    }

    private static Object toArrayElement(ColumnType type, Object value) {
        return switch (type) {
            case STRING -> String.valueOf(value);
            case NUMBER -> toBigDecimal((Number) value);
            case INTEGER -> ((Number) value).longValue();
            case DATE -> value instanceof java.sql.Date date ? date : new java.sql.Date(((Date) value).getTime());
            case DATE_TIME -> switch (value) {
                case LocalDateTime localDateTime -> localDateTime.toString();
                case Timestamp timestamp -> timestamp;
                default -> new Timestamp(((Date) value).getTime());
            };
            case BOOLEAN -> value;
        };
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

        if (operation.textOnly() && columnType != ColumnType.STRING) {
            throw new IllegalArgumentException(
                "Filter operation %s is only supported on text columns, but column '%s' is %s".formatted(
                    operation, columnSpec.name(), columnType));
        }

        return switch (operation) {
            case EQUALS -> addComparison(column, "=", columnSpec, filter, filterParameters);
            case EQUALS_IGNORE_CASE -> {
                filterParameters.add(
                    new FilterParameter(ColumnType.STRING, coerceFilterValue(columnSpec, filter.singleValue())));

                yield "LOWER(" + column + ") = LOWER(?)";
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
                List<Object> values = filter.values();

                List<Object> coercedValues = values.stream()
                    .map(value -> coerceFilterValue(columnSpec, value))
                    .toList();

                for (List<Object> chunk : partition(coercedValues)) {
                    filterParameters.add(new FilterParameter(columnType, chunk.toArray()));
                }

                yield buildAnyCondition(column, coercedValues.size());
            }
            case BETWEEN -> {
                List<Object> values = filter.values();

                Object lowerValue = coerceFilterValue(columnSpec, values.getFirst());
                Object upperValue = coerceFilterValue(columnSpec, values.getLast());

                if (columnType != ColumnType.STRING && isGreater(lowerValue, upperValue)) {
                    throw new IllegalArgumentException(
                        "Filter on column '%s' with operation BETWEEN has Value %s after To Value %s".formatted(
                            columnSpec.name(), values.getFirst(), values.getLast()));
                }

                filterParameters.add(new FilterParameter(columnType, lowerValue));
                filterParameters.add(new FilterParameter(columnType, upperValue));

                yield column + " BETWEEN ? AND ?";
            }
            case EMPTY -> columnType == ColumnType.STRING
                ? "(" + column + " IS NULL OR " + column + " = '')" : column + " IS NULL";
            case NOT_EMPTY -> columnType == ColumnType.STRING
                ? "(" + column + " IS NOT NULL AND " + column + " <> '')" : column + " IS NOT NULL";
        };
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private static boolean isGreater(Object value, Object otherValue) {
        Class<?> valueClass = value.getClass();

        return value instanceof Comparable comparable && valueClass.equals(otherValue.getClass())
            && comparable.compareTo(otherValue) > 0;
    }

    private String addComparison(
        String column, String comparisonOperator, ColumnSpec columnSpec, DataTableRowFilter filter,
        List<FilterParameter> filterParameters) {

        filterParameters.add(
            new FilterParameter(columnSpec.type(), coerceFilterValue(columnSpec, filter.singleValue())));

        return column + " " + comparisonOperator + " ?";
    }

    private String addLike(
        String column, String prefix, String suffix, boolean negated, ColumnSpec columnSpec,
        DataTableRowFilter filter, List<FilterParameter> filterParameters) {

        Object rawValue = filter.singleValue();

        String value = rawValue instanceof String string ? string : (String) coerceFilterValue(columnSpec, rawValue);

        filterParameters.add(new FilterParameter(ColumnType.STRING, prefix + escapeLikePattern(value) + suffix));

        if (negated) {
            return "(" + column + " IS NULL OR " + column + " NOT LIKE ? ESCAPE '\\')";
        }

        return column + " LIKE ? ESCAPE '\\'";
    }

    private Object coerceFilterValue(ColumnSpec columnSpec, @Nullable Object rawValue) {
        return Objects.requireNonNull(
            coerceValueStrictly(columnSpec, rawValue, false), "filter values are checked by DataTableRowFilter");
    }

    private @Nullable Object coerceValueStrictly(
        ColumnSpec columnSpec, @Nullable Object rawValue, boolean epochMillisecondsAllowed) {

        if (rawValue == null) {
            return null;
        }

        ColumnType columnType = columnSpec.type();

        if (rawValue instanceof String string && string.isBlank()) {
            return columnType == ColumnType.STRING ? "" : null;
        }

        if (rawValue instanceof Collection<?> || rawValue instanceof Map<?, ?> || isArray(rawValue)) {
            throw new IllegalArgumentException(
                "Value for column '%s' must be a single value, not a list or an object".formatted(columnSpec.name()));
        }

        boolean dateColumn = columnType == ColumnType.DATE || columnType == ColumnType.DATE_TIME;

        if (dateColumn && isEpochLike(rawValue)) {
            Object dateValue = epochMillisecondsAllowed ? fromEpochMilliseconds(columnType, rawValue) : null;

            if (dateValue != null) {
                return dateValue;
            }

            throw new IllegalArgumentException(
                "Value '%s' is not a valid %s for column '%s'; use an ISO format such as %s".formatted(
                    rawValue, columnType == ColumnType.DATE ? "date" : "date_time", columnSpec.name(),
                    columnType == ColumnType.DATE ? "2026-01-31" : "2026-01-31T08:00:00"));
        }

        Object coercedValue = switch (columnType) {
            case BOOLEAN -> toBoolean(rawValue);
            case INTEGER -> toLong(rawValue);
            case NUMBER -> toNumber(rawValue);
            case DATE -> toDate(rawValue);
            case DATE_TIME -> toTimestamp(rawValue);
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

    private static boolean isArray(Object value) {
        Class<?> valueClass = value.getClass();

        return valueClass.isArray();
    }

    private @Nullable Object toNumber(Object rawValue) {
        try {
            return coerceValue(ColumnType.NUMBER, rawValue);
        } catch (NumberFormatException numberFormatException) {
            return null;
        }
    }

    private static @Nullable Object fromEpochMilliseconds(ColumnType columnType, Object rawValue) {
        if (rawValue instanceof Boolean) {
            return null;
        }

        Long epochMilliseconds = toLong(rawValue);

        if (epochMilliseconds == null || Math.abs(epochMilliseconds) < MIN_EPOCH_MILLISECONDS) {
            return null;
        }

        LocalDateTime dateTime;

        try {
            dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilliseconds), ZoneOffset.UTC);
        } catch (DateTimeException dateTimeException) {
            return null;
        }

        return columnType == ColumnType.DATE ? java.sql.Date.valueOf(dateTime.toLocalDate()) : dateTime;
    }

    private static Instant toInstant(Date date) {
        if (date instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }

        return Instant.ofEpochMilli(date.getTime());
    }

    private @Nullable Object toDate(Object rawValue) {
        if (rawValue instanceof java.sql.Date sqlDate) {
            return sqlDate;
        }

        if (rawValue instanceof Date date) {
            return java.sql.Date.valueOf(LocalDate.ofInstant(toInstant(date), ZoneOffset.UTC));
        }

        String value = String.valueOf(rawValue)
            .trim();

        for (DateTimeFormatter dateTimeFormatter : STRICT_DATE_FORMATTERS) {
            LocalDate localDate = parseOrNull(value, dateTimeFormatter, LocalDate::from);

            if (localDate != null) {
                return java.sql.Date.valueOf(localDate);
            }
        }

        TemporalAccessor dateTime = parseDateTimeStrictly(value);

        if (dateTime == null) {
            return null;
        }

        if (dateTime.query(TemporalQueries.zone()) != null) {
            return java.sql.Date.valueOf(LocalDate.ofInstant(Instant.from(dateTime), ZoneOffset.UTC));
        }

        return java.sql.Date.valueOf(LocalDate.from(dateTime));
    }

    private @Nullable Object toTimestamp(Object rawValue) {
        if (rawValue instanceof Date date) {
            return LocalDateTime.ofInstant(toInstant(date), ZoneOffset.UTC);
        }

        TemporalAccessor dateTime = parseDateTimeStrictly(
            String.valueOf(rawValue)
                .trim());

        if (dateTime == null) {
            return null;
        }

        if (dateTime.query(TemporalQueries.zone()) != null) {
            return LocalDateTime.ofInstant(Instant.from(dateTime), ZoneOffset.UTC);
        }

        return LocalDateTime.from(dateTime);
    }

    private static @Nullable TemporalAccessor parseDateTimeStrictly(String value) {
        for (DateTimeFormatter dateTimeFormatter : STRICT_DATE_TIME_FORMATTERS) {
            TemporalAccessor dateTime = parseOrNull(value, dateTimeFormatter, temporalAccessor -> temporalAccessor);

            if (dateTime != null) {
                return dateTime;
            }
        }

        return null;
    }

    private static <T> @Nullable T parseOrNull(String value, DateTimeFormatter formatter, TemporalQuery<T> query) {
        try {
            return formatter.parse(value, query);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static boolean isEpochLike(Object rawValue) {
        if (rawValue instanceof Number || rawValue instanceof Boolean) {
            return true;
        }

        if (rawValue instanceof CharSequence charSequence) {
            String value = String.valueOf(charSequence);

            return EPOCH_PATTERN.matcher(value.trim())
                .matches();
        }

        return false;
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

    private static Optional<ColumnSpec> findColumnSpec(List<ColumnSpec> columnSpecs, String columnName) {
        return columnSpecs.stream()
            .filter(columnSpec -> {
                String name = columnSpec.name();

                return name.equalsIgnoreCase(columnName);
            })
            .findFirst();
    }

    private static ColumnSpec getColumnSpec(List<ColumnSpec> columnSpecs, String columnName) {
        return findColumnSpec(columnSpecs, columnName)
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown column: %s; available columns: %s".formatted(
                    columnName, String.join(", ", getColumnNames(columnSpecs)))));
    }

    private void checkHasId(String baseName, String physicalName) {
        String sql =
            "SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ? AND " +
                "column_name = 'id'";

        List<Integer> exists = jdbcTemplate.query(
            sql, preparedStatement -> preparedStatement.setString(1, physicalName), (resultSet, rowNum) -> 1);

        if (!exists.isEmpty()) {
            return;
        }

        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        if (columnSpecs.isEmpty()) {
            throw new IllegalArgumentException("Table '%s' not found".formatted(baseName));
        }

        throw new IllegalStateException("Table does not have primary key column 'id': " + physicalName);
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

    private static DataTableRow toDataTableRow(
        ResultSet resultSet, List<String> columnNames, boolean hasExternalIdColumn) throws SQLException {

        Map<String, Object> values = new HashMap<>();

        for (String columnName : columnNames) {
            Object value = resultSet.getObject(columnName);

            if (value instanceof Timestamp) {
                value = resultSet.getTimestamp(columnName, Calendar.getInstance(UTC_TIME_ZONE));
            }

            values.put(columnName, value);
        }

        return new DataTableRow(
            resultSet.getLong("id"), hasExternalIdColumn ? resultSet.getString(ReservedColumns.EXTERNAL_ID) : null,
            values);
    }

    private List<ColumnSpec> listColumns(String physicalName) {
        String sql = "SELECT column_name, data_type FROM information_schema.columns " +
            "WHERE table_schema = current_schema() AND table_name = ? ORDER BY ordinal_position";

        return jdbcTemplate.query(sql, preparedStatement -> preparedStatement.setString(1, physicalName),
            (resultSet, rowNum) -> {
                String name = resultSet.getString("column_name");
                String dataType = resultSet.getString("data_type");

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
                    (value instanceof BigDecimal bigDecimal) ? bigDecimal : toBigDecimal((Number) value));
            case INTEGER -> {
                switch (value) {
                    case Integer integer -> preparedStatement.setInt(index, integer);
                    case Long longValue -> preparedStatement.setLong(index, longValue);
                    case Number number -> preparedStatement.setLong(index, number.longValue());
                    default -> preparedStatement.setObject(index, value, Types.BIGINT);
                }
            }
            case DATE -> {
                if (value instanceof java.sql.Date sqlDate) {
                    preparedStatement.setDate(index, sqlDate);
                } else if (value instanceof Date date) {
                    preparedStatement.setDate(index, new java.sql.Date(date.getTime()));
                } else {
                    preparedStatement.setDate(index, DateUtils.parseSqlDate(String.valueOf(value)));
                }
            }
            case DATE_TIME -> {
                if (value instanceof LocalDateTime localDateTime) {
                    preparedStatement.setObject(index, localDateTime);
                } else if (value instanceof Timestamp timestamp) {
                    preparedStatement.setTimestamp(index, timestamp);
                } else if (value instanceof Date date) {
                    preparedStatement.setTimestamp(index, new Timestamp(date.getTime()));
                } else {
                    preparedStatement.setTimestamp(index, DateUtils.parseSqlTimestamp(String.valueOf(value)));
                }
            }
            case BOOLEAN -> {
                if (value instanceof Boolean booleanValue) {
                    preparedStatement.setBoolean(index, booleanValue);
                } else {
                    preparedStatement.setBoolean(index, Boolean.parseBoolean(String.valueOf(value)));
                }
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
        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        List<String> columnNames = getNonIdColumnNames(columnSpecs);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        String sql = "DELETE FROM " + escapeIdentifier(physicalName) + " WHERE \"id\" = ? RETURNING " +
            buildSelectColumns(columnNames, hasExternalIdColumn);

        List<DataTableRow> deletedDataTableRows = jdbcTemplate.query(
            sql, preparedStatement -> preparedStatement.setLong(1, id),
            (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames, hasExternalIdColumn));

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
        if (returningSupported != null) {
            return returningSupported;
        }

        Optional<Boolean> resolvedReturningSupported = resolveReturningSupported();

        if (resolvedReturningSupported.isEmpty()) {
            return false;
        }

        returningSupported = resolvedReturningSupported.get();

        return returningSupported;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRow insertRowReturningValues(
        String physicalName, String valuesClause, List<String> allColumnNames,
        PreparedStatementSetter preparedStatementSetter) {

        List<String> columnNames = allColumnNames.stream()
            .filter(columnName -> !ReservedColumns.isReserved(columnName))
            .toList();
        boolean hasExternalIdColumn = allColumnNames.stream()
            .anyMatch(ReservedColumns.EXTERNAL_ID::equalsIgnoreCase);

        String sql = "INSERT INTO " + escapeIdentifier(physicalName) + valuesClause + " RETURNING " +
            buildSelectColumns(columnNames, hasExternalIdColumn);

        List<DataTableRow> insertedDataTableRows = jdbcTemplate.query(
            sql, preparedStatementSetter,
            (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames, hasExternalIdColumn));

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

    private Optional<Boolean> resolveReturningSupported() {
        try {
            return Optional.ofNullable(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
                DatabaseMetaData databaseMetaData = connection.getMetaData();

                String databaseProductName = databaseMetaData.getDatabaseProductName();

                databaseProductName = databaseProductName.toLowerCase(Locale.ROOT);

                return !databaseProductName.contains("h2");
            }));
        } catch (DataAccessException dataAccessException) {
            log.warn(
                "Unable to determine the database product name, writing without RETURNING", dataAccessException);

            return Optional.empty();
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
            .filter(columnName -> !ReservedColumns.isReserved(columnName))
            .toList();
        boolean hasExternalIdColumn = allColumnNames.stream()
            .anyMatch(ReservedColumns.EXTERNAL_ID::equalsIgnoreCase);

        String sql = "UPDATE " + escapeIdentifier(physicalName) + " SET " + setClause +
            " WHERE \"id\" = ? RETURNING " + buildSelectColumns(columnNames, hasExternalIdColumn);

        List<DataTableRow> updatedDataTableRows = jdbcTemplate.query(
            sql, preparedStatementSetter,
            (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames, hasExternalIdColumn));

        return updatedDataTableRows.isEmpty() ? null : updatedDataTableRows.getFirst();
    }

    private record ColumnValue(ColumnSpec columnSpec, @Nullable Object value) {
    }

    private record FilterParameter(ColumnType columnType, Object value) {
    }

    private enum ValuePolicy {

        SINGLE_ROW,
        BULK_UPDATE
    }

    private record WhereClause(String sql, List<FilterParameter> filterParameters) {
    }
}
