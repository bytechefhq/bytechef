/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.instance.accessor;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.instance.config.IntegrationJobPrincipalAccessorIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationJobPrincipalAccessorIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class IntegrationJobPrincipalAccessorIntTest {

    private static final long CONNECTED_USER_ID = 1L;
    private static final long CONNECTION_ID = 1L;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private IntegrationJobPrincipalAccessor integrationJobPrincipalAccessor;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    private long integrationId;
    private String workflowId;
    private String workflowUuid;

    @BeforeEach
    void beforeEach() {
        Integration integration = new Integration();

        integration.setComponentName("hubspot");
        integration.setName("HubSpot " + UUID.randomUUID());

        integration = integrationService.create(integration);

        integrationId = integration.getId();
        workflowId = "workflow-" + UUID.randomUUID();

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(integrationId, 1, workflowId);

        workflowUuid = integrationWorkflow.getUuidAsString();
    }

    @Test
    void testIsWorkflowEnabledUsesConfigurationIdNotInstanceId() {
        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(true, true, Map.of());

        long integrationInstanceId = createIntegrationInstanceWithDecoyConfiguration(
            integrationInstanceConfigurationId, Map.of());

        assertThat(integrationJobPrincipalAccessor.isWorkflowEnabled(integrationInstanceId, workflowUuid)).isTrue();
    }

    @Test
    void testIsWorkflowEnabledReturnsFalseWhenConfigurationDisabled() {
        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(false, true, Map.of());

        long integrationInstanceId = createIntegrationInstance(integrationInstanceConfigurationId);

        assertThat(integrationJobPrincipalAccessor.isWorkflowEnabled(integrationInstanceId, workflowUuid)).isFalse();
    }

    @Test
    void testGetInputMapUsesConfigurationIdNotInstanceId() {
        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(
            true, true, Map.of("region", "eu"));

        long integrationInstanceId = createIntegrationInstanceWithDecoyConfiguration(
            integrationInstanceConfigurationId, Map.of("region", "decoy"));

        Map<String, ?> inputMap = integrationJobPrincipalAccessor.getInputMap(integrationInstanceId, workflowUuid);

        assertThat(inputMap).isEqualTo(Map.of("region", "eu"));
    }

    @Test
    void testGetInputMapMergesConnectedUserInputsOverConfigurationInputs() {
        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(
            true, true, Map.of("apiKey", "config-key", "region", "eu"));

        long integrationInstanceId = createIntegrationInstance(integrationInstanceConfigurationId);

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfigurationId, workflowId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                integrationInstanceId, integrationInstanceConfigurationWorkflow.getId());

        integrationInstanceWorkflow.setInputs(
            Map.of("apiKey", "user-key", "contactMapping", Map.of("objectType", "contacts", "mappings", List.of())));

        integrationInstanceWorkflowService.update(integrationInstanceWorkflow);

        Map<String, Object> inputMap = Map.copyOf(
            integrationJobPrincipalAccessor.getInputMap(integrationInstanceId, workflowUuid));

        assertThat(inputMap)
            .containsEntry("apiKey", "user-key")
            .containsEntry("region", "eu")
            .containsEntry("contactMapping", Map.of("objectType", "contacts", "mappings", List.of()));
    }

    @Test
    void testGetInputMapReturnsConfigurationInputsWhenConnectedUserHasNoRow() {
        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(
            true, true, Map.of("apiKey", "config-key"));

        long integrationInstanceId = createIntegrationInstance(integrationInstanceConfigurationId);

        Map<String, ?> inputMap = integrationJobPrincipalAccessor.getInputMap(integrationInstanceId, workflowUuid);

        assertThat(inputMap).isEqualTo(Map.of("apiKey", "config-key"));
    }

    private long createIntegrationInstance(long integrationInstanceConfigurationId) {
        IntegrationInstance integrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, CONNECTION_ID, integrationInstanceConfigurationId);

        return integrationInstance.getId();
    }

    private long createIntegrationInstanceConfiguration(
        boolean enabled, boolean workflowEnabled, Map<String, ?> workflowInputs) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("HubSpot");

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        long integrationInstanceConfigurationId = integrationInstanceConfiguration.getId();

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfigurationId, enabled);

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(workflowEnabled);
        integrationInstanceConfigurationWorkflow.setInputs(workflowInputs);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

        return integrationInstanceConfigurationId;
    }

    private long createIntegrationInstanceWithDecoyConfiguration(
        long integrationInstanceConfigurationId, Map<String, ?> decoyWorkflowInputs) {

        long integrationInstanceId = createIntegrationInstance(integrationInstanceConfigurationId);

        while (integrationInstanceId <= integrationInstanceConfigurationId) {
            integrationInstanceId = createIntegrationInstance(integrationInstanceConfigurationId);
        }

        long decoyIntegrationInstanceConfigurationId = integrationInstanceConfigurationId;

        while (decoyIntegrationInstanceConfigurationId < integrationInstanceId) {
            decoyIntegrationInstanceConfigurationId = createIntegrationInstanceConfiguration(
                false, false, decoyWorkflowInputs);
        }

        assertThat(decoyIntegrationInstanceConfigurationId).isEqualTo(integrationInstanceId);

        return integrationInstanceId;
    }
}
