/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import com.bytechef.platform.workflow.execution.dto.FocusPathElementDTO;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionChildrenPageDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationWorkflowExecutionFacadeTest {

    private ContextService contextService;
    private EnvironmentService environmentService;
    private Evaluator evaluator;
    private IntegrationWorkflowExecutionFacadeImpl facade;
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private IntegrationInstanceService integrationInstanceService;
    private IntegrationService integrationService;
    private IntegrationWorkflowFacade integrationWorkflowFacade;
    private IntegrationWorkflowService integrationWorkflowService;
    private JobService jobService;
    private PrincipalJobService principalJobService;
    private TaskExecution taskExecution;
    private TaskExecutionService taskExecutionService;
    private TaskFileStorage taskFileStorage;
    private WorkflowService workflowService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);

        contextService = mock(ContextService.class);
        environmentService = mock(EnvironmentService.class);
        evaluator = mock(Evaluator.class);
        integrationInstanceConfigurationService = mock(IntegrationInstanceConfigurationService.class);
        integrationInstanceService = mock(IntegrationInstanceService.class);
        integrationService = mock(IntegrationService.class);
        integrationWorkflowFacade = mock(IntegrationWorkflowFacade.class);
        integrationWorkflowService = mock(IntegrationWorkflowService.class);
        jobService = mock(JobService.class);
        principalJobService = mock(PrincipalJobService.class);
        taskExecutionService = mock(TaskExecutionService.class);
        taskFileStorage = mock(TaskFileStorage.class);
        workflowService = mock(WorkflowService.class);

        facade = new IntegrationWorkflowExecutionFacadeImpl(
            componentDefinitionService, contextService, environmentService, evaluator,
            principalJobService,
            integrationInstanceConfigurationService, integrationInstanceService,
            integrationService, integrationWorkflowFacade, integrationWorkflowService, jobService,
            mock(TaskDispatcherDefinitionService.class), taskExecutionService,
            taskFileStorage, mock(TriggerExecutionService.class), mock(TriggerFileStorage.class), workflowService);

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

        WorkflowTask workflowTask = mock(WorkflowTask.class);

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
    void testGetWorkflowExecutionsIncludesJobsOfPublishedIntegrationVersion() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setId(1051L);

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(1050L);
        integrationInstance.setIntegrationInstanceConfigurationId(1051L);

        Integration integration = mock(Integration.class);

        when(integration.getId()).thenReturn(1053L);

        Job job = new Job(31L);

        job.setWorkflowId("published-workflow");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn("published-workflow");

        List<IntegrationWorkflowDTO> integrationWorkflowDTOs = List.of(
            integrationWorkflowDTO("published-workflow"), integrationWorkflowDTO("draft-workflow"));

        List<IntegrationWorkflow> integrationWorkflows = List.of(
            integrationWorkflow(1053L, "published-workflow"), integrationWorkflow(1053L, "draft-workflow"));

        when(environmentService.getEnvironment(0L))
            .thenReturn(Environment.DEVELOPMENT);
        when(integrationWorkflowFacade.getIntegrationWorkflows())
            .thenReturn(integrationWorkflowDTOs);
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
            Environment.DEVELOPMENT, null, null))
                .thenReturn(List.of(integrationInstanceConfiguration));
        when(integrationInstanceService.getIntegrationInstanceConfigurationIntegrationInstances(List.of(1051L)))
            .thenReturn(List.of(integrationInstance));
        when(principalJobService.getJobIds(
            null, null, null, List.of(1050L), PlatformType.EMBEDDED, List.of("published-workflow", "draft-workflow"),
            true, 0))
                .thenReturn(new PageImpl<>(List.of(31L)));
        when(jobService.getJobs(List.of(31L)))
            .thenReturn(List.of(job));
        when(integrationService.getIntegrations())
            .thenReturn(List.of(integration));
        when(integrationWorkflowService.getIntegrationWorkflows(List.of(1053L)))
            .thenReturn(integrationWorkflows);
        when(workflowService.getWorkflows(List.of("published-workflow")))
            .thenReturn(List.of(workflow));
        when(principalJobService.getPrincipalJobs(List.of(31L), PlatformType.EMBEDDED))
            .thenReturn(List.of(new PrincipalJob(1050L, 31L, PlatformType.EMBEDDED)));
        when(integrationInstanceService.getIntegrationInstances(List.of(1050L)))
            .thenReturn(List.of(integrationInstance));
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(1051L)))
            .thenReturn(List.of(integrationInstanceConfiguration));

        Page<WorkflowExecutionDTO> workflowExecutionPage = facade.getWorkflowExecutions(
            0L, null, null, null, null, null, null, null, 0);

        assertThat(workflowExecutionPage.getContent())
            .singleElement()
            .satisfies(workflowExecutionDTO -> {
                assertThat(workflowExecutionDTO.integration()).isSameAs(integration);
                assertThat(workflowExecutionDTO.integrationInstance()).isSameAs(integrationInstance);
                assertThat(workflowExecutionDTO.integrationInstanceConfiguration())
                    .isSameAs(integrationInstanceConfiguration);
            });
    }

    @Test
    void testGetWorkflowExecutionsFiltersByInstancesOfTheConfiguration() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(1050L);
        integrationInstance.setIntegrationInstanceConfigurationId(1051L);

        List<IntegrationWorkflowDTO> integrationWorkflowDTOs = List.of(integrationWorkflowDTO("published-workflow"));

        when(integrationWorkflowFacade.getIntegrationWorkflows())
            .thenReturn(integrationWorkflowDTOs);
        when(integrationInstanceService.getIntegrationInstanceConfigurationIntegrationInstances(List.of(1051L)))
            .thenReturn(List.of(integrationInstance));
        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), eq(true), eq(0)))
            .thenReturn(Page.empty());

        facade.getWorkflowExecutions(null, null, null, null, null, 1051L, null, null, 0);

        verify(principalJobService).getJobIds(
            null, null, null, List.of(1050L), PlatformType.EMBEDDED, List.of("published-workflow"), true, 0);
    }

    @Test
    void testGetWorkflowExecutionsFiltersByInstancesOfTheConnectedUser() {
        IntegrationInstance connectedUserIntegrationInstance = new IntegrationInstance();

        connectedUserIntegrationInstance.setConnectedUserId(7L);
        connectedUserIntegrationInstance.setId(1050L);
        connectedUserIntegrationInstance.setIntegrationInstanceConfigurationId(1051L);

        IntegrationInstance otherConnectedUserIntegrationInstance = new IntegrationInstance();

        otherConnectedUserIntegrationInstance.setConnectedUserId(8L);
        otherConnectedUserIntegrationInstance.setId(1060L);
        otherConnectedUserIntegrationInstance.setIntegrationInstanceConfigurationId(1051L);

        List<IntegrationWorkflowDTO> integrationWorkflowDTOs = List.of(integrationWorkflowDTO("published-workflow"));

        when(integrationWorkflowFacade.getIntegrationWorkflows())
            .thenReturn(integrationWorkflowDTOs);
        when(integrationInstanceService.getIntegrationInstanceConfigurationIntegrationInstances(List.of(1051L)))
            .thenReturn(List.of(connectedUserIntegrationInstance, otherConnectedUserIntegrationInstance));
        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), eq(true), eq(0)))
            .thenReturn(Page.empty());

        facade.getWorkflowExecutions(null, null, null, null, null, 1051L, 7L, null, 0);

        verify(principalJobService).getJobIds(
            null, null, null, List.of(1050L), PlatformType.EMBEDDED, List.of("published-workflow"), true, 0);
    }

    @Test
    void testGetWorkflowExecutionsReturnsEmptyPageWhenTheConnectedUserHasNoInstances() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setId(1051L);

        IntegrationInstance otherConnectedUserIntegrationInstance = new IntegrationInstance();

        otherConnectedUserIntegrationInstance.setConnectedUserId(8L);
        otherConnectedUserIntegrationInstance.setId(1060L);
        otherConnectedUserIntegrationInstance.setIntegrationInstanceConfigurationId(1051L);

        List<IntegrationWorkflowDTO> integrationWorkflowDTOs = List.of(integrationWorkflowDTO("published-workflow"));

        when(integrationWorkflowFacade.getIntegrationWorkflows())
            .thenReturn(integrationWorkflowDTOs);
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(null, null, null))
            .thenReturn(List.of(integrationInstanceConfiguration));
        when(integrationInstanceService.getIntegrationInstanceConfigurationIntegrationInstances(List.of(1051L)))
            .thenReturn(List.of(otherConnectedUserIntegrationInstance));

        Page<WorkflowExecutionDTO> workflowExecutionPage = facade.getWorkflowExecutions(
            null, null, null, null, null, null, 7L, null, 0);

        assertThat(workflowExecutionPage.getContent()).isEmpty();

        verify(principalJobService, never()).getJobIds(any(), any(), any(), any(), any(), any(), eq(true), eq(0));
    }

    @Test
    void testGetWorkflowExecutionsExpandsWorkflowFilterToEveryIntegrationVersion() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setId(1051L);

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(1050L);
        integrationInstance.setIntegrationInstanceConfigurationId(1051L);

        IntegrationWorkflow draftIntegrationWorkflow = integrationWorkflow(1053L, "draft-workflow");
        IntegrationWorkflow publishedIntegrationWorkflow = integrationWorkflow(1053L, "published-workflow");
        IntegrationWorkflow otherIntegrationWorkflow = integrationWorkflow(1053L, "other-workflow");

        when(draftIntegrationWorkflow.getUuidAsString()).thenReturn("uuid-1");
        when(publishedIntegrationWorkflow.getUuidAsString()).thenReturn("uuid-1");
        when(otherIntegrationWorkflow.getUuidAsString()).thenReturn("uuid-2");

        when(integrationWorkflowService.getIntegrationWorkflows())
            .thenReturn(List.of(publishedIntegrationWorkflow, draftIntegrationWorkflow, otherIntegrationWorkflow));
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(null, null, null))
            .thenReturn(List.of(integrationInstanceConfiguration));
        when(integrationInstanceService.getIntegrationInstanceConfigurationIntegrationInstances(List.of(1051L)))
            .thenReturn(List.of(integrationInstance));
        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), eq(true), eq(0)))
            .thenReturn(Page.empty());

        facade.getWorkflowExecutions(null, null, null, null, null, null, null, "draft-workflow", 0);

        verify(principalJobService).getJobIds(
            null, null, null, List.of(1050L), PlatformType.EMBEDDED, List.of("published-workflow", "draft-workflow"),
            true, 0);
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionLoadsTaskData() {
        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.of(taskExecution));
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
    void testGetWorkflowExecutionTaskExecutionAcceptsTaskInDescendantJob() {
        Job childJob = new Job(10L);

        childJob.setParentTaskExecutionId(99L);

        TaskExecution parentTaskExecution = TaskExecution.builder()
            .id(99L)
            .jobId(5L)
            .build();

        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.of(taskExecution));
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
    void testGetWorkflowExecutionTaskExecutionRejectsTaskFromAnotherWorkflowExecution() {
        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.of(taskExecution));
        when(jobService.getJob(10L))
            .thenReturn(new Job(10L));

        assertThatThrownBy(() -> facade.getWorkflowExecutionTaskExecution(999L, 1L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionReportsMissingTaskExecution() {
        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.getWorkflowExecutionTaskExecution(10L, 1L))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionAttachesShallowChildJob() {
        Job childJob = new Job(20L);

        childJob.setInputs(Map.of("input", "value"));
        childJob.setLabel("Subflow");
        childJob.setParentTaskExecutionId(1L);
        childJob.setStatus(Job.Status.COMPLETED);

        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.of(taskExecution));
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
    void testGetWorkflowExecutionTaskExecutionChildrenAcceptsTaskInDescendantJob() {
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
    void testGetWorkflowExecutionTaskExecutionChildrenRejectsTaskFromAnotherWorkflowExecution() {
        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.of(taskExecution));
        when(jobService.getJob(10L))
            .thenReturn(new Job(10L));

        assertThatThrownBy(() -> facade.getWorkflowExecutionTaskExecutionChildren(999L, 1L, 0, 50))
            .isInstanceOf(IllegalArgumentException.class);

        verify(taskExecutionService, never()).getParentTaskExecutions(anyLong(), anyInt(), anyInt());
    }

    @Test
    void testGetWorkflowExecutionLazyReturnsTopLevelTaskExecutionsWithFocusPathAndCount() {
        Job job = new Job(10L);

        job.setWorkflowId("workflow");

        IntegrationInstance integrationInstance = stubIntegrationInstance(10L, 2L, "workflow", 2L);

        when(jobService.getJob(10L))
            .thenReturn(job);
        when(taskExecutionService.getJobTopLevelTaskExecutions(10L))
            .thenReturn(List.of(taskExecution));
        when(taskExecutionService.fetchLastJobTaskExecution(10L))
            .thenReturn(Optional.of(taskExecution));
        when(taskExecutionService.countJobTaskExecutions(10L, List.of()))
            .thenReturn(3L);

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(10L, true);

        assertThat(workflowExecutionDTO.integrationInstance()).isSameAs(integrationInstance);
        assertThat(workflowExecutionDTO.focusPath()).containsExactly(new FocusPathElementDTO(1L, null, null));
        assertThat(workflowExecutionDTO.taskExecutionCount()).isEqualTo(3L);
        assertThat(workflowExecutionDTO.job()
            .taskExecutions())
                .extracting(TaskExecutionDTO::id, TaskExecutionDTO::childCount)
                .containsExactly(tuple(1L, 0));

        verify(taskExecutionService, never()).getJobTaskExecutions(anyLong());
    }

    @Test
    void testGetWorkflowExecutionIsNotLazyByDefault() {
        Job job = new Job(10L);

        job.setWorkflowId("workflow");

        stubIntegrationInstance(10L, 2L, "workflow", 2L);

        when(jobService.getJob(10L))
            .thenReturn(job);

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(10L);

        assertThat(workflowExecutionDTO.focusPath()).isNull();
        assertThat(workflowExecutionDTO.taskExecutionCount()).isNull();

        verify(taskExecutionService).getJobTaskExecutions(10L);
        verify(taskExecutionService, never()).getJobTopLevelTaskExecutions(anyLong());
    }

    @Test
    void testGetWorkflowExecutionResolvesSubflowIntegrationInstanceThroughTheRootJob() {
        Job childJob = new Job(30L);

        childJob.setParentTaskExecutionId(99L);
        childJob.setWorkflowId("subflow-workflow");

        IntegrationInstance integrationInstance = stubIntegrationInstance(20L, 2L, "subflow-workflow", 2L);

        when(jobService.getJob(30L))
            .thenReturn(childJob);
        when(taskExecutionService.fetchTaskExecution(99L))
            .thenReturn(
                Optional.of(
                    TaskExecution.builder()
                        .id(99L)
                        .jobId(20L)
                        .build()));
        when(jobService.fetchJob(20L))
            .thenReturn(Optional.of(new Job(20L)));

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(30L, true);

        assertThat(workflowExecutionDTO.integrationInstance()).isSameAs(integrationInstance);
        assertThat(workflowExecutionDTO.integrationInstanceConfiguration()).isNotNull();
        assertThat(workflowExecutionDTO.triggerExecution()).isNull();

        verify(principalJobService, never()).getJobPrincipalId(30L, PlatformType.EMBEDDED);
    }

    @Test
    void testGetWorkflowExecutionOmitsTheRootInstanceOfASubflowFromAnotherIntegration() {
        Job childJob = new Job(30L);

        childJob.setParentTaskExecutionId(99L);
        childJob.setWorkflowId("subflow-workflow");

        stubIntegrationInstance(20L, 2L, "subflow-workflow", 4L);

        when(jobService.getJob(30L))
            .thenReturn(childJob);
        when(taskExecutionService.fetchTaskExecution(99L))
            .thenReturn(
                Optional.of(
                    TaskExecution.builder()
                        .id(99L)
                        .jobId(20L)
                        .build()));
        when(jobService.fetchJob(20L))
            .thenReturn(Optional.of(new Job(20L)));

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(30L, true);

        assertThat(workflowExecutionDTO.integration()
            .getId()).isEqualTo(4L);
        assertThat(workflowExecutionDTO.integrationInstance()).isNull();
        assertThat(workflowExecutionDTO.integrationInstanceConfiguration()).isNull();
    }

    @Test
    void testGetWorkflowExecutionFailsOnACyclicSubflowJobChain() {
        Job childJob = new Job(30L);

        childJob.setParentTaskExecutionId(99L);
        childJob.setWorkflowId("subflow-workflow");

        Job parentJob = new Job(31L);

        parentJob.setParentTaskExecutionId(98L);

        when(jobService.getJob(30L))
            .thenReturn(childJob);
        when(taskExecutionService.fetchTaskExecution(99L))
            .thenReturn(
                Optional.of(
                    TaskExecution.builder()
                        .id(99L)
                        .jobId(31L)
                        .build()));
        when(jobService.fetchJob(31L))
            .thenReturn(Optional.of(parentJob));
        when(taskExecutionService.fetchTaskExecution(98L))
            .thenReturn(
                Optional.of(
                    TaskExecution.builder()
                        .id(98L)
                        .jobId(30L)
                        .build()));
        when(jobService.fetchJob(30L))
            .thenReturn(Optional.of(childJob));

        assertThatThrownBy(() -> facade.getWorkflowExecution(30L, true))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testGetWorkflowExecutionTaskExecutionFailsOnACyclicSubflowJobChain() {
        Job childJob = new Job(30L);

        childJob.setParentTaskExecutionId(99L);

        Job parentJob = new Job(31L);

        parentJob.setParentTaskExecutionId(98L);

        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(
                Optional.of(
                    TaskExecution.builder()
                        .id(1L)
                        .jobId(30L)
                        .build()));
        when(jobService.getJob(30L))
            .thenReturn(childJob);
        when(taskExecutionService.getTaskExecution(99L))
            .thenReturn(
                TaskExecution.builder()
                    .id(99L)
                    .jobId(31L)
                    .build());
        when(jobService.getJob(31L))
            .thenReturn(parentJob);
        when(taskExecutionService.getTaskExecution(98L))
            .thenReturn(
                TaskExecution.builder()
                    .id(98L)
                    .jobId(30L)
                    .build());

        assertThatThrownBy(() -> facade.getWorkflowExecutionTaskExecution(20L, 1L))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testToTaskExecutionDTODoesNotLoadTaskDataForList() {
        TaskExecutionDTO taskExecutionDTO = facade.toTaskExecutionDTO(taskExecution, null, false);

        assertThat(taskExecutionDTO.input()).isNull();
        assertThat(taskExecutionDTO.output()).isNull();

        verify(contextService, never()).peek(anyLong(), any());
        verify(taskFileStorage, never()).readContextValue(any());
        verify(taskFileStorage, never()).readTaskExecutionOutput(any());
        verifyNoInteractions(evaluator);
    }

    private IntegrationInstance stubIntegrationInstance(
        long rootJobId, long rootIntegrationId, String workflowId, long workflowIntegrationId) {

        Integration integration = new Integration();

        integration.setId(workflowIntegrationId);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setId(5L);
        integrationInstanceConfiguration.setIntegrationId(rootIntegrationId);

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(7L);
        integrationInstance.setIntegrationInstanceConfigurationId(5L);

        when(integrationService.getWorkflowIntegration(workflowId))
            .thenReturn(integration);
        when(principalJobService.getJobPrincipalId(rootJobId, PlatformType.EMBEDDED))
            .thenReturn(7L);
        when(integrationInstanceService.getIntegrationInstance(7L))
            .thenReturn(integrationInstance);
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(5L))
            .thenReturn(integrationInstanceConfiguration);

        return integrationInstance;
    }

    private static IntegrationWorkflow integrationWorkflow(long integrationId, String workflowId) {
        IntegrationWorkflow integrationWorkflow = mock(IntegrationWorkflow.class);

        lenient()
            .when(integrationWorkflow.getIntegrationId())
            .thenReturn(integrationId);
        lenient()
            .when(integrationWorkflow.getWorkflowId())
            .thenReturn(workflowId);

        return integrationWorkflow;
    }

    private static IntegrationWorkflowDTO integrationWorkflowDTO(String workflowId) {
        IntegrationWorkflowDTO integrationWorkflowDTO = mock(IntegrationWorkflowDTO.class);

        when(integrationWorkflowDTO.getId()).thenReturn(workflowId);

        return integrationWorkflowDTO;
    }
}
