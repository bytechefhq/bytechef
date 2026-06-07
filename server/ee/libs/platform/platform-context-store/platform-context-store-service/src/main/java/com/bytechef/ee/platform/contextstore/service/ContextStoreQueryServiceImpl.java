/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuery;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuerySort;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreSearchResult;
import com.bytechef.ee.platform.contextstore.repository.ContextStoreRecordRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Read-side implementation of {@link ContextStoreQueryService}.
 *
 * <p>
 * Filter ops translate to a JOIN of {@code context_store_record_index} per filter (one alias per filter). For the
 * equality / membership / pattern ops (EQ, NEQ, IN, CONTAINS, STARTS_WITH) the {@code value_text} column is always
 * targeted; for the comparison ops (GT, GTE, LT, LTE, BETWEEN) the typed column is inferred from the runtime type of
 * the filter value: {@code Number} → {@code value_numeric}, {@code Instant}/{@code Temporal}/ISO-8601 string →
 * {@code value_timestamp}, otherwise {@code value_text} (lexicographic compare).
 * </p>
 *
 * <p>
 * Sort fields must be present in {@code context_store_record_index}; if no sort is supplied the default is
 * {@code ORDER BY r.id ASC}. Cursor pagination is intentionally simple: the cursor is a base64-encoded {@code lastId}
 * value. The sort column is honored in the {@code ORDER BY} but not embedded in the cursor — re-pagination during
 * concurrent inserts may therefore show duplicates. This is an acceptable trade-off for the MVP and is documented here
 * so a follow-up can move to a tuple {@code (sortValue, lastId)} cursor when needed.
 * </p>
 *
 * <p>
 * The {@code fields} projection on {@link ContextStoreQuery} is reserved for a future enhancement; the current
 * implementation always returns the full payload from the JSONB column.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
@SuppressFBWarnings({
    "EI_EXPOSE_REP2"
})
public class ContextStoreQueryServiceImpl implements ContextStoreQueryService {

    private static final Logger log = LoggerFactory.getLogger(ContextStoreQueryServiceImpl.class);

    private static final String COLUMN_VALUE_TEXT = "value_text";
    private static final String COLUMN_VALUE_NUMERIC = "value_numeric";
    private static final String COLUMN_VALUE_TIMESTAMP = "value_timestamp";

    private final ContextStoreRecordRepository contextStoreRecordRepository;
    private final NamedParameterJdbcOperations jdbcOperations;
    private final ObjectMapper objectMapper;
    private final RowMapper<ContextStoreRecord> rowMapper;

    public ContextStoreQueryServiceImpl(
        ContextStoreRecordRepository contextStoreRecordRepository, NamedParameterJdbcOperations jdbcOperations,
        ObjectMapper objectMapper) {

        this.contextStoreRecordRepository = contextStoreRecordRepository;
        this.jdbcOperations = jdbcOperations;
        this.objectMapper = objectMapper;
        this.rowMapper = new ContextStoreRecordRowMapper();
    }

