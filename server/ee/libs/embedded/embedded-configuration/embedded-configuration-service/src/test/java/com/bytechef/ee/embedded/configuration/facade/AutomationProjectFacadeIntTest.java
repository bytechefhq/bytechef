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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.repository.ProjectCodeWorkflowRepository;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowDraftService;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowDraftServiceImpl;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceRows;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectVersionWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacadeImpl;
import com.bytechef.ee.platform.codeworkflow.configuration.repository.CodeWorkflowContainerRepository;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerServiceImpl;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowInput;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.RecordApplicationEvents;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationProjectFacadeIntTestConfiguration.class,
        AutomationProjectFacadeIntTest.ConnectedUserServiceConfiguration.class
    },
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, TriggerDefinitionFacade.class,
    ComponentConnectionFacade.class,
    ComponentDefinitionService.class, ConnectionDefinitionService.class,
    ConnectionFacade.class, ConnectionLifecycleFacade.class, ConnectionService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class,
    GitHubProxyClient.class, JobFacade.class, JobService.class, McpComponentService.class,
    McpIntegrationComponentService.class,
    McpIntegrationInstanceConfigurationService.class, McpIntegrationInstanceConfigurationWorkflowService.class,
    McpIntegrationInstanceToolService.class,
    McpIntegrationInstanceWorkflowService.class, McpIntegrationInstanceRows.class, McpServerService.class,
    McpToolService.class,
    OAuth2ParametersFacade.class,
    OAuth2Service.class, PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class,
    ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class, TriggerDefinitionService.class,
    TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class,
    WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class AutomationProjectFacadeIntTest {

    private static final String TEST_EXTERNAL_USER_ID = "test-external-user-42";

    private final List<Long> createdIntegrationInstanceConfigurationIds = new ArrayList<>();
    private final List<Long> createdIntegrationIds = new ArrayList<>();

    @Autowired
    private AutomationProjectConfigurationResolver automationProjectConfigurationResolver;

    @Autowired
    private AutomationProjectConfigurationService automationProjectConfigurationService;

    @Autowired
    private AutomationProjectService automationProjectService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private AutomationProjectFacade automationProjectFacade;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private ProjectCodeWorkflowService projectCodeWorkflowService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private TagService tagService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private long connectedUserId;

    @BeforeEach
    void setUp() {
        ConnectedUser connectedUser = connectedUserService
            .fetchConnectedUser(TEST_EXTERNAL_USER_ID, Environment.PRODUCTION)
            .orElseGet(() -> connectedUserService.createConnectedUser(TEST_EXTERNAL_USER_ID, Environment.PRODUCTION));

        connectedUserId = connectedUser.getId();

        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        createdIntegrationInstanceConfigurationIds.forEach(integrationInstanceConfigurationService::delete);
        createdIntegrationIds.forEach(integrationService::delete);

        createdIntegrationInstanceConfigurationIds.clear();
        createdIntegrationIds.clear();
    }

    @Test
    void testPublishingACodeProjectWithAnEmptyDraftIsRefused() {
        long projectId = automationProjectFacade.createProject("CodeProject", "", null, List.of(), null);

        Project project = projectService.getProject(projectId);

        ProjectCodeWorkflow publishedProjectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(publishedProjectCodeWorkflow.getProjectVersion()).thenReturn(project.getLastProjectVersion() - 1);
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(projectId))
            .thenReturn(Optional.of(publishedProjectCodeWorkflow));

        try {
            assertThatThrownBy(() -> automationProjectFacade.publishProject(projectId))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(CodeWorkflowErrorType.CODE_WORKFLOW_DRAFT_EMPTY.getErrorKey()));

            assertThat(projectService.getProject(projectId)
                .getLastProjectVersion()).isEqualTo(project.getLastProjectVersion());
        } finally {
            reset(projectCodeWorkflowService);
        }
    }

    @Test
    void testPublishingACodeProjectWhoseCodeRowIsAtTheDraftVersionSucceeds() {
        long projectId = automationProjectFacade.createProject("CodeProject", "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(projectId, null, null);

        Project project = projectService.getProject(projectId);

        int draftVersion = project.getLastProjectVersion();

        assertThat(project.getLastPublishedProjectVersion()).isNull();

        ProjectCodeWorkflow draftProjectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(draftProjectCodeWorkflow.getProjectVersion()).thenReturn(draftVersion);
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(projectId))
            .thenReturn(Optional.of(draftProjectCodeWorkflow));

        try {
            automationProjectFacade.publishProject(projectId);

            Project publishedProject = projectService.getProject(projectId);

            assertThat(publishedProject.getLastPublishedProjectVersion()).isNotNull();
            assertThat(publishedProject.getLastPublishedProjectVersion()
                .getVersion()).isEqualTo(draftVersion);
            assertThat(publishedProject.getLastProjectVersion()).isEqualTo(draftVersion + 1);
        } finally {
            reset(projectCodeWorkflowService);
        }
    }

    @Test
    void testDeletingAWorkflowOfACodeProjectIsRefused() {
        long projectId = automationProjectFacade.createProject("CodeProject", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(projectId))
            .thenReturn(Optional.of(mock(ProjectCodeWorkflow.class)));

        try {
            assertThatThrownBy(() -> automationProjectFacade.deleteProjectWorkflow(workflowUuid))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(CodeWorkflowErrorType.CODE_PROJECT_WORKFLOW_NOT_DELETABLE.getErrorKey()));

            assertThat(automationProjectFacade.getProject(projectId)
                .workflowTemplates()).hasSize(1);
        } finally {
            reset(projectCodeWorkflowService);
        }
    }

    @Test
    void testDuplicatingACodeProjectIsRefusedBeforeAnyChange() {
        String projectName = "DuplicatedCodeProject" + UUID.randomUUID();

        long projectId = automationProjectFacade.createProject(projectName, "", null, List.of(), null);

        int projectCount = automationProjectFacade.getProjects()
            .size();

        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(projectId))
            .thenReturn(Optional.of(mock(ProjectCodeWorkflow.class)));

        try {
            assertThatThrownBy(() -> automationProjectFacade.duplicateProject(projectId))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(CodeWorkflowErrorType.CODE_PROJECT_NOT_DUPLICABLE.getErrorKey()));

            assertThat(automationProjectFacade.getProjects()).hasSize(projectCount);
        } finally {
            reset(projectCodeWorkflowService);
        }
    }

    @Test
    void testCopyWorkflowTemplate() {
        long projectId = automationProjectFacade.createProject("Onboarding", "", null, List.of(), null);
        automationProjectFacade.createProjectWorkflow(projectId, null, null);
        automationProjectFacade.publishProject(projectId);

        createEnabledConfiguration(projectId, 1);

        String publishedWorkflowUuid = automationProjectFacade.getPublishedProjects()
            .stream()
            .filter(project -> project.id() == projectId)
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .findFirst()
            .map(ConnectedUserWorkflowTemplateDTO::workflowUuid)
            .orElseThrow();

        String newWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            TEST_EXTERNAL_USER_ID, publishedWorkflowUuid, Environment.PRODUCTION);

        assertThat(newWorkflowUuid).isNotBlank()
            .isNotEqualTo(publishedWorkflowUuid);
    }

    @Test
    void testCopyWorkflowTemplateRecordsCopiedFromWorkflowUuid() {
        long projectId = automationProjectFacade.createProject("Onboarding", "", null, List.of(), null);
        automationProjectFacade.createProjectWorkflow(projectId, null, null);
        automationProjectFacade.publishProject(projectId);

        createEnabledConfiguration(projectId, 1);

        String publishedWorkflowUuid = automationProjectFacade.getPublishedProjects()
            .stream()
            .filter(project -> project.id() == projectId)
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .findFirst()
            .map(ConnectedUserWorkflowTemplateDTO::workflowUuid)
            .orElseThrow();

        String newWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            TEST_EXTERNAL_USER_ID, publishedWorkflowUuid, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow copy =
            connectedUserProjectWorkflowRepository.findAllByConnectedUserId(connectedUserId)
                .stream()
                .filter(row -> row.getAutomationUuid() == null)
                .filter(row -> {
                    ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
                        row.getProjectWorkflowId());

                    return projectWorkflow.getUuidAsString()
                        .equals(newWorkflowUuid);
                })
                .findFirst()
                .orElseThrow();

        assertThat(copy.getCopiedFromWorkflowUuid()).isEqualTo(publishedWorkflowUuid);
    }

    @Test
    void testCopyWorkflowTemplateUnknownIdThrows() {
        assertThatThrownBy(() -> connectedUserProjectFacade
            .copyWorkflowTemplate(TEST_EXTERNAL_USER_ID, "nope", Environment.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCopyWorkflowTemplateUnpublishedThrows() {
        long projectId = automationProjectFacade.createProject("Unpublished", "", null, List.of(), null);
        automationProjectFacade.createProjectWorkflow(projectId, null, null);

        String draftWorkflowUuid = automationProjectFacade.getProject(projectId)
            .workflowTemplates()
            .getFirst()
            .workflowUuid();

        assertThatThrownBy(
            () -> connectedUserProjectFacade
                .copyWorkflowTemplate(TEST_EXTERNAL_USER_ID, draftWorkflowUuid, Environment.PRODUCTION))
                    .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateAndGetProject() {
        long projectId = automationProjectFacade.createProject(
            "Onboarding", "Onboarding flows", null, List.of(), null);

        AutomationProjectDTO project = automationProjectFacade.getProject(projectId);

        assertThat(project.name()).isEqualTo("Onboarding");
        assertThat(project.description()).isEqualTo("Onboarding flows");
        assertThat(automationProjectFacade.getProjects()).extracting(AutomationProjectDTO::id)
            .contains(projectId);
    }

    @Test
    void testGetProjectVersionsListsNewestFirstWithTheirDescriptions() {
        long projectId = automationProjectFacade.createProject("Versioned", "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(projectId, null, null);
        automationProjectFacade.publishProject(projectId, "First release");
        automationProjectFacade.publishProject(projectId, "Second release");

        assertThat(automationProjectFacade.getProjectVersions(projectId))
            .extracting(
                AutomationProjectVersionDTO::version, AutomationProjectVersionDTO::status,
                AutomationProjectVersionDTO::description)
            .containsExactly(
                tuple(3, "DRAFT", null), tuple(2, "PUBLISHED", "Second release"),
                tuple(1, "PUBLISHED", "First release"));
    }

    @Test
    void testGetProjectVersionWorkflowChangesReportsAddedWorkflow() {
        long projectId = automationProjectFacade.createProject("Changes", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        automationProjectFacade.publishProject(projectId);

        VersionWorkflowChanges versionWorkflowChanges =
            automationProjectFacade.getProjectVersionWorkflowChanges(projectId, 1);

        assertThat(versionWorkflowChanges.baseVersion()).isNull();
        assertThat(versionWorkflowChanges.version()).isEqualTo(1);
        assertThat(versionWorkflowChanges.workflows()).hasSize(1);
        assertThat(versionWorkflowChanges.workflows()
            .getFirst()
            .workflowUuid()).isEqualTo(workflowUuid);
        assertThat(versionWorkflowChanges.workflows()
            .getFirst()
            .changeType()).isEqualTo(WorkflowChangeType.ADDED);

        VersionWorkflowChange versionWorkflowChange =
            automationProjectFacade.getProjectVersionWorkflowChange(projectId, 1, workflowUuid);

        assertThat(versionWorkflowChange.changeType()).isEqualTo(WorkflowChangeType.ADDED);
        assertThat(versionWorkflowChange.newDefinition()).isNotNull();
    }

    @Test
    void testGetProjectVersionWorkflowsListsWorkflowsUnchangedSincePreviousVersion() {
        long projectId = automationProjectFacade.createProject("Unchanged", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        automationProjectFacade.publishProject(projectId);
        automationProjectFacade.publishProject(projectId);

        assertThat(automationProjectFacade.getProjectVersionWorkflowChanges(projectId, 2)
            .workflows()).isEmpty();
        assertThat(automationProjectFacade.getProjectVersionWorkflows(projectId, 2))
            .extracting(AutomationProjectVersionWorkflowDTO::workflowUuid)
            .containsExactly(workflowUuid);
    }

    @Test
    void testGetProjectVersionWorkflowChangesRejectsNonAutomationProject() {
        Project project = new Project();

        project.setName("Plain " + UUID.randomUUID());
        project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);

        long projectId = projectService.create(project)
            .getId();

        assertThatThrownBy(() -> automationProjectFacade.getProjectVersionWorkflowChanges(projectId, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not an automation project");
        assertThatThrownBy(
            () -> automationProjectFacade.getProjectVersionWorkflowChange(projectId, 1, "uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not an automation project");
        assertThatThrownBy(() -> automationProjectFacade.getProjectVersionWorkflows(projectId, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not an automation project");
    }

    @Test
    void testCreateProjectWorkflow() {
        long projectId = automationProjectFacade.createProject("Onboarding", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        assertThat(workflowUuid).isNotBlank();
        assertThat(automationProjectFacade.getProject(projectId)
            .workflowTemplates())
                .extracting(ConnectedUserWorkflowTemplateDTO::workflowUuid)
                .contains(workflowUuid);
    }

    @Test
    void testUpdateProjectPersistsPermissionExpression() {
        long projectId = automationProjectFacade.createProject("Gated", "", null, List.of(), null);

        automationProjectFacade.updateProject(
            projectId, "Gated", "", null, List.of(), "metadata['tier'] == 'pro'");

        assertThat(automationProjectFacade.getProject(projectId)
            .permissionExpression()).isEqualTo("metadata['tier'] == 'pro'");
    }

    @Test
    void testCreateProjectWorkflowPersistsPermissionExpression() {
        long projectId = automationProjectFacade.createProject("Gated", "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(projectId, null, "metadata['tier'] == 'pro'");

        assertThat(automationProjectFacade.getProject(projectId)
            .workflowTemplates()
            .getFirst()
            .permissionExpression()).isEqualTo("metadata['tier'] == 'pro'");
    }

    @Test
    void testUpdateProjectWorkflowPermissionExpressionPersistsAndClears() {
        long projectId = automationProjectFacade.createProject("Gated", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        automationProjectFacade.updateProjectWorkflowPermissionExpression(
            workflowUuid, "metadata['tier'] == 'pro'");

        assertThat(automationProjectFacade.getProject(projectId)
            .workflowTemplates()
            .getFirst()
            .permissionExpression()).isEqualTo("metadata['tier'] == 'pro'");

        automationProjectFacade.updateProjectWorkflowPermissionExpression(workflowUuid, "");

        assertThat(automationProjectFacade.getProject(projectId)
            .workflowTemplates()
            .getFirst()
            .permissionExpression()).isNull();
    }

    @Test
    void testDeleteProject() {
        long projectId = automationProjectFacade.createProject("Temp", "", null, List.of(), null);

        automationProjectFacade.deleteProject(projectId);

        assertThat(automationProjectFacade.getProjects()).extracting(AutomationProjectDTO::id)
            .doesNotContain(projectId);
    }

    @Test
    void testPublishProject() {
        long publishedProjectId = automationProjectFacade.createProject(
            "PublishedAutomation", "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(publishedProjectId, null, null);
        automationProjectFacade.publishProject(publishedProjectId);

        long unpublishedProjectId = automationProjectFacade.createProject(
            "UnpublishedAutomation", "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(unpublishedProjectId, null, null);

        List<AutomationProjectDTO> publishedProjects = automationProjectFacade.getPublishedProjects();

        AutomationProjectDTO publishedProject = publishedProjects.stream()
            .filter(project -> project.id() == publishedProjectId)
            .findFirst()
            .orElseThrow();

        assertThat(publishedProject.workflowTemplates()).hasSize(1);

        AutomationProjectDTO unpublishedProject = publishedProjects.stream()
            .filter(project -> project.id() == unpublishedProjectId)
            .findFirst()
            .orElseThrow();

        assertThat(unpublishedProject.workflowTemplates()).isEmpty();
    }

    @Test
    void testPublishProjectDoesNotAccumulateWorkflowTemplatesInAdminList() {
        long projectId =
            automationProjectFacade.createProject("StableAutomation", "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(projectId, null, null);

        int workflowTemplateCountBeforePublish = automationProjectFacade.getProject(projectId)
            .workflowTemplates()
            .size();

        automationProjectFacade.publishProject(projectId);
        automationProjectFacade.publishProject(projectId);

        assertThat(automationProjectFacade.getProject(projectId)
            .workflowTemplates()).hasSize(workflowTemplateCountBeforePublish);
    }

    @Test
    void testPublishKeepsTheTemplateUuidAndMovesTheDraftToANewWorkflow() {
        long projectId = automationProjectFacade.createProject("StableUuid", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        ConnectedUserWorkflowTemplateDTO draftBeforePublish = getDraftTemplate(projectId);

        assertThat(draftBeforePublish.workflowUuid()).isEqualTo(workflowUuid);

        automationProjectFacade.publishProject(projectId);

        ConnectedUserWorkflowTemplateDTO draftAfterPublish = getDraftTemplate(projectId);

        assertThat(draftAfterPublish.workflowUuid()).isEqualTo(workflowUuid);
        assertThat(draftAfterPublish.workflowId()).isNotEqualTo(draftBeforePublish.workflowId());
        assertThat(getPublishedTemplate(projectId).workflowUuid()).isEqualTo(workflowUuid);
    }

    @Test
    void testRestoreProjectVersionReplacesTheDraftWithThePublishedWorkflows() {
        long projectId = automationProjectFacade.createProject("Restore", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(
            projectId, "{\"label\":\"Version 1\",\"tasks\":[]}", null);

        automationProjectFacade.publishProject(projectId);

        automationProjectFacade.updateProjectWorkflow(workflowUuid, "Version 2", "");

        String addedWorkflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        String draftWorkflowId = getDraftTemplate(projectId, workflowUuid).workflowId();
        String addedWorkflowId = getDraftTemplate(projectId, addedWorkflowUuid).workflowId();
        String publishedWorkflowId = getPublishedTemplate(projectId).workflowId();

        automationProjectFacade.restoreProjectVersion(projectId, 1);

        List<ConnectedUserWorkflowTemplateDTO> draftTemplates = automationProjectFacade.getProject(projectId)
            .workflowTemplates();

        assertThat(draftTemplates)
            .extracting(ConnectedUserWorkflowTemplateDTO::workflowUuid, ConnectedUserWorkflowTemplateDTO::label)
            .containsExactly(tuple(workflowUuid, "Version 1"));
        assertThat(draftTemplates.getFirst()
            .workflowId()).isNotIn(draftWorkflowId, publishedWorkflowId);

        assertThat(workflowService.fetchWorkflow(draftWorkflowId)).isEmpty();
        assertThat(workflowService.fetchWorkflow(addedWorkflowId)).isEmpty();

        ConnectedUserWorkflowTemplateDTO publishedTemplate = getPublishedTemplate(projectId);

        assertThat(publishedTemplate.workflowId()).isEqualTo(publishedWorkflowId);
        assertThat(publishedTemplate.label()).isEqualTo("Version 1");
    }

    @Test
    void testRestoreProjectVersionAddsWorkflowsMissingFromTheDraft() {
        long projectId = automationProjectFacade.createProject("RestoreDeleted", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        automationProjectFacade.publishProject(projectId);

        automationProjectFacade.deleteProjectWorkflow(workflowUuid);

        assertThat(automationProjectFacade.getProject(projectId)
            .workflowTemplates()).isEmpty();

        automationProjectFacade.restoreProjectVersion(projectId, 1);

        assertThat(automationProjectFacade.getProject(projectId)
            .workflowTemplates())
                .extracting(ConnectedUserWorkflowTemplateDTO::workflowUuid)
                .containsExactly(workflowUuid);
    }

    @Test
    void testRestoreProjectVersionRejectsAnUnpublishedVersion() {
        long projectId = automationProjectFacade.createProject("RestoreDraft", "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(projectId, null, null);

        assertThatThrownBy(() -> automationProjectFacade.restoreProjectVersion(projectId, 1))
            .isInstanceOf(ConfigurationException.class);
        assertThatThrownBy(() -> automationProjectFacade.restoreProjectVersion(projectId, 5))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testRestoreProjectVersionRejectsNonAutomationProject() {
        Project project = new Project();

        project.setName("Plain " + UUID.randomUUID());
        project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);

        long projectId = projectService.create(project)
            .getId();

        assertThatThrownBy(() -> automationProjectFacade.restoreProjectVersion(projectId, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not an automation project");
    }

    @Test
    void testUpdateProjectWorkflowAfterPublishEditsTheDraftNotThePublishedVersion() {
        long projectId = automationProjectFacade.createProject("DraftEdit", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(
            projectId, "{\"label\":\"Original\",\"tasks\":[]}", null);

        automationProjectFacade.publishProject(projectId);

        automationProjectFacade.updateProjectWorkflow(workflowUuid, "Renamed", "");

        assertThat(getDraftTemplate(projectId).label()).isEqualTo("Renamed");
        assertThat(getPublishedTemplate(projectId).label()).isEqualTo("Original");
    }

    @Test
    void testDeleteProjectWorkflowByTemplateUuid() {
        long projectId = automationProjectFacade.createProject("DeleteByUuid", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        automationProjectFacade.deleteProjectWorkflow(workflowUuid);

        assertThat(automationProjectFacade.getProject(projectId)
            .workflowTemplates()).isEmpty();
    }

    @Test
    void testDuplicateProjectWorkflowReturnsTheCopysTemplateUuid() {
        long projectId = automationProjectFacade.createProject("Duplicate", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        automationProjectFacade.publishProject(projectId);

        String duplicateWorkflowUuid = automationProjectFacade.duplicateProjectWorkflow(workflowUuid);

        assertThat(automationProjectFacade.getProject(projectId)
            .workflowTemplates()).extracting(ConnectedUserWorkflowTemplateDTO::workflowUuid)
                .containsExactlyInAnyOrder(workflowUuid, duplicateWorkflowUuid);
    }

    @Test
    void testDuplicateProjectWorkflowCopiesWorkflowTestConfigurations() {
        long projectId = automationProjectFacade.createProject(
            "DuplicateTestConfiguration", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        String duplicateWorkflowUuid = automationProjectFacade.duplicateProjectWorkflow(workflowUuid);

        verify(workflowTestConfigurationService).copyWorkflowTestConfigurations(
            projectWorkflowService.getLastWorkflowId(workflowUuid),
            projectWorkflowService.getLastWorkflowId(duplicateWorkflowUuid));
    }

    @Test
    void testWorkflowComponentsResolvedFromTaskDefinition() {
        String workflowDefinitionWithTask = """
            {
                "label": "Email Workflow",
                "description": "Sends emails",
                "inputs": [],
                "triggers": [],
                "tasks": [
                    {
                        "label": "Send Email",
                        "name": "sendEmail",
                        "type": "gmail/v1/send",
                        "parameters": {}
                    }
                ]
            }
            """;

        ComponentDefinition gmailDefinition = new ComponentDefinition("gmail");

        when(componentDefinitionService.fetchComponentDefinition(anyString(), any()))
            .thenReturn(Optional.of(gmailDefinition));

        long projectId =
            automationProjectFacade.createProject("EmailAutomation", "", null, List.of(), null);
        automationProjectFacade.createProjectWorkflow(projectId, workflowDefinitionWithTask, null);

        AutomationProjectDTO project = automationProjectFacade.getProject(projectId);

        assertThat(project.workflowTemplates()).hasSize(1);

        ConnectedUserWorkflowTemplateDTO workflowTemplate = project.workflowTemplates()
            .getFirst();

        List<ConnectedUserWorkflowTemplateDTO.Component> components = workflowTemplate.components();

        assertThat(components).isNotNull();
        assertThat(components).hasSize(1);

        ConnectedUserWorkflowTemplateDTO.Component component = components.getFirst();

        assertThat(component.name()).isEqualTo("gmail");
    }

    @Test
    void testWorkflowComponentsNonNullForEmptyWorkflow() {
        long projectId =
            automationProjectFacade.createProject("EmptyWorkflowAutomation", "", null, List.of(), null);
        automationProjectFacade.createProjectWorkflow(projectId, null, null);

        AutomationProjectDTO project = automationProjectFacade.getProject(projectId);

        List<ConnectedUserWorkflowTemplateDTO> connectedUserWorkflowTemplateDTOS = project.workflowTemplates();
        assertThat(connectedUserWorkflowTemplateDTOS).hasSize(1);

        ConnectedUserWorkflowTemplateDTO connectedUserWorkflowTemplateDTO = connectedUserWorkflowTemplateDTOS
            .getFirst();

        assertThat(connectedUserWorkflowTemplateDTO.components()).isNotNull();
    }

    @Test
    void testWorkflowComponentsIncludeTaskDispatchers() {
        String workflowDefinitionWithTaskDispatcher = """
            {
                "label": "Branching Workflow",
                "description": "Branches",
                "inputs": [],
                "triggers": [],
                "tasks": [
                    {
                        "label": "Check",
                        "name": "condition_1",
                        "type": "condition/v1",
                        "parameters": {}
                    }
                ]
            }
            """;

        long projectId =
            automationProjectFacade.createProject("BranchAutomation", "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(projectId, workflowDefinitionWithTaskDispatcher, null);

        AutomationProjectDTO project = automationProjectFacade.getProject(projectId);

        ConnectedUserWorkflowTemplateDTO workflowTemplate = project.workflowTemplates()
            .getFirst();

        assertThat(workflowTemplate.components())
            .extracting(ConnectedUserWorkflowTemplateDTO.Component::name)
            .containsExactly("condition");
    }

    @Test
    void testWorkflowTriggersFallBackToManual() {
        String workflowDefinitionWithoutTrigger = """
            {
                "label": "Manual Workflow",
                "description": "Runs manually",
                "inputs": [],
                "triggers": [],
                "tasks": []
            }
            """;

        long projectId =
            automationProjectFacade.createProject("ManualAutomation", "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(projectId, workflowDefinitionWithoutTrigger, null);

        AutomationProjectDTO project = automationProjectFacade.getProject(projectId);

        ConnectedUserWorkflowTemplateDTO workflowTemplate = project.workflowTemplates()
            .getFirst();

        assertThat(workflowTemplate.triggers())
            .extracting(ConnectedUserWorkflowTemplateDTO.Component::name)
            .containsExactly("manual");
    }

    @Test
    void testCreateProjectCreatesNewCategoryAndTags() {
        long projectId = automationProjectFacade.createProject(
            "AutomationWithNewCategoryAndTags", "", "Automation", List.of("crm", "erp"), null);

        AutomationProjectDTO project = automationProjectFacade.getProject(projectId);

        assertThat(project.categoryId()).isNotNull();
        assertThat(project.tagIds()).hasSize(2);

        assertThat(categoryService.getCategories())
            .extracting(Category::getName)
            .contains("Automation");

        assertThat(tagService.getTags())
            .extracting(Tag::getName)
            .contains("crm", "erp");
    }

    @Test
    void testCreateProjectReusesExistingCategoryByName() {
        long firstProjectId = automationProjectFacade.createProject(
            "FirstAutomationProject", "", "Reusable", List.of(), null);
        long secondProjectId = automationProjectFacade.createProject(
            "SecondAutomationProject", "", "Reusable", List.of(), null);

        AutomationProjectDTO firstProject = automationProjectFacade.getProject(firstProjectId);
        AutomationProjectDTO secondProject = automationProjectFacade.getProject(secondProjectId);

        assertThat(firstProject.categoryId()).isNotNull();
        assertThat(secondProject.categoryId()).isNotNull();
        assertThat(firstProject.categoryId()).isEqualTo(secondProject.categoryId());

        long reusableCategoryCount = categoryService.getCategories()
            .stream()
            .filter(category -> "Reusable".equals(category.getName()))
            .count();

        assertThat(reusableCategoryCount).isEqualTo(1);
    }

    @Test
    void testUpdateProjectKeepsThePermissionExpressionWhenNullAndClearsItWhenBlank() {
        long projectId = automationProjectFacade.createProject(
            "Gated", "", null, List.of(), "metadata['tier'] == 'pro'");

        automationProjectFacade.updateProject(projectId, "Gated", "", null, List.of(), null);

        assertThat(automationProjectFacade.getProject(projectId)
            .permissionExpression()).isEqualTo("metadata['tier'] == 'pro'");

        automationProjectFacade.updateProject(projectId, "Gated", "", null, List.of(), " ");

        assertThat(automationProjectFacade.getProject(projectId)
            .permissionExpression()).isNull();
    }

    @Test
    void testTemplatePermissionExpressionAppliesToThePublishedVersion() {
        long projectId = automationProjectFacade.createProject(
            "Published Gated " + UUID.randomUUID(), "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(
            projectId, null, "metadata['tier'] == 'pro'");

        automationProjectFacade.publishProject(projectId);

        assertThat(getPublishedTemplate(projectId, workflowUuid)
            .permissionExpression()).isEqualTo("metadata['tier'] == 'pro'");

        automationProjectFacade.updateProjectWorkflowPermissionExpression(
            workflowUuid, "metadata['tier'] == 'gold'");

        assertThat(getPublishedTemplate(projectId, workflowUuid)
            .permissionExpression()).isEqualTo("metadata['tier'] == 'gold'");
    }

    @Test
    void testDeleteProjectRemovesItsAutomationProjectSettings() {
        long projectId = automationProjectFacade.createProject(
            "Deleted Gated " + UUID.randomUUID(), "", null, List.of(), "metadata['tier'] == 'pro'");

        automationProjectFacade.createProjectWorkflow(projectId, null, "metadata['tier'] == 'pro'");

        assertThat(automationProjectService.fetchAutomationProject(projectId)).isPresent();
        assertThat(automationProjectService.getWorkflowPermissionExpressions(projectId)).hasSize(1);

        automationProjectFacade.deleteProject(projectId);

        assertThat(automationProjectService.fetchAutomationProject(projectId)).isEmpty();
        assertThat(automationProjectService.getWorkflowPermissionExpressions(projectId)).isEmpty();
    }

    @Test
    void testDuplicateProjectCopiesItsAutomationProjectSettings() {
        long projectId = automationProjectFacade.createProject(
            "Duplicated Gated " + UUID.randomUUID(), "", null, List.of(), "metadata['tier'] == 'pro'");

        long duplicateProjectId = automationProjectFacade.duplicateProject(projectId);

        AutomationProjectDTO duplicateProject = automationProjectFacade.getProject(duplicateProjectId);

        assertThat(duplicateProject.permissionExpression()).isEqualTo("metadata['tier'] == 'pro'");
    }

    @Test
    void testGetProjectsSortsByNameIgnoringCase() {
        long zetaProjectId = automationProjectFacade.createProject("Zeta", "", null, List.of(), null);
        long betaProjectId = automationProjectFacade.createProject("Beta", "", null, List.of(), null);
        long alphaProjectId = automationProjectFacade.createProject("alpha", "", null, List.of(), null);

        List<Long> projectIds = List.of(zetaProjectId, betaProjectId, alphaProjectId);

        assertThat(
            automationProjectFacade.getProjects()
                .stream()
                .filter(project -> projectIds.contains(project.id()))
                .map(AutomationProjectDTO::name)
                .toList()).containsExactly("alpha", "Beta", "Zeta");
    }

    @Test
    void testDuplicateProjectOfAPlainNamedProject() {
        long projectId = automationProjectFacade.createProject(
            "Xylophone", "", null, List.of(), null);

        long duplicateProjectId = automationProjectFacade.duplicateProject(projectId);

        assertThat(projectService.getProject(duplicateProjectId)
            .getName()).isEqualTo("Xylophone (Copy)");
        assertThat(automationProjectFacade.getProject(duplicateProjectId)
            .name()).isEqualTo("Xylophone (Copy)");
    }

    @Test
    void testProjectWithTheMarkerNameButNoSettingsRowIsNotAnAutomationProject() {
        Project project = new Project();

        project.setName("__EMBEDDED_AUTOMATION__Unregistered " + UUID.randomUUID());
        project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);

        long projectId = projectService.create(project)
            .getId();

        assertThatThrownBy(() -> automationProjectFacade.getProject(projectId))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(automationProjectFacade.getProjects())
            .extracting(AutomationProjectDTO::id)
            .doesNotContain(projectId);
    }

    @Test
    void testGetPublishedProjectsSkipsProjectsWithoutAnEnabledConfiguration() {
        long projectId = createPublishedProject("No Configuration");

        assertThat(getEnvironmentProjectIds()).doesNotContain(projectId);

        AutomationProjectConfiguration configuration = createConfiguration(projectId, 1, false, true);

        assertThat(getEnvironmentProjectIds()).doesNotContain(projectId);

        automationProjectConfigurationService.updateEnabled(configuration.getId(), true);

        assertThat(getEnvironmentProjectIds()).contains(projectId);
    }

    @Test
    void testGetPublishedProjectsUsesThePinnedVersion() {
        long projectId = createPublishedProject("Pinned");

        String firstWorkflowUuid = getPublishedTemplate(projectId).workflowUuid();

        String secondWorkflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        automationProjectFacade.publishProject(projectId);

        createConfiguration(projectId, 1, true, true);

        assertThat(getEnvironmentTemplateUuids(projectId)).containsExactly(firstWorkflowUuid);
        assertThat(getPublishedTemplateUuids(projectId)).containsExactlyInAnyOrder(
            firstWorkflowUuid, secondWorkflowUuid);

        AutomationProjectDTO environmentProject = getEnvironmentProject(projectId);

        assertThat(environmentProject.published()).isTrue();
        assertThat(environmentProject.lastPublishedVersion()).isEqualTo(1);
    }

    @Test
    void testGetPublishedProjectsDropsDisabledWorkflows() {
        long projectId = createPublishedProject("Disabled Workflow");

        String firstWorkflowUuid = getPublishedTemplate(projectId).workflowUuid();

        String secondWorkflowUuid = automationProjectFacade.createProjectWorkflow(projectId, null, null);

        automationProjectFacade.publishProject(projectId);

        AutomationProjectConfiguration configuration = createConfiguration(projectId, 2, true, true);

        assertThat(getEnvironmentTemplateUuids(projectId)).containsExactlyInAnyOrder(
            firstWorkflowUuid, secondWorkflowUuid);

        automationProjectConfigurationService.updateWorkflowEnabled(
            configuration.getId(), UUID.fromString(secondWorkflowUuid), false);

        assertThat(getEnvironmentTemplateUuids(projectId)).containsExactly(firstWorkflowUuid);
    }

    @Test
    void testGetPublishedProjectsCarriesTheConfigurationsHubVisibility() {
        long visibleProjectId = createPublishedProject("Hub Visible");
        long hiddenProjectId = createPublishedProject("Hub Hidden");

        createConfiguration(visibleProjectId, 1, true, true);
        createConfiguration(hiddenProjectId, 1, true, false);

        assertThat(getEnvironmentProject(visibleProjectId).automationHubVisible()).isTrue();
        assertThat(getEnvironmentProject(hiddenProjectId).automationHubVisible()).isFalse();
    }

    @Test
    void testHubHidesProjectWhenNoConfigurationExpressionPasses() {
        useRealPermissionEvaluator();

        long projectId = createPublishedProject("Beta Only");

        automationProjectConfigurationService.create(
            projectId, Environment.PRODUCTION.ordinal(), "Beta", "metadata['beta'] == 'true'", 1, true, true);

        assertThat(getEnvironmentProjectIds()).doesNotContain(projectId);
    }

    @Test
    void testHubUsesAssignedConfigurationVisibility() {
        useRealPermissionEvaluator();

        long projectId = createPublishedProject("Assigned Visibility");

        automationProjectFacade.publishProject(projectId);

        automationProjectConfigurationService.create(
            projectId, Environment.PRODUCTION.ordinal(), "A", null, 2, true, true);

        AutomationProjectConfiguration hiddenConfiguration = automationProjectConfigurationService
            .create(projectId, Environment.PRODUCTION.ordinal(), "B", null, 1, true, false);

        automationProjectConfigurationResolver.assign(
            connectedUserService.getConnectedUser(connectedUserId), projectId, hiddenConfiguration.getId());

        AutomationProjectDTO environmentProject = getEnvironmentProject(projectId);

        assertThat(environmentProject.automationHubVisible()).isFalse();
        assertThat(environmentProject.lastPublishedVersion()).isEqualTo(1);
    }

    @Test
    void testCopyWorkflowTemplateNotReleasedThrows() {
        long projectId = createPublishedProject("Not Released");

        String workflowUuid = getPublishedTemplate(projectId).workflowUuid();

        assertThatThrownBy(
            () -> connectedUserProjectFacade.copyWorkflowTemplate(
                TEST_EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getErrorKey())
                            .isEqualTo(ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED
                                .getErrorKey()));

        AutomationProjectConfiguration configuration = createConfiguration(projectId, 1, true, true);

        automationProjectConfigurationService.updateWorkflowEnabled(
            configuration.getId(), UUID.fromString(workflowUuid), false);

        assertThatThrownBy(
            () -> connectedUserProjectFacade.copyWorkflowTemplate(
                TEST_EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getErrorKey())
                            .isEqualTo(ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED
                                .getErrorKey()));
    }

    @Test
    void testCopyWorkflowTemplateCopiesThePinnedVersion() {
        long projectId = automationProjectFacade.createProject("Pinned Copy", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(
            projectId, "{\"label\":\"Original\",\"tasks\":[]}", null);

        automationProjectFacade.publishProject(projectId);

        automationProjectFacade.updateProjectWorkflow(workflowUuid, "Renamed", "");

        automationProjectFacade.publishProject(projectId);

        createConfiguration(projectId, 1, true, true);

        String copiedWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            TEST_EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        String copiedWorkflowId = projectWorkflowService.getLastWorkflowId(copiedWorkflowUuid);

        assertThat(workflowService.getWorkflow(copiedWorkflowId)
            .getLabel()).isEqualTo("Original");
        assertThat(getPublishedTemplate(projectId, workflowUuid).label()).isEqualTo("Renamed");
    }

    private void useRealPermissionEvaluator() {
        EmbeddedPermissionEvaluator realEmbeddedPermissionEvaluator = new EmbeddedPermissionEvaluator(
            SpelEvaluator.create());

        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenAnswer(invocation -> realEmbeddedPermissionEvaluator.evaluate(
                invocation.getArgument(0), invocation.getArgument(1)));
    }

    private AutomationProjectConfiguration createConfiguration(
        long projectId, int projectVersion, boolean enabled, boolean automationHubVisible) {

        return automationProjectConfigurationService.create(projectId, Environment.PRODUCTION.ordinal(),
            "Default", null, projectVersion, enabled, automationHubVisible);
    }

    private void createEnabledConfiguration(long projectId, int projectVersion) {
        createConfiguration(projectId, projectVersion, true, true);
    }

    private long createPublishedProject(String name) {
        long projectId = automationProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(projectId, null, null);
        automationProjectFacade.publishProject(projectId);

        return projectId;
    }

    private AutomationProjectDTO getEnvironmentProject(long projectId) {
        return automationProjectFacade.getPublishedProjects(TEST_EXTERNAL_USER_ID, Environment.PRODUCTION)
            .stream()
            .filter(project -> project.id() == projectId)
            .findFirst()
            .orElseThrow();
    }

    private List<Long> getEnvironmentProjectIds() {
        return automationProjectFacade.getPublishedProjects(TEST_EXTERNAL_USER_ID, Environment.PRODUCTION)
            .stream()
            .map(AutomationProjectDTO::id)
            .toList();
    }

    private List<String> getEnvironmentTemplateUuids(long projectId) {
        return getEnvironmentProject(projectId).workflowTemplates()
            .stream()
            .map(ConnectedUserWorkflowTemplateDTO::workflowUuid)
            .toList();
    }

    private List<String> getPublishedTemplateUuids(long projectId) {
        return automationProjectFacade.getPublishedProjects()
            .stream()
            .filter(project -> project.id() == projectId)
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .map(ConnectedUserWorkflowTemplateDTO::workflowUuid)
            .toList();
    }

    private ConnectedUserWorkflowTemplateDTO getPublishedTemplate(long projectId, String workflowUuid) {
        return automationProjectFacade.getPublishedProjects()
            .stream()
            .filter(project -> project.id() == projectId)
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .filter(template -> workflowUuid.equals(template.workflowUuid()))
            .findFirst()
            .orElseThrow();
    }

    private ConnectedUserWorkflowTemplateDTO getDraftTemplate(long projectId) {
        return automationProjectFacade.getProject(projectId)
            .workflowTemplates()
            .getFirst();
    }

    private ConnectedUserWorkflowTemplateDTO getDraftTemplate(long projectId, String workflowUuid) {
        return automationProjectFacade.getProject(projectId)
            .workflowTemplates()
            .stream()
            .filter(template -> workflowUuid.equals(template.workflowUuid()))
            .findFirst()
            .orElseThrow();
    }

    private ConnectedUserWorkflowTemplateDTO getPublishedTemplate(long projectId) {
        return automationProjectFacade.getPublishedProjects()
            .stream()
            .filter(project -> project.id() == projectId)
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .findFirst()
            .orElseThrow();
    }

    @Test
    void testPublishedTemplateComponentCarriesTheIntegrationEnabledInTheEnvironment() {
        stubSlackComponentDefinition();

        long integrationId = createIntegrationWithConfiguration("slack", Environment.PRODUCTION, true);

        String workflowUuid = createPublishedSlackTemplate();

        ConnectedUserWorkflowTemplateDTO.Component slack = getPublishedSlackComponent(workflowUuid);

        assertThat(slack.connectionRequired()).isTrue();
        assertThat(slack.integrationId()).isEqualTo(integrationId);
    }

    @Test
    void testPublishedTemplateComponentHasNoIntegrationWhenTheConfigurationIsElsewhereOrDisabled() {
        stubSlackComponentDefinition();

        createIntegrationWithConfiguration("slack", Environment.STAGING, true);
        createIntegrationWithConfiguration("slack", Environment.PRODUCTION, false);

        String workflowUuid = createPublishedSlackTemplate();

        ConnectedUserWorkflowTemplateDTO.Component slack = getPublishedSlackComponent(workflowUuid);

        assertThat(slack.connectionRequired()).isTrue();
        assertThat(slack.integrationId()).isNull();
    }

    @Test
    void testTheLowestIdIntegrationWinsWhenTwoAreEnabledForOneComponent() {
        stubSlackComponentDefinition();

        long firstIntegrationId = createIntegrationWithConfiguration("slack", Environment.PRODUCTION, true);

        createIntegrationWithConfiguration("slack", Environment.PRODUCTION, true);

        String workflowUuid = createPublishedSlackTemplate();

        assertThat(getPublishedSlackComponent(workflowUuid).integrationId()).isEqualTo(firstIntegrationId);
    }

    @Test
    void testAnIntegrationHiddenFromTheConnectedUserIsSkippedForTheComponent() {
        stubSlackComponentDefinition();

        when(embeddedPermissionEvaluator.evaluate(eq("hidden-from-connected-user"), any()))
            .thenReturn(false);

        long hiddenIntegrationId = createIntegrationWithConfiguration(
            "slack", Environment.PRODUCTION, true, "hidden-from-connected-user");
        long visibleIntegrationId = createIntegrationWithConfiguration("slack", Environment.PRODUCTION, true);

        assertThat(hiddenIntegrationId).isLessThan(visibleIntegrationId);

        String workflowUuid = createPublishedSlackTemplate();

        assertThat(getPublishedSlackComponent(workflowUuid).integrationId()).isEqualTo(visibleIntegrationId);
    }

    @Test
    void testPublishedTemplateTriggerCarriesTheIntegrationEnabledInTheEnvironment() {
        when(componentDefinitionService.fetchComponentDefinition("hubspot", 1))
            .thenReturn(Optional.of(new ComponentDefinition(
                ComponentDsl.component("hubspot")
                    .title("HubSpot")
                    .connection(ComponentDsl.connection()))));

        long integrationId = createIntegrationWithConfiguration("hubspot", Environment.PRODUCTION, true);

        long projectId = automationProjectFacade.createProject(
            "HubSpot contacts", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(
            projectId,
            "{\"label\":\"HubSpot contacts\",\"triggers\":[{\"name\":\"trigger_1\"," +
                "\"type\":\"hubspot/v1/newContact\",\"parameters\":{}}],\"tasks\":[]}",
            null);

        automationProjectFacade.publishProject(projectId);

        createEnabledConfiguration(projectId, 1);

        ConnectedUserWorkflowTemplateDTO.Component hubspotTrigger = automationProjectFacade
            .getPublishedProjects(TEST_EXTERNAL_USER_ID, Environment.PRODUCTION)
            .stream()
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .filter(template -> workflowUuid.equals(template.workflowUuid()))
            .flatMap(template -> template.triggers()
                .stream())
            .filter(trigger -> "hubspot".equals(trigger.name()))
            .findFirst()
            .orElseThrow();

        assertThat(hubspotTrigger.connectionRequired()).isTrue();
        assertThat(hubspotTrigger.integrationId()).isEqualTo(integrationId);
    }

    @Test
    void testPublishedTemplateInputCarriesItsComponentInputReference() {
        long projectId = automationProjectFacade.createProject(
            "Slack channel digest", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(
            projectId,
            """
                {
                    "label": "Slack channel digest",
                    "inputs": [
                        {
                            "name": "channel",
                            "label": "Channel",
                            "type": "string",
                            "required": true,
                            "componentName": "slack",
                            "componentVersion": 1,
                            "groupName": "channel"
                        },
                        {"name": "greeting", "label": "Greeting", "type": "string"}
                    ],
                    "tasks": []
                }
                """,
            null);

        automationProjectFacade.publishProject(projectId);

        List<ConnectedUserWorkflowTemplateDTO.Input> inputs = getPublishedTemplate(projectId, workflowUuid).inputs();

        assertThat(inputs)
            .extracting(ConnectedUserWorkflowTemplateDTO.Input::componentReference)
            .containsExactly(new WorkflowInput.ComponentInputReference("slack", 1, "channel"), null);
    }

    private void stubSlackComponentDefinition() {
        when(componentDefinitionService.fetchComponentDefinition("slack", 1))
            .thenReturn(Optional.of(new ComponentDefinition(
                ComponentDsl.component("slack")
                    .title("Slack")
                    .connection(ComponentDsl.connection()))));
    }

    private long createIntegrationWithConfiguration(String componentName, Environment environment, boolean enabled) {
        return createIntegrationWithConfiguration(componentName, environment, enabled, null);
    }

    private long createIntegrationWithConfiguration(
        String componentName, Environment environment, boolean enabled, String permissionExpression) {

        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setName(componentName + "-" + UUID.randomUUID());
        integration.setPermissionExpression(permissionExpression);

        integration = integrationService.create(integration);

        createdIntegrationIds.add(integration.getId());

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnabled(enabled);
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName(integration.getName());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        createdIntegrationInstanceConfigurationIds.add(integrationInstanceConfiguration.getId());

        if (enabled) {
            integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfiguration.getId(), true);
        }

        return integration.getId();
    }

    private String createPublishedSlackTemplate() {
        long projectId = automationProjectFacade.createProject("Slack digest", "", null, List.of(), null);

        String workflowUuid = automationProjectFacade.createProjectWorkflow(
            projectId,
            "{\"label\":\"Slack digest\",\"tasks\":[{\"name\":\"post\",\"type\":\"slack/v1/sendMessage\",\"parameters\":{}}]}",
            null);

        automationProjectFacade.publishProject(projectId);

        createEnabledConfiguration(projectId, 1);

        return workflowUuid;
    }

    private ConnectedUserWorkflowTemplateDTO.Component getPublishedSlackComponent(String workflowUuid) {
        return automationProjectFacade.getPublishedProjects(TEST_EXTERNAL_USER_ID, Environment.PRODUCTION)
            .stream()
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .filter(template -> workflowUuid.equals(template.workflowUuid()))
            .flatMap(template -> template.components()
                .stream())
            .filter(component -> "slack".equals(component.name()))
            .findFirst()
            .orElseThrow();
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }

    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @SpringBootTest(
        classes = CodeProjectLifecycle.CodeProjectLifecycleConfiguration.class,
        properties = {
            "bytechef.edition=EE",
            "bytechef.workflow.repository.jdbc.enabled=true",
            "bytechef.webhook-url=/webhooks/{id}",
            "spring.liquibase.contexts=configuration,user",
            "spring.main.allow-bean-definition-overriding=true"
        })
    @Import(PostgreSQLContainerConfiguration.class)
    @RecordApplicationEvents
    @MockitoBean(types = {
        ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
        ClusterElementDefinitionService.class, TriggerDefinitionFacade.class,
        ComponentConnectionFacade.class,
        ComponentDefinitionService.class, ConnectedUserService.class, ConnectionDefinitionService.class,
        ConnectionFacade.class, ConnectionLifecycleFacade.class, ConnectionService.class,
        EmbeddedPermissionEvaluator.class, EnvironmentService.class,
        GitHubProxyClient.class, JobFacade.class, JobService.class, McpComponentService.class,
        McpIntegrationComponentService.class,
        McpIntegrationInstanceConfigurationService.class, McpIntegrationInstanceConfigurationWorkflowService.class,
        McpIntegrationInstanceToolService.class, McpServerService.class, McpToolService.class,
        OAuth2ParametersFacade.class,
        OAuth2Service.class, PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class,
        ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectFacade.class,
        TaskDispatcherDefinitionService.class, TaskExecutionService.class, TriggerDefinitionService.class,
        TriggerExecutionService.class,
        TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class,
        WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class,
        WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
        WorkspaceConnectionFacade.class, WorkspaceFacade.class
    })
    class CodeProjectLifecycle {

        @Autowired
        private AutomationProjectCodeWorkflowFacade automationProjectCodeWorkflowFacade;

        @Autowired
        private AutomationProjectFacade automationProjectFacade;

        @Autowired
        private CodeWorkflowContainerService codeWorkflowContainerService;

        @Autowired
        private CodeWorkflowFileStorage codeWorkflowFileStorage;

        @Autowired
        private ProjectCodeWorkflowService projectCodeWorkflowService;

        @Autowired
        private ProjectService projectService;

        @Autowired
        private ProjectWorkflowService projectWorkflowService;

        @BeforeEach
        void setUpAdminAuthentication() {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "admin", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
        }

        @AfterEach
        void clearAuthentication() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testEveryWorkflowKeepsItsUuidAcrossCreateEditAndTwoPublishes() {
            String projectName = "uuid-continuity-create-edit";

            long projectId = automationProjectCodeWorkflowFacade.createEmptyCodeWorkflow(
                projectName, Language.JAVASCRIPT, null, null, null, null);

            automationProjectCodeWorkflowFacade.updateCodeWorkflowSource(
                projectId, jsSource(projectName, "first", "alpha", "beta"));

            Map<String, UUID> draftUuids = getLatestWorkflowUuidsByName(projectId);

            assertThat(draftUuids).containsOnlyKeys("alpha", "beta");

            automationProjectFacade.publishProject(projectId);

            Map<String, UUID> firstPublishedUuids = getLatestWorkflowUuidsByName(projectId);

            assertThat(firstPublishedUuids).isEqualTo(draftUuids);

            automationProjectCodeWorkflowFacade.updateCodeWorkflowSource(
                projectId, jsSource(projectName, "second", "alpha", "beta"));

            assertThat(getLatestWorkflowUuidsByName(projectId)).isEqualTo(firstPublishedUuids);

            automationProjectFacade.publishProject(projectId);

            Map<String, UUID> secondPublishedUuids = getLatestWorkflowUuidsByName(projectId);

            assertThat(secondPublishedUuids).isEqualTo(firstPublishedUuids);

            assertThat(getPublishedTemplateUuids(projectId)).containsExactlyInAnyOrderElementsOf(
                firstPublishedUuids.values()
                    .stream()
                    .map(UUID::toString)
                    .toList());
            assertThat(getPublishedSource(projectId)).isEqualTo(jsSource(projectName, "second", "alpha", "beta"));
        }

        @Test
        void testUploadMatchingAPublishedProjectWithAnUnpublishedEditPublishesAndKeepsUuids() {
            String projectName = "uuid-continuity-upload-over-edit";

            automationProjectCodeWorkflowFacade.save(
                jsSource(projectName, "uploaded", "alpha", "beta")
                    .getBytes(StandardCharsets.UTF_8),
                Language.JAVASCRIPT, Environment.PRODUCTION, false);

            long projectId = automationProjectFacade.fetchProjectIdByName(projectName)
                .orElseThrow();

            Map<String, UUID> publishedUuids = getLatestWorkflowUuidsByName(projectId);

            assertThat(publishedUuids).containsOnlyKeys("alpha", "beta");

            automationProjectCodeWorkflowFacade.updateCodeWorkflowSource(
                projectId, jsSource(projectName, "unpublished edit", "alpha", "beta"));

            String upload = jsSource(projectName, "second upload", "alpha", "beta");

            automationProjectCodeWorkflowFacade.save(
                upload.getBytes(StandardCharsets.UTF_8), Language.JAVASCRIPT, Environment.PRODUCTION, false);

            assertThat(getLatestWorkflowUuidsByName(projectId)).isEqualTo(publishedUuids);
            assertThat(getPublishedSource(projectId)).isEqualTo(upload);
            assertThat(
                projectService.getProject(projectId)
                    .getLastProjectVersion()).isEqualTo(
                        projectCodeWorkflowService.getProjectCodeWorkflow(projectId)
                            .getProjectVersion() + 1);
        }

        @Test
        void testFirstEditOfAProjectUploadedBeforeDraftSupportRestoresUuidsByName() {
            String projectName = "uuid-continuity-upload-then-edit";

            automationProjectCodeWorkflowFacade.save(
                jsSource(projectName, "uploaded", "alpha", "beta")
                    .getBytes(StandardCharsets.UTF_8),
                Language.JAVASCRIPT, Environment.PRODUCTION, false);

            long projectId = automationProjectFacade.fetchProjectIdByName(projectName)
                .orElseThrow();

            Map<String, UUID> uploadedUuids = getLatestWorkflowUuidsByName(projectId);

            assertThat(uploadedUuids).containsOnlyKeys("alpha", "beta");

            int draftVersion = projectService.getProject(projectId)
                .getLastProjectVersion();

            for (ProjectWorkflow draftProjectWorkflow : projectWorkflowService.getProjectWorkflows(
                projectId, draftVersion)) {

                projectWorkflowService.delete(projectId, draftVersion, draftProjectWorkflow.getWorkflowId());
            }

            assertThat(projectWorkflowService.getProjectWorkflows(projectId, draftVersion)).isEmpty();
            assertThat(
                projectCodeWorkflowService.getProjectCodeWorkflow(projectId)
                    .getProjectVersion()).isLessThan(draftVersion);

            automationProjectCodeWorkflowFacade.updateCodeWorkflowSource(
                projectId, jsSource(projectName, "first edit", "alpha", "beta"));

            assertThat(getLatestWorkflowUuidsByName(projectId)).isEqualTo(uploadedUuids);

            automationProjectFacade.publishProject(projectId);

            assertThat(getLatestWorkflowUuidsByName(projectId)).isEqualTo(uploadedUuids);
        }

        @Test
        void testAddingAndRemovingAWorkflowKeepsTheOthersAndRestoresTheRemovedOne() {
            String projectName = "uuid-continuity-add-remove";

            long projectId = automationProjectCodeWorkflowFacade.createEmptyCodeWorkflow(
                projectName, Language.JAVASCRIPT, null, null, null, null);

            automationProjectCodeWorkflowFacade.updateCodeWorkflowSource(
                projectId, jsSource(projectName, "v1", "alpha"));

            automationProjectFacade.publishProject(projectId);

            UUID alphaUuid = getLatestWorkflowUuidsByName(projectId).get("alpha");

            assertThat(alphaUuid).isNotNull();

            automationProjectCodeWorkflowFacade.updateCodeWorkflowSource(
                projectId, jsSource(projectName, "v2", "alpha", "beta"));
            automationProjectCodeWorkflowFacade.updateCodeWorkflowSource(
                projectId, jsSource(projectName, "v3", "alpha"));

            automationProjectFacade.publishProject(projectId);

            assertThat(getLatestWorkflowUuidsByName(projectId)).containsOnlyKeys("alpha")
                .containsEntry("alpha", alphaUuid);

            automationProjectCodeWorkflowFacade.updateCodeWorkflowSource(
                projectId, jsSource(projectName, "v4", "alpha", "beta"));

            automationProjectFacade.publishProject(projectId);

            Map<String, UUID> bothPublishedUuids = getLatestWorkflowUuidsByName(projectId);

            assertThat(bothPublishedUuids).containsOnlyKeys("alpha", "beta")
                .containsEntry("alpha", alphaUuid);

            UUID betaUuid = bothPublishedUuids.get("beta");

            automationProjectCodeWorkflowFacade.updateCodeWorkflowSource(
                projectId, jsSource(projectName, "v5", "alpha"));
            automationProjectCodeWorkflowFacade.updateCodeWorkflowSource(
                projectId, jsSource(projectName, "v6", "alpha", "beta"));

            assertThat(getLatestWorkflowUuidsByName(projectId)).containsEntry("alpha", alphaUuid)
                .containsEntry("beta", betaUuid);

            automationProjectFacade.publishProject(projectId);

            assertThat(getLatestWorkflowUuidsByName(projectId)).containsEntry("alpha", alphaUuid)
                .containsEntry("beta", betaUuid);
        }

        @Test
        void testPublishingWithoutAnEditSinceTheLastPublishIsRefused() {
            String projectName = "uuid-continuity-empty-draft";

            long projectId = automationProjectCodeWorkflowFacade.createEmptyCodeWorkflow(
                projectName, Language.JAVASCRIPT, null, null, null, null);

            automationProjectFacade.publishProject(projectId);

            int draftVersion = projectService.getProject(projectId)
                .getLastProjectVersion();

            assertThatThrownBy(() -> automationProjectFacade.publishProject(projectId))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(CodeWorkflowErrorType.CODE_WORKFLOW_DRAFT_EMPTY.getErrorKey()));

            assertThat(
                projectService.getProject(projectId)
                    .getLastProjectVersion()).isEqualTo(draftVersion);
        }

        private Map<String, UUID> getLatestWorkflowUuidsByName(long projectId) {
            ProjectCodeWorkflow projectCodeWorkflow = projectCodeWorkflowService.getProjectCodeWorkflow(projectId);

            Map<String, String> workflowNameIds = codeWorkflowContainerService
                .getCodeWorkflowContainer(projectCodeWorkflow.getCodeWorkflowContainerId())
                .getWorkflowNameIds();

            Map<String, UUID> workflowIdUuids = projectWorkflowService
                .getProjectWorkflows(projectId, projectCodeWorkflow.getProjectVersion())
                .stream()
                .collect(Collectors.toMap(ProjectWorkflow::getWorkflowId, ProjectWorkflow::getUuid));

            Map<String, UUID> workflowUuidsByName = new HashMap<>();

            for (Map.Entry<String, String> entry : workflowNameIds.entrySet()) {
                workflowUuidsByName.put(entry.getKey(), workflowIdUuids.get(entry.getValue()));
            }

            return workflowUuidsByName;
        }

        private List<String> getPublishedTemplateUuids(long projectId) {
            return automationProjectFacade.getPublishedProjects()
                .stream()
                .filter(project -> project.id() == projectId)
                .flatMap(project -> project.workflowTemplates()
                    .stream())
                .map(ConnectedUserWorkflowTemplateDTO::workflowUuid)
                .toList();
        }

        private String getPublishedSource(long projectId) {
            ProjectCodeWorkflow projectCodeWorkflow = projectCodeWorkflowService.getProjectCodeWorkflow(projectId);

            return codeWorkflowFileStorage.readCodeWorkflowFileContent(
                codeWorkflowContainerService.getCodeWorkflowContainer(projectCodeWorkflow.getCodeWorkflowContainerId())
                    .getWorkflows());
        }

        private static String jsSource(String projectName, String result, String... workflowNames) {
            StringBuilder workflows = new StringBuilder();

            for (String workflowName : workflowNames) {
                if (!workflows.isEmpty()) {
                    workflows.append(", ");
                }

                workflows.append(
                    "{name: \"%s\", label: \"%s\", tasks: [{name: \"my-task\", label: \"My Task\", "
                        .formatted(workflowName, workflowName)
                        + "perform: function() { return \"%s\"; }}]}".formatted(result));
            }

            return "({name: \"%s\", version: \"1\", description: \"A code workflow.\", workflows: [%s]})"
                .formatted(projectName, workflows);
        }

        @Configuration
        @Import(AutomationProjectFacadeIntTestConfiguration.class)
        static class CodeProjectLifecycleConfiguration {

            @Bean
            CodeWorkflowContainerFacade codeWorkflowContainerFacade(
                CodeWorkflowContainerService codeWorkflowContainerService,
                CodeWorkflowFileStorage codeWorkflowFileStorage,
                ObjectProvider<ComponentDefinitionService> componentDefinitionServiceProvider,
                ObjectMapper objectMapper, WorkflowService workflowService) {

                return new CodeWorkflowContainerFacadeImpl(
                    codeWorkflowContainerService, codeWorkflowFileStorage, componentDefinitionServiceProvider,
                    objectMapper, workflowService);
            }

            @Bean
            CodeWorkflowContainerService codeWorkflowContainerService(
                CodeWorkflowContainerRepository codeWorkflowContainerRepository) {

                return new CodeWorkflowContainerServiceImpl(codeWorkflowContainerRepository);
            }

            @Bean
            CodeWorkflowFileStorage codeWorkflowFileStorage() {
                return new InMemoryCodeWorkflowFileStorage();
            }

            @Bean
            ProjectCodeWorkflowDraftService projectCodeWorkflowDraftService(
                CodeWorkflowContainerFacade codeWorkflowContainerFacade,
                CodeWorkflowContainerService codeWorkflowContainerService,
                ProjectCodeWorkflowService projectCodeWorkflowService, ProjectWorkflowService projectWorkflowService,
                WorkflowService workflowService, ObjectProvider<WorkflowPreDeleteListener> workflowPreDeleteListeners) {

                return new ProjectCodeWorkflowDraftServiceImpl(
                    codeWorkflowContainerFacade, codeWorkflowContainerService, projectCodeWorkflowService,
                    projectWorkflowService, workflowService,
                    workflowPreDeleteListeners.orderedStream()
                        .toList());
            }

            @Bean
            ProjectCodeWorkflowService projectCodeWorkflowService(
                ProjectCodeWorkflowRepository projectCodeWorkflowRepository) {

                return new ProjectCodeWorkflowServiceImpl(projectCodeWorkflowRepository);
            }
        }
    }

    private static class InMemoryCodeWorkflowFileStorage implements CodeWorkflowFileStorage {

        private final Map<String, byte[]> contentsByUrl = new ConcurrentHashMap<>();

        @Override
        public void deleteCodeWorkflowFile(FileEntry fileEntry) {
            contentsByUrl.remove(fileEntry.getUrl());
        }

        @Override
        public URL getCodeWorkflowFileURL(FileEntry fileEntry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String readCodeWorkflowFileContent(FileEntry fileEntry) {
            return new String(contentsByUrl.get(fileEntry.getUrl()), StandardCharsets.UTF_8);
        }

        @Override
        public FileEntry storeCodeWorkflowFile(String filename, byte[] bytes) {
            String url = "memory://" + UUID.randomUUID();

            contentsByUrl.put(url, bytes);

            return new FileEntry(filename, url);
        }
    }
}
