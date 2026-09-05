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

package com.bytechef.task.dispatcher.subflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class SubflowChildJobLauncherTest {

    @Mock
    private ChildJobPrincipalFactory childJobPrincipalFactory;

    @Test
    void testLaunchWrapsInputsUnderTheTriggerNameAndLinksTheParentTask() {
        Job job = new Job();

        job.setId(1L);
        job.setMetadata(Map.of("tenant", "acme"));

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of("name", "callChild", "type", "subflow/v1")))
            .build();

        taskExecution.setId(10L);
        taskExecution.setJobId(1L);

        when(childJobPrincipalFactory.createChildJob(eq(1L), any())).thenReturn(200L);

        SubflowChildJobLauncher.launch(
            childJobPrincipalFactory, job, taskExecution, new Subflow("child-workflow-id", "newWorkflowCall"),
            Map.of("message", "hello"));

        ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
            ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(childJobPrincipalFactory).createChildJob(eq(1L), jobParametersDTOArgumentCaptor.capture());

        JobParametersDTO jobParametersDTO = jobParametersDTOArgumentCaptor.getValue();

        assertThat(jobParametersDTO.getWorkflowId()).isEqualTo("child-workflow-id");
        assertThat(jobParametersDTO.getParentTaskExecutionId()).isEqualTo(10L);
        assertThat(jobParametersDTO.getInputs())
            .containsEntry("newWorkflowCall", Map.of("message", "hello"))
            .containsEntry(JobInputConstants.TRIGGER_NAME_INPUT, "newWorkflowCall");
        assertThat(jobParametersDTO.getMetadata()).containsEntry("tenant", "acme");
    }

    @Test
    void testLaunchThrowsWhenSubflowWorkflowIdIsEmpty() {
        Job job = new Job();

        job.setId(1L);
        job.setMetadata(Map.of());

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of("name", "callChild", "type", "subflow/v1")))
            .build();

        taskExecution.setId(10L);
        taskExecution.setJobId(1L);

        assertThrows(IllegalStateException.class, () -> SubflowChildJobLauncher.launch(
            childJobPrincipalFactory, job, taskExecution, new Subflow("", "newWorkflowCall"),
            Map.of("message", "hello")));

        verifyNoInteractions(childJobPrincipalFactory);
    }
}