    @Override
    public ContextStoreSearchResult search(ContextStoreQuery query) {
        Objects.requireNonNull(query, "query");

        StringBuilder sql = new StringBuilder(
            "SELECT r.id, r.source_id, r.source_record_id, " +
                "r.payload, r.payload_hash, r.last_seen_at, r.deleted_at, " +
                "r.created_date, r.last_modified_date " +
                "FROM context_store_record r");

        MapSqlParameterSource params = new MapSqlParameterSource();

        List<ContextStoreQueryFilter> filters = query.filters();
        List<ContextStoreQuerySort> sorts = query.sort();

        // Track filters that should be skipped entirely (e.g. IN with empty collection).
        List<FilterBinding> filterBindings = new ArrayList<>(filters.size());

        for (int filterIndex = 0; filterIndex < filters.size(); filterIndex++) {
            ContextStoreQueryFilter filter = filters.get(filterIndex);

            FilterBinding binding = bindFilter(filter, filterIndex);

            if (binding == null) {
                // IN with empty collection — return empty result for the entire query.
                return new ContextStoreSearchResult(List.of(), null);
            }

            filterBindings.add(binding);

            sql.append(" JOIN context_store_record_index ")
                .append(binding.alias)
                .append(" ON ")
                .append(binding.alias)
                .append(".record_id = r.id AND ")
                .append(binding.alias)
                .append(".field_name = :")
                .append(binding.alias)
                .append("_field");

            params.addValue(binding.alias + "_field", filter.field());
        }

        // Add sort joins. Each sort field gets its own alias so we can ORDER BY the typed column.
        List<SortBinding> sortBindings = new ArrayList<>(sorts.size());

        for (int sortIndex = 0; sortIndex < sorts.size(); sortIndex++) {
            ContextStoreQuerySort sortEntry = sorts.get(sortIndex);

            String alias = "s" + sortIndex;

            // Default to value_text for sort; callers can override only by aligning fields with their declared types.
            // For MVP, we sort lexicographically unless the underlying index has a typed column populated. A future
            // enhancement could resolve the entity's indexed-fields metadata to pick the right typed column.
            sortBindings.add(new SortBinding(alias, sortEntry.dir(), COLUMN_VALUE_TEXT));

            sql.append(" LEFT JOIN context_store_record_index ")
                .append(alias)
                .append(" ON ")
                .append(alias)
                .append(".record_id = r.id AND ")
                .append(alias)
                .append(".field_name = :")
                .append(alias)
                .append("_field");

            params.addValue(alias + "_field", sortEntry.field());
        }

        sql.append(" WHERE r.source_id = :sourceId");
        params.addValue("sourceId", query.sourceId());

        if (!query.includeDeleted()) {
            sql.append(" AND r.deleted_at IS NULL");
        }

        for (FilterBinding binding : filterBindings) {
            sql.append(" AND ")
                .append(binding.predicate);

            for (Map.Entry<String, Object> param : binding.params.entrySet()) {
                params.addValue(param.getKey(), param.getValue());
            }
        }

        // Cursor: simple lastId-based. WHERE r.id > :cursorId.
        Long cursorId = decodeCursor(query.cursor());

        if (cursorId != null) {
            sql.append(" AND r.id > :cursorId");
            params.addValue("cursorId", cursorId);
        }

        sql.append(" ORDER BY ");

        if (sortBindings.isEmpty()) {
            sql.append("r.id ASC");
        } else {
            for (int sortIndex = 0; sortIndex < sortBindings.size(); sortIndex++) {
                SortBinding sortBinding = sortBindings.get(sortIndex);

                if (sortIndex > 0) {
                    sql.append(", ");
                }

                sql.append(sortBinding.alias)
                    .append(".")
                    .append(sortBinding.column)
                    .append(" ")
                    .append(sortBinding.direction == ContextStoreQuerySort.SortDirection.DESC ? "DESC" : "ASC");
            }

            // Tie-breaker by id for stability inside the page (cursor still keys off r.id).
            sql.append(", r.id ASC");
        }

        // Read limit + 1 to detect whether a next page exists.
        int limit = query.limit();

        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<ContextStoreRecord> rows = jdbcOperations.query(sql.toString(), params, rowMapper);

        String nextCursor = null;

        if (rows.size() > limit) {
            ContextStoreRecord lastReturned = rows.get(limit - 1);
            nextCursor = encodeCursor(lastReturned.getId());
            rows = rows.subList(0, limit);
        }

        return new ContextStoreSearchResult(rows, nextCursor);
    }

    @Override
    public Optional<ContextStoreRecord> get(Long sourceId, String sourceRecordId) {
        return contextStoreRecordRepository.findBySourceIdAndSourceRecordId(sourceId, sourceRecordId);
    }

    @Override
    public List<Long> searchRecordIds(long sourceId, ContextStoreQueryFilter filter) {
        Objects.requireNonNull(filter, "filter");

        FilterBinding binding = bindFilter(filter, 0);

        if (binding == null) {
            // IN with empty collection — no candidate records.
            return List.of();
        }

        StringBuilder sql = new StringBuilder("SELECT r.id FROM context_store_record r");

        sql.append(" JOIN context_store_record_index ")
            .append(binding.alias)
            .append(" ON ")
            .append(binding.alias)
            .append(".record_id = r.id AND ")
            .append(binding.alias)
            .append(".field_name = :")
            .append(binding.alias)
            .append("_field");

        sql.append(" WHERE r.source_id = :sourceId")
            .append(" AND r.deleted_at IS NULL")
            .append(" AND ")
            .append(binding.predicate);

        // Cap the projected set to keep the downstream IN clause bounded.
        sql.append(" ORDER BY r.id ASC LIMIT :limit");

        MapSqlParameterSource params = new MapSqlParameterSource();

        params.addValue("sourceId", sourceId);
        params.addValue(binding.alias + "_field", filter.field());
        params.addValue("limit", MAX_HYBRID_CANDIDATES + 1);

        for (Map.Entry<String, Object> param : binding.params.entrySet()) {
            params.addValue(param.getKey(), param.getValue());
        }

        List<Long> ids = jdbcOperations.queryForList(sql.toString(), params, Long.class);

        if (ids.size() > MAX_HYBRID_CANDIDATES) {
            log.warn(
                "Context Store hybrid candidate set truncated: source={} matched>{} (cap)",
                sourceId, MAX_HYBRID_CANDIDATES);

            ids = ids.subList(0, MAX_HYBRID_CANDIDATES);
        }

        return ids;
    }

