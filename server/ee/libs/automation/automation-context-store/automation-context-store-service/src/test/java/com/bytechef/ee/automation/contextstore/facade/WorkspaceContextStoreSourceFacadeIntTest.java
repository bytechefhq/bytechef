/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.contextstore.facade.config.ContextStoreSchedulingIntTestConfiguration;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.githubproxy.client.WorkflowTemplateProxyClient;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies end-to-end (against a real PostgreSQL via the {@link ProjectDeploymentFacade} graph) that enabling a Context
 * Store source enables its parent project deployment and reaches the schedule trigger-registration path. The scheduler
 * is mocked at {@link TriggerLifecycleFacade} so the test asserts the registration call without standing up Quartz.
 *
 * @version ee
 */
@SpringBootTest(
    classes = ContextStoreSchedulingIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class, ComponentConnectionFacade.class,
    ComponentDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class, EnvironmentService.class,
    GitHubProxyClient.class, JobFacade.class, JobService.class, ConnectionService.class, PrincipalJobFacade.class,
    PrincipalJobService.class, TaskExecutionService.class, TriggerDefinitionService.class,
    TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTemplateProxyClient.class
})
public class WorkspaceContextStoreSourceFacadeIntTest {

    private static final String WORKFLOW_WITH_SCHEDULE_TRIGGER =
        "{\"label\": \"Test Workflow\", \"description\": \"Test Description\", " +
            "\"triggers\": [{\"label\": \"Schedule\", \"name\": \"trigger_1\", " +
            "\"type\": \"schedule/v1/schedule\"}], \"tasks\": []}";

    private static final long SOURCE_ID = 555L;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ContextStoreSourceService contextStoreSourceService;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Autowired
    private WorkspaceContextStoreSourceFacadeImpl workspaceContextStoreSourceFacade;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace;

    @BeforeEach
    void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));

        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt()))
            .thenReturn(Page.empty());

        // Prevent NPE in getStaticWebhookUrl during trigger enable (mirrors ProjectDeploymentFacadeIntTest).
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenThrow(new IllegalArgumentException("Trigger definition not found"));
    }

    @AfterEach
    void afterEach() {
        projectDeploymentWorkflowRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void testSetEnabledEnablesDisabledDeploymentAndRegistersScheduleTrigger() {
        // Given: a Context Store-owned project/deployment/workflow with a schedule trigger, where the parent
        // deployment starts disabled (the broken-state Defect B reproduced).
        long projectDeploymentId = setUpContextStoreDeployment();

        String workflowId = projectDeploymentFacade.getProjectDeployment(projectDeploymentId)
            .projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        ContextStoreSource source = new ContextStoreSource();

        source.setId(SOURCE_ID);
        source.setEnabled(false);
        source.setWorkflowId(workflowId);

        when(contextStoreSourceService.get(SOURCE_ID)).thenReturn(source);

        reset(triggerLifecycleFacade);

        // When
        workspaceContextStoreSourceFacade.setEnabled(workspace.getId(), SOURCE_ID, true);

        // Then: the parent deployment is persisted enabled (Defect B) ...
        ProjectDeployment projectDeployment = projectDeploymentRepository.findById(projectDeploymentId)
            .orElseThrow();

        assertThat(projectDeployment.isEnabled()).isTrue();

        // ... the deployment workflow is enabled ...
        assertThat(projectDeploymentFacade.getProjectDeployment(projectDeploymentId)
            .projectDeploymentWorkflows()
            .getFirst()
            .enabled()).isTrue();

        // ... and the schedule trigger was actually registered with the scheduler (Defect A).
        verify(triggerLifecycleFacade, atLeastOnce())
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
    }

    private long setUpContextStoreDeployment() {
        Category category = categoryRepository.save(new Category("Context Store"));

        ProjectDTO projectDTO = projectFacade.getProject(
            projectFacade.createProject(
                ProjectDTO.builder()
                    .category(category)
                    .description("Context Store sync project")
                    .name("Seed name")
                    .workspaceId(workspace.getId())
                    .build()));

        projectWorkflowFacade.addWorkflow(projectDTO.id(), WORKFLOW_WITH_SCHEDULE_TRIGGER);

        projectFacade.publishProject(projectDTO.id(), "Published for test", false);

        Project dbProject = projectRepository.findById(projectDTO.id())
            .orElseThrow();

        int publishedVersion = dbProject.getProjectVersions()
            .stream()
            .filter(projectVersion -> projectVersion.getStatus() == ProjectVersion.Status.PUBLISHED)
            .toList()
            .getLast()
            .getVersion();

        ProjectWorkflow publishedProjectWorkflow = projectWorkflowRepository
            .findAllByProjectIdAndProjectVersion(projectDTO.id(), publishedVersion)
            .getFirst();

        ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO = new ProjectDeploymentWorkflowDTO(
            List.of(), null, null, Map.of(), true, null, null, null, null, null, null, null, 0,
            publishedProjectWorkflow.getWorkflowId(), publishedProjectWorkflow.getUuidAsString());

        long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(
            ProjectDeploymentDTO.builder()
                .projectId(projectDTO.id())
                .name("Context Store deployment")
                .environment(Environment.DEVELOPMENT)
                .projectVersion(publishedVersion)
                .projectDeploymentWorkflows(List.of(projectDeploymentWorkflowDTO))
                .build());

        // Rename so the facade's findOrCreateContextStoreProject resolves this project by its system name.
        Project contextStoreProject = projectRepository.findById(projectDTO.id())
            .orElseThrow();

        contextStoreProject.setName(
            WorkspaceContextStoreSourceFacadeImpl.CONTEXT_STORE_PROJECT_NAME_PREFIX + workspace.getId());

        projectRepository.save(contextStoreProject);

        // Start from the broken state: parent deployment disabled.
        projectDeploymentService.updateEnabled(projectDeploymentId, false);

        return projectDeploymentId;
    }
}
