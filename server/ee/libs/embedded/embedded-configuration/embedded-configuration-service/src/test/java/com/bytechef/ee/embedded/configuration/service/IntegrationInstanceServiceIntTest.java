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
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
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
class IntegrationInstanceServiceIntTest {

    private static final long CONNECTION_ID = 42L;
    private static final long USER_A_ID = 1001L;
    private static final long USER_B_ID = 1002L;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceRepository integrationInstanceRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @AfterEach
    void afterEach() {
        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testGetIntegrationInstancesByWorkflowUuidReturnsOnlyInstancesWhoseVersionHasThatWorkflow() {
        long integrationId = createIntegration();

        long version1ConfigurationId = createConfiguration(integrationId, 1);
        long version2ConfigurationId = createConfiguration(integrationId, 2);

        String version1WorkflowUuid = addWorkflow(integrationId, 1, "workflow-v1");
        String version2WorkflowUuid = addWorkflow(integrationId, 2, "workflow-v2");

        long instanceAId = integrationInstanceService.create(USER_A_ID, CONNECTION_ID, version1ConfigurationId)
            .getId();

        integrationInstanceService.create(USER_B_ID, CONNECTION_ID, version2ConfigurationId);

        assertThat(
            integrationInstanceService.getIntegrationInstancesByWorkflowUuid(
                USER_A_ID, version1WorkflowUuid, Environment.PRODUCTION))
                    .extracting(IntegrationInstance::getId)
                    .containsExactly(instanceAId);
        assertThat(
            integrationInstanceService.getIntegrationInstancesByWorkflowUuid(
                USER_A_ID, version2WorkflowUuid, Environment.PRODUCTION)).isEmpty();
        assertThat(
            integrationInstanceService.getIntegrationInstancesByWorkflowUuid(
                USER_B_ID, version1WorkflowUuid, Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testGetIntegrationInstancesByWorkflowUuidOrdersByIdDescending() {
        long integrationId = createIntegration();

        long configurationId = createConfiguration(integrationId, 1);

        String workflowUuid = addWorkflow(integrationId, 1, "workflow-v1");

        long firstInstanceId = integrationInstanceService.create(USER_A_ID, CONNECTION_ID, configurationId)
            .getId();
        long secondInstanceId = integrationInstanceService.create(USER_A_ID, CONNECTION_ID, configurationId)
            .getId();

        assertThat(
            integrationInstanceService.getIntegrationInstancesByWorkflowUuid(
                USER_A_ID, workflowUuid, Environment.PRODUCTION))
                    .extracting(IntegrationInstance::getId)
                    .containsExactly(secondInstanceId, firstInstanceId);
    }

    @Test
    void testFetchIntegrationInstanceByComponentNamePrefersAnOlderEnabledInstanceOverANewerDisabledOne() {
        long integrationId = createIntegration();

        long configurationId = createConfiguration(integrationId, 1);

        long enabledInstanceId = integrationInstanceService.create(USER_A_ID, CONNECTION_ID, configurationId)
            .getId();
        long disabledInstanceId = integrationInstanceService.create(USER_A_ID, CONNECTION_ID, configurationId)
            .getId();

        integrationInstanceService.updateEnabled(disabledInstanceId, false);

        assertThat(
            integrationInstanceService.fetchIntegrationInstance(USER_A_ID, "componentName", Environment.PRODUCTION))
                .hasValueSatisfying(integrationInstance -> assertThat(integrationInstance.getId())
                    .isEqualTo(enabledInstanceId));
        assertThat(
            integrationInstanceService.getIntegrationInstance(
                USER_A_ID, List.of("componentName"), Environment.PRODUCTION)
                .getId())
                    .isEqualTo(enabledInstanceId);
    }

    @Test
    void testGetIntegrationInstancesByWorkflowUuidReturnsEmptyForAMalformedUuid() {
        assertThat(
            integrationInstanceService.getIntegrationInstancesByWorkflowUuid(
                USER_A_ID, "not-a-uuid", Environment.PRODUCTION)).isEmpty();
    }

    private String addWorkflow(long integrationId, int integrationVersion, String workflowId) {
        return integrationWorkflowService.addWorkflow(integrationId, integrationVersion, workflowId)
            .getUuidAsString();
    }

    private long createConfiguration(long integrationId, int integrationVersion) {
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

    private long createIntegration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return integration.getId();
    }
}