    @Nullable
    private FilterBinding bindFilter(ContextStoreQueryFilter filter, int filterIndex) {
        String alias = "i" + filterIndex;

        Map<String, Object> params = new HashMap<>();
        String predicate;

        switch (filter.op()) {
            case EQ -> {
                predicate = alias + "." + COLUMN_VALUE_TEXT + " = :" + alias + "_val";
                params.put(alias + "_val", stringValue(filter.value()));
            }
            case NEQ -> {
                predicate = alias + "." + COLUMN_VALUE_TEXT + " <> :" + alias + "_val";
                params.put(alias + "_val", stringValue(filter.value()));
            }
            case IN -> {
                if (!(filter.value() instanceof Collection<?> collection)) {
                    throw new IllegalArgumentException(
                        "IN filter value must be a Collection, got: " +
                            (filter.value() == null ? "null" : filter.value()
                                .getClass()
                                .getName()));
                }

                if (collection.isEmpty()) {
                    // Postgres rejects empty IN (). Surface zero-result short-circuit at the call site.
                    return null;
                }

                List<String> stringValues = new ArrayList<>(collection.size());

                for (Object element : collection) {
                    stringValues.add(stringValue(element));
                }

                predicate = alias + "." + COLUMN_VALUE_TEXT + " IN (:" + alias + "_vals)";
                params.put(alias + "_vals", stringValues);
            }
            case CONTAINS -> {
                predicate = alias + "." + COLUMN_VALUE_TEXT + " ILIKE :" + alias + "_val";
                params.put(alias + "_val", "%" + stringValue(filter.value()) + "%");
            }
            case STARTS_WITH -> {
                predicate = alias + "." + COLUMN_VALUE_TEXT + " ILIKE :" + alias + "_val";
                params.put(alias + "_val", stringValue(filter.value()) + "%");
            }
            case GT -> {
                String column = inferTypedColumn(filter.value());
                predicate = alias + "." + column + " > :" + alias + "_val";
                params.put(alias + "_val", typedValue(filter.value(), column));
            }
            case GTE -> {
                String column = inferTypedColumn(filter.value());
                predicate = alias + "." + column + " >= :" + alias + "_val";
                params.put(alias + "_val", typedValue(filter.value(), column));
            }
            case LT -> {
                String column = inferTypedColumn(filter.value());
                predicate = alias + "." + column + " < :" + alias + "_val";
                params.put(alias + "_val", typedValue(filter.value(), column));
            }
            case LTE -> {
                String column = inferTypedColumn(filter.value());
                predicate = alias + "." + column + " <= :" + alias + "_val";
                params.put(alias + "_val", typedValue(filter.value(), column));
            }
            case BETWEEN -> {
                List<?> bounds = asPair(filter.value());
                Object low = bounds.get(0);
                Object high = bounds.get(1);
                String column = inferTypedColumn(low);
                predicate = alias + "." + column + " BETWEEN :" + alias + "_lo AND :" + alias + "_hi";
                params.put(alias + "_lo", typedValue(low, column));
                params.put(alias + "_hi", typedValue(high, column));
            }
            default -> throw new IllegalArgumentException("Unsupported filter op: " + filter.op());
        }

        return new FilterBinding(alias, predicate, params);
    }

    private static String stringValue(@Nullable Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Filter value must not be null");
        }

