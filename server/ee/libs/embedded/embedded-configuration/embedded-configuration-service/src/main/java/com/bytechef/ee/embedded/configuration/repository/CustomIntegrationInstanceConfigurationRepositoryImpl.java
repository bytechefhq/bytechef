/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.wrapper.EncryptedMapWrapper;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CustomIntegrationInstanceConfigurationRepositoryImpl
    implements CustomIntegrationInstanceConfigurationRepository {

    private final EncryptedStringToMapWrapperConverter encryptedStringToMapWrapperConverter;
    private final JdbcClient jdbcClient;

    @SuppressFBWarnings("EI")
    public CustomIntegrationInstanceConfigurationRepositoryImpl(
        Encryption encryption, JdbcClient jdbcClient, ObjectMapper objectMapper) {

        this.encryptedStringToMapWrapperConverter = new EncryptedStringToMapWrapperConverter(encryption, objectMapper);
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

        query += "ORDER BY integration.component_name ASC, integration_instance_configuration.integration_version " +
            "ASC, integration_instance_configuration.environment ASC";

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations = jdbcClient.sql(query)
            .params(arguments)
            .query(
                (rs, rowNum) -> {
                    IntegrationInstanceConfiguration integrationInstanceConfiguration =
                        new IntegrationInstanceConfiguration();

                    EncryptedMapWrapper encryptedMapWrapper = encryptedStringToMapWrapperConverter.convert(
                        rs.getString("connection_parameters"));

                    integrationInstanceConfiguration.setConnectionParameters(encryptedMapWrapper.getMap());
                    integrationInstanceConfiguration.setDescription(rs.getString("description"));
                    integrationInstanceConfiguration.setEnabled(rs.getBoolean("enabled"));
                    integrationInstanceConfiguration.setEnvironment(Environment.values()[rs.getInt("environment")]);
                    integrationInstanceConfiguration.setId(rs.getLong("id"));
                    integrationInstanceConfiguration.setIntegrationId(rs.getLong("integration_id"));
                    integrationInstanceConfiguration.setIntegrationVersion(rs.getInt("integration_version"));
                    integrationInstanceConfiguration.setName(rs.getString("name"));
                    integrationInstanceConfiguration.setVersion(rs.getInt("version"));

                    return integrationInstanceConfiguration;
                })
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
