/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TaskExecutionModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.WorkflowExecutionBasicModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.WorkflowExecutionModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.WorkflowExecutionStatusModel;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.ee.embedded.workflow.execution.facade.IntegrationWorkflowExecutionFacade;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowExecutionApiControllerTest {

    private static final Instant END_DATE = Instant.parse("2026-07-18T11:00:00Z");
    private static final String EXTERNAL_USER_ID = "externalUser1";
    private static final Instant START_DATE = Instant.parse("2026-07-18T10:00:00Z");

    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade = mock(
        IntegrationWorkflowExecutionFacade.class);

    private WorkflowExecutionApiController workflowExecutionApiController;

    @BeforeEach
    void beforeEach() {
        workflowExecutionApiController = new WorkflowExecutionApiController(
            environmentService, integrationWorkflowExecutionFacade);

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(EXTERNAL_USER_ID, "n/a"));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetWorkflowExecutionsPageMapsToBasicModels() {
        WorkflowExecutionDTO workflowExecutionDTO = createWorkflowExecutionDTO();

        when(environmentService.getEnvironment("PRODUCTION")).thenReturn(Environment.PRODUCTION);
        when(
            integrationWorkflowExecutionFacade.getConnectedUserWorkflowExecutions(
                eq(EXTERNAL_USER_ID), eq((long) Environment.PRODUCTION.ordinal()), eq(Job.Status.COMPLETED),
                eq(START_DATE), eq(END_DATE), eq(20L), eq(0)))
                    .thenReturn(new PageImpl<>(List.of(workflowExecutionDTO)));

        ResponseEntity<Page> response = workflowExecutionApiController.getWorkflowExecutionsPage(
            EXTERNAL_USER_ID, EnvironmentModel.PRODUCTION, WorkflowExecutionStatusModel.COMPLETED,
            START_DATE.atOffset(ZoneOffset.UTC), END_DATE.atOffset(ZoneOffset.UTC), 20L, 0);

        Page<?> page = response.getBody();

        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(1);

        WorkflowExecutionBasicModel workflowExecutionBasicModel =
            (WorkflowExecutionBasicModel) page.getContent()
                .getFirst();

        assertThat(workflowExecutionBasicModel.getId()).isEqualTo(1000L);
        assertThat(workflowExecutionBasicModel.getStatus()).isEqualTo(WorkflowExecutionStatusModel.COMPLETED);

        assertThat(workflowExecutionBasicModel.getIntegration()
            .getId()).isEqualTo(10L);
        assertThat(workflowExecutionBasicModel.getIntegration()
            .getComponentName()).isEqualTo("slack");
        assertThat(workflowExecutionBasicModel.getWorkflow()
            .getLabel()).isEqualTo("My Workflow");
    }

    @Test
    void testGetWorkflowExecutionsPageDefaultsToProductionEnvironment() {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
        when(
            integrationWorkflowExecutionFacade.getConnectedUserWorkflowExecutions(
                eq(EXTERNAL_USER_ID), eq((long) Environment.PRODUCTION.ordinal()), isNull(), isNull(), isNull(),
                isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<Page> response = workflowExecutionApiController.getWorkflowExecutionsPage(
            EXTERNAL_USER_ID, null, null, null, null, null, 0);

        assertThat(response.getBody()).isNotNull();

        verify(integrationWorkflowExecutionFacade).getConnectedUserWorkflowExecutions(
            eq(EXTERNAL_USER_ID), eq((long) Environment.PRODUCTION.ordinal()), isNull(), isNull(), isNull(), isNull(),
            eq(0));
    }

    @Test
    void testGetWorkflowExecutionMapsFullDetail() {
        WorkflowExecutionDTO workflowExecutionDTO = createWorkflowExecutionDTO();

        when(integrationWorkflowExecutionFacade.getConnectedUserWorkflowExecution(EXTERNAL_USER_ID, 1000L))
            .thenReturn(workflowExecutionDTO);

        ResponseEntity<WorkflowExecutionModel> response = workflowExecutionApiController.getWorkflowExecution(
            EXTERNAL_USER_ID, 1000L);

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
        assertThat(taskExecutionModel.getStatus()).isEqualTo(TaskExecutionModel.StatusEnum.COMPLETED);
        assertThat(taskExecutionModel.getInput()).isEqualTo(Map.of("url", "https://example.com"));
        assertThat(taskExecutionModel.getOutput()).isEqualTo(JsonNullable.of(Map.of("body", "ok")));
    }

    @Test
    void testGetWorkflowExecutionDeniesMismatchedExternalUserId() {
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> workflowExecutionApiController.getWorkflowExecution("otherUser", 1000L));
    }

    private WorkflowExecutionDTO createWorkflowExecutionDTO() {
        Integration integration = mock(Integration.class);

        when(integration.getId()).thenReturn(10L);
        when(integration.getComponentName()).thenReturn("slack");

        Workflow workflow = mock(Workflow.class);

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

        return new WorkflowExecutionDTO(1000L, integration, null, null, jobDTO, workflow, null);
    }
}
