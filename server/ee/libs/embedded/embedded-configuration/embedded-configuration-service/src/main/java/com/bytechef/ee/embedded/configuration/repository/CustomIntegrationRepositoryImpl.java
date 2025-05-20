/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CustomIntegrationRepositoryImpl implements CustomIntegrationRepository {

    private final JdbcClient jdbcClient;

    public CustomIntegrationRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Integration> findAllIntegrations(Long categoryId, List<Long> ids, Long tagId, Integer status) {
        List<Object> arguments = new ArrayList<>();
        String query = "SELECT DISTINCT integration.* FROM integration ";

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

        List<Integration> integrations = jdbcClient.sql(query)
            .params(arguments)
            .query(Integration.class)
            .list();

        for (Integration integration : integrations) {
            integration.setTagIds(
                jdbcClient
                    .sql(
                        "SELECT integration_tag.tag_id FROM integration_tag WHERE integration_id = ?")
                    .param(integration.getId())
                    .query(Long.class)
                    .list());

            integration.setIntegrationVersions(
                jdbcClient
                    .sql(
                        "SELECT integration_version.version, integration_version.status, integration_version.published_date, integration_version.description FROM integration_version WHERE integration_id = ?")
                    .param(integration.getId())
                    .query(IntegrationVersion.class)
                    .list()
                    .stream()
                    .map(integrationVersion -> new com.bytechef.ee.embedded.configuration.domain.IntegrationVersion(
                        integrationVersion.version, integrationVersion.status, integrationVersion.publishedDate,
                        integrationVersion.description))
                    .toList());
        }

        return integrations;
    }

    private record IntegrationVersion(int version, int status, Instant publishedDate, String description) {
    }

    private record IntegrationWorkflow(String workflow_id, int integration_version) {
    }
}
