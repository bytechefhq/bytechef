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
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationWorkflowExecutionFacadeTest {

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
    public void setUp() {
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
            componentDefinitionService, contextService, environmentService, evaluator, principalJobService,
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
    public void testGetWorkflowExecutionsIncludesJobsOfPublishedIntegrationVersion() {
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
            0L, null, null, null, null, null, null, 0);

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
    public void testGetWorkflowExecutionsFiltersByInstancesOfTheConfiguration() {
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

        facade.getWorkflowExecutions(null, null, null, null, null, 1051L, null, 0);

        verify(principalJobService).getJobIds(
            null, null, null, List.of(1050L), PlatformType.EMBEDDED, List.of("published-workflow"), true, 0);
    }

    @Test
    public void testGetWorkflowExecutionsExpandsWorkflowFilterToEveryIntegrationVersion() {
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

        facade.getWorkflowExecutions(null, null, null, null, null, null, "draft-workflow", 0);

        verify(principalJobService).getJobIds(
            null, null, null, List.of(1050L), PlatformType.EMBEDDED, List.of("published-workflow", "draft-workflow"),
            true, 0);
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
    public void testGetWorkflowExecutionTaskExecutionRejectsTaskFromAnotherWorkflowExecution() {
        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(taskExecution);
        when(jobService.getJob(10L))
            .thenReturn(new Job(10L));

        assertThatThrownBy(() -> facade.getWorkflowExecutionTaskExecution(999L, 1L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testToTaskExecutionDTODoesNotLoadTaskDataForList() {
        TaskExecutionDTO taskExecutionDTO = facade.toTaskExecutionDTO(taskExecution, null, false);

        assertThat(taskExecutionDTO.input()).isNull();
        assertThat(taskExecutionDTO.output()).isNull();

        verify(contextService, never()).peek(anyLong(), any());
        verify(taskFileStorage, never()).readContextValue(any());
        verify(taskFileStorage, never()).readTaskExecutionOutput(any());
        verifyNoInteractions(evaluator);
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
