/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationInstanceConfigurationServiceIntTest {

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    void afterEach() {
        jdbcClient.sql("DELETE FROM mcp_integration_instance_configuration")
            .update();
        jdbcClient.sql("DELETE FROM mcp_server")
            .update();
        jdbcClient.sql("DELETE FROM integration_instance_configuration_tag")
            .update();
        jdbcClient.sql("DELETE FROM integration_instance_configuration")
            .update();
        jdbcClient.sql("DELETE FROM integration_version")
            .update();
        jdbcClient.sql("DELETE FROM integration")
            .update();
        jdbcClient.sql("DELETE FROM tag")
            .update();
    }

    @Test
    void testGetIntegrationInstanceConfigurationsIncludesConfigurationsLinkedToAnMcpServer() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration");

        integration = integrationService.create(integration);

        long integrationId = Objects.requireNonNull(integration.getId());

        IntegrationInstanceConfiguration plainConfiguration = createIntegrationInstanceConfiguration(
            integrationId, "plain");
        IntegrationInstanceConfiguration linkedConfiguration = createIntegrationInstanceConfiguration(
            integrationId, "linked");

        linkToMcpServer(Objects.requireNonNull(linkedConfiguration.getId()));

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(null, null, null);

        assertThat(integrationInstanceConfigurations)
            .extracting(IntegrationInstanceConfiguration::getId)
            .containsExactlyInAnyOrder(plainConfiguration.getId(), linkedConfiguration.getId());
    }

    @Test
    void testGetIntegrationInstanceConfigurationsFiltersByEnvironmentIntegrationIdAndTagId() {
        long integrationId = createIntegration("componentName");
        long otherIntegrationId = createIntegration("otherComponentName");
        long tagId = createTag("tag");
        long otherTagId = createTag("otherTag");

        IntegrationInstanceConfiguration matchingConfiguration = createIntegrationInstanceConfiguration(
            integrationId, "matching", Environment.PRODUCTION, List.of(tagId));

        createIntegrationInstanceConfiguration(
            integrationId, "otherEnvironment", Environment.STAGING, List.of(tagId));
        createIntegrationInstanceConfiguration(
            otherIntegrationId, "otherIntegration", Environment.PRODUCTION, List.of(tagId));
        createIntegrationInstanceConfiguration(
            integrationId, "otherTag", Environment.PRODUCTION, List.of(otherTagId));

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                Environment.PRODUCTION, integrationId, tagId);

        assertThat(integrationInstanceConfigurations)
            .extracting(IntegrationInstanceConfiguration::getId)
            .containsExactly(matchingConfiguration.getId());
    }

    @Test
    void testGetIntegrationInstanceConfigurationsFiltersByEnvironment() {
        long integrationId = createIntegration("componentName");

        IntegrationInstanceConfiguration productionConfiguration = createIntegrationInstanceConfiguration(
            integrationId, "production", Environment.PRODUCTION, List.of());

        createIntegrationInstanceConfiguration(integrationId, "staging", Environment.STAGING, List.of());

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                Environment.PRODUCTION, null, null);

        assertThat(integrationInstanceConfigurations)
            .extracting(IntegrationInstanceConfiguration::getId)
            .containsExactly(productionConfiguration.getId());
    }

    @Test
    void testGetIntegrationInstanceConfigurationsFiltersByIntegrationId() {
        long integrationId = createIntegration("componentName");
        long otherIntegrationId = createIntegration("otherComponentName");

        IntegrationInstanceConfiguration productionConfiguration = createIntegrationInstanceConfiguration(
            integrationId, "production", Environment.PRODUCTION, List.of());
        IntegrationInstanceConfiguration stagingConfiguration = createIntegrationInstanceConfiguration(
            integrationId, "staging", Environment.STAGING, List.of());

        createIntegrationInstanceConfiguration(
            otherIntegrationId, "otherIntegration", Environment.PRODUCTION, List.of());

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(null, integrationId, null);

        assertThat(integrationInstanceConfigurations)
            .extracting(IntegrationInstanceConfiguration::getId)
            .containsExactlyInAnyOrder(productionConfiguration.getId(), stagingConfiguration.getId());
    }

    @Test
    void testGetIntegrationInstanceConfigurationsFiltersByTagId() {
        long integrationId = createIntegration("componentName");
        long otherIntegrationId = createIntegration("otherComponentName");
        long tagId = createTag("tag");
        long otherTagId = createTag("otherTag");

        IntegrationInstanceConfiguration taggedConfiguration = createIntegrationInstanceConfiguration(
            integrationId, "tagged", Environment.PRODUCTION, List.of(tagId));
        IntegrationInstanceConfiguration otherIntegrationTaggedConfiguration = createIntegrationInstanceConfiguration(
            otherIntegrationId, "otherIntegrationTagged", Environment.STAGING, List.of(tagId, otherTagId));

        createIntegrationInstanceConfiguration(integrationId, "otherTag", Environment.PRODUCTION, List.of(otherTagId));
        createIntegrationInstanceConfiguration(integrationId, "untagged", Environment.PRODUCTION, List.of());

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(null, null, tagId);

        assertThat(integrationInstanceConfigurations)
            .extracting(IntegrationInstanceConfiguration::getId)
            .containsExactlyInAnyOrder(taggedConfiguration.getId(), otherIntegrationTaggedConfiguration.getId());
    }

    private long createIntegration(String componentName) {
        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setName(componentName);

        integration = integrationService.create(integration);

        return Objects.requireNonNull(integration.getId());
    }

    private IntegrationInstanceConfiguration createIntegrationInstanceConfiguration(long integrationId, String name) {
        return createIntegrationInstanceConfiguration(integrationId, name, Environment.PRODUCTION, List.of());
    }

    private IntegrationInstanceConfiguration createIntegrationInstanceConfiguration(
        long integrationId, String name, Environment environment, List<Long> tagIds) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName(name);
        integrationInstanceConfiguration.setTagIds(tagIds);

        return integrationInstanceConfigurationService.create(integrationInstanceConfiguration);
    }

    private long createTag(String name) {
        Tag tag = tagRepository.save(new Tag(name));

        return Objects.requireNonNull(tag.getId());
    }

    private void linkToMcpServer(long integrationInstanceConfigurationId) {
        long mcpServerId = Objects.requireNonNull(
            jdbcClient
                .sql(
                    """
                        INSERT INTO mcp_server
                            (name, type, environment, enabled, secret_key, created_date, created_by,
                             last_modified_date, last_modified_by, version)
                        VALUES ('mcp-server', 1, 2, true, 'secret', now(), 'test', now(), 'test', 0)
                        RETURNING id
                        """)
                .query(Long.class)
                .single(),
            "mcp server id");

        jdbcClient
            .sql(
                """
                    INSERT INTO mcp_integration_instance_configuration
                        (mcp_server_id, integration_instance_configuration_id, created_date, created_by,
                         last_modified_date, last_modified_by, version)
                    VALUES (?, ?, now(), 'test', now(), 'test', 0)
                    """)
            .params(mcpServerId, integrationInstanceConfigurationId)
            .update();
    }
}
