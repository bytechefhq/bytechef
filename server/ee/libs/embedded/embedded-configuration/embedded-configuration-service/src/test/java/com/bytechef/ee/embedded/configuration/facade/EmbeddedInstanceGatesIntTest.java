/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowCreator;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.security.scope.ApiKeyPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ApiPlatformPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ConnectionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DataTablePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DeploymentPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ExecutionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.KnowledgeBasePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.McpPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ProjectPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkflowPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkspacePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.AppEventPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.ConnectedUserPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.EmbeddedPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.EmbeddedSharedPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.IntegrationInstanceConfigurationPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.IntegrationPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.CustomRoleScopeResolverImpl;
import com.bytechef.ee.automation.configuration.service.EmbeddedScopeCacheService;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.repository.AppEventRepository;
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
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacadeImpl;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class, EmbeddedInstanceGatesIntTest.ConnectedUserServiceConfiguration.class,
        EmbeddedInstanceGatesIntTest.RealPermissionConfiguration.class
    },
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true", "spring.liquibase.contexts=configuration,user"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, ClusterElementDefinitionService.class,
    ComponentConnectionFacade.class, ComponentDefinitionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class, JobFacade.class, JobService.class,
    McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class,
    ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectFacade.class, ProjectService.class,
    ProjectWorkflowCreator.class, ProjectWorkflowFacade.class, ProjectWorkflowService.class,
    TaskDispatcherDefinitionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, WebhookTriggerTestFacade.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class,
    WorkflowTestConfigurationService.class
})
class EmbeddedInstanceGatesIntTest {

