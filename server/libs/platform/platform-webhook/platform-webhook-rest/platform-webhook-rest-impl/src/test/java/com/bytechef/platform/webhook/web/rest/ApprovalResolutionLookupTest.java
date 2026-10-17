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

package com.bytechef.platform.webhook.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.webhook.web.rest.ApprovalResolutionLookup.Resolution;
import com.bytechef.platform.workflow.execution.JobResumeId;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApprovalResolutionLookupTest {

    private static final long JOB_ID = 42L;
    private static final String RESUME_ID = JobResumeId.of("public", JOB_ID)
        .toString();
    private static final String OTHER_RESUME_ID = JobResumeId.of("public", JOB_ID)
        .toString();

    private final JobService jobService = mock(JobService.class);
    private final ApprovalResolutionLookup approvalResolutionLookup = new ApprovalResolutionLookup(jobService, null);

    @Test
    void testDecidedApprovalIsResolvedEvenWhenTheRunFailedLater() {
        givenJob(Job.Status.FAILED, Map.of(MetadataConstants.CONSUMED_JOB_RESUME_ID, RESUME_ID));

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.RESOLVED);
    }

    @Test
    void testCompletedRunIsResolved() {
        givenJob(Job.Status.COMPLETED, Map.of());

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.RESOLVED);
    }

    @Test
    void testRunningRunIsResolved() {
        givenJob(Job.Status.STARTED, Map.of());

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.RESOLVED);
    }

    @Test
    void testRunStillWaitingOnThisApprovalIsUnavailable() {
        givenJob(Job.Status.STOPPED, Map.of(MetadataConstants.JOB_RESUME_ID, RESUME_ID));

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.UNAVAILABLE);
    }

    @Test
    void testRunWaitingOnALaterSuspendIsResolved() {
        givenJob(Job.Status.STOPPED, Map.of(MetadataConstants.JOB_RESUME_ID, OTHER_RESUME_ID));

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.RESOLVED);
    }

    @Test
    void testApprovalFailedByItsExpiryIsExpired() {
        givenJob(
            Job.Status.FAILED,
            Map.of(
                MetadataConstants.CONSUMED_JOB_RESUME_ID, RESUME_ID, MetadataConstants.EXPIRED_JOB_RESUME_ID,
                RESUME_ID));

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.EXPIRED);
    }

    @Test
    void testExpiredApprovalOfARestartedRunIsExpired() {
        givenJob(
            Job.Status.STOPPED,
            Map.of(
                MetadataConstants.JOB_RESUME_ID, OTHER_RESUME_ID, MetadataConstants.EXPIRED_JOB_RESUME_ID,
                RESUME_ID));

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.EXPIRED);
    }

    @Test
    void testApprovalOfAStoppedRunIsUnavailable() {
        givenJob(
            Job.Status.CANCELLED,
            Map.of(
                MetadataConstants.CONSUMED_JOB_RESUME_ID, RESUME_ID, MetadataConstants.CANCELLED_JOB_RESUME_ID,
                RESUME_ID));

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.UNAVAILABLE);
    }

    @Test
    void testFailedRunIsUnavailable() {
        givenJob(Job.Status.FAILED, Map.of());

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.UNAVAILABLE);
    }

    @Test
    void testCancelledRunIsUnavailable() {
        givenJob(Job.Status.CANCELLED, Map.of());

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.UNAVAILABLE);
    }

    @Test
    void testMissingRunIsUnavailable() {
        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.empty());

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.UNAVAILABLE);
    }

    @Test
    void testUnreadableRunIsUnavailable() {
        when(jobService.fetchJob(JOB_ID)).thenThrow(new IllegalStateException("database unavailable"));

        assertThat(approvalResolutionLookup.describe(RESUME_ID)).isEqualTo(Resolution.UNAVAILABLE);
    }

    @Test
    void testMalformedResumeIdIsUnavailable() {
        assertThat(approvalResolutionLookup.describe("not-a-resume-id")).isEqualTo(Resolution.UNAVAILABLE);
    }

    private void givenJob(Job.Status status, Map<String, ?> metadata) {
        Job job = new Job(JOB_ID);

        job.setStatus(status);
        job.setMetadata(metadata);

        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.of(job));
    }
}
