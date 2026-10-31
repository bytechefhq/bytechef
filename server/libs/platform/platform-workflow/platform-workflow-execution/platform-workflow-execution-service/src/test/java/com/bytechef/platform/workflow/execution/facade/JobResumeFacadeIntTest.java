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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import com.bytechef.tenant.TenantContext;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
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

    static final String SIGNING_SECRET = createSigningSecret();

    private static final long MISSING_JOB_ID = 999_999L;
    private static final long TASK_EXECUTION_ID = 7L;
    private static final String TENANT_ID = "000001";

    @DynamicPropertySource
    static void signingSecretProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
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

    @Autowired
    private JobService jobService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @MockitoBean
    private TaskFileStorage taskFileStorage;

    @MockitoBean
    private WorkflowService workflowService;

    @AfterEach
    void afterEach() {
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
                MetadataConstants.TASK_EXECUTION_RESUME_ID, TASK_EXECUTION_ID));

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
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STOPPED);
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

    private void assertNothingResumed() {
        assertThat(getResumeJobEvents()).isEmpty();
        assertThat(getJobResumedEvents()).isEmpty();
    }

    private void assertResumedWithTaskExecution(long jobId, Map<String, Object> data) {
        List<ResumeJobEvent> resumeJobEvents = getResumeJobEvents();

        assertThat(resumeJobEvents).hasSize(1);

        ResumeJobEvent resumeJobEvent = resumeJobEvents.getFirst();

        assertThat(resumeJobEvent.getJobId()).isEqualTo(jobId);
        assertThat(resumeJobEvent.getTaskExecutionId()).isEqualTo(TASK_EXECUTION_ID);
        assertThat(resumeJobEvent.getData()).isEqualTo(data);
        assertThat(resumeJobEvent.isStarted()).isFalse();
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

        return new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider, jobFacade, jobService, transactionOperations);
    }

    private static String encodeToken(String tenantId, long jobId, UUID uuid) {
        return EncodingUtils.base64EncodeToString(tenantId + ":" + jobId + ":" + uuid);
    }

    private String getConsumedJobResumeId(long jobId) {
        return (String) getJobMetadata(jobId).get(MetadataConstants.CONSUMED_JOB_RESUME_ID);
    }

    private Map<String, ?> getJobMetadata(long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getMetadata();
    }

    private Job.Status getJobStatus(long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getStatus();
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

    private long saveJob(Job.Status status, boolean storeJobResumeId, Boolean streamingResume) {
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
            metadata.put(MetadataConstants.TASK_EXECUTION_RESUME_ID, TASK_EXECUTION_ID);
        }

        if (streamingResume != null) {
            metadata.put(MetadataConstants.STREAMING_RESUME, streamingResume);
        }

        updateJobMetadata(jobId, metadata);

        return jobId;
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

        new SecureRandom().nextBytes(secretBytes);

        return EncodingUtils.base64EncodeToString(secretBytes);
    }
}
