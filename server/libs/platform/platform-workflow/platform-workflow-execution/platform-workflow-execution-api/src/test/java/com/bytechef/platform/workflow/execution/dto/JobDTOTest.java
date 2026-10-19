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

package com.bytechef.platform.workflow.execution.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class JobDTOTest {

    @Test
    void testWithoutDetailsOmitsPayloadsOfNestedTaskExecutionsAndChildJobs() {
        Job job = new Job(20L);

        job.setError(new ExecutionError("job failure", List.of()));
        job.setInputs(Map.of("email", "someone@example.com"));

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .workflowTask(new WorkflowTask(Map.of("name", "subflow", "type", "subflow/v1")))
            .build();

        TaskExecutionDTO childTaskExecutionDTO = new TaskExecutionDTO(
            taskExecution, "Child", "icon", Map.of("input", "child-input"), "child-output", null);
        TaskExecutionDTO subflowTaskExecutionDTO = new TaskExecutionDTO(
            taskExecution, "Subflow", "icon", Map.of("input", "subflow-input"), "subflow-output",
            new JobDTO(job, Map.of("result", "child-job-output"), List.of(childTaskExecutionDTO)));

        JobDTO jobDTO = new JobDTO(job, Map.of("result", "job-output"), List.of(subflowTaskExecutionDTO))
            .withoutDetails();

        assertThat(jobDTO.inputs()).isEmpty();
        assertThat(jobDTO.outputs()).isNull();
        assertThat(jobDTO.error()).isNull();
        assertThat(jobDTO.status()).isEqualTo(job.getStatus());

        List<TaskExecutionDTO> taskExecutionDTOs = jobDTO.taskExecutions();

        TaskExecutionDTO redactedSubflowTaskExecutionDTO = taskExecutionDTOs.getFirst();

        assertThat(redactedSubflowTaskExecutionDTO.input()).isNull();
        assertThat(redactedSubflowTaskExecutionDTO.output()).isNull();
        assertThat(redactedSubflowTaskExecutionDTO.title()).isEqualTo("Subflow");

        JobDTO redactedChildJobDTO = redactedSubflowTaskExecutionDTO.childJob();

        assertThat(redactedChildJobDTO.outputs()).isNull();
        assertThat(redactedChildJobDTO.inputs()).isEmpty();
        assertThat(redactedChildJobDTO.taskExecutions())
            .singleElement()
            .satisfies(redactedChildTaskExecutionDTO -> {
                assertThat(redactedChildTaskExecutionDTO.input()).isNull();
                assertThat(redactedChildTaskExecutionDTO.output()).isNull();
            });
    }
}
