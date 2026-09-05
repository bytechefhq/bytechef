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

package com.bytechef.task.dispatcher.subflow.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentSubflowResumeListenerTest {

    private static final long AGENT_JOB_ID = 100L;
    private static final String AGENT_JOB_RESUME_ID = "agent-resume-id";
    private static final long SUBFLOW_JOB_ID = 200L;
    private static final long TASK_EXECUTION_RESUME_ID = 7L;

    @Mock
    private JobFacade jobFacade;

    @Mock
    private JobService jobService;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Mock
    private TaskFileStorage taskFileStorage;

    private AgentSubflowResumeListener agentSubflowResumeListener;

    @BeforeEach
    void beforeEach() {
        agentSubflowResumeListener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        Job agentJob = new Job();

        agentJob.setId(AGENT_JOB_ID);
        agentJob.setStatus(Job.Status.STOPPED);
        agentJob.setMetadata(
            Map.of(
                MetadataConstants.JOB_RESUME_ID, AGENT_JOB_RESUME_ID, MetadataConstants.TASK_EXECUTION_RESUME_ID,
                TASK_EXECUTION_RESUME_ID));

        when(jobService.getJob(AGENT_JOB_ID)).thenReturn(agentJob);
        when(taskExecutionService.fetchLastJobTaskExecution(SUBFLOW_JOB_ID)).thenReturn(Optional.empty());
    }

    @Test
    void testResumesAgentWithSubflowOutputOnCompleted() {
        FileEntry outputs = new FileEntry("outputs.json", "base64:///outputs.json");

        Job subflowJob = stubSubflowJob(Job.Status.COMPLETED);

        subflowJob.setOutputs(outputs);

        doReturn(Map.of("answer", 42)).when(taskFileStorage)
            .readJobOutputs(outputs);

        agentSubflowResumeListener.onApplicationEvent(
            new JobStatusApplicationEvent(SUBFLOW_JOB_ID, Job.Status.COMPLETED));

        assertThat(captureResumeData()).containsEntry("status", "completed")
            .containsEntry("result", Map.of("answer", 42));
    }

    @Test
    void testCompletedSubflowWithoutOutputsStillReportsCompletion() {
        stubSubflowJob(Job.Status.COMPLETED);

        agentSubflowResumeListener.onApplicationEvent(
            new JobStatusApplicationEvent(SUBFLOW_JOB_ID, Job.Status.COMPLETED));

        assertThat(captureResumeData()).isEqualTo(Map.of("status", "completed"));
    }

    @Test
    void testFailedSubflowReportsTheFailedTaskErrorMessage() {
        stubSubflowJob(Job.Status.FAILED);

        TaskExecution failedTaskExecution = TaskExecution.builder()
            .build();

        failedTaskExecution.setStatus(TaskExecution.Status.FAILED);
        failedTaskExecution.setError(new ExecutionError("connection refused", List.of()));

        when(taskExecutionService.getJobTaskExecutions(SUBFLOW_JOB_ID)).thenReturn(List.of(failedTaskExecution));

        agentSubflowResumeListener.onApplicationEvent(
            new JobStatusApplicationEvent(SUBFLOW_JOB_ID, Job.Status.FAILED));

        Map<String, Object> resumeData = captureResumeData();

        assertThat(resumeData).containsEntry("status", "failed");
        assertThat((String) resumeData.get("error")).contains("connection refused");
    }

    @Test
    void testFailedSubflowWhoseTasksCannotBeReadFallsBackToTheJobError() {
        Job subflowJob = stubSubflowJob(Job.Status.FAILED);

        subflowJob.setError(new ExecutionError("job level failure", List.of()));

        when(taskExecutionService.getJobTaskExecutions(SUBFLOW_JOB_ID))
            .thenThrow(new IllegalStateException("database unavailable"));

        agentSubflowResumeListener.onApplicationEvent(
            new JobStatusApplicationEvent(SUBFLOW_JOB_ID, Job.Status.FAILED));

        Map<String, Object> resumeData = captureResumeData();

        assertThat(resumeData).containsEntry("status", "failed");
        assertThat((String) resumeData.get("error")).contains("job level failure");
    }

    @Test
    void testFailedSubflowWithoutAnyErrorReportsAnUnknownError() {
        stubSubflowJob(Job.Status.FAILED);

        when(taskExecutionService.getJobTaskExecutions(SUBFLOW_JOB_ID)).thenReturn(List.of());

        agentSubflowResumeListener.onApplicationEvent(
            new JobStatusApplicationEvent(SUBFLOW_JOB_ID, Job.Status.FAILED));

        assertThat((String) captureResumeData().get("error")).contains("unknown error");
    }

    @Test
    void testCompletedSubflowWithACallableResponseResumesAgentWithTheResponseOutput() {
        FileEntry callableOutput = new FileEntry("response.json", "base64:///response.json");

        stubSubflowJob(Job.Status.COMPLETED);

        TaskExecution responseTaskExecution = TaskExecution.builder()
            .metadata(Map.of(MetadataConstants.CALLABLE_RESPONSE, true))
            .output(callableOutput)
            .build();

        when(taskExecutionService.fetchLastJobTaskExecution(SUBFLOW_JOB_ID))
            .thenReturn(Optional.of(responseTaskExecution));
        doReturn(Map.of("output", Map.of("reply", "hello"))).when(taskFileStorage)
            .readTaskExecutionOutput(callableOutput);

        agentSubflowResumeListener.onApplicationEvent(
            new JobStatusApplicationEvent(SUBFLOW_JOB_ID, Job.Status.COMPLETED));

        assertThat(captureResumeData()).containsEntry("status", "completed")
            .containsEntry("result", Map.of("reply", "hello"));
    }

    @Test
    void testStoppedSubflowReleasesTheAgentWithAnError() {
        stubSubflowJob(Job.Status.STOPPED);

        agentSubflowResumeListener.onApplicationEvent(
            new JobStatusApplicationEvent(SUBFLOW_JOB_ID, Job.Status.STOPPED));

        assertThat((String) captureResumeData().get("error")).contains("stopped");
    }

    @Test
    void testSuspendedSubflowKeepsTheAgentWaiting() {
        stubSubflowJob(Job.Status.STOPPED);

        agentSubflowResumeListener.onApplicationEvent(JobStatusApplicationEvent.suspended(SUBFLOW_JOB_ID));

        verifyNoInteractions(jobFacade);
    }

    @Test
    void testJobWithoutAnAgentLinkIsIgnored() {
        Job job = new Job();

        job.setId(SUBFLOW_JOB_ID);
        job.setStatus(Job.Status.COMPLETED);
        job.setMetadata(Map.of(SubflowRequestConstants.AGENT_JOB_ID, AGENT_JOB_ID));

        when(jobService.getJob(SUBFLOW_JOB_ID)).thenReturn(job);

        agentSubflowResumeListener.onApplicationEvent(
            new JobStatusApplicationEvent(SUBFLOW_JOB_ID, Job.Status.COMPLETED));

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), any());
    }

    @Test
    void testUnreadableOutputsResumeTheAgentWithAnError() {
        FileEntry outputs = new FileEntry("outputs.json", "base64:///outputs.json");

        Job subflowJob = stubSubflowJob(Job.Status.COMPLETED);

        subflowJob.setOutputs(outputs);

        when(taskFileStorage.readJobOutputs(outputs)).thenThrow(new IllegalStateException("storage unavailable"));

        agentSubflowResumeListener.onApplicationEvent(
            new JobStatusApplicationEvent(SUBFLOW_JOB_ID, Job.Status.COMPLETED));

        Map<String, Object> resumeData = captureResumeData();

        assertThat(resumeData).containsEntry("status", "failed");
        assertThat((String) resumeData.get("error")).contains("storage unavailable");
    }

    private Job stubSubflowJob(Job.Status status) {
        Job subflowJob = new Job();

        subflowJob.setId(SUBFLOW_JOB_ID);
        subflowJob.setStatus(status);
        subflowJob.setWorkflowId("subflow-workflow");
        subflowJob.setMetadata(
            Map.of(
                SubflowRequestConstants.AGENT_JOB_ID, AGENT_JOB_ID, SubflowRequestConstants.AGENT_JOB_RESUME_ID,
                AGENT_JOB_RESUME_ID));

        when(jobService.getJob(SUBFLOW_JOB_ID)).thenReturn(subflowJob);

        return subflowJob;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureResumeData() {
        ArgumentCaptor<Map<String, ?>> resumeDataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(jobFacade).resumeJob(eq(AGENT_JOB_ID), eq(TASK_EXECUTION_RESUME_ID), resumeDataCaptor.capture());

        return new HashMap<>(resumeDataCaptor.getValue());
    }
}
