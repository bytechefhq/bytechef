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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.dto.FocusPathElementDTO;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionChildrenPageDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.WorkflowExecutionRowService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public class ProjectWorkflowExecutionFacadeTest {

    private ComponentDefinitionService componentDefinitionService;
    private ContextService contextService;
    private Evaluator evaluator;
    private ProjectWorkflowExecutionFacadeImpl facade;
    private JobService jobService;
    private ProjectDeploymentService projectDeploymentService;
    private ProjectFacade projectFacade;
    private ProjectWorkflowService projectWorkflowService;
    private TaskExecution taskExecution;
    private TaskExecutionService taskExecutionService;
    private TaskFileStorage taskFileStorage;
    private WorkflowExecutionRowService workflowExecutionRowService;
    private WorkflowTask workflowTask;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        componentDefinitionService = mock(ComponentDefinitionService.class);

        contextService = mock(ContextService.class);
        evaluator = mock(Evaluator.class);
        jobService = mock(JobService.class);
        projectDeploymentService = mock(ProjectDeploymentService.class);
        projectFacade = mock(ProjectFacade.class);
        projectWorkflowService = mock(ProjectWorkflowService.class);
        taskExecutionService = mock(TaskExecutionService.class);
        taskFileStorage = mock(TaskFileStorage.class);
        workflowExecutionRowService = mock(WorkflowExecutionRowService.class);

        facade = new ProjectWorkflowExecutionFacadeImpl(
            componentDefinitionService,
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserProjectDeploymentResolver.class),
            contextService, evaluator, mock(EnvironmentService.class),
            workflowExecutionRowService, jobService, mock(PrincipalJobService.class), projectFacade,
            projectDeploymentService, mock(ProjectService.class), projectWorkflowService,
            mock(TaskDispatcherDefinitionService.class), taskExecutionService, taskFileStorage,
            mock(TriggerExecutionService.class), mock(TriggerFileStorage.class), mock(WorkflowService.class));

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        lenient()
            .when(componentDefinition.getTitle())
            .thenReturn("Title");
        lenient()
            .when(componentDefinition.getIcon())
            .thenReturn("icon");
        lenient()
            .when(componentDefinitionService.hasComponentDefinition(anyString(), any()))
            .thenReturn(true);
        lenient()
            .when(componentDefinitionService.getComponentDefinition(anyString(), any()))
            .thenReturn(componentDefinition);

        workflowTask = mock(WorkflowTask.class);

        lenient()
            .when(workflowTask.getType())
            .thenReturn("myComponent/v1/myAction");
        lenient()
            .when(workflowTask.evaluateParameters(any(), any()))
            .thenReturn((Map) Map.of("evaluated", true));

        taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(10L)
            .output(mock(FileEntry.class))
            .workflowTask(workflowTask)
            .build();
    }

    @Test
    public void testToTaskExecutionDTODoesNotLoadTaskDataForList() {
        TaskExecutionDTO taskExecutionDTO = facade.toTaskExecutionDTO(taskExecution, null, false);

        assertThat(taskExecutionDTO.input())
            .isNull();
        assertThat(taskExecutionDTO.output())
            .isNull();

        verify(contextService, never()).peek(anyLong(), any());
        verify(taskFileStorage, never()).readContextValue(any());
        verify(taskFileStorage, never()).readTaskExecutionOutput(any());
        verifyNoInteractions(evaluator);
    }

    @Test
    public void testToTaskExecutionDTOLoadsTaskDataForDetail() {
        doReturn(Map.of("context", true))
            .when(taskFileStorage)
            .readContextValue(any());
        when(taskFileStorage.readTaskExecutionOutput(any()))
            .thenReturn("output-value");

        TaskExecutionDTO taskExecutionDTO = facade.toTaskExecutionDTO(taskExecution, null, true);

        assertThat(taskExecutionDTO.input())
            .isEqualTo(Map.of("evaluated", true));
        assertThat(taskExecutionDTO.output())
            .isEqualTo("output-value");

        Long taskExecutionId = Objects.requireNonNull(taskExecution.getId(), "taskExecution.id");

        verify(contextService).peek(taskExecutionId, Context.Classname.TASK_EXECUTION);
        verify(taskFileStorage).readTaskExecutionOutput(any());
    }

    @Test
    public void testGetWorkflowExecutionTaskExecutionLoadsTaskData() {
        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(taskExecution);
        doReturn(Map.of("context", true))
            .when(taskFileStorage)
            .readContextValue(any());
        when(taskFileStorage.readTaskExecutionOutput(any()))
            .thenReturn("output-value");

        TaskExecutionDTO taskExecutionDTO = facade.getWorkflowExecutionTaskExecution(10L, 1L);

        assertThat(taskExecutionDTO.input())
            .isEqualTo(Map.of("evaluated", true));
        assertThat(taskExecutionDTO.output())
            .isEqualTo("output-value");
    }

    @Test
    public void testGetWorkflowExecutionTaskExecutionAttachesShallowChildJob() {
        Job childJob = new Job(20L);

        childJob.setInputs(Map.of("input", "value"));
        childJob.setLabel("Subflow");
        childJob.setParentTaskExecutionId(1L);
        childJob.setStatus(Job.Status.COMPLETED);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(taskExecution);
        when(jobService.getChildJobs(List.of(1L)))
            .thenReturn(List.of(childJob));

        TaskExecutionDTO taskExecutionDTO = facade.getWorkflowExecutionTaskExecution(10L, 1L);

        JobDTO childJobDTO = Objects.requireNonNull(taskExecutionDTO.childJob());

        assertThat(childJobDTO.id()).isEqualTo(20L);
        assertThat(childJobDTO.label()).isEqualTo("Subflow");
        assertThat(childJobDTO.status()).isEqualTo(Job.Status.COMPLETED);
        assertThat(childJobDTO.inputs()).isEmpty();
        assertThat(childJobDTO.taskExecutions()).isEmpty();

        verify(jobService, never()).getChildJobIds(anyLong());
    }

    @Test
    public void testGetWorkflowExecutionTaskExecutionWithoutChildJob() {
        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(taskExecution);
        when(jobService.getChildJobs(List.of(1L)))
            .thenReturn(List.of());

        TaskExecutionDTO taskExecutionDTO = facade.getWorkflowExecutionTaskExecution(10L, 1L);

        assertThat(taskExecutionDTO.childJob()).isNull();
    }

    @Test
    public void testGetWorkflowExecutionTaskExecutionAcceptsTaskInDescendantJob() {
        Job childJob = new Job(10L);

        childJob.setParentTaskExecutionId(99L);

        TaskExecution parentTaskExecution = TaskExecution.builder()
            .id(99L)
            .jobId(5L)
            .build();

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(taskExecution);
        when(jobService.getJob(10L))
            .thenReturn(childJob);
        when(taskExecutionService.getTaskExecution(99L))
            .thenReturn(parentTaskExecution);
        doReturn(Map.of("context", true))
            .when(taskFileStorage)
            .readContextValue(any());
        when(taskFileStorage.readTaskExecutionOutput(any()))
            .thenReturn("output-value");

        TaskExecutionDTO taskExecutionDTO = facade.getWorkflowExecutionTaskExecution(5L, 1L);

        assertThat(taskExecutionDTO.input())
            .isEqualTo(Map.of("evaluated", true));
    }

    @Test
    public void testGetWorkflowExecutionTaskExecutionChildrenAcceptsTaskInDescendantJob() {
        Job childJob = new Job(10L);

        childJob.setParentTaskExecutionId(99L);

        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.of(taskExecution));
        when(jobService.getJob(10L))
            .thenReturn(childJob);
        when(taskExecutionService.getTaskExecution(99L))
            .thenReturn(
                TaskExecution.builder()
                    .id(99L)
                    .jobId(5L)
                    .build());

        TaskExecutionChildrenPageDTO taskExecutionChildrenPageDTO = facade.getWorkflowExecutionTaskExecutionChildren(
            5L, 1L, 0, 50);

        assertThat(taskExecutionChildrenPageDTO.number()).isZero();
        assertThat(taskExecutionChildrenPageDTO.size()).isEqualTo(50);
    }

    @Test
    public void testGetWorkflowExecutionTaskExecutionRejectsTaskFromAnotherWorkflowExecution() {
        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(taskExecution);
        when(jobService.getJob(10L))
            .thenReturn(new Job(10L));

        assertThatThrownBy(() -> facade.getWorkflowExecutionTaskExecution(999L, 1L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testGetSubflowJobTaskExecutionsSkipsTaskLoadWhenNoChildJobs() {
        when(jobService.getChildJobIds(33911L))
            .thenReturn(List.of());

        List<TaskExecutionDTO> taskExecutionDTOs = facade.getSubflowJobTaskExecutions(33911L);

        assertThat(taskExecutionDTOs)
            .isEmpty();

        verify(taskExecutionService, never()).getJobTaskExecutions(anyLong());
    }

    @Test
    public void getWorkflowExecutionLazyForChildJob() {
        Job childJob = new Job(30L);

        childJob.setParentTaskExecutionId(99L);
        childJob.setWorkflowId("subflow-workflow");

        TaskExecution topLevelTaskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(30L)
            .workflowTask(workflowTask)
            .build();

        when(jobService.getJob(30L))
            .thenReturn(childJob);
        when(taskExecutionService.getJobTopLevelTaskExecutions(30L))
            .thenReturn(List.of(topLevelTaskExecution));
        when(taskExecutionService.fetchLastJobTaskExecution(30L))
            .thenReturn(Optional.of(topLevelTaskExecution));
        when(taskExecutionService.countJobTaskExecutions(30L, List.of()))
            .thenReturn(5L);

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(30L, true);

        assertThat(workflowExecutionDTO.projectDeployment()).isNull();
        assertThat(workflowExecutionDTO.triggerExecution()).isNull();
        assertThat(workflowExecutionDTO.focusPath()).containsExactly(new FocusPathElementDTO(1L, null));
        assertThat(workflowExecutionDTO.taskExecutionCount()).isEqualTo(5L);

        JobDTO jobDTO = Objects.requireNonNull(workflowExecutionDTO.job());

        assertThat(jobDTO.taskExecutions())
            .extracting(TaskExecutionDTO::id, TaskExecutionDTO::childCount)
            .containsExactly(tuple(1L, 0));

        verify(taskExecutionService, never()).getJobTaskExecutions(anyLong());
    }

    @Test
    public void getWorkflowExecutionLazyResolvesEachTaskTypeDefinitionOnce() {
        Job job = new Job(30L);

        job.setWorkflowId("workflow");

        TaskExecution firstTaskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(30L)
            .workflowTask(workflowTask)
            .build();
        TaskExecution secondTaskExecution = TaskExecution.builder()
            .id(2L)
            .jobId(30L)
            .workflowTask(workflowTask)
            .build();

        when(jobService.getJob(30L))
            .thenReturn(job);
        when(taskExecutionService.getJobTopLevelTaskExecutions(30L))
            .thenReturn(List.of(firstTaskExecution, secondTaskExecution));

        facade.getWorkflowExecution(30L, true);

        verify(componentDefinitionService, times(1)).getComponentDefinition(anyString(), any());
    }

    @Test
    public void getWorkflowExecutionWithoutLazyIsUnchanged() {
        Job job = new Job(30L);

        job.setWorkflowId("workflow");

        TaskExecution parentTaskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(30L)
            .workflowTask(workflowTask)
            .build();
        TaskExecution childTaskExecution = TaskExecution.builder()
            .id(2L)
            .jobId(30L)
            .parentId(1L)
            .workflowTask(workflowTask)
            .build();

        when(jobService.getJob(30L))
            .thenReturn(job);
        when(taskExecutionService.getJobTaskExecutions(30L))
            .thenReturn(List.of(parentTaskExecution, childTaskExecution));

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(30L);

        assertThat(workflowExecutionDTO.focusPath()).isNull();
        assertThat(workflowExecutionDTO.taskExecutionCount()).isNull();

        List<TaskExecutionDTO> taskExecutionDTOs = Objects.requireNonNull(workflowExecutionDTO.job())
            .taskExecutions();

        assertThat(taskExecutionDTOs)
            .extracting(TaskExecutionDTO::id)
            .containsExactly(1L);

        TaskExecutionDTO parentTaskExecutionDTO = taskExecutionDTOs.getFirst();

        assertThat(parentTaskExecutionDTO.childCount()).isNull();
        assertThat(parentTaskExecutionDTO.children())
            .extracting(TaskExecutionDTO::id, TaskExecutionDTO::childCount)
            .containsExactly(tuple(2L, null));

        verify(taskExecutionService, never()).countJobTaskExecutions(anyLong(), anyList());
        verify(taskExecutionService, never()).getFailedJobTaskExecutions(anyLong());
    }

    @Test
    public void getWorkflowExecutionTaskExecutionChildrenRejectsForeignTask() {
        TaskExecution foreignTaskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(20L)
            .workflowTask(workflowTask)
            .build();

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(foreignTaskExecution);
        when(jobService.getJob(20L))
            .thenReturn(new Job(20L));

        assertThatThrownBy(() -> facade.getWorkflowExecutionTaskExecutionChildren(10L, 1L, 0, 50))
            .isInstanceOf(IllegalArgumentException.class);

        verify(taskExecutionService, never()).getParentTaskExecutions(anyLong(), anyInt(), anyInt());
    }

    @Nested
    class GetWorkflowExecutionsTest {

        private static final String CATALOG_WORKFLOW_ID = "catalog-workflow";
        private static final long CATALOG_PROJECT_ID = 7L;
        private static final String OTHER_WORKSPACE_WORKFLOW_ID = "other-workspace-workflow";
        private static final long OTHER_WORKSPACE_DEPLOYMENT_ID = 6L;
        private static final long OTHER_WORKSPACE_PROJECT_ID = 8L;
        private static final long REFERENCE_DEPLOYMENT_ID = 5L;
        private static final long WORKSPACE_ID = 1L;

        @BeforeEach
        public void beforeEach() {
            ProjectDeployment referenceProjectDeployment = new ProjectDeployment();

            referenceProjectDeployment.setId(REFERENCE_DEPLOYMENT_ID);
            referenceProjectDeployment.setName("__EMBEDDED__ext__DEVELOPMENT");
            referenceProjectDeployment.setProjectId(CATALOG_PROJECT_ID);
            referenceProjectDeployment.setProjectVersion(1);

            ProjectDeployment otherWorkspaceProjectDeployment = new ProjectDeployment();

            otherWorkspaceProjectDeployment.setId(OTHER_WORKSPACE_DEPLOYMENT_ID);
            otherWorkspaceProjectDeployment.setName("__EMBEDDED__other__DEVELOPMENT");
            otherWorkspaceProjectDeployment.setProjectId(OTHER_WORKSPACE_PROJECT_ID);
            otherWorkspaceProjectDeployment.setProjectVersion(1);

            lenient()
                .when(projectFacade.getWorkspaceProjectWorkflows(WORKSPACE_ID))
                .thenReturn(List.of());
            lenient()
                .when(projectDeploymentService.getProjectDeployments(true, null, null, null, WORKSPACE_ID))
                .thenReturn(List.of(referenceProjectDeployment));
            lenient()
                .when(projectDeploymentService.getProjectDeployments(true, null, null, null, null))
                .thenReturn(List.of(referenceProjectDeployment, otherWorkspaceProjectDeployment));
            lenient()
                .when(projectWorkflowService.getProjectWorkflows(List.of(CATALOG_PROJECT_ID)))
                .thenReturn(
                    List.of(new ProjectWorkflow(CATALOG_PROJECT_ID, 1, CATALOG_WORKFLOW_ID, UUID.randomUUID())));
            lenient()
                .when(projectWorkflowService.getProjectWorkflows(
                    List.of(CATALOG_PROJECT_ID, OTHER_WORKSPACE_PROJECT_ID)))
                .thenReturn(
                    List.of(
                        new ProjectWorkflow(CATALOG_PROJECT_ID, 1, CATALOG_WORKFLOW_ID, UUID.randomUUID()),
                        new ProjectWorkflow(
                            OTHER_WORKSPACE_PROJECT_ID, 1, OTHER_WORKSPACE_WORKFLOW_ID, UUID.randomUUID())));
            lenient()
                .when(workflowExecutionRowService.getWorkflowExecutionRows(
                    any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyList(), anyInt()))
                .thenReturn(Page.empty());
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testEmbeddedIncludesWorkflowsOfEmbeddedDeploymentProjects() {
            facade.getWorkflowExecutions(true, null, null, null, null, null, null, null, null, WORKSPACE_ID, 0);

            ArgumentCaptor<List<String>> workflowIdsArgumentCaptor = ArgumentCaptor.forClass(List.class);

            verify(workflowExecutionRowService).getWorkflowExecutionRows(
                any(), any(), any(), eq(List.of(REFERENCE_DEPLOYMENT_ID)), eq(PlatformType.AUTOMATION),
                workflowIdsArgumentCaptor.capture(), anyBoolean(), anyList(), anyInt());

            assertThat(workflowIdsArgumentCaptor.getValue()).containsExactly(CATALOG_WORKFLOW_ID);

            verify(projectDeploymentService, times(1)).getProjectDeployments(
                eq(true), any(), any(), any(), any());
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testEmbeddedWorkflowFilterByReferenceWorkflowUuidCoversEveryCatalogVersion() {
            String referenceWorkflowUuid = UUID.randomUUID()
                .toString();

            when(projectWorkflowService.getWorkflowProjectWorkflows(List.of(referenceWorkflowUuid)))
                .thenReturn(List.of());
            when(projectWorkflowService.getWorkflowUuidProjectWorkflows(referenceWorkflowUuid))
                .thenReturn(
                    List.of(
                        new ProjectWorkflow(
                            CATALOG_PROJECT_ID, 1, CATALOG_WORKFLOW_ID, UUID.fromString(referenceWorkflowUuid)),
                        new ProjectWorkflow(
                            CATALOG_PROJECT_ID, 2, "catalog-workflow-v2", UUID.fromString(referenceWorkflowUuid))));

            facade.getWorkflowExecutions(
                true, null, null, null, null, null, null, referenceWorkflowUuid, null, WORKSPACE_ID, 0);

            ArgumentCaptor<List<String>> workflowIdsArgumentCaptor = ArgumentCaptor.forClass(List.class);

            verify(workflowExecutionRowService).getWorkflowExecutionRows(
                any(), any(), any(), eq(List.of(REFERENCE_DEPLOYMENT_ID, OTHER_WORKSPACE_DEPLOYMENT_ID)),
                eq(PlatformType.AUTOMATION), workflowIdsArgumentCaptor.capture(), anyBoolean(), anyList(), anyInt());

            assertThat(workflowIdsArgumentCaptor.getValue()).containsExactly(
                CATALOG_WORKFLOW_ID, "catalog-workflow-v2");
        }

        @Test
        public void testNotEmbeddedDoesNotLookUpEmbeddedDeploymentProjects() {
            facade.getWorkflowExecutions(false, null, null, null, null, null, null, null, null, WORKSPACE_ID, 0);

            verify(projectDeploymentService, never()).getProjectDeployments(
                eq(true), any(), any(), any(), any());
            verifyNoInteractions(workflowExecutionRowService);
        }
    }
}
