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

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ApprovalFormInputs;
import com.bytechef.platform.component.definition.SuspendUtils;
import com.bytechef.platform.workflow.execution.ApprovalId;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.JobSuspends;
import com.bytechef.platform.workflow.execution.event.JobApprovalClosedEvent;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@Service
public class JobResumeFacadeImpl implements JobResumeFacade {

    private static final Logger log = LoggerFactory.getLogger(JobResumeFacadeImpl.class);

    private static final String APPROVAL_RESOLUTION_METRIC_NAME = "bytechef_approval_resolution";

    private static final String APPROVED = "approved";

    private static final String APPROVED_BY = "approvedBy";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectProvider<ApprovalTokens> approvalTokensProvider;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final ObjectProvider<MeterRegistry> meterRegistryObjectProvider;
    private final TaskExecutionService taskExecutionService;
    private final TaskStateService taskStateService;
    private final TransactionOperations transactionOperations;

    @Autowired
    @SuppressFBWarnings("EI")
    public JobResumeFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher, ObjectProvider<ApprovalTokens> approvalTokensProvider,
        JobFacade jobFacade, JobService jobService, ObjectProvider<MeterRegistry> meterRegistryObjectProvider,
        PlatformTransactionManager platformTransactionManager, TaskExecutionService taskExecutionService,
        TaskStateService taskStateService) {

        this(
            applicationEventPublisher, approvalTokensProvider, jobFacade, jobService, meterRegistryObjectProvider,
            new TransactionTemplate(platformTransactionManager), taskExecutionService, taskStateService);
    }

    @SuppressFBWarnings("EI")
    public JobResumeFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher, ObjectProvider<ApprovalTokens> approvalTokensProvider,
        JobFacade jobFacade, JobService jobService, ObjectProvider<MeterRegistry> meterRegistryObjectProvider,
        TransactionOperations transactionOperations, TaskExecutionService taskExecutionService,
        TaskStateService taskStateService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.approvalTokensProvider = approvalTokensProvider;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.meterRegistryObjectProvider = meterRegistryObjectProvider;
        this.taskExecutionService = taskExecutionService;
        this.taskStateService = taskStateService;
        this.transactionOperations = transactionOperations;
    }

    @Override
    public boolean cancelApproval(long jobId) {
        return Objects.requireNonNull(transactionOperations.execute(transactionStatus -> {
            Job job = jobService.getJob(jobId);

            if (!JobSuspends.isAwaitingApproval(job)) {
                return false;
            }

            JobResumeId jobResumeId = JobResumeId.parse((String) job.getMetadata(MetadataConstants.JOB_RESUME_ID));

            JobSuspends.closeCancelledApproval(job);

            job.setEndDate(Instant.now());
            job.setStatus(Job.Status.CANCELLED);

            jobService.update(job);

            taskStateService.delete(jobResumeId);

            applicationEventPublisher.publishEvent(new JobStatusApplicationEvent(jobId, Job.Status.CANCELLED));
            applicationEventPublisher.publishEvent(new JobApprovalClosedEvent(jobId));

            log.info("Cancelled job {} while it waited on an approval", jobId);

            return true;
        }));
    }

    @Override
    public boolean isApprovalResumeId(String id) {
        Optional<String> innerTokenOptional = resolveInnerToken(id);

        if (innerTokenOptional.isEmpty()) {
            return false;
        }

        JobResumeId jobResumeId = parseJobResumeId(innerTokenOptional.get());

        if (jobResumeId == null) {
            return false;
        }

        return callInTenantTransaction(
            jobResumeId, transactionStatus -> jobService.fetchJob(jobResumeId.getJobId())
                .map(JobSuspends::isSuspendedForApproval)
                .orElse(false));
    }

    @Override
    public JobResumeOutcome resumeExpiredJob(String id) {
        JobResumeId jobResumeId = parseJobResumeId(id);

        if (jobResumeId == null) {
            return JobResumeOutcome.INVALID_ID;
        }

        return callInTenantTransaction(jobResumeId, transactionStatus -> {
            Job job = jobService.getJob(jobResumeId.getJobId());

            JobResumeOutcome rejectedOutcome = getRejectedOutcome(job, jobResumeId);

            if (rejectedOutcome == JobResumeOutcome.NOT_YET_SUSPENDED) {
                return rejectedOutcome;
            }

            if (rejectedOutcome != null) {
                log.info(
                    "Ignoring the expired suspend of job {}; the job no longer waits on it ({})",
                    jobResumeId.getJobId(), rejectedOutcome);

                return rejectedOutcome;
            }

            if (JobSuspends.isSuspendedForApproval(job)) {
                failExpiredApproval(job, jobResumeId, "timer");

                return JobResumeOutcome.JOB_FAILED;
            }

            consumeJobResumeId(job);

            if (!jobService.tryClaimResume(job)) {
                log.info(
                    "Ignoring the expired suspend of job {}; a concurrent resolution already resumed it",
                    jobResumeId.getJobId());

                return JobResumeOutcome.GONE;
            }

            jobFacade.resumeClaimedJob(
                jobResumeId.getJobId(), MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID),
                null);

            applicationEventPublisher.publishEvent(new JobResumedEvent(id));

            return JobResumeOutcome.OK;
        });
    }

    @Override
    public JobResumeOutcome resumeApproval(String token) {
        Optional<ApprovalId> approvalIdOptional = parseSignedApprovalId(token);

        if (approvalIdOptional.isEmpty()) {
            log.warn("Rejected an approval link: an approval needs a valid signed token");

            return JobResumeOutcome.INVALID_ID;
        }

        ApprovalId approvalId = approvalIdOptional.get();

        AtomicReference<List<String>> approvalInputErrorsReference = new AtomicReference<>();

        JobResumeOutcome outcome = TenantContext.callWithTenantId(approvalId.getTenantId(), () -> {
            Optional<JobResumeOutcome> outcomeOptional = Objects.requireNonNull(
                transactionOperations.execute(
                    transactionStatus -> resumeSuspendedApproval(approvalId, approvalInputErrorsReference)));

            return outcomeOptional.orElseGet(() -> resumeStoppedApproval(approvalId));
        });

        throwIfApprovalInputsInvalid(approvalInputErrorsReference);

        return outcome;
    }

    @Override
    public JobResumeOutcome resumeJob(String id, Map<String, Object> data) {
        return resumeJob(id, data, null, false, jobId -> {});
    }

    @Override
    public JobResumeOutcome resumeJob(String id, Map<String, Object> data, String approvedBy) {
        return resumeJob(id, data, approvedBy, false, jobId -> {});
    }

    @Override
    public JobResumeOutcome resumeJobStreaming(
        String id, Map<String, Object> data, LongConsumer jobIdConsumer) {

        return resumeJob(id, data, null, true, jobIdConsumer);
    }

    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    private JobResumeOutcome resumeJob(
        String id, Map<String, Object> data, @Nullable String approvedBy, boolean streaming,
        LongConsumer jobIdConsumer) {

        Map<String, Object> resumeData = normalizeApprovedBy(data, approvedBy);

        boolean signedToken = isVerifiedSignedToken(id);

        Optional<String> innerTokenOptional = resolveInnerToken(id);

        if (innerTokenOptional.isEmpty()) {
            log.warn("Invalid resume token: {}", id.replaceAll("[\\r\\n]", ""));

            return JobResumeOutcome.INVALID_ID;
        }

        JobResumeId jobResumeId = parseJobResumeId(innerTokenOptional.get());

        if (jobResumeId == null) {
            return JobResumeOutcome.INVALID_ID;
        }

        AtomicReference<List<String>> approvalInputErrorsReference = new AtomicReference<>();

        JobResumeOutcome outcome = callInTenantTransaction(jobResumeId, transactionStatus -> {
            Job job = jobService.getJob(jobResumeId.getJobId());

            JobResumeOutcome rejectedOutcome = getRejectedOutcome(job, jobResumeId);

            if (rejectedOutcome != null) {
                return rejectedOutcome;
            }

            if (!signedToken && JobSuspends.isSuspendedForApproval(job)) {
                log.warn("Rejected an unsigned resume id for job {}: an approval needs a signed token",
                    jobResumeId.getJobId());

                return JobResumeOutcome.INVALID_ID;
            }

            if (JobSuspends.isSuspendedForApproval(job) && !(resumeData.get(APPROVED) instanceof Boolean)) {
                log.warn("Rejected a resume of job {}: an approval needs an explicit boolean 'approved' decision",
                    jobResumeId.getJobId());

                return JobResumeOutcome.INVALID_ID;
            }

            if (JobSuspends.isSuspendedForSubflow(job)) {
                log.warn("Job {} waits on a sub-workflow and resumes itself when it finishes", jobResumeId.getJobId());

                return JobResumeOutcome.NOT_YET_SUSPENDED;
            }

            if (streaming && !isStreamingResumeAllowed(job)) {
                log.warn("Job {} was not suspended for a streaming resume", jobResumeId.getJobId());

                return JobResumeOutcome.STREAMING_NOT_ALLOWED;
            }

            if (isSuspendExpired(job)) {
                log.warn("Rejected resume for job {}: the pending approval expired", jobResumeId.getJobId());

                failIfApprovalExpired(job, jobResumeId);

                return JobResumeOutcome.GONE;
            }

            Map<String, Object> resolvedResumeData = resumeData;

            if (JobSuspends.isSuspendedForApproval(job)) {
                ApprovalFormInputs.Resolution resolution = resolveApprovalInputs(job, jobResumeId, resumeData);

                if (!resolution.isValid()) {
                    approvalInputErrorsReference.set(resolution.errors());

                    return JobResumeOutcome.INVALID_ID;
                }

                resolvedResumeData = resolution.data();
            }

            consumeJobResumeId(job);

            if (!jobService.tryClaimResume(job)) {
                log.warn(
                    "Lost the resume claim for job {} to a concurrent resolution or the expiry sweep",
                    jobResumeId.getJobId());

                return JobResumeOutcome.GONE;
            }

            jobIdConsumer.accept(jobResumeId.getJobId());

            jobFacade.resumeClaimedJob(
                jobResumeId.getJobId(), MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID),
                resolvedResumeData);

            applicationEventPublisher.publishEvent(new JobResumedEvent(id));

            incrementApprovalResolutionCounter(resumeData);

            return JobResumeOutcome.OK;
        });

        throwIfApprovalInputsInvalid(approvalInputErrorsReference);

        return outcome;
    }

    private void failIfApprovalExpired(Job job, JobResumeId jobResumeId) {
        if (JobSuspends.isSuspendedForApproval(job) && fetchSuspendExpiresAt(job) != null) {
            failExpiredApproval(job, jobResumeId, "resume");
        } else {
            incrementApprovalExpiredCounter("resume");
        }
    }

    private void failExpiredApproval(Job job, JobResumeId jobResumeId, String source) {
        long jobId = jobResumeId.getJobId();

        Instant now = Instant.now();

        Instant expiresAt = fetchSuspendExpiresAt(job);

        JobSuspends.failExpiredApproval(job, expiresAt == null ? now : expiresAt, now);

        jobService.update(job);

        failSuspendedTaskExecution(job, now);

        taskStateService.delete(jobResumeId);

        incrementApprovalExpiredCounter(source);

        applicationEventPublisher.publishEvent(new JobStatusApplicationEvent(jobId, Job.Status.FAILED));
        applicationEventPublisher.publishEvent(new JobApprovalClosedEvent(jobId));

        ExecutionError executionError = Objects.requireNonNull(job.getError());

        log.warn("Failed job {}: {}", jobId, executionError.getMessage());
    }

    private void failSuspendedTaskExecution(Job job, Instant now) {
        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId == null) {
            return;
        }

        Optional<TaskExecution> taskExecutionOptional = taskExecutionService.fetchTaskExecution(taskExecutionResumeId);

        if (taskExecutionOptional.isEmpty()) {
            return;
        }

        TaskExecution taskExecution = taskExecutionOptional.get();

        TaskExecution.Status status = taskExecution.getStatus();

        if (status != null && status.isTerminated()) {
            return;
        }

        taskExecution.setEndDate(now);
        taskExecution.setError(job.getError());
        taskExecution.setStatus(TaskExecution.Status.FAILED);

        taskExecutionService.update(taskExecution);
    }

    private @Nullable Instant fetchSuspendExpiresAt(Job job) {
        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId == null) {
            return null;
        }

        try {
            return taskExecutionService.fetchTaskExecution(taskExecutionResumeId)
                .map(taskExecution -> SuspendUtils.extractSuspendExpiresAt(taskExecution.getMetadata()))
                .orElse(null);
        } catch (RuntimeException exception) {
            log.warn("Could not read the suspend expiry of job {}", job.getId(), exception);

            return null;
        }
    }

    private Optional<JobResumeOutcome> resumeSuspendedApproval(
        ApprovalId approvalId, AtomicReference<List<String>> approvalInputErrorsReference) {

        long jobId = approvalId.getJobId();

        Job job = jobService.getJob(jobId);

        JobResumeOutcome statusOutcome = getStatusRejectedOutcome(job);

        if (statusOutcome != null) {
            return Optional.of(statusOutcome);
        }

        List<String> consumedApprovalIds = getConsumedApprovalIds(job);

        if (consumedApprovalIds.contains(approvalId.getUuidAsString())) {
            log.warn("Rejected a replayed approval link for job {}", jobId);

            return Optional.of(JobResumeOutcome.GONE);
        }

        String storedJobResumeIdString = (String) job.getMetadata(MetadataConstants.JOB_RESUME_ID);
        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (storedJobResumeIdString == null || taskExecutionResumeId == null) {
            return Optional.empty();
        }

        if (Objects.equals(storedJobResumeIdString, job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID))) {
            log.warn("Rejected an approval link for job {}: its suspend was already resolved", jobId);

            return Optional.of(JobResumeOutcome.GONE);
        }

        if (JobSuspends.isSuspendedForSubflow(job)) {
            log.warn("Job {} waits on a sub-workflow and resumes itself when it finishes", jobId);

            return Optional.of(JobResumeOutcome.NOT_YET_SUSPENDED);
        }

        if (!isIssuedForCurrentSuspend(approvalId, job, taskExecutionResumeId)) {
            log.warn("Rejected an approval link for job {}: it was issued for a different suspend", jobId);

            return Optional.of(JobResumeOutcome.INVALID_ID);
        }

        if (isSuspendExpired(job)) {
            log.warn("Rejected an approval link for job {}: the pending approval expired", jobId);

            failIfApprovalExpired(job, JobResumeId.parse(storedJobResumeIdString));

            return Optional.of(JobResumeOutcome.GONE);
        }

        Map<String, Object> approvalData = Map.of(APPROVED, approvalId.isApproved());

        Map<String, Object> resumeData = approvalData;

        if (JobSuspends.isSuspendedForApproval(job)) {
            ApprovalFormInputs.Resolution resolution = resolveApprovalInputs(
                job, JobResumeId.parse(storedJobResumeIdString), approvalData);

            if (!resolution.isValid()) {
                approvalInputErrorsReference.set(resolution.errors());

                return Optional.of(JobResumeOutcome.INVALID_ID);
            }

            resumeData = resolution.data();
        }

        List<String> updatedConsumedApprovalIds = new ArrayList<>(consumedApprovalIds);

        updatedConsumedApprovalIds.add(approvalId.getUuidAsString());

        consumeJobResumeId(job, Map.of(MetadataConstants.CONSUMED_APPROVAL_IDS, updatedConsumedApprovalIds));

        if (!jobService.tryClaimResume(job)) {
            log.warn("Lost the resume claim for job {} to a concurrent resolution or the expiry sweep", jobId);

            return Optional.of(JobResumeOutcome.GONE);
        }

        jobFacade.resumeClaimedJob(jobId, taskExecutionResumeId, resumeData);

        applicationEventPublisher.publishEvent(new JobResumedEvent(storedJobResumeIdString));

        incrementApprovalResolutionCounter(resumeData);

        return Optional.of(JobResumeOutcome.OK);
    }

    private ApprovalFormInputs.Resolution resolveApprovalInputs(
        Job job, JobResumeId jobResumeId, Map<String, Object> resumeData) {

        boolean approved = Boolean.TRUE.equals(resumeData.get(APPROVED));
        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        Optional<TaskExecution> taskExecutionOptional = taskExecutionResumeId == null
            ? Optional.empty() : taskExecutionService.fetchTaskExecution(taskExecutionResumeId);

        if (taskExecutionOptional.isEmpty()) {
            return ApprovalFormInputs.resolve(null, resumeData, approved);
        }

        Map<String, ?> formParameters = ApprovalFormParameters.get(
            taskStateService, jobResumeId, taskExecutionOptional.get());

        ApprovalFormInputs.Resolution resolution = ApprovalFormInputs.resolve(
            formParameters.get(ApprovalFormInputs.INPUTS), resumeData, approved);

        if (!resolution.isValid()) {
            log.warn(
                "Rejected an approval of job {}: its form inputs are invalid: {}", job.getId(), resolution.errors());
        }

        return resolution;
    }

    private static void throwIfApprovalInputsInvalid(AtomicReference<List<String>> approvalInputErrorsReference) {
        List<String> approvalInputErrors = approvalInputErrorsReference.get();

        if (approvalInputErrors != null) {
            throw new ApprovalInputException(approvalInputErrors);
        }
    }

    private JobResumeOutcome resumeStoppedApproval(ApprovalId approvalId) {
        try {
            transactionOperations.executeWithoutResult(
                transactionStatus -> jobFacade.resumeApproval(
                    approvalId.getJobId(), approvalId.getUuidAsString(), approvalId.isApproved()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            log.warn("Rejected an approval link for job {}", approvalId.getJobId(), exception);

            return JobResumeOutcome.GONE;
        }

        return JobResumeOutcome.OK;
    }

    private static boolean isStreamingResumeAllowed(Job job) {
        Map<String, ?> metadata = job.getMetadata();

        return MapUtils.getBoolean(metadata, MetadataConstants.STREAMING_RESUME, false) ||
            MapUtils.getBoolean(metadata, MetadataConstants.STREAMED_EXECUTION, false);
    }

    private static boolean isIssuedForCurrentSuspend(ApprovalId approvalId, Job job, long taskExecutionResumeId) {
        Long issuingTaskExecutionId = approvalId.getIssuingTaskExecutionId();

        if (issuingTaskExecutionId == null || issuingTaskExecutionId == taskExecutionResumeId) {
            return true;
        }

        if (issuingTaskExecutionId > taskExecutionResumeId) {
            return false;
        }

        Long lastResumedTaskExecutionId = MapUtils.getLong(
            job.getMetadata(), MetadataConstants.LAST_RESUMED_TASK_EXECUTION_ID);

        return lastResumedTaskExecutionId == null || lastResumedTaskExecutionId < issuingTaskExecutionId;
    }

    private static List<String> getConsumedApprovalIds(Job job) {
        Object consumedApprovalIds = job.getMetadata(MetadataConstants.CONSUMED_APPROVAL_IDS);

        if (!(consumedApprovalIds instanceof Collection<?> consumedApprovalIdCollection)) {
            return List.of();
        }

        List<String> consumedApprovalIdStrings = new ArrayList<>();

        for (Object consumedApprovalId : consumedApprovalIdCollection) {
            consumedApprovalIdStrings.add(String.valueOf(consumedApprovalId));
        }

        return consumedApprovalIdStrings;
    }

    private <T> T callInTenantTransaction(JobResumeId jobResumeId, TransactionCallback<T> transactionCallback) {
        return TenantContext.callWithTenantId(
            jobResumeId.getTenantId(),
            () -> Objects.requireNonNull(transactionOperations.execute(transactionCallback)));
    }

    private Optional<ApprovalId> parseSignedApprovalId(String token) {
        ApprovalTokens approvalTokens = approvalTokensProvider.getIfAvailable();

        if (approvalTokens == null) {
            return Optional.empty();
        }

        Optional<String> innerTokenOptional = approvalTokens.parseSignedToken(token);

        if (innerTokenOptional.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(ApprovalId.parse(innerTokenOptional.get()));
        } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            return Optional.empty();
        }
    }

    private boolean isVerifiedSignedToken(String id) {
        ApprovalTokens approvalTokens = approvalTokensProvider.getIfAvailable();

        if (approvalTokens == null) {
            return false;
        }

        Optional<String> innerTokenOptional = approvalTokens.parseSignedToken(id);

        return innerTokenOptional.isPresent();
    }

    private Optional<String> resolveInnerToken(String id) {
        ApprovalTokens approvalTokens = approvalTokensProvider.getIfAvailable();

        if (approvalTokens == null) {
            return Optional.of(id);
        }

        return approvalTokens.resolveInnerToken(id);
    }

    private static Map<String, Object> normalizeApprovedBy(Map<String, Object> data, String approvedBy) {
        Map<String, Object> resumeData = data == null ? new HashMap<>() : new HashMap<>(data);

        resumeData.remove(APPROVED_BY);

        if (approvedBy != null && !approvedBy.isBlank()) {
            resumeData.put(APPROVED_BY, approvedBy);
        }

        return resumeData;
    }

    private void incrementApprovalResolutionCounter(Map<String, Object> data) {
        if (!data.containsKey(APPROVED)) {
            return;
        }

        MeterRegistry meterRegistry = meterRegistryObjectProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter(APPROVAL_RESOLUTION_METRIC_NAME, APPROVED, String.valueOf(data.get(APPROVED)))
            .increment();
    }

    private boolean isSuspendExpired(Job job) {
        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId == null) {
            return false;
        }

        Optional<TaskExecution> taskExecutionOptional = taskExecutionService.fetchTaskExecution(taskExecutionResumeId);

        if (taskExecutionOptional.isEmpty()) {
            log.warn(
                "Rejected resume for job {}: its suspended task execution {} no longer exists", job.getId(),
                taskExecutionResumeId);

            return true;
        }

        TaskExecution taskExecution = taskExecutionOptional.get();

        try {
            Instant expiresAt = SuspendUtils.extractSuspendExpiresAt(taskExecution.getMetadata());

            return expiresAt != null && expiresAt.isBefore(Instant.now());
        } catch (RuntimeException exception) {
            log.warn(
                "Rejected resume for job {}: the suspend expiry of task execution {} is unreadable", job.getId(),
                taskExecutionResumeId, exception);

            return true;
        }
    }

    private void incrementApprovalExpiredCounter(String source) {
        MeterRegistry meterRegistry = meterRegistryObjectProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter("bytechef_approval_expired", "source", source)
            .increment();
    }

    private void consumeJobResumeId(Job job) {
        consumeJobResumeId(job, Map.of());
    }

    private void consumeJobResumeId(Job job, Map<String, Object> additionalMetadata) {
        markJobResumeIdConsumed(job, additionalMetadata);

        jobService.update(job);
    }

    private static void markJobResumeIdConsumed(Job job, Map<String, Object> additionalMetadata) {
        Map<String, Object> jobMetadata = new HashMap<>(job.getMetadata());

        jobMetadata.put(MetadataConstants.CONSUMED_JOB_RESUME_ID, job.getMetadata(MetadataConstants.JOB_RESUME_ID));

        Object taskExecutionResumeId = job.getMetadata(MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId != null) {
            jobMetadata.put(MetadataConstants.LAST_RESUMED_TASK_EXECUTION_ID, taskExecutionResumeId);
        }

        jobMetadata.putAll(additionalMetadata);

        job.setMetadata(jobMetadata);
    }

    private static @Nullable JobResumeOutcome getRejectedOutcome(Job job, JobResumeId jobResumeId) {
        String consumedJobResumeIdString = (String) job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID);

        if (jobResumeId.matches(consumedJobResumeIdString)) {
            log.debug("Job {} was already resumed with this resume id", jobResumeId.getJobId());

            return JobResumeOutcome.GONE;
        }

        JobResumeOutcome statusOutcome = getStatusRejectedOutcome(job);

        if (statusOutcome != null) {
            return statusOutcome;
        }

        String storedJobResumeIdString = (String) job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        if (!jobResumeId.matches(storedJobResumeIdString)) {
            log.warn("Resume token does not match stored value for job {}", jobResumeId.getJobId());

            return JobResumeOutcome.INVALID_ID;
        }

        return null;
    }

    private static @Nullable JobResumeOutcome getStatusRejectedOutcome(Job job) {
        Job.Status status = job.getStatus();

        if (status == Job.Status.CREATED || status == Job.Status.STARTED) {
            log.debug("Job {} has not been suspended yet; status is {}", job.getId(), status);

            return JobResumeOutcome.NOT_YET_SUSPENDED;
        }

        if (status == Job.Status.FAILED) {
            if (JobSuspends.isApprovalExpired(job)) {
                log.warn("Cannot resume job {}; its pending approval expired", job.getId());

                return JobResumeOutcome.GONE;
            }

            log.warn("Cannot resume job {}; it failed", job.getId());

            return JobResumeOutcome.JOB_FAILED;
        }

        if (status != Job.Status.STOPPED) {
            log.warn("Cannot resume job {}; status is {}", job.getId(), status);

            return JobResumeOutcome.GONE;
        }

        return null;
    }

    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    private static @Nullable JobResumeId parseJobResumeId(String id) {
        try {
            return JobResumeId.parse(id);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Invalid resume id: {}", id.replaceAll("[\\r\\n]", ""));

            return null;
        }
    }
}
