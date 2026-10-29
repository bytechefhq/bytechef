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
    void testIsSuspendedWhenStoppedWithResumeId() {
        assertThat(SuspendedJobs.isSuspended(createJob(Job.Status.STOPPED, true))).isTrue();
    }

    @Test
    void testIsSuspendedWhenStoppedWithoutResumeId() {
        assertThat(SuspendedJobs.isSuspended(createJob(Job.Status.STOPPED, false))).isFalse();
    }

    @Test
    void testIsSuspendedWhenStartedWithResumeId() {
        assertThat(SuspendedJobs.isSuspended(createJob(Job.Status.STARTED, true))).isFalse();
    }

    @Test
    void testIsStoppedByUserWhenStoppedWithoutResumeId() {
        assertThat(SuspendedJobs.isStoppedByUser(createJob(Job.Status.STOPPED, false))).isTrue();
    }

    @Test
    void testIsStoppedByUserWhenSuspended() {
        assertThat(SuspendedJobs.isStoppedByUser(createJob(Job.Status.STOPPED, true))).isFalse();
    }

    @Test
    void testIsStoppedByUserWhenWaitingForApproval() {
        Job job = createJob(Job.Status.STOPPED, false);

        job.setMetadata(Map.of(SuspendedJobs.WAIT_FOR_APPROVAL, true));

        assertThat(SuspendedJobs.isStoppedByUser(job)).isFalse();
    }

    @Test
    void testIsStoppedByUserWhenCompleted() {
        assertThat(SuspendedJobs.isStoppedByUser(createJob(Job.Status.COMPLETED, false))).isFalse();
    }

    private static Job createJob(Job.Status status, boolean withResumeId) {
        Job job = new Job();

        job.setStatus(status);

        if (withResumeId) {
            job.setMetadata(Map.of(SuspendedJobs.JOB_RESUME_ID, "abc"));
        }

        return job;
    }
}
