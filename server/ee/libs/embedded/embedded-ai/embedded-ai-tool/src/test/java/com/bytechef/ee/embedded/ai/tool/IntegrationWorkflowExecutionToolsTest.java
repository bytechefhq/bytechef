/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.ee.embedded.ai.tool.model.WorkflowExecutionDetailInfo;
import com.bytechef.ee.embedded.ai.tool.model.WorkflowExecutionSummary;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.ee.embedded.workflow.execution.facade.IntegrationWorkflowExecutionFacade;
import com.bytechef.error.ExecutionError;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class IntegrationWorkflowExecutionToolsTest {

    private final IntegrationWorkflowExecutionFacade facade = mock(IntegrationWorkflowExecutionFacade.class);
    private final IntegrationWorkflowExecutionTools integrationWorkflowExecutionTools =
        new IntegrationWorkflowExecutionTools(facade);

    @Test
    void testGetWorkflowExecutionMapsTaskFailure() {
        Job job = new Job();

        job.setId(7L);
        job.setStatus(Job.Status.FAILED);
        job.setWorkflowId("wf-1");

        TaskExecution taskExecution = TaskExecution.builder()
            .id(11L)
            .workflowTask(new WorkflowTask(Map.of("name", "getUser", "type", "httpClient/v1/get")))
            .status(TaskExecution.Status.FAILED)
            .error(new ExecutionError("boom", List.of("at x", "at y")))
            .build();

        TaskExecutionDTO taskExecutionDTO = new TaskExecutionDTO(
            taskExecution, "Get user", "icon", Map.of("url", "https://x"), null, null);

        JobDTO jobDTO = new JobDTO(job, Map.of(), List.of(taskExecutionDTO));

        Integration integration = new Integration();

        integration.setName("Demo");

        Workflow workflow = new Workflow("{\"label\": \"My Workflow\", \"tasks\": []}", Format.JSON);

        WorkflowExecutionDTO dto = new WorkflowExecutionDTO(7L, integration, null, null, jobDTO, workflow, null);

        when(facade.getWorkflowExecution(7L)).thenReturn(dto);

        ToolContext toolContext = new ToolContext(Map.of());

        WorkflowExecutionDetailInfo result = integrationWorkflowExecutionTools.getWorkflowExecution(7L, toolContext);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.workflowLabel()).isEqualTo("My Workflow");
        assertThat(result.integrationName()).isEqualTo("Demo");
        assertThat(result.taskExecutions()).hasSize(1);
        assertThat(result.taskExecutions()
            .get(0)
            .error()).isEqualTo("boom");
        assertThat(result.taskExecutions()
            .get(0)
            .type()).isEqualTo("httpClient/v1/get");
    }

    @Test
    void testGetWorkflowExecutionReturnsWhenEnvironmentMatches() {
        Job job = new Job();

        job.setId(7L);
        job.setStatus(Job.Status.COMPLETED);
        job.setWorkflowId("wf-1");

        JobDTO jobDTO = new JobDTO(job, Map.of(), List.of());

        Integration integration = new Integration();

        integration.setName("Demo");

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);

        Workflow workflow = new Workflow("{\"label\": \"My Workflow\", \"tasks\": []}", Format.JSON);

        WorkflowExecutionDTO dto = new WorkflowExecutionDTO(
            7L, integration, integrationInstanceConfiguration, null, jobDTO, workflow, null);

        when(facade.getWorkflowExecution(7L)).thenReturn(dto);

        ToolContext toolContext = new ToolContext(
            Map.of(
                IntegrationWorkflowExecutionToolContextKeys.ENVIRONMENT_ID,
                (long) Environment.PRODUCTION.ordinal()));

        WorkflowExecutionDetailInfo result = integrationWorkflowExecutionTools.getWorkflowExecution(7L, toolContext);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    @Test
    void testGetWorkflowExecutionDeniesCrossEnvironmentAccess() {
        Job job = new Job();

        job.setId(7L);
        job.setStatus(Job.Status.COMPLETED);

        JobDTO jobDTO = new JobDTO(job, Map.of(), List.of());

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);

        WorkflowExecutionDTO dto = new WorkflowExecutionDTO(
            7L, null, integrationInstanceConfiguration, null, jobDTO, null, null);

        when(facade.getWorkflowExecution(7L)).thenReturn(dto);

        // Caller is scoped to DEVELOPMENT but the execution belongs to PRODUCTION.
        ToolContext toolContext = new ToolContext(
            Map.of(
                IntegrationWorkflowExecutionToolContextKeys.ENVIRONMENT_ID,
                (long) Environment.DEVELOPMENT.ordinal()));

        assertThatThrownBy(() -> integrationWorkflowExecutionTools.getWorkflowExecution(7L, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void testGetWorkflowExecutionThrowsOnFailure() {
        when(facade.getWorkflowExecution(7L)).thenThrow(new RuntimeException("boom"));

        ToolContext toolContext = new ToolContext(Map.of());

        assertThatThrownBy(() -> integrationWorkflowExecutionTools.getWorkflowExecution(7L, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Failed to get workflow execution");
    }

    @Test
    void testListWorkflowExecutionsThrowsOnFailure() {
        when(
            facade.getWorkflowExecutions(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyInt()))
                    .thenThrow(new RuntimeException("boom"));

        ToolContext toolContext = new ToolContext(Map.of());

        assertThatThrownBy(() -> integrationWorkflowExecutionTools.listWorkflowExecutions(null, null, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Failed to list workflow executions");
    }

    @Test
    void testListWorkflowExecutionsScopesByEnvironmentFromToolContext() {
        Job job = new Job();

        job.setId(7L);
        job.setStatus(Job.Status.COMPLETED);

        JobDTO jobDTO = new JobDTO(job, Map.of(), List.of());

        Integration integration = new Integration();

        integration.setName("Demo");

        Workflow workflow = new Workflow("{\"label\": \"My Workflow\", \"tasks\": []}", Format.JSON);

        WorkflowExecutionDTO dto = new WorkflowExecutionDTO(7L, integration, null, null, jobDTO, workflow, null);

        Page<WorkflowExecutionDTO> page = new PageImpl<>(List.of(dto));

        when(
            facade.getWorkflowExecutions(
                eq(2L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyInt()))
                    .thenReturn(page);

        ToolContext toolContext = new ToolContext(
            Map.of(IntegrationWorkflowExecutionToolContextKeys.ENVIRONMENT_ID, 2L));

        List<WorkflowExecutionSummary> result =
            integrationWorkflowExecutionTools.listWorkflowExecutions(null, null, toolContext);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .id()).isEqualTo(7L);
        assertThat(result.get(0)
            .status()).isEqualTo("COMPLETED");
        assertThat(result.get(0)
            .integrationName()).isEqualTo("Demo");
    }
}
