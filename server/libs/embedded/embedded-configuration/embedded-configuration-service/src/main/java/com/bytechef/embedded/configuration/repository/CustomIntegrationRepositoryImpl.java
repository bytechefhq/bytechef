/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.configuration.repository;

import com.bytechef.embedded.configuration.domain.Integration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
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
        String query = "SELECT integration.* FROM integration ";

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

        query += "ORDER BY integration.component_name ASC";

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
                    .map(integrationVersion -> new com.bytechef.embedded.configuration.domain.IntegrationVersion(
                        integrationVersion.version, integrationVersion.status, integrationVersion.publishedDate,
                        integrationVersion.description))
                    .toList());
        }

        return integrations;
    }

    private record IntegrationVersion(int version, int status, LocalDateTime publishedDate, String description) {
    }

    private record IntegrationWorkflow(String workflow_id, int integration_version) {
    }
}
