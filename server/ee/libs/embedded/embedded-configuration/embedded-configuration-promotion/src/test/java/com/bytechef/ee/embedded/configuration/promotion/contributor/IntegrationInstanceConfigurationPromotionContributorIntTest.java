/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.promotion.contributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionChange.Kind;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.test.config.PromotionIntTestSharedMocks;
import com.bytechef.ee.automation.promotion.test.config.RecordingPermissionService;
import com.bytechef.ee.automation.promotion.util.PromotionChangeHashes;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.exception.IntegrationInstanceConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.promotion.config.EmbeddedConfigurationPromotionIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.promotion.config.EmbeddedConfigurationPromotionIntTestMocks;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EmbeddedConfigurationPromotionIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@PromotionIntTestSharedMocks
@EmbeddedConfigurationPromotionIntTestMocks
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
class IntegrationInstanceConfigurationPromotionContributorIntTest {

    private static final long CONNECTED_USER_ID = 1050L;

    private static final String INTERNAL_ONLY_WORKFLOW_DEFINITION = """
        {
            "label": "Provision Account",
            "inputs": [
                {"name": "accountId", "label": "Account", "type": "string", "required": true, "internalOnly": true}
            ],
            "triggers": [],
            "tasks": []
        }""";

    private static final String NOTIFY_WORKFLOW_DEFINITION = """
        {
            "label": "Notify",
            "inputs": [
                {"name": "recipient", "label": "Recipient", "type": "string", "required": false}
            ],
            "triggers": [],
            "tasks": []
        }""";

    private static final String SYNC_ORDERS_V1_WORKFLOW_DEFINITION = """
        {
            "label": "Sync Orders",
            "inputs": [
                {"name": "region", "label": "Region", "type": "string", "required": false}
            ],
            "triggers": [],
            "tasks": []
        }""";

    private static final String SYNC_ORDERS_V2_WORKFLOW_DEFINITION = """
        {
            "label": "Sync Orders",
            "inputs": [
                {"name": "region", "label": "Region", "type": "string", "required": false},
                {"name": "channel", "label": "Channel", "type": "string", "required": false}
            ],
            "triggers": [],
            "tasks": []
        }""";

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private EnvironmentPromotionContributor integrationInstanceConfigurationPromotionContributor;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingPermissionService permissionService;

    @Autowired
    private TagService tagService;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        permissionService.reset();

        deleteTestData();

