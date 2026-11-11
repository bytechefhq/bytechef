/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CustomIntegrationRepositoryImpl implements CustomIntegrationRepository {

    private final JdbcAggregateTemplate jdbcAggregateTemplate;
    private final JdbcClient jdbcClient;

    @SuppressFBWarnings("EI")
    public CustomIntegrationRepositoryImpl(JdbcAggregateTemplate jdbcAggregateTemplate, JdbcClient jdbcClient) {
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Integration> findAllIntegrations(Long categoryId, List<Long> ids, Long tagId, Integer status) {
        List<Object> arguments = new ArrayList<>();
        String query = "SELECT DISTINCT integration.id, integration.name FROM integration ";

        if (status != null) {
            query += "JOIN integration_version ON integration.id = integration_version.integration_id ";
        }

        if (tagId != null) {
            query += "JOIN integration_tag ON integration.id = integration_tag.integration_id ";
        }

        if (categoryId != null || !ids.isEmpty() || tagId != null || status != null) {
            query += "WHERE ";
        }

        if (categoryId != null) {
            arguments.add(categoryId);

            query += "category_id = ? ";
        }

        if (!ids.isEmpty()) {
            arguments.addAll(ids);

            if (categoryId != null) {
                query += "AND ";
            }

            StringBuilder idsString = new StringBuilder();

            for (int i = 0; i < ids.size(); i++) {
                idsString.append("?");

                if (i < ids.size() - 1) {
                    idsString.append(",");
                }
            }

            query += "integration.id in (%s) ".formatted(idsString);
        }

        if (tagId != null) {
            arguments.add(tagId);

            if (categoryId != null || !ids.isEmpty()) {
                query += "AND ";
            }

            query += "tag_id = ? ";
        }

        if (status != null) {
            arguments.add(status);

            if (categoryId != null || !ids.isEmpty() || tagId != null) {
                query += "AND ";
            }

            query += "status = ? ";
        }

        query += "ORDER BY integration.name ASC";

        List<Long> integrationIds = jdbcClient.sql(query)
            .params(arguments)
            .query((resultSet, rowNumber) -> resultSet.getLong("id"))
            .list();

        if (integrationIds.isEmpty()) {
            return List.of();
        }

        List<Integration> integrations = jdbcAggregateTemplate.findAllById(integrationIds, Integration.class);

        Map<Long, Integration> integrationMap = integrations.stream()
            .collect(Collectors.toMap(Integration::getId, Function.identity()));

        return integrationIds.stream()
            .map(integrationMap::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private record IntegrationWorkflow(String workflow_id, int integration_version) {
    }
}
