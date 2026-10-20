/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
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
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import java.util.Map;
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
    private IntegrationWorkflowService integrationWorkflowService;
    private JobService jobService;
    private PermissionService permissionService;
    private PrincipalJobService principalJobService;
    private TaskExecution taskExecution;
    private TaskExecutionService taskExecutionService;
    private TaskFileStorage taskFileStorage;
    private TriggerExecutionService triggerExecutionService;
    private TriggerFileStorage triggerFileStorage;
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
        integrationWorkflowService = mock(IntegrationWorkflowService.class);
        jobService = mock(JobService.class);
        permissionService = mock(PermissionService.class);
        principalJobService = mock(PrincipalJobService.class);
        taskExecutionService = mock(TaskExecutionService.class);
        taskFileStorage = mock(TaskFileStorage.class);
        triggerExecutionService = mock(TriggerExecutionService.class);
        triggerFileStorage = mock(TriggerFileStorage.class);
        workflowService = mock(WorkflowService.class);

        facade = new IntegrationWorkflowExecutionFacadeImpl(
            componentDefinitionService, contextService, environmentService, evaluator,
            permissionService, principalJobService,
            integrationInstanceConfigurationService, integrationInstanceService,
            integrationService, integrationWorkflowService, jobService,
            mock(TaskDispatcherDefinitionService.class), taskExecutionService,
            taskFileStorage, triggerExecutionService, triggerFileStorage, workflowService);

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
        integrationInstanceConfiguration.setEnvironment(Environment.DEVELOPMENT);

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(1050L);
        integrationInstance.setIntegrationInstanceConfigurationId(1051L);

        Integration integration = mock(Integration.class);

        when(integration.getId()).thenReturn(1053L);

        Job job = new Job(31L);

        job.setWorkflowId("published-workflow");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn("published-workflow");

        List<IntegrationWorkflow> integrationWorkflows = List.of(
            integrationWorkflow(1053L, "published-workflow"), integrationWorkflow(1053L, "draft-workflow"));

        when(environmentService.getEnvironment(0L))
            .thenReturn(Environment.DEVELOPMENT);
        when(integrationWorkflowService.getIntegrationWorkflows())
            .thenReturn(integrationWorkflows);
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
                assertThat(workflowExecutionDTO.integrationInstanceConfiguration()
                    .getId()).isEqualTo(1051L);
            });
    }

    @Test
    void testGetWorkflowExecutionsFiltersByInstancesOfTheConfiguration() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(1050L);
        integrationInstance.setIntegrationInstanceConfigurationId(1051L);

        IntegrationWorkflow integrationWorkflow = integrationWorkflow(1053L, "published-workflow");

        when(integrationWorkflowService.getIntegrationWorkflows())
            .thenReturn(List.of(integrationWorkflow));
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

        IntegrationWorkflow integrationWorkflow = integrationWorkflow(1053L, "published-workflow");

        when(integrationWorkflowService.getIntegrationWorkflows())
            .thenReturn(List.of(integrationWorkflow));
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

        IntegrationWorkflow integrationWorkflow = integrationWorkflow(1053L, "published-workflow");

        when(integrationWorkflowService.getIntegrationWorkflows())
            .thenReturn(List.of(integrationWorkflow));
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
        when(permissionService.hasResourceScope(anyLong(), anyString(), anyString()))
            .thenReturn(true);
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
    void testGetWorkflowExecutionTaskExecutionAcceptsTaskInDescendantJob() {
        Job childJob = new Job(10L);

        childJob.setParentTaskExecutionId(99L);

        TaskExecution parentTaskExecution = TaskExecution.builder()
            .id(99L)
            .jobId(5L)
            .build();

        when(permissionService.hasResourceScope(anyLong(), anyString(), anyString()))
            .thenReturn(true);
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
    void testGetWorkflowExecutionTaskExecutionRejectsTaskFromAnotherWorkflowExecution() {
        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(taskExecution);
        when(jobService.getJob(10L))
            .thenReturn(new Job(10L));

        assertThatThrownBy(() -> facade.getWorkflowExecutionTaskExecution(999L, 1L))
            .isInstanceOf(IllegalArgumentException.class);
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

    @Test
    void testAViewerSeesTheRunButNotItsInputsOutputsOrTrigger() {
        stubRun(false);

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(10L);

        assertThat(workflowExecutionDTO.job()).isNotNull();
        assertThat(workflowExecutionDTO.job()
            .id()).isEqualTo(10L);
        assertThat(workflowExecutionDTO.job()
            .inputs()).isEmpty();

        TriggerExecutionDTO triggerExecutionDTO = workflowExecutionDTO.triggerExecution();

        assertThat(triggerExecutionDTO).isNotNull();
        assertThat(triggerExecutionDTO.input()).isNull();
        assertThat(triggerExecutionDTO.output()).isNull();
        assertThat(triggerExecutionDTO.state()).isNull();
    }

    @Test
    void testAnEditorSeesRunDetails() {
        stubRun(true);

        WorkflowExecutionDTO workflowExecutionDTO = facade.getWorkflowExecution(10L);

        assertThat(workflowExecutionDTO.job()
            .inputs()).isEqualTo(Map.of("jobInput", "value"));

        TriggerExecutionDTO triggerExecutionDTO = workflowExecutionDTO.triggerExecution();

        assertThat(triggerExecutionDTO.input()).isEqualTo(Map.of("evaluated", true));
        assertThat(triggerExecutionDTO.output()).isEqualTo("trigger-output");
    }

    @Test
    void testATaskExecutionIsNotReadWithoutDetails() {
        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(taskExecution);
        when(permissionService.hasResourceScope(10L, "Job", "EXECUTION_DETAILS_VIEW"))
            .thenReturn(false);

        TaskExecutionDTO taskExecutionDTO = facade.getWorkflowExecutionTaskExecution(10L, 1L);

        assertThat(taskExecutionDTO.input()).isNull();
        assertThat(taskExecutionDTO.output()).isNull();
        assertThat(taskExecutionDTO.error()).isNull();

        verifyNoInteractions(taskFileStorage, contextService, evaluator);
    }

    @Test
    void testRunListsRedactPerConfigurationEnvironment() {
        IntegrationInstanceConfiguration developmentConfiguration = configuration(1051L, Environment.DEVELOPMENT);
        IntegrationInstanceConfiguration productionConfiguration = configuration(1061L, Environment.PRODUCTION);

        IntegrationInstance developmentInstance = instance(1050L, 1051L);
        IntegrationInstance productionInstance = instance(1060L, 1061L);
        IntegrationInstance otherProductionInstance = instance(1062L, 1061L);

        Integration integration = mock(Integration.class);

        when(integration.getId()).thenReturn(1053L);

        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn("workflow");

        IntegrationWorkflow integrationWorkflow = integrationWorkflow(1053L, "workflow");

        when(integrationWorkflowService.getIntegrationWorkflows())
            .thenReturn(List.of(integrationWorkflow));
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(null, null, null))
            .thenReturn(List.of(developmentConfiguration, productionConfiguration));
        when(integrationInstanceService.getIntegrationInstanceConfigurationIntegrationInstances(List.of(1051L, 1061L)))
            .thenReturn(List.of(developmentInstance, productionInstance, otherProductionInstance));
        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), eq(true), eq(0)))
            .thenReturn(new PageImpl<>(List.of(31L, 32L, 33L)));
        when(jobService.getJobs(List.of(31L, 32L, 33L)))
            .thenReturn(List.of(job(31L), job(32L), job(33L)));
        when(integrationService.getIntegrations())
            .thenReturn(List.of(integration));
        when(integrationWorkflowService.getIntegrationWorkflows(List.of(1053L)))
            .thenReturn(List.of(integrationWorkflow));
        when(workflowService.getWorkflows(any()))
            .thenReturn(List.of(workflow));
        when(principalJobService.getPrincipalJobs(List.of(31L, 32L, 33L), PlatformType.EMBEDDED))
            .thenReturn(
                List.of(
                    new PrincipalJob(1050L, 31L, PlatformType.EMBEDDED),
                    new PrincipalJob(1060L, 32L, PlatformType.EMBEDDED),
                    new PrincipalJob(1062L, 33L, PlatformType.EMBEDDED)));
        when(integrationInstanceService.getIntegrationInstances(any()))
            .thenReturn(List.of(developmentInstance, productionInstance, otherProductionInstance));
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(anyList()))
            .thenReturn(List.of(developmentConfiguration, productionConfiguration));
        when(permissionService.hasEmbeddedScope("EXECUTION_DETAILS_VIEW", Environment.PRODUCTION))
            .thenReturn(true);
        when(permissionService.hasEmbeddedScope("EXECUTION_DETAILS_VIEW", Environment.DEVELOPMENT))
            .thenReturn(false);

        Page<WorkflowExecutionDTO> page =
            facade.getWorkflowExecutions(null, null, null, null, null, null, null, null, 0);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent()
            .get(0)
            .job()
            .inputs()).isEmpty();
        assertThat(page.getContent()
            .get(1)
            .job()
            .inputs()).isEqualTo(Map.of("jobInput", "value"));
        assertThat(page.getContent()
            .get(2)
            .job()
            .inputs()).isEqualTo(Map.of("jobInput", "value"));

        verify(permissionService, times(1)).hasEmbeddedScope("EXECUTION_DETAILS_VIEW", Environment.PRODUCTION);
        verify(permissionService, times(1)).hasEmbeddedScope("EXECUTION_DETAILS_VIEW", Environment.DEVELOPMENT);
    }

    @Test
    void testARowWithoutAConfigurationIsRedactedUnlessTheCallerIsTenantAdmin() {
        Job job = job(31L);

        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn("workflow");

        Integration integration = mock(Integration.class);

        when(integration.getId()).thenReturn(1053L);

        IntegrationWorkflow integrationWorkflow = integrationWorkflow(1053L, "workflow");

        when(integrationWorkflowService.getIntegrationWorkflows())
            .thenReturn(List.of(integrationWorkflow));
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(null, null, null))
            .thenReturn(List.of(configuration(1051L, Environment.PRODUCTION)));
        when(integrationInstanceService.getIntegrationInstanceConfigurationIntegrationInstances(List.of(1051L)))
            .thenReturn(List.of(instance(1050L, 1051L)));
        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), eq(true), eq(0)))
            .thenReturn(new PageImpl<>(List.of(31L)));
        when(jobService.getJobs(List.of(31L)))
            .thenReturn(List.of(job));
        when(integrationService.getIntegrations())
            .thenReturn(List.of(integration));
        when(integrationWorkflowService.getIntegrationWorkflows(List.of(1053L)))
            .thenReturn(List.of(integrationWorkflow));
        when(workflowService.getWorkflows(any()))
            .thenReturn(List.of(workflow));
        when(principalJobService.getPrincipalJobs(List.of(31L), PlatformType.EMBEDDED))
            .thenReturn(List.of());

        when(permissionService.isTenantAdmin())
            .thenReturn(false);

        assertThat(facade.getWorkflowExecutions(null, null, null, null, null, null, null, null, 0)
            .getContent()
            .getFirst()
            .job()
            .inputs()).isEmpty();

        when(permissionService.isTenantAdmin())
            .thenReturn(true);

        assertThat(facade.getWorkflowExecutions(null, null, null, null, null, null, null, null, 0)
            .getContent()
            .getFirst()
            .job()
            .inputs()).isEqualTo(Map.of("jobInput", "value"));
    }

    @Test
    void testAConfigurationFilterOutsideTheRequestedEnvironmentListsNothing() {
        IntegrationWorkflow integrationWorkflow = integrationWorkflow(1053L, "workflow");

        when(integrationWorkflowService.getIntegrationWorkflows())
            .thenReturn(List.of(integrationWorkflow));
        when(environmentService.getEnvironment(2L))
            .thenReturn(Environment.PRODUCTION);
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(1051L)))
            .thenReturn(List.of(configuration(1051L, Environment.DEVELOPMENT)));

        Page<WorkflowExecutionDTO> page =
            facade.getWorkflowExecutions(2L, null, null, null, null, 1051L, null, null, 0);

        assertThat(page.getContent()).isEmpty();

        verifyNoInteractions(principalJobService);
    }

    @Test
    void testANonexistentConfigurationFilterWithAnEnvironmentListsNothing() {
        when(environmentService.getEnvironment(2L))
            .thenReturn(Environment.PRODUCTION);
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(999L)))
            .thenReturn(List.of());

        Page<WorkflowExecutionDTO> page = facade.getWorkflowExecutions(2L, null, null, null, null, 999L, null, null, 0);

        assertThat(page.getContent()).isEmpty();

        verifyNoInteractions(principalJobService);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private void stubRun(boolean detailsVisible) {
        Job job = job(10L);

        IntegrationInstance integrationInstance = instance(1050L, 1051L);
        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn("workflow");

        TriggerExecution triggerExecution = mock(TriggerExecution.class);
        WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);

        when(triggerExecution.getType()).thenReturn("myComponent/v1/myTrigger");
        when(triggerExecution.getWorkflowTrigger()).thenReturn(workflowTrigger);
        when(triggerExecution.getOutput()).thenReturn(mock(FileEntry.class));
        when(workflowTrigger.getParameters()).thenReturn(Map.of());
        when(workflowTrigger.evaluateParameters(any(), any())).thenReturn((Map) Map.of("evaluated", true));
        when(triggerFileStorage.readTriggerExecutionOutput(any())).thenReturn("trigger-output");
        when(triggerExecutionService.fetchJobTriggerExecution(10L)).thenReturn(Optional.of(triggerExecution));
        when(jobService.getJob(10L)).thenReturn(job);
        when(jobService.getChildJobIds(10L)).thenReturn(List.of());
        when(jobService.getJobs(List.of())).thenReturn(List.of());
        when(taskExecutionService.getJobTaskExecutions(10L)).thenReturn(List.of());
        when(principalJobService.getJobPrincipalId(10L, PlatformType.EMBEDDED)).thenReturn(1050L);
        when(integrationInstanceService.getIntegrationInstance(1050L)).thenReturn(integrationInstance);
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(1051L))
            .thenReturn(configuration(1051L, Environment.PRODUCTION));
        when(integrationService.getWorkflowIntegration("workflow")).thenReturn(mock(Integration.class));
        when(workflowService.getWorkflow("workflow")).thenReturn(workflow);
        when(permissionService.hasResourceScope(10L, "Job", "EXECUTION_DETAILS_VIEW")).thenReturn(detailsVisible);
    }

    private static Job job(long id) {
        Job job = new Job(id);

        job.setInputs(Map.of("jobInput", "value"));
        job.setWorkflowId("workflow");

        return job;
    }

    private static IntegrationInstanceConfiguration configuration(long id, Environment environment) {
        IntegrationInstanceConfiguration configuration = new IntegrationInstanceConfiguration();

        configuration.setId(id);
        configuration.setEnvironment(environment);

        return configuration;
    }

    private static IntegrationInstance instance(long id, long configurationId) {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(id);
        integrationInstance.setIntegrationInstanceConfigurationId(configurationId);

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
}
