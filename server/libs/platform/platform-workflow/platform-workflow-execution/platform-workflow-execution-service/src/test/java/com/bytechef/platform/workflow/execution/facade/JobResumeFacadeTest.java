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

package com.bytechef.platform.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import com.bytechef.tenant.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class JobResumeFacadeTest {

    private static final long JOB_ID = 42L;
    private static final long TASK_EXECUTION_ID = 7L;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private JobFacade jobFacade;

    @Mock
    private JobService jobService;

    @Mock
    private TaskExecutionService taskExecutionService;

    private JobResumeFacadeImpl jobResumeFacade;
    private SimpleMeterRegistry meterRegistry;

    static {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        MapUtils.setObjectMapper(objectMapper);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // Unconfigured (no secret, not required) -> resolveInnerToken passes the legacy token through unchanged.
        ApprovalTokensImpl approvalTokens = new ApprovalTokensImpl(
            Clock.systemUTC(), null, List.of(), Duration.ofDays(30), Duration.ofSeconds(60), false);

        meterRegistry = new SimpleMeterRegistry();

        ObjectProvider<MeterRegistry> meterRegistryObjectProvider = mock(ObjectProvider.class);

        lenient().when(meterRegistryObjectProvider.getIfAvailable())
            .thenReturn(meterRegistry);

        // Default: the run's resume claim succeeds. The lost-claim case overrides this to false.
        lenient().when(jobService.tryClaimResume(any()))
            .thenReturn(true);

        jobResumeFacade = new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokens, jobFacade, jobService, meterRegistryObjectProvider,
            taskExecutionService);
    }

    @Test
    public void testResumeJobReturnsInvalidIdForUnparseableToken() {
        JobResumeOutcome outcome = jobResumeFacade.resumeJob("not-a-token", Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobReturnsGoneWhenJobNotStopped() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.COMPLETED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobReturnsInvalidIdWhenStoredMetadataMissing() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, null);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobReturnsInvalidIdWhenUuidMismatch() {
        JobResumeId suppliedJobResumeId = JobResumeId.of(JOB_ID);
        JobResumeId storedJobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, storedJobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(suppliedJobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobReturnsInvalidIdWhenTenantMismatch() {
        String currentTenantId = TenantContext.getCurrentTenantId();
        UUID sharedUuid = UUID.randomUUID();

        String suppliedToken = encodeToken(currentTenantId, JOB_ID, sharedUuid);
        String storedToken = encodeToken(currentTenantId + "-other", JOB_ID, sharedUuid);

        Job job = jobOf(Job.Status.STOPPED, storedToken);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(suppliedToken, Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobReturnsOkWhenTokenMatches() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        Map<String, Object> data = Map.of("foo", "bar");

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), data);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        verify(jobFacade).resumeJob(JOB_ID, TASK_EXECUTION_ID, data);
        verify(applicationEventPublisher).publishEvent(any(JobResumedEvent.class));

        // No "approved" key (ask-user-question style resume) -> no approval-resolution counter.
        assertThat(meterRegistry.find("bytechef_approval_resolution")
            .counter()).isNull();
    }

    @Test
    public void testResumeJobReturnsGoneWhenTheClaimIsLostToAConcurrentResolutionOrExpiry() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(jobService.tryClaimResume(any())).thenReturn(false);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("approved", true));

        // The loser of a concurrent resolution — or a resume that raced the expiry sweep flipping the run to FAILED —
        // must be told GONE, with no resume dispatch, no resumed event, and no approval-resolution counter increment.
        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), any());
        verify(applicationEventPublisher, never()).publishEvent(any(JobResumedEvent.class));

        assertThat(meterRegistry.find("bytechef_approval_resolution")
            .counter()).isNull();
    }

    @Test
    public void testResumeJobCountsApprovalResolution() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            jobResumeId.toString(), Map.of("approved", false, "comment", "not now"));

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        assertThat(meterRegistry.counter("bytechef_approval_resolution", "approved", "false")
            .count()).isEqualTo(1.0);
    }

    @Test
    public void testResumeJobWithNullDataDoesNotThrow() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        // A GET or body-less POST to /job/resume passes null data; it must normalize to an empty map, not NPE inside
        // the transaction (which would roll back an already-accepted resume).
        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), null);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        verify(jobFacade).resumeJob(eq(JOB_ID), eq(TASK_EXECUTION_ID), anyMap());
    }

    @Test
    public void testResumeJobStampsVerifiedApprovedBy() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            jobResumeId.toString(), Map.of("approved", true), "@jane");

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.captor();

        verify(jobFacade).resumeJob(eq(JOB_ID), eq(TASK_EXECUTION_ID), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsEntry("approvedBy", "@jane");
    }

    @Test
    public void testResumeJobStripsSpoofedApprovedByWhenNoVerifiedIdentity() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            jobResumeId.toString(), Map.of("approved", true, "approvedBy", "@attacker"));

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.captor();

        verify(jobFacade).resumeJob(eq(JOB_ID), eq(TASK_EXECUTION_ID), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).doesNotContainKey("approvedBy");
    }

    @Test
    public void testResumeJobReturnsGoneWhenSuspendExpired() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.putMetadata(
            "suspend",
            Map.of("expiresAt", java.time.Instant.now()
                .minusSeconds(60)
                .toEpochMilli()));

        when(taskExecutionService.getTaskExecution(TASK_EXECUTION_ID)).thenReturn(taskExecution);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());

        assertThat(meterRegistry.counter("bytechef_approval_expired", "source", "resume")
            .count()).isEqualTo(1.0);
    }

    @Test
    public void testResumeJobProceedsWhenSuspendNotExpired() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.putMetadata(
            "suspend",
            Map.of("expiresAt", java.time.Instant.now()
                .plusSeconds(3600)
                .toEpochMilli()));

        when(taskExecutionService.getTaskExecution(TASK_EXECUTION_ID)).thenReturn(taskExecution);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
    }

    private static Job jobOf(Job.Status status, String storedJobResumeIdString) {
        Job job = new Job(JOB_ID);

        job.setStatus(status);

        if (storedJobResumeIdString != null) {
            Map<String, Object> metadata = new HashMap<>();

            metadata.put(MetadataConstants.JOB_RESUME_ID, storedJobResumeIdString);
            metadata.put(MetadataConstants.TASK_EXECUTION_RESUME_ID, TASK_EXECUTION_ID);

            job.setMetadata(metadata);
        }

        return job;
    }

    private static String encodeToken(String tenantId, long jobId, UUID uuid) {
        return EncodingUtils.base64EncodeToString(tenantId + ":" + jobId + ":" + uuid);
    }
}
