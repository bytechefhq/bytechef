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
import com.bytechef.platform.data.table.domain.ColumnValues;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.platform.data.table.internal.DataTableDialect;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.writer.CsvWriter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Reaches only the physical table its {@link DataTableRef} names. Nothing here builds a physical name, so nothing here
 * chooses an owner -- see {@link DataTableRowService} for why that separation is what keeps one account's rows out of
 * another's.
 *
 * <p>
 * Every statement here addresses its table through the ref it is given. It chooses the rows, through the predicates
 * {@link RowQuerySqlBuilder} builds, and it is not a parameter of any operation -- which is the whole design: an owner
 * that cannot be passed cannot be passed wrongly, and there is exactly one place, resolution, where it is decided.
 *
 *
 * @author Ivica Cardic
 */
@Service
public class DataTableRowServiceImpl implements DataTableRowService {

    private static final Logger log = LoggerFactory.getLogger(DataTableRowServiceImpl.class);

    private static final int ESTIMATED_ROW_OVERHEAD_BYTES = 64;
    private static final int MAX_EXTERNAL_ID_LENGTH = 255;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final DataTableStorageService dataTableStorageService;
    private final JdbcTemplate jdbcTemplate;

    private final DataTableDialect dataTableDialect = new DataTableDialect();

    @SuppressFBWarnings("EI")
    public DataTableRowServiceImpl(
        ApplicationEventPublisher applicationEventPublisher, JdbcTemplate jdbcTemplate,
        DataTableStorageService dataTableStorageService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.jdbcTemplate = jdbcTemplate;
        this.dataTableStorageService = dataTableStorageService;
    }

    /**
     * Deletes a row from a data table by ID.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)}, which enforces a strict allowlist pattern {@code [a-z_][a-z0-9_]*},
     * and the physical name comes from a {@link DataTableRef} built from numeric ids, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public boolean deleteRow(DataTableRef dataTableRef, long id) {
        DataTableRow deletedDataTableRow;

        if (isReturningSupported()) {
            deletedDataTableRow = deleteRowReturningValues(dataTableRef, id);
        } else {
            deletedDataTableRow = deleteRowAfterReadingValues(dataTableRef, id);
        }

        if (deletedDataTableRow == null) {
            return false;
        }

        publishDeleted(dataTableRef, deletedDataTableRow);

        return true;
    }

    private void publishDeleted(DataTableRef dataTableRef, DataTableRow deletedDataTableRow) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("id", deletedDataTableRow.id());
        payload.put("values", deletedDataTableRow.values());

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(dataTableRef, DataTableWebhookType.RECORD_DELETED, payload));
    }

    /**
     * Gets a single row from a data table by ID.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)}, which enforces a strict allowlist pattern {@code [a-z_][a-z0-9_]*},
     * and the physical name comes from a {@link DataTableRef} built from numeric ids, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public DataTableRow getRow(DataTableRef dataTableRef, long id) {
        String physicalName = dataTableRef.physicalName();

        List<ColumnSpec> columnSpecs = requireColumns(physicalName);
        List<String> columnNames = userColumnNames(columnSpecs);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        String sql = "SELECT " + selectColumns(columnNames, hasExternalIdColumn) + " FROM " +
            escapeIdentifier(physicalName) + " WHERE \"id\" = ?";

        List<DataTableRow> rows = jdbcTemplate.query(sql, ps -> {
            ps.setLong(1, id);

        }, (resultSet, rowNum) -> toRow(resultSet, columnNames, hasExternalIdColumn));

        return rows.isEmpty() ? null : rows.getFirst();
    }

    /**
     * Gets a single row from a data table by its caller-supplied external id.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)}, which enforces a strict allowlist pattern {@code [a-z_][a-z0-9_]*},
     * and the physical name comes from a {@link DataTableRef} built from numeric ids, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public Optional<DataTableRow> fetchRowByExternalId(DataTableRef dataTableRef, String externalId) {
        String physicalName = dataTableRef.physicalName();

        List<ColumnSpec> columnSpecs = requireColumns(physicalName);
        List<String> columnNames = userColumnNames(columnSpecs);

        String sql = "SELECT " + selectColumns(columnNames, true) + " FROM " + escapeIdentifier(physicalName) +
            " WHERE " + escapeIdentifier(ReservedColumns.EXTERNAL_ID) + " = ?";

        List<DataTableRow> rows = jdbcTemplate.query(sql, ps -> {
            ps.setString(1, externalId);

        }, (resultSet, rowNum) -> toRow(resultSet, columnNames, true));

        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    @Override
    public String exportCsv(DataTableRef dataTableRef) {
        String physicalName = dataTableRef.physicalName();

        List<ColumnSpec> columnSpecs = requireColumns(physicalName);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);
        List<String> columnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .filter(columnName -> !ReservedColumns.isReserved(columnName))
            .toList();

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

        List<DataTableRow> dataTableRows = listRows(dataTableRef, Integer.MAX_VALUE, 0);

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
    public int importCsv(DataTableRef dataTableRef, String csv) {
        dataTableStorageService.checkWithinLimit(
            csv == null ? 0 : csv.getBytes(StandardCharsets.UTF_8).length);

        String physicalName = dataTableRef.physicalName();

        if (csv == null) {
            requireColumns(physicalName);

            return 0;
        }

        List<ColumnSpec> columnSpecs = requireColumns(physicalName);

        List<String> columnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .toList();
        Map<String, ColumnType> columnTypes = columnTypeMap(columnSpecs);

        List<NewRow> newRows = new ArrayList<>();
        int rowNumber = 0;

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

                    // Build mapping from file index -> actual column name (skip unknown and id, key on external_id)
                    mappedColumnNames = new ArrayList<>(headers.size());

                    for (String header : headers) {
                        if (ReservedColumns.EXTERNAL_ID.equalsIgnoreCase(header)) {
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

                        if (columnName == null) {
                            throw new DataTableException(
                                "CSV header '" + header + "' is not a column of this table",
                                DataTableErrorType.CSV_INVALID);
                        }

                        mappedColumnNames.add(columnName);
                    }

                    continue;
                }

                if (fieldStream.allMatch(field -> field == null || field.isBlank())) {
                    continue;
                }

                rowNumber++;

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
                        externalId = (field == null || field.isBlank()) ? null : field;

                        if (externalId != null && externalId.length() > MAX_EXTERNAL_ID_LENGTH) {
                            throw new DataTableException(
                                "CSV external_id must be at most " + MAX_EXTERNAL_ID_LENGTH + " characters",
                                DataTableErrorType.CSV_INVALID);
                        }

                        continue;
                    }

                    if (field == null || field.isEmpty()) {
                        values.put(curColumnName, null);

                        continue;
                    }

                    ColumnType columnType = columnTypes.get(curColumnName.toLowerCase(Locale.ROOT));

                    if (columnType != null && !ColumnValues.isCoercible(columnType, field)) {
                        throw new DataTableException(
                            "CSV row " + rowNumber + ", column '" + curColumnName + "': '" + field +
                                "' is not a valid " + columnType,
                            DataTableErrorType.CSV_INVALID);
                    }

                    values.put(curColumnName, field);
                }

                newRows.add(new NewRow(values, externalId));
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to import CSV", exception);
        }

        long estimatedRowsBytes = newRows.stream()
            .mapToLong(newRow -> estimateRowBytes(newRow.values(), newRow.externalId()))
            .sum();

        dataTableStorageService.checkWithinLimit(estimatedRowsBytes);

        for (NewRow newRow : newRows) {
            insertRow(dataTableRef, newRow.values(), newRow.externalId(), columnSpecs);
        }

        return newRows.size();
    }

    /**
     * Inserts a new row into a data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)}, which enforces a strict allowlist pattern {@code [a-z_][a-z0-9_]*},
     * and the physical name comes from a {@link DataTableRef} built from numeric ids, preventing SQL injection.
     * User-provided row values use parameterized queries.
     */
    @Override
    public DataTableRow insertRow(DataTableRef dataTableRef, Map<String, Object> values) {
        return insertRow(dataTableRef, values, null);
    }

