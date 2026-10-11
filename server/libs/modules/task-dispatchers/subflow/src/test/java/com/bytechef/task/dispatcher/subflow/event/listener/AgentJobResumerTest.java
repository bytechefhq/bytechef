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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class AgentJobResumerTest {

    private static final long AGENT_JOB_ID = 100L;
    private static final String AGENT_JOB_RESUME_ID = "agent-resume-id";
    private static final SubflowOutcome SUBFLOW_OUTCOME = new SubflowOutcome.Completed("done");
    private static final long TASK_EXECUTION_RESUME_ID = 7L;

    @Mock
    private JobFacade jobFacade;

    @Mock
    private JobService jobService;

    @Test
    void testResumesTheAgentAndConsumesTheSuspend() {
        stubAgentJob(Job.Status.STOPPED, waitingMetadata());

        boolean resumed = new AgentJobResumer(jobFacade, jobService).resume(
            AGENT_JOB_ID, AGENT_JOB_RESUME_ID, SUBFLOW_OUTCOME);

        assertThat(resumed).isTrue();

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobService).update(jobCaptor.capture());

        Map<String, Object> updatedMetadata = new HashMap<>(
            jobCaptor.getValue()
                .getMetadata());

        assertThat(updatedMetadata).containsEntry(MetadataConstants.CONSUMED_JOB_RESUME_ID, AGENT_JOB_RESUME_ID)
            .doesNotContainKey(SubflowRequestConstants.LAUNCHED_SUBFLOW_RESUME_ID);

        verify(jobFacade).resumeJob(
            AGENT_JOB_ID, TASK_EXECUTION_RESUME_ID, Map.of("status", "completed", "result", "done"));
    }

    @Test
    void testDoesNotResumeAnAgentThatIsNotStopped() {
        stubAgentJob(Job.Status.STARTED, waitingMetadata());

        assertNotResumed();
    }

    @Test
    void testDoesNotResumeAnAgentWaitingOnAnotherSuspend() {
        Map<String, Object> metadata = waitingMetadata();

        metadata.put(MetadataConstants.JOB_RESUME_ID, "another-resume-id");

        stubAgentJob(Job.Status.STOPPED, metadata);

        assertNotResumed();
    }

    @Test
    void testDoesNotResumeAnAlreadyConsumedSuspend() {
        Map<String, Object> metadata = waitingMetadata();

        metadata.put(MetadataConstants.CONSUMED_JOB_RESUME_ID, AGENT_JOB_RESUME_ID);

        stubAgentJob(Job.Status.STOPPED, metadata);

        assertNotResumed();
    }

    @Test
    void testDoesNotResumeWithoutATaskExecutionToResume() {
        Map<String, Object> metadata = waitingMetadata();

        metadata.remove(MetadataConstants.TASK_EXECUTION_RESUME_ID);

        stubAgentJob(Job.Status.STOPPED, metadata);

        assertNotResumed();
    }

    @Test
    void testDoesNotResumeWhenAConcurrentUpdateWins() {
        stubAgentJob(Job.Status.STOPPED, waitingMetadata());

        when(jobService.update(any())).thenThrow(new OptimisticLockingFailureException("version conflict"));

        assertNotResumed();
    }

    @Test
    void testReportsNotResumedWhenDispatchingTheResumeFails() {
        stubAgentJob(Job.Status.STOPPED, waitingMetadata());

        doThrow(new IllegalStateException("broker down")).when(jobFacade)
            .resumeJob(anyLong(), anyLong(), any());

        boolean resumed = new AgentJobResumer(jobFacade, jobService).resume(
            AGENT_JOB_ID, AGENT_JOB_RESUME_ID, SUBFLOW_OUTCOME);

        assertThat(resumed).isFalse();
    }

    private void assertNotResumed() {
        boolean resumed = new AgentJobResumer(jobFacade, jobService).resume(
            AGENT_JOB_ID, AGENT_JOB_RESUME_ID, SUBFLOW_OUTCOME);

        assertThat(resumed).isFalse();

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), any());
    }

    private void stubAgentJob(Job.Status status, Map<String, Object> metadata) {
        Job agentJob = new Job();

        agentJob.setId(AGENT_JOB_ID);
        agentJob.setStatus(status);
        agentJob.setMetadata(metadata);

        when(jobService.getJob(AGENT_JOB_ID)).thenReturn(agentJob);
    }

    private static Map<String, Object> waitingMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put(MetadataConstants.JOB_RESUME_ID, AGENT_JOB_RESUME_ID);
        metadata.put(MetadataConstants.TASK_EXECUTION_RESUME_ID, TASK_EXECUTION_RESUME_ID);
        metadata.put(SubflowRequestConstants.LAUNCHED_SUBFLOW_RESUME_ID, AGENT_JOB_RESUME_ID);

        return metadata;
    }
}
