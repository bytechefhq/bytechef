/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowDraftServiceImpl;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade.CodeWorkflowReconciliation;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
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
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(classes = ProjectCodeWorkflowFacadeIntTest.Config.class, properties = "bytechef.edition=ee")
@MockitoBean(types = {
    CacheManager.class, CategoryService.class, CodeWorkflowContainerFacade.class, CodeWorkflowContainerService.class,
    CodeWorkflowFileStorage.class,
    ComponentConnectionFacade.class, ComponentDefinitionHelper.class, ConnectionFacade.class,
    ConnectionLifecycleFacade.class, ConnectionService.class, EnvironmentService.class, Evaluator.class,
    JobFacade.class, JobService.class, PreBuiltTemplateService.class, PrincipalJobFacade.class,
    PrincipalJobService.class, ProjectCodeWorkflowService.class, ProjectDeploymentService.class,
    ProjectDeploymentWorkflowService.class, ProjectFacade.class, ProjectService.class, ProjectWorkflowService.class,
    SharedTemplateFileStorage.class, SharedTemplateService.class, TagService.class, TriggerDefinitionService.class,
    TriggerExecutionService.class, TriggerLifecycleFacade.class, WorkflowCacheManager.class, WorkflowFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowService.class, WorkflowTestConfigurationService.class,
    WorkflowValidatorFacade.class, WorkspaceConnectionService.class, WorkspaceService.class
})
class ProjectCodeWorkflowFacadeIntTest {

    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 31L;
    private static final long PROJECT_ID = 5L;
    private static final String PROJECT_NAME = "my-code-project";
    private static final long WORKSPACE_ID = 21L;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private ApplicationProperties applicationProperties;

    @Autowired
    private CodeWorkflowContainerFacade codeWorkflowContainerFacade;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private CodeWorkflowContainerService codeWorkflowContainerService;

    @Autowired
    private CodeWorkflowContainerFacade codeWorkflowContainerFacade;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade;

    @Autowired
    private ProjectCodeWorkflowService projectCodeWorkflowService;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        reset(
            codeWorkflowContainerFacade, codeWorkflowContainerService, componentConnectionFacade, environmentService,
            permissionService, projectCodeWorkflowService, projectDeploymentService, projectDeploymentWorkflowService,
            projectFacade, projectService, projectWorkflowService, workflowService);

        authenticate(AuthorityConstants.USER);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveIsAdminOnly() {
        assertRequiresAdminAuthority(() -> projectCodeWorkflowFacade.save(WORKSPACE_ID, new byte[0], Language.JAVA));
    }

    @Test
    void testSaveUpdatesTheSameNamedProjectOfTheCallerWorkspace() {
        authenticate(AuthorityConstants.ADMIN);

        Project project = new Project();

        project.setId(5L);
        project.setName("Orders");
        project.setWorkspaceId(WORKSPACE_ID);

        when(projectService.fetchProject("Orders", WORKSPACE_ID)).thenReturn(Optional.of(project));
        when(projectService.update(project)).thenReturn(project);

        saveProjectNamed("Orders");

        verify(projectService).update(project);
        verify(projectService, never()).create(any());
        verify(projectCodeWorkflowService).create(any(), eq(project));
    }

    @Test
    void testSaveCreatesAProjectInTheCallerWorkspaceWhenItHasNoProjectOfThatName() {
        authenticate(AuthorityConstants.ADMIN);

        Project createdProject = new Project();

        createdProject.setId(6L);

        when(projectService.fetchProject("Orders", WORKSPACE_ID)).thenReturn(Optional.empty());
        when(projectService.create(any())).thenReturn(createdProject);

        saveProjectNamed("Orders");

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);

        verify(projectService).create(projectArgumentCaptor.capture());
        verify(projectService, never()).update(any());

        Project project = projectArgumentCaptor.getValue();

        assertThat(project.getName()).isEqualTo("Orders");
        assertThat(project.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
    }

    private void saveProjectNamed(String name) {
        ProjectDefinition projectDefinition = mock(ProjectDefinition.class);

        when(projectDefinition.getName()).thenReturn(name);

        ProjectHandler projectHandler = mock(ProjectHandler.class);

        when(projectHandler.getDefinition()).thenReturn(projectDefinition);
        when(codeWorkflowContainerFacade.create(any(), any(), any(), any(), any(), any()))
            .thenReturn(mock(CodeWorkflowContainer.class));

        try (MockedStatic<ProjectHandlerLoader> projectHandlerLoader = mockStatic(ProjectHandlerLoader.class)) {
            projectHandlerLoader
                .when(() -> ProjectHandlerLoader.loadProjectHandler(any(), any(), any(), any()))
                .thenReturn(projectHandler);

            projectCodeWorkflowFacade.save(WORKSPACE_ID, new byte[0], Language.JAVA);
        }
    }

