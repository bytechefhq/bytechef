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
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = {
        "bytechef.edition=EE", "spring.liquibase.contexts=configuration"
    })
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationInstanceConfigurationServiceIntTest {

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @AfterEach
    void afterEach() {
        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testGetIntegrationIntegrationInstanceConfigurationPicksTheHighestEnabledVersion() {
        long integrationId = createIntegration();

        createEnabledConfiguration(integrationId, 1);

        long version3Id = createEnabledConfiguration(integrationId, 3);

        createEnabledConfiguration(integrationId, 2);
        createDisabledConfiguration(integrationId, 5);

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationIntegrationInstanceConfiguration(
                integrationId, Environment.PRODUCTION, true);

        assertThat(integrationInstanceConfiguration.getId()).isEqualTo(version3Id);
    }

    @Test
    void testSelfServeConfigurationTieBreaksOnHighestId() {
        long integrationId = createIntegration();

        createEnabledConfiguration(integrationId, 2);

        long laterId = createEnabledConfiguration(integrationId, 2);

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationIntegrationInstanceConfiguration(
                integrationId, Environment.PRODUCTION, true);

        assertThat(integrationInstanceConfiguration.getId()).isEqualTo(laterId);
    }

    private long createDisabledConfiguration(long integrationId, int integrationVersion) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(integrationVersion);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        return integrationInstanceConfiguration.getId();
    }

    private long createEnabledConfiguration(long integrationId, int integrationVersion) {
        long integrationInstanceConfigurationId = createDisabledConfiguration(integrationId, integrationVersion);

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfigurationId, true);

        return integrationInstanceConfigurationId;
    }

    private long createIntegration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return integration.getId();
    }
}
