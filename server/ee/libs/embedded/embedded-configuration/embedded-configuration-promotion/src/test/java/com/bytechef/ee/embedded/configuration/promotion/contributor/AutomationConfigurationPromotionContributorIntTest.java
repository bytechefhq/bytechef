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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionChange.Kind;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionCandidate;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.test.config.PromotionIntTestSharedMocks;
import com.bytechef.ee.automation.promotion.test.config.RecordingPermissionService;
import com.bytechef.ee.automation.promotion.util.PromotionChangeHashes;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.promotion.config.EmbeddedConfigurationPromotionIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.promotion.config.EmbeddedConfigurationPromotionIntTestMocks;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

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
class AutomationConfigurationPromotionContributorIntTest {

    private static final long CONNECTED_USER_ID = 1050L;

    private static final long TARGET_CONNECTED_USER_ID = 1051L;

    private static final String NOTIFY_WORKFLOW_DEFINITION = """
        {
            "label": "Notify",
            "inputs": [
                {"name": "recipient", "label": "Recipient", "type": "string", "required": false, "internalOnly": true}
            ],
            "triggers": [],
            "tasks": []
        }""";

    private static final String PROVISION_WORKFLOW_DEFINITION = """
        {
            "label": "Provision Account",
            "inputs": [
                {"name": "accountId", "label": "Account", "type": "string", "required": true, "internalOnly": true}
            ],
            "triggers": [],
            "tasks": []
        }""";

    private static final String TYPED_INPUT_STRING_WORKFLOW_DEFINITION = """
        {
            "label": "Typed",
            "inputs": [
                {"name": "limit", "label": "Limit", "type": "string", "required": false, "internalOnly": true},
                {"name": "mode", "label": "Mode", "type": "string", "required": false, "internalOnly": true}
            ],
            "triggers": [],
            "tasks": []
        }""";

    private static final String TYPED_INPUT_NUMBER_WORKFLOW_DEFINITION = """
        {
            "label": "Typed",
            "inputs": [
                {"name": "limit", "label": "Limit", "type": "number", "required": false, "internalOnly": true},
                {"name": "mode", "label": "Mode", "type": "string", "required": false, "internalOnly": true}
            ],
            "triggers": [],
            "tasks": []
        }""";

    private static final String TYPED_INPUT_REMOVED_WORKFLOW_DEFINITION = """
        {
            "label": "Typed",
            "inputs": [
                {"name": "mode", "label": "Mode", "type": "string", "required": false, "internalOnly": true}
            ],
            "triggers": [],
            "tasks": []
        }""";

    private static final String TYPED_INPUT_NOT_INTERNAL_ONLY_WORKFLOW_DEFINITION = """
        {
            "label": "Typed",
            "inputs": [
                {"name": "limit", "label": "Limit", "type": "string", "required": false, "internalOnly": true},
                {"name": "mode", "label": "Mode", "type": "string", "required": false, "internalOnly": false}
            ],
            "triggers": [],
            "tasks": []
        }""";

    private static final String SYNC_ORDERS_V1_WORKFLOW_DEFINITION = """
        {
            "label": "Sync Orders",
            "inputs": [
                {"name": "apiKey", "label": "API Key", "type": "string", "required": false, "internalOnly": true}
            ],
            "triggers": [],
            "tasks": [{"name": "slack_1", "type": "slack/v1/sendMessage", "parameters": {}}]
        }""";

    private static final String SYNC_ORDERS_V2_WORKFLOW_DEFINITION = """
        {
            "label": "Sync Orders",
            "inputs": [
                {"name": "apiKey", "label": "API Key", "type": "string", "required": false, "internalOnly": true},
                {"name": "region", "label": "Region", "type": "string", "required": false, "internalOnly": true}
            ],
            "triggers": [],
            "tasks": [{"name": "slack_1", "type": "slack/v1/sendMessage", "parameters": {}}]
        }""";

    @Autowired
    private EnvironmentPromotionContributor automationConfigurationPromotionContributor;

    @Autowired
    private AutomationProjectConfigurationService automationProjectConfigurationService;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @MockitoSpyBean
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingPermissionService permissionService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        permissionService.reset();

