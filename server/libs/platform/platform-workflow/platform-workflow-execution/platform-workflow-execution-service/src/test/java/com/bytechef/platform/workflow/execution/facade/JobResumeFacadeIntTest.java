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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.ApprovalId;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import com.bytechef.platform.workflow.execution.event.JobApprovalClosedEvent;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import com.bytechef.tenant.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@Import({
    JobFacadeImpl.class, JobResumeFacadeImpl.class, JobServiceImpl.class, TaskExecutionServiceImpl.class
})
@RecordApplicationEvents
@SpringBootTest(
    classes = PlatformWorkflowExecutionIntTestConfiguration.class)
class JobResumeFacadeIntTest {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static final String SIGNING_SECRET = createSigningSecret();

    private static final long MISSING_JOB_ID = 999_999L;
    private static final long MISSING_TASK_EXECUTION_ID = 999_999L;
    private static final String TENANT_ID = "000001";

    @DynamicPropertySource
    static void signingSecretProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("bytechef.approval.signed-token.clock-skew", () -> "PT60S");
        dynamicPropertyRegistry.add("bytechef.approval.signed-token.default-ttl", () -> "P30D");
        dynamicPropertyRegistry.add("bytechef.approval.signed-token.secret", () -> SIGNING_SECRET);
    }

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private ApprovalTokens approvalTokens;

    @MockitoBean
    private ContextService contextService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobFacade jobFacade;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobResumeFacade jobResumeFacade;

    @MockitoSpyBean
    private JobService jobService;

    private SimpleMeterRegistry meterRegistry;

    private JobResumeFacade meteredJobResumeFacade;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @MockitoSpyBean
    private TaskExecutionService taskExecutionService;

    @MockitoBean
    private TaskFileStorage taskFileStorage;

    @Autowired
    private TaskStateService taskStateService;

    @MockitoBean
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        meterRegistry = new SimpleMeterRegistry();

        meteredJobResumeFacade = createJobResumeFacade(
            approvalTokensProvider(approvalTokens), new TransactionTemplate(platformTransactionManager));
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM task_state");
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");
    }

    @Test
    void testResumeJobReturnsOkForSignedToken() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        String signedToken = approvalTokens.toSignedToken(getStoredJobResumeId(jobId), Duration.ofHours(1));
        Map<String, Object> data = Map.of("foo", "bar");

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(signedToken, data);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        assertResumedWithTaskExecution(jobId, data);
        assertThat(getJobResumedEvents()).containsExactly(new JobResumedEvent(signedToken));
    }

    @Test
    void testResumeJobReturnsInvalidIdForTamperedSignedToken() {
        String tamperedToken = tamperSignature(
            approvalTokens.toSignedToken(JobResumeId.of(MISSING_JOB_ID)
                .toString(), Duration.ofHours(1)));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(tamperedToken, Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsInvalidIdForUnsignedTokenWhenSigningRequired() {
        JobResumeFacade requiredJobResumeFacade = createJobResumeFacade(
            approvalTokensProvider(createApprovalTokens(true)), new TransactionTemplate(platformTransactionManager));

        JobResumeOutcome outcome = requiredJobResumeFacade.resumeJob(
            JobResumeId.of(MISSING_JOB_ID)
                .toString(),
            Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        assertNothingResumed();
    }

    @Test
    void testResumeExpiredJobAcceptsTheStoredUnsignedIdWhenSigningRequired() {
        JobResumeFacade requiredJobResumeFacade = createJobResumeFacade(
            approvalTokensProvider(createApprovalTokens(true)), new TransactionTemplate(platformTransactionManager));

        long jobId = saveJob(Job.Status.STOPPED, true, null);

        JobResumeOutcome outcome = requiredJobResumeFacade.resumeExpiredJob(getStoredJobResumeId(jobId));

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
    }

    @Test
    void testResumeJobAcceptsRawTokenWhenApprovalTokensAbsent() {
        JobResumeFacade unsignedJobResumeFacade = createJobResumeFacade(
            approvalTokensProvider(null), new TransactionTemplate(platformTransactionManager));

        long jobId = saveJob(Job.Status.STOPPED, true, null);

        JobResumeOutcome outcome = unsignedJobResumeFacade.resumeJob(getStoredJobResumeId(jobId), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
    }

    @Test
    void testResumeJobOpensItsTransactionUnderTheTenantOfTheResumeId() {
        long jobId = saveJob(Job.Status.COMPLETED, false, null);

        JobResumeId jobResumeId = TenantContext.callWithTenantId(TENANT_ID, () -> JobResumeId.of(jobId));

        List<String> transactionTenantIds = new ArrayList<>();

        JobResumeFacade tenantRecordingJobResumeFacade = createJobResumeFacade(
            approvalTokensProvider(approvalTokens), tenantRecordingTransactionOperations(transactionTenantIds));

        tenantRecordingJobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(transactionTenantIds).containsExactly(TENANT_ID);
    }

    @Test
    void testResumeExpiredJobOpensItsTransactionUnderTheTenantOfTheResumeId() {
        long jobId = saveJob(Job.Status.COMPLETED, false, null);

        JobResumeId jobResumeId = TenantContext.callWithTenantId(TENANT_ID, () -> JobResumeId.of(jobId));

        List<String> transactionTenantIds = new ArrayList<>();

        JobResumeFacade tenantRecordingJobResumeFacade = createJobResumeFacade(
            approvalTokensProvider(approvalTokens), tenantRecordingTransactionOperations(transactionTenantIds));

        tenantRecordingJobResumeFacade.resumeExpiredJob(jobResumeId.toString());

        assertThat(transactionTenantIds).containsExactly(TENANT_ID);
    }

    @Test
    void testResumeJobReturnsInvalidIdForUnparseableToken() {
        JobResumeOutcome outcome = jobResumeFacade.resumeJob("not-a-token", Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsGoneWhenJobNotStopped() {
        long jobId = saveJob(Job.Status.COMPLETED, true, null);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(getStoredJobResumeId(jobId), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsNotYetSuspendedWhileJobIsStillRunning() {
        long jobId = saveJob(Job.Status.STARTED, false, null);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            JobResumeId.of(jobId)
                .toString(),
            Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);

        assertNothingResumed();
    }

    @Test
    void testResumeJobStreamingReturnsNotYetSuspendedWhileJobIsStillRunning() {
        long jobId = saveJob(Job.Status.STARTED, false, true);

        List<Long> consumedJobIds = new ArrayList<>();

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            JobResumeId.of(jobId)
                .toString(),
            Map.of(), consumedJobIds::add);

        assertThat(outcome).isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);
        assertThat(consumedJobIds).isEmpty();

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsInvalidIdWhenStoredMetadataMissing() {
        long jobId = saveJob(Job.Status.STOPPED, false, null);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            JobResumeId.of(jobId)
                .toString(),
            Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsInvalidIdWhenUuidMismatch() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            JobResumeId.of(jobId)
                .toString(),
            Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsInvalidIdWhenTenantMismatch() {
        long jobId = saveJob(Job.Status.STOPPED, false, null);

        String currentTenantId = TenantContext.getCurrentTenantId();
        UUID sharedUuid = UUID.randomUUID();

        String suppliedToken = encodeToken(currentTenantId, jobId, sharedUuid);

        updateJobMetadata(
            jobId,
            Map.of(
                MetadataConstants.JOB_RESUME_ID, encodeToken(currentTenantId + "-other", jobId, sharedUuid),
                MetadataConstants.TASK_EXECUTION_RESUME_ID, MISSING_TASK_EXECUTION_ID));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(suppliedToken, Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsOkWhenTokenMatches() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        String jobResumeId = getStoredJobResumeId(jobId);
        Map<String, Object> data = Map.of("foo", "bar");

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId, data);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
        assertThat(getConsumedJobResumeId(jobId)).isEqualTo(jobResumeId);

        assertResumedWithTaskExecution(jobId, data);
        assertThat(getJobResumedEvents()).containsExactly(new JobResumedEvent(jobResumeId));
    }

    @Test
    void testResumeJobRejectsAJobWaitingOnASubflowItLaunched() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        String jobResumeId = getStoredJobResumeId(jobId);

        Map<String, Object> metadata = new HashMap<>(getJobMetadata(jobId));

        metadata.put(MetadataConstants.SUBFLOW_RESUME, true);

        updateJobMetadata(jobId, metadata);

        int version = getJobVersion(jobId);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            jobResumeId, Map.of("status", "completed", "result", "forged"));

        assertThat(outcome).isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);
        assertThat(getJobVersion(jobId)).isEqualTo(version);

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsGoneForAnAlreadyConsumedResumeIdWhileTheJobRuns() {
        long jobId = saveJob(Job.Status.STARTED, false, null);

        String jobResumeId = JobResumeId.of(jobId)
            .toString();

        updateJobMetadata(jobId, Map.of(MetadataConstants.CONSUMED_JOB_RESUME_ID, jobResumeId));

        int version = getJobVersion(jobId);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId, Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);
        assertThat(getJobVersion(jobId)).isEqualTo(version);

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsGoneForAnAlreadyConsumedResumeIdAfterTheJobSuspendedAgain() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        String consumedJobResumeId = JobResumeId.of(jobId)
            .toString();

        Map<String, Object> metadata = new HashMap<>(getJobMetadata(jobId));

        metadata.put(MetadataConstants.CONSUMED_JOB_RESUME_ID, consumedJobResumeId);

        updateJobMetadata(jobId, metadata);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(consumedJobResumeId, Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsJobFailedWhenTheJobFailed() {
        long jobId = saveJob(Job.Status.FAILED, true, null);

        int version = getJobVersion(jobId);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(getStoredJobResumeId(jobId), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.JOB_FAILED);
        assertThat(getJobVersion(jobId)).isEqualTo(version);

        assertNothingResumed();
    }

    @Test
    void testResumeJobIgnoresTheStreamingResumeFlag() {
        long jobId = saveJob(Job.Status.STOPPED, true, false);

        Map<String, Object> data = Map.of("foo", "bar");

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(getStoredJobResumeId(jobId), data);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        assertResumedWithTaskExecution(jobId, data);
    }

    @Test
    void testResumeJobStreamingCallsJobIdConsumerBeforeResumingTheJob() {
        long jobId = saveJob(Job.Status.STOPPED, true, true);

        String jobResumeId = getStoredJobResumeId(jobId);
        List<String> consumedJobResumeIdsSeenByConsumer = new ArrayList<>();
        List<Long> resumeJobEventCountsSeenByConsumer = new ArrayList<>();
        List<Long> consumedJobIds = new ArrayList<>();
        Map<String, Object> data = Map.of("foo", "bar");

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            jobResumeId, data, consumedJobId -> {
                consumedJobIds.add(consumedJobId);
                consumedJobResumeIdsSeenByConsumer.add(getConsumedJobResumeId(consumedJobId));
                resumeJobEventCountsSeenByConsumer.add(applicationEvents.stream(ResumeJobEvent.class)
                    .count());
            });

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
        assertThat(consumedJobIds).containsExactly(jobId);
        assertThat(consumedJobResumeIdsSeenByConsumer).containsExactly(jobResumeId);
        assertThat(resumeJobEventCountsSeenByConsumer).containsExactly(0L);

        assertResumedWithTaskExecution(jobId, data);
        assertThat(getJobResumedEvents()).containsExactly(new JobResumedEvent(jobResumeId));
    }

    @Test
    void testResumeJobStreamingDoesNotCallJobIdConsumerForAnInvalidId() {
        List<Long> consumedJobIds = new ArrayList<>();

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming("not-a-token", Map.of(), consumedJobIds::add);

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(consumedJobIds).isEmpty();

        assertNothingResumed();
    }

    @Test
    void testResumeJobStreamingDoesNotCallJobIdConsumerForATokenMismatch() {
        long jobId = saveJob(Job.Status.STOPPED, true, true);

        List<Long> consumedJobIds = new ArrayList<>();

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            JobResumeId.of(jobId)
                .toString(),
            Map.of(), consumedJobIds::add);

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(consumedJobIds).isEmpty();

        assertNothingResumed();
    }

    @Test
    void testResumeJobStreamingDoesNotCallJobIdConsumerWhenJobIsGone() {
        long jobId = saveJob(Job.Status.COMPLETED, true, true);

        List<Long> consumedJobIds = new ArrayList<>();

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            getStoredJobResumeId(jobId), Map.of(), consumedJobIds::add);

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);
        assertThat(consumedJobIds).isEmpty();

        assertNothingResumed();
    }

    @Test
    void testResumeJobStreamingReturnsStreamingNotAllowedWithoutStreamingResumeMetadata() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        List<Long> consumedJobIds = new ArrayList<>();

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            getStoredJobResumeId(jobId), Map.of(), consumedJobIds::add);

        assertThat(outcome).isEqualTo(JobResumeOutcome.STREAMING_NOT_ALLOWED);
        assertThat(consumedJobIds).isEmpty();
        assertThat(getConsumedJobResumeId(jobId)).isNull();

        assertNothingResumed();
    }

    @Test
    void testResumeJobStreamingReturnsStreamingNotAllowedWhenStreamingResumeIsFalse() {
        long jobId = saveJob(Job.Status.STOPPED, true, false);

        List<Long> consumedJobIds = new ArrayList<>();

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            getStoredJobResumeId(jobId), Map.of(), consumedJobIds::add);

        assertThat(outcome).isEqualTo(JobResumeOutcome.STREAMING_NOT_ALLOWED);
        assertThat(consumedJobIds).isEmpty();
        assertThat(getConsumedJobResumeId(jobId)).isNull();

        assertNothingResumed();
    }

    @Test
    void testResumeExpiredJobResumesAJobStillWaitingOnTheSuspendAndConsumesTheResumeId() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        String jobResumeId = getStoredJobResumeId(jobId);

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(jobResumeId);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
        assertThat(getConsumedJobResumeId(jobId)).isEqualTo(jobResumeId);

        assertResumedWithTaskExecution(jobId, null);
        assertThat(getJobResumedEvents()).containsExactly(new JobResumedEvent(jobResumeId));
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STARTED);
    }

    @Test
    void testResumeExpiredJobRestartsTheJobWhenNoSuspendedTaskExecutionIsStored() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        String jobResumeId = getStoredJobResumeId(jobId);

        updateJobMetadata(jobId, Map.of(MetadataConstants.JOB_RESUME_ID, jobResumeId));

        assertThat(jobResumeFacade.resumeExpiredJob(jobResumeId)).isEqualTo(JobResumeOutcome.OK);

        List<ResumeJobEvent> resumeJobEvents = getResumeJobEvents();

        assertThat(resumeJobEvents).hasSize(1);
        assertThat(resumeJobEvents.getFirst()
            .isStarted()).isTrue();
        assertThat(resumeJobEvents.getFirst()
            .getJobId()).isEqualTo(jobId);
        assertThat(resumeJobEvents.getFirst()
            .getTaskExecutionId()).isNull();
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STARTED);
    }

    @Test
    void testResumeExpiredJobReturnsNotYetSuspendedWhileTheJobIsStillRunning() {
        long jobId = saveJob(Job.Status.STARTED, false, null);

        int version = getJobVersion(jobId);

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(
            JobResumeId.of(jobId)
                .toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);
        assertThat(getJobVersion(jobId)).isEqualTo(version);

        assertNothingResumed();
    }

    @Test
    void testResumeExpiredJobDoesNotRestartAFailedJob() {
        long jobId = saveJob(Job.Status.FAILED, true, null);

        int version = getJobVersion(jobId);

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(getStoredJobResumeId(jobId));

        assertThat(outcome).isEqualTo(JobResumeOutcome.JOB_FAILED);
        assertThat(getJobVersion(jobId)).isEqualTo(version);
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.FAILED);

        assertNothingResumed();
    }

    @Test
    void testResumeExpiredJobReturnsInvalidIdForAStoppedJobWithoutAStoredResumeId() {
        long jobId = saveJob(Job.Status.STOPPED, false, null);

        int version = getJobVersion(jobId);

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(
            JobResumeId.of(jobId)
                .toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(getJobVersion(jobId)).isEqualTo(version);

        assertNothingResumed();
    }

    @Test
    void testResumeExpiredJobDoesNotResumeAJobSuspendedAgainOnANewResumeId() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(
            JobResumeId.of(jobId)
                .toString());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        assertNothingResumed();
    }

    @Test
    void testResumeExpiredJobDoesNotResumeAnAlreadyAnsweredSuspend() {
        long jobId = saveJob(Job.Status.STARTED, false, null);

        String jobResumeId = JobResumeId.of(jobId)
            .toString();

        updateJobMetadata(jobId, Map.of(MetadataConstants.CONSUMED_JOB_RESUME_ID, jobResumeId));

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(jobResumeId);

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsGoneForAnAnswerAfterTheSuspendExpired() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        String jobResumeId = getStoredJobResumeId(jobId);

        jobResumeFacade.resumeExpiredJob(jobResumeId);

        Job job = jobService.getJob(jobId);

        job.setStatus(Job.Status.STARTED);

        jobService.update(job);

        applicationEvents.clear();

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId, Map.of("answer", "late"));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        assertNothingResumed();
    }

    @Test
    void testResumeExpiredJobReturnsInvalidIdForAnUnparseableId() {
        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob("not-a-token");

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        assertNothingResumed();
    }

    @Test
    void testResumeJobRejectsAnUnsignedIdForAnApprovalWhenSigningIsNotRequired() {
        long jobId = saveApprovalJob(Map.of(), Map.of());

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(getStoredJobResumeId(jobId), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(getConsumedJobResumeId(jobId)).isNull();

        assertNothingResumed();
    }

    @Test
    void testResumeJobResolvesAnApprovalWithASignedToken() {
        long jobId = saveApprovalJob(Map.of(), Map.of());

        String signedToken = approvalTokens.toSignedToken(getStoredJobResumeId(jobId), Duration.ofHours(1));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(signedToken, Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        assertResumedWithTaskExecution(jobId, Map.of("approved", true));
        assertThat(getJobResumedEvents()).containsExactly(new JobResumedEvent(signedToken));
    }

    @Test
    void testResumeJobRejectsAnApprovalWithoutAnApprovedDecision() {
        long jobId = saveApprovalJob(Map.of(), Map.of());

        String signedToken = approvalTokens.toSignedToken(getStoredJobResumeId(jobId), Duration.ofHours(1));

        assertThat(jobResumeFacade.resumeJob(signedToken, Map.of("comment", "looks fine")))
            .isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(jobResumeFacade.resumeJob(signedToken, Map.of("approved", "true")))
            .isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(jobResumeFacade.resumeJob(signedToken, null))
            .isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(getConsumedJobResumeId(jobId)).isNull();

        assertNothingResumed();
    }

    @Test
    void testResumeJobRejectsAnApprovalWhenApprovalTokensAbsent() {
        JobResumeFacade unsignedJobResumeFacade = createJobResumeFacade(
            approvalTokensProvider(null), new TransactionTemplate(platformTransactionManager));

        long jobId = saveApprovalJob(Map.of(), Map.of());

        JobResumeOutcome outcome = unsignedJobResumeFacade.resumeJob(getStoredJobResumeId(jobId), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        assertNothingResumed();
    }

    @Test
    void testResumeJobStreamingResumesAnApprovalOfAStreamedChatRun() {
        long jobId = saveJob(Job.Status.STOPPED, true, false);

        putJobMetadata(
            jobId, Map.of(MetadataConstants.APPROVAL_RESUME, true, MetadataConstants.STREAMED_EXECUTION, true));

        String signedToken = approvalTokens.toSignedToken(getStoredJobResumeId(jobId), Duration.ofHours(1));
        List<Long> consumedJobIds = new ArrayList<>();
        List<Long> resumeJobEventCountsSeenByConsumer = new ArrayList<>();

        JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
            signedToken, Map.of("approved", true), consumedJobId -> {
                consumedJobIds.add(consumedJobId);
                resumeJobEventCountsSeenByConsumer.add(applicationEvents.stream(ResumeJobEvent.class)
                    .count());
            });

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);
        assertThat(consumedJobIds).containsExactly(jobId);
        assertThat(resumeJobEventCountsSeenByConsumer).containsExactly(0L);

        assertResumedWithTaskExecution(jobId, Map.of("approved", true));
    }

    @Test
    void testResumeExpiredJobFailsAnExpiredApprovalInsteadOfResumingIt() {
        long jobId = saveApprovalJob(
            Map.of(), Map.of(MetadataConstants.SUSPEND, Map.of("expiresAt", "2026-10-17T08:31:52Z")));

        String jobResumeId = getStoredJobResumeId(jobId);

        taskStateService.save(JobResumeId.parse(jobResumeId), new Suspend(Map.of(), null));

        JobResumeOutcome outcome = meteredJobResumeFacade.resumeExpiredJob(jobResumeId);

        assertThat(outcome).isEqualTo(JobResumeOutcome.JOB_FAILED);

        Job job = jobService.getJob(jobId);

        ExecutionError executionError = Validate.notNull(job.getError(), "error");

        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(job.getEndDate()).isNotNull();
        assertThat(executionError.getMessage()).isEqualTo(
            "The pending approval expired at 2026-10-17T08:31:52Z without being resolved");
        assertThat(job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isEqualTo(jobResumeId);
        assertThat(job.getMetadata(MetadataConstants.EXPIRED_JOB_RESUME_ID)).isEqualTo(jobResumeId);
        assertThat(getStoredTaskExecutionStatus(jobId)).isEqualTo(TaskExecution.Status.FAILED);
        assertThat(taskStateService.fetchUnlockedValue(JobResumeId.parse(jobResumeId))).isEmpty();
        assertThat(getJobApprovalClosedEvents()).containsExactly(new JobApprovalClosedEvent(jobId));
        assertThat(getJobStatusApplicationEventStatuses()).containsExactly(Job.Status.FAILED);

        assertNothingResumed();

        verify(jobService, never()).tryClaimResume(any());

        assertThat(meterRegistry.counter("bytechef_approval_expired", "source", "timer")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testIsApprovalResumeIdTellsAnApprovalFromAWait() {
        long approvalJobId = saveApprovalJob(Map.of(), Map.of());
        long waitJobId = saveJob(Job.Status.STOPPED, true, null);

        String signedToken = approvalTokens.toSignedToken(getStoredJobResumeId(approvalJobId), Duration.ofHours(1));

        assertThat(jobResumeFacade.isApprovalResumeId(signedToken)).isTrue();
        assertThat(jobResumeFacade.isApprovalResumeId(getStoredJobResumeId(waitJobId))).isFalse();
        assertThat(jobResumeFacade.isApprovalResumeId(
            JobResumeId.of(MISSING_JOB_ID)
                .toString())).isFalse();
        assertThat(jobResumeFacade.isApprovalResumeId("not-a-token")).isFalse();
    }

    @Test
    void testCancelApprovalCancelsAJobWaitingOnAnApprovalAndMakesItsLinksGone() {
        long jobId = saveApprovalJob(Map.of(), Map.of());

        String jobResumeId = getStoredJobResumeId(jobId);

        taskStateService.save(JobResumeId.parse(jobResumeId), new Suspend(Map.of(), null));

        assertThat(jobResumeFacade.cancelApproval(jobId)).isTrue();

        Job job = jobService.getJob(jobId);

        assertThat(job.getStatus()).isEqualTo(Job.Status.CANCELLED);
        assertThat(job.getEndDate()).isNotNull();
        assertThat(job.getMetadata(MetadataConstants.CANCELLED_JOB_RESUME_ID)).isEqualTo(jobResumeId);
        assertThat(taskStateService.fetchUnlockedValue(JobResumeId.parse(jobResumeId))).isEmpty();
        assertThat(getJobApprovalClosedEvents()).containsExactly(new JobApprovalClosedEvent(jobId));
        assertThat(getJobStatusApplicationEventStatuses()).containsExactly(Job.Status.CANCELLED);

        String signedToken = approvalTokens.toSignedToken(jobResumeId, Duration.ofHours(1));

        assertThat(jobResumeFacade.resumeJob(signedToken, Map.of("approved", true)))
            .isEqualTo(JobResumeOutcome.GONE);

        assertNothingResumed();
    }

    @Test
    void testCancelApprovalLeavesAJobThatDoesNotWaitOnAnApproval() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        int version = getJobVersion(jobId);

        assertThat(jobResumeFacade.cancelApproval(jobId)).isFalse();
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STOPPED);
        assertThat(getJobVersion(jobId)).isEqualTo(version);
        assertThat(getJobApprovalClosedEvents()).isEmpty();
    }

    @Test
    void testResumeExpiredJobReturnsGoneWhenTheResumeClaimIsLost() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        doReturn(false).when(jobService)
            .tryClaimResume(any());

        JobResumeOutcome outcome = jobResumeFacade.resumeExpiredJob(getStoredJobResumeId(jobId));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STOPPED);
        assertThat(getJobApprovalClosedEvents()).isEmpty();
        assertThat(getJobStatusApplicationEventStatuses()).isEmpty();

        assertNothingResumed();
    }

    @Test
    void testResumeJobReturnsGoneWhenTheClaimIsLostToAConcurrentResolutionOrExpiry() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        doReturn(false).when(jobService)
            .tryClaimResume(any());

        JobResumeOutcome outcome = meteredJobResumeFacade.resumeJob(
            getStoredJobResumeId(jobId), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STOPPED);

        assertNothingResumed();

        assertThat(meterRegistry.find("bytechef_approval_resolution")
            .counter()).isNull();
    }

    @Test
    void testResumeJobCountsApprovalResolution() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        Map<String, Object> data = Map.of("approved", false, "comment", "not now");

        JobResumeOutcome outcome = meteredJobResumeFacade.resumeJob(getStoredJobResumeId(jobId), data);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        assertResumedWithTaskExecution(jobId, data);

        assertThat(meterRegistry.counter("bytechef_approval_resolution", "approved", "false")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testResumeJobWithNullDataDoesNotThrow() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(getStoredJobResumeId(jobId), null);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        assertResumedWithTaskExecution(jobId, Map.of());
    }

    @Test
    void testResumeJobStampsVerifiedApprovedBy() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            getStoredJobResumeId(jobId), Map.of("approved", true), "@jane");

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        assertResumedWithTaskExecution(jobId, Map.of("approved", true, "approvedBy", "@jane"));
    }

    @Test
    void testResumeJobStripsSpoofedApprovedByWhenNoVerifiedIdentity() {
        long jobId = saveApprovalJob(Map.of(), Map.of());

        String signedToken = approvalTokens.toSignedToken(getStoredJobResumeId(jobId), Duration.ofHours(1));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            signedToken, Map.of("approved", true, "approvedBy", "@attacker"));

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        assertResumedWithTaskExecution(jobId, Map.of("approved", true));
    }

    @Test
    void testResumeJobKeepsTheApprovedByFieldOfAPlainWaitCallback() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            getStoredJobResumeId(jobId), Map.of("approvedBy", "Jane", "order", 42));

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        assertResumedWithTaskExecution(jobId, Map.of("approvedBy", "Jane", "order", 42));
    }

    @Test
    void testResumeJobReturnsGoneWhenSuspendExpired() {
        Instant expiresAt = Instant.now()
            .minusSeconds(60);

        long jobId = saveJob(
            Job.Status.STOPPED, true, null, Map.of(),
            Map.of(MetadataConstants.SUSPEND, Map.of("expiresAt", expiresAt.toEpochMilli())));

        JobResumeOutcome outcome = meteredJobResumeFacade.resumeJob(
            getStoredJobResumeId(jobId), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STOPPED);

        assertNothingResumed();

        assertThat(meterRegistry.counter("bytechef_approval_expired", "source", "resume")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testResumeJobFailsTheRunAtOnceWhenItsApprovalExpired() {
        long jobId = saveApprovalJob(
            Map.of(), Map.of(MetadataConstants.SUSPEND, Map.of("expiresAt", "2026-10-17T11:00:00Z")));

        String jobResumeId = getStoredJobResumeId(jobId);

        taskStateService.save(JobResumeId.parse(jobResumeId), new Suspend(Map.of(), null));

        String signedToken = approvalTokens.toSignedToken(jobResumeId, Duration.ofHours(1));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(signedToken, Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        Job job = jobService.getJob(jobId);

        ExecutionError executionError = Validate.notNull(job.getError(), "error");

        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(executionError.getMessage()).isEqualTo(
            "The pending approval expired at 2026-10-17T11:00:00Z without being resolved");
        assertThat(job.getMetadata(MetadataConstants.EXPIRED_JOB_RESUME_ID)).isEqualTo(jobResumeId);
        assertThat(getStoredTaskExecutionStatus(jobId)).isEqualTo(TaskExecution.Status.FAILED);
        assertThat(taskStateService.fetchUnlockedValue(JobResumeId.parse(jobResumeId))).isEmpty();
        assertThat(getJobApprovalClosedEvents()).containsExactly(new JobApprovalClosedEvent(jobId));
        assertThat(getJobStatusApplicationEventStatuses()).containsExactly(Job.Status.FAILED);

        assertNothingResumed();

        assertThat(jobResumeFacade.resumeJob(signedToken, Map.of("approved", true)))
            .isEqualTo(JobResumeOutcome.GONE);
    }

    @Test
    void testResumeJobReturnsGoneForAnApprovalTheSweepExpired() {
        long jobId = saveApprovalJob(Map.of(), Map.of());

        String jobResumeId = getStoredJobResumeId(jobId);

        putJobMetadata(jobId, Map.of(MetadataConstants.EXPIRED_JOB_RESUME_ID, jobResumeId));

        Job job = jobService.getJob(jobId);

        job.setStatus(Job.Status.FAILED);

        jobService.update(job);

        int version = getJobVersion(jobId);

        String signedToken = approvalTokens.toSignedToken(jobResumeId, Duration.ofHours(1));

        assertThat(jobResumeFacade.resumeJob(signedToken, Map.of("approved", true)))
            .isEqualTo(JobResumeOutcome.GONE);
        assertThat(getJobVersion(jobId)).isEqualTo(version);

        assertNothingResumed();
    }

    @Test
    void testResumeJobProceedsWhenSuspendNotExpired() {
        Instant expiresAt = Instant.now()
            .plusSeconds(3600);

        long jobId = saveJob(
            Job.Status.STOPPED, true, null, Map.of(),
            Map.of(MetadataConstants.SUSPEND, Map.of("expiresAt", expiresAt.toEpochMilli())));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(getStoredJobResumeId(jobId), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        assertResumedWithTaskExecution(jobId, Map.of("approved", true));
    }

    @Test
    void testResumeJobFailsClosedWhenTheSuspendedTaskExecutionIsMissing() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        putJobMetadata(jobId, Map.of(MetadataConstants.TASK_EXECUTION_RESUME_ID, MISSING_TASK_EXECUTION_ID));

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(getStoredJobResumeId(jobId), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STOPPED);
        assertThat(getConsumedJobResumeId(jobId)).isNull();

        assertNothingResumed();

        verify(jobService, never()).tryClaimResume(any());
    }

    @Test
    void testResumeJobFailsClosedWhenTheSuspendExpiryIsUnreadable() {
        long jobId = saveJob(Job.Status.STOPPED, true, null);

        long taskExecutionId = Validate.notNull(getStoredTaskExecutionId(jobId), "taskExecutionId");

        TaskExecution unreadableTaskExecution = mock(TaskExecution.class);

        when(unreadableTaskExecution.getMetadata()).thenThrow(new IllegalStateException("corrupt metadata"));

        doReturn(Optional.of(unreadableTaskExecution)).when(taskExecutionService)
            .fetchTaskExecution(taskExecutionId);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(getStoredJobResumeId(jobId), Map.of("approved", true));

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STOPPED);

        assertNothingResumed();
    }

    @Nested
    class ResumeApproval {

        @Test
        void testResumeApprovalRejectsAnUnsignedToken() {
            String unsignedToken = ApprovalId.of(TENANT_ID, MISSING_JOB_ID, true)
                .toString();

            JobResumeOutcome outcome = jobResumeFacade.resumeApproval(unsignedToken);

            assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

            verify(jobService, never()).getJob(anyLong());

            assertNothingResumed();
        }

        @Test
        void testResumeApprovalRejectsATamperedToken() {
            String tamperedToken = tamperSignature(signedApprovalToken(MISSING_JOB_ID, true));

            JobResumeOutcome outcome = jobResumeFacade.resumeApproval(tamperedToken);

            assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

            verify(jobService, never()).getJob(anyLong());

            assertNothingResumed();
        }

        @Test
        void testResumeApprovalRejectsAnUnsignedTokenWhenApprovalTokensAbsent() {
            JobResumeFacade unsignedJobResumeFacade = createJobResumeFacade(
                approvalTokensProvider(null), new TransactionTemplate(platformTransactionManager));

            String unsignedToken = ApprovalId.of(TENANT_ID, MISSING_JOB_ID, true)
                .toString();

            JobResumeOutcome outcome = unsignedJobResumeFacade.resumeApproval(unsignedToken);

            assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

            verify(jobService, never()).getJob(anyLong());
        }

        @Test
        void testResumeApprovalResolvesASuspendedApprovalForASignedToken() {
            long jobId = saveApprovalJob(Map.of(), Map.of());

            JobResumeOutcome outcome = jobResumeFacade.resumeApproval(signedApprovalToken(jobId, true));

            assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

            assertResumedWithTaskExecution(jobId, Map.of("approved", true));
            assertThat(getJobResumedEvents()).containsExactly(new JobResumedEvent(getStoredJobResumeId(jobId)));
        }

        private String signedApprovalToken(long jobId, boolean approved) {
            ApprovalId approvalId = ApprovalId.of(jobId, approved);

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
            long jobId = saveApprovalJob(Map.of("inputs", INPUTS), Map.of());

            String signedToken = signedToken(jobId);

            int version = getJobVersion(jobId);

            assertThatThrownBy(() -> jobResumeFacade.resumeJob(signedToken, Map.of("approved", true)))
                .isInstanceOf(ApprovalInputException.class)
                .hasMessage("The approval form is incomplete: 'Reason' is required; 'Amount' is required.");

            assertThat(getConsumedJobResumeId(jobId)).isNull();
            assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STOPPED);
            assertThat(getJobVersion(jobId)).isEqualTo(version);

            assertNothingResumed();

            verify(jobService, never()).tryClaimResume(any());
        }

        @Test
        void testResumeJobRejectsAnApprovalWithBlankOrMistypedInputs() {
            long jobId = saveApprovalJob(Map.of("inputs", INPUTS), Map.of());

            String signedToken = signedToken(jobId);

            assertThatThrownBy(
                () -> jobResumeFacade.resumeJob(
                    signedToken,
                    Map.of("approved", true, "reason", "  ", "amount", "seven", "priority", "urgent")))
                        .isInstanceOfSatisfying(
                            ApprovalInputException.class,
                            approvalInputException -> assertThat(approvalInputException.getErrors())
                                .containsExactly(
                                    "'Reason' is required", "'Amount' must be a number",
                                    "'priority' must be one of low, high"));

            assertNothingResumed();
        }

        @Test
        void testResumeJobCoercesTypedInputsAndAddsTheAbsentOnes() {
            long jobId = saveApprovalJob(Map.of("inputs", INPUTS), Map.of());

            JobResumeOutcome outcome = jobResumeFacade.resumeJob(
                signedToken(jobId), Map.of("approved", true, "reason", "budget ok", "amount", "7"));

            assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

            Map<String, Object> expectedData = new HashMap<>();

            expectedData.put("approved", true);
            expectedData.put("reason", "budget ok");
            expectedData.put("amount", 7L);
            expectedData.put("note", "");
            expectedData.put("priority", "");

            assertResumedWithTaskExecution(jobId, expectedData);
        }

        @Test
        void testResumeJobLetsADiscardOmitRequiredInputs() {
            long jobId = saveApprovalJob(Map.of("inputs", INPUTS), Map.of());

            JobResumeOutcome outcome = jobResumeFacade.resumeJob(
                signedToken(jobId), Map.of("approved", false, "comment", "no budget"));

            assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

            Map<String, Object> expectedData = new HashMap<>();

            expectedData.put("approved", false);
            expectedData.put("comment", "no budget");
            expectedData.put("reason", "");
            expectedData.put("amount", null);
            expectedData.put("note", "");
            expectedData.put("priority", "");

            assertResumedWithTaskExecution(jobId, expectedData);
        }

        @Test
        void testResumeJobReadsTheInputsOfAnApprovalToolFromItsSuspend() {
            long jobId = saveApprovalJob(Map.of(), Map.of());

            Map<String, Object> continueParameters = Map.of(
                MetadataConstants.APPROVAL_FORM_PARAMETERS,
                Map.of("inputs", List.of(Map.of("fieldName", "reason", "fieldType", 6, "required", true))));

            taskStateService.save(
                JobResumeId.parse(getStoredJobResumeId(jobId)), new Suspend(continueParameters, null));

            String signedToken = signedToken(jobId);

            assertThatThrownBy(() -> jobResumeFacade.resumeJob(signedToken, Map.of("approved", true)))
                .isInstanceOf(ApprovalInputException.class)
                .hasMessage("The approval form is incomplete: 'reason' is required.");

            assertNothingResumed();
        }

        @Test
        void testResumeJobDoesNotCheckTheInputsParameterOfAPlainWait() {
            long jobId = saveJob(Job.Status.STOPPED, true, null, Map.of("inputs", INPUTS), Map.of());

            JobResumeOutcome outcome = jobResumeFacade.resumeJob(
                getStoredJobResumeId(jobId), Map.of("approved", true));

            assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

            assertResumedWithTaskExecution(jobId, Map.of("approved", true));
        }

        private String signedToken(long jobId) {
            return approvalTokens.toSignedToken(getStoredJobResumeId(jobId), Duration.ofHours(1));
        }
    }

    private void assertNothingResumed() {
        assertThat(getResumeJobEvents()).isEmpty();
        assertThat(getJobResumedEvents()).isEmpty();
    }

    private void assertResumedWithTaskExecution(long jobId, Map<String, ?> data) {
        List<ResumeJobEvent> resumeJobEvents = getResumeJobEvents();

        assertThat(resumeJobEvents).hasSize(1);

        ResumeJobEvent resumeJobEvent = resumeJobEvents.getFirst();

        assertThat(resumeJobEvent.getJobId()).isEqualTo(jobId);
        assertThat(resumeJobEvent.getTaskExecutionId()).isEqualTo(getStoredTaskExecutionId(jobId));
        assertThat(resumeJobEvent.getData()).isEqualTo(data);
        assertThat(resumeJobEvent.isStarted()).isTrue();
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STARTED);
    }

    private ObjectProvider<ApprovalTokens> approvalTokensProvider(ApprovalTokens providedApprovalTokens) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        if (providedApprovalTokens != null) {
            beanFactory.addBean("approvalTokens", providedApprovalTokens);
        }

        return beanFactory.getBeanProvider(ApprovalTokens.class);
    }

    private static ApprovalTokens createApprovalTokens(boolean required) {
        return new ApprovalTokensImpl(
            Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), required);
    }

    private JobResumeFacade createJobResumeFacade(
        ObjectProvider<ApprovalTokens> approvalTokensProvider, TransactionOperations transactionOperations) {

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        beanFactory.addBean("meterRegistry", meterRegistry);

        ObjectProvider<MeterRegistry> meterRegistryProvider = beanFactory.getBeanProvider(MeterRegistry.class);

        return new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider, jobFacade, jobService, meterRegistryProvider,
            transactionOperations, taskExecutionService, taskStateService);
    }

    private static String encodeToken(String tenantId, long jobId, UUID uuid) {
        return EncodingUtils.base64EncodeToString(tenantId + ":" + jobId + ":" + uuid);
    }

    private String getConsumedJobResumeId(long jobId) {
        return (String) getJobMetadata(jobId).get(MetadataConstants.CONSUMED_JOB_RESUME_ID);
    }

    private List<JobApprovalClosedEvent> getJobApprovalClosedEvents() {
        return applicationEvents.stream(JobApprovalClosedEvent.class)
            .toList();
    }

    private Map<String, ?> getJobMetadata(long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getMetadata();
    }

    private Job.Status getJobStatus(long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getStatus();
    }

    private List<Job.Status> getJobStatusApplicationEventStatuses() {
        return applicationEvents.stream(JobStatusApplicationEvent.class)
            .map(JobStatusApplicationEvent::getStatus)
            .toList();
    }

    private int getJobVersion(long jobId) {
        Integer version = jdbcTemplate.queryForObject("SELECT version FROM job WHERE id = ?", Integer.class, jobId);

        return Validate.notNull(version, "version");
    }

    private List<JobResumedEvent> getJobResumedEvents() {
        return applicationEvents.stream(JobResumedEvent.class)
            .toList();
    }

    private List<ResumeJobEvent> getResumeJobEvents() {
        return applicationEvents.stream(ResumeJobEvent.class)
            .toList();
    }

    private String getStoredJobResumeId(long jobId) {
        return (String) getJobMetadata(jobId).get(MetadataConstants.JOB_RESUME_ID);
    }

    private Long getStoredTaskExecutionId(long jobId) {
        Number taskExecutionId = (Number) getJobMetadata(jobId).get(MetadataConstants.TASK_EXECUTION_RESUME_ID);

        return taskExecutionId == null ? null : taskExecutionId.longValue();
    }

    private TaskExecution.Status getStoredTaskExecutionStatus(long jobId) {
        long taskExecutionId = Validate.notNull(getStoredTaskExecutionId(jobId), "taskExecutionId");

        TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

        return taskExecution.getStatus();
    }

    private void putJobMetadata(long jobId, Map<String, ?> additionalMetadata) {
        Map<String, Object> metadata = new HashMap<>(getJobMetadata(jobId));

        metadata.putAll(additionalMetadata);

        updateJobMetadata(jobId, metadata);
    }

    private long saveApprovalJob(Map<String, ?> taskParameters, Map<String, ?> taskMetadata) {
        long jobId = saveJob(Job.Status.STOPPED, true, null, taskParameters, taskMetadata);

        putJobMetadata(jobId, Map.of(MetadataConstants.APPROVAL_RESUME, true));

        return jobId;
    }

    private long saveJob(Job.Status status, boolean storeJobResumeId, Boolean streamingResume) {
        return saveJob(status, storeJobResumeId, streamingResume, Map.of(), Map.of());
    }

    private long saveJob(
        Job.Status status, boolean storeJobResumeId, Boolean streamingResume, Map<String, ?> taskParameters,
        Map<String, ?> taskMetadata) {

        Job job = new Job();

        job.setStatus(status);
        job.setWorkflowId("workflow-1");

        job = jobRepository.save(job);

        long jobId = Validate.notNull(job.getId(), "id");

        Map<String, Object> metadata = new HashMap<>();

        if (storeJobResumeId) {
            metadata.put(
                MetadataConstants.JOB_RESUME_ID, JobResumeId.of(jobId)
                    .toString());
            metadata.put(
                MetadataConstants.TASK_EXECUTION_RESUME_ID, saveTaskExecution(jobId, taskParameters, taskMetadata));
        }

        if (streamingResume != null) {
            metadata.put(MetadataConstants.STREAMING_RESUME, streamingResume);
        }

        updateJobMetadata(jobId, metadata);

        return jobId;
    }

    private long saveTaskExecution(long jobId, Map<String, ?> parameters, Map<String, ?> metadata) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .metadata(metadata)
            .status(TaskExecution.Status.STARTED)
            .workflowTask(
                new WorkflowTask(
                    Map.of("name", "approval_1", "type", "approval/v1/requestApproval", "parameters", parameters)))
            .build();

        taskExecution = taskExecutionService.create(taskExecution);

        return Validate.notNull(taskExecution.getId(), "id");
    }

    private static String tamperSignature(String signedToken) {
        int signatureStart = signedToken.lastIndexOf('.') + 1;
        char replacement = signedToken.charAt(signatureStart) == 'A' ? 'B' : 'A';

        return signedToken.substring(0, signatureStart) + replacement + signedToken.substring(signatureStart + 1);
    }

    private TransactionOperations tenantRecordingTransactionOperations(List<String> transactionTenantIds) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        return new TransactionOperations() {

            @Override
            public <T> T execute(TransactionCallback<T> transactionCallback) {
                transactionTenantIds.add(TenantContext.getCurrentTenantId());

                return transactionTemplate.execute(transactionCallback);
            }
        };
    }

    private void updateJobMetadata(long jobId, Map<String, ?> metadata) {
        Job job = jobService.getJob(jobId);

        job.setMetadata(metadata);

        jobService.update(job);
    }

    private static String createSigningSecret() {
        byte[] secretBytes = new byte[32];

        SECURE_RANDOM.nextBytes(secretBytes);

        return EncodingUtils.base64EncodeToString(secretBytes);
    }
}
