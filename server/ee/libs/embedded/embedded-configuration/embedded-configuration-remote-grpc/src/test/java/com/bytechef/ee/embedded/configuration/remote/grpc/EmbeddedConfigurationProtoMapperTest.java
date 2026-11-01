/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedConfigurationProtoMapperTest {

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper =
        new EmbeddedConfigurationProtoMapper(
            JsonMapper.builder()
                .build());

    @Test
    void testIntegrationRoundTrip() {
        Integration integration = integration(7L);

        assertThat(integration.getIntegrationVersions()).hasSize(2);

        Integration readIntegration = embeddedConfigurationProtoMapper.toIntegration(
            embeddedConfigurationProtoMapper.toProto(integration));

        assertThat(readIntegration).usingRecursiveComparison()
            .isEqualTo(integration);
        assertThat(embeddedConfigurationProtoMapper.toIntegrations(
            embeddedConfigurationProtoMapper.toRemoteIntegrations(List.of(integration, integration(8L)))))
                .extracting(Integration::getId)
                .containsExactly(7L, 8L);
    }

    @Test
    void testIntegrationInstanceConfigurationRoundTrip() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setAuthorizationType(AuthorizationType.API_KEY);
        integrationInstanceConfiguration.setConnectionParameters(Map.of("key", "secret", "id", 9_007_199_254_740_993L));
        integrationInstanceConfiguration.setDescription("Configured");
        integrationInstanceConfiguration.setEnabled(true);
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setId(5L);
        integrationInstanceConfiguration.setIntegrationId(7L);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("Airtable");
        integrationInstanceConfiguration.setTagIds(List.of(3L));
        integrationInstanceConfiguration.setVersion(2);

        setAuditFields(integrationInstanceConfiguration);

        IntegrationInstanceConfiguration readIntegrationInstanceConfiguration =
            embeddedConfigurationProtoMapper.toIntegrationInstanceConfiguration(
                embeddedConfigurationProtoMapper.toProto(integrationInstanceConfiguration));

        assertThat(readIntegrationInstanceConfiguration).usingRecursiveComparison()
            .isEqualTo(integrationInstanceConfiguration);
        assertThat(readIntegrationInstanceConfiguration.getConnectionParameters()
            .get("id")).isInstanceOf(Long.class);
    }

    @Test
    void testIntegrationInstanceConfigurationWorkflowRoundTrip() {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setConnections(List.of(connection(4L), connection(5L)));
        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setId(11L);
        integrationInstanceConfigurationWorkflow.setInputs(Map.of("ratio", 5.0));
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(5L);
        integrationInstanceConfigurationWorkflow.setVersion(2);
        integrationInstanceConfigurationWorkflow.setWorkflowId("workflow-1");

        setAuditFields(integrationInstanceConfigurationWorkflow);

        assertThat(embeddedConfigurationProtoMapper.toIntegrationInstanceConfigurationWorkflow(
            embeddedConfigurationProtoMapper.toProto(integrationInstanceConfigurationWorkflow)))
                .usingRecursiveComparison()
                .isEqualTo(integrationInstanceConfigurationWorkflow);
        assertThat(embeddedConfigurationProtoMapper.toIntegrationInstanceConfigurationWorkflowConnections(
            embeddedConfigurationProtoMapper.toRemoteIntegrationInstanceConfigurationWorkflowConnections(
                List.of(connection(4L)))))
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(connection(4L));
    }

    @Test
    void testIntegrationInstanceWorkflowRoundTrip() {
        IntegrationInstanceWorkflow integrationInstanceWorkflow = new IntegrationInstanceWorkflow();

        integrationInstanceWorkflow.setEnabled(true);
        integrationInstanceWorkflow.setId(13L);
        integrationInstanceWorkflow.setInputs(Map.of("count", 5));
        integrationInstanceWorkflow.setIntegrationInstanceConfigurationWorkflowId(11L);
        integrationInstanceWorkflow.setIntegrationInstanceId(12L);
        integrationInstanceWorkflow.setVersion(2);

        setAuditFields(integrationInstanceWorkflow);

        assertThat(embeddedConfigurationProtoMapper.toIntegrationInstanceWorkflow(
            embeddedConfigurationProtoMapper.toProto(integrationInstanceWorkflow)))
                .usingRecursiveComparison()
                .isEqualTo(integrationInstanceWorkflow);
    }

    @Test
    void testAppEventRoundTrip() {
        AppEvent appEvent = new AppEvent();

        appEvent.setId(3L);
        appEvent.setName("order.created");
        appEvent.setSchema("{\"type\":\"object\"}");
        appEvent.setVersion(1);

        setAuditFields(appEvent);

        assertThat(embeddedConfigurationProtoMapper.toAppEvent(embeddedConfigurationProtoMapper.toProto(appEvent)))
            .usingRecursiveComparison()
            .isEqualTo(appEvent);
    }

    @Test
    void testAutomationWorkflowProjectsRoundTrip() {
        List<AutomationWorkflowProjectDTO> automationWorkflowProjectDTOs = List.of(
            new AutomationWorkflowProjectDTO(
                7L, "Live Project", "Live", 2L, List.of(3L), true, 2, 1,
                List.of(
                    new ConnectedUserWorkflowTemplateDTO(
                        "uuid-1", "Sync", "Syncs", "2026-10-26T10:00:00Z",
                        List.of(
                            new ConnectedUserWorkflowTemplateDTO.Component("airtable", "Airtable", "icon", true, 4L)),
                        List.of(new ConnectedUserWorkflowTemplateDTO.Component("httpClient", "HTTP", "icon")),
                        List.of(new ConnectedUserWorkflowTemplateDTO.Input("name", "Name", "STRING", true)),
                        "hasAuthority('ADMIN')", "workflow-1")),
                "hasAuthority('ADMIN')", true, 3, Instant.parse("2026-10-26T10:00:00Z")));

        assertThat(embeddedConfigurationProtoMapper.toAutomationWorkflowProjectDTOs(
            embeddedConfigurationProtoMapper.toRemoteAutomationWorkflowProjects(automationWorkflowProjectDTOs)))
                .isEqualTo(automationWorkflowProjectDTOs);
    }

    private static IntegrationInstanceConfigurationWorkflowConnection connection(long connectionId) {
        return new IntegrationInstanceConfigurationWorkflowConnection(connectionId, "airtable", "airtable_1");
    }

    private static Integration integration(long id) {
        Integration integration = new Integration();

        integration.setCategoryId(2L);
        integration.setComponentName("airtable");
        integration.setComponentVersion(1);
        integration.setDescription("Airtable");
        integration.setId(id);
        integration.setIntegrationVersions(
            List.of(
                new IntegrationVersion(
                    1, IntegrationVersion.Status.PUBLISHED.ordinal(), Instant.parse("2026-10-26T10:30:00Z"), "First"),
                new IntegrationVersion(2)));
        integration.setMultipleInstances(true);
        integration.setName("Airtable");
        integration.setPermissionExpression("hasAuthority('ADMIN')");
        integration.setTagIds(List.of(3L));
        integration.setVersion(4);

        setAuditFields(integration);

        return integration;
    }

    private static void setAuditFields(Object object) {
        ReflectionTestUtils.setField(object, "createdBy", "admin@localhost.com");
        ReflectionTestUtils.setField(object, "createdDate", Instant.parse("2026-10-26T10:00:00Z"));
        ReflectionTestUtils.setField(object, "lastModifiedBy", "user@localhost.com");
        ReflectionTestUtils.setField(object, "lastModifiedDate", Instant.parse("2026-10-26T11:00:00Z"));
    }
}
