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

package com.bytechef.platform.workflow.test.coordinator.task.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction.ResumeResponse;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.EvaluationException;

/**
 * @author Marko Kriskovic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class TestTaskDispatcherPreSendProcessorTest {

    private static final long JOB_ID = 1L;
    private static final String WORKFLOW_ID = "workflow1";
    private static final String TASK_NAME = "action1";

    @Mock
    private JobService jobService;

    @Mock
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private TestTaskDispatcherPreSendProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TestTaskDispatcherPreSendProcessor(
            jobService, workflowNodeOutputFacade, workflowTestConfigurationService);
    }

    @Test
    void processSuccessfully() {
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getName()).thenReturn(TASK_NAME);

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(JOB_ID)
            .workflowTask(workflowTask)
            .build();

        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(WORKFLOW_ID, TASK_NAME, 0))
            .thenReturn(List.of());
        when(workflowNodeOutputFacade.getWorkflowNodeOutput(
            WORKFLOW_ID, TASK_NAME, Environment.DEVELOPMENT.ordinal()))
                .thenReturn(null);

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        assertThat(metadata.get(MetadataConstants.ENVIRONMENT_ID)).isEqualTo(Environment.DEVELOPMENT.ordinal());
        assertThat(metadata.get(MetadataConstants.EDITOR_ENVIRONMENT)).isEqualTo(true);
        assertThat(metadata.get(MetadataConstants.WORKFLOW_ID)).isEqualTo(WORKFLOW_ID);
    }

    @Test
    void processHandlesEvaluationExceptionGracefully() {
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getName()).thenReturn(TASK_NAME);

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(JOB_ID)
            .workflowTask(workflowTask)
            .build();

        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(WORKFLOW_ID, TASK_NAME, 0))
            .thenReturn(List.of());
        when(workflowNodeOutputFacade.getWorkflowNodeOutput(
            WORKFLOW_ID, TASK_NAME, Environment.DEVELOPMENT.ordinal()))
                .thenThrow(new EvaluationException("Couldn't evaluate expression with sample output"));

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        assertThat(metadata.get(MetadataConstants.ENVIRONMENT_ID)).isEqualTo(Environment.DEVELOPMENT.ordinal());
        assertThat(metadata.get(MetadataConstants.EDITOR_ENVIRONMENT)).isEqualTo(true);
        assertThat(metadata.get(MetadataConstants.WORKFLOW_ID)).isEqualTo(WORKFLOW_ID);
        assertThat(metadata.get(MetadataConstants.RESUME_DATA)).isNull();
    }

    @Test
    void processWithConnectionIdsPutsConnectionMetadata() {
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getName()).thenReturn(TASK_NAME);

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(JOB_ID)
            .workflowTask(workflowTask)
            .build();

        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(jobService.getJob(JOB_ID)).thenReturn(job);

        WorkflowTestConfigurationConnection connection =
            new WorkflowTestConfigurationConnection(42L, "connKey", TASK_NAME);

        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(WORKFLOW_ID, TASK_NAME, 0))
            .thenReturn(List.of(connection));
        when(workflowNodeOutputFacade.getWorkflowNodeOutput(
            WORKFLOW_ID, TASK_NAME, Environment.DEVELOPMENT.ordinal()))
                .thenReturn(null);

        TaskExecution result = processor.process(taskExecution);

        @SuppressWarnings("unchecked")
        Map<String, Long> connectionIds = (Map<String, Long>) result.getMetadata()
            .get(MetadataConstants.CONNECTION_IDS);

        assertThat(connectionIds).containsEntry("connKey", 42L);
    }

    @Test
    void processDoesNotPutConnectionMetadataWhenConnectionsEmpty() {
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getName()).thenReturn(TASK_NAME);

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(JOB_ID)
            .workflowTask(workflowTask)
            .build();

        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(WORKFLOW_ID, TASK_NAME, 0))
            .thenReturn(List.of());
        when(workflowNodeOutputFacade.getWorkflowNodeOutput(
            WORKFLOW_ID, TASK_NAME, Environment.DEVELOPMENT.ordinal()))
                .thenReturn(null);

        TaskExecution result = processor.process(taskExecution);

        assertThat(result.getMetadata()
            .get(MetadataConstants.CONNECTION_IDS)).isNull();
    }

    @Test
    void processDoesNotSetResumeMetadataWhenSampleOutputIsNotMap() {
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getName()).thenReturn(TASK_NAME);

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(JOB_ID)
            .workflowTask(workflowTask)
            .build();

        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(WORKFLOW_ID, TASK_NAME, 0))
            .thenReturn(List.of());

        WorkflowNodeOutputDTO workflowNodeOutputDTO = new WorkflowNodeOutputDTO(
            null, null, new OutputResponse(null, "not-a-map", null), null, false, null, TASK_NAME);

        when(workflowNodeOutputFacade.getWorkflowNodeOutput(
            WORKFLOW_ID, TASK_NAME, Environment.DEVELOPMENT.ordinal()))
                .thenReturn(workflowNodeOutputDTO);

        TaskExecution result = processor.process(taskExecution);

        assertThat(result.getMetadata()
            .get(MetadataConstants.RESUME_DATA)).isNull();
        assertThat(result.getMetadata()
            .get(MetadataConstants.SUSPEND)).isNull();
    }

    @Test
    void processSetsResumeAndSuspendMetadataWhenSampleOutputHasResumedTrue() {
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getName()).thenReturn(TASK_NAME);

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(JOB_ID)
            .workflowTask(workflowTask)
            .build();

        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(WORKFLOW_ID, TASK_NAME, 0))
            .thenReturn(List.of());

        Map<String, Object> resumeData = Map.of("key", "value");
        ResumeResponse resumeResponse = ResumeResponse.of(resumeData);

        WorkflowNodeOutputDTO workflowNodeOutputDTO = new WorkflowNodeOutputDTO(
            null, null, new OutputResponse(null, resumeResponse, null), null, false, null, TASK_NAME);

        when(workflowNodeOutputFacade.getWorkflowNodeOutput(
            WORKFLOW_ID, TASK_NAME, Environment.DEVELOPMENT.ordinal()))
                .thenReturn(workflowNodeOutputDTO);

        TaskExecution result = processor.process(taskExecution);

        assertThat(result.getMetadata()
            .get(MetadataConstants.RESUME_DATA)).isEqualTo(resumeData);
        assertThat(result.getMetadata()
            .get(MetadataConstants.SUSPEND)).isInstanceOf(ActionContext.Suspend.class);
    }
}
