/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.automation.configuration.callback.ProjectCallback;
import com.bytechef.automation.configuration.callback.ProjectWorkflowCallback;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.cache.config.CacheConfiguration;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentServiceImpl;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.dto.FocusPathElementDTO;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionChildrenPageDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.repository.WorkflowExecutionRowRepositoryImpl;
import com.bytechef.platform.workflow.execution.repository.converter.JobResumeIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToJobResumeIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToTaskStateValueConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToTriggerStateValueConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowExecutionIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowTriggerConverter;
import com.bytechef.platform.workflow.execution.repository.converter.TaskStateValueToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.TriggerStateValueToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowExecutionIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowTriggerToStringConverter;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobServiceImpl;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionServiceImpl;
import com.bytechef.platform.workflow.execution.service.WorkflowExecutionRowServiceImpl;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Runs {@link ProjectWorkflowExecutionFacadeImpl} over the real job, task execution, context, principal job, project,
 * project deployment, project workflow and workflow services backed by Postgres. Only the component/task-dispatcher
 * registries, the trigger file storage, {@link ProjectFacade} and the embedded deployment owner contributor (normally
 * supplied by the embedded module) are test doubles. {@link JobService}, {@link TaskExecutionService} and
 * {@link ProjectDeploymentService} are spies over the real beans: they count calls for the cases that pin how often
 * the facade hits the database, and stub the "missing row" cases that foreign keys make impossible to arrange.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectWorkflowExecutionFacadeIntTest.ProjectWorkflowExecutionFacadeIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Transactional
class ProjectWorkflowExecutionFacadeIntTest {

    private static final String WORKFLOW_DEFINITION = """
        {
          "label": "Test Workflow",
          "tasks": [
            {"name": "task", "type": "myComponent/v1/myAction", "parameters": {"evaluated": true}}
          ]
        }
        """;

    @MockitoBean
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private EmbeddedProjectDeploymentIds embeddedProjectDeploymentIds;

    @Autowired
    private ProjectWorkflowExecutionFacade facade;

    @Autowired
    private JdbcJobRepository jobRepository;

    @MockitoSpyBean
    private JobService jobService;

    @Autowired
    private PrincipalJobService principalJobService;

    @MockitoSpyBean
    private ProjectDeploymentService projectDeploymentService;

    @MockitoBean
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @MockitoBean
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    @MockitoSpyBean
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @MockitoBean
    private TriggerFileStorage triggerFileStorage;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace;

    @BeforeEach
    void beforeEach() {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getTitle()).thenReturn("Title");
        when(componentDefinition.getIcon()).thenReturn("icon");
        when(componentDefinitionService.hasComponentDefinition(anyString(), any())).thenReturn(true);
        when(componentDefinitionService.getComponentDefinition(anyString(), any())).thenReturn(componentDefinition);
        when(projectFacade.getWorkspaceProjectWorkflows(anyLong())).thenReturn(List.of());

