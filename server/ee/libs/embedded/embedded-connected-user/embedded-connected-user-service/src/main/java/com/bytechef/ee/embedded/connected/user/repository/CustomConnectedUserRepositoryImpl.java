/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.repository;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public class CustomConnectedUserRepositoryImpl implements CustomConnectedUserRepository {

    private final JdbcClient jdbcClient;

    @SuppressFBWarnings("EI")
    public CustomConnectedUserRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Page<ConnectedUser> findAll(
        Integer environment, String search, LocalDate createDateFrom, LocalDate createDateTo, Long integrationId,
        Pageable pageable) {

        Page<ConnectedUser> page;
        JdbcClient.StatementSpec statementSpec = buildQuery(
            environment, search, createDateFrom, createDateTo, integrationId, pageable, true);

        long total = statementSpec.query(Long.class)
            .single();

        if (total == 0) {
            page = Page.empty();
        } else {
            statementSpec =
                buildQuery(environment, search, createDateFrom, createDateTo, integrationId, pageable, false);

            List<ConnectedUser> connectedUsers = statementSpec
                .query(ConnectedUser.class)
                .list();

            for (ConnectedUser connectedUser : connectedUsers) {
                connectedUser.setMetadata(
                    jdbcClient
                        .sql(
                            "SELECT connected_user_metadata.key, connected_user_metadata.value FROM connected_user_metadata WHERE connected_user_id = ?")
                        .param(connectedUser.getId())
                        .query(ConnectedUserMetadata.class)
                        .list()
                        .stream()
                        .collect(
                            Collectors.toMap(
                                connectedUserMetadata -> connectedUserMetadata.key,
                                connectedUserMetadata -> connectedUserMetadata.value)));
            }

            page = new PageImpl<>(connectedUsers, pageable, total);
        }

        return page;
    }

    private JdbcClient.StatementSpec buildQuery(
        Integer environment, String search, LocalDate createDateFrom, LocalDate createDateTo, Long integrationId,
        Pageable pageable,
        boolean countQuery) {

        String query;

        if (countQuery) {
            query = "SELECT COUNT(connected_user.id) FROM connected_user ";
        } else {
            query = "SELECT connected_user.* FROM connected_user ";
        }

        List<String> conditions = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();

        if (environment != null) {
            conditions.add("connected_user.environment = ?");

            arguments.add(environment);
        }

        if (search != null) {
            conditions.add(
                "(LOWER(connected_user.name) LIKE ? OR LOWER(connected_user.email) LIKE ? OR " +
                    "LOWER(connected_user.external_id) LIKE ?)");

            String searchPattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

            arguments.addAll(List.of(searchPattern, searchPattern, searchPattern));
        }

        if (createDateFrom != null) {
            conditions.add("connected_user.created_date >= ?");

            arguments.add(createDateFrom);
        }

        if (createDateTo != null) {
            conditions.add("connected_user.created_date <= ?");

            arguments.add(createDateTo);
        }

        if (integrationId != null) {
            conditions.add(
                """
                    connected_user.id IN (
                        SELECT integration_instance.connected_user_id FROM integration_instance
                        JOIN integration_instance_configuration ON integration_instance_configuration.id = integration_instance.integration_instance_configuration_id
                        WHERE integration_instance_configuration.integration_id = ?)""");

            arguments.add(integrationId);
        }

        if (!conditions.isEmpty()) {
            query += "WHERE " + String.join(" AND ", conditions) + " ";
        }

        if (!countQuery && pageable != null) {
            query +=
                "ORDER BY connected_user.id LIMIT %s OFFSET %s".formatted(pageable.getPageSize(), pageable.getOffset());
        }

        return jdbcClient
            .sql(query)
            .params(arguments);
    }

    private record ConnectedUserMetadata(String key, String value) {
    }
}
