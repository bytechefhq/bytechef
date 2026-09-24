/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Custom repository implementation for paged, filtered audit event reads, distinct event types and the retention purge.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CustomPersistenceAuditEventRepositoryImpl implements CustomPersistenceAuditEventRepository {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public CustomPersistenceAuditEventRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC",
        justification = "Query is safely built using parameterized placeholders; all user input passed via arguments array")
    public Page<PersistentAuditEvent> findAllFiltered(AuditEventFilter auditEventFilter, Pageable pageable) {

        List<Object> arguments = new ArrayList<>();
        String whereClause = buildWhereClause(auditEventFilter, arguments);

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM persistent_audit_event e" + whereClause, Long.class, arguments.toArray());

        if (total == null || total == 0) {
            return Page.empty(pageable);
        }

        // LIMIT/OFFSET are applied to the events alone, inside the derived table, and the data rows are joined
        // afterwards. Paging the joined rows instead would count every data entry as a row, returning short pages and
        // truncating the data of the event that straddles the page boundary. The id tiebreaker keeps the order total
        // so events sharing an event_date cannot repeat or go missing between pages.
        String dataQuery = "SELECT e.id, e.principal, e.event_date, e.event_type, d.key, d.value"
            + " FROM (SELECT e.id, e.principal, e.event_date, e.event_type FROM persistent_audit_event e"
            + whereClause
            + " ORDER BY e.event_date DESC, e.id DESC"
            + " LIMIT ? OFFSET ?) e"
            + " LEFT JOIN persistent_audit_event_data d ON e.id = d.persistent_audit_event_id"
            + " ORDER BY e.event_date DESC, e.id DESC";

        List<Object> pageArguments = new ArrayList<>(arguments);

        pageArguments.add(pageable.getPageSize());
        pageArguments.add(pageable.getOffset());

        Map<Long, PersistentAuditEvent> eventMap = new LinkedHashMap<>();
        Map<Long, Map<String, String>> eventDataMap = new HashMap<>();

        jdbcTemplate.query(dataQuery, (ResultSet resultSet) -> {
            long id = resultSet.getLong("id");

            if (!eventMap.containsKey(id)) {
                eventMap.put(id, mapPersistentAuditEvent(id, resultSet));
                eventDataMap.put(id, new HashMap<>());
            }

            String key = resultSet.getString("key");

            if (key != null) {
                Map<String, String> data = eventDataMap.get(id);

                data.put(key, resultSet.getString("value"));
            }
        }, pageArguments.toArray());

        for (Map.Entry<Long, PersistentAuditEvent> entry : eventMap.entrySet()) {
            PersistentAuditEvent persistentAuditEvent = entry.getValue();

            persistentAuditEvent.setData(eventDataMap.get(entry.getKey()));
        }

        return new PageImpl<>(new ArrayList<>(eventMap.values()), pageable, total);
    }

    @Override
    public int deleteByEventDateBefore(LocalDateTime cutoff) {
        jdbcTemplate.update(
            "DELETE FROM persistent_audit_event_data WHERE persistent_audit_event_id IN "
                + "(SELECT id FROM persistent_audit_event WHERE event_date < ?)",
            cutoff);

        return jdbcTemplate.update("DELETE FROM persistent_audit_event WHERE event_date < ?", cutoff);
    }

    @Override
    public List<String> findDistinctEventTypes() {
        return jdbcTemplate.queryForList(
            "SELECT DISTINCT event_type FROM persistent_audit_event ORDER BY event_type", String.class);
    }

    private String buildWhereClause(AuditEventFilter auditEventFilter, List<Object> arguments) {
        String principal = auditEventFilter.principal();
        String eventType = auditEventFilter.eventType();
        LocalDateTime fromDate = auditEventFilter.fromDate();
        LocalDateTime toDate = auditEventFilter.toDate();
        String dataSearch = auditEventFilter.dataSearch();

        StringBuilder whereClause = new StringBuilder();

        if (principal != null) {
            whereClause.append(" WHERE e.principal = ?");

            arguments.add(principal);
        }

        if (eventType != null) {
            whereClause.append(whereClause.isEmpty() ? " WHERE" : " AND");
            whereClause.append(" e.event_type = ?");

            arguments.add(eventType);
        }

        if (fromDate != null) {
            whereClause.append(whereClause.isEmpty() ? " WHERE" : " AND");
            whereClause.append(" e.event_date >= ?");

            arguments.add(Timestamp.valueOf(fromDate));
        }

        if (toDate != null) {
            whereClause.append(whereClause.isEmpty() ? " WHERE" : " AND");
            whereClause.append(" e.event_date <= ?");

            arguments.add(Timestamp.valueOf(toDate));
        }

        if (dataSearch != null) {
            whereClause.append(whereClause.isEmpty() ? " WHERE" : " AND");
            whereClause.append(
                " EXISTS ("
                    + "SELECT 1 FROM persistent_audit_event_data ped"
                    + " WHERE ped.persistent_audit_event_id = e.id"
                    + " AND LOWER(ped.value) LIKE LOWER(CONCAT('%', ?, '%')) ESCAPE '\\'"
                    + ")");

            arguments.add(escapeLikePattern(dataSearch));
        }

        return whereClause.toString();
    }

    private static String escapeLikePattern(String value) {
        return value.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

    private static PersistentAuditEvent mapPersistentAuditEvent(long id, ResultSet resultSet) throws SQLException {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setId(id);
        persistentAuditEvent.setPrincipal(resultSet.getString("principal"));

        Timestamp eventDate = resultSet.getTimestamp("event_date");

        if (eventDate != null) {
            persistentAuditEvent.setEventDate(eventDate.toLocalDateTime());
        }

        persistentAuditEvent.setEventType(resultSet.getString("event_type"));

        return persistentAuditEvent;
    }
}