        when(componentConnectionFacade.getComponentConnection(anyString(), anyString(), anyString()))
            .thenReturn(new ComponentConnection("slack", 1, "node_1", "slack", true));
    }

    @AfterEach
    void afterEach() {
        deleteTestData();

        permissionService.reset();
    }

    @Test
    void testPromoteCreatesDisabledCounterpartWithoutSettingsOrInputValues() {
        Fixture fixture = createFixture();

        Connection targetConnection = createConnection("Slack Prod", Environment.PRODUCTION);

        EnvironmentPromotionResult environmentPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(
                fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION,
                Map.of(fixture.sourceConnectionId(), Objects.requireNonNull(targetConnection.getId())), null);

        assertThat(environmentPromotionResult.created()).isTrue();
        assertThat(environmentPromotionResult.unresolvedConnectionIds()).isEmpty();

        IntegrationInstanceConfiguration target = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(environmentPromotionResult.targetId());

        assertThat(target.getUuid()).isEqualTo(fixture.uuid());
        assertThat(target.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(target.isEnabled()).isFalse();
        assertThat(target.getIntegrationId()).isEqualTo(fixture.integrationId());
        assertThat(target.getIntegrationVersion()).isEqualTo(2);
        assertThat(target.getAuthorizationType()).isEqualTo(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
        assertThat(target.getName()).isEqualTo("Orders");
        assertThat(target.getDescription()).isEqualTo("Orders integration");
        assertThat(target.getPermissionExpression()).isEqualTo("hasRole('ORDERS')");
        assertThat(target.getTagIds()).containsExactly(fixture.tagId());
        assertThat(target.getConnectionParameters()).isEmpty();

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflows = workflowsByWorkflowId(
            environmentPromotionResult.targetId());

        assertThat(targetWorkflows).containsOnlyKeys(fixture.syncOrdersV2WorkflowId(), fixture.notifyWorkflowId());

        IntegrationInstanceConfigurationWorkflow syncOrdersWorkflow = targetWorkflows.get(
            fixture.syncOrdersV2WorkflowId());

        assertThat(syncOrdersWorkflow.isEnabled()).isTrue();
        assertThat(syncOrdersWorkflow.getInputs()).isEmpty();
        assertThat(syncOrdersWorkflow.getConnections()).containsExactly(
            new IntegrationInstanceConfigurationWorkflowConnection(targetConnection.getId(), "slack", "node_1"));

        IntegrationInstanceConfigurationWorkflow notifyWorkflow = targetWorkflows.get(fixture.notifyWorkflowId());

        assertThat(notifyWorkflow.isEnabled()).isFalse();
        assertThat(notifyWorkflow.getInputs()).isEmpty();
    }

    @Test
    void testPromoteWithoutMappingBindsSameNameTargetConnection() {
        Fixture fixture = createFixture();

        Connection sameNameConnection = createConnection("Slack Dev", Environment.PRODUCTION);

        createConnection("Slack Other", Environment.PRODUCTION);

        EnvironmentPromotionResult environmentPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(environmentPromotionResult.unresolvedConnectionIds()).isEmpty();

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByWorkflowId = workflowsByWorkflowId(
            environmentPromotionResult.targetId());

        IntegrationInstanceConfigurationWorkflow syncOrdersWorkflow = targetWorkflowsByWorkflowId.get(
            fixture.syncOrdersV2WorkflowId());

        assertThat(syncOrdersWorkflow.getConnections()).containsExactly(
            new IntegrationInstanceConfigurationWorkflowConnection(sameNameConnection.getId(), "slack", "node_1"));
    }

    @Test
    void testPromoteWithoutMappingAndWithoutSameNameCandidateStoresNoBinding() {
        Fixture fixture = createFixture();

        createConnection("Slack Other", Environment.PRODUCTION);

        EnvironmentPromotionResult environmentPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(environmentPromotionResult.unresolvedConnectionIds()).containsExactly(fixture.sourceConnectionId());

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByWorkflowId = workflowsByWorkflowId(
            environmentPromotionResult.targetId());

        IntegrationInstanceConfigurationWorkflow syncOrdersWorkflow = targetWorkflowsByWorkflowId.get(
            fixture.syncOrdersV2WorkflowId());

        assertThat(syncOrdersWorkflow.getConnections()).isEmpty();
    }

    @Test
    void testPromoteRequestedMappingWinsOverSameNameTargetConnection() {
        Fixture fixture = createFixture();

        createConnection("Slack Dev", Environment.PRODUCTION);

        Connection requestedConnection = createConnection("Slack Prod", Environment.PRODUCTION);

        EnvironmentPromotionResult environmentPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(
                fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION,
                Map.of(fixture.sourceConnectionId(), Objects.requireNonNull(requestedConnection.getId())), null);

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByWorkflowId = workflowsByWorkflowId(
            environmentPromotionResult.targetId());

        IntegrationInstanceConfigurationWorkflow syncOrdersWorkflow = targetWorkflowsByWorkflowId.get(
            fixture.syncOrdersV2WorkflowId());

        assertThat(syncOrdersWorkflow.getConnections()).containsExactly(
            new IntegrationInstanceConfigurationWorkflowConnection(requestedConnection.getId(), "slack", "node_1"));
    }

    @Test
    void testPromoteCreateLeavesUnmappedConnectionUnresolved() {
        Fixture fixture = createFixture();

        EnvironmentPromotionResult environmentPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(environmentPromotionResult.unresolvedConnectionIds()).containsExactly(fixture.sourceConnectionId());

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByWorkflowId = workflowsByWorkflowId(
            environmentPromotionResult.targetId());

        IntegrationInstanceConfigurationWorkflow syncOrdersWorkflow = targetWorkflowsByWorkflowId.get(
            fixture.syncOrdersV2WorkflowId());

        assertThat(syncOrdersWorkflow.getConnections()).isEmpty();
    }

    @Test
    void testPromoteUpdateKeepsTargetSettingsEnabledStateAndInputValues() {
        Fixture fixture = createFixture();

        Connection targetConnection = createConnection("Slack Prod", Environment.PRODUCTION);

        IntegrationInstanceConfiguration existingTarget = new IntegrationInstanceConfiguration();

        existingTarget.setAuthorizationType(AuthorizationType.API_KEY);
        existingTarget.setConnectionParameters(Map.of("clientId", "production-client"));
        existingTarget.setEnvironment(Environment.PRODUCTION);
        existingTarget.setIntegrationId(fixture.integrationId());
        existingTarget.setIntegrationVersion(1);
        existingTarget.setName("Orders Production");
        existingTarget.setUuid(fixture.uuid());

        existingTarget = integrationInstanceConfigurationService.create(existingTarget);

        long targetId = Objects.requireNonNull(existingTarget.getId());

        createConfigurationWorkflow(
            targetId, fixture.syncOrdersV1WorkflowId(), false, Map.of("region", "us"),
            new IntegrationInstanceConfigurationWorkflowConnection(targetConnection.getId(), "slack", "node_1"));

        integrationInstanceConfigurationService.updateEnabled(targetId, true);

        EnvironmentPromotionPreview environmentPromotionPreview = integrationInstanceConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.existingTargetId()).isEqualTo(targetId);
        assertThat(environmentPromotionPreview.warnings()).containsExactlyInAnyOrder(
            "Workflow \"Sync Orders\" input \"channel\" needs a value in Production.",
            "Workflow \"Notify\" input \"recipient\" needs a value in Production.");
        List<PromotionConnectionMapping> connectionMappings = environmentPromotionPreview.connections();

        PromotionConnectionMapping connectionMapping = connectionMappings.getFirst();

        assertThat(connectionMapping.suggestedTargetConnectionId()).isEqualTo(targetConnection.getId());

        EnvironmentPromotionResult environmentPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(environmentPromotionResult.created()).isFalse();
        assertThat(environmentPromotionResult.targetId()).isEqualTo(targetId);
        assertThat(environmentPromotionResult.unresolvedConnectionIds()).isEmpty();

        IntegrationInstanceConfiguration target = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(targetId);

        assertThat(target.getIntegrationVersion()).isEqualTo(2);
        assertThat(target.isEnabled()).isTrue();
        assertThat(target.getAuthorizationType()).isEqualTo(AuthorizationType.API_KEY);
        assertThat(target.getConnectionParameters()).isEqualTo(Map.of("clientId", "production-client"));
        assertThat(target.getName()).isEqualTo("Orders");

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflows = workflowsByWorkflowId(targetId);

        assertThat(targetWorkflows).containsOnlyKeys(fixture.syncOrdersV2WorkflowId(), fixture.notifyWorkflowId());

        IntegrationInstanceConfigurationWorkflow syncOrdersWorkflow = targetWorkflows.get(
            fixture.syncOrdersV2WorkflowId());

        assertThat(syncOrdersWorkflow.isEnabled()).isFalse();
        assertThat(syncOrdersWorkflow.getInputs()).isEqualTo(Map.of("region", "us"));
        assertThat(syncOrdersWorkflow.getConnections()).containsExactly(
            new IntegrationInstanceConfigurationWorkflowConnection(targetConnection.getId(), "slack", "node_1"));

        IntegrationInstanceConfigurationWorkflow notifyWorkflow = targetWorkflows.get(fixture.notifyWorkflowId());

        assertThat(notifyWorkflow.isEnabled()).isFalse();
        assertThat(notifyWorkflow.getInputs()).isEmpty();
    }

    @Test
    void testPromoteCreateDisablesWorkflowNeedingInternalOnlyInput() {
        Fixture fixture = createFixture();

        String provisionWorkflowId = addProvisionWorkflow(fixture);

        EnvironmentPromotionPreview environmentPromotionPreview = integrationInstanceConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.warnings()).contains(
            "Workflow \"Provision Account\" is created disabled until its internal-only input \"accountId\" is " +
                "filled in Production.");

        EnvironmentPromotionResult environmentPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(environmentPromotionResult.created()).isTrue();

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflows = workflowsByWorkflowId(
            environmentPromotionResult.targetId());

        IntegrationInstanceConfigurationWorkflow provisionWorkflow = targetWorkflows.get(provisionWorkflowId);

        assertThat(provisionWorkflow.isEnabled()).isFalse();
        assertThat(provisionWorkflow.getInputs()).isEmpty();

        IntegrationInstanceConfigurationWorkflow syncOrdersWorkflow = targetWorkflows.get(
            fixture.syncOrdersV2WorkflowId());

        assertThat(syncOrdersWorkflow.isEnabled()).isTrue();
    }

    @Test
    void testPromoteUpdateCreatesNewWorkflowNeedingInternalOnlyInputDisabled() {
        Fixture fixture = createFixture();

        EnvironmentPromotionResult firstPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        long targetId = firstPromotionResult.targetId();

        String provisionWorkflowId = addProvisionWorkflow(fixture);

        EnvironmentPromotionPreview environmentPromotionPreview = integrationInstanceConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.warnings()).contains(
            "Workflow \"Provision Account\" is created disabled until its internal-only input \"accountId\" is " +
                "filled in Production.");

        EnvironmentPromotionResult secondPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(secondPromotionResult.created()).isFalse();
        assertThat(secondPromotionResult.targetId()).isEqualTo(targetId);

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflows = workflowsByWorkflowId(targetId);

        IntegrationInstanceConfigurationWorkflow provisionWorkflow = targetWorkflows.get(provisionWorkflowId);

        assertThat(provisionWorkflow.isEnabled()).isFalse();
        assertThat(provisionWorkflow.getInputs()).isEmpty();
    }

    @Test
    void testPromoteUpdateOfEnabledTargetCreatesNewWorkflowNeedingInternalOnlyInputDisabled() {
        Fixture fixture = createFixture();

        EnvironmentPromotionResult firstPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        long targetId = firstPromotionResult.targetId();

        integrationInstanceConfigurationService.updateEnabled(targetId, true);

        Map<String, IntegrationInstanceConfigurationWorkflow> workflowsBeforePromotion =
            workflowsByWorkflowId(targetId);

        String provisionWorkflowId = addProvisionWorkflow(fixture);

        EnvironmentPromotionResult secondPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(secondPromotionResult.created()).isFalse();
        assertThat(secondPromotionResult.targetId()).isEqualTo(targetId);

        IntegrationInstanceConfiguration target = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(targetId);

        assertThat(target.isEnabled()).isTrue();

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflows = workflowsByWorkflowId(targetId);

        IntegrationInstanceConfigurationWorkflow provisionWorkflow = targetWorkflows.get(provisionWorkflowId);

        assertThat(provisionWorkflow.isEnabled()).isFalse();
        assertThat(provisionWorkflow.getInputs()).isEmpty();

        for (String workflowId : List.of(fixture.syncOrdersV2WorkflowId(), fixture.notifyWorkflowId())) {
            IntegrationInstanceConfigurationWorkflow workflowBeforePromotion = workflowsBeforePromotion.get(workflowId);
            IntegrationInstanceConfigurationWorkflow workflowAfterPromotion = targetWorkflows.get(workflowId);

            assertThat(workflowAfterPromotion.isEnabled()).isEqualTo(workflowBeforePromotion.isEnabled());
            assertThat(workflowAfterPromotion.getInputs()).isEqualTo(workflowBeforePromotion.getInputs());
        }
    }

    @Test
    void testPromoteUpdateRefusesMissingInternalOnlyInputOfExistingWorkflowAndRollsBack() {
        Fixture fixture = createFixture();

        String provisionWorkflowId = addProvisionWorkflow(fixture);

        EnvironmentPromotionResult firstPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        long targetId = firstPromotionResult.targetId();

        jdbcTemplate.update(
            "UPDATE integration_instance_configuration_workflow SET enabled = true " +
                "WHERE integration_instance_configuration_id = ? AND workflow_id = ?",
            targetId, provisionWorkflowId);

        EnvironmentPromotionPreview environmentPromotionPreview = integrationInstanceConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.warnings()).contains(
            "Workflow \"Provision Account\" needs a value for its internal-only input \"accountId\" in Production; " +
                "the promotion is refused until it is filled there.");

        jdbcTemplate.update(
            "UPDATE integration_instance_configuration SET name = ?, description = ? WHERE id = ?", "Orders Renamed",
            "Renamed description", fixture.sourceId());

        assertThatThrownBy(() -> integrationInstanceConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(IntegrationInstanceConfigurationErrorType.INTERNAL_ONLY_INPUT_MISSING.getErrorKey());

        IntegrationInstanceConfiguration target = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(targetId);

        assertThat(target.getName()).isEqualTo("Orders");
        assertThat(target.getDescription()).isEqualTo("Orders integration");

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflows = workflowsByWorkflowId(targetId);

        assertThat(targetWorkflows)
            .containsOnlyKeys(fixture.syncOrdersV2WorkflowId(), fixture.notifyWorkflowId(), provisionWorkflowId);

        IntegrationInstanceConfigurationWorkflow provisionWorkflow = targetWorkflows.get(provisionWorkflowId);

        assertThat(provisionWorkflow.isEnabled()).isTrue();
        assertThat(provisionWorkflow.getInputs()).isEmpty();
    }

    @Test
    void testPromoteRejectsMappingToNonCandidateConnection() {
        Fixture fixture = createFixture();

        Connection otherEnvironmentConnection = createConnection("Slack Staging", Environment.STAGING);

        assertThatThrownBy(() -> integrationInstanceConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION,
            Map.of(fixture.sourceConnectionId(), Objects.requireNonNull(otherEnvironmentConnection.getId())), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());

        assertThat(
            integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
                fixture.uuid(), Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testPromoteRejectsMappingToConnectedUserOwnedConnection() {
        Fixture fixture = createFixture();

        Connection connectedUserConnection = createConnection("Slack Customer", Environment.PRODUCTION);

        long connectedUserConnectionId = Objects.requireNonNull(connectedUserConnection.getId());

        jdbcTemplate.update(
            "INSERT INTO connected_user_connection (connected_user_id, connection_id) VALUES (?, ?)",
            CONNECTED_USER_ID, connectedUserConnectionId);

        assertThatThrownBy(() -> integrationInstanceConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION,
            Map.of(fixture.sourceConnectionId(), connectedUserConnectionId), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());

        assertThat(
            integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
                fixture.uuid(), Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testPromoteRejectsNonTenantAdmin() {
        Fixture fixture = createFixture();

        permissionService.setTenantAdmin(false);

        assertThatThrownBy(() -> integrationInstanceConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    void testPreviewDeletesEqualPromoteDeletes() {
        Fixture fixture = createFixture();

        long targetId = promoteAndReturnTargetId(fixture);

        String firstExtraWorkflowId = addTargetOnlyWorkflow(fixture, targetId);
        String secondExtraWorkflowId = addTargetOnlyWorkflow(fixture, targetId);

        Set<String> uuidsBeforePromotion = targetWorkflowUuids(fixture, targetId);

        EnvironmentPromotionPreview environmentPromotionPreview = integrationInstanceConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        Set<String> previewedDeleteKeys = new HashSet<>();

        for (PromotionChange change : environmentPromotionPreview.changes()) {
            if (change.kind() == Kind.DELETE) {
                previewedDeleteKeys.add(change.key());
            }
        }

        assertThat(previewedDeleteKeys).containsExactlyInAnyOrder(
            workflowUuid(fixture, firstExtraWorkflowId), workflowUuid(fixture, secondExtraWorkflowId));
        assertThat(environmentPromotionPreview.destructiveChangesHash()).isEqualTo(
            PromotionChangeHashes.destructiveChangesHash(environmentPromotionPreview.changes()));

        integrationInstanceConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(),
            environmentPromotionPreview.destructiveChangesHash());

        Set<String> removedUuids = new HashSet<>(uuidsBeforePromotion);

        removedUuids.removeAll(targetWorkflowUuids(fixture, targetId));

        assertThat(removedUuids).isEqualTo(previewedDeleteKeys);
    }

    @Test
    void testDeleteNamesAffectedConnectedUsers() {
        Fixture fixture = createFixture();

        long targetId = promoteAndReturnTargetId(fixture);

        String extraWorkflowId = addTargetOnlyWorkflow(fixture, targetId);

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflows = workflowsByWorkflowId(targetId);

        IntegrationInstanceConfigurationWorkflow extraConfigurationWorkflow = targetWorkflows.get(extraWorkflowId);
        IntegrationInstanceConfigurationWorkflow keptConfigurationWorkflow = targetWorkflows.get(
            fixture.notifyWorkflowId());

        long extraConfigurationWorkflowId = Objects.requireNonNull(extraConfigurationWorkflow.getId());
        long keptConfigurationWorkflowId = Objects.requireNonNull(keptConfigurationWorkflow.getId());

        addConnectedUser(targetId, 2001L, extraConfigurationWorkflowId, keptConfigurationWorkflowId);
        addConnectedUser(targetId, 2002L, extraConfigurationWorkflowId, keptConfigurationWorkflowId);
        addConnectedUser(targetId, 2003L, keptConfigurationWorkflowId);

        EnvironmentPromotionPreview environmentPromotionPreview = integrationInstanceConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.changes()).containsExactly(
            new PromotionChange(
                Kind.DELETE, "CONFIGURATION_WORKFLOW", workflowUuid(fixture, extraWorkflowId), "Orders",
                "Provision Account (removes it from 2 connected users)", true));

        int instanceWorkflowsBeforePromotion = countInstanceWorkflows();

        integrationInstanceConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(),
            environmentPromotionPreview.destructiveChangesHash());

        assertThat(instanceWorkflowsBeforePromotion - countInstanceWorkflows()).isEqualTo(2);
    }

    @Test
    void testUnchangedConfigurationHasNoChanges() {
        Fixture fixture = createFixture();

        EnvironmentPromotionPreview newTargetPreview = integrationInstanceConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(newTargetPreview.changes()).extracting(PromotionChange::kind)
            .containsOnly(Kind.CREATE);
        assertThat(newTargetPreview.changes()).extracting(PromotionChange::name)
            .containsExactlyInAnyOrder("Sync Orders", "Notify");
        assertThat(newTargetPreview.changes()).extracting(PromotionChange::destructive)
            .containsOnly(false);

        promoteAndReturnTargetId(fixture);

        EnvironmentPromotionPreview unchangedPreview = integrationInstanceConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(unchangedPreview.changes()).isEmpty();
        assertThat(unchangedPreview.destructiveChangesHash()).isEqualTo(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void testRenamedConfigurationIsUpdate() {
        Fixture fixture = createFixture();

        promoteAndReturnTargetId(fixture);

        jdbcTemplate.update(
            "UPDATE integration_instance_configuration SET name = ? WHERE id = ?", "Orders Renamed",
            fixture.sourceId());

        EnvironmentPromotionPreview environmentPromotionPreview = integrationInstanceConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.changes()).containsExactly(
            new PromotionChange(
                Kind.UPDATE, "INTEGRATION_INSTANCE_CONFIGURATION", String.valueOf(fixture.uuid()), null,
                "Orders Renamed", false));
        assertThat(environmentPromotionPreview.destructiveChangesHash()).isEqualTo(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void testPreviewReportsUpdateForChangedDefinitionAndCreateForNewWorkflow() {
        Fixture fixture = createFixture();

        IntegrationInstanceConfiguration existingTarget = new IntegrationInstanceConfiguration();

        existingTarget.setAuthorizationType(AuthorizationType.API_KEY);
        existingTarget.setEnvironment(Environment.PRODUCTION);
        existingTarget.setIntegrationId(fixture.integrationId());
        existingTarget.setIntegrationVersion(1);
        existingTarget.setName("Orders Production");
        existingTarget.setUuid(fixture.uuid());

        existingTarget = integrationInstanceConfigurationService.create(existingTarget);

        createConfigurationWorkflow(
            Objects.requireNonNull(existingTarget.getId()), fixture.syncOrdersV1WorkflowId(), false, Map.of());

        EnvironmentPromotionPreview environmentPromotionPreview = integrationInstanceConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.changes()).extracting(PromotionChange::kind, PromotionChange::name)
            .containsExactlyInAnyOrder(
                tuple(Kind.UPDATE, "Orders"),
                tuple(Kind.UPDATE, "Sync Orders"),
                tuple(Kind.CREATE, "Notify"));
        assertThat(environmentPromotionPreview.changes()).extracting(PromotionChange::destructive)
            .containsOnly(false);
    }

    @Test
    void testPromoteWithStaleHashWritesNothing() {
        Fixture fixture = createFixture();

        long targetId = promoteAndReturnTargetId(fixture);

        addTargetOnlyWorkflow(fixture, targetId);

        EnvironmentPromotionPreview environmentPromotionPreview = integrationInstanceConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        addTargetOnlyWorkflow(fixture, targetId);

        jdbcTemplate.update(
            "UPDATE integration_instance_configuration SET name = ? WHERE id = ?", "Orders Renamed",
            fixture.sourceId());

        Set<String> uuidsBeforePromotion = targetWorkflowUuids(fixture, targetId);

        assertThatThrownBy(() -> integrationInstanceConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(),
            environmentPromotionPreview.destructiveChangesHash()))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.PROMOTION_PREVIEW_STALE.getErrorKey());

        assertThat(targetWorkflowUuids(fixture, targetId)).isEqualTo(uuidsBeforePromotion);
        IntegrationInstanceConfiguration target = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(targetId);

        assertThat(target.getName()).isEqualTo("Orders");
    }

    private Fixture createFixture() {
        Integration integration = new Integration();

        integration.setComponentName("slack");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        long integrationId = Objects.requireNonNull(integration.getId());
        UUID syncOrdersUuid = UUID.randomUUID();

        String syncOrdersV1WorkflowId = createWorkflow(SYNC_ORDERS_V1_WORKFLOW_DEFINITION);

        integrationWorkflowService.addWorkflow(integrationId, 1, syncOrdersV1WorkflowId, syncOrdersUuid);

        integrationService.publishIntegration(integrationId, null);

        String syncOrdersV2WorkflowId = createWorkflow(SYNC_ORDERS_V2_WORKFLOW_DEFINITION);
        String notifyWorkflowId = createWorkflow(NOTIFY_WORKFLOW_DEFINITION);

        integrationWorkflowService.addWorkflow(integrationId, 2, syncOrdersV2WorkflowId, syncOrdersUuid);
        integrationWorkflowService.addWorkflow(integrationId, 2, notifyWorkflowId, UUID.randomUUID());

        integrationService.publishIntegration(integrationId, null);

        List<Tag> tags = tagService.save(List.of(new Tag("billing-" + UUID.randomUUID())));

        Tag tag = tags.getFirst();

        long tagId = Objects.requireNonNull(tag.getId());

        Connection sourceConnection = createConnection("Slack Dev", Environment.DEVELOPMENT);

        IntegrationInstanceConfiguration source = new IntegrationInstanceConfiguration();

        source.setAuthorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
        source.setConnectionParameters(Map.of("clientId", "development-client", "clientSecret", "development"));
        source.setDescription("Orders integration");
        source.setEnvironment(Environment.DEVELOPMENT);
        source.setIntegrationId(integrationId);
        source.setIntegrationVersion(2);
        source.setName("Orders");
        source.setPermissionExpression("hasRole('ORDERS')");
        source.setTagIds(List.of(tagId));

        source = integrationInstanceConfigurationService.create(source);

        long sourceId = Objects.requireNonNull(source.getId());
        long sourceConnectionId = Objects.requireNonNull(sourceConnection.getId());

        createConfigurationWorkflow(
            sourceId, syncOrdersV2WorkflowId, true, Map.of("region", "eu", "channel", "#dev"),
            new IntegrationInstanceConfigurationWorkflowConnection(sourceConnectionId, "slack", "node_1"));
        createConfigurationWorkflow(sourceId, notifyWorkflowId, false, Map.of("recipient", "dev@example.com"));

        integrationInstanceConfigurationService.updateEnabled(sourceId, true);

        return new Fixture(
            integrationId, sourceId, sourceConnectionId, tagId, source.getUuid(), syncOrdersV1WorkflowId,
            syncOrdersV2WorkflowId, notifyWorkflowId);
    }

    private String addTargetOnlyWorkflow(Fixture fixture, long targetId) {
        String extraWorkflowId = createWorkflow(INTERNAL_ONLY_WORKFLOW_DEFINITION);

        integrationWorkflowService.addWorkflow(fixture.integrationId(), 2, extraWorkflowId, UUID.randomUUID());

        createConfigurationWorkflow(targetId, extraWorkflowId, false, Map.of());

        return extraWorkflowId;
    }

    private void addConnectedUser(
        long configurationId, long connectedUserId, long... configurationWorkflowIds) {

        Connection connection = createConnection("Slack User " + connectedUserId, Environment.PRODUCTION);

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            connectedUserId, Objects.requireNonNull(connection.getId()), configurationId);

        for (long configurationWorkflowId : configurationWorkflowIds) {
            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                Objects.requireNonNull(integrationInstance.getId()), configurationWorkflowId);
        }
    }

    private int countInstanceWorkflows() {
        Integer count =
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM integration_instance_workflow", Integer.class);

        return Objects.requireNonNull(count);
    }

    private long promoteAndReturnTargetId(Fixture fixture) {
        EnvironmentPromotionResult environmentPromotionResult = integrationInstanceConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        return environmentPromotionResult.targetId();
    }

    private Set<String> targetWorkflowUuids(Fixture fixture, long targetId) {
        Set<String> workflowUuids = new HashSet<>();

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByWorkflowId =
            workflowsByWorkflowId(targetId);

        for (String workflowId : targetWorkflowsByWorkflowId.keySet()) {
            workflowUuids.add(workflowUuid(fixture, workflowId));
        }

        return workflowUuids;
    }

    private String workflowUuid(Fixture fixture, String workflowId) {
        for (IntegrationWorkflow integrationWorkflow : integrationWorkflowService.getIntegrationWorkflows(
            fixture.integrationId())) {

            if (Objects.equals(integrationWorkflow.getWorkflowId(), workflowId)) {

                return integrationWorkflow.getUuidAsString();
            }
        }

        throw new IllegalStateException("No integration workflow for " + workflowId);
    }

    private String addProvisionWorkflow(Fixture fixture) {
        String provisionWorkflowId = createWorkflow(INTERNAL_ONLY_WORKFLOW_DEFINITION);

        integrationWorkflowService.addWorkflow(fixture.integrationId(), 2, provisionWorkflowId, UUID.randomUUID());

        createConfigurationWorkflow(
            fixture.sourceId(), provisionWorkflowId, true, Map.of("accountId", "development-account"));

        return provisionWorkflowId;
    }

    private void createConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId, boolean enabled, Map<String, Object> inputs,
        IntegrationInstanceConfigurationWorkflowConnection... connections) {

        IntegrationInstanceConfigurationWorkflow configurationWorkflow = new IntegrationInstanceConfigurationWorkflow(
            List.of(connections), inputs, workflowId);

        configurationWorkflow.setEnabled(enabled);
        configurationWorkflow.setIntegrationInstanceConfigurationId(integrationInstanceConfigurationId);

        integrationInstanceConfigurationWorkflowService.create(configurationWorkflow);
    }

    private Connection createConnection(String name, Environment environment) {
        return connectionService.create(
            null, "slack", 1, environment.ordinal(), name, Map.of(), PlatformType.EMBEDDED);
    }

    private String createWorkflow(String definition) {
        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return workflow.getId();
    }

    private void deleteTestData() {
        jdbcTemplate.update("DELETE FROM connected_user_connection");
        jdbcTemplate.update("DELETE FROM integration_instance_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_workflow_connection");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_tag");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM integration_workflow");
        jdbcTemplate.update("DELETE FROM integration_version");
        jdbcTemplate.update("DELETE FROM integration");
        jdbcTemplate.update("DELETE FROM connection");
        jdbcTemplate.update("DELETE FROM tag");
    }

    private Map<String, IntegrationInstanceConfigurationWorkflow> workflowsByWorkflowId(
        long integrationInstanceConfigurationId) {

        List<IntegrationInstanceConfigurationWorkflow> configurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfigurationId);

        return configurationWorkflows.stream()
            .collect(
                Collectors.toMap(
                    IntegrationInstanceConfigurationWorkflow::getWorkflowId,
                    configurationWorkflow -> configurationWorkflow));
    }

    private record Fixture(
        long integrationId, long sourceId, long sourceConnectionId, long tagId, UUID uuid,
        String syncOrdersV1WorkflowId, String syncOrdersV2WorkflowId, String notifyWorkflowId) {
    }
}