    @Override
    public DataTableRow insertRow(DataTableRef dataTableRef, Map<String, Object> values, @Nullable String externalId) {
        dataTableStorageService.checkWithinLimit(estimateRowBytes(values, externalId));

        return insertRow(dataTableRef, values, externalId, requireColumns(dataTableRef.physicalName()));
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRow insertRow(
        DataTableRef dataTableRef, Map<String, Object> values, @Nullable String externalId,
        List<ColumnSpec> columnSpecs) {

        String physicalName = dataTableRef.physicalName();

        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);
        List<String> allColumnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .toList();

        List<String> insertableColumnNames = resolveWritableColumnNames(values, allColumnNames);

        List<String> insertColumnNames = new ArrayList<>(insertableColumnNames);

        if (externalId != null) {
            insertColumnNames.add(ReservedColumns.EXTERNAL_ID);
        }

        String columnsClause = insertColumnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", "));
        String placeholders = insertColumnNames.stream()
            .map(columnName -> "?")
            .collect(Collectors.joining(", "));

        List<String> returningColumnNames = new ArrayList<>();

        returningColumnNames.add("id");

        if (hasExternalIdColumn) {
            returningColumnNames.add(ReservedColumns.EXTERNAL_ID);
        }

        returningColumnNames.addAll(allColumnNames.stream()
            .filter(columnName -> !ReservedColumns.isReserved(columnName))
            .toList());

        String returningClause = returningColumnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", "));

        String valuesClause = insertColumnNames.isEmpty()
            ? " DEFAULT VALUES" : (" (" + columnsClause + ") VALUES (" + placeholders + ")");

        String sql =
            "INSERT INTO " + escapeIdentifier(physicalName) + valuesClause + " RETURNING " + returningClause;

        Map<String, ColumnType> typeMap = columnTypeMap(columnSpecs);

        DataTableRow result;

        PreparedStatementSetter preparedStatementSetter = ps -> {
            int i = 1;

            for (String columnName : insertableColumnNames) {
                ColumnType columnType = typeMap.getOrDefault(
                    columnName.toLowerCase(Locale.ROOT), ColumnType.STRING);
                Object rawValue = getValueCaseInsensitive(values, columnName);

                Object coercedValue = coerceValue(columnType, rawValue);

                setParam(ps, i++, columnType, coercedValue);
            }

            if (externalId != null) {
                ps.setString(i++, externalId);
            }
        };

        try {
            if (isReturningSupported()) {
                result = jdbcTemplate.query(sql, preparedStatementSetter, resultSet -> {
                    if (resultSet.next()) {
                        long id = resultSet.getLong("id");
                        Map<String, Object> map = new HashMap<>();

                        for (String columnName : returningColumnNames) {
                            if (!ReservedColumns.isReserved(columnName)) {
                                map.put(columnName, resultSet.getObject(columnName));
                            }
                        }

                        return new DataTableRow(id, readExternalId(resultSet, hasExternalIdColumn), map);
                    }
                    throw new IllegalStateException("Failed to insert row");
                });
            } else {
                result = insertRowAfterInserting(dataTableRef, valuesClause, preparedStatementSetter);
            }
        } catch (DuplicateKeyException duplicateKeyException) {
            throw new DataTableException(
                "A row with external id '" + externalId + "' already exists", duplicateKeyException,
                DataTableErrorType.ROW_EXTERNAL_ID_CONFLICT);
        }

        if (result == null) {
            throw new IllegalStateException("Failed to insert row");
        }

        // Dispatch event for webhooks
        Map<String, Object> payload = new HashMap<>();

        payload.put("id", result.id());
        payload.put("values", result.values());

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(dataTableRef, DataTableWebhookType.RECORD_CREATED, payload));

        return result;
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<DataTableRow> listRows(DataTableRef dataTableRef, int limit, int offset) {
        return listRows(dataTableRef, limit, offset, List.of(), List.of());
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<DataTableRow> listRows(
        DataTableRef dataTableRef, int limit, int offset, List<RowFilter> rowFilters, List<RowSort> rowSorts) {

        String physicalName = dataTableRef.physicalName();

        List<ColumnSpec> columnSpecs = requireColumns(physicalName);
        List<String> columnNames = userColumnNames(columnSpecs);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        Map<String, ColumnType> columnTypes = columnTypeMap(columnSpecs);

        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(rowFilters, columnTypes);

        String sql =
            "SELECT " + selectColumns(columnNames, hasExternalIdColumn) + " FROM " + escapeIdentifier(physicalName) +
                " WHERE TRUE" + fragment.sql() +
                RowQuerySqlBuilder.orderBy(rowSorts, columnTypes) + " LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql, ps -> {
            int index = 1;

            for (RowQuerySqlBuilder.Binding binding : fragment.bindings()) {
                setParam(ps, index++, binding.type(), binding.value());
            }

            ps.setInt(index++, Math.max(0, limit));
            ps.setInt(index, Math.max(0, offset));
        }, (resultSet, rowNum) -> toRow(resultSet, columnNames, hasExternalIdColumn));
    }

    @Override
    public DataTableRow updateRow(DataTableRef dataTableRef, long id, Map<String, Object> values) {
        return updateRow(dataTableRef, id, values, null);
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public DataTableRow updateRow(
        DataTableRef dataTableRef, long id, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch) {

        checkUpdateWithinLimit(values, externalIdPatch, () -> getRow(dataTableRef, id));

        String physicalName = dataTableRef.physicalName();

        List<ColumnSpec> columnSpecs = requireColumns(physicalName);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);
        List<String> allColumnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .toList();

        List<String> updatableColumnNames = resolveWritableColumnNames(values, allColumnNames);

        List<String> setClauses = new ArrayList<>(updatableColumnNames.stream()
            .map(columnName -> escapeIdentifier(columnName) + " = ?")
            .toList());

        if (externalIdPatch != null) {
            setClauses.add(escapeIdentifier(ReservedColumns.EXTERNAL_ID) + " = ?");
        }

        if (setClauses.isEmpty()) {
            DataTableRow current = getRow(dataTableRef, id);

            if (current == null) {
                throw new DataTableException("Row not found: id=" + id, DataTableErrorType.ROW_NOT_FOUND);
            }

            return current;
        }

        String setClause = String.join(", ", setClauses);

        List<String> returningColumnNames = new ArrayList<>();

        returningColumnNames.add("id");

        if (hasExternalIdColumn) {
            returningColumnNames.add(ReservedColumns.EXTERNAL_ID);
        }

        returningColumnNames.addAll(allColumnNames.stream()
            .filter(columnName -> !ReservedColumns.isReserved(columnName))
            .toList());

        String returningClause = returningColumnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", "));

        String sql =
            "UPDATE " + escapeIdentifier(physicalName) + " SET " + setClause + " WHERE \"id\" = ?" + " RETURNING "
                + returningClause;

        Map<String, ColumnType> columnTypeMap = columnTypeMap(columnSpecs);

        DataTableRow updatedDataTableRow;

        PreparedStatementSetter preparedStatementSetter = ps -> {
            int i = 1;

            for (String columnName : updatableColumnNames) {
                ColumnType columnType = columnTypeMap.getOrDefault(
                    columnName.toLowerCase(Locale.ROOT), ColumnType.STRING);

                Object rawValue = getValueCaseInsensitive(values, columnName);

                Object coercedValue = coerceValue(columnType, rawValue);

                setParam(ps, i++, columnType, coercedValue);
            }

            if (externalIdPatch != null) {
                ps.setString(i++, externalIdPatch.externalId());
            }

            ps.setLong(i++, id);

        };

        try {
            if (isReturningSupported()) {
                updatedDataTableRow = jdbcTemplate.query(sql, preparedStatementSetter, resultSet -> {
                    if (resultSet.next()) {
                        long curId = resultSet.getLong("id");
                        Map<String, Object> map = new HashMap<>();

                        for (String columnName : returningColumnNames) {
                            if (!ReservedColumns.isReserved(columnName)) {
                                map.put(columnName, resultSet.getObject(columnName));
                            }
                        }

                        return new DataTableRow(curId, readExternalId(resultSet, hasExternalIdColumn), map);
                    }

                    throw new DataTableException("Row not found: id=" + id, DataTableErrorType.ROW_NOT_FOUND);
                });
            } else {
                updatedDataTableRow = updateRowAfterUpdating(dataTableRef, setClause, id, preparedStatementSetter);
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

        Map<String, Object> payload = new HashMap<>();

        payload.put("id", updatedDataTableRow.id());
        payload.put("values", updatedDataTableRow.values());

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(dataTableRef, DataTableWebhookType.RECORD_UPDATED, payload));

        return updatedDataTableRow;
    }

    /**
     * Inserts the row keyed by {@code externalId} or merges {@code values} into the row that already carries it, in one
     * statement rather than a read followed by a write.
     *
     * <p>
     * <b>Security Note:</b> the SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)}, which enforces a strict allowlist pattern {@code [a-z_][a-z0-9_]*},
     * and user-provided row values use parameterized queries.
     */
    @Override
    public UpsertResult upsertRow(DataTableRef dataTableRef, String externalId, Map<String, Object> values) {
        checkUpsertWithinLimit(dataTableRef, List.of(new NewRow(values, externalId)));

        return upsertRow(dataTableRef, externalId, values, requireColumns(dataTableRef.physicalName()));
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private UpsertResult upsertRow(
        DataTableRef dataTableRef, String externalId, Map<String, Object> values, List<ColumnSpec> columnSpecs) {

        Assert.hasText(externalId, "externalId must not be empty");

        String physicalName = dataTableRef.physicalName();

        Assert.isTrue(hasExternalIdColumn(columnSpecs), "Table " + physicalName + " has no external_id column");

        List<String> allColumnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .toList();
        List<String> userColumnNames = userColumnNames(columnSpecs);
        List<String> writtenColumnNames = resolveWritableColumnNames(values, allColumnNames);

        List<String> insertColumnNames = new ArrayList<>();

        insertColumnNames.add(ReservedColumns.EXTERNAL_ID);
        insertColumnNames.addAll(writtenColumnNames);

        String columnsClause = insertColumnNames.stream()
            .map(this::escapeIdentifier)
            .collect(Collectors.joining(", "));
        String placeholders = insertColumnNames.stream()
            .map(columnName -> "?")
            .collect(Collectors.joining(", "));

        String updateClause = writtenColumnNames.isEmpty()
            ? escapeIdentifier(ReservedColumns.EXTERNAL_ID) + " = EXCLUDED."
                + escapeIdentifier(ReservedColumns.EXTERNAL_ID)
            : writtenColumnNames.stream()
                .map(columnName -> escapeIdentifier(columnName) + " = EXCLUDED." + escapeIdentifier(columnName))
                .collect(Collectors.joining(", "));

        String sql = "INSERT INTO " + escapeIdentifier(physicalName) + " (" + columnsClause + ") VALUES (" +
            placeholders + ") ON CONFLICT (" +
            escapeIdentifier(ReservedColumns.EXTERNAL_ID) + ") WHERE " + escapeIdentifier(ReservedColumns.EXTERNAL_ID) +
            " IS NOT NULL DO UPDATE SET " + updateClause + " RETURNING " + selectColumns(userColumnNames, true) +
            ", (xmax = 0) AS \"created\"";

        Map<String, ColumnType> typeMap = columnTypeMap(columnSpecs);

        UpsertResult upsertResult;

        if (isReturningSupported()) {
            upsertResult = jdbcTemplate.query(sql, ps -> {
                int i = 1;

                ps.setString(i++, externalId);

                for (String columnName : writtenColumnNames) {
                    ColumnType columnType = typeMap.getOrDefault(
                        columnName.toLowerCase(Locale.ROOT), ColumnType.STRING);

                    setParam(
                        ps, i++, columnType, coerceValue(columnType, getValueCaseInsensitive(values, columnName)));
                }
            }, resultSet -> {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Upsert returned no row");
                }

                return new UpsertResult(toRow(resultSet, userColumnNames, true), resultSet.getBoolean("created"));
            });
        } else {
            upsertResult = upsertRowWithoutReturning(
                dataTableRef, externalId, values, insertColumnNames, writtenColumnNames, typeMap);
        }

        Map<String, Object> payload = new HashMap<>();

        payload.put("id", upsertResult.row()
            .id());
        payload.put("values", upsertResult.row()
            .values());

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(
                dataTableRef,
                upsertResult.created() ? DataTableWebhookType.RECORD_CREATED : DataTableWebhookType.RECORD_UPDATED,
                payload));

        return upsertResult;
    }

    /**
     * Counts the rows a run may read that also satisfy {@code rowFilters}. Applies the same filter fragment, over the
     * same filter fragment as the filtered {@link #listRows} -- so a page's {@code totalElements} always agrees with
     * its content.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)}, which enforces a strict allowlist pattern {@code [a-z_][a-z0-9_]*}.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public long countRows(DataTableRef dataTableRef, List<RowFilter> rowFilters) {
        String physicalName = dataTableRef.physicalName();

        List<ColumnSpec> columnSpecs = requireColumns(physicalName);
        Map<String, ColumnType> columnTypes = columnTypeMap(columnSpecs);

        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(rowFilters, columnTypes);

        String sql = "SELECT COUNT(*) FROM " + escapeIdentifier(physicalName) + " WHERE TRUE" + fragment.sql();

        Long count = jdbcTemplate.query(sql, ps -> {
            int index = 1;

            for (RowQuerySqlBuilder.Binding binding : fragment.bindings()) {
                setParam(ps, index++, binding.type(), binding.value());
            }
        }, resultSet -> {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }

            return 0L;
        });

        return count == null ? 0L : count;
    }

    /**
     * Deletes the rows among {@code ids} the ref may write, one {@link DataTableWebhookType#RECORD_DELETED} event per
     * row actually removed.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)}, which enforces a strict allowlist pattern {@code [a-z_][a-z0-9_]*}.
     * Row ids are bound as a parameterized array, never interpolated.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<Long> deleteRows(DataTableRef dataTableRef, List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        String physicalName = dataTableRef.physicalName();

        List<ColumnSpec> columnSpecs = requireColumns(physicalName);
        List<String> columnNames = userColumnNames(columnSpecs);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        List<DataTableRow> deletedDataTableRows;

        if (isReturningSupported()) {
            String sql = "DELETE FROM " + escapeIdentifier(physicalName) + " WHERE \"id\" = ANY(?)" + " RETURNING " +
                selectColumns(columnNames, hasExternalIdColumn);

            deletedDataTableRows = jdbcTemplate.query(sql, ps -> {
                ps.setArray(1, ps.getConnection()
                    .createArrayOf("bigint", ids.toArray()));

            }, (resultSet, rowNum) -> toRow(resultSet, columnNames, hasExternalIdColumn));
        } else {
            deletedDataTableRows = deleteRowsAfterReadingRows(physicalName, ids, columnNames, hasExternalIdColumn);
        }

        for (DataTableRow deletedDataTableRow : deletedDataTableRows) {
            publishDeleted(dataTableRef, deletedDataTableRow);
        }

        return deletedDataTableRows.stream()
            .map(DataTableRow::id)
            .toList();
    }

    /**
     * Deletes every row the ref may write, one {@link DataTableWebhookType#RECORD_DELETED} event per row.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)}, which enforces a strict allowlist pattern {@code [a-z_][a-z0-9_]*}.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public long clearRows(DataTableRef dataTableRef) {
        String physicalName = dataTableRef.physicalName();

        List<ColumnSpec> columnSpecs = requireColumns(physicalName);
        List<String> columnNames = userColumnNames(columnSpecs);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        List<DataTableRow> deletedDataTableRows;

        if (isReturningSupported()) {
            String sql = "DELETE FROM " + escapeIdentifier(physicalName) + " WHERE TRUE" + " RETURNING " +
                selectColumns(columnNames, hasExternalIdColumn);

            deletedDataTableRows = jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> toRow(resultSet, columnNames, hasExternalIdColumn));
        } else {
            List<DataTableRow> existingDataTableRows = jdbcTemplate.query(
                "SELECT " + selectColumns(columnNames, hasExternalIdColumn) + " FROM " + escapeIdentifier(physicalName),
                (resultSet, rowNum) -> toRow(resultSet, columnNames, hasExternalIdColumn));

            deletedDataTableRows = deleteReadRows(physicalName, existingDataTableRows);
        }

        for (DataTableRow deletedDataTableRow : deletedDataTableRows) {
            publishDeleted(dataTableRef, deletedDataTableRow);
        }

        return deletedDataTableRows.size();
    }

    @Override
    @Transactional
    public List<DataTableRow> insertRows(
        DataTableRef dataTableRef, List<NewRow> newRows, CreateStrategy createStrategy) {

        if (createStrategy == CreateStrategy.UPSERT) {
            boolean missingKey = newRows.stream()
                .anyMatch(newRow -> {
                    String externalId = newRow.externalId();

                    return externalId == null || externalId.isBlank();
                });

            if (missingKey) {
                throw new DataTableException(
                    "Every row needs an externalId under the UPSERT strategy",
                    DataTableErrorType.ROW_EXTERNAL_ID_REQUIRED);
            }
        }

        if (newRows.isEmpty()) {
            return List.of();
        }

        if (createStrategy == CreateStrategy.UPSERT) {
            checkUpsertWithinLimit(dataTableRef, newRows);
        } else {
            long estimatedRowsBytes = newRows.stream()
                .mapToLong(newRow -> estimateRowBytes(newRow.values(), newRow.externalId()))
                .sum();

            dataTableStorageService.checkWithinLimit(estimatedRowsBytes);
        }

        List<ColumnSpec> columnSpecs = requireColumns(dataTableRef.physicalName());

        List<DataTableRow> dataTableRows = new ArrayList<>(newRows.size());

        for (NewRow newRow : newRows) {
            if (createStrategy == CreateStrategy.UPSERT) {
                UpsertResult upsertResult = upsertRow(
                    dataTableRef, newRow.externalId(), newRow.values(), columnSpecs);

                dataTableRows.add(upsertResult.row());
            } else {
                dataTableRows.add(insertRow(dataTableRef, newRow.values(), newRow.externalId(), columnSpecs));
            }
        }

        return dataTableRows;
    }

    private List<ColumnSpec> requireColumns(String physicalName) {
        List<ColumnSpec> columnSpecs = listColumns(physicalName);

        if (columnSpecs.isEmpty()) {
            throw new DataTableException(
                "Data table does not exist in this environment", DataTableErrorType.DATA_TABLE_NOT_FOUND);
        }

        boolean hasId = columnSpecs.stream()
            .anyMatch(columnSpec -> ReservedColumns.ID.equalsIgnoreCase(columnSpec.name()));

        if (!hasId) {
            throw new IllegalStateException("Table does not have primary key column 'id': " + physicalName);
        }

        return columnSpecs;
    }

    private void checkUpdateWithinLimit(
        Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        Supplier<@Nullable DataTableRow> currentDataTableRowSupplier) {

        long incomingBytes = estimateValuesBytes(
            values, externalIdPatch == null ? null : externalIdPatch.externalId());

        try {
            dataTableStorageService.checkWithinLimit(incomingBytes);
        } catch (DataTableStorageLimitExceededException dataTableStorageLimitExceededException) {
            DataTableRow currentDataTableRow = currentDataTableRowSupplier.get();

            if (currentDataTableRow == null) {
                throw dataTableStorageLimitExceededException;
            }

            long growthBytes = incomingBytes - estimateValuesBytes(
                currentValues(values, currentDataTableRow),
                externalIdPatch == null ? null : currentDataTableRow.externalId());

            if (growthBytes > 0) {
                dataTableStorageService.checkWithinLimit(growthBytes);
            }
        }
    }

    private void checkUpsertWithinLimit(DataTableRef dataTableRef, List<NewRow> newRows) {
        long incomingBytes = newRows.stream()
            .mapToLong(newRow -> estimateRowBytes(newRow.values(), newRow.externalId()))
            .sum();

        try {
            dataTableStorageService.checkWithinLimit(incomingBytes);
        } catch (DataTableStorageLimitExceededException dataTableStorageLimitExceededException) {
            Map<String, Map<String, Object>> finalValuesByExternalId = new LinkedHashMap<>();

            for (NewRow newRow : newRows) {
                Map<String, Object> finalValues = finalValuesByExternalId.computeIfAbsent(
                    Objects.requireNonNull(newRow.externalId()), externalId -> new HashMap<>());

                finalValues.putAll(newRow.values());
            }

            long growthBytes = 0;

            for (Map.Entry<String, Map<String, Object>> entry : finalValuesByExternalId.entrySet()) {
                String externalId = entry.getKey();
                Map<String, Object> finalValues = entry.getValue();

                Optional<DataTableRow> existingDataTableRowOptional = fetchRowByExternalId(dataTableRef, externalId);

                if (existingDataTableRowOptional.isPresent()) {
                    DataTableRow existingDataTableRow = existingDataTableRowOptional.get();

                    growthBytes += estimateValuesBytes(finalValues, null) -
                        estimateValuesBytes(currentValues(finalValues, existingDataTableRow), null);
                } else {
                    growthBytes += estimateRowBytes(finalValues, externalId);
                }
            }

            if (growthBytes > 0) {
                dataTableStorageService.checkWithinLimit(growthBytes);
            }
        }
    }

    private Map<String, Object> currentValues(Map<String, Object> values, DataTableRow currentDataTableRow) {
        Map<String, Object> currentValues = new HashMap<>();

        for (String columnName : values.keySet()) {
            currentValues.put(columnName, getValueCaseInsensitive(currentDataTableRow.values(), columnName));
        }

        return currentValues;
    }

    private static String toCsvValue(@Nullable Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Timestamp timestamp) {
            LocalDateTime localDateTime = timestamp.toLocalDateTime();

            return localDateTime.toString();
        }

        return String.valueOf(value);
    }

    private static long estimateRowBytes(Map<String, Object> values, @Nullable String externalId) {
        return ESTIMATED_ROW_OVERHEAD_BYTES + estimateValuesBytes(values, externalId);
    }

    private static long estimateValuesBytes(Map<String, Object> values, @Nullable String externalId) {
        long estimatedBytes = 0;

        if (externalId != null) {
            estimatedBytes += externalId.getBytes(StandardCharsets.UTF_8).length;
        }

        for (Object value : values.values()) {
            if (value != null) {
                String stringValue = String.valueOf(value);

                estimatedBytes += stringValue.getBytes(StandardCharsets.UTF_8).length;
            }
        }

        return estimatedBytes;
    }

    private static boolean hasExternalIdColumn(List<ColumnSpec> columnSpecs) {
        return columnSpecs.stream()
            .anyMatch(columnSpec -> ReservedColumns.EXTERNAL_ID.equalsIgnoreCase(columnSpec.name()));
    }

    private static @Nullable String readExternalId(ResultSet resultSet, boolean hasExternalIdColumn)
        throws SQLException {

        return hasExternalIdColumn ? resultSet.getString(ReservedColumns.EXTERNAL_ID) : null;
    }

    private static List<String> userColumnNames(List<ColumnSpec> columnSpecs) {
        return columnSpecs.stream()
            .map(ColumnSpec::name)
            .filter(columnName -> !ReservedColumns.isReserved(columnName))
            .toList();
    }

    private static List<String> resolveWritableColumnNames(Map<String, Object> values, List<String> allColumnNames) {
        return values.keySet()
            .stream()
            .filter(columnName -> allColumnNames.stream()
                .anyMatch(column -> column.equalsIgnoreCase(columnName)))
            .filter(columnName -> !ReservedColumns.isReserved(columnName))
            .map(columnName -> allColumnNames.stream()
                .filter(column -> column.equalsIgnoreCase(columnName))
                .findFirst()
                .orElse(columnName))
            .toList();
    }

    private String selectColumns(List<String> columnNames, boolean hasExternalIdColumn) {
        return "\"id\"" + (hasExternalIdColumn ? ", " + escapeIdentifier(ReservedColumns.EXTERNAL_ID) : "") +
            (columnNames.isEmpty() ? "" : ", " + columnNames.stream()
                .map(this::escapeIdentifier)
                .collect(Collectors.joining(", ")));
    }

    private static DataTableRow toRow(ResultSet resultSet, List<String> columnNames, boolean hasExternalIdColumn)
        throws SQLException {

        Map<String, Object> values = new HashMap<>();

        for (String columnName : columnNames) {
            values.put(columnName, resultSet.getObject(columnName));
        }

        return new DataTableRow(resultSet.getLong("id"), readExternalId(resultSet, hasExternalIdColumn), values);
    }

    static Object coerceValue(ColumnType type, Object rawValue) {
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

    private static Map<String, ColumnType> columnTypeMap(List<ColumnSpec> columnSpecs) {
        Map<String, ColumnType> columnTypeMap = new HashMap<>();

        for (ColumnSpec columnSpec : columnSpecs) {
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
                    ps.setDate(index, DateUtils.parseSqlDate(String.valueOf(value)));
            }
            case DATE_TIME -> {
                if (value instanceof Timestamp t)
                    ps.setTimestamp(index, t);
                else if (value instanceof Date d)
                    ps.setTimestamp(index, new Timestamp(d.getTime()));
                else
                    ps.setTimestamp(
                        index,
                        DateUtils.parseSqlTimestamp(String.valueOf(value)));
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
    private DataTableRow deleteRowReturningValues(DataTableRef dataTableRef, long id) {
        String physicalName = dataTableRef.physicalName();

        List<ColumnSpec> columnSpecs = requireColumns(physicalName);
        List<String> columnNames = userColumnNames(columnSpecs);
        boolean hasExternalIdColumn = hasExternalIdColumn(columnSpecs);

        String sql = "DELETE FROM " + escapeIdentifier(physicalName) + " WHERE \"id\" = ?" + " RETURNING " +
            selectColumns(columnNames, hasExternalIdColumn);

        List<DataTableRow> deletedDataTableRows = jdbcTemplate.query(sql, ps -> {
            ps.setLong(1, id);

        }, (resultSet, rowNum) -> toRow(resultSet, columnNames, hasExternalIdColumn));

        return deletedDataTableRows.isEmpty() ? null : deletedDataTableRows.getFirst();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRow deleteRowAfterReadingValues(DataTableRef dataTableRef, long id) {
        DataTableRow dataTableRow = getRow(dataTableRef, id);

        if (dataTableRow == null) {
            return null;
        }

        String sql = "DELETE FROM " + escapeIdentifier(dataTableRef.physicalName()) + " WHERE \"id\" = ?";

        int deletedRowCount = jdbcTemplate.update(sql, (PreparedStatementSetter) ps -> {
            ps.setLong(1, id);

        });

        return deletedRowCount == 0 ? null : dataTableRow;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<DataTableRow> deleteRowsAfterReadingRows(
        String physicalName, List<Long> ids, List<String> columnNames, boolean hasExternalIdColumn) {

        String placeholders = ids.stream()
            .map(id -> "?")
            .collect(Collectors.joining(", "));

        List<DataTableRow> existingDataTableRows = jdbcTemplate.query(
            "SELECT " + selectColumns(columnNames, hasExternalIdColumn) + " FROM " + escapeIdentifier(physicalName) +
                " WHERE \"id\" IN (" + placeholders + ")",
            (resultSet, rowNum) -> toRow(resultSet, columnNames, hasExternalIdColumn), ids.toArray());

        return deleteReadRows(physicalName, existingDataTableRows);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<DataTableRow> deleteReadRows(String physicalName, List<DataTableRow> dataTableRows) {
        String sql = "DELETE FROM " + escapeIdentifier(physicalName) + " WHERE \"id\" = ?";

        List<DataTableRow> deletedDataTableRows = new ArrayList<>();

        for (DataTableRow dataTableRow : dataTableRows) {
            int deletedRowCount = jdbcTemplate.update(sql, dataTableRow.id());

            if (deletedRowCount > 0) {
                deletedDataTableRows.add(dataTableRow);
            }
        }

        return deletedDataTableRows;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private UpsertResult upsertRowWithoutReturning(
        DataTableRef dataTableRef, String externalId, Map<String, Object> values, List<String> insertColumnNames,
        List<String> writtenColumnNames, Map<String, ColumnType> typeMap) {

        String physicalName = dataTableRef.physicalName();

        Optional<Long> existingId = findRowIdByExternalId(physicalName, externalId);

        if (existingId.isEmpty()) {
            String valuesClause = " (" + insertColumnNames.stream()
                .map(this::escapeIdentifier)
                .collect(Collectors.joining(", ")) + ") VALUES ("
                + insertColumnNames.stream()
                    .map(columnName -> "?")
                    .collect(Collectors.joining(", "))
                + ")";

            PreparedStatementSetter insertPreparedStatementSetter = ps -> {
                int i = 1;

                ps.setString(i++, externalId);

                setWrittenValues(ps, i, writtenColumnNames, values, typeMap);
            };

            try {
                DataTableRow insertedDataTableRow = insertRowAfterInserting(
                    dataTableRef, valuesClause, insertPreparedStatementSetter);

                if (insertedDataTableRow == null) {
                    throw new IllegalStateException("Upsert returned no row");
                }

                return new UpsertResult(insertedDataTableRow, true);
            } catch (DuplicateKeyException duplicateKeyException) {
                existingId = findRowIdByExternalId(physicalName, externalId);

                if (existingId.isEmpty()) {
                    throw duplicateKeyException;
                }
            }
        }

        long id = existingId.get();

        if (!writtenColumnNames.isEmpty()) {
            String setClause = writtenColumnNames.stream()
                .map(columnName -> escapeIdentifier(columnName) + " = ?")
                .collect(Collectors.joining(", "));

            jdbcTemplate.update(
                "UPDATE " + escapeIdentifier(physicalName) + " SET " + setClause + " WHERE \"id\" = ?",
                (PreparedStatementSetter) ps -> {
                    int nextIndex = setWrittenValues(ps, 1, writtenColumnNames, values, typeMap);

                    ps.setLong(nextIndex, id);
                });
        }

        DataTableRow dataTableRow = getRow(dataTableRef, id);

        if (dataTableRow == null) {
            throw new IllegalStateException("Upsert returned no row");
        }

        return new UpsertResult(dataTableRow, false);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Optional<Long> findRowIdByExternalId(String physicalName, String externalId) {
        List<Long> ids = jdbcTemplate.query(
            "SELECT \"id\" FROM " + escapeIdentifier(physicalName) + " WHERE "
                + escapeIdentifier(ReservedColumns.EXTERNAL_ID) + " = ?",
            (resultSet, rowNum) -> resultSet.getLong("id"), externalId);

        return ids.stream()
            .findFirst();
    }

    private int setWrittenValues(
        PreparedStatement ps, int startIndex, List<String> writtenColumnNames, Map<String, Object> values,
        Map<String, ColumnType> typeMap) throws SQLException {

        int i = startIndex;

        for (String columnName : writtenColumnNames) {
            ColumnType columnType = typeMap.getOrDefault(columnName.toLowerCase(Locale.ROOT), ColumnType.STRING);

            setParam(ps, i++, columnType, coerceValue(columnType, getValueCaseInsensitive(values, columnName)));
        }

        return i;
    }

    private boolean isReturningSupported() {
        return !dataTableDialect.isH2(jdbcTemplate);
    }

    private DataTableRow insertRowAfterInserting(
        DataTableRef dataTableRef, String valuesClause, PreparedStatementSetter preparedStatementSetter) {

        String sql = "INSERT INTO " + escapeIdentifier(dataTableRef.physicalName()) + valuesClause;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
            connection -> createInsertPreparedStatement(connection, sql, preparedStatementSetter), keyHolder);

        Number generatedKey = keyHolder.getKey();

        if (generatedKey == null) {
            return null;
        }

        return getRow(dataTableRef, generatedKey.longValue());
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRow updateRowAfterUpdating(
        DataTableRef dataTableRef, String setClause, long id, PreparedStatementSetter preparedStatementSetter) {

        String sql = "UPDATE " + escapeIdentifier(dataTableRef.physicalName()) + " SET " + setClause +
            " WHERE \"id\" = ?";

        int updatedRowCount = jdbcTemplate.update(sql, preparedStatementSetter);

        if (updatedRowCount == 0) {
            return null;
        }

        return getRow(dataTableRef, id);
    }
}
