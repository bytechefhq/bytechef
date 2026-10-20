/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowExecutionDTOTest {

    @Test
    void testWithoutDetailsClearsEveryNestedField() {
        Job job = new Job(7L);

        job.setError(new ExecutionError("job failed", List.of("frame")));
        job.setInputs(Map.of("secret", "job-input"));

        TaskExecution childTaskExecution = TaskExecution.builder()
            .id(3L)
            .workflowTask(new WorkflowTask(Map.of("name", "child", "type", "test/v1")))
            .jobId(7L)
            .error(new ExecutionError("child failed", List.of()))
            .build();

        TaskExecutionDTO childTaskExecutionDTO = new TaskExecutionDTO(
            childTaskExecution, "Child", "icon", Map.of("secret", "child-input"), "child-output", null);

        TaskExecution taskExecution = TaskExecution.builder()
            .id(2L)
            .workflowTask(new WorkflowTask(Map.of("name", "task", "type", "test/v1")))
            .jobId(7L)
            .error(new ExecutionError("task failed", List.of()))
            .build();

        TaskExecutionDTO taskExecutionDTO = new TaskExecutionDTO(
            taskExecution, "Task", "icon", Map.of("secret", "task-input"), "task-output",
            new JobDTO(job, Map.of("secret", "child-job-output"), List.of(childTaskExecutionDTO)));

        JobDTO jobDTO = new JobDTO(job, Map.of("secret", "job-output"), List.of(taskExecutionDTO));

        TriggerExecution triggerExecution = mock(TriggerExecution.class);
        TriggerExecutionDTO triggerExecutionDTO = new TriggerExecutionDTO(
            triggerExecution, "Trigger", "icon", Map.of("secret", "trigger-input"), "trigger-output");

        WorkflowExecutionDTO workflowExecutionDTO = new WorkflowExecutionDTO(
            7L, mock(Integration.class), mock(IntegrationInstanceConfiguration.class),
            mock(IntegrationInstance.class), jobDTO, mock(Workflow.class), triggerExecutionDTO);

        WorkflowExecutionDTO redactedWorkflowExecutionDTO = workflowExecutionDTO.withoutDetails();

        assertThat(redactedWorkflowExecutionDTO.id()).isEqualTo(7L);
        assertThat(redactedWorkflowExecutionDTO.integration()).isSameAs(workflowExecutionDTO.integration());
        assertThat(redactedWorkflowExecutionDTO.workflow()).isSameAs(workflowExecutionDTO.workflow());

        JobDTO redactedJobDTO = redactedWorkflowExecutionDTO.job();

        assertThat(redactedJobDTO.error()).isNull();
        assertThat(redactedJobDTO.inputs()).isEmpty();
        assertThat(redactedJobDTO.outputs()).isNull();

        TaskExecutionDTO redactedTaskExecutionDTO = redactedJobDTO.taskExecutions()
            .getFirst();

        assertThat(redactedTaskExecutionDTO.error()).isNull();
        assertThat(redactedTaskExecutionDTO.input()).isNull();
        assertThat(redactedTaskExecutionDTO.output()).isNull();

        JobDTO redactedChildJobDTO = redactedTaskExecutionDTO.childJob();

        assertThat(redactedChildJobDTO.inputs()).isEmpty();
        assertThat(redactedChildJobDTO.outputs()).isNull();

        TaskExecutionDTO redactedChildTaskExecutionDTO = redactedChildJobDTO.taskExecutions()
            .getFirst();

        assertThat(redactedChildTaskExecutionDTO.error()).isNull();
        assertThat(redactedChildTaskExecutionDTO.input()).isNull();
        assertThat(redactedChildTaskExecutionDTO.output()).isNull();

        TriggerExecutionDTO redactedTriggerExecutionDTO = redactedWorkflowExecutionDTO.triggerExecution();

        assertThat(redactedTriggerExecutionDTO.error()).isNull();
        assertThat(redactedTriggerExecutionDTO.input()).isNull();
        assertThat(redactedTriggerExecutionDTO.output()).isNull();
        assertThat(redactedTriggerExecutionDTO.state()).isNull();
    }

    @Test
    void testWithoutDetailsToleratesAbsentJobAndTrigger() {
        WorkflowExecutionDTO workflowExecutionDTO = new WorkflowExecutionDTO(
            7L, mock(Integration.class), null, null, null, mock(Workflow.class), null);

        WorkflowExecutionDTO redactedWorkflowExecutionDTO = workflowExecutionDTO.withoutDetails();

        assertThat(redactedWorkflowExecutionDTO.job()).isNull();
        assertThat(redactedWorkflowExecutionDTO.triggerExecution()).isNull();
        assertThat(redactedWorkflowExecutionDTO.isTriggerOnly()).isTrue();
    }
}