    @Test
    void testSaveWithoutEnableLeavesDeploymentsAlone() {
        authenticate(AuthorityConstants.ADMIN);

        stubPublishOfNewProject();

        projectCodeWorkflowFacade.save(WORKSPACE_ID, projectBytes(), Language.JAVASCRIPT, Environment.STAGING, false);

        verify(projectFacade).publishProject(PROJECT_ID, null, false);
        verify(projectDeploymentService, never()).fetchProjectDeployment(anyLong(), any());
        verify(projectDeploymentService, never()).create(any());
    }

    @Test
    void testSaveWithEnableCreatesAnEnabledDeploymentOfThePublishedVersion() {
        authenticate(AuthorityConstants.ADMIN);

        stubPublishOfNewProject();
        stubDeploymentCreation();

        projectCodeWorkflowFacade.save(WORKSPACE_ID, projectBytes(), Language.JAVASCRIPT, Environment.STAGING, true);

        ArgumentCaptor<ProjectDeployment> projectDeploymentArgumentCaptor =
            ArgumentCaptor.forClass(ProjectDeployment.class);

        verify(projectDeploymentService).create(projectDeploymentArgumentCaptor.capture());

        ProjectDeployment projectDeployment = projectDeploymentArgumentCaptor.getValue();

        assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(projectDeployment.getName()).isEqualTo(PROJECT_NAME);
        assertThat(projectDeployment.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(projectDeployment.getProjectVersion()).isEqualTo(1);

        verify(projectDeploymentWorkflowService).updateEnabled(PROJECT_DEPLOYMENT_WORKFLOW_ID, true);
        verify(projectDeploymentService).updateEnabled(PROJECT_DEPLOYMENT_ID, true);
    }

    @Test
    void testSaveWithEnableLeavesAWorkflowThatNeedsAConnectionDisabled() {
        authenticate(AuthorityConstants.ADMIN);

        stubPublishOfNewProject();
        stubDeploymentCreation();

        when(workflowService.getWorkflow("wf-1"))
            .thenReturn(
                new Workflow(
                    "wf-1", "{\"tasks\":[{\"name\":\"slack_1\",\"type\":\"slack/v1/sendMessage\"}]}",
                    Workflow.Format.JSON));
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenReturn(List.of(new ComponentConnection("slack", 1, "slack_1", "slack", true)));

        projectCodeWorkflowFacade.save(WORKSPACE_ID, projectBytes(), Language.JAVASCRIPT, Environment.STAGING, true);

        verify(projectDeploymentService).create(any(ProjectDeployment.class));
        verify(projectDeploymentWorkflowService, never()).updateEnabled(PROJECT_DEPLOYMENT_WORKFLOW_ID, true);
        verify(projectDeploymentService).updateEnabled(PROJECT_DEPLOYMENT_ID, true);
    }

    @Test
    void testSaveWithEnableMovesTheExistingDeploymentToThePublishedVersion() {
        authenticate(AuthorityConstants.ADMIN);

        stubMoveToVersionTwo();

        projectCodeWorkflowFacade.save(WORKSPACE_ID, projectBytes(), Language.JAVASCRIPT, Environment.STAGING, true);

        ArgumentCaptor<ProjectDeployment> projectDeploymentArgumentCaptor =
            ArgumentCaptor.forClass(ProjectDeployment.class);

        verify(projectDeploymentService).update(projectDeploymentArgumentCaptor.capture());

        assertThat(projectDeploymentArgumentCaptor.getValue()
            .getProjectVersion()).isEqualTo(2);

        verify(projectDeploymentService, never()).create(any());
        verify(projectDeploymentWorkflowService).updateEnabled(22L, true);
        verify(projectDeploymentService).updateEnabled(PROJECT_DEPLOYMENT_ID, true);

        ArgumentCaptor<ProjectDeploymentWorkflow> projectDeploymentWorkflowArgumentCaptor =
            ArgumentCaptor.forClass(ProjectDeploymentWorkflow.class);

        verify(projectDeploymentWorkflowService).update(projectDeploymentWorkflowArgumentCaptor.capture());

        ProjectDeploymentWorkflow carriedProjectDeploymentWorkflow = projectDeploymentWorkflowArgumentCaptor.getValue();

        assertThat(carriedProjectDeploymentWorkflow.getId()).isEqualTo(21L);
        assertThat(carriedProjectDeploymentWorkflow.getWorkflowId()).isEqualTo("wf-1");
        assertThat(carriedProjectDeploymentWorkflow.isEnabled()).isTrue();

        Map<String, ?> carriedInputs = carriedProjectDeploymentWorkflow.getInputs();

        assertThat(carriedInputs.get("channel")).isEqualTo("general");

        verify(projectDeploymentWorkflowService, never()).delete(21L);
    }

    @Test
    void testSaveWithEnableLeavesANewWorkflowThatNeedsAConnectionDisabledWhenMoving() {
        authenticate(AuthorityConstants.ADMIN);

        stubMoveToVersionTwo();

        when(workflowService.getWorkflow("wf-2"))
            .thenReturn(
                new Workflow(
                    "wf-2", "{\"tasks\":[{\"name\":\"slack_1\",\"type\":\"slack/v1/sendMessage\"}]}",
                    Workflow.Format.JSON));
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenReturn(List.of(new ComponentConnection("slack", 1, "slack_1", "slack", true)));

        projectCodeWorkflowFacade.save(WORKSPACE_ID, projectBytes(), Language.JAVASCRIPT, Environment.STAGING, true);

        verify(projectDeploymentWorkflowService, never()).updateEnabled(22L, true);
        verify(projectDeploymentService).updateEnabled(PROJECT_DEPLOYMENT_ID, true);
    }

    @Test
    void testFirstUploadAddsEveryWorkflowToTheDraftAndPublishesIt() {
        authenticate(AuthorityConstants.ADMIN);

        when(projectService.create(any(Project.class))).thenAnswer(invocation -> newProject());

        stubReconcilingCodeWorkflowContainer();

        projectCodeWorkflowFacade.save(WORKSPACE_ID, projectBytes("my-workflow"), Language.JAVASCRIPT);

        verify(codeWorkflowContainerFacade).create(
            eq(PROJECT_NAME), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.AUTOMATION),
            eq(Map.of()));
        verify(projectWorkflowService).addWorkflow(PROJECT_ID, 1, "new-my-workflow");

        InOrder inOrder = inOrder(projectWorkflowService, projectFacade);

        inOrder.verify(projectWorkflowService)
            .addWorkflow(PROJECT_ID, 1, "new-my-workflow");
        inOrder.verify(projectFacade)
            .publishProject(PROJECT_ID, null, false);

        verify(projectService, never()).publishProject(anyLong(), any(), anyBoolean());
    }