    private static final String DEFINITION = """
        {"label":"Sync contacts","inputs":[],"triggers":[],"tasks":[]}
        """;
    private static final String EDITOR_IN_PRODUCTION = "editor-in-production";
    private static final String EMBEDDED_ADMIN = "embedded-admin";
    private static final String EMBEDDED_VIEWER = "embedded-viewer";
    private static final String NON_MEMBER = "non-member";
    private static final String PRODUCTION_ADMIN = "production-admin";
    private static final String TENANT_ADMIN = "tenant-admin";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        TENANT_ADMIN, 9600L, EMBEDDED_VIEWER, 9601L, EDITOR_IN_PRODUCTION, 9602L, EMBEDDED_ADMIN, 9603L,
        NON_MEMBER, 9604L, PRODUCTION_ADMIN, 9605L);

    @Autowired
    private AppEventFacade appEventFacade;

    @Autowired
    private AppEventRepository appEventRepository;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IntegrationFacade integrationFacade;

    @Autowired
    private JobFacade jobFacade;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Autowired
    private IntegrationInstanceAdminFacade integrationInstanceAdminFacade;

    @Autowired
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationInstanceFacade integrationInstanceFacade;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowRepository integrationInstanceConfigurationWorkflowRepository;

    @Autowired
    private IntegrationInstanceRepository integrationInstanceRepository;

    @Autowired
    private IntegrationInstanceWorkflowRepository integrationInstanceWorkflowRepository;

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

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectionDefinitionService connectionDefinitionService;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private WorkflowValidatorFacade workflowValidatorFacade;

    private ConnectedUser connectedUser;
    private long developmentConfigurationId;
    private long integrationId;
    private String externalUserId;
    private long integrationInstanceId;
    private long productionConfigurationId;
    private String workflowId;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment(anyLong()))
            .thenAnswer(invocation -> Environment.values()[((Long) invocation.getArgument(0)).intValue()]);
        when(componentDefinitionService.getComponentDefinition(anyString(), any()))
            .thenReturn(new ComponentDefinition("componentName"));
        when(connectionDefinitionService.getConnectionDefinition(anyString(), any()))
            .thenReturn(new ConnectionDefinition(ComponentDsl.connection(), "componentName", null, null));

        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EMBEDDED_VIEWER), WorkspaceRole.VIEWER));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(EDITOR_IN_PRODUCTION), WorkspaceRole.EDITOR, Environment.PRODUCTION));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EMBEDDED_ADMIN), WorkspaceRole.ADMIN));

        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(PRODUCTION_ADMIN), WorkspaceRole.ADMIN, Environment.PRODUCTION));

        permissionService.evictAllEmbeddedScopeCache();

        externalUserId = "external-user-" + UUID.randomUUID();

        connectedUser = connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        Integration integration = createPublishedIntegration();

        Workflow workflow = workflowService.create(DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        workflowId = workflow.getId();
        integrationId = integration.getId();

        integrationWorkflowService.addWorkflow(integration.getId(), 1, workflowId);

        productionConfigurationId = createConfiguration(integration.getId(), Environment.PRODUCTION, 1);
        developmentConfigurationId = createConfiguration(integration.getId(), Environment.DEVELOPMENT, 2);

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(productionConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            connectedUser.getId(), 41L, productionConfigurationId);

        integrationInstanceId = integrationInstance.getId();

        integrationInstanceService.updateEnabled(integrationInstanceId, true);
        integrationInstanceConfigurationService.updateEnabled(productionConfigurationId, true);
        integrationInstanceConfigurationService.updateEnabled(developmentConfigurationId, true);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            embeddedUserRepository.deleteByUserId(userId);
        }

        permissionService.evictAllEmbeddedScopeCache();

        appEventRepository.deleteAll();
        integrationInstanceWorkflowRepository.deleteAll();
        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationWorkflowRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();

        connectedUserService.deleteConnectedUser(connectedUser.getId());

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }
    }

    @Test
    void testAProductionEditorEditsOnlyProductionConfigurations() {
        authenticate(EDITOR_IN_PRODUCTION, false);

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
            productionConfigurationId, List.of());
        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfiguration(productionConfigurationId, false);

        assertThat(integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(
            productionConfigurationId)).isNotNull();
        assertThat(integrationInstanceFacade.getIntegrationInstance(integrationInstanceId)).isNotNull();

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
                developmentConfigurationId, List.of()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.enableIntegrationInstanceConfiguration(
                developmentConfigurationId, true))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(
                developmentConfigurationId))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.deleteIntegrationInstanceConfiguration(
                productionConfigurationId))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationInstanceFacade.deleteIntegrationInstance(integrationInstanceId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurations(
                (long) Environment.PRODUCTION.ordinal(), null, null, false))
                    .extracting(IntegrationInstanceConfigurationDTO::id)
                    .containsExactly(productionConfigurationId);

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurations(
                (long) Environment.DEVELOPMENT.ordinal(), null, null, false))
                    .isInstanceOf(AccessDeniedException.class);

        authenticate(EMBEDDED_ADMIN, false);

        integrationInstanceConfigurationFacade.deleteIntegrationInstanceConfiguration(productionConfigurationId);

        assertThat(integrationInstanceConfigurationRepository.findById(productionConfigurationId)).isEmpty();
        assertThat(integrationInstanceRepository.findById(integrationInstanceId)).isEmpty();
        assertThat(integrationInstanceConfigurationRepository.findById(developmentConfigurationId)).isPresent();
    }

    @Test
    void testTheTagListShowsOnlyTagsFromEnvironmentsTheCallerCanView() {
        String productionTagName = "production-tag-" + UUID.randomUUID();
        String developmentTagName = "development-tag-" + UUID.randomUUID();

        authenticate(EMBEDDED_ADMIN, false);

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
            productionConfigurationId, List.of(new Tag(productionTagName)));
        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
            developmentConfigurationId, List.of(new Tag(developmentTagName)));

        assertThat(integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationTags())
            .extracting(Tag::getName)
            .contains(productionTagName, developmentTagName);

        authenticate(EDITOR_IN_PRODUCTION, false);

        assertThat(integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationTags())
            .extracting(Tag::getName)
            .contains(productionTagName)
            .doesNotContain(developmentTagName);
    }

    @Test
    void testAProductionEditorCreatesConfigurationsOnlyInProduction() {
        Integration integration = createPublishedIntegration();

        authenticate(EDITOR_IN_PRODUCTION, false);

        assertThat(
            integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                newIntegrationInstanceConfigurationDTO(integration.getId(), Environment.PRODUCTION)))
                    .isPositive();

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                newIntegrationInstanceConfigurationDTO(integration.getId(), Environment.DEVELOPMENT)))
                    .isInstanceOf(AccessDeniedException.class);

        assertThat(integrationInstanceConfigurationRepository.findAll()).hasSize(3);
    }

    @Test
    void testAProductionEditorUpdatesOnlyProductionConfigurations() {
        authenticate(EDITOR_IN_PRODUCTION, false);

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfiguration(
            new IntegrationInstanceConfigurationDTO(
                integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                    productionConfigurationId)));

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfiguration(
                new IntegrationInstanceConfigurationDTO(
                    integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                        developmentConfigurationId))))
                            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAProductionAdminCannotDeleteAnIntegrationThatHasADevelopmentConfiguration() {
        authenticate(PRODUCTION_ADMIN, false);

        assertThatThrownBy(() -> integrationFacade.deleteIntegration(integrationId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.deleteIntegrationInstanceConfiguration(
                developmentConfigurationId))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(jobFacade, principalJobService, triggerLifecycleFacade);

        assertThat(integrationInstanceConfigurationRepository.findAll()).hasSize(2);
        assertThat(integrationInstanceRepository.findAll()).hasSize(1);
        assertThat(integrationInstanceConfigurationRepository.findById(productionConfigurationId))
            .hasValueSatisfying(configuration -> assertThat(configuration.isEnabled()).isTrue());
    }

    @Test
    void testAnAdminInEveryEnvironmentDeletesAnIntegrationWithAllItsConfigurations() {
        authenticate(EMBEDDED_ADMIN, false);

        integrationFacade.deleteIntegration(integrationId);

        assertThat(integrationInstanceConfigurationRepository.findAll()).isEmpty();
        assertThat(integrationInstanceRepository.findAll()).isEmpty();
        assertThat(integrationRepository.findById(integrationId)).isEmpty();
    }

    @Test
    void testAListWithoutAnEnvironmentNeedsTheScopeEverywhere() {
        authenticate(EDITOR_IN_PRODUCTION, false);

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurations(
                null, null, null, false))
                    .isInstanceOf(AccessDeniedException.class);

        authenticate(EMBEDDED_VIEWER, false);

        assertThat(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurations(null, null, null, false))
                .extracting(IntegrationInstanceConfigurationDTO::id)
                .containsExactlyInAnyOrder(productionConfigurationId, developmentConfigurationId);
        assertThat(integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationTags()).isEmpty();

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
                productionConfigurationId, List.of()))
                    .isInstanceOf(AccessDeniedException.class);

        assertThat(appEventFacade.getAppEvents()).isEmpty();

        assertThatThrownBy(() -> appEventFacade.create(newAppEvent()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAConnectedUserStillTogglesTheirInstanceWorkflow() {
        authenticateConnectedUser();

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(integrationInstanceId, workflowId, true);
        integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            integrationInstanceId, workflowId, Map.of("key", "value"));

        assertThatThrownBy(() -> integrationInstanceFacade.deleteIntegrationInstance(integrationInstanceId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstance(integrationInstanceId, false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationInstanceFacade.getIntegrationInstance(integrationInstanceId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.enableIntegrationInstanceWorkflow(
                integrationInstanceId, workflowId, false))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appEventFacade.getAppEvents())
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testATenantAdminWithoutARowStillPasses() {
        authenticate(TENANT_ADMIN, true);

        assertThat(integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(
            developmentConfigurationId)).isNotNull();
        assertThat(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurations(null, null, null, false))
                .hasSize(2);
        assertThat(integrationInstanceFacade.getIntegrationInstance(integrationInstanceId)).isNotNull();

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
            developmentConfigurationId, List.of());
        integrationInstanceAdminFacade.enableIntegrationInstanceWorkflow(integrationInstanceId, workflowId, true);

        Integration integration = createPublishedIntegration();

        assertThat(
            integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                newIntegrationInstanceConfigurationDTO(integration.getId(), Environment.STAGING)))
                    .isPositive();

        AppEvent appEvent = appEventFacade.create(newAppEvent());

        assertThat(appEventFacade.getAppEvent(appEvent.getId())).isNotNull();

        appEventFacade.delete(appEvent.getId());

        assertThat(appEventRepository.findAll()).isEmpty();
    }

    @Test
    void testANonMemberIsDenied() {
        authenticate(NON_MEMBER, false);

        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO =
            newIntegrationInstanceConfigurationDTO(1L, Environment.PRODUCTION);
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(productionConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                integrationInstanceConfigurationDTO))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
                productionConfigurationId, List.of()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfigurationWorkflow))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(
                productionConfigurationId))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationTags())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurations(
                (long) Environment.PRODUCTION.ordinal(), null, null, false))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationInstanceFacade.getIntegrationInstance(integrationInstanceId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstance(integrationInstanceId, false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(
                integrationInstanceId, workflowId, true))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationInstanceFacade.deleteIntegrationInstance(integrationInstanceId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.enableIntegrationInstanceWorkflow(
                integrationInstanceId, workflowId, true))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appEventFacade.create(newAppEvent()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appEventFacade.getAppEvents())
            .isInstanceOf(AccessDeniedException.class);

        assertThat(integrationInstanceConfigurationRepository.findAll()).hasSize(2);
        assertThat(integrationInstanceRepository.findAll()).hasSize(1);
        assertThat(appEventRepository.findAll()).isEmpty();
    }

    private void authenticate(String login, boolean tenantAdmin) {
        String authority = tenantAdmin ? "ROLE_ADMIN" : "ROLE_USER";

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private void authenticateConnectedUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new EmbeddedApiKeyAuthenticationToken(
                    Environment.PRODUCTION.ordinal(), connectedUser.getId(),
                    new org.springframework.security.core.userdetails.User(externalUserId, "", List.of()), false));
    }

    private long createConfiguration(long integrationId, Environment environment, int integrationVersion) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(integrationVersion);
        integrationInstanceConfiguration.setName("configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        return integrationInstanceConfiguration.getId();
    }

    private Integration createPublishedIntegration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        integrationService.publishIntegration(integration.getId(), null);

        return integrationService.getIntegration(integration.getId());
    }

    private static AppEvent newAppEvent() {
        AppEvent appEvent = new AppEvent();

        appEvent.setName("event-" + UUID.randomUUID());
        appEvent.setSchema("{\"type\":\"object\"}");

        return appEvent;
    }

    private static IntegrationInstanceConfigurationDTO newIntegrationInstanceConfigurationDTO(
        long integrationId, Environment environment) {

        return IntegrationInstanceConfigurationDTO.builder()
            .authorizationType(AuthorizationType.API_KEY)
            .connectionParameters(Map.of("key", "admin-value"))
            .environment(environment)
            .integrationId(integrationId)
            .integrationInstanceConfigurationWorkflows(List.of())
            .integrationVersion(1)
            .name("instance-configuration-" + UUID.randomUUID())
            .tags(List.of())
            .build();
    }

    private static long userId(String login) {
        return USER_IDS_BY_LOGIN.get(login);
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }

    @TestConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ApiKeyPermissionScopeProvider.class, ApiPlatformPermissionScopeProvider.class,
        AppEventPermissionScopeProvider.class, ConnectedUserPermissionScopeProvider.class,
        ConnectionPermissionScopeProvider.class, CurrentUserResolver.class, CustomRoleScopeResolverImpl.class,
        DataTablePermissionScopeProvider.class, DeploymentPermissionScopeProvider.class,
        EmbeddedPermissionScopeProvider.class, EmbeddedScopeCacheService.class,
        EmbeddedSharedPermissionScopeProvider.class, ExecutionPermissionScopeProvider.class,
        IntegrationInstanceConfigurationPermissionScopeProvider.class, IntegrationPermissionScopeProvider.class,
        KnowledgeBasePermissionScopeProvider.class, McpPermissionScopeProvider.class, PermissionScopeRegistry.class,
        PermissionServiceImpl.class, ProjectPermissionScopeProvider.class, WorkflowPermissionScopeProvider.class,
        WorkspacePermissionScopeProvider.class, WorkspaceScopeCacheService.class
    })
    static class RealPermissionConfiguration {

        @Bean
        ResourceVisibilityResolver resourceVisibilityResolver() {
            return (resourceType, workspaceId, candidates) -> candidates.stream()
                .map(VisibilityRecord::id)
                .collect(Collectors.toSet());
        }

        @Bean
        UserService userService() {
            UserService userService = mock(UserService.class);

            for (Map.Entry<String, Long> entry : USER_IDS_BY_LOGIN.entrySet()) {
                User user = new User();

                user.setId(entry.getValue());
                user.setLogin(entry.getKey());

                when(userService.fetchUserByLogin(entry.getKey())).thenReturn(Optional.of(user));
                when(userService.getUser(entry.getKey())).thenReturn(user);
                when(userService.getUser(entry.getValue())).thenReturn(user);
            }

            return userService;
        }

        @Bean
        WorkflowFacade workflowFacade(
            ComponentConnectionFacade componentConnectionFacade,
            ComponentDefinitionService componentDefinitionService, WorkflowValidatorFacade workflowValidatorFacade,
            WorkflowService workflowService) {

            return new WorkflowFacadeImpl(
                componentConnectionFacade, componentDefinitionService, workflowValidatorFacade, workflowService);
        }

        @Bean
        WorkflowService workflowService(
            CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
            List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
        }
    }
}
