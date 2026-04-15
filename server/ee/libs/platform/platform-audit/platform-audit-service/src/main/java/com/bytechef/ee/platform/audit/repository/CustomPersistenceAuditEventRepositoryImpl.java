/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

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
 * Custom repository implementation for audit event queries with pagination support.
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
    public Page<PersistentAuditEvent> findAllFiltered(
        String principal, String eventType, LocalDateTime fromDate, LocalDateTime toDate, String dataSearch,
        Pageable pageable) {

        List<Object> arguments = new ArrayList<>();
        String whereClause = buildWhereClause(principal, eventType, fromDate, toDate, dataSearch, arguments);

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM persistent_audit_event" + whereClause, Long.class, arguments.toArray());

        if (total == null || total == 0) {
            return Page.empty(pageable);
        }

        String dataQuery = "SELECT e.id, e.principal, e.event_date, e.event_type, d.key, d.value"
            + " FROM persistent_audit_event e"
            + " LEFT JOIN persistent_audit_event_data d ON e.id = d.persistent_audit_event_id"
            + whereClause
            + " ORDER BY e.event_date DESC"
            + " LIMIT ? OFFSET ?";

        List<Object> pageArguments = new ArrayList<>(arguments);

        pageArguments.add(pageable.getPageSize());
        pageArguments.add(pageable.getOffset());

        Map<Long, PersistentAuditEvent> eventMap = new LinkedHashMap<>();

        jdbcTemplate.query(dataQuery, (ResultSet resultSet) -> {
            long id = resultSet.getLong("id");

            PersistentAuditEvent event = eventMap.computeIfAbsent(id, eventId -> {
                PersistentAuditEvent newEvent = new PersistentAuditEvent();

                try {
                    newEvent.setId(eventId);
                    newEvent.setPrincipal(resultSet.getString("principal"));

                    Timestamp eventDate = resultSet.getTimestamp("event_date");

                    if (eventDate != null) {
                        newEvent.setEventDate(eventDate.toLocalDateTime());
                    }

                    newEvent.setEventType(resultSet.getString("event_type"));
                } catch (SQLException sqlException) {
                    throw new RuntimeException("Error mapping persistent audit event", sqlException);
                }

                newEvent.setData(Map.of());

                return newEvent;
            });

            String key = resultSet.getString("key");

            if (key != null) {
                Map<String, String> data = new HashMap<>(event.getData());

                data.put(key, resultSet.getString("value"));

                event.setData(data);
            }
        }, pageArguments.toArray());

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

    private String buildWhereClause(
        String principal, String eventType, LocalDateTime fromDate, LocalDateTime toDate, String dataSearch,
        List<Object> arguments) {

        StringBuilder whereClause = new StringBuilder();

        if (principal != null) {
            whereClause.append(" WHERE principal = ?");

            arguments.add(principal);
        }

        if (eventType != null) {
            whereClause.append(whereClause.isEmpty() ? " WHERE" : " AND");
            whereClause.append(" event_type = ?");

            arguments.add(eventType);
        }

        if (fromDate != null) {
            whereClause.append(whereClause.isEmpty() ? " WHERE" : " AND");
            whereClause.append(" event_date >= ?");

            arguments.add(Timestamp.valueOf(fromDate));
        }

        if (toDate != null) {
            whereClause.append(whereClause.isEmpty() ? " WHERE" : " AND");
            whereClause.append(" event_date <= ?");

            arguments.add(Timestamp.valueOf(toDate));
        }

        if (dataSearch != null) {
            whereClause.append(whereClause.isEmpty() ? " WHERE" : " AND");
            whereClause.append(
                " EXISTS ("
                    + "SELECT 1 FROM persistent_audit_event_data ped"
                    + " WHERE ped.persistent_audit_event_id = id"
                    + " AND LOWER(ped.value) LIKE LOWER(CONCAT('%', ?, '%'))"
                    + ")");

            arguments.add(dataSearch);
        }

        return whereClause.toString();
    }
}