        deleteTestData();

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenReturn(List.of(new ComponentConnection("slack", 1, "slack_1", "slack", true)));
    }

    @AfterEach
    void afterEach() {
        deleteTestData();

        permissionService.reset();
    }

    @Test
    void testPromoteCreatesDisabledCounterpartWithoutInputValues() {
        Fixture fixture = createFixture();

        Connection targetConnection = createConnection("Slack Prod", Environment.PRODUCTION);

        EnvironmentPromotionResult environmentPromotionResult = automationConfigurationPromotionContributor
            .promote(
                fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION,
                Map.of(fixture.sourceConnectionId(), Objects.requireNonNull(targetConnection.getId())), null);

        assertThat(environmentPromotionResult.created()).isTrue();
        assertThat(environmentPromotionResult.unresolvedConnectionIds()).isEmpty();

        AutomationProjectConfiguration target = automationProjectConfigurationService.getConfiguration(
            environmentPromotionResult.targetId());

        assertThat(target.getUuid()).isEqualTo(fixture.uuid());
        assertThat(target.getEnvironmentId()).isEqualTo(Environment.PRODUCTION.ordinal());
        assertThat(target.isEnabled()).isFalse();
        assertThat(target.getProjectId()).isEqualTo(fixture.projectId());
        assertThat(target.getProjectVersion()).isEqualTo(2);
        assertThat(target.isAutomationHubVisible()).isFalse();
        assertThat(target.getName()).isEqualTo("Orders");
        assertThat(target.getPermissionExpression()).isEqualTo("hasRole('ORDERS')");

        AutomationProjectConfigurationWorkflow syncOrdersRow = workflowRow(target, fixture.syncOrdersUuid());

        assertThat(syncOrdersRow.isEnabled()).isTrue();
        assertThat(syncOrdersRow.getProvidedInputs()).isEmpty();
        assertThat(syncOrdersRow.getProvidedConnections()).isEqualTo(Map.of("slack", targetConnection.getId()));

        AutomationProjectConfigurationWorkflow notifyRow = workflowRow(target, fixture.notifyUuid());

        assertThat(notifyRow.isEnabled()).isFalse();
        assertThat(notifyRow.getProvidedInputs()).isEmpty();
        assertThat(connectedUserAssignmentCount(environmentPromotionResult.targetId())).isZero();
        assertThat(connectedUserAssignmentCount(fixture.sourceId())).isEqualTo(1);
    }

    @Test
    void testPromoteWithoutMappingBindsSameNameTargetConnection() {
        Fixture fixture = createFixture();

        Connection sameNameConnection = createConnection("Slack Dev", Environment.PRODUCTION);

        createConnection("Slack Other", Environment.PRODUCTION);

        EnvironmentPromotionResult environmentPromotionResult = automationConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(environmentPromotionResult.unresolvedConnectionIds()).isEmpty();

        AutomationProjectConfiguration target = automationProjectConfigurationService.getConfiguration(
            environmentPromotionResult.targetId());

        AutomationProjectConfigurationWorkflow syncOrdersRow = workflowRow(target, fixture.syncOrdersUuid());

        assertThat(syncOrdersRow.getProvidedConnections()).isEqualTo(Map.of("slack", sameNameConnection.getId()));
    }

    @Test
    void testPromoteWithoutMappingAndWithoutSameNameCandidateLeavesConnectionUnresolved() {
        Fixture fixture = createFixture();

        createConnection("Slack Other", Environment.PRODUCTION);

        EnvironmentPromotionResult environmentPromotionResult = automationConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(environmentPromotionResult.unresolvedConnectionIds()).containsExactly(fixture.sourceConnectionId());

        AutomationProjectConfiguration target = automationProjectConfigurationService.getConfiguration(
            environmentPromotionResult.targetId());

        AutomationProjectConfigurationWorkflow syncOrdersRow = workflowRow(target, fixture.syncOrdersUuid());

        assertThat(syncOrdersRow.getProvidedConnections()).isEmpty();
    }

    @Test
    void testPromoteRequestedMappingWinsOverSameNameTargetConnection() {
        Fixture fixture = createFixture();

        createConnection("Slack Dev", Environment.PRODUCTION);

        Connection requestedConnection = createConnection("Slack Prod", Environment.PRODUCTION);

        EnvironmentPromotionResult environmentPromotionResult = automationConfigurationPromotionContributor
            .promote(
                fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION,
                Map.of(fixture.sourceConnectionId(), Objects.requireNonNull(requestedConnection.getId())), null);

        AutomationProjectConfiguration target = automationProjectConfigurationService.getConfiguration(
            environmentPromotionResult.targetId());

        AutomationProjectConfigurationWorkflow syncOrdersRow = workflowRow(target, fixture.syncOrdersUuid());

        assertThat(syncOrdersRow.getProvidedConnections()).isEqualTo(Map.of("slack", requestedConnection.getId()));
    }

    @Test
    void testPreviewListsInputsNeedingValuesAndEmbeddedCandidates() {
        Fixture fixture = createFixture();

        Connection targetConnection = createConnection("Slack Dev", Environment.PRODUCTION);

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.existingTargetId()).isNull();
        assertThat(environmentPromotionPreview.warnings()).containsExactlyInAnyOrder(
            "The promoted automation configuration is created disabled; fill in its inputs in Production before " +
                "enabling it.",
            "Workflow \"Sync Orders\" input \"apiKey\" needs a value in Production.",
            "Workflow \"Sync Orders\" input \"region\" needs a value in Production.",
            "Workflow \"Notify\" input \"recipient\" needs a value in Production.");

        List<PromotionConnectionMapping> connectionMappings = environmentPromotionPreview.connections();

        assertThat(connectionMappings).hasSize(1);

        PromotionConnectionMapping connectionMapping = connectionMappings.getFirst();

        assertThat(connectionMapping.sourceConnectionId()).isEqualTo(fixture.sourceConnectionId());
        assertThat(connectionMapping.suggestedTargetConnectionId()).isEqualTo(targetConnection.getId());
        assertThat(connectionMapping.targetCandidates()).containsExactly(
            new PromotionConnectionCandidate(Objects.requireNonNull(targetConnection.getId()), "Slack Dev"));
    }

    @Test
    void testPromoteUpdateKeepsTargetInputsAndAddsNewWorkflowsEmpty() {
        Fixture fixture = createFixture();

        Connection targetConnection = createConnection("Slack Prod", Environment.PRODUCTION);

        AutomationProjectConfiguration existingTarget = automationProjectConfigurationService.create(
            fixture.projectId(), Environment.PRODUCTION.ordinal(), "Orders Production", null, 1, false, true,
            fixture.uuid());

        long targetId = Objects.requireNonNull(existingTarget.getId());

        automationProjectConfigurationService.updateWorkflowProvidedValues(
            targetId, fixture.syncOrdersUuid(), Map.of("slack", Objects.requireNonNull(targetConnection.getId())),
            Map.of("apiKey", "production-key"));
        automationProjectConfigurationService.updateEnabled(targetId, true);

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.existingTargetId()).isEqualTo(targetId);
        assertThat(environmentPromotionPreview.warnings()).containsExactlyInAnyOrder(
            "Workflow \"Sync Orders\" input \"region\" needs a value in Production.",
            "Workflow \"Notify\" input \"recipient\" needs a value in Production.");

        List<PromotionConnectionMapping> connectionMappings = environmentPromotionPreview.connections();

        PromotionConnectionMapping connectionMapping = connectionMappings.getFirst();

        assertThat(connectionMapping.suggestedTargetConnectionId()).isEqualTo(targetConnection.getId());

        EnvironmentPromotionResult environmentPromotionResult = automationConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(environmentPromotionResult.created()).isFalse();
        assertThat(environmentPromotionResult.targetId()).isEqualTo(targetId);
        assertThat(environmentPromotionResult.unresolvedConnectionIds()).isEmpty();

        AutomationProjectConfiguration target = automationProjectConfigurationService.getConfiguration(targetId);

        assertThat(target.getProjectVersion()).isEqualTo(2);
        assertThat(target.getName()).isEqualTo("Orders");
        assertThat(target.getPermissionExpression()).isEqualTo("hasRole('ORDERS')");
        assertThat(target.isEnabled()).isTrue();
        assertThat(target.isAutomationHubVisible()).isFalse();

        AutomationProjectConfigurationWorkflow syncOrdersRow = workflowRow(target, fixture.syncOrdersUuid());

        assertThat(syncOrdersRow.getProvidedInputs()).isEqualTo(Map.of("apiKey", "production-key"));
        assertThat(syncOrdersRow.getProvidedConnections()).isEqualTo(Map.of("slack", targetConnection.getId()));

        AutomationProjectConfigurationWorkflow notifyRow = workflowRow(target, fixture.notifyUuid());

        assertThat(notifyRow.isEnabled()).isFalse();
        assertThat(notifyRow.getProvidedInputs()).isEmpty();
    }

    @Test
    void testPromoteUpdateCreatesNewWorkflowNeedingInternalOnlyInputDisabled() {
        Fixture fixture = createFixture();

        AutomationProjectConfiguration existingTarget = automationProjectConfigurationService.create(
            fixture.projectId(), Environment.PRODUCTION.ordinal(), "Orders Production", null, 1, false, true,
            fixture.uuid());

        long targetId = Objects.requireNonNull(existingTarget.getId());

        UUID provisionUuid = addProvisionWorkflow(fixture);

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.warnings()).contains(
            "Workflow \"Provision Account\" is created disabled until its internal-only input \"accountId\" is " +
                "filled in Production.");

        EnvironmentPromotionResult environmentPromotionResult = automationConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(environmentPromotionResult.created()).isFalse();
        assertThat(environmentPromotionResult.targetId()).isEqualTo(targetId);

        AutomationProjectConfiguration target = automationProjectConfigurationService.getConfiguration(targetId);

        assertThat(target.getProjectVersion()).isEqualTo(2);

        AutomationProjectConfigurationWorkflow provisionRow = workflowRow(target, provisionUuid);

        assertThat(provisionRow.isEnabled()).isFalse();
        assertThat(provisionRow.getProvidedInputs()).isEmpty();
    }

    @Test
    void testPromoteUpdateOfEnabledTargetCreatesNewWorkflowNeedingInternalOnlyInputDisabled() {
        Fixture fixture = createFixture();

        Connection targetConnection = createConnection("Slack Prod", Environment.PRODUCTION);

        AutomationProjectConfiguration existingTarget = automationProjectConfigurationService.create(
            fixture.projectId(), Environment.PRODUCTION.ordinal(), "Orders Production", null, 1, false, true,
            fixture.uuid());

        long targetId = Objects.requireNonNull(existingTarget.getId());

        automationProjectConfigurationService.updateWorkflowProvidedValues(
            targetId, fixture.syncOrdersUuid(), Map.of("slack", Objects.requireNonNull(targetConnection.getId())),
            Map.of("apiKey", "production-key"));
        automationProjectConfigurationService.updateEnabled(targetId, true);

        UUID provisionUuid = addProvisionWorkflow(fixture);

        EnvironmentPromotionResult environmentPromotionResult = automationConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(environmentPromotionResult.created()).isFalse();
        assertThat(environmentPromotionResult.targetId()).isEqualTo(targetId);

        AutomationProjectConfiguration target = automationProjectConfigurationService.getConfiguration(targetId);

        assertThat(target.isEnabled()).isTrue();
        assertThat(target.getProjectVersion()).isEqualTo(2);

        AutomationProjectConfigurationWorkflow provisionRow = workflowRow(target, provisionUuid);

        assertThat(provisionRow.isEnabled()).isFalse();
        assertThat(provisionRow.getProvidedInputs()).isEmpty();

        AutomationProjectConfigurationWorkflow syncOrdersRow = workflowRow(target, fixture.syncOrdersUuid());

        assertThat(syncOrdersRow.isEnabled()).isTrue();
        assertThat(syncOrdersRow.getProvidedInputs()).isEqualTo(Map.of("apiKey", "production-key"));
        assertThat(syncOrdersRow.getProvidedConnections()).isEqualTo(Map.of("slack", targetConnection.getId()));
    }

    @Test
    void testPromoteCreateRefusedByFacadeCreatesNothing() {
        Fixture fixture = createFixture();

        stubProjectVersions(fixture.projectId(), ProjectVersion.Status.PUBLISHED, ProjectVersion.Status.DRAFT);

        assertThatThrownBy(() -> automationConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(AutomationProjectConfigurationErrorType.PROJECT_VERSION_NOT_PUBLISHED.getErrorKey());

        assertThat(
            automationProjectConfigurationService.fetchConfiguration(
                fixture.uuid(), Environment.PRODUCTION.ordinal())).isEmpty();
    }

    @Test
    void testPromoteUpdateSurfacesFacadeRefusalForExistingWorkflowAndRollsBack() {
        Fixture fixture = createFixture();

        UUID provisionUuid = addProvisionWorkflow(fixture);

        AutomationProjectConfiguration existingTarget = automationProjectConfigurationService.create(
            fixture.projectId(), Environment.PRODUCTION.ordinal(), "Orders Production", null, 2, false, true,
            fixture.uuid());

        long targetId = Objects.requireNonNull(existingTarget.getId());

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.warnings()).contains(
            "Workflow \"Provision Account\" needs a value for its internal-only input \"accountId\" in Production; " +
                "the promotion is refused until it is filled there.");

        assertThatThrownBy(() -> automationConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(AutomationProjectConfigurationErrorType.INTERNAL_ONLY_INPUT_MISSING.getErrorKey());

        AutomationProjectConfiguration target = automationProjectConfigurationService.getConfiguration(targetId);

        assertThat(target.getName()).isEqualTo("Orders Production");
        assertThat(target.fetchWorkflow(provisionUuid)).isEmpty();
    }

    @Test
    void testPromoteCreateDisablesWorkflowNeedingInternalOnlyInput() {
        Fixture fixture = createFixture();

        UUID provisionUuid = addProvisionWorkflow(fixture);

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.warnings()).contains(
            "Workflow \"Provision Account\" is created disabled until its internal-only input \"accountId\" is " +
                "filled in Production.");

        EnvironmentPromotionResult environmentPromotionResult = automationConfigurationPromotionContributor
            .promote(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        AutomationProjectConfiguration target = automationProjectConfigurationService.getConfiguration(
            environmentPromotionResult.targetId());

        AutomationProjectConfigurationWorkflow provisionRow = workflowRow(target, provisionUuid);

        assertThat(provisionRow.isEnabled()).isFalse();
        assertThat(provisionRow.getProvidedInputs()).isEmpty();
        AutomationProjectConfigurationWorkflow syncOrdersRow = workflowRow(target, fixture.syncOrdersUuid());

        assertThat(syncOrdersRow.isEnabled()).isTrue();
    }

    @Test
    void testPromoteRefusesNameClashWithAnotherConfiguration() {
        Fixture fixture = createFixture();

        automationProjectConfigurationService.create(
            fixture.projectId(), Environment.PRODUCTION.ordinal(), "Orders", null, 1, false, true,
            UUID.randomUUID());

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.warnings()).contains(
            "An automation configuration named \"Orders\" already exists in Production; rename one of them " +
                "before promoting.");

        assertThatThrownBy(() -> automationConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT.getErrorKey());

        assertThat(
            automationProjectConfigurationService.fetchConfiguration(
                fixture.uuid(), Environment.PRODUCTION.ordinal())).isEmpty();
    }

    @Test
    void testPromoteRejectsMappingToNonCandidateConnection() {
        Fixture fixture = createFixture();

        Connection otherEnvironmentConnection = createConnection("Slack Staging", Environment.STAGING);

        assertThatThrownBy(() -> automationConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION,
            Map.of(fixture.sourceConnectionId(), Objects.requireNonNull(otherEnvironmentConnection.getId())), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());

        assertThat(
            automationProjectConfigurationService.fetchConfiguration(
                fixture.uuid(), Environment.PRODUCTION.ordinal())).isEmpty();
    }

    @Test
    void testPromoteRejectsMappingToConnectedUserOwnedConnection() {
        Fixture fixture = createFixture();

        Connection connectedUserConnection = createConnection("Slack Customer", Environment.PRODUCTION);

        long connectedUserConnectionId = Objects.requireNonNull(connectedUserConnection.getId());

        jdbcTemplate.update(
            "INSERT INTO connected_user_connection (connected_user_id, connection_id) VALUES (?, ?)",
            CONNECTED_USER_ID, connectedUserConnectionId);

        assertThatThrownBy(() -> automationConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION,
            Map.of(fixture.sourceConnectionId(), connectedUserConnectionId), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());

        assertThat(
            automationProjectConfigurationService.fetchConfiguration(
                fixture.uuid(), Environment.PRODUCTION.ordinal())).isEmpty();
    }

    @Test
    void testPromoteRejectsNonTenantAdmin() {
        Fixture fixture = createFixture();

        permissionService.setTenantAdmin(false);

        assertThatThrownBy(() -> automationConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    void testNewWorkflowInVersionIsCreate() {
        Fixture fixture = createFixture();

        AutomationProjectConfiguration target = createOlderTarget(fixture);

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.existingTargetId()).isEqualTo(target.getId());
        assertThat(environmentPromotionPreview.changes()).containsExactlyInAnyOrder(
            new PromotionChange(
                Kind.UPDATE, "AUTOMATION_CONFIGURATION", String.valueOf(fixture.uuid()), null, "Orders", false),
            new PromotionChange(
                Kind.CREATE, "CONFIGURATION_WORKFLOW", String.valueOf(fixture.notifyUuid()), "Orders", "Notify",
                false),
            new PromotionChange(
                Kind.UPDATE, "CONFIGURATION_WORKFLOW", String.valueOf(fixture.syncOrdersUuid()), "Orders",
                "Sync Orders", false));
        assertThat(environmentPromotionPreview.destructiveChangesHash()).isEqualTo(
            PromotionChangeHashes.destructiveChangesHash(List.of()));
    }

    @Test
    void testFreshTargetReportsEveryWorkflowAsCreate() {
        Fixture fixture = createFixture();

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.changes())
            .extracting(PromotionChange::kind, PromotionChange::key, PromotionChange::destructive)
            .containsExactlyInAnyOrder(
                tuple(Kind.CREATE, String.valueOf(fixture.syncOrdersUuid()), false),
                tuple(Kind.CREATE, String.valueOf(fixture.notifyUuid()), false));
    }

    @Test
    void testInputDroppedByTypeChangeIsDestructive() {
        Fixture fixture = createFixture();

        UUID typedUuid = UUID.randomUUID();

        AutomationProjectConfiguration target =
            createTypedTarget(
                fixture, typedUuid, TYPED_INPUT_NUMBER_WORKFLOW_DEFINITION, Map.of("limit", "10", "mode", "fast"));

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        PromotionChange droppedInput = new PromotionChange(
            Kind.UPDATE, "WORKFLOW_INPUT", typedUuid + ":limit", "Typed", "limit", true);

        assertThat(environmentPromotionPreview.changes()).contains(
            droppedInput,
            new PromotionChange(
                Kind.UPDATE, "CONFIGURATION_WORKFLOW", typedUuid.toString(), "Orders", "Typed", false));
        assertThat(
            environmentPromotionPreview.changes()
                .stream()
                .filter(change -> "WORKFLOW_INPUT".equals(change.assetType()))
                .toList()).containsExactly(droppedInput);
        assertThat(environmentPromotionPreview.destructiveChangesHash()).isEqualTo(
            PromotionChangeHashes.destructiveChangesHash(List.of(droppedInput)));

        automationConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(),
            environmentPromotionPreview.destructiveChangesHash());

        AutomationProjectConfiguration promoted = automationProjectConfigurationService.getConfiguration(
            Objects.requireNonNull(target.getId()));

        AutomationProjectConfigurationWorkflow typedWorkflowRow = workflowRow(promoted, typedUuid);

        assertThat(typedWorkflowRow.getProvidedInputs()).isEqualTo(Map.of("mode", "fast"));
    }

    @Test
    void testInputRemovedFromNewWorkflowIsDestructive() {
        assertDroppedInputsMatchPromotion(TYPED_INPUT_REMOVED_WORKFLOW_DEFINITION, "limit", Map.of("mode", "fast"));
    }

    @Test
    void testInputNoLongerInternalOnlyIsDestructive() {
        assertDroppedInputsMatchPromotion(
            TYPED_INPUT_NOT_INTERNAL_ONLY_WORKFLOW_DEFINITION, "mode", Map.of("limit", "10"));
    }

    private void assertDroppedInputsMatchPromotion(
        String sourceDefinition, String droppedInputName, Map<String, Object> expectedKeptInputs) {

        Fixture fixture = createFixture();

        UUID typedUuid = UUID.randomUUID();

        AutomationProjectConfiguration target = createTypedTarget(
            fixture, typedUuid, sourceDefinition, Map.of("limit", "10", "mode", "fast"));

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        PromotionChange droppedInput = new PromotionChange(
            Kind.UPDATE, "WORKFLOW_INPUT", typedUuid + ":" + droppedInputName, "Typed", droppedInputName, true);

        assertThat(
            environmentPromotionPreview.changes()
                .stream()
                .filter(change -> "WORKFLOW_INPUT".equals(change.assetType()))
                .toList()).containsExactly(droppedInput);
        assertThat(environmentPromotionPreview.destructiveChangesHash()).isEqualTo(
            PromotionChangeHashes.destructiveChangesHash(List.of(droppedInput)));

        automationConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(),
            environmentPromotionPreview.destructiveChangesHash());

        AutomationProjectConfiguration promoted = automationProjectConfigurationService.getConfiguration(
            Objects.requireNonNull(target.getId()));

        AutomationProjectConfigurationWorkflow typedWorkflowRow = workflowRow(promoted, typedUuid);

        assertThat(typedWorkflowRow.getProvidedInputs()).isEqualTo(expectedKeptInputs);
    }

    @Test
    void testUnchangedConfigurationHasNoChanges() {
        Fixture fixture = createFixture();

        promoteAndReturnTargetId(fixture);

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.changes()).isEmpty();
        assertThat(environmentPromotionPreview.destructiveChangesHash()).isEqualTo(
            PromotionChangeHashes.destructiveChangesHash(List.of()));
    }

    @Test
    void testRenamedConfigurationIsUpdate() {
        Fixture fixture = createFixture();

        promoteAndReturnTargetId(fixture);

        AutomationProjectConfiguration source = automationProjectConfigurationService.getConfiguration(
            fixture.sourceId());

        automationProjectConfigurationService.update(
            fixture.sourceId(), "Orders Renamed", source.getPermissionExpression(), source.getProjectVersion(),
            source.isEnabled(), source.isAutomationHubVisible());

        EnvironmentPromotionPreview environmentPromotionPreview = automationConfigurationPromotionContributor
            .preview(fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(environmentPromotionPreview.changes()).containsExactly(
            new PromotionChange(
                Kind.UPDATE, "AUTOMATION_CONFIGURATION", String.valueOf(fixture.uuid()), null, "Orders Renamed",
                false));
        assertThat(environmentPromotionPreview.destructiveChangesHash()).isEqualTo(
            PromotionChangeHashes.destructiveChangesHash(List.of()));
    }

    @Test
    void testPreviewOfRefusedDowngradeFails() {
        Fixture fixture = createFixture();

        long targetId = promoteAndReturnTargetId(fixture);

        AutomationProjectConfiguration source = automationProjectConfigurationService.getConfiguration(
            fixture.sourceId());

        automationProjectConfigurationService.update(
            fixture.sourceId(), source.getName(), source.getPermissionExpression(), 1, source.isEnabled(),
            source.isAutomationHubVisible());

        jdbcTemplate.update(
            """
                INSERT INTO connected_user_project_configuration (connected_user_id, project_id,
                    automation_project_configuration_id, created_by, created_date, last_modified_by,
                    last_modified_date, version)
                VALUES (?, ?, ?, 'system', now(), 'system', now(), 0)
                """,
            TARGET_CONNECTED_USER_ID, fixture.projectId(), targetId);

        doReturn(
            List.of(
                new ConnectedUserWorkflowReferenceDTO(
                    String.valueOf(fixture.notifyUuid()),
                    TARGET_CONNECTED_USER_ID, "user-1", Environment.PRODUCTION.name(), true, false, null, null, null)))
                        .when(connectedUserWorkflowReferenceFacade)
                        .getReferences(anySet());

        assertThatThrownBy(() -> automationConfigurationPromotionContributor.preview(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(AutomationProjectConfigurationErrorType.VERSION_LACKS_REFERENCED_WORKFLOWS.getErrorKey());
    }

    @Test
    void testPromoteWithStaleHashWritesNothing() {
        Fixture fixture = createFixture();

        UUID typedUuid = UUID.randomUUID();

        AutomationProjectConfiguration target = createTypedTarget(
            fixture, typedUuid, TYPED_INPUT_NUMBER_WORKFLOW_DEFINITION, Map.of("limit", "10"));

        long targetId = Objects.requireNonNull(target.getId());

        assertThatThrownBy(() -> automationConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(),
            PromotionChangeHashes.destructiveChangesHash(List.of())))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.PROMOTION_PREVIEW_STALE.getErrorKey());

        AutomationProjectConfiguration unchanged = automationProjectConfigurationService.getConfiguration(targetId);

        assertThat(unchanged.getName()).isEqualTo("Orders Production");
        assertThat(unchanged.getProjectVersion()).isEqualTo(1);
        AutomationProjectConfigurationWorkflow typedWorkflowRow = workflowRow(unchanged, typedUuid);

        assertThat(typedWorkflowRow.getProvidedInputs()).isEqualTo(Map.of("limit", "10"));
    }

    private long promoteAndReturnTargetId(Fixture fixture) {
        EnvironmentPromotionResult environmentPromotionResult = automationConfigurationPromotionContributor.promote(
            fixture.sourceId(), Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        return environmentPromotionResult.targetId();
    }

    private AutomationProjectConfiguration createOlderTarget(Fixture fixture) {
        return automationProjectConfigurationService.create(
            fixture.projectId(), Environment.PRODUCTION.ordinal(), "Orders Production", null, 1, false, true,
            fixture.uuid());
    }

    private AutomationProjectConfiguration createTypedTarget(
        Fixture fixture, UUID typedUuid, String sourceDefinition, Map<String, Object> targetProvidedInputs) {

        String stringWorkflowId = createWorkflow(TYPED_INPUT_STRING_WORKFLOW_DEFINITION);
        String numberWorkflowId = createWorkflow(sourceDefinition);

        List<ProjectWorkflow> versionOneWorkflows = new ArrayList<>(
            projectWorkflowService.getProjectWorkflows(fixture.projectId(), 1));

        versionOneWorkflows.add(new ProjectWorkflow(fixture.projectId(), 1, stringWorkflowId, typedUuid));

        stubProjectWorkflows(fixture.projectId(), 1, versionOneWorkflows);

        List<ProjectWorkflow> versionTwoWorkflows = new ArrayList<>(
            projectWorkflowService.getProjectWorkflows(fixture.projectId(), 2));

        versionTwoWorkflows.add(new ProjectWorkflow(fixture.projectId(), 2, numberWorkflowId, typedUuid));

        stubProjectWorkflows(fixture.projectId(), 2, versionTwoWorkflows);

        AutomationProjectConfiguration target = createOlderTarget(fixture);

        automationProjectConfigurationService.updateWorkflowProvidedValues(
            Objects.requireNonNull(target.getId()), typedUuid, Map.of(), targetProvidedInputs);

        return target;
    }

    private UUID addProvisionWorkflow(Fixture fixture) {
        UUID provisionUuid = UUID.randomUUID();

        String provisionWorkflowId = createWorkflow(PROVISION_WORKFLOW_DEFINITION);

        List<ProjectWorkflow> versionTwoWorkflows = new ArrayList<>(
            projectWorkflowService.getProjectWorkflows(fixture.projectId(), 2));

        versionTwoWorkflows.add(new ProjectWorkflow(fixture.projectId(), 2, provisionWorkflowId, provisionUuid));

        stubProjectWorkflows(fixture.projectId(), 2, versionTwoWorkflows);

        automationProjectConfigurationService.updateWorkflowProvidedValues(
            fixture.sourceId(), provisionUuid, Map.of(), Map.of("accountId", "development-account"));

        return provisionUuid;
    }

    private int connectedUserAssignmentCount(long configurationId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM connected_user_project_configuration WHERE automation_project_configuration_id = ?",
            Integer.class, configurationId);

        return count == null ? 0 : count;
    }

    private Connection createConnection(String name, Environment environment) {
        return connectionService.create(
            null, "slack", 1, environment.ordinal(), name, Map.of(), PlatformType.EMBEDDED);
    }

    private Fixture createFixture() {
        Long workspaceId = jdbcTemplate.queryForObject(
            """
                INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            Long.class, "workspace-" + UUID.randomUUID());

        Long projectId = jdbcTemplate.queryForObject(
            """
                INSERT INTO project (name, uuid, workspace_id, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES ('Orders project', gen_random_uuid(), ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            Long.class, workspaceId);

        long fixtureProjectId = Objects.requireNonNull(projectId);

        stubProjectVersions(fixtureProjectId, ProjectVersion.Status.PUBLISHED, ProjectVersion.Status.PUBLISHED);

        UUID syncOrdersUuid = UUID.randomUUID();
        UUID notifyUuid = UUID.randomUUID();

        stubProjectWorkflows(
            fixtureProjectId, 1,
            List.of(
                new ProjectWorkflow(
                    fixtureProjectId, 1, createWorkflow(SYNC_ORDERS_V1_WORKFLOW_DEFINITION), syncOrdersUuid)));
        stubProjectWorkflows(
            fixtureProjectId, 2,
            List.of(
                new ProjectWorkflow(
                    fixtureProjectId, 2, createWorkflow(SYNC_ORDERS_V2_WORKFLOW_DEFINITION), syncOrdersUuid),
                new ProjectWorkflow(fixtureProjectId, 2, createWorkflow(NOTIFY_WORKFLOW_DEFINITION), notifyUuid)));

        Connection sourceConnection = createConnection("Slack Dev", Environment.DEVELOPMENT);

        long sourceConnectionId = Objects.requireNonNull(sourceConnection.getId());

        AutomationProjectConfiguration source = automationProjectConfigurationService.create(
            fixtureProjectId, Environment.DEVELOPMENT.ordinal(), "Orders", "hasRole('ORDERS')", 2, true, false);

        long sourceId = Objects.requireNonNull(source.getId());

        automationProjectConfigurationService.updateWorkflowProvidedValues(
            sourceId, syncOrdersUuid, Map.of("slack", sourceConnectionId),
            Map.of("apiKey", "development-key", "region", "eu"));
        automationProjectConfigurationService.updateWorkflowProvidedValues(
            sourceId, notifyUuid, Map.of(), Map.of("recipient", "dev@example.com"));
        automationProjectConfigurationService.updateWorkflowEnabled(sourceId, notifyUuid, false);

        jdbcTemplate.update(
            """
                INSERT INTO connected_user_project_configuration (connected_user_id, project_id,
                    automation_project_configuration_id, created_by, created_date, last_modified_by,
                    last_modified_date, version)
                VALUES (?, ?, ?, 'system', now(), 'system', now(), 0)
                """,
            CONNECTED_USER_ID, fixtureProjectId, sourceId);

        return new Fixture(
            fixtureProjectId, sourceId, sourceConnectionId, source.getUuid(), syncOrdersUuid, notifyUuid);
    }

    private String createWorkflow(String definition) {
        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return workflow.getId();
    }

    private void deleteTestData() {
        jdbcTemplate.update("DELETE FROM connected_user_project_configuration");
        jdbcTemplate.update("DELETE FROM connected_user_connection");
        jdbcTemplate.update("DELETE FROM automation_project_configuration_workflow");
        jdbcTemplate.update("DELETE FROM automation_project_configuration");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
        jdbcTemplate.update("DELETE FROM connection");
    }

    private void stubProjectVersions(
        long projectId, ProjectVersion.Status firstVersionStatus, ProjectVersion.Status secondVersionStatus) {

        Project project = new Project();

        project.setId(projectId);
        project.setProjectVersions(
            List.of(
                new ProjectVersion(1, firstVersionStatus.ordinal(), Instant.now(), null),
                new ProjectVersion(2, secondVersionStatus.ordinal(), Instant.now(), null)));

        when(projectService.getProject(projectId)).thenReturn(project);
    }

    private void stubProjectWorkflows(long projectId, int projectVersion, List<ProjectWorkflow> projectWorkflows) {
        when(projectWorkflowService.getProjectWorkflows(projectId, projectVersion)).thenReturn(projectWorkflows);

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            when(projectWorkflowService.fetchProjectWorkflow(
                projectId, projectVersion, projectWorkflow.getUuidAsString()))
                    .thenReturn(Optional.of(projectWorkflow));
        }
    }

    private static AutomationProjectConfigurationWorkflow workflowRow(
        AutomationProjectConfiguration configuration, UUID workflowUuid) {

        Optional<AutomationProjectConfigurationWorkflow> workflowRow = configuration.fetchWorkflow(workflowUuid);

        assertThat(workflowRow).isPresent();

        return workflowRow.get();
    }

    private record Fixture(
        long projectId, long sourceId, long sourceConnectionId, UUID uuid, UUID syncOrdersUuid, UUID notifyUuid) {
    }
}
