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

package com.bytechef.atlas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SuspendedJobsTest {

    @Test
    void testStoppedJobWithResumeIdIsSuspended() {
        Job job = createJob(Job.Status.STOPPED, Map.of(SuspendedJobs.JOB_RESUME_ID, "resume-id"));

        assertThat(SuspendedJobs.isSuspended(job)).isTrue();
        assertThat(SuspendedJobs.canRunTasks(job)).isTrue();
    }

    @Test
    void testStoppedJobWithoutResumeIdIsNotSuspended() {
        Job job = createJob(Job.Status.STOPPED, Map.of());

        assertThat(SuspendedJobs.isSuspended(job)).isFalse();
        assertThat(SuspendedJobs.canRunTasks(job)).isFalse();
    }

    @Test
    void testStartedJobCanRunTasks() {
        Job job = createJob(Job.Status.STARTED, Map.of());

        assertThat(SuspendedJobs.isSuspended(job)).isFalse();
        assertThat(SuspendedJobs.canRunTasks(job)).isTrue();
    }

    @Test
    void testEndedJobsCannotRunTasksEvenWithAResumeId() {
        for (Job.Status status : new Job.Status[] {
            Job.Status.CANCELLED, Job.Status.COMPLETED, Job.Status.FAILED
        }) {
            Job job = createJob(status, Map.of(SuspendedJobs.JOB_RESUME_ID, "resume-id"));

            assertThat(SuspendedJobs.canRunTasks(job)).isFalse();
        }
    }

    @Test
    void testSuspendedTaskExecutionIdIsThePendingSuspendsTaskExecution() {
        Job job = createJob(
            Job.Status.STARTED,
            Map.of(SuspendedJobs.JOB_RESUME_ID, "resume-id", SuspendedJobs.TASK_EXECUTION_RESUME_ID, 7));

        assertThat(SuspendedJobs.getSuspendedTaskExecutionId(job)).isEqualTo(7L);
    }

    @Test
    void testSuspendedTaskExecutionIdIsNullOnceTheSuspendWasConsumed() {
        Job job = createJob(Job.Status.STARTED, Map.of(SuspendedJobs.TASK_EXECUTION_RESUME_ID, 7L));

        assertThat(SuspendedJobs.getSuspendedTaskExecutionId(job)).isNull();
    }

    private static Job createJob(Job.Status status, Map<String, Object> metadata) {
        Job job = new Job();

        job.setMetadata(metadata);
        job.setStatus(status);

        return job;
    }
}
