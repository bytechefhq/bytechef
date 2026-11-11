/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = {
        "bytechef.edition=EE", "spring.liquibase.contexts=configuration,user"
    })
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationInstanceServiceIntTest {

    private static final long CONNECTION_ID = 42L;
    private static final long USER_A_ID = 1001L;
    private static final long USER_B_ID = 1002L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        jdbcTemplate.update("DELETE FROM connected_user");

        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testFetchFirstRunnableIntegrationInstance() {
        long publishedIntegrationId = createIntegration();

        integrationService.publishIntegration(publishedIntegrationId, null);

        long unpublishedIntegrationId = createIntegration();

        long enabledConfigurationId = createConfiguration(publishedIntegrationId, 1, Environment.DEVELOPMENT, true);
        long disabledConfigurationId = createConfiguration(publishedIntegrationId, 1, Environment.DEVELOPMENT, false);
        long unpublishedConfigurationId = createConfiguration(
            unpublishedIntegrationId, 1, Environment.DEVELOPMENT, true);
        long stagingConfigurationId = createConfiguration(publishedIntegrationId, 1, Environment.STAGING, true);

        long disabledUserId = insertConnectedUser("disabled-user", Environment.DEVELOPMENT, false);
        long disabledInstanceUserId = insertConnectedUser("disabled-instance-user", Environment.DEVELOPMENT);
        long disabledConfigurationUserId = insertConnectedUser("disabled-configuration-user", Environment.DEVELOPMENT);
        long unpublishedUserId = insertConnectedUser("unpublished-user", Environment.DEVELOPMENT);
        long stagingUserId = insertConnectedUser("staging-user", Environment.STAGING);
        long mismatchedEnvironmentUserId = insertConnectedUser("mismatched-environment-user", Environment.STAGING);

        long runnableUserId = insertConnectedUser("runnable-user", Environment.DEVELOPMENT);

        long newerRunnableUserId = insertConnectedUser("newer-runnable-user", Environment.DEVELOPMENT);

        long disabledInstanceId = integrationInstanceService
            .create(disabledInstanceUserId, CONNECTION_ID, enabledConfigurationId)
            .getId();

        integrationInstanceService.updateEnabled(disabledInstanceId, false);

        integrationInstanceService.create(disabledUserId, CONNECTION_ID, enabledConfigurationId);
        integrationInstanceService.create(disabledConfigurationUserId, CONNECTION_ID, disabledConfigurationId);
        integrationInstanceService.create(unpublishedUserId, CONNECTION_ID, unpublishedConfigurationId);
        integrationInstanceService.create(stagingUserId, CONNECTION_ID, stagingConfigurationId);
        integrationInstanceService.create(mismatchedEnvironmentUserId, CONNECTION_ID, enabledConfigurationId);

        assertThat(integrationInstanceService.fetchFirstRunnableIntegrationInstance(Environment.DEVELOPMENT))
            .isEmpty();

        integrationInstanceService.create(newerRunnableUserId, CONNECTION_ID, enabledConfigurationId);
        integrationInstanceService.create(runnableUserId, CONNECTION_ID, enabledConfigurationId);

        assertThat(integrationInstanceService.fetchFirstRunnableIntegrationInstance(Environment.DEVELOPMENT))
            .hasValueSatisfying(integrationInstance -> {
                assertThat(integrationInstance.getConnectedUserId()).isEqualTo(runnableUserId);
                assertThat(integrationInstance.getIntegrationInstanceConfigurationId())
                    .isEqualTo(enabledConfigurationId);
            });
        assertThat(integrationInstanceService.fetchFirstRunnableIntegrationInstance(Environment.STAGING))
            .hasValueSatisfying(
                integrationInstance -> assertThat(integrationInstance.getConnectedUserId()).isEqualTo(stagingUserId));
        assertThat(integrationInstanceService.fetchFirstRunnableIntegrationInstance(Environment.PRODUCTION))
            .isEmpty();
    }

    @Test
    void testCreateRecordsProvisionedBy() {
        long configurationId = createConfiguration(createIntegration(), 1);

        IntegrationInstance provisionedInstance = integrationInstanceService.create(
            USER_A_ID, CONNECTION_ID, configurationId, "admin@localhost.com");
        IntegrationInstance selfActivatedInstance = integrationInstanceService.create(
            USER_B_ID, CONNECTION_ID, configurationId);

        assertThat(
            integrationInstanceService.getIntegrationInstance(provisionedInstance.getId())
                .getProvisionedBy()).isEqualTo("admin@localhost.com");
        assertThat(
            integrationInstanceService.getIntegrationInstance(selfActivatedInstance.getId())
                .getProvisionedBy()).isNull();
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
    void testGetIntegrationInstanceByComponentNamesMatchesComponentNamesCaseInsensitively() {
        long configurationId = createConfiguration(createIntegration("HubSpot"), 1);

        long integrationInstanceId = integrationInstanceService.create(USER_A_ID, CONNECTION_ID, configurationId)
            .getId();

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            USER_A_ID, List.of("HubSpot"), Environment.PRODUCTION);

        assertThat(integrationInstance.getId()).isEqualTo(integrationInstanceId);
    }

    @Test
    void testGetIntegrationInstanceByComponentNamesRejectsEmptyComponentNames() {
        integrationInstanceService.create(USER_A_ID, CONNECTION_ID, createConfiguration(createIntegration(), 1));

        assertThatThrownBy(
            () -> integrationInstanceService.getIntegrationInstance(USER_A_ID, List.of(), Environment.PRODUCTION))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testFetchIntegrationInstanceByIntegrationIdIgnoresANewerInstanceOfASiblingIntegration() {
        long integrationId = createIntegration();

        long ownInstanceId = integrationInstanceService
            .create(USER_A_ID, CONNECTION_ID, createConfiguration(integrationId, 1))
            .getId();

        long siblingInstanceId = integrationInstanceService
            .create(USER_A_ID, CONNECTION_ID, createConfiguration(createIntegration(), 1))
            .getId();

        assertThat(
            integrationInstanceService.fetchIntegrationInstance(USER_A_ID, "componentName", Environment.PRODUCTION))
                .hasValueSatisfying(integrationInstance -> assertThat(integrationInstance.getId())
                    .isEqualTo(siblingInstanceId));
        assertThat(
            integrationInstanceService.fetchIntegrationInstanceByIntegrationId(
                USER_A_ID, integrationId, Environment.PRODUCTION))
                    .hasValueSatisfying(integrationInstance -> assertThat(integrationInstance.getId())
                        .isEqualTo(ownInstanceId));
        assertThat(
            integrationInstanceService.fetchIntegrationInstanceByIntegrationId(
                USER_B_ID, integrationId, Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testFetchIntegrationInstanceByIntegrationIdMatchesOnlyTheRequestedEnvironment() {
        long integrationId = createIntegration();

        integrationInstanceService.create(USER_A_ID, CONNECTION_ID, createConfiguration(integrationId, 1));

        assertThat(
            integrationInstanceService.fetchIntegrationInstanceByIntegrationId(
                USER_A_ID, integrationId, Environment.DEVELOPMENT)).isEmpty();
    }

    @Test
    void testFetchIntegrationInstanceByIntegrationIdPrefersAnOlderEnabledInstanceOverANewerDisabledOne() {
        long integrationId = createIntegration();

        long configurationId = createConfiguration(integrationId, 1);

        long enabledInstanceId = integrationInstanceService.create(USER_A_ID, CONNECTION_ID, configurationId)
            .getId();
        long disabledInstanceId = integrationInstanceService.create(USER_A_ID, CONNECTION_ID, configurationId)
            .getId();

        integrationInstanceService.updateEnabled(disabledInstanceId, false);

        assertThat(
            integrationInstanceService.fetchIntegrationInstanceByIntegrationId(
                USER_A_ID, integrationId, Environment.PRODUCTION))
                    .hasValueSatisfying(integrationInstance -> assertThat(integrationInstance.getId())
                        .isEqualTo(enabledInstanceId));
    }

    @Test
    void testExistsByConnectionIdReportsWhetherAnIntegrationInstanceUsesTheConnection() {
        long configurationId = createConfiguration(createIntegration(), 1);

        integrationInstanceService.create(USER_A_ID, CONNECTION_ID, configurationId);

        assertThat(integrationInstanceService.existsByConnectionId(CONNECTION_ID)).isTrue();
        assertThat(integrationInstanceService.existsByConnectionId(CONNECTION_ID + 1)).isFalse();
    }

    @Test
    void testGetConnectionIdsInReturnsOnlyTheConnectionsAnIntegrationInstanceUses() {
        long configurationId = createConfiguration(createIntegration(), 1);

        integrationInstanceService.create(USER_A_ID, CONNECTION_ID, configurationId);

        assertThat(integrationInstanceService.getConnectionIdsIn(Set.of(CONNECTION_ID, CONNECTION_ID + 1)))
            .containsExactly(CONNECTION_ID);
        assertThat(integrationInstanceService.getConnectionIdsIn(Set.of())).isEmpty();
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
        return createConfiguration(integrationId, integrationVersion, Environment.PRODUCTION, false);
    }

    private long insertConnectedUser(String externalId, Environment environment) {
        return insertConnectedUser(externalId, environment, true);
    }

    private long insertConnectedUser(String externalId, Environment environment, boolean enabled) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO connected_user
                        (external_id, enabled, environment, created_date, created_by, last_modified_date,
                         last_modified_by, version)
                    VALUES (?, ?, ?, NOW(), 'test', NOW(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, externalId, enabled, environment.ordinal()));
    }

    private long createConfiguration(
        long integrationId, int integrationVersion, Environment environment, boolean enabled) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(integrationVersion);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfiguration.getId(), enabled);

        return integrationInstanceConfiguration.getId();
    }

    private long createIntegration() {
        return createIntegration("componentName");
    }

    private long createIntegration(String componentName) {
        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return integration.getId();
    }
}