    @Test
    void testReUploadKeepsWorkflowIdsByNameDropsRemovedWorkflowsAndAddsNewOnes() {
        authenticate(AuthorityConstants.ADMIN);

        Project project = publishedProject(1);

        when(projectService.fetchProject(PROJECT_NAME)).thenReturn(Optional.of(project));
        when(projectService.update(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID keptWorkflowUuid = UUID.randomUUID();
        UUID removedWorkflowUuid = UUID.randomUUID();

        stubPublishedProjectCodeWorkflow(
            Map.of("kept-workflow", "published-kept", "removed-workflow", "published-removed"));

        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, 1))
            .thenReturn(
                List.of(
                    new ProjectWorkflow(PROJECT_ID, 1, "published-kept", keptWorkflowUuid),
                    new ProjectWorkflow(PROJECT_ID, 1, "published-removed", removedWorkflowUuid)));
        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, 2))
            .thenReturn(
                List.of(
                    new ProjectWorkflow(PROJECT_ID, 2, "draft-kept", keptWorkflowUuid),
                    new ProjectWorkflow(PROJECT_ID, 2, "draft-removed", removedWorkflowUuid)));

        stubReconcilingCodeWorkflowContainer();

        projectCodeWorkflowFacade.save(
            WORKSPACE_ID, projectBytes("kept-workflow", "added-workflow"), Language.JAVASCRIPT);

        verify(codeWorkflowContainerFacade).create(
            eq(PROJECT_NAME), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.AUTOMATION),
            eq(Map.of("kept-workflow", "draft-kept", "removed-workflow", "draft-removed")));

        verify(projectWorkflowService, times(1)).addWorkflow(anyLong(), anyInt(), anyString());
        verify(projectWorkflowService).addWorkflow(PROJECT_ID, 2, "new-added-workflow");
        verify(projectWorkflowService, never()).update(any(ProjectWorkflow.class));

        verify(projectWorkflowService, times(1)).delete(anyLong(), anyInt(), anyString());
        verify(projectWorkflowService).delete(PROJECT_ID, 2, "draft-removed");
        verify(workflowService).delete("draft-removed");
        verify(workflowService, never()).delete("draft-kept");

        verify(projectFacade).publishProject(PROJECT_ID, null, false);
        verify(projectService, never()).publishProject(anyLong(), any(), anyBoolean());
    }

    private ProjectDeployment existingProjectDeployment() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.STAGING);
        projectDeployment.setId(PROJECT_DEPLOYMENT_ID);
        projectDeployment.setName(PROJECT_NAME);
        projectDeployment.setProjectId(PROJECT_ID);
        projectDeployment.setProjectVersion(1);

        return projectDeployment;
    }

    private static Project newProject() {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setName(PROJECT_NAME);

        return project;
    }

    private static byte[] projectBytes() {
        return ("({name: \"" + PROJECT_NAME + "\", version: \"1\", workflows: [{name: \"my-workflow\", "
            + "label: \"My Workflow\", tasks: [{name: \"my-task\", label: \"My Task\", "
            + "perform: function() { return \"hello\"; }}]}]})").getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] projectBytes(String... workflowNames) {
        String workflows = Arrays.stream(workflowNames)
            .map(workflowName -> "{name: \"" + workflowName + "\", label: \"" + workflowName + "\", tasks: [{name: "
                + "\"my-task\", label: \"My Task\", perform: function() { return \"hello\"; }}]}")
            .collect(Collectors.joining(", "));

        return ("({name: \"" + PROJECT_NAME + "\", version: \"1\", workflows: [" + workflows + "]})")
            .getBytes(StandardCharsets.UTF_8);
    }

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(long id, String workflowId, boolean enabled) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(List.of());
        projectDeploymentWorkflow.setEnabled(enabled);
        projectDeploymentWorkflow.setId(id);
        projectDeploymentWorkflow.setInputs(Map.of());
        projectDeploymentWorkflow.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private static Project publishedProject(int publishedVersions) {
        Project project = newProject();

        for (int index = 0; index < publishedVersions; index++) {
            project.publish(null);
        }

        return project;
    }

    private void stubCodeWorkflowContainer() {
        stubReconcilingCodeWorkflowContainer();

        when(workflowService.getWorkflow(anyString()))
            .thenAnswer(invocation -> new Workflow(invocation.getArgument(0), "{\"tasks\":[]}", Workflow.Format.JSON));
        when(environmentService.getEnvironment(anyLong())).thenCallRealMethod();
        when(permissionService.hasResourceScope(any(), eq("Project"), eq("DEPLOYMENT_PUSH"))).thenReturn(true);
    }

    private void stubPublishedProjectCodeWorkflow(Map<String, String> publishedWorkflowNameIds) {
        ProjectCodeWorkflow publishedProjectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(publishedProjectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(7L);
        when(publishedProjectCodeWorkflow.getProjectVersion()).thenReturn(1);

        CodeWorkflowContainer publishedCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(publishedCodeWorkflowContainer.getWorkflowNameIds()).thenReturn(publishedWorkflowNameIds);

        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(PROJECT_ID))
            .thenReturn(Optional.of(publishedProjectCodeWorkflow));
        when(projectCodeWorkflowService.fetchPublishedProjectCodeWorkflow(PROJECT_ID, 2))
            .thenReturn(Optional.of(publishedProjectCodeWorkflow));
        when(codeWorkflowContainerService.getCodeWorkflowContainer(7L)).thenReturn(publishedCodeWorkflowContainer);
    }

    private void stubReconcilingCodeWorkflowContainer() {
        when(codeWorkflowContainerFacade.create(
            eq(PROJECT_NAME), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.AUTOMATION), any()))
                .thenAnswer(invocation -> reconcile(invocation.getArgument(2), invocation.getArgument(6)));
        when(codeWorkflowContainerFacade.create(
            eq(PROJECT_NAME), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.AUTOMATION)))
                .thenAnswer(invocation -> {
                    CodeWorkflowReconciliation reconciliation = reconcile(invocation.getArgument(2), Map.of());

                    return reconciliation.codeWorkflowContainer();
                });
    }

    private static CodeWorkflowReconciliation reconcile(
        List<? extends WorkflowDefinition> workflowDefinitions, Map<String, String> reusableWorkflowNameIds) {

        Set<String> workflowNames = workflowDefinitions.stream()
            .map(WorkflowDefinition::getName)
            .collect(Collectors.toSet());

        Map<String, String> addedWorkflowNameIds = workflowNames.stream()
            .filter(workflowName -> !reusableWorkflowNameIds.containsKey(workflowName))
            .collect(Collectors.toMap(Function.identity(), workflowName -> "new-" + workflowName));

        Map<String, String> removedWorkflowNameIds = reusableWorkflowNameIds.entrySet()
            .stream()
            .filter(entry -> !workflowNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, String> workflowNameIds = workflowNames.stream()
            .collect(Collectors.toMap(Function.identity(),
                workflowName -> reusableWorkflowNameIds.getOrDefault(workflowName, "new-" + workflowName)));

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getWorkflowNameIds()).thenReturn(workflowNameIds);

        return new CodeWorkflowReconciliation(codeWorkflowContainer, addedWorkflowNameIds, removedWorkflowNameIds);
    }

    private void stubDeploymentCreation() {
        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, 1))
            .thenReturn(List.of(new ProjectWorkflow(PROJECT_ID, 1, "wf-1", UUID.randomUUID())));
        when(projectDeploymentService.create(any(ProjectDeployment.class)))
            .thenAnswer(invocation -> {
                ProjectDeployment projectDeployment = invocation.getArgument(0);

                projectDeployment.setId(PROJECT_DEPLOYMENT_ID);

                return projectDeployment;
            });
        when(projectDeploymentService.getProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenAnswer(invocation -> existingProjectDeployment());
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, "wf-1"))
            .thenReturn(projectDeploymentWorkflow(PROJECT_DEPLOYMENT_WORKFLOW_ID, "wf-1", false));
    }

    private void stubMoveToVersionTwo() {
        Project project = newProject();

        project.publish(null);

        when(projectService.fetchProject(PROJECT_NAME)).thenReturn(Optional.of(project));
        when(projectService.update(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectService.getProject(PROJECT_ID)).thenAnswer(invocation -> publishedProject(2));

        stubCodeWorkflowContainer();

        UUID carriedWorkflowUuid = UUID.randomUUID();

        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID))
            .thenReturn(
                List.of(
                    new ProjectWorkflow(PROJECT_ID, 1, "wf-old", carriedWorkflowUuid),
                    new ProjectWorkflow(PROJECT_ID, 2, "wf-1", carriedWorkflowUuid),
                    new ProjectWorkflow(PROJECT_ID, 2, "wf-2", UUID.randomUUID())));
        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, 2))
            .thenReturn(
                List.of(
                    new ProjectWorkflow(PROJECT_ID, 2, "wf-1", carriedWorkflowUuid),
                    new ProjectWorkflow(PROJECT_ID, 2, "wf-2", UUID.randomUUID())));
        when(projectWorkflowService.getProjectWorkflowUuid(PROJECT_DEPLOYMENT_ID, "wf-old"))
            .thenReturn(carriedWorkflowUuid.toString());

        when(projectDeploymentService.fetchProjectDeployment(PROJECT_ID, Environment.STAGING))
            .thenAnswer(invocation -> Optional.of(existingProjectDeployment()));
        when(projectDeploymentService.getProjectDeploymentId(PROJECT_ID, Environment.STAGING))
            .thenReturn(PROJECT_DEPLOYMENT_ID);
        when(projectDeploymentService.getProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenAnswer(invocation -> existingProjectDeployment());
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(PROJECT_DEPLOYMENT_ID))
            .thenAnswer(invocation -> {
                ProjectDeploymentWorkflow carriedProjectDeploymentWorkflow = projectDeploymentWorkflow(
                    21L, "wf-old", true);

                carriedProjectDeploymentWorkflow.setInputs(Map.of("channel", "general"));

                return List.of(carriedProjectDeploymentWorkflow);
            });
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, "wf-2"))
            .thenReturn(projectDeploymentWorkflow(22L, "wf-2", false));
    }

    private void stubPublishOfNewProject() {
        when(projectService.create(any(Project.class))).thenAnswer(invocation -> newProject());
        when(projectService.getProject(PROJECT_ID)).thenAnswer(invocation -> publishedProject(1));

        stubCodeWorkflowContainer();
    }

    private void assertRequiresAdminAuthority(ThrowingCallable call) {
        authenticate(AuthorityConstants.USER);

        assertDenied(call);

        authenticate(AuthorityConstants.ADMIN);

        assertNotDenied(call);

        authenticate(AuthorityConstants.USER);
    }

    private static void assertDenied(ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
    }

    private static void assertNotDenied(ThrowingCallable call) {
        Throwable throwable = catchThrowable(call);

        assertThat(throwable)
            .as("Expected the gate to let the call through, but it was denied: %s", throwable)
            .satisfiesAnyOf(
                actual -> assertThat(actual).isNull(),
                actual -> assertThat(actual).isNotInstanceOf(AccessDeniedException.class));
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    @SpringBootConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ProjectCodeWorkflowDraftServiceImpl.class, ProjectCodeWorkflowFacadeImpl.class,
        ProjectDeploymentFacadeImpl.class
    })
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }
}
