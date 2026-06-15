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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationWorkflowExecutionFacadeTest {

    private ContextService contextService;
    private Evaluator evaluator;
    private IntegrationWorkflowExecutionFacadeImpl facade;
    private JobService jobService;
    private TaskExecution taskExecution;
    private TaskExecutionService taskExecutionService;
    private TaskFileStorage taskFileStorage;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);

        contextService = mock(ContextService.class);
        evaluator = mock(Evaluator.class);
        jobService = mock(JobService.class);
        taskExecutionService = mock(TaskExecutionService.class);
        taskFileStorage = mock(TaskFileStorage.class);

        facade = new IntegrationWorkflowExecutionFacadeImpl(
            componentDefinitionService, contextService, mock(EnvironmentService.class), evaluator,
            mock(PrincipalJobService.class), mock(IntegrationInstanceConfigurationService.class),
            mock(IntegrationInstanceService.class), mock(IntegrationInstanceConfigurationWorkflowService.class),
            mock(IntegrationService.class), mock(IntegrationWorkflowFacade.class),
            mock(IntegrationWorkflowService.class), jobService,
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
}
