/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceRowFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.exception.IntegrationInstanceConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class,
        IntegrationInstanceConfigurationFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}"
    })
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationInstanceConfigurationFacadeIntTest {

    private static final String CRON_WORKFLOW_DEFINITION = """
        {
            "label": "Cron Input Workflow",
            "inputs": [
                {"name": "cronExpression", "label": "Cron expression", "type": "string", "required": true}
            ],
            "triggers": [
                {
                    "label": "Cron",
                    "name": "trigger_1",
                    "type": "schedule/v1/cron",
                    "parameters": {"expression": "${cronExpression}", "timezone": "Europe/Zagreb"}
                }
            ],
            "tasks": []
        }""";

    private static final String INTERNAL_ONLY_OPTIONAL_WORKFLOW_DEFINITION = """
        {
            "label": "Optional Internal Only Workflow",
            "inputs": [
                {"name": "note", "label": "Note", "type": "string", "required": false, "internalOnly": true}
            ],
            "triggers": [],
            "tasks": []
        }""";

    private static final String INTERNAL_ONLY_REQUIRED_WORKFLOW_DEFINITION = """
        {
            "label": "Required Internal Only Workflow",
            "inputs": [
                {"name": "accountId", "label": "Account", "type": "string", "required": true, "internalOnly": true},
                {"name": "region", "label": "Region", "type": "string", "required": true}
            ],
            "triggers": [],
            "tasks": []
        }""";

    private static final String SLACK_TRIGGER_WORKFLOW_DEFINITION = """
        {
            "label": "Slack Workflow",
            "triggers": [
                {
                    "label": "New message",
                    "name": "slack_trigger_1",
                    "type": "slack/v1/newMessage",
                    "parameters": {}
                }
            ],
            "tasks": []
        }""";

    private static final long CONNECTED_USER_ID = 1L;
    private static final long FIRST_UPGRADE_USER_ID = 11L;
    private static final String NEW_CRON_EXPRESSION = "5/5 16 * * ?";
    private static final String OLD_CRON_EXPRESSION = "0/5 15 * * ?";
    private static final long OTHER_CONNECTED_USER_ID = 21L;
    private static final long SECOND_UPGRADE_USER_ID = 12L;
    private static final long STAGING_UPGRADE_USER_ID = 15L;
    private static final long TARGET_UPGRADE_USER_ID = 13L;
    private static final long UNASSIGNED_CONNECTED_USER_ID = 31L;
    private static final long UNKNOWN_UPGRADE_USER_ID = 14L;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private ConnectionDefinitionService connectionDefinitionService;

    @Autowired
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowRepository integrationInstanceConfigurationWorkflowRepository;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationInstanceRepository integrationInstanceRepository;

    @MockitoSpyBean
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationInstanceWorkflowRepository integrationInstanceWorkflowRepository;

    @Autowired
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;

    @Autowired
    private McpIntegrationInstanceRowFacade mcpIntegrationInstanceRowFacade;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        when(componentDefinitionService.getComponentDefinition(anyString(), any()))
            .thenReturn(new ComponentDefinition("componentName"));
        when(connectionDefinitionService.getConnectionDefinition(anyString(), any()))
            .thenReturn(new ConnectionDefinition(ComponentDsl.connection(), "componentName", "componentTitle", null));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        integrationInstanceWorkflowRepository.deleteAll();
        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationWorkflowRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testCreateIntegrationInstanceConfigurationAllowsASecondConfigurationOfTheSameVersion() {
        authenticate(admin());

        Integration integration = createPublishedIntegration();

        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integration.getId());

        long secondIntegrationInstanceConfigurationId =
            integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                newIntegrationInstanceConfigurationDTO(integration.getId(), 1));

        assertThat(secondIntegrationInstanceConfigurationId).isNotEqualTo(integrationInstanceConfigurationId);
        assertThat(
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                secondIntegrationInstanceConfigurationId)
                .getIntegrationVersion())
                    .isEqualTo(1);
    }

    @Test
    void testUpdateCredentialsReplacesTheSharedParametersOfTheConfigurationAndOfItsConnections() {
        authenticate(admin());

        Integration integration = createPublishedIntegration();

        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integration.getId());
        long otherIntegrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integration.getId());

        integrationInstanceService.create(CONNECTED_USER_ID, 41L, integrationInstanceConfigurationId);
        integrationInstanceService.create(CONNECTED_USER_ID, 42L, integrationInstanceConfigurationId);
        integrationInstanceService.create(CONNECTED_USER_ID, 43L, otherIntegrationInstanceConfigurationId);

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationCredentials(
            integrationInstanceConfigurationId, Map.of("key", "rotated-value"),
            getLockVersion(integrationInstanceConfigurationId));

        assertThat(
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId)
                .getConnectionParameters())
                    .isEqualTo(Map.of("key", "rotated-value"));
        assertThat(
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                otherIntegrationInstanceConfigurationId)
                .getConnectionParameters())
                    .isEqualTo(Map.of("key", "admin-value"));

        verify(connectionService).updateConnectionParameters(41L, Map.of("key", "rotated-value"));
        verify(connectionService).updateConnectionParameters(42L, Map.of("key", "rotated-value"));
        verify(connectionService, never()).updateConnectionParameters(eq(43L), any());
    }

    @Test
    void testUpdateCredentialsRejectsAStaleVersion() {
        authenticate(admin());

        Integration integration = createPublishedIntegration();

        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integration.getId());

        integrationInstanceService.create(CONNECTED_USER_ID, 41L, integrationInstanceConfigurationId);

        int staleVersion = getLockVersion(integrationInstanceConfigurationId) - 1;

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationCredentials(
                integrationInstanceConfigurationId, Map.of("key", "rotated-value"), staleVersion))
                    .isInstanceOf(ConfigurationException.class)
                    .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                        .isEqualTo(108));

        verify(connectionService, never()).updateConnectionParameters(anyLong(), any());
    }

    @Test
    void testUpdateCredentialsRejectsAParameterTheConfigurationDoesNotShare() {
        authenticate(admin());

        Integration integration = createPublishedIntegration();

        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integration.getId());

        integrationInstanceService.create(CONNECTED_USER_ID, 41L, integrationInstanceConfigurationId);

        int lockVersion = getLockVersion(integrationInstanceConfigurationId);

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationCredentials(
                integrationInstanceConfigurationId, Map.of("access_token", "injected"), lockVersion))
                    .isInstanceOf(ConfigurationException.class)
                    .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                        .isEqualTo(109));

        assertThat(
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId)
                .getConnectionParameters())
                    .isEqualTo(Map.of("key", "admin-value"));

        verify(connectionService, never()).updateConnectionParameters(anyLong(), any());
    }

    @Test
    void testUpdateCredentialsRejectsABlankValue() {
        authenticate(admin());

        Integration integration = createPublishedIntegration();

        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integration.getId());

        integrationInstanceService.create(CONNECTED_USER_ID, 41L, integrationInstanceConfigurationId);

        int lockVersion = getLockVersion(integrationInstanceConfigurationId);

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationCredentials(
                integrationInstanceConfigurationId, Map.of("key", " "), lockVersion))
                    .isInstanceOf(ConfigurationException.class)
                    .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                        .isEqualTo(110));

        assertThat(
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId)
                .getConnectionParameters())
                    .isEqualTo(Map.of("key", "admin-value"));

        verify(connectionService, never()).updateConnectionParameters(anyLong(), any());
    }

    @Test
    void testUpdateCredentialsIsDeniedToAConnectedUser() {
        authenticate(admin());

        Integration integration = createPublishedIntegration();

        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integration.getId());

        int lockVersion = getLockVersion(integrationInstanceConfigurationId);

        authenticate(connectedUser());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationCredentials(
                integrationInstanceConfigurationId, Map.of("key", "rotated-value"), lockVersion))
                    .isInstanceOf(AccessDeniedException.class);

        verify(connectionService, never()).updateConnectionParameters(anyLong(), any());
    }

    @Test
    void testCreateIntegrationInstanceConfigurationPublishesAnIntegrationWithoutWorkflows() {
        authenticate(admin());

        Integration integration = createIntegration();

        long integrationInstanceConfigurationId =
            integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                newIntegrationInstanceConfigurationDTO(integration.getId(), 1));

        Integration publishedIntegration = integrationService.getIntegration(integration.getId());

        assertThat(publishedIntegration.isPublished()).isTrue();
        assertThat(publishedIntegration.getLastIntegrationVersion()).isEqualTo(2);
        assertThat(
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId)
                .getIntegrationVersion())
                    .isEqualTo(1);
    }

    @Test
    void testCreateIntegrationInstanceConfigurationEnablesAConfigurationWithoutWorkflows() {
        authenticate(admin());

        Integration integration = createIntegration();

        long integrationInstanceConfigurationId =
            integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                newIntegrationInstanceConfigurationDTO(integration.getId(), 1));

        assertThat(
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId)
                .isEnabled())
                    .isTrue();
    }

    @Test
    void testCreateIntegrationInstanceConfigurationLeavesAConfigurationWithWorkflowsDisabled() {
        authenticate(admin());

        Integration integration = createPublishedIntegration();

        addCronWorkflow(integration.getId(), 1, UUID.randomUUID());

        long integrationInstanceConfigurationId =
            integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                newIntegrationInstanceConfigurationDTO(integration.getId(), 1));

        assertThat(
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId)
                .isEnabled())
                    .isFalse();
    }

    @Test
    void testCreateIntegrationInstanceConfigurationRefusesAnUnpublishedIntegrationWithWorkflows() {
        authenticate(admin());

        Integration integration = createIntegration();

        addCronWorkflow(integration.getId(), 1, UUID.randomUUID());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                newIntegrationInstanceConfigurationDTO(integration.getId(), 1)))
                    .isInstanceOf(ConfigurationException.class)
                    .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                        .isEqualTo(IntegrationInstanceConfigurationErrorType.INTEGRATION_NOT_PUBLISHED.getErrorKey()));

        assertThat(integrationService.getIntegration(integration.getId())
            .isPublished()).isFalse();
    }

    @Test
    void testCreateIntegrationInstanceConfigurationAllowsAnotherVersionWhateverTheExistingLockVersion() {
        authenticate(admin());

        Integration integration = createPublishedIntegration();

        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integration.getId());

        int lockVersion = bumpLockVersionPast(integrationInstanceConfigurationId, 1);

        while (integration.getLastIntegrationVersion() <= lockVersion) {
            integrationService.publishIntegration(integration.getId(), null);

            integration = integrationService.getIntegration(integration.getId());
        }

        long secondIntegrationInstanceConfigurationId =
            integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                newIntegrationInstanceConfigurationDTO(integration.getId(), lockVersion));

        assertThat(
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                secondIntegrationInstanceConfigurationId)
                .getIntegrationVersion())
                    .isEqualTo(lockVersion);
    }

    @Test
    void testCreateIntegrationInstanceConfigurationWithoutAuthorizationType() {
        authenticate(admin());

        Integration integration = createPublishedIntegration();

        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO =
            newIntegrationInstanceConfigurationDTO(integration.getId(), 1)
                .toBuilder()
                .authorizationType(null)
                .build();

        long integrationInstanceConfigurationId =
            integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                integrationInstanceConfigurationDTO);

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId);

        assertThat(integrationInstanceConfiguration.getAuthorizationType()).isNull();
    }

    @Test
    void testIntegrationsReadReturnsOneEntryPerIntegrationOnTheHighestEnabledVersion() {
        authenticate(admin());

        Integration integration = createPublishedIntegration();

        integrationService.publishIntegration(integration.getId(), null);

        long integrationId = integration.getId();

        long versionOneConfigurationId = integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
            newIntegrationInstanceConfigurationDTO(integrationId, 1));
        long versionTwoConfigurationId = integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
            newIntegrationInstanceConfigurationDTO(integrationId, 2));

        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfiguration(versionOneConfigurationId, true);
        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfiguration(versionTwoConfigurationId, true);

        List<IntegrationInstanceConfigurationDTO> integrations =
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationIntegrations(
                true, Environment.STAGING, null);

        assertThat(integrations)
            .filteredOn(integrationInstanceConfigurationDTO -> integrationInstanceConfigurationDTO
                .integrationId() == integrationId)
            .hasSize(1)
            .first()
            .extracting(IntegrationInstanceConfigurationDTO::integrationVersion)
            .isEqualTo(2);
        String externalUserId = "external-user-" + UUID.randomUUID();

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUserService.fetchConnectedUser(externalUserId, Environment.STAGING))
            .thenReturn(Optional.of(connectedUser));
        when(embeddedPermissionEvaluator.evaluate(any(), eq(connectedUser))).thenReturn(true);

        authenticate(
            new EmbeddedApiKeyAuthenticationToken(
                Environment.STAGING.ordinal(), 1L, new User(externalUserId, "", List.of()), false));

        assertThat(
            integrationInstanceConfigurationFacade
                .getIntegrationInstanceConfigurationIntegration(integrationId, true, Environment.STAGING, connectedUser)
                .integrationVersion()).isEqualTo(2);
    }

    @Test
    void testListReadIsDeliberatelyOnlyAuthenticated() {
        long integrationInstanceConfigurationId = createEnabledIntegrationInstanceConfiguration();

        for (Authentication authentication : List.of(nonAdmin(), connectedUser())) {
            authenticate(authentication);

            assertThat(
                integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationIntegrations(
                    true, Environment.PRODUCTION, null))
                        .as("list read allowed to %s", authentication.getName())
                        .extracting(IntegrationInstanceConfigurationDTO::id)
                        .contains(integrationInstanceConfigurationId);

            assertThatThrownBy(
                () -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(
                    integrationInstanceConfigurationId))
                        .as("by-id read denied to %s", authentication.getName())
                        .isInstanceOf(AccessDeniedException.class);
        }

        authenticate(anonymous());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationIntegrations(
                true, Environment.PRODUCTION, null))
                    .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUpdateIntegrationInstanceConfigurationWorkflowReschedulesEveryActiveIntegrationInstance() {
        CronFixture cronFixture = createCronFixture(List.of());

        authenticate(admin());

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
            withCronExpression(getConfigurationWorkflow(cronFixture), NEW_CRON_EXPRESSION));

        for (long integrationInstanceId : cronFixture.activeIntegrationInstanceIds()) {
            InOrder inOrder = inOrder(triggerLifecycleFacade);

            inOrder.verify(triggerLifecycleFacade)
                .executeTriggerDisable(
                    any(), argThat(workflowExecutionId -> isFor(workflowExecutionId, integrationInstanceId)), any(),
                    argThat(parameters -> hasExpression(parameters, OLD_CRON_EXPRESSION)), any());
            inOrder.verify(triggerLifecycleFacade)
                .executeTriggerEnable(
                    any(), argThat(workflowExecutionId -> isFor(workflowExecutionId, integrationInstanceId)), any(),
                    argThat(parameters -> hasExpression(parameters, NEW_CRON_EXPRESSION)), any(), any(), anyLong());
        }

        verify(triggerLifecycleFacade, never()).executeTriggerDisable(
            any(),
            argThat(workflowExecutionId -> isFor(workflowExecutionId, cronFixture.inactiveIntegrationInstanceId())),
            any(), any(), any());
        verify(triggerLifecycleFacade, never()).executeTriggerEnable(
            any(),
            argThat(workflowExecutionId -> isFor(workflowExecutionId, cronFixture.inactiveIntegrationInstanceId())),
            any(), any(), any(), any(), anyLong());

        assertThat(getConfigurationWorkflow(cronFixture).getInputs())
            .isEqualTo(Map.of("cronExpression", NEW_CRON_EXPRESSION));
    }

    @Test
    void testUpdateIntegrationInstanceConfigurationWorkflowWithDisabledConfigurationDoesNotTouchTriggers() {
        CronFixture cronFixture = createCronFixture(List.of());

        integrationInstanceConfigurationService.updateEnabled(cronFixture.integrationInstanceConfigurationId(), false);

        authenticate(admin());

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
            withCronExpression(getConfigurationWorkflow(cronFixture), NEW_CRON_EXPRESSION));

        verifyNoInteractions(triggerLifecycleFacade);
    }

    @Test
    void testUpdateIntegrationInstanceConfigurationWorkflowWithStaleVersionKeepsEnabledTriggers() {
        CronFixture cronFixture = createCronFixture(List.of());

        IntegrationInstanceConfigurationWorkflow staleIntegrationInstanceConfigurationWorkflow =
            getConfigurationWorkflow(cronFixture);

        authenticate(admin());

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
            withCronExpression(getConfigurationWorkflow(cronFixture), "10/5 15 * * ?"));

        reset(triggerLifecycleFacade);

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
                withCronExpression(staleIntegrationInstanceConfigurationWorkflow, NEW_CRON_EXPRESSION)))
                    .isInstanceOf(OptimisticLockingFailureException.class);

        verifyNoInteractions(triggerLifecycleFacade);
    }

    @Test
    void testUpdateIntegrationInstanceConfigurationWorkflowRestoresPreviousTriggersWhenAnInstanceFailsToEnable() {
        CronFixture cronFixture = createCronFixture(List.of());

        long firstIntegrationInstanceId = cronFixture.activeIntegrationInstanceIds()
            .getFirst();
        long failingIntegrationInstanceId = cronFixture.activeIntegrationInstanceIds()
            .getLast();

        IllegalStateException enableFailure = new IllegalStateException("Trigger registration failed");

        doThrow(enableFailure)
            .when(triggerLifecycleFacade)
            .executeTriggerEnable(
                any(), argThat(workflowExecutionId -> isFor(workflowExecutionId, failingIntegrationInstanceId)), any(),
                argThat(parameters -> hasExpression(parameters, NEW_CRON_EXPRESSION)), any(), any(), anyLong());

        authenticate(admin());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
                withCronExpression(getConfigurationWorkflow(cronFixture), NEW_CRON_EXPRESSION)))
                    .isSameAs(enableFailure);

        InOrder firstInOrder = inOrder(triggerLifecycleFacade);

        firstInOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(
                any(), argThat(workflowExecutionId -> isFor(workflowExecutionId, firstIntegrationInstanceId)), any(),
                argThat(parameters -> hasExpression(parameters, NEW_CRON_EXPRESSION)), any(), any(), anyLong());
        firstInOrder.verify(triggerLifecycleFacade)
            .executeTriggerDisable(
                any(), argThat(workflowExecutionId -> isFor(workflowExecutionId, firstIntegrationInstanceId)), any(),
                argThat(parameters -> hasExpression(parameters, NEW_CRON_EXPRESSION)), any());
        firstInOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(
                any(), argThat(workflowExecutionId -> isFor(workflowExecutionId, firstIntegrationInstanceId)), any(),
                argThat(parameters -> hasExpression(parameters, OLD_CRON_EXPRESSION)), any(), any(), anyLong());

        verify(triggerLifecycleFacade).executeTriggerEnable(
            any(), argThat(workflowExecutionId -> isFor(workflowExecutionId, failingIntegrationInstanceId)), any(),
            argThat(parameters -> hasExpression(parameters, OLD_CRON_EXPRESSION)), any(), any(), anyLong());

        assertThat(getConfigurationWorkflow(cronFixture).getInputs())
            .isEqualTo(Map.of("cronExpression", OLD_CRON_EXPRESSION));
    }

    @Test
    void testUpdateIntegrationInstanceConfigurationWorkflowDisablesTriggersWithThePreviousConnection() {
        long oldConnectionId = 1001L;
        long newConnectionId = 1002L;

        ComponentConnection componentConnection = new ComponentConnection(
            "schedule", 1, "trigger_1", "connection", false);

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTrigger.class)))
            .thenReturn(List.of(componentConnection));
        when(componentConnectionFacade.getComponentConnection(anyString(), anyString(), anyString()))
            .thenReturn(componentConnection);

        Connection connection = new Connection();

        connection.setComponentName("schedule");

        when(connectionService.getConnection(anyLong())).thenReturn(connection);

        CronFixture cronFixture = createCronFixture(
            List.of(
                new IntegrationInstanceConfigurationWorkflowConnection(oldConnectionId, "connection", "trigger_1")));

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow = getConfigurationWorkflow(
            cronFixture);

        integrationInstanceConfigurationWorkflow.setConnections(
            List.of(
                new IntegrationInstanceConfigurationWorkflowConnection(newConnectionId, "connection", "trigger_1")));

        authenticate(admin());

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
            integrationInstanceConfigurationWorkflow);

        long integrationInstanceId = cronFixture.activeIntegrationInstanceIds()
            .getFirst();

        InOrder inOrder = inOrder(triggerLifecycleFacade);

        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerDisable(
                any(), argThat(workflowExecutionId -> isFor(workflowExecutionId, integrationInstanceId)), any(),
                any(), eq(oldConnectionId));
        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(
                any(), argThat(workflowExecutionId -> isFor(workflowExecutionId, integrationInstanceId)), any(),
                any(), eq(newConnectionId), any(), anyLong());
    }

    @Test
    void testMoveRemapsInstanceWorkflowsByUuidKeepingInputs() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.API_KEY);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.moveConnectedUsers(
                moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(),
                List.of(CONNECTED_USER_ID));

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);
        assertThat(configurationMoveResultDTO.skipped()).isEmpty();
        assertThat(configurationMoveResultDTO.needsAttentionConnectedUserIds()).isEmpty();

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            moveFixture.integrationInstanceId());

        assertThat(integrationInstance.getIntegrationInstanceConfigurationId())
            .isEqualTo(moveFixture.targetConfigurationId());

        IntegrationInstanceWorkflow integrationInstanceWorkflow = integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflows(moveFixture.integrationInstanceId())
            .getFirst();

        assertThat(integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId())
            .isEqualTo(moveFixture.targetConfigurationWorkflowId());
        assertThat(integrationInstanceWorkflow.isEnabled()).isTrue();
        assertThat(integrationInstanceWorkflow.getInputs()).isEqualTo(Map.of("owner", "alice"));

        InOrder inOrder = inOrder(triggerLifecycleFacade);

        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerDisable(eq(moveFixture.sourceWorkflowId()), any(), any(), any(), any());
        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(
                eq(moveFixture.targetWorkflowId()), any(), any(),
                argThat(parameters -> hasExpression(parameters, NEW_CRON_EXPRESSION)), any(), any(), anyLong());
    }

    @Test
    void testEnabledConfigurationWorkflowReachesUsersWithItsMcpServers() {
        CronFixture cronFixture = createCronFixture(List.of());

        authenticate(admin());

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            getConfigurationWorkflow(cronFixture);

        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfigurationWorkflow(
            cronFixture.integrationInstanceConfigurationId(), integrationInstanceConfigurationWorkflow.getWorkflowId(),
            false);

        verify(mcpIntegrationInstanceRowFacade, never()).addMissingRowsForIntegrationInstanceConfiguration(anyLong());

        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfigurationWorkflow(
            cronFixture.integrationInstanceConfigurationId(), integrationInstanceConfigurationWorkflow.getWorkflowId(),
            true);

        verify(mcpIntegrationInstanceRowFacade).addMissingRowsForIntegrationInstanceConfiguration(
            cronFixture.integrationInstanceConfigurationId());
    }

    @Test
    void testEnableConfigurationWorkflowIgnoresConnectionsOfDisabledTasks() {
        CronFixture cronFixture = createCronFixture(List.of());

        authenticate(admin());

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            getConfigurationWorkflow(cronFixture);

        String workflowId = integrationInstanceConfigurationWorkflow.getWorkflowId();

        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfigurationWorkflow(
            cronFixture.integrationInstanceConfigurationId(), workflowId, false);

        String definition = CRON_WORKFLOW_DEFINITION.replace(
            "\"tasks\": []",
            """
                "tasks": [
                    {"name": "postToSlack", "type": "slack/v1/sendChannelMessage", "disabled": true}
                ]""");

        when(workflowService.getWorkflow(workflowId))
            .thenReturn(new Workflow(workflowId, definition, Workflow.Format.JSON));
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenReturn(List.of(new ComponentConnection("slack", 1, "postToSlack", "slack", true)));

        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfigurationWorkflow(
            cronFixture.integrationInstanceConfigurationId(), workflowId, true);

        assertThat(getConfigurationWorkflow(cronFixture).isEnabled()).isTrue();
    }

    @Test
    void testUpdatedConfigurationReachesUsersWithItsMcpServers() {
        CronFixture cronFixture = createCronFixture(List.of());

        authenticate(admin());

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfiguration(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(
                cronFixture.integrationInstanceConfigurationId()));

        verify(mcpIntegrationInstanceRowFacade).addMissingRowsForIntegrationInstanceConfiguration(
            cronFixture.integrationInstanceConfigurationId());
    }

    @Test
    void testMoveConnectedUsersRemapsMcpRows() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.API_KEY);

        authenticate(admin());

        integrationInstanceConfigurationFacade.moveConnectedUsers(
            moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(), List.of(CONNECTED_USER_ID));

        verify(mcpIntegrationInstanceRowFacade).remapIntegrationInstance(moveFixture.integrationInstanceId());
    }

    @Test
    void testSkippedMoveLeavesMcpRowsAlone() {
        MoveFixture moveFixture = createMoveFixture(false, AuthorizationType.API_KEY);

        authenticate(admin());

        integrationInstanceConfigurationFacade.moveConnectedUsers(
            moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(), List.of(CONNECTED_USER_ID));

        verify(mcpIntegrationInstanceRowFacade, never()).remapIntegrationInstance(anyLong());
    }

    @Test
    void testMoveSkipsInstanceWhenTargetLacksAnEnabledWorkflow() {
        MoveFixture moveFixture = createMoveFixture(false, AuthorizationType.API_KEY);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.moveConnectedUsers(
                moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(),
                List.of(CONNECTED_USER_ID));

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).isEmpty();
        assertThat(configurationMoveResultDTO.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(
                CONNECTED_USER_ID, ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_ENABLED_WORKFLOWS,
                List.of("Cron Input Workflow")));

        assertStillOnSource(moveFixture);
    }

    @Test
    void testMoveSkipsInstanceOnAuthorizationTypeMismatch() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.BEARER_TOKEN);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.moveConnectedUsers(
                moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(),
                List.of(CONNECTED_USER_ID));

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).isEmpty();
        assertThat(configurationMoveResultDTO.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(
                CONNECTED_USER_ID, ConfigurationMoveResultDTO.Skipped.CONNECTION_MISMATCH, List.of()));

        assertStillOnSource(moveFixture);
    }

    @Test
    void testMoveSkipsConnectedUserThatIsNotOnTheSource() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.API_KEY);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.moveConnectedUsers(
                moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(), List.of(999L));

        assertThat(configurationMoveResultDTO.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(999L, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));

        assertStillOnSource(moveFixture);
    }

    @Test
    void testMoveSkipsInstanceAlreadyMovedByAnotherCall() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.API_KEY);

        long otherConfigurationId = createConfiguration(
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                moveFixture.sourceConfigurationId())
                .getIntegrationId(),
            3, AuthorizationType.API_KEY);

        doAnswer(invocation -> {
            integrationInstanceService.updateIntegrationInstanceConfigurationId(
                moveFixture.integrationInstanceId(), otherConfigurationId);

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerDisable(any(), any(), any(), any(), any());

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.moveConnectedUsers(
                moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(),
                List.of(CONNECTED_USER_ID));

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).isEmpty();
        assertThat(configurationMoveResultDTO.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(
                CONNECTED_USER_ID, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            moveFixture.integrationInstanceId());

        assertThat(integrationInstance.getIntegrationInstanceConfigurationId()).isEqualTo(otherConfigurationId);
        assertThat(
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(moveFixture.integrationInstanceId())
                .getFirst()
                .getIntegrationInstanceConfigurationWorkflowId()).isEqualTo(
                    moveFixture.sourceConfigurationWorkflowId());
    }

    @Test
    void testMoveImpactSkippedEntriesEqualTheMoveOutcomeForUsersWithSeveralInstances() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.API_KEY);

        long integrationId = integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
            moveFixture.sourceConfigurationId())
            .getIntegrationId();

        String sourceOnlyWorkflowId = addCronWorkflow(integrationId, 1, UUID.randomUUID());

        long sourceOnlyConfigurationWorkflowId = createConfigurationWorkflow(
            moveFixture.sourceConfigurationId(), sourceOnlyWorkflowId, OLD_CRON_EXPRESSION);

        long secondUserId = CONNECTED_USER_ID + 1;

        ConnectedUser secondConnectedUser = new ConnectedUser();

        secondConnectedUser.setEnabled(true);

        when(connectedUserService.getConnectedUser(secondUserId)).thenReturn(secondConnectedUser);

        createSourceInstanceWorkflow(CONNECTED_USER_ID, moveFixture, sourceOnlyConfigurationWorkflowId);
        createSourceInstanceWorkflow(secondUserId, moveFixture, sourceOnlyConfigurationWorkflowId);

        authenticate(admin());

        List<Long> connectedUserIds = List.of(CONNECTED_USER_ID, secondUserId);

        ConfigurationMoveImpactDTO configurationMoveImpactDTO = integrationInstanceConfigurationFacade.getMoveImpact(
            moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(), connectedUserIds);
        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.moveConnectedUsers(
                moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(), connectedUserIds);

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);
        assertThat(configurationMoveResultDTO.skipped()).extracting(ConfigurationMoveResultDTO.Skipped::connectedUserId)
            .containsExactly(CONNECTED_USER_ID, secondUserId);
        assertThat(configurationMoveImpactDTO.skipped()).isEqualTo(configurationMoveResultDTO.skipped());
    }

    @Test
    void testPartlyMovedUserIsReportedAsSkippedInTheImpactAndTheResult() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.API_KEY);

        long integrationId = integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
            moveFixture.sourceConfigurationId())
            .getIntegrationId();

        String sourceOnlyWorkflowId = addCronWorkflow(integrationId, 1, UUID.randomUUID());

        long sourceOnlyConfigurationWorkflowId = createConfigurationWorkflow(
            moveFixture.sourceConfigurationId(), sourceOnlyWorkflowId, OLD_CRON_EXPRESSION);

        createSourceInstanceWorkflow(CONNECTED_USER_ID, moveFixture, sourceOnlyConfigurationWorkflowId);

        authenticate(admin());

        ConfigurationMoveResultDTO.Skipped expectedSkipped = new ConfigurationMoveResultDTO.Skipped(
            CONNECTED_USER_ID, ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_ENABLED_WORKFLOWS,
            List.of("Cron Input Workflow"));

        ConfigurationMoveImpactDTO configurationMoveImpactDTO = integrationInstanceConfigurationFacade.getMoveImpact(
            moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(), List.of(CONNECTED_USER_ID));

        assertThat(configurationMoveImpactDTO.skipped()).containsExactly(expectedSkipped);

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.moveConnectedUsers(
                moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(), List.of(CONNECTED_USER_ID));

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);
        assertThat(configurationMoveResultDTO.needsAttentionConnectedUserIds()).containsExactly(CONNECTED_USER_ID);
        assertThat(configurationMoveResultDTO.skipped()).containsExactly(expectedSkipped);
        assertThat(integrationInstanceService.getIntegrationInstance(moveFixture.integrationInstanceId())
            .getIntegrationInstanceConfigurationId()).isEqualTo(moveFixture.targetConfigurationId());
    }

    @Test
    void testMoveImpactCountsTheMcpServersOfTheTarget() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.API_KEY);

        when(mcpIntegrationInstanceConfigurationService
            .getMcpIntegrationInstanceConfigurationsByIntegrationInstanceConfigurationId(
                moveFixture.targetConfigurationId()))
                    .thenReturn(
                        List.of(
                            new McpIntegrationInstanceConfiguration(1L, moveFixture.targetConfigurationId(), 77L),
                            new McpIntegrationInstanceConfiguration(2L, moveFixture.targetConfigurationId(), 77L)));

        authenticate(admin());

        ConfigurationMoveImpactDTO configurationMoveImpactDTO = integrationInstanceConfigurationFacade.getMoveImpact(
            moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(), List.of(CONNECTED_USER_ID));

        assertThat(configurationMoveImpactDTO.mcpServerCount()).isEqualTo(1);
    }

    @Test
    void testMoveFailsWhenTheTargetBelongsToAnotherIntegration() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.API_KEY);

        Integration otherIntegration = new Integration();

        otherIntegration.setComponentName("componentName");
        otherIntegration.setName("integration-" + UUID.randomUUID());

        otherIntegration = integrationService.create(otherIntegration);

        long otherConfigurationId = createConfiguration(otherIntegration.getId(), 1, AuthorizationType.API_KEY);

        authenticate(admin());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.moveConnectedUsers(
                moveFixture.sourceConfigurationId(), otherConfigurationId, List.of(CONNECTED_USER_ID)))
                    .isInstanceOf(ConfigurationException.class)
                    .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                        .isEqualTo(107));

        assertStillOnSource(moveFixture);
    }

    @Test
    void testMoveReportsNeedsAttentionWhenTheTargetTriggersFailToEnable() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.API_KEY);

        doThrow(new IllegalStateException("Trigger registration failed"))
            .when(triggerLifecycleFacade)
            .executeTriggerEnable(
                eq(moveFixture.targetWorkflowId()), any(), any(), any(), any(), any(), anyLong());

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.moveConnectedUsers(
                moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(),
                List.of(CONNECTED_USER_ID));

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);
        assertThat(configurationMoveResultDTO.needsAttentionConnectedUserIds()).containsExactly(CONNECTED_USER_ID);

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            moveFixture.integrationInstanceId());

        assertThat(integrationInstance.getIntegrationInstanceConfigurationId())
            .isEqualTo(moveFixture.targetConfigurationId());
    }

    @Test
    void testGetMoveImpactListsSkippedUsersAndWorkflowsNeedingNewInputs() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.API_KEY);

        IntegrationInstanceConfigurationWorkflow targetConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                moveFixture.targetConfigurationWorkflowId());

        targetConfigurationWorkflow.setInputs(Map.of("unrelated", "value"));

        integrationInstanceConfigurationWorkflowService.update(targetConfigurationWorkflow);

        authenticate(admin());

        ConfigurationMoveImpactDTO configurationMoveImpactDTO = integrationInstanceConfigurationFacade.getMoveImpact(
            moveFixture.sourceConfigurationId(), moveFixture.targetConfigurationId(), List.of(CONNECTED_USER_ID, 999L));

        assertThat(configurationMoveImpactDTO.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(999L, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));
        assertThat(configurationMoveImpactDTO.workflows()).hasSize(1);

        assertThat(configurationMoveImpactDTO.workflows()
            .getFirst()
            .newRequiredInputs()).containsExactly("cronExpression");
        assertThat(configurationMoveImpactDTO.workflows()
            .getFirst()
            .affectedConnectedUserCount()).isEqualTo(1);

        assertStillOnSource(moveFixture);
    }

    @Test
    void testEnableIntegrationInstanceConfigurationWorkflowRefusesAnEmptyRequiredInternalOnlyInput() {
        IntegrationInstanceConfigurationWorkflow configurationWorkflow = createInternalOnlyConfigurationWorkflow(
            INTERNAL_ONLY_REQUIRED_WORKFLOW_DEFINITION, false, Map.of("region", "eu"));

        authenticate(admin());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.enableIntegrationInstanceConfigurationWorkflow(
                configurationWorkflow.getIntegrationInstanceConfigurationId(), configurationWorkflow.getWorkflowId(),
                true))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("accountId")
                    .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                        .isEqualTo(
                            IntegrationInstanceConfigurationErrorType.INTERNAL_ONLY_INPUT_MISSING.getErrorKey()));

        assertThat(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                configurationWorkflow.getId())
                .isEnabled()).isFalse();
    }

    @Test
    void testEnableIntegrationInstanceConfigurationWorkflowAllowsAnEmptyOptionalInternalOnlyInput() {
        IntegrationInstanceConfigurationWorkflow configurationWorkflow = createInternalOnlyConfigurationWorkflow(
            INTERNAL_ONLY_OPTIONAL_WORKFLOW_DEFINITION, false, Map.of());

        authenticate(admin());

        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfigurationWorkflow(
            configurationWorkflow.getIntegrationInstanceConfigurationId(), configurationWorkflow.getWorkflowId(), true);

        assertThat(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                configurationWorkflow.getId())
                .isEnabled()).isTrue();
    }

    @Test
    void testUpdateIntegrationInstanceConfigurationWorkflowRefusesClearingARequiredInternalOnlyInputOfAnEnabledWorkflow() {
        IntegrationInstanceConfigurationWorkflow configurationWorkflow = createInternalOnlyConfigurationWorkflow(
            INTERNAL_ONLY_REQUIRED_WORKFLOW_DEFINITION, true, Map.of("accountId", "account-1", "region", "eu"));

        authenticate(admin());

        IntegrationInstanceConfigurationWorkflow storedConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                configurationWorkflow.getId());

        storedConfigurationWorkflow.setInputs(Map.of("region", "eu"));

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
                storedConfigurationWorkflow))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("accountId")
                    .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                        .isEqualTo(
                            IntegrationInstanceConfigurationErrorType.INTERNAL_ONLY_INPUT_MISSING.getErrorKey()));

        assertThat(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                configurationWorkflow.getId())
                .getInputs()).isEqualTo(Map.of("accountId", "account-1", "region", "eu"));
    }

    @Test
    void testUpdateIntegrationInstanceConfigurationWorkflowAllowsAnEmptyRequiredInternalOnlyInputOfADisabledWorkflow() {
        IntegrationInstanceConfigurationWorkflow configurationWorkflow = createInternalOnlyConfigurationWorkflow(
            INTERNAL_ONLY_REQUIRED_WORKFLOW_DEFINITION, false, Map.of("accountId", "account-1", "region", "eu"));

        authenticate(admin());

        IntegrationInstanceConfigurationWorkflow storedConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                configurationWorkflow.getId());

        storedConfigurationWorkflow.setInputs(Map.of("region", "eu"));

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
            storedConfigurationWorkflow);

        assertThat(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                configurationWorkflow.getId())
                .getInputs()).isEqualTo(Map.of("region", "eu"));
    }

    @Test
    void testChangeConnectedUserVersionCopyLeavesAWorkflowWithAnEmptyRequiredInternalOnlyInputDisabled() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        UUID internalOnlyWorkflowUuid = UUID.randomUUID();

        String sourceInternalOnlyWorkflowId = addWorkflow(
            changeVersionFixture.integrationId(), 1, internalOnlyWorkflowUuid,
            INTERNAL_ONLY_REQUIRED_WORKFLOW_DEFINITION);
        String targetInternalOnlyWorkflowId = addWorkflow(
            changeVersionFixture.integrationId(), 2, internalOnlyWorkflowUuid,
            INTERNAL_ONLY_REQUIRED_WORKFLOW_DEFINITION);

        IntegrationInstanceConfigurationWorkflow sourceInternalOnlyConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        sourceInternalOnlyConfigurationWorkflow.setEnabled(true);
        sourceInternalOnlyConfigurationWorkflow.setInputs(Map.of("region", "eu"));
        sourceInternalOnlyConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            changeVersionFixture.sourceConfigurationId());
        sourceInternalOnlyConfigurationWorkflow.setWorkflowId(sourceInternalOnlyWorkflowId);

        integrationInstanceConfigurationWorkflowService.create(sourceInternalOnlyConfigurationWorkflow);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, null);

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);

        IntegrationInstanceConfiguration copyConfiguration = getCopyConfiguration(
            changeVersionFixture.integrationId(), List.of(changeVersionFixture.sourceConfigurationId()));

        IntegrationInstanceConfigurationWorkflow copiedInternalOnlyConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                copyConfiguration.getId(), targetInternalOnlyWorkflowId);

        assertThat(copiedInternalOnlyConfigurationWorkflow.isEnabled()).isFalse();

        IntegrationInstanceConfigurationWorkflow carriedConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                copyConfiguration.getId(), changeVersionFixture.otherWorkflowId());

        assertThat(carriedConfigurationWorkflow.isEnabled()).isTrue();
    }

    @Test
    void testMoveImpactLeavesOutInternalOnlyInputs() {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEnabled(true);

        when(connectedUserService.getConnectedUser(CONNECTED_USER_ID)).thenReturn(connectedUser);

        Integration integration = createIntegration();

        UUID workflowUuid = UUID.randomUUID();

        String sourceWorkflowId = addWorkflow(
            integration.getId(), 1, workflowUuid, INTERNAL_ONLY_REQUIRED_WORKFLOW_DEFINITION);
        String targetWorkflowId = addWorkflow(
            integration.getId(), 2, workflowUuid, INTERNAL_ONLY_REQUIRED_WORKFLOW_DEFINITION);

        long sourceConfigurationId = createConfiguration(integration.getId(), 1, AuthorizationType.API_KEY);
        long targetConfigurationId = createConfiguration(integration.getId(), 2, AuthorizationType.API_KEY);

        long sourceConfigurationWorkflowId = createConfigurationWorkflow(
            sourceConfigurationId, sourceWorkflowId, "unused");

        createConfigurationWorkflow(targetConfigurationId, targetWorkflowId, "unused");

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, 41L, sourceConfigurationId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                integrationInstance.getId(), sourceConfigurationWorkflowId);

        integrationInstanceWorkflow.setEnabled(true);

        integrationInstanceWorkflowService.update(integrationInstanceWorkflow);

        authenticate(admin());

        ConfigurationMoveImpactDTO configurationMoveImpactDTO = integrationInstanceConfigurationFacade.getMoveImpact(
            sourceConfigurationId, targetConfigurationId, List.of(CONNECTED_USER_ID));

        assertThat(configurationMoveImpactDTO.workflows()).hasSize(1);
        assertThat(configurationMoveImpactDTO.workflows()
            .getFirst()
            .newRequiredInputs()).containsExactly("region");
    }

    @Test
    void testUpgradeConnectedUsersMovesUsersFromSeveralConfigurations() {
        UpgradeFixture upgradeFixture = createUpgradeFixture();

        long firstIntegrationInstanceId = createUpgradeInstance(
            FIRST_UPGRADE_USER_ID, upgradeFixture.firstSourceConfigurationId(),
            upgradeFixture.firstSourceConfigurationWorkflowId());
        long secondIntegrationInstanceId = createUpgradeInstance(
            SECOND_UPGRADE_USER_ID, upgradeFixture.secondSourceConfigurationId(),
            upgradeFixture.secondSourceConfigurationWorkflowId());

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.upgradeConnectedUsers(
                upgradeFixture.targetConfigurationId(), List.of(SECOND_UPGRADE_USER_ID, FIRST_UPGRADE_USER_ID));

        assertThat(configurationMoveResultDTO.movedConnectedUserIds())
            .containsExactly(FIRST_UPGRADE_USER_ID, SECOND_UPGRADE_USER_ID);
        assertThat(configurationMoveResultDTO.skipped()).isEmpty();
        assertThat(configurationMoveResultDTO.needsAttentionConnectedUserIds()).isEmpty();
        assertThat(getConfigurationId(firstIntegrationInstanceId)).isEqualTo(upgradeFixture.targetConfigurationId());
        assertThat(getConfigurationId(secondIntegrationInstanceId)).isEqualTo(upgradeFixture.targetConfigurationId());
    }

    @Test
    void testUpgradeConnectedUsersSkipsUsersAlreadyOnTargetOrWithoutTheIntegration() {
        UpgradeFixture upgradeFixture = createUpgradeFixture();

        long firstIntegrationInstanceId = createUpgradeInstance(
            FIRST_UPGRADE_USER_ID, upgradeFixture.firstSourceConfigurationId(),
            upgradeFixture.firstSourceConfigurationWorkflowId());

        createUpgradeInstance(
            TARGET_UPGRADE_USER_ID, upgradeFixture.targetConfigurationId(),
            upgradeFixture.targetConfigurationWorkflowId());

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.upgradeConnectedUsers(
                upgradeFixture.targetConfigurationId(),
                List.of(FIRST_UPGRADE_USER_ID, TARGET_UPGRADE_USER_ID, UNKNOWN_UPGRADE_USER_ID, FIRST_UPGRADE_USER_ID));

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(FIRST_UPGRADE_USER_ID);
        assertThat(configurationMoveResultDTO.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(
                TARGET_UPGRADE_USER_ID, ConfigurationMoveResultDTO.Skipped.ALREADY_ON_TARGET, List.of()),
            new ConfigurationMoveResultDTO.Skipped(
                UNKNOWN_UPGRADE_USER_ID, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));
        assertThat(getConfigurationId(firstIntegrationInstanceId)).isEqualTo(upgradeFixture.targetConfigurationId());
    }

    @Test
    void testUpgradeConnectedUsersIgnoresOtherEnvironments() {
        UpgradeFixture upgradeFixture = createUpgradeFixture();

        long stagingConfigurationId = createConfiguration(
            upgradeFixture.integrationId(), 1, AuthorizationType.API_KEY, Environment.STAGING);

        long stagingConfigurationWorkflowId = createConfigurationWorkflow(
            stagingConfigurationId, upgradeFixture.firstSourceWorkflowId(), OLD_CRON_EXPRESSION);

        long stagingIntegrationInstanceId = createUpgradeInstance(
            STAGING_UPGRADE_USER_ID, stagingConfigurationId, stagingConfigurationWorkflowId);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.upgradeConnectedUsers(
                upgradeFixture.targetConfigurationId(), List.of(STAGING_UPGRADE_USER_ID));

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).isEmpty();
        assertThat(configurationMoveResultDTO.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(
                STAGING_UPGRADE_USER_ID, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));
        assertThat(getConfigurationId(stagingIntegrationInstanceId)).isEqualTo(stagingConfigurationId);
    }

    @Test
    void testUpgradeConnectedUsersMovesEveryInstanceOfAUser() {
        UpgradeFixture upgradeFixture = createUpgradeFixture();

        long firstIntegrationInstanceId = createUpgradeInstance(
            FIRST_UPGRADE_USER_ID, upgradeFixture.firstSourceConfigurationId(),
            upgradeFixture.firstSourceConfigurationWorkflowId());
        long secondIntegrationInstanceId = createUpgradeInstance(
            FIRST_UPGRADE_USER_ID, upgradeFixture.secondSourceConfigurationId(),
            upgradeFixture.secondSourceConfigurationWorkflowId());

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.upgradeConnectedUsers(
                upgradeFixture.targetConfigurationId(), List.of(FIRST_UPGRADE_USER_ID));

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(FIRST_UPGRADE_USER_ID);
        assertThat(configurationMoveResultDTO.skipped()).isEmpty();
        assertThat(configurationMoveResultDTO.needsAttentionConnectedUserIds()).isEmpty();
        assertThat(getConfigurationId(firstIntegrationInstanceId)).isEqualTo(upgradeFixture.targetConfigurationId());
        assertThat(getConfigurationId(secondIntegrationInstanceId)).isEqualTo(upgradeFixture.targetConfigurationId());
    }

    @Test
    void testUpgradeConnectedUsersReportsAUserMovedFromOneSourceAndSkippedOnAnotherAsNeedingAttention() {
        UpgradeFixture upgradeFixture = createUpgradeFixture();

        String sourceOnlyWorkflowId = addCronWorkflow(upgradeFixture.integrationId(), 2, UUID.randomUUID());

        long sourceOnlyConfigurationWorkflowId = createConfigurationWorkflow(
            upgradeFixture.secondSourceConfigurationId(), sourceOnlyWorkflowId, OLD_CRON_EXPRESSION);

        long movedIntegrationInstanceId = createUpgradeInstance(
            FIRST_UPGRADE_USER_ID, upgradeFixture.firstSourceConfigurationId(),
            upgradeFixture.firstSourceConfigurationWorkflowId());
        long skippedIntegrationInstanceId = createUpgradeInstance(
            FIRST_UPGRADE_USER_ID, upgradeFixture.secondSourceConfigurationId(), sourceOnlyConfigurationWorkflowId);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.upgradeConnectedUsers(
                upgradeFixture.targetConfigurationId(), List.of(FIRST_UPGRADE_USER_ID));

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(FIRST_UPGRADE_USER_ID);
        assertThat(configurationMoveResultDTO.skipped()).hasSize(1);
        assertThat(configurationMoveResultDTO.needsAttentionConnectedUserIds())
            .containsExactly(FIRST_UPGRADE_USER_ID);
        assertThat(getConfigurationId(movedIntegrationInstanceId)).isEqualTo(upgradeFixture.targetConfigurationId());
        assertThat(getConfigurationId(skippedIntegrationInstanceId))
            .isEqualTo(upgradeFixture.secondSourceConfigurationId());
    }

    @Test
    void testGetUpgradeImpactMergesSources() {
        UpgradeFixture upgradeFixture = createUpgradeFixture();

        long firstIntegrationInstanceId = createUpgradeInstance(
            FIRST_UPGRADE_USER_ID, upgradeFixture.firstSourceConfigurationId(),
            upgradeFixture.firstSourceConfigurationWorkflowId());

        createUpgradeInstance(
            SECOND_UPGRADE_USER_ID, upgradeFixture.secondSourceConfigurationId(),
            upgradeFixture.secondSourceConfigurationWorkflowId());

        IntegrationInstanceConfigurationWorkflow targetConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                upgradeFixture.targetConfigurationWorkflowId());

        targetConfigurationWorkflow.setInputs(Map.of("unrelated", "value"));

        integrationInstanceConfigurationWorkflowService.update(targetConfigurationWorkflow);

        authenticate(admin());

        ConfigurationMoveImpactDTO configurationMoveImpactDTO =
            integrationInstanceConfigurationFacade.getUpgradeImpact(
                upgradeFixture.targetConfigurationId(), List.of(FIRST_UPGRADE_USER_ID, SECOND_UPGRADE_USER_ID));

        assertThat(configurationMoveImpactDTO.skipped()).isEmpty();
        assertThat(configurationMoveImpactDTO.workflows()).hasSize(1);

        AutomationProjectConfigurationVersionImpactDTO.Workflow impactWorkflow =
            configurationMoveImpactDTO.workflows()
                .getFirst();

        assertThat(impactWorkflow.workflowUuid()).isEqualTo(upgradeFixture.workflowUuid()
            .toString());
        assertThat(impactWorkflow.newRequiredInputs()).containsExactly("cronExpression");
        assertThat(impactWorkflow.affectedConnectedUserCount()).isEqualTo(2);
        assertThat(getConfigurationId(firstIntegrationInstanceId))
            .isEqualTo(upgradeFixture.firstSourceConfigurationId());
    }

    @Test
    void testUpgradeIsDeniedToNonAdmins() {
        UpgradeFixture upgradeFixture = createUpgradeFixture();

        long firstIntegrationInstanceId = createUpgradeInstance(
            FIRST_UPGRADE_USER_ID, upgradeFixture.firstSourceConfigurationId(),
            upgradeFixture.firstSourceConfigurationWorkflowId());

        authenticate(nonAdmin());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.upgradeConnectedUsers(
                upgradeFixture.targetConfigurationId(), List.of(FIRST_UPGRADE_USER_ID)))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.getUpgradeImpact(
                upgradeFixture.targetConfigurationId(), List.of(FIRST_UPGRADE_USER_ID)))
                    .isInstanceOf(AccessDeniedException.class);
        assertThat(getConfigurationId(firstIntegrationInstanceId))
            .isEqualTo(upgradeFixture.firstSourceConfigurationId());
    }

    @Test
    void testChangeConnectedUserVersionMovesTheUserToAnEnabledConfigurationOnThatVersion() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        long targetConfigurationId = createConfiguration(
            changeVersionFixture.integrationId(), 2, AuthorizationType.API_KEY);

        createConfigurationWorkflow(targetConfigurationId, changeVersionFixture.otherWorkflowId(), NEW_CRON_EXPRESSION);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, null);

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);
        assertThat(configurationMoveResultDTO.targetConfigurationId()).isEqualTo(targetConfigurationId);
        assertThat(getConfigurationId(changeVersionFixture.integrationInstanceId())).isEqualTo(targetConfigurationId);
        assertThat(getConfigurationId(changeVersionFixture.otherIntegrationInstanceId()))
            .isEqualTo(changeVersionFixture.sourceConfigurationId());
        assertThat(getIntegrationConfigurationIds(changeVersionFixture.integrationId()))
            .containsExactlyInAnyOrder(changeVersionFixture.sourceConfigurationId(), targetConfigurationId);
    }

    @Test
    void testChangeConnectedUserVersionMovesTheUserToTheGivenTarget() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        long firstTargetConfigurationId = createConfiguration(
            changeVersionFixture.integrationId(), 2, AuthorizationType.API_KEY);
        long secondTargetConfigurationId = createConfiguration(
            changeVersionFixture.integrationId(), 2, AuthorizationType.API_KEY);

        createConfigurationWorkflow(
            firstTargetConfigurationId, changeVersionFixture.otherWorkflowId(), NEW_CRON_EXPRESSION);
        createConfigurationWorkflow(
            secondTargetConfigurationId, changeVersionFixture.otherWorkflowId(), NEW_CRON_EXPRESSION);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, secondTargetConfigurationId);

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);
        assertThat(getConfigurationId(changeVersionFixture.integrationInstanceId()))
            .isEqualTo(secondTargetConfigurationId);
    }

    @Test
    void testChangeConnectedUserVersionCopiesTheSourceWhenNoConfigurationRunsThatVersion() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        String newWorkflowId = addCronWorkflow(changeVersionFixture.integrationId(), 2, UUID.randomUUID());

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, null);

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);

        IntegrationInstanceConfiguration copyConfiguration = getCopyConfiguration(
            changeVersionFixture.integrationId(), List.of(changeVersionFixture.sourceConfigurationId()));

        assertThat(copyConfiguration.getName()).isEqualTo(changeVersionFixture.sourceConfigurationName() + " · V2");
        assertThat(configurationMoveResultDTO.targetConfigurationId()).isEqualTo(copyConfiguration.getId());
        assertThat(copyConfiguration.getIntegrationVersion()).isEqualTo(2);
        assertThat(copyConfiguration.isEnabled()).isTrue();
        assertThat(copyConfiguration.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(copyConfiguration.getAuthorizationType()).isEqualTo(AuthorizationType.API_KEY);
        assertThat(copyConfiguration.getDescription()).isEqualTo("Source description");
        assertThat(getConfigurationId(changeVersionFixture.integrationInstanceId()))
            .isEqualTo(copyConfiguration.getId());
        assertThat(getConfigurationId(changeVersionFixture.otherIntegrationInstanceId()))
            .isEqualTo(changeVersionFixture.sourceConfigurationId());

        IntegrationInstanceConfigurationWorkflow carriedConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                copyConfiguration.getId(), changeVersionFixture.otherWorkflowId());

        assertThat(carriedConfigurationWorkflow.isEnabled()).isTrue();
        assertThat(carriedConfigurationWorkflow.getInputs())
            .isEqualTo(Map.of("cronExpression", OLD_CRON_EXPRESSION));

        IntegrationInstanceConfigurationWorkflow newConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                copyConfiguration.getId(), newWorkflowId);

        assertThat(newConfigurationWorkflow.isEnabled()).isTrue();
        assertThat(newConfigurationWorkflow.getInputs()).isEmpty();
    }

    @Test
    void testChangeConnectedUserVersionLeavesANewWorkflowNeedingAdminConnectionsDisabled() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        String slackWorkflowId = addWorkflow(
            changeVersionFixture.integrationId(), 2, UUID.randomUUID(), SLACK_TRIGGER_WORKFLOW_DEFINITION);

        when(componentConnectionFacade.getComponentConnections(
            argThat((WorkflowTrigger workflowTrigger) -> workflowTrigger != null &&
                Objects.equals(workflowTrigger.getName(), "slack_trigger_1"))))
                    .thenReturn(List.of(new ComponentConnection("slack", 1, "slack_trigger_1", "slack", true)));

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, null);

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);

        IntegrationInstanceConfiguration copyConfiguration = getCopyConfiguration(
            changeVersionFixture.integrationId(), List.of(changeVersionFixture.sourceConfigurationId()));

        IntegrationInstanceConfigurationWorkflow slackConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                copyConfiguration.getId(), slackWorkflowId);

        assertThat(slackConfigurationWorkflow.isEnabled()).isFalse();
    }

    @Test
    void testChangeConnectedUserVersionKeepsTheCopyOutOfSelfServe() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        EmbeddedPermissionEvaluator realEmbeddedPermissionEvaluator =
            new EmbeddedPermissionEvaluator(SpelEvaluator.create());

        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenAnswer(invocation -> realEmbeddedPermissionEvaluator.evaluate(
                invocation.getArgument(0), invocation.getArgument(1)));

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, null);

        long copyConfigurationId = Objects.requireNonNull(configurationMoveResultDTO.targetConfigurationId());

        IntegrationInstanceConfiguration copyConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(copyConfigurationId);

        assertThat(copyConfiguration.isEnabled()).isTrue();
        assertThat(copyConfiguration.getIntegrationVersion()).isEqualTo(2);
        assertThat(copyConfiguration.getPermissionExpression()).isEqualTo("false");

        ConnectedUser unassignedConnectedUser = new ConnectedUser(
            Map.of(), null, true, "unassigned-user-" + UUID.randomUUID(), UNASSIGNED_CONNECTED_USER_ID, null, 0);

        unassignedConnectedUser.setEnvironment(Environment.PRODUCTION);

        List<IntegrationInstanceConfigurationDTO> selfServeConfigurations =
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationIntegrations(
                true, Environment.PRODUCTION, unassignedConnectedUser);

        assertThat(selfServeConfigurations)
            .filteredOn(integrationInstanceConfigurationDTO -> Objects.equals(
                integrationInstanceConfigurationDTO.integrationId(), changeVersionFixture.integrationId()))
            .extracting(IntegrationInstanceConfigurationDTO::id)
            .containsExactly(changeVersionFixture.sourceConfigurationId());
        assertThat(getConfigurationId(changeVersionFixture.integrationInstanceId())).isEqualTo(copyConfigurationId);
        assertThat(getConfigurationId(changeVersionFixture.otherIntegrationInstanceId()))
            .isEqualTo(changeVersionFixture.sourceConfigurationId());
    }

    @Test
    void testChangeConnectedUserVersionDowngrades() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(2, true);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 1, null);

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);

        IntegrationInstanceConfiguration copyConfiguration = getCopyConfiguration(
            changeVersionFixture.integrationId(), List.of(changeVersionFixture.sourceConfigurationId()));

        assertThat(copyConfiguration.getName()).isEqualTo(changeVersionFixture.sourceConfigurationName() + " · V1");
        assertThat(copyConfiguration.getIntegrationVersion()).isEqualTo(1);
        assertThat(getConfigurationId(changeVersionFixture.integrationInstanceId()))
            .isEqualTo(copyConfiguration.getId());
    }

    @Test
    void testChangeConnectedUserVersionRejectsTheCurrentVersion() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        authenticate(admin());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 1, null))
                    .isInstanceOf(IllegalArgumentException.class);

        assertNothingChanged(changeVersionFixture);
    }

    @Test
    void testChangeConnectedUserVersionRejectsAnUnpublishedVersion() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        addCronWorkflow(changeVersionFixture.integrationId(), 3, UUID.randomUUID());

        authenticate(admin());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 3, null))
                    .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 7, null))
                    .isInstanceOf(IllegalArgumentException.class);

        assertNothingChanged(changeVersionFixture);
    }

    @Test
    void testChangeConnectedUserVersionRejectsAnInvalidTarget() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        long stagingConfigurationId = createConfiguration(
            changeVersionFixture.integrationId(), 2, AuthorizationType.API_KEY, Environment.STAGING);
        long disabledConfigurationId = createConfiguration(
            changeVersionFixture.integrationId(), 2, AuthorizationType.API_KEY);
        long otherVersionConfigurationId = createConfiguration(
            changeVersionFixture.integrationId(), 1, AuthorizationType.API_KEY);

        integrationInstanceConfigurationService.updateEnabled(disabledConfigurationId, false);

        authenticate(admin());

        for (long targetConfigurationId : List.of(
            stagingConfigurationId, disabledConfigurationId, otherVersionConfigurationId)) {

            assertThatThrownBy(
                () -> integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                    changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, targetConfigurationId))
                        .isInstanceOf(IllegalArgumentException.class);
        }

        assertThat(getConfigurationId(changeVersionFixture.integrationInstanceId()))
            .isEqualTo(changeVersionFixture.sourceConfigurationId());
        assertThat(getIntegrationConfigurationIds(changeVersionFixture.integrationId()))
            .containsExactlyInAnyOrder(
                changeVersionFixture.sourceConfigurationId(), stagingConfigurationId, disabledConfigurationId,
                otherVersionConfigurationId);
    }

    @Test
    void testChangeConnectedUserVersionDeletesTheCopyWhenTheMoveSkipsTheUser() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, false);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, null);

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).isEmpty();
        assertThat(configurationMoveResultDTO.targetConfigurationId()).isNull();
        assertThat(configurationMoveResultDTO.skipped())
            .extracting(ConfigurationMoveResultDTO.Skipped::reason)
            .containsExactly(ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_ENABLED_WORKFLOWS);

        assertNothingChanged(changeVersionFixture);
    }

    @Test
    void testChangeConnectedUserVersionSuffixesACollidingCopyName() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        long collidingConfigurationId = createConfiguration(
            changeVersionFixture.integrationId(), 2, AuthorizationType.API_KEY);

        IntegrationInstanceConfiguration collidingConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(collidingConfigurationId);

        String sourceConfigurationName = changeVersionFixture.sourceConfigurationName();

        collidingConfiguration.setName(sourceConfigurationName.toUpperCase() + " · V2");

        integrationInstanceConfigurationService.update(collidingConfiguration);

        integrationInstanceConfigurationService.updateEnabled(collidingConfigurationId, false);

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, null);

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);

        IntegrationInstanceConfiguration copyConfiguration = getCopyConfiguration(
            changeVersionFixture.integrationId(),
            List.of(changeVersionFixture.sourceConfigurationId(), collidingConfigurationId));

        assertThat(copyConfiguration.getName()).isEqualTo(sourceConfigurationName + " · V2 (2)");
    }

    @Test
    void testChangeConnectedUserVersionDropsAConnectionWhoseNodeIsMissingFromTheTargetVersion() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        IntegrationInstanceConfigurationWorkflowConnection carriedConnection =
            new IntegrationInstanceConfigurationWorkflowConnection(1001L, "schedule", "trigger_1");
        IntegrationInstanceConfigurationWorkflowConnection removedNodeConnection =
            new IntegrationInstanceConfigurationWorkflowConnection(1002L, "slack", "removed_node_1");

        List<IntegrationInstanceConfigurationWorkflow> sourceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                changeVersionFixture.sourceConfigurationId());

        IntegrationInstanceConfigurationWorkflow sourceConfigurationWorkflow = sourceConfigurationWorkflows.getFirst();

        sourceConfigurationWorkflow.setConnections(List.of(carriedConnection, removedNodeConnection));

        integrationInstanceConfigurationWorkflowService.update(sourceConfigurationWorkflow);

        ComponentConnection scheduleComponentConnection = new ComponentConnection(
            "schedule", 1, "trigger_1", "schedule", false);

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTrigger.class)))
            .thenReturn(List.of(scheduleComponentConnection));
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("trigger_1"), eq("schedule")))
            .thenReturn(scheduleComponentConnection);
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("removed_node_1"), anyString()))
            .thenThrow(new NoSuchElementException("No value present"));

        Connection scheduleConnection = new Connection();

        scheduleConnection.setComponentName("schedule");

        Connection slackConnection = new Connection();

        slackConnection.setComponentName("slack");

        when(connectionService.getConnection(1001L)).thenReturn(scheduleConnection);
        when(connectionService.getConnection(1002L)).thenReturn(slackConnection);
        when(connectionService.fetchConnection(1001L)).thenReturn(Optional.of(scheduleConnection));
        when(connectionService.fetchConnection(1002L)).thenReturn(Optional.of(slackConnection));

        authenticate(admin());

        ConfigurationMoveResultDTO configurationMoveResultDTO =
            integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, null);

        assertThat(configurationMoveResultDTO.movedConnectedUserIds()).containsExactly(CONNECTED_USER_ID);

        IntegrationInstanceConfiguration copyConfiguration = getCopyConfiguration(
            changeVersionFixture.integrationId(), List.of(changeVersionFixture.sourceConfigurationId()));

        assertThat(getConfigurationId(changeVersionFixture.integrationInstanceId()))
            .isEqualTo(copyConfiguration.getId());

        IntegrationInstanceConfigurationWorkflow copyConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                copyConfiguration.getId(), changeVersionFixture.otherWorkflowId());

        assertThat(copyConfigurationWorkflow.isEnabled()).isTrue();
        assertThat(copyConfigurationWorkflow.getConnections())
            .extracting(
                IntegrationInstanceConfigurationWorkflowConnection::getConnectionId,
                IntegrationInstanceConfigurationWorkflowConnection::getWorkflowNodeName)
            .containsExactly(tuple(1001L, "trigger_1"));
    }

    @Test
    void testChangeConnectedUserVersionDeletesTheCopyWhenTheMoveThrows() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        IllegalStateException moveFailure = new IllegalStateException("move failed");
        List<List<Long>> configurationIdsAtFailure = new ArrayList<>();

        doAnswer(invocation -> {
            configurationIdsAtFailure.add(getIntegrationConfigurationIds(changeVersionFixture.integrationId()));

            throw moveFailure;
        }).when(integrationInstanceService)
            .getIntegrationInstances(changeVersionFixture.sourceConfigurationId());

        authenticate(admin());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, null))
                    .isSameAs(moveFailure);

        reset(integrationInstanceService);

        assertThat(configurationIdsAtFailure).hasSize(1);
        assertThat(configurationIdsAtFailure.getFirst()).hasSize(2);

        assertNothingChanged(changeVersionFixture);
    }

    @Test
    void testChangeConnectedUserVersionIsDeniedToNonAdmins() {
        ChangeVersionFixture changeVersionFixture = createChangeVersionFixture(1, true);

        authenticate(nonAdmin());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.changeConnectedUserVersion(
                changeVersionFixture.sourceConfigurationId(), CONNECTED_USER_ID, 2, null))
                    .isInstanceOf(AccessDeniedException.class);

        assertNothingChanged(changeVersionFixture);
    }

    @Test
    void testGetConfigurationConnectedUsers() {
        MoveFixture moveFixture = createMoveFixture(true, AuthorizationType.API_KEY);

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(CONNECTED_USER_ID);
        when(connectedUser.getExternalId()).thenReturn("external-user-1");
        when(connectedUser.getName()).thenReturn("Alice");
        when(connectedUser.getEmail()).thenReturn("alice@example.com");
        when(connectedUserService.getConnectedUsers(List.of(CONNECTED_USER_ID))).thenReturn(List.of(connectedUser));

        authenticate(admin());

        assertThat(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationConnectedUsers(
                moveFixture.sourceConfigurationId()))
                    .containsExactly(
                        new IntegrationInstanceConfigurationConnectedUserDTO(
                            CONNECTED_USER_ID, "external-user-1", "Alice", "alice@example.com"));
        assertThat(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationConnectedUsers(
                moveFixture.targetConfigurationId())).isEmpty();
    }

    @Test
    void testTheListReadStillHasExactlyOneCaller() throws IOException {
        Path sourceRoot = moduleSourceRoot();

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            List<String> callers = paths.filter(path -> path.toString()
                .endsWith(".java"))
                .filter(path -> {
                    try {
                        String source = Files.readString(path);

                        return source.contains("getIntegrationInstanceConfigurationIntegrations(")
                            && !path.endsWith("IntegrationInstanceConfigurationFacadeImpl.java");
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                })
                .map(path -> String.valueOf(path.getFileName()))
                .toList();

            assertThat(callers)
                .as(
                    "a new caller of the unguarded list read must apply its own per-row filtering; see the method's " +
                        "Javadoc")
                .containsExactly("ConnectedUserIntegrationFacadeImpl.java");
        }
    }

    @Test
    void testTheListReadStillHasExactlyOneCallerWorthOfArguments() throws NoSuchMethodException {
        Method method = IntegrationInstanceConfigurationFacadeImpl.class.getMethod(
            "getIntegrationInstanceConfigurationIntegrations", boolean.class, Environment.class, ConnectedUser.class);

        assertThat(Arrays.stream(method.getParameterTypes()))
            .noneMatch(parameterType -> parameterType == long.class || parameterType == Long.class);
    }

    private static Path moduleSourceRoot() {
        Path path;

        try {
            path = Path.of(
                IntegrationInstanceConfigurationFacadeIntTest.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(exception);
        }

        while (path != null && !Files.isDirectory(path.resolve("src/main/java"))) {
            path = path.getParent();
        }

        if (path == null) {
            throw new IllegalStateException("Could not locate the module's src/main/java");
        }

        return path.resolve("src/main/java");
    }

    private static Authentication admin() {
        return new UsernamePasswordAuthenticationToken(
            "admin", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN)));
    }

    private static Authentication anonymous() {
        return new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    private int getLockVersion(long integrationInstanceConfigurationId) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstanceConfigurationId);

        return integrationInstanceConfiguration.getVersion();
    }

    private int bumpLockVersionPast(long integrationInstanceConfigurationId, int integrationVersion) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstanceConfigurationId);

        while (integrationInstanceConfiguration.getVersion() <= integrationVersion) {
            integrationInstanceConfigurationService.updateEnabled(
                integrationInstanceConfigurationId, !integrationInstanceConfiguration.isEnabled());

            integrationInstanceConfiguration = integrationInstanceConfigurationService
                .getIntegrationInstanceConfiguration(integrationInstanceConfigurationId);
        }

        return integrationInstanceConfiguration.getVersion();
    }

    private static Authentication connectedUser() {
        return new EmbeddedApiKeyAuthenticationToken(
            Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false);
    }

    private long createIntegrationInstanceConfiguration(long integrationId) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setAuthorizationType(AuthorizationType.API_KEY);
        integrationInstanceConfiguration.setConnectionParameters(Map.of("key", "admin-value"));
        integrationInstanceConfiguration.setEnvironment(Environment.STAGING);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        return integrationInstanceConfiguration.getId();
    }

    private void assertStillOnSource(MoveFixture moveFixture) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            moveFixture.integrationInstanceId());

        assertThat(integrationInstance.getIntegrationInstanceConfigurationId())
            .isEqualTo(moveFixture.sourceConfigurationId());

        IntegrationInstanceWorkflow integrationInstanceWorkflow = integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflows(moveFixture.integrationInstanceId())
            .getFirst();

        assertThat(integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId())
            .isEqualTo(moveFixture.sourceConfigurationWorkflowId());

        verify(triggerLifecycleFacade, never()).executeTriggerDisable(any(), any(), any(), any(), any());
    }

    private String addCronWorkflow(long integrationId, int integrationVersion, UUID workflowUuid) {
        Workflow workflow = new Workflow(
            String.valueOf(UUID.randomUUID()), CRON_WORKFLOW_DEFINITION, Workflow.Format.JSON);

        when(workflowService.getWorkflow(workflow.getId())).thenReturn(workflow);

        integrationWorkflowRepository.save(
            new IntegrationWorkflow(integrationId, integrationVersion, workflow.getId(), workflowUuid));

        return workflow.getId();
    }

    private void
        createSourceInstanceWorkflow(long connectedUserId, MoveFixture moveFixture, long configurationWorkflowId) {
        IntegrationInstance integrationInstance = integrationInstanceService.create(
            connectedUserId, 41L, moveFixture.sourceConfigurationId());

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                integrationInstance.getId(), configurationWorkflowId);

        integrationInstanceWorkflow.setEnabled(true);

        integrationInstanceWorkflowService.update(integrationInstanceWorkflow);
    }

    private String addWorkflow(long integrationId, int integrationVersion, UUID workflowUuid, String definition) {
        Workflow workflow = new Workflow(String.valueOf(UUID.randomUUID()), definition, Workflow.Format.JSON);

        when(workflowService.getWorkflow(workflow.getId())).thenReturn(workflow);

        integrationWorkflowRepository.save(
            new IntegrationWorkflow(integrationId, integrationVersion, workflow.getId(), workflowUuid));

        return workflow.getId();
    }

    private void assertNothingChanged(ChangeVersionFixture changeVersionFixture) {
        assertThat(getConfigurationId(changeVersionFixture.integrationInstanceId()))
            .isEqualTo(changeVersionFixture.sourceConfigurationId());
        assertThat(getConfigurationId(changeVersionFixture.otherIntegrationInstanceId()))
            .isEqualTo(changeVersionFixture.sourceConfigurationId());
        assertThat(getIntegrationConfigurationIds(changeVersionFixture.integrationId()))
            .containsExactly(changeVersionFixture.sourceConfigurationId());
    }

    private ChangeVersionFixture createChangeVersionFixture(int sourceVersion, boolean otherVersionSharesWorkflow) {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEnabled(true);

        when(connectedUserService.getConnectedUser(anyLong())).thenReturn(connectedUser);

        Integration integration = createIntegration();

        integrationService.publishIntegration(integration.getId(), null);
        integrationService.publishIntegration(integration.getId(), null);

        UUID workflowUuid = UUID.randomUUID();
        int otherVersion = sourceVersion == 1 ? 2 : 1;

        String sourceWorkflowId = addCronWorkflow(integration.getId(), sourceVersion, workflowUuid);
        String otherWorkflowId = addCronWorkflow(
            integration.getId(), otherVersion, otherVersionSharesWorkflow ? workflowUuid : UUID.randomUUID());

        long sourceConfigurationId = createConfiguration(integration.getId(), sourceVersion, AuthorizationType.API_KEY);

        IntegrationInstanceConfiguration sourceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(sourceConfigurationId);

        sourceConfiguration.setDescription("Source description");

        sourceConfiguration = integrationInstanceConfigurationService.update(sourceConfiguration);

        long sourceConfigurationWorkflowId = createConfigurationWorkflow(
            sourceConfigurationId, sourceWorkflowId, OLD_CRON_EXPRESSION);

        long integrationInstanceId = createUpgradeInstance(
            CONNECTED_USER_ID, sourceConfigurationId, sourceConfigurationWorkflowId);
        long otherIntegrationInstanceId = createUpgradeInstance(
            OTHER_CONNECTED_USER_ID, sourceConfigurationId, sourceConfigurationWorkflowId);

        reset(triggerLifecycleFacade);

        return new ChangeVersionFixture(
            integration.getId(), sourceConfigurationId, sourceConfiguration.getName(), integrationInstanceId,
            otherIntegrationInstanceId, otherWorkflowId);
    }

    private IntegrationInstanceConfiguration getCopyConfiguration(long integrationId, List<Long> excludedIds) {
        List<IntegrationInstanceConfiguration> copyConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(integrationId)
                .stream()
                .filter(configuration -> !excludedIds.contains(configuration.getId()))
                .toList();

        assertThat(copyConfigurations).hasSize(1);

        return copyConfigurations.getFirst();
    }

    private List<Long> getIntegrationConfigurationIds(long integrationId) {
        return integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(integrationId)
            .stream()
            .map(IntegrationInstanceConfiguration::getId)
            .toList();
    }

    private long createConfiguration(long integrationId, int integrationVersion, AuthorizationType authorizationType) {
        return createConfiguration(integrationId, integrationVersion, authorizationType, Environment.PRODUCTION);
    }

    private long createConfiguration(
        long integrationId, int integrationVersion, AuthorizationType authorizationType, Environment environment) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setAuthorizationType(authorizationType);
        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(integrationVersion);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfiguration.getId(), true);

        return integrationInstanceConfiguration.getId();
    }

    private IntegrationInstanceConfigurationWorkflow createInternalOnlyConfigurationWorkflow(
        String definition, boolean enabled, Map<String, ?> inputs) {

        Integration integration = createIntegration();

        String workflowId = addWorkflow(integration.getId(), 1, UUID.randomUUID(), definition);

        long integrationInstanceConfigurationId = createEnabledIntegrationInstanceConfiguration(integration.getId());

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(enabled);
        integrationInstanceConfigurationWorkflow.setInputs(inputs);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        return integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);
    }

    private long createConfigurationWorkflow(long configurationId, String workflowId, String cronExpression) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setInputs(Map.of("cronExpression", cronExpression));
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(configurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        integrationInstanceConfigurationWorkflow = integrationInstanceConfigurationWorkflowService.create(
            integrationInstanceConfigurationWorkflow);

        return integrationInstanceConfigurationWorkflow.getId();
    }

    private MoveFixture createMoveFixture(boolean targetHasWorkflow, AuthorizationType targetAuthorizationType) {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEnabled(true);

        when(connectedUserService.getConnectedUser(CONNECTED_USER_ID)).thenReturn(connectedUser);

        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        UUID workflowUuid = UUID.randomUUID();

        String sourceWorkflowId = addCronWorkflow(integration.getId(), 1, workflowUuid);
        String targetWorkflowId = addCronWorkflow(integration.getId(), 2, workflowUuid);

        long sourceConfigurationId = createConfiguration(integration.getId(), 1, AuthorizationType.API_KEY);
        long targetConfigurationId = createConfiguration(integration.getId(), 2, targetAuthorizationType);

        long sourceConfigurationWorkflowId = createConfigurationWorkflow(
            sourceConfigurationId, sourceWorkflowId, OLD_CRON_EXPRESSION);
        long targetConfigurationWorkflowId = -1;

        if (targetHasWorkflow) {
            targetConfigurationWorkflowId = createConfigurationWorkflow(
                targetConfigurationId, targetWorkflowId, NEW_CRON_EXPRESSION);
        }

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, 41L, sourceConfigurationId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                integrationInstance.getId(), sourceConfigurationWorkflowId);

        integrationInstanceWorkflow.setEnabled(true);
        integrationInstanceWorkflow.setInputs(Map.of("owner", "alice"));

        integrationInstanceWorkflowService.update(integrationInstanceWorkflow);

        reset(triggerLifecycleFacade);

        return new MoveFixture(
            integrationInstance.getId(), sourceConfigurationId, sourceConfigurationWorkflowId, sourceWorkflowId,
            targetConfigurationId, targetConfigurationWorkflowId, targetWorkflowId);
    }

    private UpgradeFixture createUpgradeFixture() {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEnabled(true);

        when(connectedUserService.getConnectedUser(anyLong())).thenReturn(connectedUser);

        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        UUID workflowUuid = UUID.randomUUID();

        String firstSourceWorkflowId = addCronWorkflow(integration.getId(), 1, workflowUuid);
        String secondSourceWorkflowId = addCronWorkflow(integration.getId(), 2, workflowUuid);
        String targetWorkflowId = addCronWorkflow(integration.getId(), 3, workflowUuid);

        long firstSourceConfigurationId = createConfiguration(integration.getId(), 1, AuthorizationType.API_KEY);
        long secondSourceConfigurationId = createConfiguration(integration.getId(), 2, AuthorizationType.API_KEY);
        long targetConfigurationId = createConfiguration(integration.getId(), 3, AuthorizationType.API_KEY);

        long firstSourceConfigurationWorkflowId = createConfigurationWorkflow(
            firstSourceConfigurationId, firstSourceWorkflowId, OLD_CRON_EXPRESSION);
        long secondSourceConfigurationWorkflowId = createConfigurationWorkflow(
            secondSourceConfigurationId, secondSourceWorkflowId, OLD_CRON_EXPRESSION);
        long targetConfigurationWorkflowId = createConfigurationWorkflow(
            targetConfigurationId, targetWorkflowId, NEW_CRON_EXPRESSION);

        reset(triggerLifecycleFacade);

        return new UpgradeFixture(
            integration.getId(), workflowUuid, firstSourceWorkflowId, firstSourceConfigurationId,
            firstSourceConfigurationWorkflowId, secondSourceConfigurationId, secondSourceConfigurationWorkflowId,
            targetConfigurationId, targetConfigurationWorkflowId);
    }

    private long createUpgradeInstance(long connectedUserId, long configurationId, long configurationWorkflowId) {
        IntegrationInstance integrationInstance = integrationInstanceService.create(
            connectedUserId, 41L, configurationId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                integrationInstance.getId(), configurationWorkflowId);

        integrationInstanceWorkflow.setEnabled(true);
        integrationInstanceWorkflow.setInputs(Map.of("owner", "alice"));

        integrationInstanceWorkflowService.update(integrationInstanceWorkflow);

        return integrationInstance.getId();
    }

    private long getConfigurationId(long integrationInstanceId) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        return integrationInstance.getIntegrationInstanceConfigurationId();
    }

    private long createEnabledIntegrationInstanceConfiguration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return createEnabledIntegrationInstanceConfiguration(integration.getId());
    }

    private long createEnabledIntegrationInstanceConfiguration(long integrationId) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        long integrationInstanceConfigurationId = integrationInstanceConfiguration.getId();

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfigurationId, true);

        return integrationInstanceConfigurationId;
    }

    private Integration createIntegration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        return integrationService.create(integration);
    }

    private Integration createPublishedIntegration() {
        Integration integration = createIntegration();

        integrationService.publishIntegration(integration.getId(), null);

        return integrationService.getIntegration(integration.getId());
    }

    private static IntegrationInstanceConfigurationDTO newIntegrationInstanceConfigurationDTO(
        long integrationId, int integrationVersion) {

        return IntegrationInstanceConfigurationDTO.builder()
            .authorizationType(AuthorizationType.API_KEY)
            .connectionParameters(Map.of("key", "admin-value"))
            .environment(Environment.STAGING)
            .integrationId(integrationId)
            .integrationInstanceConfigurationWorkflows(List.of())
            .integrationVersion(integrationVersion)
            .name("instance-configuration-" + UUID.randomUUID())
            .tags(List.of())
            .build();
    }

    private CronFixture createCronFixture(List<IntegrationInstanceConfigurationWorkflowConnection> connections) {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEnabled(true);

        when(connectedUserService.getConnectedUser(CONNECTED_USER_ID)).thenReturn(connectedUser);

        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        Workflow workflow = new Workflow(
            String.valueOf(UUID.randomUUID()), CRON_WORKFLOW_DEFINITION, Workflow.Format.JSON);

        when(workflowService.getWorkflow(workflow.getId())).thenReturn(workflow);

        integrationWorkflowService.addWorkflow(integration.getId(), 1, workflow.getId());

        long integrationInstanceConfigurationId = createEnabledIntegrationInstanceConfiguration(integration.getId());

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setConnections(connections);
        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setInputs(Map.of("cronExpression", OLD_CRON_EXPRESSION));
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflow.getId());

        integrationInstanceConfigurationWorkflow = integrationInstanceConfigurationWorkflowService.create(
            integrationInstanceConfigurationWorkflow);

        long firstIntegrationInstanceId = createIntegrationInstance(
            integrationInstanceConfigurationId, integrationInstanceConfigurationWorkflow.getId(), true);
        long secondIntegrationInstanceId = createIntegrationInstance(
            integrationInstanceConfigurationId, integrationInstanceConfigurationWorkflow.getId(), true);
        long inactiveIntegrationInstanceId = createIntegrationInstance(
            integrationInstanceConfigurationId, integrationInstanceConfigurationWorkflow.getId(), false);

        reset(triggerLifecycleFacade);

        return new CronFixture(
            integrationInstanceConfigurationId, integrationInstanceConfigurationWorkflow.getId(),
            List.of(firstIntegrationInstanceId, secondIntegrationInstanceId), inactiveIntegrationInstanceId);
    }

    private long createIntegrationInstance(
        long integrationInstanceConfigurationId, long integrationInstanceConfigurationWorkflowId,
        boolean workflowEnabled) {

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, 41L, integrationInstanceConfigurationId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                integrationInstance.getId(), integrationInstanceConfigurationWorkflowId);

        integrationInstanceWorkflowService.updateEnabled(integrationInstanceWorkflow.getId(), workflowEnabled);

        return integrationInstance.getId();
    }

    private IntegrationInstanceConfigurationWorkflow getConfigurationWorkflow(CronFixture cronFixture) {
        return integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
            cronFixture.integrationInstanceConfigurationWorkflowId());
    }

    private static boolean hasExpression(Map<String, ?> parameters, String expression) {
        return parameters != null && Objects.equals(parameters.get("expression"), expression);
    }

    private static boolean isFor(WorkflowExecutionId workflowExecutionId, long integrationInstanceId) {
        return workflowExecutionId != null && workflowExecutionId.getJobPrincipalId() == integrationInstanceId;
    }

    private static IntegrationInstanceConfigurationWorkflow withCronExpression(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow, String cronExpression) {

        integrationInstanceConfigurationWorkflow.setInputs(Map.of("cronExpression", cronExpression));

        return integrationInstanceConfigurationWorkflow;
    }

    private static Authentication nonAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "user", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    private record MoveFixture(
        long integrationInstanceId, long sourceConfigurationId, long sourceConfigurationWorkflowId,
        String sourceWorkflowId, long targetConfigurationId, long targetConfigurationWorkflowId,
        String targetWorkflowId) {
    }

    private record ChangeVersionFixture(
        long integrationId, long sourceConfigurationId, String sourceConfigurationName, long integrationInstanceId,
        long otherIntegrationInstanceId, String otherWorkflowId) {
    }

    private record CronFixture(
        long integrationInstanceConfigurationId, long integrationInstanceConfigurationWorkflowId,
        List<Long> activeIntegrationInstanceIds, long inactiveIntegrationInstanceId) {
    }

    private record UpgradeFixture(
        long integrationId, UUID workflowUuid, String firstSourceWorkflowId, long firstSourceConfigurationId,
        long firstSourceConfigurationWorkflowId, long secondSourceConfigurationId,
        long secondSourceConfigurationWorkflowId, long targetConfigurationId, long targetConfigurationWorkflowId) {
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.isTenantAdmin())
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
        }
    }
}
