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
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableLinkColumn;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.repository.DataTableLinkColumnRepository;
import com.bytechef.platform.data.table.configuration.repository.DataTableRepository;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
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
import com.bytechef.platform.data.table.util.DataTablePhysicalNameUtils;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
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

    private static final int MAX_EXTERNAL_ID_LENGTH = 255;

    private static final long MIN_EPOCH_MILLISECONDS = 100_000_000_000L;

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone(ZoneOffset.UTC);

    private static final String LINK_EXTERNAL_ID_SUFFIX = ".externalId";

    private static final Pattern EPOCH_PATTERN = Pattern.compile("-?\\d+");

    private static final Pattern LINK_CONSTRAINT_PATTERN =
        Pattern.compile("fk_dt_link_(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Set<String> FOREIGN_KEY_VIOLATION_SQL_STATES = Set.of("23503", "23506");

    private static final Set<DataTableRowFilter.Operation> LINK_FILTER_OPERATIONS = EnumSet.of(
        DataTableRowFilter.Operation.EQUALS, DataTableRowFilter.Operation.NOT_EQUALS,
        DataTableRowFilter.Operation.IN, DataTableRowFilter.Operation.EMPTY, DataTableRowFilter.Operation.NOT_EMPTY);

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
    private final DataTableLinkColumnRepository dataTableLinkColumnRepository;
    private final DataTableRepository dataTableRepository;
    private final JdbcTemplate jdbcTemplate;

    private Boolean returningSupported;

    @SuppressFBWarnings("EI")
    public DataTableRowServiceImpl(
        ApplicationEventPublisher applicationEventPublisher,
        DataTableLinkColumnRepository dataTableLinkColumnRepository, DataTableRepository dataTableRepository,
        JdbcTemplate jdbcTemplate) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.dataTableLinkColumnRepository = dataTableLinkColumnRepository;
        this.dataTableRepository = dataTableRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public boolean deleteRow(long dataTableId, long id, long environmentId) {
        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        List<DataTableLinkColumn> targetingLinkColumns =
            dataTableLinkColumnRepository.findAllByTargetDataTableIdAndEnvironmentId(dataTableId, environmentId);

        List<BlockingLink> blockingLinks = loadBlockingLinks(
            targetingLinkColumns, physicalName, environmentId, id, null);
        List<ClearedLinks> clearedLinks =
            selectClearedLinks(targetingLinkColumns, physicalName, environmentId, id, null);

        DataTableRow deletedDataTableRow;

        try {
            if (isReturningSupported()) {
                deletedDataTableRow = deleteRowReturningValues(physicalName, id);
            } else {
                deletedDataTableRow = deleteRowAfterReadingValues(dataTableId, physicalName, id, environmentId);
            }
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw toRowStillLinked(dataTableId, id, blockingLinks, dataIntegrityViolationException);
        }

        if (deletedDataTableRow == null) {
            return false;
        }

        publishWebhookEvent(dataTableId, DataTableWebhookType.RECORD_DELETED, List.of(deletedDataTableRow),
            environmentId);
        publishClearedLinkEvents(clearedLinks, environmentId);

        return true;
    }

    @Override
    @Transactional
    public List<DataTableRow> deleteAllRows(long dataTableId, long environmentId) {
        return deleteMatchingRows(dataTableId, DataTableRowConditions.matchAll(), environmentId);
    }

    @Override
    @Transactional
    public List<DataTableRow> deleteRows(long dataTableId, DataTableRowConditions conditions, long environmentId) {
        Objects.requireNonNull(conditions, "conditions");

        conditions.requireAtLeastOneCondition();

        return deleteMatchingRows(dataTableId, conditions, environmentId);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<DataTableRow> deleteMatchingRows(
        long dataTableId, DataTableRowConditions conditions, long environmentId) {

        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(dataTableId, physicalName, environmentId);

        List<String> columnNames = getNonIdColumnNames(columnSpecs);

        WhereClause whereClause = buildWhereClause(columnSpecs, conditions);

        String tableName = escapeIdentifier(physicalName);

        List<DataTableLinkColumn> targetingLinkColumns =
            dataTableLinkColumnRepository.findAllByTargetDataTableIdAndEnvironmentId(dataTableId, environmentId);

        List<BlockingLink> blockingLinks = loadBlockingLinks(
            targetingLinkColumns, physicalName, environmentId, null, whereClause);
        List<ClearedLinks> clearedLinks = selectClearedLinks(
            targetingLinkColumns, physicalName, environmentId, null, whereClause);

        List<DataTableRow> deletedDataTableRows;

        try {
            if (isReturningSupported()) {
                String sql = "DELETE FROM " + tableName + whereClause.sql() + " RETURNING " +
                    buildSelectColumns(columnNames);

                deletedDataTableRows = jdbcTemplate.query(
                    sql,
                    preparedStatement -> setFilterParameters(preparedStatement, 1, whereClause.filterParameters()),
                    (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));
            } else {
                deletedDataTableRows = selectRowsForUpdate(tableName, columnNames, whereClause);

                List<Long> ids = deletedDataTableRows.stream()
                    .map(DataTableRow::id)
                    .toList();

                if (!ids.isEmpty()) {
                    jdbcTemplate.update(
                        "DELETE FROM " + tableName + " WHERE " + buildAnyCondition("\"id\"", ids.size()),
                        (PreparedStatementSetter) preparedStatement -> setIds(preparedStatement, 1, ids));
                }
            }
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw toRowStillLinked(dataTableId, null, blockingLinks, dataIntegrityViolationException);
        }

        List<DataTableRow> sortedDataTableRows = sortById(deletedDataTableRows);

        publishWebhookEvent(dataTableId, DataTableWebhookType.RECORD_DELETED, sortedDataTableRows, environmentId);
        publishClearedLinkEvents(clearedLinks, environmentId);

        return sortedDataTableRows;
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public @Nullable DataTableRow getRow(long dataTableId, long id, long environmentId) {
        return getRow(dataTableId, id, List.of(), environmentId);
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public @Nullable DataTableRow getRow(long dataTableId, long id, List<String> expand, long environmentId) {
        Objects.requireNonNull(expand, "expand");

        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        Map<String, LinkSpec> expandLinkSpecs = resolveExpandLinkSpecs(
            dataTableId, physicalName, expand, environmentId);

        List<String> columnNames = listPhysicalColumns(physicalName).stream()
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

        if (rows.isEmpty()) {
            return null;
        }

        return expandLinks(rows, expandLinkSpecs, environmentId).getFirst();
    }

    @Override
    public String exportCsv(long dataTableId, long environmentId) {
        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(dataTableId, physicalName, environmentId);
        List<String> columnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .filter(n -> !"id".equalsIgnoreCase(n))
            .toList();

        List<String> headerNames = new ArrayList<>();

        for (String columnName : columnNames) {
            headerNames.add(columnName);

            if (isLinkColumn(columnSpecs, columnName)) {
                headerNames.add(columnName + LINK_EXTERNAL_ID_SUFFIX);
            }
        }

        StringWriter stringWriter = new StringWriter();

        CsvWriter csvWriter = CsvWriter.builder()
            .build(stringWriter);

        // Header
        csvWriter.writeRow(headerNames);

        List<DataTableRow> dataTableRows = listRows(dataTableId, Integer.MAX_VALUE, 0, environmentId);

        Map<String, Map<Long, String>> targetExternalIds = selectLinkTargetExternalIds(
            columnSpecs, dataTableRows, environmentId);

        for (DataTableRow dataTableRow : dataTableRows) {
            List<String> curValues = new ArrayList<>();

            for (String columnName : columnNames) {
                Map<String, Object> values = dataTableRow.values();

                Object value = ReservedColumns.EXTERNAL_ID.equalsIgnoreCase(columnName)
                    ? dataTableRow.externalId() : values.get(columnName);

                curValues.add(toCsvValue(value));

                if (isLinkColumn(columnSpecs, columnName)) {
                    Map<Long, String> externalIdsByRowId = targetExternalIds.get(columnName);

                    curValues.add(
                        value instanceof Number number ? toCsvValue(externalIdsByRowId.get(number.longValue())) : "");
                }
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
    public int importCsv(long dataTableId, String csv, long environmentId) {
        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        if (csv == null) {
            return 0;
        }

        List<ColumnSpec> columnSpecs = listColumns(dataTableId, physicalName, environmentId);

        List<String> columnNames = columnSpecs.stream()
            .map(ColumnSpec::name)
            .toList();

        CsvReader.CsvReaderBuilder csvReaderBuilder = CsvReader.builder();

        List<DataTableRow> insertedDataTableRows = new ArrayList<>();

        try (CsvReader csvReader = csvReaderBuilder.build(new StringReader(csv))) {
            List<String> headers = null;
            List<String> mappedColumnNames = null;
            List<Boolean> externalIdHeaderFlags = null;
            List<Map<String, Object>> parsedValues = new ArrayList<>();
            List<@Nullable String> parsedExternalIds = new ArrayList<>();
            List<Long> parsedLineNumbers = new ArrayList<>();

            for (CsvRow csvRow : csvReader) {
                List<String> fields = csvRow.getFields();

                Stream<String> fieldStream = fields.stream();

                if (headers == null) {
                    headers = fieldStream.map(s -> s == null ? "" : s.trim())
                        .toList();

                    // Build mapping from file index -> actual column name (skip unknown and id)
                    mappedColumnNames = new ArrayList<>(headers.size());
                    externalIdHeaderFlags = new ArrayList<>(headers.size());

                    for (String header : headers) {
                        if (header.equalsIgnoreCase("id")) {
                            mappedColumnNames.add(null);
                            externalIdHeaderFlags.add(false);

                            continue;
                        }

                        String columnName = columnNames.stream()
                            .filter(curColumnName -> curColumnName.equalsIgnoreCase(header))
                            .findFirst()
                            .orElse(null);

                        boolean externalIdHeader = false;

                        if (columnName == null && header.toLowerCase(Locale.ROOT)
                            .endsWith(LINK_EXTERNAL_ID_SUFFIX.toLowerCase(Locale.ROOT))) {

                            String linkColumnName =
                                header.substring(0, header.length() - LINK_EXTERNAL_ID_SUFFIX.length());

                            Optional<ColumnSpec> linkColumnSpecOptional = findColumnSpec(columnSpecs, linkColumnName);

                            if (linkColumnSpecOptional.isPresent() && linkColumnSpecOptional.get()
                                .type() == ColumnType.LINK) {

                                columnName = linkColumnSpecOptional.get()
                                    .name();
                                externalIdHeader = true;
                            }
                        }

                        mappedColumnNames.add(columnName);
                        externalIdHeaderFlags.add(externalIdHeader);
                    }

                    if (mappedColumnNames.stream()
                        .allMatch(Objects::isNull)) {

                        List<String> tableColumnNames = getUserColumnNames(columnSpecs);

                        throw new IllegalArgumentException(
                            "CSV header %s matches no column of data table %d; its columns are %s; no row was imported"
                                .formatted(headers, dataTableId, tableColumnNames));
                    }

                    continue;
                }

                if (fieldStream.allMatch(field -> field == null || field.isBlank())) {
                    continue;
                }

                Map<String, Object> values = new HashMap<>();
                Map<String, Object> externalIdLinkValues = new HashMap<>();

                int limit = Math.min(mappedColumnNames.size(), fields.size());

                for (int col = 0; col < limit; col++) {
                    String curColumnName = mappedColumnNames.get(col);

                    if (curColumnName == null) {
                        continue;
                    }

                    String field = fields.get(col);

                    if (externalIdHeaderFlags.get(col)) {
                        externalIdLinkValues.put(
                            curColumnName,
                            (field == null || field.isBlank()) ? null : Map.of("externalId", field.trim()));
                    } else {
                        values.put(curColumnName, (field == null || field.isEmpty()) ? null : field);
                    }
                }

                values.putAll(externalIdLinkValues);

                parsedValues.add(values);
                parsedExternalIds.add(toExternalId(values.remove(ReservedColumns.EXTERNAL_ID)));
                parsedLineNumbers.add(csvRow.getOriginalLineNumber());
            }

            List<Map<String, Object>> resolvedValues = resolveLinkValues(
                columnSpecs, parsedValues, environmentId,
                (index, message) -> "CSV line %d: %s; no row was imported".formatted(
                    parsedLineNumbers.get(index), message));

            insertedDataTableRows.addAll(
                insertCsvRows(
                    dataTableId, physicalName, columnSpecs, resolvedValues, parsedExternalIds, parsedLineNumbers,
                    environmentId));
        } catch (IOException exception) {
            throw new RuntimeException("Failed to import CSV", exception);
        }

        publishWebhookEvent(dataTableId, DataTableWebhookType.RECORD_CREATED, insertedDataTableRows, environmentId);

        return insertedDataTableRows.size();
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public DataTableRow insertRow(long dataTableId, Map<String, Object> values, long environmentId) {
        return insertRow(dataTableId, values, null, environmentId);
    }

    @Override
    public DataTableRow insertRow(
        long dataTableId, Map<String, Object> values, @Nullable String externalId, long environmentId) {

        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        Set<String> ignoredColumnNames = new LinkedHashSet<>();

        List<ColumnSpec> columnSpecs = listColumns(dataTableId, physicalName, environmentId);

        DataTableRow dataTableRow = insertRowWithoutEvent(
            dataTableId, physicalName, columnSpecs, resolveLinkValues(columnSpecs, values, environmentId),
            externalId, environmentId, ignoredColumnNames);

        logIgnoredColumnNames(dataTableId, environmentId, ignoredColumnNames);

        publishWebhookEvent(dataTableId, DataTableWebhookType.RECORD_CREATED, List.of(dataTableRow), environmentId);

        return dataTableRow;
    }

    @Override
    @Transactional
    public List<DataTableRow> insertRows(long dataTableId, List<Map<String, Object>> rowValues, long environmentId) {
        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(dataTableId, physicalName, environmentId);
        List<DataTableRow> insertedDataTableRows = new ArrayList<>();
        Set<String> ignoredColumnNames = new LinkedHashSet<>();

        List<Map<String, Object>> resolvedRowValues = resolveLinkValues(
            columnSpecs, rowValues, environmentId,
            (index, message) -> "Record %d: %s; no record was inserted".formatted(index + 1, message));

        for (int index = 0; index < resolvedRowValues.size(); index++) {
            try {
                insertedDataTableRows.add(
                    insertRowWithoutEvent(
                        dataTableId, physicalName, columnSpecs, resolvedRowValues.get(index), null, environmentId,
                        ignoredColumnNames));
            } catch (DataTableException dataTableException) {
                throw new DataTableException(
                    "Record %d: %s; no record was inserted".formatted(index + 1, dataTableException.getMessage()),
                    dataTableException, dataTableException.getErrorType());
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

        logIgnoredColumnNames(dataTableId, environmentId, ignoredColumnNames);

        publishWebhookEvent(dataTableId, DataTableWebhookType.RECORD_CREATED, insertedDataTableRows, environmentId);

        return insertedDataTableRows;
    }

    @Override
    @Transactional
    public List<DataTableRow> insertRows(
        long dataTableId, List<NewRow> newRows, CreateStrategy createStrategy, long environmentId) {

        Objects.requireNonNull(createStrategy, "createStrategy");

        if (createStrategy == CreateStrategy.UPSERT && newRows.stream()
            .anyMatch(DataTableRowServiceImpl::hasNoExternalId)) {

            throw new DataTableException(
                "Every row needs an external id under the UPSERT strategy",
                DataTableErrorType.ROW_EXTERNAL_ID_REQUIRED);
        }

        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(dataTableId, physicalName, environmentId);
        List<DataTableRow> dataTableRows = new ArrayList<>();
        List<DataTableRow> createdDataTableRows = new ArrayList<>();
        List<DataTableRow> updatedDataTableRows = new ArrayList<>();
        Set<String> ignoredColumnNames = new LinkedHashSet<>();

        List<Map<String, Object>> resolvedRowValues = resolveLinkValues(
            columnSpecs, newRows.stream()
                .map(NewRow::values)
                .toList(),
            environmentId, (index, message) -> "Record %d: %s; no record was inserted".formatted(index + 1, message));

        for (int index = 0; index < newRows.size(); index++) {
            NewRow newRow = newRows.get(index);
            Map<String, Object> resolvedValues = resolvedRowValues.get(index);

            try {
                if (createStrategy == CreateStrategy.UPSERT) {
                    UpsertResult upsertResult = upsertRowWithoutEvent(
                        dataTableId, physicalName, columnSpecs, newRow.externalId(), resolvedValues, environmentId,
                        ignoredColumnNames);

                    dataTableRows.add(upsertResult.row());

                    if (upsertResult.created()) {
                        createdDataTableRows.add(upsertResult.row());
                    } else {
                        updatedDataTableRows.add(upsertResult.row());
                    }
                } else {
                    DataTableRow dataTableRow = insertRowWithoutEvent(
                        dataTableId, physicalName, columnSpecs, resolvedValues, newRow.externalId(), environmentId,
                        ignoredColumnNames);

                    dataTableRows.add(dataTableRow);
                    createdDataTableRows.add(dataTableRow);
                }
            } catch (DataTableException dataTableException) {
                throw new DataTableException(
                    "Record %d: %s; no record was inserted".formatted(index + 1, dataTableException.getMessage()),
                    dataTableException, dataTableException.getErrorType());
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

        logIgnoredColumnNames(dataTableId, environmentId, ignoredColumnNames);

        publishWebhookEvent(dataTableId, DataTableWebhookType.RECORD_CREATED, createdDataTableRows, environmentId);
        publishWebhookEvent(dataTableId, DataTableWebhookType.RECORD_UPDATED, updatedDataTableRows, environmentId);

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
        long dataTableId, String physicalName, List<ColumnSpec> columnSpecs, Map<String, Object> values,
        @Nullable String externalId, long environmentId, Set<String> ignoredColumnNames) {

        List<String> allColumnNames = getColumnNames(columnSpecs);

        List<ColumnValue> columnValues = new ArrayList<>(
            getColumnValues(columnSpecs, values, ValuePolicy.SINGLE_ROW, ignoredColumnNames));

        if (externalId != null) {
            columnValues.add(toExternalIdColumnValue(dataTableId, columnSpecs, externalId));
        }

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

        try {
            if (isReturningSupported()) {
                result = insertRowReturningValues(
                    physicalName, valuesClause, allColumnNames, preparedStatementSetter);
            } else {
                result = insertRowAfterInserting(
                    dataTableId, physicalName, valuesClause, environmentId, preparedStatementSetter);
            }
        } catch (DuplicateKeyException duplicateKeyException) {
            throw toExternalIdConflict(externalId, duplicateKeyException);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw toLinkedRowNotFound(dataTableId, columnSpecs, values, dataIntegrityViolationException);
        }

        if (result == null) {
            throw new IllegalStateException("Failed to insert row");
        }

        return result;
    }

    @Override
    public List<DataTableRow> listRows(long dataTableId, int limit, int offset, long environmentId) {
        return findRows(dataTableId, DataTableRowConditions.matchAll(), List.of(), limit, offset, environmentId);
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<DataTableRow> findRows(
        long dataTableId, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int limit,
        int offset, long environmentId) {

        return findRows(dataTableId, conditions, sorts, limit, offset, List.of(), environmentId);
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<DataTableRow> findRows(
        long dataTableId, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int limit,
        int offset, List<String> expand, long environmentId) {

        Objects.requireNonNull(conditions, "conditions");
        Objects.requireNonNull(sorts, "sorts");
        Objects.requireNonNull(expand, "expand");

        if (limit < 0 || offset < 0) {
            throw new IllegalArgumentException(
                "limit and offset must not be negative, got limit=%d and offset=%d".formatted(limit, offset));
        }

        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(dataTableId, physicalName, environmentId);

        List<String> columnNames = getNonIdColumnNames(columnSpecs);

        Map<String, LinkSpec> expandLinkSpecs = resolveExpandLinkSpecs(columnSpecs, expand);

        WhereClause whereClause = buildWhereClause(columnSpecs, conditions);

        StringBuilder orderByClause = new StringBuilder(" ORDER BY ");

        for (DataTableRowSort sort : sorts) {
            ColumnSpec sortColumnSpec = getColumnSpec(columnSpecs, sort.column());
            DataTableRowSort.Direction direction = sort.direction();

            orderByClause.append(escapeIdentifier(sortColumnSpec.name()))
                .append(' ')
                .append(direction.name())
                .append(" NULLS LAST, ");
        }

        orderByClause.append("\"id\"");

        String sql = "SELECT " + buildSelectColumns(columnNames) + " FROM " + escapeIdentifier(physicalName) +
            whereClause.sql() + orderByClause + " LIMIT ? OFFSET ?";

        List<DataTableRow> dataTableRows = jdbcTemplate.query(sql, preparedStatement -> {
            int index = setFilterParameters(preparedStatement, 1, whereClause.filterParameters());

            preparedStatement.setInt(index++, limit);
            preparedStatement.setInt(index, offset);
        }, (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));

        return expandLinks(dataTableRows, expandLinkSpecs, environmentId);
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public long countRows(long dataTableId, DataTableRowConditions conditions, long environmentId) {
        Objects.requireNonNull(conditions, "conditions");

        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        WhereClause whereClause = buildWhereClause(listColumns(dataTableId, physicalName, environmentId), conditions);

        Long count = jdbcTemplate.query(
            "SELECT COUNT(*) FROM " + escapeIdentifier(physicalName) + whereClause.sql(),
            preparedStatement -> setFilterParameters(preparedStatement, 1, whereClause.filterParameters()),
            resultSet -> {
                if (!resultSet.next()) {
                    return 0L;
                }

                return resultSet.getLong(1);
            });

        return count == null ? 0L : count;
    }

    @Override
    public Optional<DataTableRow> fetchRowByExternalId(long dataTableId, String externalId, long environmentId) {
        Assert.hasText(externalId, "externalId must not be empty");

        List<DataTableRow> dataTableRows = findRows(
            dataTableId, externalIdConditions(externalId), List.of(), 1, 0, environmentId);

        return dataTableRows.stream()
            .findFirst();
    }

    @Override
    public DataTableRow updateRow(long dataTableId, long id, Map<String, Object> values, long environmentId) {
        return updateRow(dataTableId, id, values, null, environmentId);
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public DataTableRow updateRow(
        long dataTableId, long id, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        long environmentId) {

        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(dataTableId, physicalName, environmentId);

        List<String> allColumnNames = getColumnNames(columnSpecs);

        Set<String> ignoredColumnNames = new LinkedHashSet<>();

        Map<String, Object> resolvedValues = resolveLinkValues(columnSpecs, values, environmentId);

        List<ColumnValue> columnValues = new ArrayList<>(
            getColumnValues(columnSpecs, resolvedValues, ValuePolicy.SINGLE_ROW, ignoredColumnNames));

        if (externalIdPatch != null) {
            columnValues.add(toExternalIdColumnValue(dataTableId, columnSpecs, externalIdPatch.externalId()));
        }

        logIgnoredColumnNames(dataTableId, environmentId, ignoredColumnNames);

        if (columnValues.isEmpty()) {
            DataTableRow dataTableRow = getRow(dataTableId, id, environmentId);

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

        try {
            if (isReturningSupported()) {
                updatedDataTableRow = updateRowReturningValues(
                    physicalName, setClause, allColumnNames, preparedStatementSetter);
            } else {
                updatedDataTableRow = updateRowAfterUpdating(
                    dataTableId, physicalName, setClause, id, environmentId, preparedStatementSetter);
            }
        } catch (DuplicateKeyException duplicateKeyException) {
            throw toExternalIdConflict(
                externalIdPatch == null ? null : externalIdPatch.externalId(), duplicateKeyException);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw toLinkedRowNotFound(dataTableId, columnSpecs, resolvedValues, dataIntegrityViolationException);
        }

        if (updatedDataTableRow == null) {
            throw new IllegalArgumentException("Row not found: id=" + id);
        }

        publishWebhookEvent(dataTableId, DataTableWebhookType.RECORD_UPDATED, List.of(updatedDataTableRow),
            environmentId);

        return updatedDataTableRow;
    }

    @Override
    @Transactional
    public UpsertResult upsertRow(
        long dataTableId, String externalId, Map<String, Object> values, long environmentId) {

        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        Set<String> ignoredColumnNames = new LinkedHashSet<>();

        List<ColumnSpec> columnSpecs = listColumns(dataTableId, physicalName, environmentId);

        UpsertResult upsertResult = upsertRowWithoutEvent(
            dataTableId, physicalName, columnSpecs, externalId, resolveLinkValues(columnSpecs, values, environmentId),
            environmentId, ignoredColumnNames);

        logIgnoredColumnNames(dataTableId, environmentId, ignoredColumnNames);

        publishWebhookEvent(
            dataTableId,
            upsertResult.created() ? DataTableWebhookType.RECORD_CREATED : DataTableWebhookType.RECORD_UPDATED,
            List.of(upsertResult.row()), environmentId);

        return upsertResult;
    }

    private UpsertResult upsertRowWithoutEvent(
        long dataTableId, String physicalName, List<ColumnSpec> columnSpecs, @Nullable String externalId,
        Map<String, Object> values, long environmentId, Set<String> ignoredColumnNames) {

        try {
            return upsertRowStatement(
                dataTableId, physicalName, columnSpecs, externalId, values, environmentId, ignoredColumnNames);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw toLinkedRowNotFound(dataTableId, columnSpecs, values, dataIntegrityViolationException);
        }
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private UpsertResult upsertRowStatement(
        long dataTableId, String physicalName, List<ColumnSpec> columnSpecs, @Nullable String externalId,
        Map<String, Object> values, long environmentId, Set<String> ignoredColumnNames) {

        if (externalId == null || externalId.isBlank()) {
            throw new DataTableException(
                "An upsert needs an external id", DataTableErrorType.ROW_EXTERNAL_ID_REQUIRED);
        }

        ColumnValue externalIdColumnValue = toExternalIdColumnValue(dataTableId, columnSpecs, externalId);

        List<ColumnValue> columnValues = getColumnValues(
            columnSpecs, values, ValuePolicy.SINGLE_ROW, ignoredColumnNames);

        if (!isReturningSupported()) {
            Optional<DataTableRow> existingDataTableRow = findRows(
                dataTableId, externalIdConditions(externalId), List.of(), 1, 0, environmentId).stream()
                    .findFirst();

            if (existingDataTableRow.isEmpty()) {
                return new UpsertResult(
                    insertRowWithoutEvent(
                        dataTableId, physicalName, columnSpecs, values, externalId, environmentId, ignoredColumnNames),
                    true);
            }

            DataTableRow dataTableRow = existingDataTableRow.get();

            if (columnValues.isEmpty()) {
                return new UpsertResult(dataTableRow, false);
            }

            String setClause = buildSetClause(columnValues);

            DataTableRow updatedDataTableRow = updateRowAfterUpdating(
                dataTableId, physicalName, setClause, dataTableRow.id(), environmentId, preparedStatement -> {
                    int index = setColumnValues(preparedStatement, 1, columnValues);

                    preparedStatement.setLong(index, dataTableRow.id());
                });

            return new UpsertResult(Objects.requireNonNull(updatedDataTableRow, "updatedDataTableRow"), false);
        }

        List<ColumnValue> insertedColumnValues = new ArrayList<>();

        insertedColumnValues.add(externalIdColumnValue);
        insertedColumnValues.addAll(columnValues);

        String columnsClause = insertedColumnValues.stream()
            .map(columnValue -> {
                ColumnSpec columnSpec = columnValue.columnSpec();

                return escapeIdentifier(columnSpec.name());
            })
            .collect(Collectors.joining(", "));

        List<ColumnValue> updatedColumnValues = columnValues.isEmpty() ? List.of(externalIdColumnValue) : columnValues;

        String updateClause = updatedColumnValues.stream()
            .map(columnValue -> {
                ColumnSpec columnSpec = columnValue.columnSpec();

                String column = escapeIdentifier(columnSpec.name());

                return column + " = EXCLUDED." + column;
            })
            .collect(Collectors.joining(", "));

        List<String> columnNames = getNonIdColumnNames(columnSpecs);

        String sql = "INSERT INTO " + escapeIdentifier(physicalName) + " (" + columnsClause + ") VALUES (" +
            buildPlaceholders(insertedColumnValues.size()) + ") ON CONFLICT (" +
            escapeIdentifier(ReservedColumns.EXTERNAL_ID) + ") DO UPDATE SET " + updateClause + " RETURNING " +
            buildSelectColumns(columnNames) + ", (xmax = 0) AS \"created\"";

        UpsertResult upsertResult = jdbcTemplate.query(
            sql, preparedStatement -> setColumnValues(preparedStatement, 1, insertedColumnValues), resultSet -> {
                if (!resultSet.next()) {
                    return null;
                }

                return new UpsertResult(toDataTableRow(resultSet, columnNames), resultSet.getBoolean("created"));
            });

        return Objects.requireNonNull(upsertResult, "upsertResult");
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    @Transactional
    public List<DataTableRow> updateRows(
        long dataTableId, DataTableRowConditions conditions, Map<String, Object> values, long environmentId) {

        Objects.requireNonNull(conditions, "conditions");

        conditions.requireAtLeastOneCondition();

        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        checkHasId(dataTableId, physicalName);

        List<ColumnSpec> columnSpecs = listColumns(dataTableId, physicalName, environmentId);

        List<String> columnNames = getNonIdColumnNames(columnSpecs);

        WhereClause whereClause = buildWhereClause(columnSpecs, conditions);

        Map<String, Object> resolvedValues = resolveLinkValues(columnSpecs, values, environmentId);

        List<ColumnValue> columnValues = getColumnValues(
            columnSpecs, resolvedValues, ValuePolicy.BULK_UPDATE, Set.of());

        if (columnValues.isEmpty()) {
            throw new IllegalArgumentException("Updating rows requires a value for at least one column");
        }

        String tableName = escapeIdentifier(physicalName);
        String setClause = buildSetClause(columnValues);

        try {
            return updateMatchingRows(
                dataTableId, tableName, setClause, columnNames, whereClause, columnValues, environmentId);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw toLinkedRowNotFound(dataTableId, columnSpecs, resolvedValues, dataIntegrityViolationException);
        }
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<DataTableRow> updateMatchingRows(
        long dataTableId, String tableName, String setClause, List<String> columnNames, WhereClause whereClause,
        List<ColumnValue> columnValues, long environmentId) {

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

            String idsClause = " WHERE " + buildAnyCondition("\"id\"", ids.size());

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

        publishWebhookEvent(dataTableId, DataTableWebhookType.RECORD_UPDATED, sortedDataTableRows, environmentId);

        return sortedDataTableRows;
    }

    private List<DataTableRow> insertCsvRows(
        long dataTableId, String physicalName, List<ColumnSpec> columnSpecs, List<Map<String, Object>> rowValues,
        List<@Nullable String> externalIds, List<Long> lineNumbers, long environmentId) {

        List<DataTableRow> insertedDataTableRows = new ArrayList<>();

        for (int index = 0; index < rowValues.size(); index++) {
            long lineNumber = lineNumbers.get(index);

            try {
                insertedDataTableRows.add(
                    insertRowWithoutEvent(
                        dataTableId, physicalName, columnSpecs, rowValues.get(index), externalIds.get(index),
                        environmentId, new HashSet<>()));
            } catch (DataTableException dataTableException) {
                throw new DataTableException(
                    "CSV line %d: %s; no row was imported".formatted(lineNumber, dataTableException.getMessage()),
                    dataTableException, dataTableException.getErrorType());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                    "CSV line %d: %s; no row was imported".formatted(lineNumber, exception.getMessage()), exception);
            } catch (DataIntegrityViolationException dataIntegrityViolationException) {
                throw withRowPosition(
                    "CSV line " + lineNumber, "no row was imported", dataIntegrityViolationException);
            }
        }

        return insertedDataTableRows;
    }

    private static boolean isLinkColumn(List<ColumnSpec> columnSpecs, String columnName) {
        return findColumnSpec(columnSpecs, columnName).filter(columnSpec -> columnSpec.type() == ColumnType.LINK)
            .isPresent();
    }

    private Map<String, Map<Long, String>> selectLinkTargetExternalIds(
        List<ColumnSpec> columnSpecs, List<DataTableRow> dataTableRows, long environmentId) {

        Map<String, Map<Long, String>> targetExternalIds = new HashMap<>();

        for (ColumnSpec columnSpec : columnSpecs) {
            if (columnSpec.type() != ColumnType.LINK) {
                continue;
            }

            Set<Long> targetRowIds = new LinkedHashSet<>();

            for (DataTableRow dataTableRow : dataTableRows) {
                if (dataTableRow.values()
                    .get(columnSpec.name()) instanceof Number number) {

                    targetRowIds.add(number.longValue());
                }
            }

            Map<Long, String> externalIdsByRowId = new HashMap<>();

            if (!targetRowIds.isEmpty()) {
                LinkSpec linkSpec = Objects.requireNonNull(columnSpec.link());

                String targetPhysicalName = DataTablePhysicalNameUtils.getPhysicalName(
                    environmentId, linkSpec.targetDataTableId());

                for (DataTableRow targetDataTableRow : selectRowsByIds(
                    targetPhysicalName, List.of(ReservedColumns.EXTERNAL_ID), new ArrayList<>(targetRowIds))) {

                    externalIdsByRowId.put(targetDataTableRow.id(), targetDataTableRow.externalId());
                }
            }

            targetExternalIds.put(columnSpec.name(), externalIdsByRowId);
        }

        return targetExternalIds;
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

            if (ReservedColumns.EXTERNAL_ID.equalsIgnoreCase(columnName)) {
                if (valuePolicy == ValuePolicy.BULK_UPDATE) {
                    throw new IllegalArgumentException("The external_id column cannot be updated by conditions");
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
                        String.join(", ", unknownColumnNames), String.join(", ", getUserColumnNames(columnSpecs))));
            }

            ignoredColumnNames.addAll(unknownColumnNames);
        }

        return columnValues;
    }

    private static void logIgnoredColumnNames(long dataTableId, long environmentId, Set<String> ignoredColumnNames) {
        if (!ignoredColumnNames.isEmpty()) {
            log.warn(
                "Ignored values for unknown column(s) {} of data table {} in environment {}",
                String.join(", ", ignoredColumnNames), dataTableId, environmentId);
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
            .filter(name -> !"id".equalsIgnoreCase(name))
            .toList();
    }

    private static List<String> getUserColumnNames(List<ColumnSpec> columnSpecs) {
        return columnSpecs.stream()
            .map(ColumnSpec::name)
            .filter(name -> !ReservedColumns.isReserved(name))
            .toList();
    }

    private static DataTableRowConditions externalIdConditions(String externalId) {
        return DataTableRowConditions.anyOf(
            List.of(
                List.of(
                    new DataTableRowFilter(
                        ReservedColumns.EXTERNAL_ID, DataTableRowFilter.Operation.EQUALS, externalId))));
    }

    private static ColumnValue toExternalIdColumnValue(
        long dataTableId, List<ColumnSpec> columnSpecs, @Nullable String externalId) {

        ColumnSpec externalIdColumnSpec = findColumnSpec(columnSpecs, ReservedColumns.EXTERNAL_ID)
            .orElseThrow(() -> new IllegalStateException(
                "Data table %d has no external_id column".formatted(dataTableId)));

        if (externalId != null && (externalId.isBlank() || externalId.length() > MAX_EXTERNAL_ID_LENGTH)) {
            throw new DataTableException(
                "An external id must be 1-%d characters".formatted(MAX_EXTERNAL_ID_LENGTH),
                DataTableErrorType.ROW_VALUE_INVALID);
        }

        return new ColumnValue(externalIdColumnSpec, externalId);
    }

    private static RuntimeException toExternalIdConflict(
        @Nullable String externalId, DuplicateKeyException duplicateKeyException) {

        if (externalId == null) {
            return duplicateKeyException;
        }

        return new DataTableException(
            "External id '%s' is already used by another row".formatted(externalId), duplicateKeyException,
            DataTableErrorType.ROW_EXTERNAL_ID_CONFLICT);
    }

    private static @Nullable String toExternalId(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        String externalId = String.valueOf(value)
            .trim();

        return externalId.isEmpty() ? null : externalId;
    }

    private void publishWebhookEvent(
        long dataTableId, DataTableWebhookType dataTableWebhookType, List<DataTableRow> dataTableRows,
        long environmentId) {

        List<DataTableRow> publishedDataTableRows = dataTableRows;

        if (dataTableWebhookType == DataTableWebhookType.RECORD_CREATED) {
            publishedDataTableRows = dataTableRows.stream()
                .filter(DataTableRowServiceImpl::hasValues)
                .toList();
        }

        if (publishedDataTableRows.isEmpty()) {
            return;
        }

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(dataTableId, dataTableWebhookType, publishedDataTableRows, environmentId));
    }

    private static boolean hasValues(DataTableRow dataTableRow) {
        Map<String, Object> values = dataTableRow.values();

        return values.values()
            .stream()
            .anyMatch(Objects::nonNull);
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
            case INTEGER, LINK -> "bigint";
            case DATE -> "date";
            case DATE_TIME -> "timestamp";
            case BOOLEAN -> "boolean";
        };
    }

    private static Object toArrayElement(ColumnType type, Object value) {
        return switch (type) {
            case STRING -> String.valueOf(value);
            case NUMBER -> toBigDecimal((Number) value);
            case INTEGER, LINK -> ((Number) value).longValue();
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

    private String buildCondition(
        ColumnSpec columnSpec, DataTableRowFilter filter, List<FilterParameter> filterParameters) {

        String column = escapeIdentifier(columnSpec.name());
        ColumnType columnType = columnSpec.type();
        DataTableRowFilter.Operation operation = filter.operation();

        if (columnType == ColumnType.LINK && !LINK_FILTER_OPERATIONS.contains(operation)) {
            throw new DataTableException(
                "Filter operation %s is not supported on link column '%s'; use EQUALS, NOT_EQUALS, IN, EMPTY or NOT_EMPTY"
                    .formatted(operation, columnSpec.name()),
                DataTableErrorType.FILTER_INVALID);
        }

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
            case CONTAINS -> addLike(column, "%", "%", false, false, columnSpec, filter, filterParameters);
            case CONTAINS_IGNORE_CASE -> addLike(column, "%", "%", false, true, columnSpec, filter, filterParameters);
            case NOT_CONTAINS -> addLike(column, "%", "%", true, false, columnSpec, filter, filterParameters);
            case STARTS_WITH -> addLike(column, "", "%", false, false, columnSpec, filter, filterParameters);
            case ENDS_WITH -> addLike(column, "%", "", false, false, columnSpec, filter, filterParameters);
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
        String column, String prefix, String suffix, boolean negated, boolean ignoreCase, ColumnSpec columnSpec,
        DataTableRowFilter filter, List<FilterParameter> filterParameters) {

        Object rawValue = filter.singleValue();

        String value = rawValue instanceof String string ? string : (String) coerceFilterValue(columnSpec, rawValue);

        filterParameters.add(new FilterParameter(ColumnType.STRING, prefix + escapeLikePattern(value) + suffix));

        if (negated) {
            return "(" + column + " IS NULL OR " + column + " NOT LIKE ? ESCAPE '\\')";
        }

        if (ignoreCase) {
            return "LOWER(" + column + ") LIKE LOWER(?) ESCAPE '\\'";
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

        if (columnType == ColumnType.LINK) {
            return toLinkId(columnSpec, rawValue);
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

    private void checkHasId(long dataTableId, String physicalName) {
        String sql =
            "SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ? AND " +
                "column_name = 'id'";

        List<Integer> exists = jdbcTemplate.query(
            sql, preparedStatement -> preparedStatement.setString(1, physicalName), (resultSet, rowNum) -> 1);

        if (!exists.isEmpty()) {
            return;
        }

        List<ColumnSpec> columnSpecs = listPhysicalColumns(physicalName);

        if (columnSpecs.isEmpty()) {
            throw new IllegalArgumentException("Data table %d not found in this environment".formatted(dataTableId));
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
                case INTEGER, LINK -> com.bytechef.commons.util.NumberUtils.parseLong(trimmed);
                case NUMBER -> com.bytechef.commons.util.NumberUtils.parseBigDecimal(trimmed);
                case DATE -> DateUtils.parseSqlDate(trimmed);
                case DATE_TIME -> DateUtils.parseSqlTimestamp(trimmed);
            };
        }

        return switch (type) {
            case STRING -> String.valueOf(rawValue);
            case BOOLEAN -> (rawValue instanceof Boolean) ? rawValue
                : BooleanUtils.parseBoolean(String.valueOf(rawValue));
            case INTEGER, LINK -> (rawValue instanceof Number n) ? Long.valueOf(n.longValue())
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
        String externalId = null;

        for (String columnName : columnNames) {
            if (ReservedColumns.EXTERNAL_ID.equalsIgnoreCase(columnName)) {
                externalId = resultSet.getString(columnName);

                continue;
            }

            Object value = resultSet.getObject(columnName);

            if (value instanceof Timestamp) {
                value = resultSet.getTimestamp(columnName, Calendar.getInstance(UTC_TIME_ZONE));
            }

            values.put(columnName, value);
        }

        return new DataTableRow(resultSet.getLong("id"), externalId, values);
    }

    private List<ColumnSpec> listColumns(long dataTableId, String physicalName, long environmentId) {
        List<ColumnSpec> physicalColumnSpecs = listPhysicalColumns(physicalName);

        List<DataTableLinkColumn> dataTableLinkColumns =
            dataTableLinkColumnRepository.findAllByDataTableIdAndEnvironmentId(dataTableId, environmentId);

        if (dataTableLinkColumns.isEmpty()) {
            return physicalColumnSpecs;
        }

        Map<String, LinkSpec> linkSpecs = new HashMap<>();

        for (DataTableLinkColumn dataTableLinkColumn : dataTableLinkColumns) {
            linkSpecs.put(dataTableLinkColumn.getColumnName(), dataTableLinkColumn.toLinkSpec());
        }

        List<ColumnSpec> columnSpecs = new ArrayList<>(physicalColumnSpecs.size());

        for (ColumnSpec columnSpec : physicalColumnSpecs) {
            LinkSpec linkSpec = linkSpecs.get(columnSpec.name());

            columnSpecs.add(
                linkSpec == null ? columnSpec : new ColumnSpec(columnSpec.name(), ColumnType.LINK, linkSpec));
        }

        return columnSpecs;
    }

    private Map<String, Object> resolveLinkValues(
        List<ColumnSpec> columnSpecs, Map<String, Object> values, long environmentId) {

        List<Map<String, Object>> resolvedRowValues = resolveLinkValues(
            columnSpecs, List.of(values), environmentId, (index, message) -> message);

        return resolvedRowValues.getFirst();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<Map<String, Object>> resolveLinkValues(
        List<ColumnSpec> columnSpecs, List<Map<String, Object>> rowValues, long environmentId,
        BiFunction<Integer, String, String> messageDecorator) {

        Map<String, ColumnSpec> linkColumnSpecs = new HashMap<>();

        for (ColumnSpec columnSpec : columnSpecs) {
            if (columnSpec.type() == ColumnType.LINK) {
                linkColumnSpecs.put(columnSpec.name(), columnSpec);
            }
        }

        if (linkColumnSpecs.isEmpty()) {
            return rowValues;
        }

        Map<String, Set<String>> externalIdsByColumnName = new HashMap<>();

        for (int index = 0; index < rowValues.size(); index++) {
            for (Map.Entry<String, Object> entry : rowValues.get(index)
                .entrySet()) {

                ColumnSpec linkColumnSpec = linkColumnSpecs.get(entry.getKey()
                    .toLowerCase(Locale.ROOT));

                if (linkColumnSpec != null && entry.getValue() instanceof Map<?, ?> linkValue) {
                    Set<String> externalIds = externalIdsByColumnName.computeIfAbsent(
                        linkColumnSpec.name(), columnName -> new LinkedHashSet<>());

                    externalIds.add(toLinkExternalId(linkColumnSpec, linkValue, index, messageDecorator));
                }
            }
        }

        if (externalIdsByColumnName.isEmpty()) {
            return rowValues;
        }

        Map<String, Map<String, Long>> idsByColumnName = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : externalIdsByColumnName.entrySet()) {
            LinkSpec linkSpec = Objects.requireNonNull(linkColumnSpecs.get(entry.getKey())
                .link());

            idsByColumnName.put(
                entry.getKey(),
                selectIdsByExternalId(
                    DataTablePhysicalNameUtils.getPhysicalName(environmentId, linkSpec.targetDataTableId()),
                    new ArrayList<>(entry.getValue())));
        }

        List<Map<String, Object>> resolvedRowValues = new ArrayList<>(rowValues.size());

        for (int index = 0; index < rowValues.size(); index++) {
            Map<String, Object> resolvedValues = new LinkedHashMap<>(rowValues.get(index));

            for (Map.Entry<String, Object> entry : resolvedValues.entrySet()) {
                ColumnSpec linkColumnSpec = linkColumnSpecs.get(entry.getKey()
                    .toLowerCase(Locale.ROOT));

                if (linkColumnSpec == null || !(entry.getValue() instanceof Map<?, ?> linkValue)) {
                    continue;
                }

                String externalId = toLinkExternalId(linkColumnSpec, linkValue, index, messageDecorator);

                Map<String, Long> idsByExternalId = idsByColumnName.get(linkColumnSpec.name());

                Long id = idsByExternalId.get(externalId);

                if (id == null) {
                    LinkSpec linkSpec = Objects.requireNonNull(linkColumnSpec.link());

                    throw new DataTableException(
                        messageDecorator.apply(
                            index,
                            "Column '%s' links to external id '%s' which matches no row of data table %d".formatted(
                                linkColumnSpec.name(), externalId, linkSpec.targetDataTableId())),
                        DataTableErrorType.LINKED_ROW_NOT_FOUND);
                }

                entry.setValue(id);
            }

            resolvedRowValues.add(resolvedValues);
        }

        return resolvedRowValues;
    }

    private static String toLinkExternalId(
        ColumnSpec linkColumnSpec, Map<?, ?> linkValue, int index,
        BiFunction<Integer, String, String> messageDecorator) {

        Object externalId = linkValue.get("externalId");

        if (linkValue.size() != 1 || externalId == null || String.valueOf(externalId)
            .isBlank()) {

            throw new DataTableException(
                messageDecorator.apply(
                    index,
                    "Value for link column '%s' must be a row id or {\"externalId\": \"...\"}".formatted(
                        linkColumnSpec.name())),
                DataTableErrorType.ROW_VALUE_INVALID);
        }

        return String.valueOf(externalId)
            .trim();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Map<String, Long> selectIdsByExternalId(String targetPhysicalName, List<String> externalIds) {
        String sql = "SELECT \"id\", " + escapeIdentifier(ReservedColumns.EXTERNAL_ID) + " FROM " +
            escapeIdentifier(targetPhysicalName) + " WHERE " +
            buildAnyCondition(escapeIdentifier(ReservedColumns.EXTERNAL_ID), externalIds.size());

        Map<String, Long> idsByExternalId = jdbcTemplate.query(sql, preparedStatement -> {
            int index = 1;

            for (List<String> chunk : partition(externalIds)) {
                setArrayParam(preparedStatement, index++, ColumnType.STRING, chunk.toArray());
            }
        }, resultSet -> {
            Map<String, Long> result = new HashMap<>();

            while (resultSet.next()) {
                result.put(resultSet.getString(ReservedColumns.EXTERNAL_ID), resultSet.getLong("id"));
            }

            return result;
        });

        return Objects.requireNonNull(idsByExternalId, "idsByExternalId");
    }

    private static Long toLinkId(ColumnSpec columnSpec, Object rawValue) {
        Long id = null;

        if (!(rawValue instanceof Collection<?>) && !(rawValue instanceof Map<?, ?>) && !isArray(rawValue)
            && !(rawValue instanceof Boolean)) {

            id = toLong(rawValue);
        }

        if (id == null) {
            throw new DataTableException(
                "Value '%s' is not a valid link for column '%s'; use a row id or {\"externalId\": \"...\"}".formatted(
                    rawValue, columnSpec.name()),
                DataTableErrorType.ROW_VALUE_INVALID);
        }

        return id;
    }

    private static boolean isForeignKeyViolation(DataIntegrityViolationException dataIntegrityViolationException) {
        Throwable mostSpecificCause = dataIntegrityViolationException.getMostSpecificCause();

        return mostSpecificCause instanceof SQLException sqlException
            && FOREIGN_KEY_VIOLATION_SQL_STATES.contains(sqlException.getSQLState());
    }

    private static @Nullable Long parseLinkColumnId(DataIntegrityViolationException dataIntegrityViolationException) {
        Throwable mostSpecificCause = dataIntegrityViolationException.getMostSpecificCause();

        Matcher matcher = LINK_CONSTRAINT_PATTERN.matcher(String.valueOf(mostSpecificCause.getMessage()));

        return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
    }

    private static RuntimeException toLinkedRowNotFound(
        long dataTableId, List<ColumnSpec> columnSpecs, Map<String, Object> values,
        DataIntegrityViolationException dataIntegrityViolationException) {

        if (!isForeignKeyViolation(dataIntegrityViolationException)) {
            return dataIntegrityViolationException;
        }

        Throwable mostSpecificCause = dataIntegrityViolationException.getMostSpecificCause();

        String causeMessage = String.valueOf(mostSpecificCause.getMessage())
            .toLowerCase(Locale.ROOT);

        List<String> writtenLinkColumnNames = columnSpecs.stream()
            .filter(columnSpec -> columnSpec.type() == ColumnType.LINK)
            .map(ColumnSpec::name)
            .filter(columnName -> values.entrySet()
                .stream()
                .anyMatch(entry -> columnName.equalsIgnoreCase(entry.getKey()) && entry.getValue() != null))
            .toList();

        List<String> mentionedLinkColumnNames = writtenLinkColumnNames.stream()
            .filter(columnName -> causeMessage.contains(columnName))
            .toList();

        List<String> linkColumnNames = mentionedLinkColumnNames.isEmpty()
            ? writtenLinkColumnNames : mentionedLinkColumnNames;

        return new DataTableException(
            "Link column(s) %s of data table %d point to a row that does not exist".formatted(
                linkColumnNames, dataTableId),
            dataIntegrityViolationException, DataTableErrorType.LINKED_ROW_NOT_FOUND);
    }

    private List<BlockingLink> loadBlockingLinks(
        List<DataTableLinkColumn> targetingLinkColumns, String physicalName, long environmentId, @Nullable Long rowId,
        @Nullable WhereClause whereClause) {

        List<BlockingLink> blockingLinks = new ArrayList<>();

        for (DataTableLinkColumn dataTableLinkColumn : targetingLinkColumns) {
            if (dataTableLinkColumn.getOnDelete() != LinkDeleteRule.BLOCK) {
                continue;
            }

            String tableName = dataTableRepository.findById(dataTableLinkColumn.getDataTableId())
                .map(DataTable::getName)
                .orElse(String.valueOf(dataTableLinkColumn.getDataTableId()));

            long linkingRowCount = countLinkingRows(
                DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableLinkColumn.getDataTableId()),
                dataTableLinkColumn.getColumnName(), physicalName, rowId, whereClause);

            blockingLinks.add(
                new BlockingLink(
                    dataTableLinkColumn.getId(), tableName, dataTableLinkColumn.getColumnName(), linkingRowCount));
        }

        return blockingLinks;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private long countLinkingRows(
        String sourcePhysicalName, String columnName, String targetPhysicalName, @Nullable Long rowId,
        @Nullable WhereClause whereClause) {

        String column = escapeIdentifier(columnName);
        String sourceTableName = escapeIdentifier(sourcePhysicalName);

        Long count;

        if (rowId != null) {
            count = jdbcTemplate.query(
                "SELECT COUNT(*) FROM " + sourceTableName + " WHERE " + column + " = ?",
                preparedStatement -> preparedStatement.setLong(1, rowId), resultSet -> {
                    resultSet.next();

                    return resultSet.getLong(1);
                });
        } else {
            WhereClause targetWhereClause = Objects.requireNonNull(whereClause, "whereClause");

            count = jdbcTemplate.query(
                "SELECT COUNT(*) FROM " + sourceTableName + " WHERE " + column + " IN (SELECT \"id\" FROM " +
                    escapeIdentifier(targetPhysicalName) + targetWhereClause.sql() + ")",
                preparedStatement -> setFilterParameters(preparedStatement, 1, targetWhereClause.filterParameters()),
                resultSet -> {
                    resultSet.next();

                    return resultSet.getLong(1);
                });
        }

        return count == null ? 0L : count;
    }

    private List<ClearedLinks> selectClearedLinks(
        List<DataTableLinkColumn> targetingLinkColumns, String targetPhysicalName, long environmentId,
        @Nullable Long rowId, @Nullable WhereClause whereClause) {

        Map<Long, Set<Long>> sourceRowIdsByDataTableId = new LinkedHashMap<>();

        for (DataTableLinkColumn dataTableLinkColumn : targetingLinkColumns) {
            if (dataTableLinkColumn.getOnDelete() != LinkDeleteRule.CLEAR) {
                continue;
            }

            List<Long> sourceRowIds = selectLinkingRowIds(
                DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableLinkColumn.getDataTableId()),
                dataTableLinkColumn.getColumnName(), targetPhysicalName, rowId, whereClause);

            if (!sourceRowIds.isEmpty()) {
                Set<Long> mergedSourceRowIds = sourceRowIdsByDataTableId.computeIfAbsent(
                    dataTableLinkColumn.getDataTableId(), key -> new TreeSet<>());

                mergedSourceRowIds.addAll(sourceRowIds);
            }
        }

        List<ClearedLinks> clearedLinks = new ArrayList<>();

        for (Map.Entry<Long, Set<Long>> entry : sourceRowIdsByDataTableId.entrySet()) {
            clearedLinks.add(new ClearedLinks(entry.getKey(), new ArrayList<>(entry.getValue())));
        }

        return clearedLinks;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<Long> selectLinkingRowIds(
        String sourcePhysicalName, String columnName, String targetPhysicalName, @Nullable Long rowId,
        @Nullable WhereClause whereClause) {

        String column = escapeIdentifier(columnName);
        String sourceTableName = escapeIdentifier(sourcePhysicalName);

        if (rowId != null) {
            return jdbcTemplate.query(
                "SELECT \"id\" FROM " + sourceTableName + " WHERE " + column + " = ?",
                preparedStatement -> preparedStatement.setLong(1, rowId),
                (resultSet, rowNum) -> resultSet.getLong(1));
        }

        WhereClause targetWhereClause = Objects.requireNonNull(whereClause, "whereClause");

        return jdbcTemplate.query(
            "SELECT \"id\" FROM " + sourceTableName + " WHERE " + column + " IN (SELECT \"id\" FROM " +
                escapeIdentifier(targetPhysicalName) + targetWhereClause.sql() + ")",
            preparedStatement -> setFilterParameters(preparedStatement, 1, targetWhereClause.filterParameters()),
            (resultSet, rowNum) -> resultSet.getLong(1));
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void publishClearedLinkEvents(List<ClearedLinks> clearedLinks, long environmentId) {
        for (ClearedLinks clearedLink : clearedLinks) {
            long sourceDataTableId = clearedLink.dataTableId();

            String sourcePhysicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, sourceDataTableId);

            List<String> columnNames =
                getNonIdColumnNames(listColumns(sourceDataTableId, sourcePhysicalName, environmentId));

            List<Long> sourceRowIds = clearedLink.rowIds();

            List<DataTableRow> sourceDataTableRows = selectRowsByIds(sourcePhysicalName, columnNames, sourceRowIds);

            publishWebhookEvent(
                sourceDataTableId, DataTableWebhookType.RECORD_UPDATED, sourceDataTableRows, environmentId);
        }
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<DataTableRow> selectRowsByIds(String physicalName, List<String> columnNames, List<Long> rowIds) {
        return jdbcTemplate.query(
            "SELECT " + buildSelectColumns(columnNames) + " FROM " + escapeIdentifier(physicalName) + " WHERE " +
                buildAnyCondition("\"id\"", rowIds.size()) + " ORDER BY \"id\"",
            (PreparedStatementSetter) preparedStatement -> setIds(preparedStatement, 1, rowIds),
            (resultSet, rowNum) -> toDataTableRow(resultSet, columnNames));
    }

    private Map<String, LinkSpec> resolveExpandLinkSpecs(
        long dataTableId, String physicalName, List<String> expand, long environmentId) {

        if (expand.isEmpty()) {
            return Map.of();
        }

        return resolveExpandLinkSpecs(listColumns(dataTableId, physicalName, environmentId), expand);
    }

    private static Map<String, LinkSpec> resolveExpandLinkSpecs(List<ColumnSpec> columnSpecs, List<String> expand) {
        if (expand.isEmpty()) {
            return Map.of();
        }

        Map<String, LinkSpec> expandLinkSpecs = new LinkedHashMap<>();

        for (String columnName : expand) {
            Optional<ColumnSpec> columnSpecOptional = findColumnSpec(columnSpecs, columnName);

            if (columnSpecOptional.isEmpty() || columnSpecOptional.get()
                .type() != ColumnType.LINK) {

                throw new DataTableException(
                    "Column '" + columnName + "' is not a link column", DataTableErrorType.COLUMN_NOT_FOUND);
            }

            ColumnSpec columnSpec = columnSpecOptional.get();

            expandLinkSpecs.put(columnSpec.name(), columnSpec.link());
        }

        return expandLinkSpecs;
    }

    private List<DataTableRow> expandLinks(
        List<DataTableRow> dataTableRows, Map<String, LinkSpec> expandLinkSpecs, long environmentId) {

        if (expandLinkSpecs.isEmpty() || dataTableRows.isEmpty()) {
            return dataTableRows;
        }

        List<Map<String, Object>> expandedValues = new ArrayList<>(dataTableRows.size());

        for (DataTableRow dataTableRow : dataTableRows) {
            expandedValues.add(new HashMap<>(dataTableRow.values()));
        }

        for (Map.Entry<String, LinkSpec> entry : expandLinkSpecs.entrySet()) {
            String columnName = entry.getKey();

            Set<Long> targetRowIds = new LinkedHashSet<>();

            for (DataTableRow dataTableRow : dataTableRows) {
                if (dataTableRow.values()
                    .get(columnName) instanceof Number number) {

                    targetRowIds.add(number.longValue());
                }
            }

            Map<Long, DataTableRow> targetDataTableRows = new HashMap<>();

            if (!targetRowIds.isEmpty()) {
                String targetPhysicalName = DataTablePhysicalNameUtils.getPhysicalName(
                    environmentId, entry.getValue()
                        .targetDataTableId());

                List<String> targetColumnNames = getNonIdColumnNames(listPhysicalColumns(targetPhysicalName));

                for (DataTableRow targetDataTableRow : selectRowsByIds(
                    targetPhysicalName, targetColumnNames, new ArrayList<>(targetRowIds))) {

                    targetDataTableRows.put(targetDataTableRow.id(), targetDataTableRow);
                }
            }

            for (int index = 0; index < dataTableRows.size(); index++) {
                Object linkValue = dataTableRows.get(index)
                    .values()
                    .get(columnName);

                expandedValues.get(index)
                    .put(
                        columnName,
                        linkValue instanceof Number number ? targetDataTableRows.get(number.longValue()) : null);
            }
        }

        List<DataTableRow> expandedDataTableRows = new ArrayList<>(dataTableRows.size());

        for (int index = 0; index < dataTableRows.size(); index++) {
            DataTableRow dataTableRow = dataTableRows.get(index);

            expandedDataTableRows.add(
                new DataTableRow(dataTableRow.id(), dataTableRow.externalId(), expandedValues.get(index)));
        }

        return expandedDataTableRows;
    }

    private static RuntimeException toRowStillLinked(
        long dataTableId, @Nullable Long rowId, List<BlockingLink> blockingLinks,
        DataIntegrityViolationException dataIntegrityViolationException) {

        if (!isForeignKeyViolation(dataIntegrityViolationException)) {
            return dataIntegrityViolationException;
        }

        Long linkColumnId = parseLinkColumnId(dataIntegrityViolationException);

        List<BlockingLink> firedBlockingLinks = blockingLinks.stream()
            .filter(blockingLink -> linkColumnId != null && linkColumnId.equals(blockingLink.linkColumnId()))
            .toList();

        if (firedBlockingLinks.isEmpty()) {
            firedBlockingLinks = blockingLinks.stream()
                .filter(blockingLink -> blockingLink.linkingRowCount() > 0)
                .toList();
        }

        String linkedFrom = firedBlockingLinks.stream()
            .map(blockingLink -> "%d %s in %s.%s".formatted(
                blockingLink.linkingRowCount(), blockingLink.linkingRowCount() == 1 ? "row" : "rows",
                blockingLink.tableName(), blockingLink.columnName()))
            .collect(Collectors.joining(", "));

        String subject = rowId == null
            ? "Rows to delete from data table %d are".formatted(dataTableId) : "Row %d is".formatted(rowId);

        return new DataTableException(
            "%s linked from %s; no row was deleted".formatted(subject, linkedFrom), dataIntegrityViolationException,
            DataTableErrorType.ROW_STILL_LINKED);
    }

    private List<ColumnSpec> listPhysicalColumns(String physicalName) {
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
                case INTEGER, LINK -> Types.BIGINT;
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
            case INTEGER, LINK -> {
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

    private static boolean hasNoExternalId(NewRow newRow) {
        String externalId = newRow.externalId();

        return externalId == null || externalId.isBlank();
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
        List<String> columnNames = listPhysicalColumns(physicalName).stream()
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
        long dataTableId, String physicalName, long id, long environmentId) {

        DataTableRow dataTableRow = getRow(dataTableId, id, environmentId);

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
        long dataTableId, String physicalName, String valuesClause, long environmentId,
        PreparedStatementSetter preparedStatementSetter) {

        String sql = "INSERT INTO " + escapeIdentifier(physicalName) + valuesClause;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
            connection -> createInsertPreparedStatement(connection, sql, preparedStatementSetter), keyHolder);

        Number generatedKey = keyHolder.getKey();

        if (generatedKey == null) {
            return null;
        }

        return getRow(dataTableId, generatedKey.longValue(), environmentId);
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
        long dataTableId, String physicalName, String setClause, long id, long environmentId,
        PreparedStatementSetter preparedStatementSetter) {

        String sql = "UPDATE " + escapeIdentifier(physicalName) + " SET " + setClause + " WHERE \"id\" = ?";

        int updatedRowCount = jdbcTemplate.update(sql, preparedStatementSetter);

        if (updatedRowCount == 0) {
            return null;
        }

        return getRow(dataTableId, id, environmentId);
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

    private record BlockingLink(
        @Nullable Long linkColumnId, String tableName, String columnName, long linkingRowCount) {
    }

    private record ClearedLinks(long dataTableId, List<Long> rowIds) {
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
