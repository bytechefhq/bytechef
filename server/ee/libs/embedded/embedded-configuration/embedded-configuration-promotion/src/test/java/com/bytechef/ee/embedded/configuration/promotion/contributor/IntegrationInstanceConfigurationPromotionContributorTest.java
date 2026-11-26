/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.promotion.contributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionCandidate;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.test.assertion.PromotionContributorAssertions;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.configuration.promotion.EmbeddedConnectionCandidates;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class IntegrationInstanceConfigurationPromotionContributorTest {

    private static final long INTEGRATION_ID = 7L;
    private static final long SOURCE_CONNECTION_ID = 41L;
    private static final long SOURCE_ID = 100L;
    private static final long TARGET_CONNECTION_ID = 51L;
    private static final long TARGET_ID = 200L;
    private static final UUID CONFIGURATION_UUID = UUID.fromString("5b1f4c1e-8b8a-4e43-9d1f-3a3f7c7a0001");
    private static final UUID FIRST_WORKFLOW_UUID = UUID.fromString("5b1f4c1e-8b8a-4e43-9d1f-3a3f7c7a0011");
    private static final UUID PROVISION_WORKFLOW_UUID = UUID.fromString("5b1f4c1e-8b8a-4e43-9d1f-3a3f7c7a0013");
    private static final UUID SECOND_WORKFLOW_UUID = UUID.fromString("5b1f4c1e-8b8a-4e43-9d1f-3a3f7c7a0012");

    @Mock
    private ConnectionService connectionService;

    @Mock
    private EmbeddedConnectionCandidates embeddedConnectionCandidates;

    @Mock
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @Mock
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Mock
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Mock
    private IntegrationInstanceService integrationInstanceService;

    @Mock
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Mock
    private IntegrationWorkflowService integrationWorkflowService;

    @Mock
    private TagService tagService;

    @Mock
    private WorkflowService workflowService;

    private IntegrationInstanceConfigurationPromotionContributor contributor;

    @BeforeEach
    void beforeEach() {
        contributor = new IntegrationInstanceConfigurationPromotionContributor(
            connectionService, embeddedConnectionCandidates, integrationInstanceConfigurationFacade,
            integrationInstanceConfigurationService, integrationInstanceConfigurationWorkflowService,
            integrationInstanceService, integrationInstanceWorkflowService, integrationWorkflowService, tagService,
            workflowService);

        lenient().when(integrationWorkflowService.getIntegrationWorkflows(INTEGRATION_ID))
            .thenReturn(List.of(
                new IntegrationWorkflow(INTEGRATION_ID, 1, "workflow-a-v1", FIRST_WORKFLOW_UUID),
                new IntegrationWorkflow(INTEGRATION_ID, 2, "workflow-a-v2", FIRST_WORKFLOW_UUID),
                new IntegrationWorkflow(INTEGRATION_ID, 2, "workflow-b-v2", SECOND_WORKFLOW_UUID),
                new IntegrationWorkflow(INTEGRATION_ID, 2, "workflow-p-v2", PROVISION_WORKFLOW_UUID)));
        lenient().when(workflowService.getWorkflow("workflow-a-v1"))
            .thenReturn(workflow("workflow-a-v1", "Sync Orders", "region"));
        lenient().when(workflowService.getWorkflow("workflow-a-v2"))
            .thenReturn(workflow("workflow-a-v2", "Sync Orders", "region", "channel"));
        lenient().when(workflowService.getWorkflow("workflow-b-v2"))
            .thenReturn(workflow("workflow-b-v2", "Notify", "recipient"));
        lenient().when(workflowService.getWorkflow("workflow-p-v2"))
            .thenReturn(provisionWorkflow());
        lenient().when(connectionService.getConnections(anyList()))
            .thenReturn(List.of(connection(SOURCE_CONNECTION_ID, "Slack Dev", "slack")));
        lenient().when(embeddedConnectionCandidates.candidates("slack", Environment.PRODUCTION))
            .thenReturn(List.of(new PromotionConnectionCandidate(TARGET_CONNECTION_ID, "Slack Prod")));
        lenient().when(tagService.getTags(List.of(3L)))
            .thenReturn(List.of(tag(3L, "billing")));
    }

    @Test
    void testGetPlatformType() {
        assertThat(contributor.getPlatformType()).isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testGetResourceType() {
        assertThat(contributor.getResourceType()).isEqualTo("INTEGRATION_INSTANCE_CONFIGURATION");
    }

    @Test
    void testPreviewAndPromoteRequireTenantAdmin() {
        PromotionContributorAssertions.assertPreAuthorize(
            IntegrationInstanceConfigurationPromotionContributor.class, "preview",
            new Class<?>[] {
                long.class, Environment.class, Environment.class
            },
            "isTenantAdmin()");
        PromotionContributorAssertions.assertPreAuthorize(
            IntegrationInstanceConfigurationPromotionContributor.class, "promote",
            new Class<?>[] {
                long.class, Environment.class, Environment.class, Map.class, String.class
            },
            "isTenantAdmin()");
    }

    @Test
    void testPromoteCreatesDisabledCopyWithEmptySettingsAndInputs() {
        IntegrationInstanceConfiguration source = sourceConfiguration(2);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(source);
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(
                configurationWorkflow(
                    "workflow-a-v2", true, Map.of("region", "eu", "channel", "#dev"),
                    new IntegrationInstanceConfigurationWorkflowConnection(SOURCE_CONNECTION_ID, "slack", "node_1")),
                configurationWorkflow("workflow-b-v2", false, Map.of("recipient", "dev@example.com"))));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.empty());

        ArgumentCaptor<IntegrationInstanceConfiguration> createdCaptor =
            ArgumentCaptor.forClass(IntegrationInstanceConfiguration.class);

        when(integrationInstanceConfigurationService.create(createdCaptor.capture()))
            .thenAnswer(invocation -> {
                IntegrationInstanceConfiguration created = invocation.getArgument(0);

                created.setId(TARGET_ID);

                return created;
            });

        EnvironmentPromotionResult result = contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION,
            Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID), null);

        IntegrationInstanceConfiguration created = createdCaptor.getValue();

        assertThat(created.getUuid()).isEqualTo(CONFIGURATION_UUID);
        assertThat(created.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(created.isEnabled()).isFalse();
        assertThat(created.getIntegrationId()).isEqualTo(INTEGRATION_ID);
        assertThat(created.getIntegrationVersion()).isEqualTo(2);
        assertThat(created.getAuthorizationType()).isEqualTo(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
        assertThat(created.getName()).isEqualTo("Orders");
        assertThat(created.getDescription()).isEqualTo("Orders integration");
        assertThat(created.getPermissionExpression()).isEqualTo("hasRole('ORDERS')");
        assertThat(created.getTagIds()).containsExactly(3L);
        assertThat(created.getConnectionParameters()).isEmpty();

        IntegrationInstanceConfigurationDTO updated = captureFacadeUpdate();

        assertThat(updated.id()).isEqualTo(TARGET_ID);
        assertThat(updated.enabled()).isFalse();
        assertThat(updated.integrationVersion()).isEqualTo(2);
        assertThat(updated.name()).isEqualTo("Orders");
        assertThat(updated.tags()).extracting(Tag::getId)
            .containsExactly(3L);

        List<IntegrationInstanceConfigurationWorkflow> workflows = toWorkflows(updated);

        assertThat(workflows).extracting(IntegrationInstanceConfigurationWorkflow::getWorkflowId)
            .containsExactly("workflow-a-v2", "workflow-b-v2");
        assertThat(workflows).extracting(IntegrationInstanceConfigurationWorkflow::isEnabled)
            .containsExactly(true, false);
        assertThat(workflows).allSatisfy(workflow -> assertThat(workflow.getInputs()).isEmpty());
        IntegrationInstanceConfigurationWorkflow firstWorkflow = workflows.getFirst();

        assertThat(firstWorkflow.getConnections()).containsExactly(
            new IntegrationInstanceConfigurationWorkflowConnection(TARGET_CONNECTION_ID, "slack", "node_1"));

        assertThat(result.targetId()).isEqualTo(TARGET_ID);
        assertThat(result.created()).isTrue();
        assertThat(result.unresolvedConnectionIds()).isEmpty();
    }

    @Test
    void testPromoteCreateLeavesUnmappedConnectionUnresolved() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(
                configurationWorkflow(
                    "workflow-a-v2", true, Map.of(),
                    new IntegrationInstanceConfigurationWorkflowConnection(SOURCE_CONNECTION_ID, "slack", "node_1"))));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.empty());
        when(integrationInstanceConfigurationService.create(any()))
            .thenAnswer(invocation -> {
                IntegrationInstanceConfiguration created = invocation.getArgument(0);

                created.setId(TARGET_ID);

                return created;
            });

        EnvironmentPromotionResult result = contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        List<IntegrationInstanceConfigurationWorkflow> workflows = toWorkflows(captureFacadeUpdate());

        IntegrationInstanceConfigurationWorkflow firstWorkflow = workflows.getFirst();

        assertThat(firstWorkflow.getConnections()).isEmpty();
        assertThat(result.unresolvedConnectionIds()).containsExactly(SOURCE_CONNECTION_ID);
    }

    @Test
    void testPromoteUpdateKeepsTargetSettingsEnabledFlagAndInputs() {
        IntegrationInstanceConfiguration target = targetConfiguration(1, true);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(
                configurationWorkflow(
                    "workflow-a-v2", true, Map.of("region", "eu", "channel", "#dev"),
                    new IntegrationInstanceConfigurationWorkflowConnection(SOURCE_CONNECTION_ID, "slack", "node_1")),
                configurationWorkflow("workflow-b-v2", true, Map.of("recipient", "dev@example.com"))));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.of(target));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(TARGET_ID))
            .thenReturn(List.of(
                configurationWorkflow(
                    "workflow-a-v1", false, Map.of("region", "us"),
                    new IntegrationInstanceConfigurationWorkflowConnection(77L, "slack", "node_1"))));

        EnvironmentPromotionResult result = contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        verify(integrationInstanceConfigurationService, never()).create(any());

        IntegrationInstanceConfigurationDTO updated = captureFacadeUpdate();

        assertThat(updated.id()).isEqualTo(TARGET_ID);
        assertThat(updated.version()).isEqualTo(4);
        assertThat(updated.enabled()).isTrue();
        assertThat(updated.environment()).isEqualTo(Environment.PRODUCTION);
        assertThat(updated.integrationVersion()).isEqualTo(2);
        assertThat(updated.connectionParameters()).isEqualTo(Map.of("clientId", "production-client"));
        assertThat(updated.authorizationType()).isEqualTo(AuthorizationType.API_KEY);
        assertThat(updated.name()).isEqualTo("Orders");
        assertThat(updated.description()).isEqualTo("Orders integration");
        assertThat(updated.permissionExpression()).isEqualTo("hasRole('ORDERS')");

        List<IntegrationInstanceConfigurationWorkflow> workflows = toWorkflows(updated);

        assertThat(workflows).extracting(IntegrationInstanceConfigurationWorkflow::getWorkflowId)
            .containsExactly("workflow-a-v2", "workflow-b-v2");

        IntegrationInstanceConfigurationWorkflow matchedWorkflow = workflows.getFirst();

        assertThat(matchedWorkflow.isEnabled()).isFalse();
        assertThat(matchedWorkflow.getInputs()).isEqualTo(Map.of("region", "us"));
        assertThat(matchedWorkflow.getConnections()).containsExactly(
            new IntegrationInstanceConfigurationWorkflowConnection(77L, "slack", "node_1"));

        IntegrationInstanceConfigurationWorkflow newWorkflow = workflows.get(1);

        assertThat(newWorkflow.isEnabled()).isTrue();
        assertThat(newWorkflow.getInputs()).isEmpty();

        assertThat(result.targetId()).isEqualTo(TARGET_ID);
        assertThat(result.created()).isFalse();
        assertThat(result.unresolvedConnectionIds()).isEmpty();
    }

    @Test
    void testPromoteUpdatePrefersRequestedMappingOverTargetConnection() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(
                configurationWorkflow(
                    "workflow-a-v2", true, Map.of(),
                    new IntegrationInstanceConfigurationWorkflowConnection(SOURCE_CONNECTION_ID, "slack", "node_1"))));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.of(targetConfiguration(1, false)));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(TARGET_ID))
            .thenReturn(List.of(
                configurationWorkflow(
                    "workflow-a-v1", true, Map.of(),
                    new IntegrationInstanceConfigurationWorkflowConnection(77L, "slack", "node_1"))));

        contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION,
            Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID), null);

        List<IntegrationInstanceConfigurationWorkflow> workflows = toWorkflows(captureFacadeUpdate());

        IntegrationInstanceConfigurationWorkflow firstWorkflow = workflows.getFirst();

        assertThat(firstWorkflow.getConnections()).containsExactly(
            new IntegrationInstanceConfigurationWorkflowConnection(TARGET_CONNECTION_ID, "slack", "node_1"));
    }

    @Test
    void testPromoteRejectsMappingToNonCandidateConnection() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(
                configurationWorkflow(
                    "workflow-a-v2", true, Map.of(),
                    new IntegrationInstanceConfigurationWorkflowConnection(SOURCE_CONNECTION_ID, "slack", "node_1"))));

        assertThatThrownBy(() -> contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(SOURCE_CONNECTION_ID, 999L), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());

        verify(integrationInstanceConfigurationService, never()).create(any());
        verify(integrationInstanceConfigurationFacade, never()).updateIntegrationInstanceConfiguration(any());
    }

    @Test
    void testPromoteRejectsMappingOfConnectionTheSourceDoesNotUse() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of());

        assertThatThrownBy(() -> contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(88L, TARGET_CONNECTION_ID), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());
    }

    @Test
    void testPromoteRejectsSourceWithoutIntegrationVersion() {
        IntegrationInstanceConfiguration source = sourceConfiguration(2);

        source.setIntegrationVersion(null);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(source);

        assertThatThrownBy(() -> contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.SOURCE_VERSION_MISSING.getErrorKey());

        verify(integrationInstanceConfigurationService, never()).create(any());
        verify(integrationInstanceConfigurationFacade, never()).updateIntegrationInstanceConfiguration(any());
    }

    @Test
    void testPreviewRejectsSourceWithoutIntegrationVersion() {
        IntegrationInstanceConfiguration source = sourceConfiguration(2);

        source.setIntegrationVersion(null);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(source);

        assertThatThrownBy(() -> contributor.preview(SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorKey")
            .isEqualTo(EnvironmentPromotionErrorType.SOURCE_VERSION_MISSING.getErrorKey());
    }

    @Test
    void testPromoteRejectsWrongSourceEnvironment() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));

        assertThatThrownBy(() -> contributor.promote(
            SOURCE_ID, Environment.STAGING, Environment.PRODUCTION, Map.of(), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.SOURCE_ENVIRONMENT_MISMATCH.getErrorKey());
    }

    @Test
    void testPreviewRejectsWrongSourceEnvironment() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));

        assertThatThrownBy(() -> contributor.preview(SOURCE_ID, Environment.STAGING, Environment.PRODUCTION))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorKey")
            .isEqualTo(EnvironmentPromotionErrorType.SOURCE_ENVIRONMENT_MISMATCH.getErrorKey());
    }

    @Test
    void testPreviewRejectsSameEnvironment() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));

        assertThatThrownBy(() -> contributor.preview(SOURCE_ID, Environment.DEVELOPMENT, Environment.DEVELOPMENT))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorKey")
            .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey());
    }

    @Test
    void testPreviewOfNewTargetWarnsAboutEveryInputAndCarriesCandidates() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(
                configurationWorkflow(
                    "workflow-a-v2", true, Map.of("region", "eu"),
                    new IntegrationInstanceConfigurationWorkflowConnection(SOURCE_CONNECTION_ID, "slack", "node_1")),
                configurationWorkflow("workflow-b-v2", false, Map.of())));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.empty());

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.resourceType()).isEqualTo("INTEGRATION_INSTANCE_CONFIGURATION");
        assertThat(preview.sourceEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(preview.targetEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(preview.existingTargetId()).isNull();
        assertThat(preview.existingTargetName()).isNull();
        assertThat(preview.warnings()).contains(
            "Workflow \"Sync Orders\" input \"region\" needs a value in Production.",
            "Workflow \"Sync Orders\" input \"channel\" needs a value in Production.",
            "Workflow \"Notify\" input \"recipient\" needs a value in Production.");
        assertThat(preview.warnings()).anySatisfy(warning -> assertThat(warning).contains("created disabled"));

        List<PromotionConnectionMapping> connectionMappings = preview.connections();

        PromotionConnectionMapping connectionMapping = connectionMappings.getFirst();

        assertThat(connectionMapping.sourceConnectionId()).isEqualTo(SOURCE_CONNECTION_ID);
        assertThat(connectionMapping.componentName()).isEqualTo("slack");
        assertThat(connectionMapping.usedBy()).containsExactly("Sync Orders › node_1");
        assertThat(connectionMapping.targetCandidates())
            .containsExactly(new PromotionConnectionCandidate(TARGET_CONNECTION_ID, "Slack Prod"));
    }

    @Test
    void testPreviewOfExistingTargetWarnsOnlyAboutNewInputs() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(
                configurationWorkflow(
                    "workflow-a-v2", true, Map.of(),
                    new IntegrationInstanceConfigurationWorkflowConnection(SOURCE_CONNECTION_ID, "slack", "node_1")),
                configurationWorkflow("workflow-b-v2", true, Map.of())));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.of(targetConfiguration(1, true)));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(TARGET_ID))
            .thenReturn(List.of(
                configurationWorkflow(
                    "workflow-a-v1", true, Map.of("region", "us"),
                    new IntegrationInstanceConfigurationWorkflowConnection(77L, "slack", "node_1"))));

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.existingTargetId()).isEqualTo(TARGET_ID);
        assertThat(preview.existingTargetName()).isEqualTo("Orders Production");
        assertThat(preview.warnings()).containsExactly(
            "Workflow \"Sync Orders\" input \"channel\" needs a value in Production.",
            "Workflow \"Notify\" input \"recipient\" needs a value in Production.");
        List<PromotionConnectionMapping> connectionMappings = preview.connections();

        PromotionConnectionMapping connectionMapping = connectionMappings.getFirst();

        assertThat(connectionMapping.suggestedTargetConnectionId()).isEqualTo(77L);
    }

    @Test
    void testPreviewWarnsAboutConnectionWithoutSuggestedTarget() {
        when(embeddedConnectionCandidates.candidates("slack", Environment.PRODUCTION)).thenReturn(List.of());
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(
                configurationWorkflow(
                    "workflow-a-v2", true, Map.of(),
                    new IntegrationInstanceConfigurationWorkflowConnection(SOURCE_CONNECTION_ID, "slack", "node_1"))));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.empty());

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.warnings()).contains(
            "Connection \"Slack Dev\" has no counterpart in Production; the workflows that use it stay without a " +
                "connection until one is chosen.");
    }

    @Test
    void testPreviewOfNewTargetWarnsThatWorkflowNeedingInternalOnlyInputIsCreatedDisabled() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(configurationWorkflow("workflow-p-v2", true, Map.of("accountId", "dev-account"))));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.empty());

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.warnings()).contains(
            "Workflow \"Provision Account\" is created disabled until its internal-only input \"accountId\" is " +
                "filled in Production.");
    }

    @Test
    void testPreviewOfExistingTargetWarnsThatMissingInternalOnlyInputRefusesPromotion() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(configurationWorkflow("workflow-p-v2", true, Map.of("accountId", "dev-account"))));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.of(targetConfiguration(2, true)));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(TARGET_ID))
            .thenReturn(List.of(configurationWorkflow("workflow-p-v2", true, Map.of())));

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.warnings()).contains(
            "Workflow \"Provision Account\" needs a value for its internal-only input \"accountId\" in Production; " +
                "the promotion is refused until it is filled there.");
        assertThat(preview.warnings()).noneSatisfy(
            warning -> assertThat(warning).contains("is created disabled until"));
    }

    @Test
    void testPreviewOfExistingTargetWarnsThatNewWorkflowNeedingInternalOnlyInputIsCreatedDisabled() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(configurationWorkflow("workflow-p-v2", true, Map.of("accountId", "dev-account"))));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.of(targetConfiguration(1, true)));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(TARGET_ID))
            .thenReturn(List.of());

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.warnings()).contains(
            "Workflow \"Provision Account\" is created disabled until its internal-only input \"accountId\" is " +
                "filled in Production.");
        assertThat(preview.warnings()).noneSatisfy(
            warning -> assertThat(warning).contains("the promotion is refused"));
    }

    @Test
    void testPromoteUpdateDisablesOnlyNewWorkflowNeedingInternalOnlyInput() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(
                configurationWorkflow("workflow-b-v2", true, Map.of()),
                configurationWorkflow("workflow-p-v2", true, Map.of("accountId", "dev-account"))));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.of(targetConfiguration(1, true)));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(TARGET_ID))
            .thenReturn(List.of());

        EnvironmentPromotionResult result = contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        verify(integrationInstanceConfigurationService, never()).create(any());

        List<IntegrationInstanceConfigurationWorkflow> workflows = toWorkflows(captureFacadeUpdate());

        assertThat(workflows).extracting(IntegrationInstanceConfigurationWorkflow::getWorkflowId)
            .containsExactly("workflow-b-v2", "workflow-p-v2");
        assertThat(workflows).extracting(IntegrationInstanceConfigurationWorkflow::isEnabled)
            .containsExactly(true, false);
        assertThat(result.created()).isFalse();
    }

    @Test
    void testPromoteUpdateKeepsExistingWorkflowNeedingInternalOnlyInputEnabledForFacadeCheck() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(configurationWorkflow("workflow-p-v2", true, Map.of("accountId", "dev-account"))));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.of(targetConfiguration(2, true)));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(TARGET_ID))
            .thenReturn(List.of(configurationWorkflow("workflow-p-v2", true, Map.of())));

        contributor.promote(SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        List<IntegrationInstanceConfigurationWorkflow> workflows = toWorkflows(captureFacadeUpdate());

        assertThat(workflows).extracting(IntegrationInstanceConfigurationWorkflow::isEnabled)
            .containsExactly(true);
    }

    @Test
    void testPromoteCreateDisablesWorkflowNeedingInternalOnlyInput() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(SOURCE_ID))
            .thenReturn(sourceConfiguration(2));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(SOURCE_ID))
            .thenReturn(List.of(
                configurationWorkflow("workflow-b-v2", true, Map.of()),
                configurationWorkflow("workflow-p-v2", true, Map.of("accountId", "dev-account"))));
        when(integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            CONFIGURATION_UUID, Environment.PRODUCTION)).thenReturn(Optional.empty());
        when(integrationInstanceConfigurationService.create(any()))
            .thenAnswer(invocation -> {
                IntegrationInstanceConfiguration created = invocation.getArgument(0);

                created.setId(TARGET_ID);

                return created;
            });

        contributor.promote(SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        List<IntegrationInstanceConfigurationWorkflow> workflows = toWorkflows(captureFacadeUpdate());

        assertThat(workflows).extracting(IntegrationInstanceConfigurationWorkflow::getWorkflowId)
            .containsExactly("workflow-b-v2", "workflow-p-v2");
        assertThat(workflows).extracting(IntegrationInstanceConfigurationWorkflow::isEnabled)
            .containsExactly(true, false);
    }

    private IntegrationInstanceConfigurationDTO captureFacadeUpdate() {
        ArgumentCaptor<IntegrationInstanceConfigurationDTO> updatedCaptor =
            ArgumentCaptor.forClass(IntegrationInstanceConfigurationDTO.class);

        verify(integrationInstanceConfigurationFacade).updateIntegrationInstanceConfiguration(
            updatedCaptor.capture());

        return updatedCaptor.getValue();
    }

    private static List<IntegrationInstanceConfigurationWorkflow> toWorkflows(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        List<IntegrationInstanceConfigurationWorkflow> workflows = new ArrayList<>();

        for (IntegrationInstanceConfigurationWorkflowDTO workflowDTO : integrationInstanceConfigurationDTO
            .integrationInstanceConfigurationWorkflows()) {

            workflows.add(workflowDTO.toIntegrationInstanceConfigurationWorkflow());
        }

        return workflows;
    }

    private static IntegrationInstanceConfiguration sourceConfiguration(int integrationVersion) {
        IntegrationInstanceConfiguration source = new IntegrationInstanceConfiguration();

        source.setAuthorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
        source.setConnectionParameters(Map.of("clientId", "development-client", "clientSecret", "development"));
        source.setDescription("Orders integration");
        source.setEnabled(true);
        source.setEnvironment(Environment.DEVELOPMENT);
        source.setId(SOURCE_ID);
        source.setIntegrationId(INTEGRATION_ID);
        source.setIntegrationVersion(integrationVersion);
        source.setName("Orders");
        source.setPermissionExpression("hasRole('ORDERS')");
        source.setTagIds(List.of(3L));
        source.setUuid(CONFIGURATION_UUID);

        return source;
    }

    private static IntegrationInstanceConfiguration targetConfiguration(int integrationVersion, boolean enabled) {
        IntegrationInstanceConfiguration target = new IntegrationInstanceConfiguration();

        target.setAuthorizationType(AuthorizationType.API_KEY);
        target.setConnectionParameters(Map.of("clientId", "production-client"));
        target.setDescription("Old description");
        target.setEnabled(enabled);
        target.setEnvironment(Environment.PRODUCTION);
        target.setId(TARGET_ID);
        target.setIntegrationId(INTEGRATION_ID);
        target.setIntegrationVersion(integrationVersion);
        target.setName("Orders Production");
        target.setUuid(CONFIGURATION_UUID);
        target.setVersion(4);

        return target;
    }

    private static IntegrationInstanceConfigurationWorkflow configurationWorkflow(
        String workflowId, boolean enabled, Map<String, Object> inputs,
        IntegrationInstanceConfigurationWorkflowConnection... connections) {

        IntegrationInstanceConfigurationWorkflow configurationWorkflow = new IntegrationInstanceConfigurationWorkflow(
            List.of(connections), inputs, workflowId);

        configurationWorkflow.setEnabled(enabled);

        return configurationWorkflow;
    }

    private static Workflow workflow(String id, String label, String... inputNames) {
        StringBuilder inputs = new StringBuilder();

        for (String inputName : inputNames) {
            if (!inputs.isEmpty()) {
                inputs.append(',');
            }

            inputs.append("{\"name\": \"%s\", \"label\": \"%s\", \"type\": \"string\", \"required\": false}"
                .formatted(inputName, inputName));
        }

        return new Workflow(
            id, "{\"label\": \"%s\", \"inputs\": [%s], \"triggers\": [], \"tasks\": []}".formatted(label, inputs),
            Workflow.Format.JSON);
    }

    private static Workflow provisionWorkflow() {
        return new Workflow(
            "workflow-p-v2",
            "{\"label\": \"Provision Account\", \"inputs\": [{\"name\": \"accountId\", \"label\": \"Account\", " +
                "\"type\": \"string\", \"required\": true, \"internalOnly\": true}], \"triggers\": [], " +
                "\"tasks\": []}",
            Workflow.Format.JSON);
    }

    private static Connection connection(long id, String name, String componentName) {
        return Connection.builder()
            .componentName(componentName)
            .connectionVersion(1)
            .id(id)
            .name(name)
            .type(PlatformType.EMBEDDED)
            .build();
    }

    private static Tag tag(long id, String name) {
        Tag tag = new Tag(name);

        tag.setId(id);

        return tag;
    }
}
