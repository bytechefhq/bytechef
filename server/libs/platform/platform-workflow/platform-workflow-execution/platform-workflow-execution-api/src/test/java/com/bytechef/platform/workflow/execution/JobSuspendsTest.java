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

package com.bytechef.platform.workflow.execution;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.platform.component.constant.MetadataConstants;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class JobSuspendsTest {

    @Test
    void testATaskExecutionWithAJobResumeIdIsSuspending() {
        TaskExecution suspendingTaskExecution = new TaskExecution();

        suspendingTaskExecution.putMetadata(MetadataConstants.JOB_RESUME_ID, "resume-1");

        assertThat(JobSuspends.isSuspending(suspendingTaskExecution)).isTrue();
        assertThat(JobSuspends.isSuspending(new TaskExecution())).isFalse();
    }

    @Test
    void testAStoppedJobSuspendedForASubflowIsAwaitingTheSubflow() {
        Job job = job(
            Job.Status.STOPPED,
            Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1", MetadataConstants.SUBFLOW_RESUME, true));

        assertThat(JobSuspends.isAwaitingSubflow(job)).isTrue();
    }

    @Test
    void testAStoppedJobSuspendedForAWaitIsNotAwaitingASubflow() {
        Job job = job(
            Job.Status.STOPPED,
            Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1", MetadataConstants.SUBFLOW_RESUME, false));

        assertThat(JobSuspends.isAwaitingSubflow(job)).isFalse();
    }

    @Test
    void testAStoppedJobWhoseSubflowResumeIsInFlightIsStillAwaitingTheSubflow() {
        Job job = job(
            Job.Status.STOPPED,
            Map.of(
                MetadataConstants.JOB_RESUME_ID, "resume-1", MetadataConstants.SUBFLOW_RESUME, true,
                MetadataConstants.CONSUMED_JOB_RESUME_ID, "resume-1"));

        assertThat(JobSuspends.isAwaitingSubflow(job)).isTrue();
    }

    @Test
    void testAJobAlreadyResumedFromASubflowSuspendWasSuspendedForASubflow() {
        Job job = job(Job.Status.STARTED, Map.of(MetadataConstants.SUBFLOW_RESUME, true));

        assertThat(JobSuspends.isSuspendedForSubflow(job)).isTrue();
    }

    @Test
    void testAJobSuspendedForAWaitWasNotSuspendedForASubflow() {
        Job job = job(
            Job.Status.STOPPED,
            Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1", MetadataConstants.SUBFLOW_RESUME, false));

        assertThat(JobSuspends.isSuspendedForSubflow(job)).isFalse();
    }

    @Test
    void testARunningJobIsNotAwaitingASubflow() {
        Job job = job(
            Job.Status.STARTED,
            Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1", MetadataConstants.SUBFLOW_RESUME, true));

        assertThat(JobSuspends.isAwaitingSubflow(job)).isFalse();
    }

    private static Job job(Job.Status status, Map<String, ?> metadata) {
        Job job = new Job();

        job.setMetadata(metadata);
        job.setStatus(status);

        return job;
    }
}
