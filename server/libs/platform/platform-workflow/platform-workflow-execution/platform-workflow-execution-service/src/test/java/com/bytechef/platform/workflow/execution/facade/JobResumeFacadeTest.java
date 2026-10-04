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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import com.bytechef.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class JobResumeFacadeTest {

    private static final long JOB_ID = 42L;
    private static final long TASK_EXECUTION_ID = 7L;
    private static final String SIGNING_SECRET = Base64.getEncoder()
        .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    private static final String TENANT_ID = "000001";

    private final ApprovalTokens approvalTokens = new ApprovalTokensImpl(
        Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), false);

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private JobFacade jobFacade;

    @Mock
    private JobService jobService;

    private JobResumeFacadeImpl jobResumeFacade;

    private final List<String> transactionTenantIds = new ArrayList<>();

    private final TransactionOperations transactionOperations = new TransactionOperations() {

        @Override
        public <T> T execute(TransactionCallback<T> transactionCallback) {
            transactionTenantIds.add(TenantContext.getCurrentTenantId());

            return transactionCallback.doInTransaction(new SimpleTransactionStatus());
        }
    };

    static {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        MapUtils.setObjectMapper(objectMapper);
    }

    @BeforeEach
    void setUp() {
        jobResumeFacade = new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider(approvalTokens), jobFacade, jobService,
            transactionOperations);
    }

    @Test
    public void testResumeJobReturnsOkForSignedToken() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));
        Map<String, Object> data = Map.of("foo", "bar");

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(signedToken, data);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        verify(jobFacade).resumeJob(JOB_ID, TASK_EXECUTION_ID, data);
        verify(applicationEventPublisher).publishEvent(new JobResumedEvent(signedToken));
    }

    @Test
    public void testResumeJobReturnsInvalidIdForTamperedSignedToken() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        String tamperedToken =
            tamperSignature(approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1)));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(tamperedToken, Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobService, never()).getJob(anyLong());
        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobReturnsInvalidIdForUnsignedTokenWhenSigningRequired() {
        ApprovalTokens requiredApprovalTokens = new ApprovalTokensImpl(
            Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), true);

        JobResumeFacadeImpl requiredJobResumeFacade = new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider(requiredApprovalTokens), jobFacade, jobService,
            transactionOperations);

        JobResumeOutcome outcome = requiredJobResumeFacade.resumeJob(JobResumeId.of(JOB_ID)
            .toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobService, never()).getJob(anyLong());
    }

    @Test
    public void testResumeExpiredJobAcceptsTheStoredUnsignedIdWhenSigningRequired() {
        ApprovalTokens requiredApprovalTokens = new ApprovalTokensImpl(
            Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), true);

        JobResumeFacadeImpl requiredJobResumeFacade = new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider(requiredApprovalTokens), jobFacade, jobService,
            transactionOperations);

        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STOPPED, jobResumeId.toString()));

        JobResumeOutcome outcome = requiredJobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
    }

    @Test
    public void testResumeJobAcceptsRawTokenWhenApprovalTokensAbsent() {
        JobResumeFacadeImpl unsignedJobResumeFacade = new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider(null), jobFacade, jobService, transactionOperations);

        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = unsignedJobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
    }

    @Test
    public void testResumeJobOpensItsTransactionUnderTheTenantOfTheResumeId() {
        JobResumeId jobResumeId = TenantContext.callWithTenantId(TENANT_ID, () -> JobResumeId.of(JOB_ID));

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.COMPLETED, jobResumeId.toString()));

        jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(transactionTenantIds).containsExactly(TENANT_ID);
    }

    @Test
    public void testResumeExpiredJobOpensItsTransactionUnderTheTenantOfTheResumeId() {
        JobResumeId jobResumeId = TenantContext.callWithTenantId(TENANT_ID, () -> JobResumeId.of(JOB_ID));

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.COMPLETED, jobResumeId.toString()));

        jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(transactionTenantIds).containsExactly(TENANT_ID);
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
    public void testResumeJobReturnsNotYetSuspendedWhileJobIsStillRunning() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STARTED, null);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobStreamingReturnsNotYetSuspendedWhileJobIsStillRunning() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STARTED, null, true);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            jobResumeId.toString(), Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
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
        assertThat(job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isEqualTo(jobResumeId.toString());

        InOrder inOrder = inOrder(jobService, jobFacade);

        inOrder.verify(jobService)
            .update(job);
        inOrder.verify(jobFacade)
            .resumeJob(JOB_ID, TASK_EXECUTION_ID, data);
        verify(applicationEventPublisher).publishEvent(any(JobResumedEvent.class));
    }

    @Test
    public void testResumeJobReturnsGoneForAnAlreadyConsumedResumeIdWhileTheJobRuns() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STARTED, null);

        job.setMetadata(Map.of(MetadataConstants.CONSUMED_JOB_RESUME_ID, jobResumeId.toString()));

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobService, never()).update(any());
        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
    }

    @Test
    public void testResumeJobReturnsGoneForAnAlreadyConsumedResumeIdAfterTheJobSuspendedAgain() {
        JobResumeId consumedJobResumeId = JobResumeId.of(JOB_ID);
        JobResumeId currentJobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, currentJobResumeId.toString());

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.put(MetadataConstants.CONSUMED_JOB_RESUME_ID, consumedJobResumeId.toString());

        job.setMetadata(metadata);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(consumedJobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
    }

    @Test
    public void testResumeJobReturnsJobFailedWhenTheJobFailed() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.FAILED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.JOB_FAILED);

        verify(jobService, never()).update(any());
        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
    }

    @Test
    public void testResumeJobIgnoresTheStreamingResumeFlag() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString(), false);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        Map<String, Object> data = Map.of("foo", "bar");

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), data);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        verify(jobFacade).resumeJob(JOB_ID, TASK_EXECUTION_ID, data);
    }

    @Test
    public void testResumeJobStreamingCallsJobIdConsumerBeforeResumingTheJob() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString(), true);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);
        Map<String, Object> data = Map.of("foo", "bar");

        JobResumeOutcome outcome =
            jobResumeFacade.resumeJobStreaming(jobResumeId.toString(), data, jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        InOrder inOrder = inOrder(jobService, jobIdConsumer, jobFacade, applicationEventPublisher);

        inOrder.verify(jobService)
            .update(job);
        inOrder.verify(jobIdConsumer)
            .accept(JOB_ID);
        inOrder.verify(jobFacade)
            .resumeJob(JOB_ID, TASK_EXECUTION_ID, data);
        inOrder.verify(applicationEventPublisher)
            .publishEvent(any(JobResumedEvent.class));
    }

    @Test
    public void testResumeJobStreamingDoesNotCallJobIdConsumerForAnInvalidId() {
        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming("not-a-token", Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
    }

    @Test
    public void testResumeJobStreamingDoesNotCallJobIdConsumerForATokenMismatch() {
        JobResumeId suppliedJobResumeId = JobResumeId.of(JOB_ID);
        JobResumeId storedJobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, storedJobResumeId.toString(), true);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            suppliedJobResumeId.toString(), Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
    }

    @Test
    public void testResumeJobStreamingDoesNotCallJobIdConsumerWhenJobIsGone() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.COMPLETED, jobResumeId.toString(), true);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            jobResumeId.toString(), Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
    }

    @Test
    public void testResumeJobStreamingReturnsStreamingNotAllowedWithoutStreamingResumeMetadata() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            jobResumeId.toString(), Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.STREAMING_NOT_ALLOWED);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobStreamingReturnsStreamingNotAllowedWhenStreamingResumeIsFalse() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString(), false);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            jobResumeId.toString(), Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.STREAMING_NOT_ALLOWED);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeExpiredJobResumesAJobStillWaitingOnTheSuspendAndConsumesTheResumeId() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
        assertThat(job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isEqualTo(jobResumeId.toString());

        InOrder inOrder = inOrder(jobService, jobFacade, applicationEventPublisher);

        inOrder.verify(jobService)
            .update(job);
        inOrder.verify(jobFacade)
            .resumeJob(JOB_ID, TASK_EXECUTION_ID, null);
        inOrder.verify(applicationEventPublisher)
            .publishEvent(new JobResumedEvent(jobResumeId.toString()));

        verify(jobFacade, never()).resumeJob(JOB_ID);
    }

    @Test
    public void testResumeExpiredJobRestartsTheJobWhenNoSuspendedTaskExecutionIsStored() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.remove(MetadataConstants.TASK_EXECUTION_RESUME_ID);

        job.setMetadata(metadata);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        assertThat(jobResumeFacade.resumeExpiredJob(jobResumeId.toString())).isEqualTo(JobResumeOutcome.OK);

        verify(jobFacade).resumeJob(JOB_ID);
    }

    @Test
    public void testResumeExpiredJobReturnsNotYetSuspendedWhileTheJobIsStillRunning() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STARTED, null));

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);

        verify(jobService, never()).update(any());
        verify(jobFacade, never()).resumeJob(anyLong());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeExpiredJobDoesNotRestartAFailedJob() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.FAILED, jobResumeId.toString()));

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.JOB_FAILED);

        verify(jobService, never()).update(any());
        verify(jobFacade, never()).resumeJob(anyLong());
    }

    @Test
    public void testResumeExpiredJobReturnsInvalidIdForAStoppedJobWithoutAStoredResumeId() {
        JobResumeId expiredJobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STOPPED, null));

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(expiredJobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobService, never()).update(any());
        verify(jobFacade, never()).resumeJob(anyLong());
    }

    @Test
    public void testResumeExpiredJobDoesNotResumeAJobSuspendedAgainOnANewResumeId() {
        JobResumeId expiredJobResumeId = JobResumeId.of(JOB_ID);
        JobResumeId currentJobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STOPPED, currentJobResumeId.toString()));

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(expiredJobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeJob(anyLong());
    }

    @Test
    public void testResumeExpiredJobDoesNotResumeAnAlreadyAnsweredSuspend() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STARTED, null);

        job.setMetadata(Map.of(MetadataConstants.CONSUMED_JOB_RESUME_ID, jobResumeId.toString()));

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeJob(anyLong());
    }

    @Test
    public void testResumeJobReturnsGoneForAnAnswerAfterTheSuspendExpired() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        job.setStatus(Job.Status.STARTED);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("answer", "late"));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
    }

    @Test
    public void testResumeExpiredJobReturnsInvalidIdForAnUnparseableId() {
        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob("not-a-token");

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobService, never()).getJob(anyLong());
    }

    private static Job jobOf(Job.Status status, String storedJobResumeIdString, boolean streamingResume) {
        Job job = jobOf(status, storedJobResumeIdString);

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.put(MetadataConstants.STREAMING_RESUME, streamingResume);

        job.setMetadata(metadata);

        return job;
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

    private static ObjectProvider<ApprovalTokens> approvalTokensProvider(ApprovalTokens approvalTokens) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        if (approvalTokens != null) {
            beanFactory.addBean("approvalTokens", approvalTokens);
        }

        return beanFactory.getBeanProvider(ApprovalTokens.class);
    }

    private static String tamperSignature(String signedToken) {
        int signatureStart = signedToken.lastIndexOf('.') + 1;
        char replacement = signedToken.charAt(signatureStart) == 'A' ? 'B' : 'A';

        return signedToken.substring(0, signatureStart) + replacement + signedToken.substring(signatureStart + 1);
    }
}
