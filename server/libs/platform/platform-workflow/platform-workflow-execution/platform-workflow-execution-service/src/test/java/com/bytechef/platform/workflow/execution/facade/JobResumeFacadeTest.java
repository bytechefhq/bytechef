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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.ApprovalId;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobApprovalClosedEvent;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import com.bytechef.tenant.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class JobResumeFacadeTest {

    private static final long JOB_ID = 42L;
    private static final long TASK_EXECUTION_ID = 7L;
    private static final String SIGNING_SECRET = EncodingUtils.base64EncodeToString("0123456789abcdef0123456789abcdef");
    private static final String TENANT_ID = "000001";

    private final ApprovalTokens approvalTokens = new ApprovalTokensImpl(
        Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), false);

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private JobFacade jobFacade;

    @Mock
    private JobService jobService;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Mock
    private TaskStateService taskStateService;

    private JobResumeFacadeImpl jobResumeFacade;
    private SimpleMeterRegistry meterRegistry;
    private ObjectProvider<MeterRegistry> meterRegistryObjectProvider;

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
    @SuppressWarnings("unchecked")
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        meterRegistryObjectProvider = mock(ObjectProvider.class);

        lenient().when(meterRegistryObjectProvider.getIfAvailable())
            .thenReturn(meterRegistry);

        lenient().when(jobService.tryClaimResume(any()))
            .thenReturn(true);

        lenient().when(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID))
            .thenReturn(Optional.of(taskExecutionOf(Map.of())));

        jobResumeFacade = new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider(approvalTokens), jobFacade, jobService,
            meterRegistryObjectProvider, transactionOperations, taskExecutionService, taskStateService);
    }

    @Test
    void testResumeJobReturnsOkForSignedToken() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));
        Map<String, Object> data = Map.of("foo", "bar");

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(signedToken, data);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        verify(jobFacade).resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, data);
        verify(applicationEventPublisher).publishEvent(new JobResumedEvent(signedToken));
    }

    @Test
    void testResumeJobReturnsInvalidIdForTamperedSignedToken() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        String tamperedToken =
            tamperSignature(approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1)));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(tamperedToken, Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobService, never()).getJob(anyLong());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testResumeJobReturnsInvalidIdForUnsignedTokenWhenSigningRequired() {
        ApprovalTokens requiredApprovalTokens = new ApprovalTokensImpl(
            Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), true);

        JobResumeFacadeImpl requiredJobResumeFacade = new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider(requiredApprovalTokens), jobFacade, jobService,
            meterRegistryObjectProvider, transactionOperations, taskExecutionService, taskStateService);

        JobResumeOutcome outcome = requiredJobResumeFacade.resumeJob(JobResumeId.of(JOB_ID)
            .toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobService, never()).getJob(anyLong());
    }

    @Test
    void testResumeExpiredJobAcceptsTheStoredUnsignedIdWhenSigningRequired() {
        ApprovalTokens requiredApprovalTokens = new ApprovalTokensImpl(
            Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), true);

        JobResumeFacadeImpl requiredJobResumeFacade = new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider(requiredApprovalTokens), jobFacade, jobService,
            meterRegistryObjectProvider, transactionOperations, taskExecutionService, taskStateService);

        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STOPPED, jobResumeId.toString()));

        JobResumeOutcome outcome = requiredJobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
    }

    @Test
    void testResumeJobRejectsAnUnsignedIdForAnApprovalWhenSigningIsNotRequired() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = approvalJobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isNull();

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Test
    void testResumeJobResolvesAnApprovalWithASignedToken() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(approvalJobOf(Job.Status.STOPPED, jobResumeId.toString()));

        String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(signedToken, Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        verify(jobFacade).resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, Map.of("approved", true));
    }

    @Test
    void testResumeJobRejectsAnApprovalWithoutAnApprovedDecision() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = approvalJobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        assertThat(jobResumeFacade.resumeJob(signedToken, Map.of("comment", "looks fine")))
            .isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(jobResumeFacade.resumeJob(signedToken, Map.of("approved", "true")))
            .isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(jobResumeFacade.resumeJob(signedToken, null))
            .isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isNull();

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Test
    void testResumeJobRejectsAnApprovalWhenApprovalTokensAbsent() {
        JobResumeFacadeImpl unsignedJobResumeFacade = new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider(null), jobFacade, jobService,
            meterRegistryObjectProvider, transactionOperations, taskExecutionService, taskStateService);

        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(approvalJobOf(Job.Status.STOPPED, jobResumeId.toString()));

        JobResumeOutcome outcome = unsignedJobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);
    }

    @Test
    void testResumeJobAcceptsRawTokenWhenApprovalTokensAbsent() {
        JobResumeFacadeImpl unsignedJobResumeFacade = new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider(null), jobFacade, jobService,
            meterRegistryObjectProvider, transactionOperations, taskExecutionService, taskStateService);

        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = unsignedJobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
    }

    @Test
    void testResumeJobOpensItsTransactionUnderTheTenantOfTheResumeId() {
        JobResumeId jobResumeId = TenantContext.callWithTenantId(TENANT_ID, () -> JobResumeId.of(JOB_ID));

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.COMPLETED, jobResumeId.toString()));

        jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(transactionTenantIds).containsExactly(TENANT_ID);
    }

    @Test
    void testResumeExpiredJobOpensItsTransactionUnderTheTenantOfTheResumeId() {
        JobResumeId jobResumeId = TenantContext.callWithTenantId(TENANT_ID, () -> JobResumeId.of(JOB_ID));

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.COMPLETED, jobResumeId.toString()));

        jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(transactionTenantIds).containsExactly(TENANT_ID);
    }

    @Test
    void testResumeJobReturnsInvalidIdForUnparseableToken() {
        JobResumeOutcome outcome = jobResumeFacade.resumeJob("not-a-token", Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testResumeJobReturnsGoneWhenJobNotStopped() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.COMPLETED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testResumeJobReturnsNotYetSuspendedWhileJobIsStillRunning() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STARTED, null);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testResumeJobStreamingReturnsNotYetSuspendedWhileJobIsStillRunning() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STARTED, null, true);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            jobResumeId.toString(), Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Test
    void testResumeJobReturnsInvalidIdWhenStoredMetadataMissing() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, null);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testResumeJobReturnsInvalidIdWhenUuidMismatch() {
        JobResumeId suppliedJobResumeId = JobResumeId.of(JOB_ID);
        JobResumeId storedJobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, storedJobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(suppliedJobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testResumeJobReturnsInvalidIdWhenTenantMismatch() {
        String currentTenantId = TenantContext.getCurrentTenantId();
        UUID sharedUuid = UUID.randomUUID();

        String suppliedToken = encodeToken(currentTenantId, JOB_ID, sharedUuid);
        String storedToken = encodeToken(currentTenantId + "-other", JOB_ID, sharedUuid);

        Job job = jobOf(Job.Status.STOPPED, storedToken);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(suppliedToken, Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testResumeJobReturnsOkWhenTokenMatches() {
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
            .resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, data);
        verify(applicationEventPublisher).publishEvent(any(JobResumedEvent.class));
    }

    @Test
    void testResumeJobRejectsAJobWaitingOnASubflowItLaunched() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.put(MetadataConstants.SUBFLOW_RESUME, true);

        job.setMetadata(metadata);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            jobResumeId.toString(), Map.of("status", "completed", "result", "forged"));

        assertThat(outcome).isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);

        verify(jobService, never()).update(any());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Test
    void testResumeJobReturnsGoneForAnAlreadyConsumedResumeIdWhileTheJobRuns() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STARTED, null);

        job.setMetadata(Map.of(MetadataConstants.CONSUMED_JOB_RESUME_ID, jobResumeId.toString()));

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobService, never()).update(any());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Test
    void testResumeJobReturnsGoneForAnAlreadyConsumedResumeIdAfterTheJobSuspendedAgain() {
        JobResumeId consumedJobResumeId = JobResumeId.of(JOB_ID);
        JobResumeId currentJobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, currentJobResumeId.toString());

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.put(MetadataConstants.CONSUMED_JOB_RESUME_ID, consumedJobResumeId.toString());

        job.setMetadata(metadata);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(consumedJobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Test
    void testResumeJobReturnsJobFailedWhenTheJobFailed() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.FAILED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.JOB_FAILED);

        verify(jobService, never()).update(any());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Test
    void testResumeJobIgnoresTheStreamingResumeFlag() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString(), false);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        Map<String, Object> data = Map.of("foo", "bar");

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), data);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        verify(jobFacade).resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, data);
    }

    @Test
    void testResumeJobStreamingCallsJobIdConsumerBeforeResumingTheJob() {
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
            .resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, data);
        inOrder.verify(applicationEventPublisher)
            .publishEvent(any(JobResumedEvent.class));
    }

    @Test
    void testResumeJobStreamingDoesNotCallJobIdConsumerForAnInvalidId() {
        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming("not-a-token", Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Test
    void testResumeJobStreamingDoesNotCallJobIdConsumerForATokenMismatch() {
        JobResumeId suppliedJobResumeId = JobResumeId.of(JOB_ID);
        JobResumeId storedJobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, storedJobResumeId.toString(), true);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            suppliedJobResumeId.toString(), Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Test
    void testResumeJobStreamingDoesNotCallJobIdConsumerWhenJobIsGone() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.COMPLETED, jobResumeId.toString(), true);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            jobResumeId.toString(), Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Test
    void testResumeJobStreamingReturnsStreamingNotAllowedWithoutStreamingResumeMetadata() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            jobResumeId.toString(), Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.STREAMING_NOT_ALLOWED);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testResumeJobStreamingReturnsStreamingNotAllowedWhenStreamingResumeIsFalse() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString(), false);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            jobResumeId.toString(), Map.of(), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.STREAMING_NOT_ALLOWED);

        verify(jobIdConsumer, never()).accept(anyLong());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testResumeJobStreamingResumesAnApprovalOfAStreamedChatRun() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = approvalJobOf(Job.Status.STOPPED, jobResumeId.toString());

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.put(MetadataConstants.STREAMED_EXECUTION, true);
        metadata.put(MetadataConstants.STREAMING_RESUME, false);

        job.setMetadata(metadata);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        LongConsumer jobIdConsumer = mock(LongConsumer.class);

        String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            signedToken, Map.of("approved", true), jobIdConsumer);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        InOrder inOrder = inOrder(jobIdConsumer, jobFacade);

        inOrder.verify(jobIdConsumer)
            .accept(JOB_ID);
        inOrder.verify(jobFacade)
            .resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, Map.of("approved", true));
    }

    @Test
    void testResumeExpiredJobResumesAJobStillWaitingOnTheSuspendAndConsumesTheResumeId() {
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
            .resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, null);
        inOrder.verify(applicationEventPublisher)
            .publishEvent(new JobResumedEvent(jobResumeId.toString()));

        verify(jobFacade, never()).resumeJob(JOB_ID);
    }

    @Test
    void testResumeExpiredJobFailsAnExpiredApprovalInsteadOfResumingIt() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = approvalJobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        Instant expiresAt = Instant.parse("2026-10-17T08:31:52Z");

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.putMetadata(MetadataConstants.SUSPEND, Map.of("expiresAt", expiresAt.toString()));

        when(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID)).thenReturn(Optional.of(taskExecution));

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.JOB_FAILED);
        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(job.getEndDate()).isNotNull();
        assertThat(job.getError()
            .getMessage()).isEqualTo(
                "The pending approval expired at 2026-10-17T08:31:52Z without being resolved");
        assertThat(job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isEqualTo(jobResumeId.toString());
        assertThat(job.getMetadata(MetadataConstants.EXPIRED_JOB_RESUME_ID)).isEqualTo(jobResumeId.toString());

        verify(jobService).update(job);
        verify(jobService, never()).tryClaimResume(any());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
        verify(taskStateService).delete(jobResumeId);
        verify(applicationEventPublisher).publishEvent(new JobApprovalClosedEvent(JOB_ID));
        verify(applicationEventPublisher).publishEvent(any(JobStatusApplicationEvent.class));
        verify(applicationEventPublisher, never()).publishEvent(any(JobResumedEvent.class));

        assertThat(meterRegistry.counter("bytechef_approval_expired", "source", "timer")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testIsApprovalResumeIdTellsAnApprovalFromAWait() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.fetchJob(JOB_ID))
            .thenReturn(Optional.of(approvalJobOf(Job.Status.STOPPED, jobResumeId.toString())))
            .thenReturn(Optional.of(jobOf(Job.Status.STOPPED, jobResumeId.toString())))
            .thenReturn(Optional.empty());

        String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        assertThat(jobResumeFacade.isApprovalResumeId(signedToken)).isTrue();
        assertThat(jobResumeFacade.isApprovalResumeId(jobResumeId.toString())).isFalse();
        assertThat(jobResumeFacade.isApprovalResumeId(jobResumeId.toString())).isFalse();
        assertThat(jobResumeFacade.isApprovalResumeId("not-a-token")).isFalse();
    }

    @Test
    void testCancelApprovalCancelsAJobWaitingOnAnApprovalAndMakesItsLinksGone() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = approvalJobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        assertThat(jobResumeFacade.cancelApproval(JOB_ID)).isTrue();
        assertThat(job.getStatus()).isEqualTo(Job.Status.CANCELLED);
        assertThat(job.getEndDate()).isNotNull();
        assertThat(job.getMetadata(MetadataConstants.CANCELLED_JOB_RESUME_ID)).isEqualTo(jobResumeId.toString());

        verify(jobService).update(job);
        verify(taskStateService).delete(jobResumeId);
        verify(applicationEventPublisher).publishEvent(new JobApprovalClosedEvent(JOB_ID));
        verify(applicationEventPublisher).publishEvent(any(JobStatusApplicationEvent.class));

        assertThat(jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("approved", true)))
            .isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
    }

    @Test
    void testCancelApprovalLeavesAJobThatDoesNotWaitOnAnApproval() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        assertThat(jobResumeFacade.cancelApproval(JOB_ID)).isFalse();
        assertThat(job.getStatus()).isEqualTo(Job.Status.STOPPED);

        verify(jobService, never()).update(any());
    }

    @Test
    void testResumeExpiredJobRestartsTheJobWhenNoSuspendedTaskExecutionIsStored() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.remove(MetadataConstants.TASK_EXECUTION_RESUME_ID);

        job.setMetadata(metadata);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        assertThat(jobResumeFacade.resumeExpiredJob(jobResumeId.toString())).isEqualTo(JobResumeOutcome.OK);

        verify(jobFacade).resumeClaimedJob(JOB_ID, null, null);
    }

    @Test
    void testResumeExpiredJobReturnsGoneWhenTheResumeClaimIsLost() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STOPPED, jobResumeId.toString()));
        when(jobService.tryClaimResume(any())).thenReturn(false);

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
        verify(jobFacade, never()).resumeJob(anyLong());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testResumeExpiredJobReturnsNotYetSuspendedWhileTheJobIsStillRunning() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STARTED, null));

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);

        verify(jobService, never()).update(any());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testResumeExpiredJobDoesNotRestartAFailedJob() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.FAILED, jobResumeId.toString()));

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.JOB_FAILED);

        verify(jobService, never()).update(any());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
    }

    @Test
    void testResumeExpiredJobReturnsInvalidIdForAStoppedJobWithoutAStoredResumeId() {
        JobResumeId expiredJobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STOPPED, null));

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(expiredJobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobService, never()).update(any());
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
    }

    @Test
    void testResumeExpiredJobDoesNotResumeAJobSuspendedAgainOnANewResumeId() {
        JobResumeId expiredJobResumeId = JobResumeId.of(JOB_ID);
        JobResumeId currentJobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STOPPED, currentJobResumeId.toString()));

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(expiredJobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
    }

    @Test
    void testResumeExpiredJobDoesNotResumeAnAlreadyAnsweredSuspend() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STARTED, null);

        job.setMetadata(Map.of(MetadataConstants.CONSUMED_JOB_RESUME_ID, jobResumeId.toString()));

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
    }

    @Test
    void testResumeJobReturnsGoneForAnAnswerAfterTheSuspendExpired() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        jobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        job.setStatus(Job.Status.STARTED);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("answer", "late"));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Test
    void testResumeExpiredJobReturnsInvalidIdForAnUnparseableId() {
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

    @Test
    public void testResumeJobReturnsGoneWhenTheClaimIsLostToAConcurrentResolutionOrExpiry() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(jobService.tryClaimResume(any())).thenReturn(false);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
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

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), null);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        verify(jobFacade).resumeClaimedJob(eq(JOB_ID), eq(TASK_EXECUTION_ID), anyMap());
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

        verify(jobFacade).resumeClaimedJob(eq(JOB_ID), eq(TASK_EXECUTION_ID), dataCaptor.capture());

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

        verify(jobFacade).resumeClaimedJob(eq(JOB_ID), eq(TASK_EXECUTION_ID), dataCaptor.capture());

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
            Map.of("expiresAt", Instant.now()
                .minusSeconds(60)
                .toEpochMilli()));

        when(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID)).thenReturn(Optional.of(taskExecution));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());

        assertThat(meterRegistry.counter("bytechef_approval_expired", "source", "resume")
            .count()).isEqualTo(1.0);
    }

    @Test
    public void testResumeJobFailsTheRunAtOnceWhenItsApprovalExpired() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = approvalJobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        Instant expiresAt = Instant.parse("2026-10-17T11:00:00Z");

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.putMetadata(MetadataConstants.SUSPEND, Map.of("expiresAt", expiresAt.toString()));

        when(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID)).thenReturn(Optional.of(taskExecution));

        String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(signedToken, Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);
        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(job.getError()
            .getMessage()).isEqualTo("The pending approval expired at 2026-10-17T11:00:00Z without being resolved");
        assertThat(job.getMetadata(MetadataConstants.EXPIRED_JOB_RESUME_ID)).isEqualTo(jobResumeId.toString());
        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.FAILED);

        verify(jobService).update(job);
        verify(taskExecutionService).update(taskExecution);
        verify(taskStateService).delete(jobResumeId);
        verify(applicationEventPublisher).publishEvent(new JobApprovalClosedEvent(JOB_ID));
        verify(applicationEventPublisher).publishEvent(any(JobStatusApplicationEvent.class));
        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());

        assertThat(jobResumeFacade.resumeJob(signedToken, Map.of("approved", true)))
            .isEqualTo(JobResumeOutcome.GONE);
    }

    @Test
    public void testResumeJobReturnsGoneForAnApprovalTheSweepExpired() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = approvalJobOf(Job.Status.FAILED, jobResumeId.toString());

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.put(MetadataConstants.EXPIRED_JOB_RESUME_ID, jobResumeId.toString());

        job.setMetadata(metadata);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        assertThat(jobResumeFacade.resumeJob(signedToken, Map.of("approved", true)))
            .isEqualTo(JobResumeOutcome.GONE);

        verify(jobService, never()).update(any());
    }

    @Test
    public void testResumeJobProceedsWhenSuspendNotExpired() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.putMetadata(
            "suspend",
            Map.of("expiresAt", Instant.now()
                .plusSeconds(3600)
                .toEpochMilli()));

        when(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID)).thenReturn(Optional.of(taskExecution));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
    }

    @Test
    public void testResumeJobFailsClosedWhenTheSuspendedTaskExecutionIsMissing() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STOPPED, jobResumeId.toString()));
        when(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID)).thenReturn(Optional.empty());

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        verify(jobService, never()).tryClaimResume(any());
    }

    @Test
    public void testResumeJobFailsClosedWhenTheSuspendExpiryIsUnreadable() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STOPPED, jobResumeId.toString()));

        TaskExecution taskExecution = mock(TaskExecution.class);

        when(taskExecution.getMetadata()).thenThrow(new IllegalStateException("corrupt metadata"));
        when(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID)).thenReturn(Optional.of(taskExecution));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
    }

    @Nested
    class ResumeApproval {

        @Test
        void testResumeApprovalRejectsAnUnsignedToken() {
            String unsignedToken = ApprovalId.of(TENANT_ID, JOB_ID, true)
                .toString();

            JobResumeOutcome outcome = jobResumeFacade.resumeApproval(unsignedToken);

            assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

            verify(jobService, never()).getJob(anyLong());
            verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
            verify(jobFacade, never()).resumeApproval(anyLong(), anyString(), anyBoolean());
        }

        @Test
        void testResumeApprovalRejectsATamperedToken() {
            String tamperedToken = tamperSignature(signedApprovalToken(true));

            JobResumeOutcome outcome = jobResumeFacade.resumeApproval(tamperedToken);

            assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

            verify(jobService, never()).getJob(anyLong());
            verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        }

        @Test
        void testResumeApprovalRejectsAnUnsignedTokenWhenApprovalTokensAbsent() {
            JobResumeFacadeImpl unsignedJobResumeFacade = new JobResumeFacadeImpl(
                applicationEventPublisher, approvalTokensProvider(null), jobFacade, jobService,
                meterRegistryObjectProvider, transactionOperations, taskExecutionService, taskStateService);

            String unsignedToken = ApprovalId.of(TENANT_ID, JOB_ID, true)
                .toString();

            JobResumeOutcome outcome = unsignedJobResumeFacade.resumeApproval(unsignedToken);

            assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

            verify(jobService, never()).getJob(anyLong());
        }

        @Test
        void testResumeApprovalResolvesASuspendedApprovalForASignedToken() {
            JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

            when(jobService.getJob(JOB_ID)).thenReturn(approvalJobOf(Job.Status.STOPPED, jobResumeId.toString()));

            JobResumeOutcome outcome = jobResumeFacade.resumeApproval(signedApprovalToken(true));

            assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

            verify(jobFacade).resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, Map.of("approved", true));
        }

        private String signedApprovalToken(boolean approved) {
            ApprovalId approvalId = ApprovalId.of(TENANT_ID, JOB_ID, approved);

            return approvalTokens.toSignedToken(approvalId.toString(), Duration.ofHours(1));
        }
    }

    @Nested
    class ApprovalInputs {

        private static final List<Map<String, Object>> INPUTS = List.of(
            Map.of("fieldName", "reason", "fieldLabel", "Reason", "fieldType", 6, "required", true),
            Map.of("fieldName", "amount", "fieldLabel", "Amount", "fieldType", 9, "required", true),
            Map.of("fieldName", "note", "fieldType", 5),
            Map.of(
                "fieldName", "priority", "fieldType", 7,
                "fieldOptions",
                List.of(Map.of("label", "Low", "value", "low"), Map.of("label", "High", "value", "high"))));

        @Test
        void testResumeJobRejectsAnApprovalMissingRequiredInputsAndLeavesTheRunSuspended() {
            Job job = suspendedApprovalJob();

            String signedToken = signedToken(job);

            assertThatThrownBy(() -> jobResumeFacade.resumeJob(signedToken, Map.of("approved", true)))
                .isInstanceOf(ApprovalInputException.class)
                .hasMessage("The approval form is incomplete: 'Reason' is required; 'Amount' is required.");

            assertThat(job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isNull();

            verify(jobService, never()).update(any());
            verify(jobService, never()).tryClaimResume(any());
            verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        }

        @Test
        void testResumeJobRejectsAnApprovalWithBlankOrMistypedInputs() {
            Job job = suspendedApprovalJob();

            String signedToken = signedToken(job);

            assertThatThrownBy(
                () -> jobResumeFacade.resumeJob(
                    signedToken,
                    Map.of("approved", true, "reason", "  ", "amount", "seven", "priority", "urgent")))
                        .isInstanceOf(ApprovalInputException.class)
                        .satisfies(exception -> assertThat(((ApprovalInputException) exception).getErrors())
                            .containsExactly(
                                "'Reason' is required", "'Amount' must be a number",
                                "'priority' must be one of low, high"));

            verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
        }

        @Test
        void testResumeJobCoercesTypedInputsAndAddsTheAbsentOnes() {
            Job job = suspendedApprovalJob();

            JobResumeOutcome outcome = jobResumeFacade.resumeJob(
                signedToken(job), Map.of("approved", true, "reason", "budget ok", "amount", "7"));

            assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

            Map<String, Object> expectedData = new HashMap<>();

            expectedData.put("approved", true);
            expectedData.put("reason", "budget ok");
            expectedData.put("amount", 7L);
            expectedData.put("note", "");
            expectedData.put("priority", "");

            verify(jobFacade).resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, expectedData);
        }

        @Test
        void testResumeJobLetsADiscardOmitRequiredInputs() {
            Job job = suspendedApprovalJob();

            JobResumeOutcome outcome = jobResumeFacade.resumeJob(
                signedToken(job), Map.of("approved", false, "comment", "no budget"));

            assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

            Map<String, Object> expectedData = new HashMap<>();

            expectedData.put("approved", false);
            expectedData.put("comment", "no budget");
            expectedData.put("reason", "");
            expectedData.put("amount", null);
            expectedData.put("note", "");
            expectedData.put("priority", "");

            verify(jobFacade).resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, expectedData);
        }

        @Test
        void testResumeJobReadsTheInputsOfAnApprovalToolFromItsSuspend() {
            JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

            Job job = approvalJobOf(Job.Status.STOPPED, jobResumeId.toString());

            when(jobService.getJob(JOB_ID)).thenReturn(job);

            Map<String, Object> continueParameters = Map.of(
                MetadataConstants.APPROVAL_FORM_PARAMETERS,
                Map.of("inputs", List.of(Map.of("fieldName", "reason", "fieldType", 6, "required", true))));

            when(taskStateService.fetchValue(any(JobResumeId.class)))
                .thenReturn(Optional.of(new Suspend(continueParameters, null)));

            String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

            assertThatThrownBy(() -> jobResumeFacade.resumeJob(signedToken, Map.of("approved", true)))
                .isInstanceOf(ApprovalInputException.class)
                .hasMessage("The approval form is incomplete: 'reason' is required.");
        }

        @Test
        void testResumeJobDoesNotCheckTheInputsParameterOfAPlainWait() {
            JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

            when(jobService.getJob(JOB_ID)).thenReturn(jobOf(Job.Status.STOPPED, jobResumeId.toString()));

            JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of("approved", true));

            assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

            verify(jobFacade).resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, Map.of("approved", true));
            verify(taskStateService, never()).fetchValue(any());
        }

        private Job suspendedApprovalJob() {
            JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

            Job job = approvalJobOf(Job.Status.STOPPED, jobResumeId.toString());

            when(jobService.getJob(JOB_ID)).thenReturn(job);

            when(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID))
                .thenReturn(Optional.of(taskExecutionOf(Map.of("inputs", INPUTS))));

            return job;
        }

        private String signedToken(Job job) {
            return approvalTokens.toSignedToken(
                (String) job.getMetadata(MetadataConstants.JOB_RESUME_ID), Duration.ofHours(1));
        }
    }

    private static TaskExecution taskExecutionOf(Map<String, ?> parameters) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setWorkflowTask(
            new WorkflowTask(
                Map.of("name", "approval_1", "type", "approval/v1/requestApproval", "parameters", parameters)));

        return taskExecution;
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

    private static Job approvalJobOf(Job.Status status, String storedJobResumeIdString) {
        Job job = jobOf(status, storedJobResumeIdString);

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.put(MetadataConstants.APPROVAL_RESUME, true);

        job.setMetadata(metadata);

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
