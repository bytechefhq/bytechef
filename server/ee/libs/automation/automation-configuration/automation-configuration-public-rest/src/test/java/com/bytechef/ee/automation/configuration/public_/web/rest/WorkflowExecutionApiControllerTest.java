/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.TaskExecutionModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowExecutionBasicModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowExecutionModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowExecutionStatusModel;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowExecutionApiControllerTest {

    private static final Instant END_DATE = Instant.parse("2026-07-18T11:00:00Z");
    private static final Instant START_DATE = Instant.parse("2026-07-18T10:00:00Z");

    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade = mock(
        ProjectWorkflowExecutionFacade.class);

    private WorkflowExecutionApiController workflowExecutionApiController;

    @BeforeEach
    void beforeEach() {
        workflowExecutionApiController = new WorkflowExecutionApiController(
            environmentService, projectWorkflowExecutionFacade);
    }

    @Test
    void testGetWorkflowExecutionsPageMapsToBasicModels() {
        WorkflowExecutionDTO workflowExecutionDTO = createWorkflowExecutionDTO();

        when(environmentService.getEnvironment("STAGING")).thenReturn(Environment.STAGING);
        when(
            projectWorkflowExecutionFacade.getWorkflowExecutions(
                isNull(), eq((long) Environment.STAGING.ordinal()), eq(Job.Status.COMPLETED), eq(START_DATE),
                eq(END_DATE), eq(10L), eq(20L), eq("workflow1"), eq(100L), eq(0)))
                    .thenReturn(new PageImpl<>(List.of(workflowExecutionDTO)));

        ResponseEntity<Page> response = workflowExecutionApiController.getWorkflowExecutionsPage(
            100L, EnvironmentModel.STAGING, WorkflowExecutionStatusModel.COMPLETED, START_DATE.atOffset(ZoneOffset.UTC),
            END_DATE.atOffset(ZoneOffset.UTC), 10L, 20L, "workflow1", 0);

        Page<?> page = response.getBody();

        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(1);

        WorkflowExecutionBasicModel workflowExecutionBasicModel =
            (WorkflowExecutionBasicModel) page.getContent()
                .getFirst();

        assertThat(workflowExecutionBasicModel.getId()).isEqualTo(1000L);
        assertThat(workflowExecutionBasicModel.getStatus()).isEqualTo(WorkflowExecutionStatusModel.COMPLETED);
        assertThat(workflowExecutionBasicModel.getStartDate()).isEqualTo(START_DATE.atOffset(ZoneOffset.UTC));
        assertThat(workflowExecutionBasicModel.getEndDate()).isEqualTo(END_DATE.atOffset(ZoneOffset.UTC));

        assertThat(workflowExecutionBasicModel.getProject()
            .getId()).isEqualTo(10L);
        assertThat(workflowExecutionBasicModel.getProject()
            .getName()).isEqualTo("My Project");
        assertThat(workflowExecutionBasicModel.getProjectDeployment()
            .getId()).isEqualTo(20L);
        assertThat(workflowExecutionBasicModel.getWorkflow()
            .getId()).isEqualTo("workflow1");
        assertThat(workflowExecutionBasicModel.getWorkflow()
            .getLabel()).isEqualTo("My Workflow");
    }

    @Test
    void testGetWorkflowExecutionsPageDefaultsToProductionEnvironment() {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
        when(
            projectWorkflowExecutionFacade.getWorkflowExecutions(
                isNull(), eq((long) Environment.PRODUCTION.ordinal()), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq(100L), eq(0)))
                    .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<Page> response = workflowExecutionApiController.getWorkflowExecutionsPage(
            100L, null, null, null, null, null, null, null, 0);

        assertThat(response.getBody()).isNotNull();

        verify(projectWorkflowExecutionFacade).getWorkflowExecutions(
            isNull(), eq((long) Environment.PRODUCTION.ordinal()), isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), eq(100L), eq(0));
    }

    @Test
    void testGetWorkflowExecutionMapsFullDetail() {
        WorkflowExecutionDTO workflowExecutionDTO = createWorkflowExecutionDTO();

        when(projectWorkflowExecutionFacade.getWorkflowExecution(1000L)).thenReturn(workflowExecutionDTO);

        ResponseEntity<WorkflowExecutionModel> response = workflowExecutionApiController.getWorkflowExecution(1000L);

        WorkflowExecutionModel workflowExecutionModel = response.getBody();

        assertThat(workflowExecutionModel).isNotNull();
        assertThat(workflowExecutionModel.getId()).isEqualTo(1000L);
        assertThat(workflowExecutionModel.getStatus()).isEqualTo(WorkflowExecutionStatusModel.COMPLETED);
        assertThat(workflowExecutionModel.getInputs()).isEqualTo(Map.of("inputKey", "inputValue"));
        assertThat(workflowExecutionModel.getOutputs()).isEqualTo(Map.of("outputKey", "outputValue"));

        List<TaskExecutionModel> taskExecutionModels = workflowExecutionModel.getTaskExecutions();

        assertThat(taskExecutionModels).hasSize(1);

        TaskExecutionModel taskExecutionModel = taskExecutionModels.getFirst();

        assertThat(taskExecutionModel.getId()).isEqualTo("2000");
        assertThat(taskExecutionModel.getName()).isEqualTo("httpRequest1");
        assertThat(taskExecutionModel.getTitle()).isEqualTo("HTTP Request");
        assertThat(taskExecutionModel.getType()).isEqualTo("httpClient/v1/get");
        assertThat(taskExecutionModel.getStatus()).isEqualTo(TaskExecutionModel.StatusEnum.COMPLETED);
        assertThat(taskExecutionModel.getInput()).isEqualTo(Map.of("url", "https://example.com"));
        assertThat(taskExecutionModel.getOutput()).isEqualTo(JsonNullable.of(Map.of("body", "ok")));

        assertThat(taskExecutionModel.getError()).isNotNull();
        assertThat(taskExecutionModel.getError()
            .getMessage()).isEqualTo("Task failed");
        assertThat(taskExecutionModel.getError()
            .getStackTrace()).containsExactly("line1", "line2");
    }

    private WorkflowExecutionDTO createWorkflowExecutionDTO() {
        Project project = mock(Project.class);

        when(project.getId()).thenReturn(10L);
        when(project.getName()).thenReturn("My Project");

        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);

        when(projectDeployment.getId()).thenReturn(20L);
        when(projectDeployment.getName()).thenReturn("My Deployment");
        when(projectDeployment.getProjectVersion()).thenReturn(3);
        when(projectDeployment.getEnvironmentId()).thenReturn(2L);
        when(projectDeployment.isEnabled()).thenReturn(true);

        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn("workflow1");
        when(workflow.getLabel()).thenReturn("My Workflow");

        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getName()).thenReturn("httpRequest1");

        TaskExecutionDTO taskExecutionDTO = new TaskExecutionDTO(
            null, null, END_DATE, new ExecutionError("Task failed", List.of("line1", "line2")), 0, null, 2000L,
            Map.of("url", "https://example.com"), null, null, null, 0, Map.of("body", "ok"), null, 0, 0, 0, null, 0, 0,
            START_DATE, TaskExecution.Status.COMPLETED, 0, "HTTP Request", "httpClient/v1/get", workflowTask, List.of(),
            List.of(), null);

        JobDTO jobDTO = new JobDTO(
            null, null, 0, END_DATE, null, 3000L, Map.of("inputKey", "inputValue"), null, null, null, Map.of(),
            Map.of("outputKey", "outputValue"), null, 0, START_DATE, Job.Status.COMPLETED, List.of(taskExecutionDTO), 0,
            List.of(), "workflow1");

        return new WorkflowExecutionDTO(1000L, project, projectDeployment, jobDTO, workflow, null);
    }
}