        return value.toString();
    }

    /**
     * Infers the typed column from the runtime type of the filter value.
     *
     * <ul>
     * <li>{@link Number} → {@code value_numeric}</li>
     * <li>{@link Instant} or {@link Temporal} → {@code value_timestamp}</li>
     * <li>ISO-8601 parseable {@link String} → {@code value_timestamp}</li>
     * <li>Other {@link String} → {@code value_text} (lexicographic compare)</li>
     * </ul>
     */
    private static String inferTypedColumn(@Nullable Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Comparison filter value must not be null");
        }

        if (value instanceof Number) {
            return COLUMN_VALUE_NUMERIC;
        }

        if (value instanceof Instant || value instanceof Temporal) {
            return COLUMN_VALUE_TIMESTAMP;
        }

        if (value instanceof String stringValue) {
            try {
                Instant.parse(stringValue);

                return COLUMN_VALUE_TIMESTAMP;
            } catch (DateTimeParseException ignored) {
                return COLUMN_VALUE_TEXT;
            }
        }

        return COLUMN_VALUE_TEXT;
    }

    private static Object typedValue(@Nullable Object value, String column) {
        if (value == null) {
            throw new IllegalArgumentException("Filter value must not be null");
        }

        return switch (column) {
            case COLUMN_VALUE_NUMERIC -> {
                if (value instanceof BigDecimal bigDecimal) {
                    yield bigDecimal;
                }

                if (value instanceof Number number) {
                    yield new BigDecimal(number.toString());
                }

                yield new BigDecimal(value.toString());
            }
            case COLUMN_VALUE_TIMESTAMP -> {
                if (value instanceof Instant instant) {
                    yield Timestamp.from(instant);
                }

                if (value instanceof String stringValue) {
                    yield Timestamp.from(Instant.parse(stringValue));
                }

                throw new IllegalArgumentException(
                    "Cannot bind value of type " + value.getClass()
                        .getName() + " to a timestamp column");
            }
            default -> value.toString();
        };
    }

    private static List<?> asPair(@Nullable Object value) {
        if (value instanceof List<?> list) {
            if (list.size() != 2) {
                throw new IllegalArgumentException(
                    "BETWEEN value must have exactly 2 elements, got: " + list.size());
            }

            return list;
        }

        if (value instanceof Object[] array) {
            if (array.length != 2) {
                throw new IllegalArgumentException(
                    "BETWEEN value must have exactly 2 elements, got: " + array.length);
            }

            return List.of(array[0], array[1]);
        }

        throw new IllegalArgumentException(
            "BETWEEN value must be a 2-element List or Object[], got: " +
                (value == null ? "null" : value.getClass()
                    .getName()));
    }

    @Nullable
    private static Long decodeCursor(@Nullable String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            byte[] decoded = Base64.getUrlDecoder()
                .decode(cursor);
            String idString = new String(decoded, StandardCharsets.UTF_8);

            return Long.parseLong(idString);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid cursor: " + cursor, exception);
        }
    }

    private static String encodeCursor(Long lastId) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(String.valueOf(lastId)
                .getBytes(StandardCharsets.UTF_8));
    }

    private record FilterBinding(String alias, String predicate, Map<String, Object> params) {
    }

    private record SortBinding(String alias, ContextStoreQuerySort.SortDirection direction, String column) {
    }

    private final class ContextStoreRecordRowMapper implements RowMapper<ContextStoreRecord> {

        @Override
        public ContextStoreRecord mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            ContextStoreRecord record = new ContextStoreRecord();

            record.setId(resultSet.getLong("id"));
            record.setSourceId(resultSet.getLong("source_id"));
            record.setSourceRecordId(resultSet.getString("source_record_id"));

            String payloadJson = resultSet.getString("payload");

            if (payloadJson != null) {
                Map<String, Object> payloadMap = objectMapper.readValue(payloadJson, new TypeReference<>() {});

                // The MapWrapper field is private and the setter takes Map<String, ?>; we go through the public
                // setter, which is the canonical write path.
                record.setPayload(payloadMap);
            } else {
                record.setPayload(Map.of());
            }

            record.setPayloadHash(resultSet.getString("payload_hash"));

            Timestamp lastSeenAt = resultSet.getTimestamp("last_seen_at");

            if (lastSeenAt != null) {
                record.setLastSeenAt(lastSeenAt.toInstant());
            }

            Timestamp deletedAt = resultSet.getTimestamp("deleted_at");

            if (deletedAt != null) {
                record.setDeletedAt(deletedAt.toInstant());
            }

            // createdDate / lastModifiedDate are private fields without setters; reflectively inject them so the
            // domain object faithfully reflects the row even though Spring Data JDBC normally manages those columns.
            Timestamp createdDate = resultSet.getTimestamp("created_date");

            if (createdDate != null) {
                writePrivateInstantField(record, "createdDate", createdDate.toInstant());
            }

            Timestamp lastModifiedDate = resultSet.getTimestamp("last_modified_date");

            if (lastModifiedDate != null) {
                writePrivateInstantField(record, "lastModifiedDate", lastModifiedDate.toInstant());
            }

            return record;
        }

        private void writePrivateInstantField(ContextStoreRecord target, String fieldName, Instant value) {
            try {
                Field field = ContextStoreRecord.class.getDeclaredField(fieldName);

                field.setAccessible(true);
                field.set(target, value);
            } catch (NoSuchFieldException | IllegalAccessException exception) {
                throw new IllegalStateException(
                    "Failed to populate " + fieldName + " on ContextStoreRecord row", exception);
            }
        }
    }

}
