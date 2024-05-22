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

import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @author Ivica Cardic
 */
public class CustomIntegrationInstanceConfigurationRepositoryImpl
    implements CustomIntegrationInstanceConfigurationRepository {

    private final JdbcClient jdbcClient;

    @SuppressFBWarnings("EI")
    public CustomIntegrationInstanceConfigurationRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<IntegrationInstanceConfiguration> findAllIntegrationInstanceConfigurations(
        Integer environment, Long integrationId, Long tagId) {

        List<Object> arguments = new ArrayList<>();
        String query =
            """
                    SELECT integration_instance_configuration.*, integration.component_name FROM integration_instance_configuration
                    JOIN integration on integration_instance_configuration.integration_id = integration.id
                """;

        if (tagId != null) {
            query += "JOIN integration_instance_configuration_tag " +
                "ON integration_instance_configuration.id = " +
                "integration_instance_configuration_tag.integration_instance_configuration_id ";
        }

        if (environment != null || integrationId != null || tagId != null) {
            query += "WHERE ";
        }

        if (environment != null) {
            arguments.add(environment);

            query += "environment = ? ";
        }

        if (integrationId != null) {
            arguments.add(integrationId);

            if (environment != null) {
                query += "AND ";
            }

            query += "integration_id = ? ";
        }

        if (tagId != null) {
            arguments.add(tagId);

            if (environment != null || integrationId != null) {
                query += "AND ";
            }

            query += "tag_id = ? ";
        }

        query += "ORDER BY integration.component_name ASC, integration_instance_configuration.enabled DESC";

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations = jdbcClient.sql(query)
            .params(arguments)
            .query(IntegrationInstanceConfiguration.class)
            .list();

        for (IntegrationInstanceConfiguration integrationInstanceConfiguration : integrationInstanceConfigurations) {
            integrationInstanceConfiguration.setTagIds(
                jdbcClient
                    .sql(
                        "SELECT integration_instance_configuration_tag.tag_id " +
                            "FROM integration_instance_configuration_tag " +
                            "WHERE integration_instance_configuration_id = ?")
                    .param(integrationInstanceConfiguration.getId())
                    .query(Long.class)
                    .list());
        }

        return integrationInstanceConfigurations;
    }
}