        workspace = workspaceRepository.save(new Workspace("execution-workspace"));
    }

    @AfterEach
    void afterEach() {
        embeddedProjectDeploymentIds.clear();
    }

    @Test
    void testToTaskExecutionDTODoesNotLoadTaskDataForList() {
        Job job = saveJob(createWorkflowId(createProject(workspace)), null);
        TaskExecution taskExecution = saveTaskExecutionWithData(job, null);

        TaskExecutionDTO taskExecutionDTO = getFacadeTarget().toTaskExecutionDTO(taskExecution, null, false);

        assertThat(taskExecutionDTO.input()).isNull();
        assertThat(taskExecutionDTO.output()).isNull();

        verify(taskExecutionService, never()).getTaskExecution(anyLong());
    }

    @Test
    void testToTaskExecutionDTOLoadsTaskDataForDetail() {
        Job job = saveJob(createWorkflowId(createProject(workspace)), null);
        TaskExecution taskExecution = saveTaskExecutionWithData(job, null);

        TaskExecutionDTO taskExecutionDTO = getFacadeTarget().toTaskExecutionDTO(taskExecution, null, true);

        assertThat(taskExecutionDTO.input()).isEqualTo(Map.of("evaluated", true));
        assertThat(taskExecutionDTO.output()).isEqualTo("output-value");
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionLoadsTaskData() {
        Job job = saveJob(createWorkflowId(createProject(workspace)), null);
        TaskExecution taskExecution = saveTaskExecutionWithData(job, null);

        TaskExecutionDTO taskExecutionDTO = facade.getWorkflowExecutionTaskExecution(
            getId(job), getId(taskExecution));

        assertThat(taskExecutionDTO.input()).isEqualTo(Map.of("evaluated", true));
        assertThat(taskExecutionDTO.output()).isEqualTo("output-value");
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionAttachesShallowChildJob() {
        String workflowId = createWorkflowId(createProject(workspace));

        Job job = saveJob(workflowId, null);
        TaskExecution taskExecution = saveTaskExecutionWithData(job, null);

        Job childJob = new Job();

        childJob.setInputs(Map.of("input", "value"));
        childJob.setLabel("Subflow");
        childJob.setParentTaskExecutionId(taskExecution.getId());
        childJob.setStatus(Job.Status.COMPLETED);
        childJob.setWorkflowId(workflowId);

        childJob = jobRepository.save(childJob);

        saveTaskExecution(childJob, null);

        TaskExecutionDTO taskExecutionDTO = facade.getWorkflowExecutionTaskExecution(
            getId(job), getId(taskExecution));

        JobDTO childJobDTO = Objects.requireNonNull(taskExecutionDTO.childJob());

        assertThat(childJobDTO.id()).isEqualTo(childJob.getId());
        assertThat(childJobDTO.label()).isEqualTo("Subflow");
        assertThat(childJobDTO.status()).isEqualTo(Job.Status.COMPLETED);
        assertThat(childJobDTO.inputs()).isEmpty();
        assertThat(childJobDTO.taskExecutions()).isEmpty();

        verify(jobService, never()).getChildJobIds(anyLong());
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionWithoutChildJob() {
        Job job = saveJob(createWorkflowId(createProject(workspace)), null);
        TaskExecution taskExecution = saveTaskExecutionWithData(job, null);

        TaskExecutionDTO taskExecutionDTO = facade.getWorkflowExecutionTaskExecution(
            getId(job), getId(taskExecution));

        assertThat(taskExecutionDTO.childJob()).isNull();
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionAcceptsTaskInDescendantJob() {
        String workflowId = createWorkflowId(createProject(workspace));

        Job rootJob = saveJob(workflowId, null);
        TaskExecution parentTaskExecution = saveTaskExecution(rootJob, null);
        Job childJob = saveJob(workflowId, parentTaskExecution.getId());
        TaskExecution taskExecution = saveTaskExecutionWithData(childJob, null);

        TaskExecutionDTO taskExecutionDTO = facade.getWorkflowExecutionTaskExecution(
            getId(rootJob), getId(taskExecution));

        assertThat(taskExecutionDTO.input()).isEqualTo(Map.of("evaluated", true));
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionChildrenAcceptsTaskInDescendantJob() {
        String workflowId = createWorkflowId(createProject(workspace));

        Job rootJob = saveJob(workflowId, null);
        TaskExecution parentTaskExecution = saveTaskExecution(rootJob, null);
        Job childJob = saveJob(workflowId, parentTaskExecution.getId());
        TaskExecution taskExecution = saveTaskExecution(childJob, null);

        TaskExecutionChildrenPageDTO taskExecutionChildrenPageDTO = facade.getWorkflowExecutionTaskExecutionChildren(
            getId(rootJob), getId(taskExecution), 0, 50);

        assertThat(taskExecutionChildrenPageDTO.number()).isZero();
        assertThat(taskExecutionChildrenPageDTO.size()).isEqualTo(50);
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionRejectsTaskFromAnotherWorkflowExecution() {
        String workflowId = createWorkflowId(createProject(workspace));

        Job job = saveJob(workflowId, null);
        TaskExecution taskExecution = saveTaskExecution(job, null);
        Job otherJob = saveJob(workflowId, null);

        assertThatThrownBy(() -> facade.getWorkflowExecutionTaskExecution(getId(otherJob), getId(taskExecution)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetSubflowJobTaskExecutionsSkipsTaskLoadWhenNoChildJobs() {
        Job job = saveJob(createWorkflowId(createProject(workspace)), null);

        saveTaskExecution(job, null);

        List<TaskExecutionDTO> taskExecutionDTOs = getFacadeTarget().getSubflowJobTaskExecutions(getId(job));

        assertThat(taskExecutionDTOs).isEmpty();

        verify(taskExecutionService, never()).getJobTaskExecutions(anyLong());
        verify(taskExecutionService, never()).getTaskExecutions(anyList());
    }

    @Test
    void testGetSubflowJobTaskExecutionsLoadsParentTaskExecutionsInOneCall() {
        String workflowId = createWorkflowId(createProject(workspace));

        Job job = saveJob(workflowId, null);
        TaskExecution firstParentTaskExecution = saveTaskExecution(job, null);
        TaskExecution secondParentTaskExecution = saveTaskExecution(job, null);
        Job firstChildJob = saveJob(workflowId, firstParentTaskExecution.getId());
        Job secondChildJob = saveJob(workflowId, secondParentTaskExecution.getId());

        List<TaskExecutionDTO> taskExecutionDTOs = getFacadeTarget().getSubflowJobTaskExecutions(getId(job));

        assertThat(taskExecutionDTOs)
            .extracting(TaskExecutionDTO::id)
            .containsExactlyInAnyOrder(firstParentTaskExecution.getId(), secondParentTaskExecution.getId());
        assertThat(taskExecutionDTOs)
            .extracting(taskExecutionDTO -> Objects.requireNonNull(taskExecutionDTO.childJob())
                .id())
            .containsExactlyInAnyOrder(firstChildJob.getId(), secondChildJob.getId());

        verify(taskExecutionService, times(1)).getTaskExecutions(anyList());
        verify(taskExecutionService, never()).getTaskExecution(anyLong());
    }

    @Test
    void testGetSubflowJobTaskExecutionsFailsWhenParentTaskExecutionIsMissing() {
        String workflowId = createWorkflowId(createProject(workspace));

        Job job = saveJob(workflowId, null);
        TaskExecution firstParentTaskExecution = saveTaskExecution(job, null);
        TaskExecution secondParentTaskExecution = saveTaskExecution(job, null);

        saveJob(workflowId, firstParentTaskExecution.getId());
        saveJob(workflowId, secondParentTaskExecution.getId());

        doReturn(List.of(secondParentTaskExecution))
            .when(taskExecutionService)
            .getTaskExecutions(anyList());

        assertThatThrownBy(() -> getFacadeTarget().getSubflowJobTaskExecutions(getId(job)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("'" + firstParentTaskExecution.getId() + "'");
    }

    @Test
    void testGetWorkflowExecutionLazyForChildJob() {
        Project project = createProject(workspace);

        Job rootJob = saveJob(createWorkflowId(project), null);
        TaskExecution parentTaskExecution = saveTaskExecution(rootJob, null);
        Job childJob = saveJob(createWorkflowId(project), parentTaskExecution.getId());
        TaskExecution topLevelTaskExecution = saveTaskExecution(childJob, null);
        ProjectDeployment projectDeployment = createProjectDeployment(project, "root");

        principalJobService.create(getId(rootJob), getId(projectDeployment), PlatformType.AUTOMATION);

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(getId(childJob), true);

        assertThat(workflowExecutionDTO.projectDeployment())
            .extracting(ProjectDeployment::getId)
            .isEqualTo(projectDeployment.getId());
        assertThat(workflowExecutionDTO.triggerExecution()).isNull();
        assertThat(workflowExecutionDTO.focusPath())
            .containsExactly(new FocusPathElementDTO(getId(topLevelTaskExecution), null, null));
        assertThat(workflowExecutionDTO.taskExecutionCount()).isEqualTo(1L);

        JobDTO jobDTO = Objects.requireNonNull(workflowExecutionDTO.job());

        assertThat(jobDTO.taskExecutions())
            .extracting(TaskExecutionDTO::id, TaskExecutionDTO::childCount)
            .containsExactly(tuple(topLevelTaskExecution.getId(), 0));

        verify(taskExecutionService, never()).getJobTaskExecutions(anyLong());
    }

    @Test
    void testGetWorkflowExecutionWalksNestedSubflowsUpToTheRootJob() {
        Project project = createProject(workspace);

        Job rootJob = saveJob(createWorkflowId(project), null);
        TaskExecution rootTaskExecution = saveTaskExecution(rootJob, null);
        Job childJob = saveJob(createWorkflowId(project), rootTaskExecution.getId());
        TaskExecution childTaskExecution = saveTaskExecution(childJob, null);
        Job grandchildJob = saveJob(createWorkflowId(project), childTaskExecution.getId());
        ProjectDeployment projectDeployment = createProjectDeployment(project, "root");

        principalJobService.create(getId(rootJob), getId(projectDeployment), PlatformType.AUTOMATION);

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(getId(grandchildJob), true);

        assertThat(workflowExecutionDTO.projectDeployment())
            .extracting(ProjectDeployment::getId)
            .isEqualTo(projectDeployment.getId());
    }

    @Test
    void testGetWorkflowExecutionOmitsTheRootDeploymentOfASubflowFromAnotherProject() {
        Project rootProject = createProject(workspace);
        Project subflowProject = createProject(workspace);

        Job rootJob = saveJob(createWorkflowId(rootProject), null);
        TaskExecution parentTaskExecution = saveTaskExecution(rootJob, null);
        Job childJob = saveJob(createWorkflowId(subflowProject), parentTaskExecution.getId());
        ProjectDeployment rootProjectDeployment = createProjectDeployment(rootProject, "root");

        principalJobService.create(getId(rootJob), getId(rootProjectDeployment), PlatformType.AUTOMATION);

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(getId(childJob), true);

        assertThat(workflowExecutionDTO.project())
            .extracting(Project::getId)
            .isEqualTo(subflowProject.getId());
        assertThat(workflowExecutionDTO.projectDeployment()).isNull();
    }

    @Test
    void testGetWorkflowExecutionFailsOnACyclicSubflowJobChain() {
        String workflowId = createWorkflowId(createProject(workspace));

        Job childJob = saveJob(workflowId, null);
        Job parentJob = saveJob(workflowId, null);

        linkInCycle(childJob, parentJob);

        assertThatThrownBy(() -> facade.getWorkflowExecution(getId(childJob), true))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionFailsOnACyclicSubflowJobChain() {
        String workflowId = createWorkflowId(createProject(workspace));

        Job childJob = saveJob(workflowId, null);
        Job parentJob = saveJob(workflowId, null);
        Job unrelatedJob = saveJob(workflowId, null);

        linkInCycle(childJob, parentJob);

        TaskExecution taskExecution = saveTaskExecution(childJob, null);

        assertThatThrownBy(
            () -> facade.getWorkflowExecutionTaskExecution(getId(unrelatedJob), getId(taskExecution)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testGetWorkflowExecutionLoadsSubflowWhoseParentTaskExecutionIsMissing() {
        Project project = createProject(workspace);

        Job rootJob = saveJob(createWorkflowId(project), null);
        TaskExecution parentTaskExecution = saveTaskExecution(rootJob, null);
        Job childJob = saveJob(createWorkflowId(project), parentTaskExecution.getId());
        ProjectDeployment childProjectDeployment = createProjectDeployment(project, "child");

        principalJobService.create(getId(childJob), getId(childProjectDeployment), PlatformType.AUTOMATION);

        doReturn(Optional.empty())
            .when(taskExecutionService)
            .fetchTaskExecution(getId(parentTaskExecution));

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(getId(childJob), true);

        assertThat(workflowExecutionDTO.id()).isEqualTo(childJob.getId());
        assertThat(workflowExecutionDTO.projectDeployment())
            .extracting(ProjectDeployment::getId)
            .isEqualTo(childProjectDeployment.getId());
    }

    @Test
    void testGetWorkflowExecutionLoadsSubflowWhoseParentJobIsMissing() {
        Project project = createProject(workspace);

        Job rootJob = saveJob(createWorkflowId(project), null);
        TaskExecution parentTaskExecution = saveTaskExecution(rootJob, null);
        Job childJob = saveJob(createWorkflowId(project), parentTaskExecution.getId());
        ProjectDeployment childProjectDeployment = createProjectDeployment(project, "child");

        principalJobService.create(getId(childJob), getId(childProjectDeployment), PlatformType.AUTOMATION);

        doReturn(Optional.empty())
            .when(jobService)
            .fetchJob(getId(rootJob));

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(getId(childJob), false);

        assertThat(workflowExecutionDTO.id()).isEqualTo(childJob.getId());
        assertThat(workflowExecutionDTO.projectDeployment())
            .extracting(ProjectDeployment::getId)
            .isEqualTo(childProjectDeployment.getId());
    }

    @Test
    void testGetWorkflowExecutionLazyResolvesEachTaskTypeDefinitionOnce() {
        Job job = saveJob(createWorkflowId(createProject(workspace)), null);

        saveTaskExecution(job, null);
        saveTaskExecution(job, null);

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(getId(job), true);

        assertThat(Objects.requireNonNull(workflowExecutionDTO.job())
            .taskExecutions()).hasSize(2);

        verify(componentDefinitionService, times(1)).getComponentDefinition(anyString(), any());
    }

    @Test
    void testGetWorkflowExecutionWithoutLazyIsUnchanged() {
        Job job = saveJob(createWorkflowId(createProject(workspace)), null);
        TaskExecution parentTaskExecution = saveTaskExecution(job, null);
        TaskExecution childTaskExecution = saveTaskExecution(job, parentTaskExecution.getId());

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(getId(job));

        assertThat(workflowExecutionDTO.focusPath()).isNull();
        assertThat(workflowExecutionDTO.taskExecutionCount()).isNull();

        List<TaskExecutionDTO> taskExecutionDTOs = Objects.requireNonNull(workflowExecutionDTO.job())
            .taskExecutions();

        assertThat(taskExecutionDTOs)
            .extracting(TaskExecutionDTO::id)
            .containsExactly(parentTaskExecution.getId());

        TaskExecutionDTO parentTaskExecutionDTO = taskExecutionDTOs.getFirst();

        assertThat(parentTaskExecutionDTO.childCount()).isNull();
        assertThat(parentTaskExecutionDTO.children())
            .extracting(TaskExecutionDTO::id, TaskExecutionDTO::childCount)
            .containsExactly(tuple(childTaskExecution.getId(), null));

        verify(taskExecutionService, never()).countJobTaskExecutions(anyLong(), anyList());
        verify(taskExecutionService, never()).getLatestAttemptFailedLeafJobTaskExecutions(
            anyLong(), anyList(), anyInt(), anyInt());
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionChildrenRejectsForeignTask() {
        String workflowId = createWorkflowId(createProject(workspace));

        Job job = saveJob(workflowId, null);
        Job foreignJob = saveJob(workflowId, null);
        TaskExecution foreignTaskExecution = saveTaskExecution(foreignJob, null);

        assertThatThrownBy(
            () -> facade.getWorkflowExecutionTaskExecutionChildren(getId(job), getId(foreignTaskExecution), 0, 50))
                .isInstanceOf(IllegalArgumentException.class);

        verify(taskExecutionService, never()).getParentTaskExecutions(anyLong(), anyInt(), anyInt());
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionChildrenReportsMissingTaskAsNotFound() {
        Job job = saveJob(createWorkflowId(createProject(workspace)), null);

        assertThatThrownBy(() -> facade.getWorkflowExecutionTaskExecutionChildren(getId(job), Long.MAX_VALUE, 0, 50))
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> facade.getWorkflowExecutionTaskExecution(getId(job), Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Nested
    class GetWorkflowExecutionsIntTest {

        private String catalogWorkflowId;
        private Job catalogJob;
        private Project catalogProject;
        private Job otherWorkspaceJob;
        private ProjectDeployment otherWorkspaceProjectDeployment;
        private ProjectDeployment referenceProjectDeployment;
        private UUID referenceWorkflowUuid;

        @BeforeEach
        void beforeEach() {
            Workspace otherWorkspace = workspaceRepository.save(new Workspace("other-execution-workspace"));

            catalogProject = createProject(workspace);

            Project otherWorkspaceProject = createProject(otherWorkspace);

            referenceWorkflowUuid = UUID.randomUUID();
            catalogWorkflowId = createWorkflowId(catalogProject, 1, referenceWorkflowUuid);

            String otherWorkspaceWorkflowId = createWorkflowId(otherWorkspaceProject);

            referenceProjectDeployment = createProjectDeployment(catalogProject, "__EMBEDDED__ext__DEVELOPMENT");
            otherWorkspaceProjectDeployment = createProjectDeployment(
                otherWorkspaceProject, "__EMBEDDED__other__DEVELOPMENT");

            embeddedProjectDeploymentIds.add(getId(referenceProjectDeployment));
            embeddedProjectDeploymentIds.add(getId(otherWorkspaceProjectDeployment));

            catalogJob = saveJob(catalogWorkflowId, null);
            otherWorkspaceJob = saveJob(otherWorkspaceWorkflowId, null);

            principalJobService.create(
                getId(catalogJob), getId(referenceProjectDeployment), PlatformType.AUTOMATION);
            principalJobService.create(
                getId(otherWorkspaceJob), getId(otherWorkspaceProjectDeployment), PlatformType.AUTOMATION);
        }

        @Test
        void testEmbeddedIncludesWorkflowsOfEmbeddedDeploymentProjects() {
            Page<WorkflowExecutionDTO> workflowExecutionDTOPage = facade.getWorkflowExecutions(
                true, null, null, null, null, null, null, null, null, getId(workspace), 0);

            assertThat(workflowExecutionDTOPage.getContent())
                .extracting(WorkflowExecutionDTO::id)
                .containsExactly(catalogJob.getId());

            verify(projectDeploymentService, times(1)).getProjectDeployments(
                eq(true), any(), any(), any(), any());
        }

        @Test
        void testEmbeddedWorkflowFilterByReferenceWorkflowUuidCoversEveryCatalogVersion() {
            String catalogWorkflowV2Id = createWorkflowId(catalogProject, 2, referenceWorkflowUuid);

            Job catalogV2Job = saveJob(catalogWorkflowV2Id, null);

            principalJobService.create(
                getId(catalogV2Job), getId(otherWorkspaceProjectDeployment), PlatformType.AUTOMATION);

            Page<WorkflowExecutionDTO> workflowExecutionDTOPage = facade.getWorkflowExecutions(
                true, null, null, null, null, null, null, referenceWorkflowUuid.toString(), null, getId(workspace),
                0);

            assertThat(workflowExecutionDTOPage.getContent())
                .extracting(WorkflowExecutionDTO::id)
                .containsExactlyInAnyOrder(catalogJob.getId(), catalogV2Job.getId());
        }

        @Test
        void testNotEmbeddedDoesNotLookUpEmbeddedDeploymentProjects() {
            Page<WorkflowExecutionDTO> workflowExecutionDTOPage = facade.getWorkflowExecutions(
                false, null, null, null, null, null, null, null, null, getId(workspace), 0);

            assertThat(workflowExecutionDTOPage.getContent()).isEmpty();

            verify(projectDeploymentService, never()).getProjectDeployments(
                eq(true), any(), any(), any(), any());
        }
    }

    private Project createProject(Workspace projectWorkspace) {
        Project project = Project.builder()
            .name("project-" + UUID.randomUUID())
            .workspaceId(projectWorkspace.getId())
            .build();

        return projectService.create(project);
    }

    private ProjectDeployment createProjectDeployment(Project project, String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        return projectDeploymentService.create(projectDeployment);
    }

    private String createWorkflowId(Project project) {
        return createWorkflowId(project, 1, UUID.randomUUID());
    }

    private String createWorkflowId(Project project, int projectVersion, UUID workflowUuid) {
        Workflow workflow = workflowService.create(WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        String workflowId = Objects.requireNonNull(workflow.getId());

        projectWorkflowRepository.save(
            new ProjectWorkflow(getId(project), projectVersion, workflowId, workflowUuid));

        return workflowId;
    }

    private ProjectWorkflowExecutionFacadeImpl getFacadeTarget() {
        return AopTestUtils.getTargetObject(facade);
    }

    private static long getId(Job job) {
        return Objects.requireNonNull(job.getId());
    }

    private static long getId(Project project) {
        return Objects.requireNonNull(project.getId());
    }

    private static long getId(ProjectDeployment projectDeployment) {
        return Objects.requireNonNull(projectDeployment.getId());
    }

    private static long getId(TaskExecution taskExecution) {
        return Objects.requireNonNull(taskExecution.getId());
    }

    private static long getId(Workspace workspace) {
        return Objects.requireNonNull(workspace.getId());
    }

    private void linkInCycle(Job childJob, Job parentJob) {
        TaskExecution childTaskExecution = saveTaskExecution(childJob, null);
        TaskExecution parentTaskExecution = saveTaskExecution(parentJob, null);

        childJob.setParentTaskExecutionId(parentTaskExecution.getId());
        parentJob.setParentTaskExecutionId(childTaskExecution.getId());

        jobRepository.save(childJob);
        jobRepository.save(parentJob);
    }

    private Job saveJob(String workflowId, Long parentTaskExecutionId) {
        Job job = new Job();

        job.setParentTaskExecutionId(parentTaskExecutionId);
        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId(workflowId);

        return jobRepository.save(job);
    }

    private TaskExecution saveTaskExecution(Job job, Long parentId) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(job.getId())
            .parentId(parentId)
            .status(TaskExecution.Status.COMPLETED)
            .workflowTask(
                new WorkflowTask(
                    Map.of("name", "task", "type", "myComponent/v1/myAction", "parameters", Map.of("evaluated", true))))
            .build();

        return taskExecutionRepository.save(taskExecution);
    }

    private TaskExecution saveTaskExecutionWithData(Job job, Long parentId) {
        TaskExecution taskExecution = saveTaskExecution(job, parentId);

        long taskExecutionId = getId(taskExecution);

        contextService.push(
            taskExecutionId, Context.Classname.TASK_EXECUTION,
            taskFileStorage.storeContextValue(
                taskExecutionId, Context.Classname.TASK_EXECUTION, Map.of("context", true)));

        taskExecution.setOutput(taskFileStorage.storeTaskExecutionOutput(getId(job), taskExecutionId, "output-value"));

        return taskExecutionRepository.save(taskExecution);
    }

    static class EmbeddedProjectDeploymentIds {

        private final Set<Long> projectDeploymentIds = ConcurrentHashMap.newKeySet();

        void add(long projectDeploymentId) {
            projectDeploymentIds.add(projectDeploymentId);
        }

        void clear() {
            projectDeploymentIds.clear();
        }

        String getConditionTemplate() {
            if (projectDeploymentIds.isEmpty()) {
                return "%1$s = 0";
            }

            return "%1$s IN (" + projectDeploymentIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")) + ")";
        }
    }

    @Configuration
    @EnableAutoConfiguration
    @Import({
        CacheConfiguration.class, ContextServiceImpl.class, EnvironmentServiceImpl.class, JacksonConfiguration.class,
        JobServiceImpl.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class,
        PrincipalJobServiceImpl.class, ProjectCallback.class, ProjectDeploymentServiceImpl.class, ProjectServiceImpl.class,
        ProjectWorkflowCallback.class,
        ProjectWorkflowExecutionFacadeImpl.class, ProjectWorkflowServiceImpl.class,
        TaskExecutionServiceImpl.class, TriggerExecutionServiceImpl.class, WorkflowExecutionRowRepositoryImpl.class,
        WorkflowExecutionRowServiceImpl.class, WorkflowServiceImpl.class
    })
    static class ProjectWorkflowExecutionFacadeIntTestConfiguration {

        @Bean
        EmbeddedProjectDeploymentIds embeddedProjectDeploymentIds() {
            return new EmbeddedProjectDeploymentIds();
        }

        @Bean
        ProjectDeploymentOwnerContributor embeddedProjectDeploymentOwnerContributor(
            EmbeddedProjectDeploymentIds embeddedProjectDeploymentIds) {

            return ProjectDeploymentOwnerContributor.of(
                ProjectDeploymentOwner.EMBEDDED, embeddedProjectDeploymentIds::getConditionTemplate);
        }

        @Bean
        Evaluator evaluator() {
            return SpelEvaluator.create();
        }

        @Bean
        TaskFileStorage taskFileStorage() {
            return new TaskFileStorageImpl(new Base64FileStorageService());
        }

        @Configuration
        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class ProjectWorkflowExecutionFacadeIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            ProjectWorkflowExecutionFacadeIntTestJdbcConfiguration(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return List.of(
                    new ExecutionErrorToStringConverter(objectMapper), new FileEntryToStringConverter(objectMapper),
                    new JobResumeIdToStringConverter(), new MapWrapperToStringConverter(objectMapper),
                    new StringToExecutionErrorConverter(objectMapper), new StringToFileEntryConverter(objectMapper),
                    new StringToJobResumeIdConverter(), new StringToMapWrapperConverter(objectMapper),
                    new StringToTaskStateValueConverter(objectMapper),
                    new StringToTriggerStateValueConverter(objectMapper), new StringToWebhooksConverter(objectMapper),
                    new StringToWorkflowExecutionIdConverter(), new StringToWorkflowTaskConverter(objectMapper),
                    new StringToWorkflowTriggerConverter(objectMapper), new TaskStateValueToStringConverter(objectMapper),
                    new TriggerStateValueToStringConverter(objectMapper), new WebhooksToStringConverter(objectMapper),
                    new WorkflowExecutionIdToStringConverter(), new WorkflowTaskToStringConverter(objectMapper),
                    new WorkflowTriggerToStringConverter(objectMapper));
            }
        }
    }
}
