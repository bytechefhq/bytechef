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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
class AgentSubflowResumeListenerTest {

    @Mock
    private JobFacade jobFacade;

    @Mock
    private JobService jobService;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Mock
    private TaskFileStorage taskFileStorage;

    @Test
    void testResumesAgentWithSubflowOutputOnCompleted() {
        long subflowJobId = 200L;
        long agentJobId = 100L;

        Job subflowJob = new Job();

        subflowJob.setId(subflowJobId);
        subflowJob.setMetadata(Map.of(SubflowRequestConstants.AGENT_JOB_ID, agentJobId));

        when(jobService.getJob(subflowJobId)).thenReturn(subflowJob);
        when(taskExecutionService.fetchLastJobTaskExecution(subflowJobId)).thenReturn(Optional.empty());

        Job agentJob = new Job();

        agentJob.setId(agentJobId);
        agentJob.setStatus(Job.Status.STOPPED);
        agentJob.setMetadata(Map.of(MetadataConstants.TASK_EXECUTION_RESUME_ID, 1L));

        when(jobService.getJob(agentJobId)).thenReturn(agentJob);

        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(subflowJobId, Job.Status.COMPLETED));

        verify(jobFacade).resumeJob(eq(agentJobId), anyLong(), any());
    }

    @Test
    void testResumesAgentWithErrorOnFailed() {
        Job subflowJob = new Job();

        subflowJob.setId(200L);
        subflowJob.setWorkflowId("wf-99");
        subflowJob.setMetadata(Map.of(SubflowRequestConstants.AGENT_JOB_ID, 100L));

        when(jobService.getJob(200L)).thenReturn(subflowJob);

        Job agentJob = new Job();

        agentJob.setId(100L);
        agentJob.setStatus(Job.Status.STOPPED);
        agentJob.setMetadata(Map.of(MetadataConstants.TASK_EXECUTION_RESUME_ID, 1L));

        when(jobService.getJob(100L)).thenReturn(agentJob);

        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(200L, Job.Status.FAILED));

        verify(jobFacade).resumeJob(eq(100L), anyLong(), org.mockito.ArgumentMatchers.argThat(
            (Map<String, ?> data) -> data != null && data.containsKey("error")));
    }

    @Test
    void testIgnoresNonAgentSubflowJob() {
        Job job = new Job();

        job.setId(200L);
        job.setMetadata(Map.of());

        when(jobService.getJob(200L)).thenReturn(job);

        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(200L, Job.Status.COMPLETED));

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), any());
    }

    @Test
    void testNoOpWhenAgentNotStopped() {
        Job subflowJob = new Job();

        subflowJob.setId(200L);
        subflowJob.setMetadata(Map.of(SubflowRequestConstants.AGENT_JOB_ID, 100L));

        when(jobService.getJob(200L)).thenReturn(subflowJob);

        Job agentJob = new Job();

        agentJob.setId(100L);
        agentJob.setStatus(Job.Status.STARTED);

        when(jobService.getJob(100L)).thenReturn(agentJob);

        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(200L, Job.Status.COMPLETED));

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), any());
    }

    @Test
    void testIgnoresNonTerminalStatus() {
        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(200L, Job.Status.STARTED));

        verify(jobService, never()).getJob(anyLong());
        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), any());
    }

    /**
     * I5 (happy path): when the sub-workflow's last task execution carries the {@code CALLABLE_RESPONSE} marker, the
     * listener must resume the agent with the unwrapped {@code CallableResponse.output()}, not the raw job outputs.
     * Spec phase 4 / step 11.
     */
    @Test
    void testResumeUsesCallableResponseOutputWhenLastTaskHasIt() {
        long subflowJobId = 200L;
        long agentJobId = 100L;

        Job subflowJob = new Job();

        subflowJob.setId(subflowJobId);
        subflowJob.setMetadata(Map.of(SubflowRequestConstants.AGENT_JOB_ID, agentJobId));

        when(jobService.getJob(subflowJobId)).thenReturn(subflowJob);

        TaskExecution lastTaskExecution = new TaskExecution();
        FileEntry callableResponseFileEntry = new FileEntry("callable.json", "/tmp/callable.json");

        lastTaskExecution.setOutput(callableResponseFileEntry);
        lastTaskExecution.putMetadata(MetadataConstants.CALLABLE_RESPONSE, true);

        when(taskExecutionService.fetchLastJobTaskExecution(subflowJobId)).thenReturn(Optional.of(lastTaskExecution));

        // The raw stored output is a Map shaped like a CallableResponse record -- ConvertUtils.convertValue will
        // materialize a CallableResponse whose output() is the inner map.

        Map<String, Object> callableResponseMap = Map.of(
            "output", Map.of("approved", true, "approver", "alice"));

        when(taskFileStorage.readTaskExecutionOutput(callableResponseFileEntry)).thenReturn(callableResponseMap);

        Job agentJob = new Job();

        agentJob.setId(agentJobId);
        agentJob.setStatus(Job.Status.STOPPED);
        agentJob.setMetadata(Map.of(MetadataConstants.TASK_EXECUTION_RESUME_ID, 1L));

        when(jobService.getJob(agentJobId)).thenReturn(agentJob);

        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(subflowJobId, Job.Status.COMPLETED));

        ArgumentCaptor<Map<String, ?>> resumeDataCaptor = ArgumentCaptor.captor();

        verify(jobFacade).resumeJob(eq(agentJobId), anyLong(), resumeDataCaptor.capture());

        Map<String, ?> resumeData = resumeDataCaptor.getValue();

        // The unwrapped callable output, not the raw job outputs wrapper.

        assertEquals(true, resumeData.get("approved"));
        assertEquals("alice", resumeData.get("approver"));
    }

    /**
     * C2 / I5 (failure path): if {@code CALLABLE_RESPONSE} metadata is present but extraction blows up (schema drift,
     * IO failure), the listener MUST surface this as an LLM-readable error payload, NOT silently fall back to raw job
     * outputs. The previous implementation caught the exception and returned {@code Optional.empty()}, causing the
     * agent to see the wrong payload -- the same silent-corruption failure mode #5055 was filed against.
     */
    @Test
    void testResumeFallsBackToErrorWhenCallableResponseExtractionFails() {
        long subflowJobId = 200L;
        long agentJobId = 100L;

        Job subflowJob = new Job();

        subflowJob.setId(subflowJobId);
        subflowJob.setWorkflowId("wf-99");
        subflowJob.setMetadata(Map.of(SubflowRequestConstants.AGENT_JOB_ID, agentJobId));

        when(jobService.getJob(subflowJobId)).thenReturn(subflowJob);

        TaskExecution lastTaskExecution = new TaskExecution();
        FileEntry callableResponseFileEntry = new FileEntry("callable.json", "/tmp/callable.json");

        lastTaskExecution.setOutput(callableResponseFileEntry);
        lastTaskExecution.putMetadata(MetadataConstants.CALLABLE_RESPONSE, true);

        when(taskExecutionService.fetchLastJobTaskExecution(subflowJobId)).thenReturn(Optional.of(lastTaskExecution));

        // Simulate extraction failure -- e.g., file storage IO error.

        when(taskFileStorage.readTaskExecutionOutput(callableResponseFileEntry))
            .thenThrow(new IllegalStateException("storage IO failure"));

        Job agentJob = new Job();

        agentJob.setId(agentJobId);
        agentJob.setStatus(Job.Status.STOPPED);
        agentJob.setMetadata(Map.of(MetadataConstants.TASK_EXECUTION_RESUME_ID, 1L));

        when(jobService.getJob(agentJobId)).thenReturn(agentJob);

        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(subflowJobId, Job.Status.COMPLETED));

        ArgumentCaptor<Map<String, ?>> resumeDataCaptor = ArgumentCaptor.captor();

        verify(jobFacade).resumeJob(eq(agentJobId), anyLong(), resumeDataCaptor.capture());

        Map<String, ?> resumeData = resumeDataCaptor.getValue();

        // The agent must receive an explicit error payload -- NOT a silently-substituted raw job-outputs map.
        // Regression guard against the C2 silent-corruption failure mode.

        assertEquals(
            true, resumeData.containsKey("error"),
            "On extraction failure the agent must be resumed with an error payload, not a silent fallback");

        Object errorMessage = resumeData.get("error");

        assertEquals(String.class, errorMessage.getClass());

        String errorString = (String) errorMessage;

        // The error must mention the storage failure so the LLM has something to reason about.

        assertEquals(
            true, errorString.contains("storage IO failure"),
            "Expected the underlying extraction failure cause in the error payload, got: " + errorString);
    }

    /**
     * S2: the failure-path error payload must include the sub-workflow's actual failure cause from
     * {@link Job#getError()}, not just "Sub-workflow '$base64-id' failed.". The LLM needs the reason to reason about
     * the failure.
     */
    @Test
    void testFailedResumeIncludesExecutionErrorMessage() {
        long subflowJobId = 200L;
        long agentJobId = 100L;

        Job subflowJob = new Job();

        subflowJob.setId(subflowJobId);
        subflowJob.setWorkflowId("wf-99");
        subflowJob.setMetadata(Map.of(SubflowRequestConstants.AGENT_JOB_ID, agentJobId));
        subflowJob.setError(new ExecutionError("approval task threw", List.of()));

        when(jobService.getJob(subflowJobId)).thenReturn(subflowJob);

        Job agentJob = new Job();

        agentJob.setId(agentJobId);
        agentJob.setStatus(Job.Status.STOPPED);
        agentJob.setMetadata(Map.of(MetadataConstants.TASK_EXECUTION_RESUME_ID, 1L));

        when(jobService.getJob(agentJobId)).thenReturn(agentJob);

        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(subflowJobId, Job.Status.FAILED));

        ArgumentCaptor<Map<String, ?>> resumeDataCaptor = ArgumentCaptor.captor();

        verify(jobFacade).resumeJob(eq(agentJobId), anyLong(), resumeDataCaptor.capture());

        String errorString = (String) resumeDataCaptor.getValue()
            .get("error");

        assertEquals(
            true, errorString.contains("approval task threw"),
            "Expected the ExecutionError message in the failed-resume payload, got: " + errorString);
    }
}
