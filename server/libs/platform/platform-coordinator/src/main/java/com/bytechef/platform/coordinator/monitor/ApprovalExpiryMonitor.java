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

package com.bytechef.platform.coordinator.monitor;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.SuspendUtils;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.JobSuspends;
import com.bytechef.platform.workflow.execution.event.JobApprovalClosedEvent;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author Ivica Cardic
 */
public class ApprovalExpiryMonitor {

    private static final Logger log = LoggerFactory.getLogger(ApprovalExpiryMonitor.class);

    private static final String ORPHANED_APPROVAL_REASON =
        "The suspended task execution of the pending approval no longer exists";

    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final ObjectProvider<MeterRegistry> meterRegistryObjectProvider;
    private final AtomicLong pendingApprovalCount = new AtomicLong();
    private final TaskExecutionService taskExecutionService;
    private final @Nullable TaskStateService taskStateService;
    private final TenantService tenantService;

    private boolean gaugeRegistered;

    @SuppressFBWarnings("EI")
    public ApprovalExpiryMonitor(
        ApplicationEventPublisher eventPublisher, JobService jobService,
        ObjectProvider<MeterRegistry> meterRegistryObjectProvider, TaskExecutionService taskExecutionService,
        @Nullable TaskStateService taskStateService, TenantService tenantService) {

        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.meterRegistryObjectProvider = meterRegistryObjectProvider;
        this.taskExecutionService = taskExecutionService;
        this.taskStateService = taskStateService;
        this.tenantService = tenantService;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT15M")
    public void sweepExpiredApprovals() {
        registerPendingGaugeIfNeeded();

        long pending = 0;

        for (String tenantId : tenantService.getTenantIds()) {
            try {
                pending += TenantContext.callWithTenantId(tenantId, this::sweepCurrentTenant);
            } catch (RuntimeException runtimeException) {
                log.warn("Approval-expiry sweep failed for tenant {}", tenantId, runtimeException);
            }
        }

        pendingApprovalCount.set(pending);
    }

    private long sweepCurrentTenant() {
        Instant now = Instant.now();

        List<Job> stoppedJobs;

        try {
            stoppedJobs = jobService.getStaleJobs(Job.Status.STOPPED, now);
        } catch (UnsupportedOperationException unsupportedOperationException) {
            if (log.isDebugEnabled()) {
                log.debug("Stale-job finder unavailable; skipping approval-expiry sweep");
            }

            return 0;
        }

        long pending = 0;

        for (Job job : stoppedJobs) {
            if (JobSuspends.isAwaitingApproval(job) && !sweepJob(job, now)) {
                pending++;
            }
        }

        return pending;
    }

    private boolean sweepJob(Job job, Instant now) {
        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId == null) {
            return false;
        }

        TaskExecution taskExecution;

        try {
            taskExecution = taskExecutionService.getTaskExecution(taskExecutionResumeId);
        } catch (IllegalStateException illegalStateException) {
            log.warn(
                "The suspended task execution {} of job {} no longer exists; failing the orphaned job",
                taskExecutionResumeId, job.getId(), illegalStateException);

            return failJob(job, null, null);
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Could not read the suspended task execution {} of job {}; retrying next sweep", taskExecutionResumeId,
                job.getId(), runtimeException);

            return false;
        }

        Instant expiresAt = SuspendUtils.extractSuspendExpiresAt(taskExecution.getMetadata());

        if (expiresAt == null || !expiresAt.isBefore(now)) {
            return false;
        }

        return failJob(job, taskExecution, expiresAt);
    }

    private boolean failJob(Job job, @Nullable TaskExecution taskExecution, @Nullable Instant expiresAt) {
        long jobId = Objects.requireNonNull(job.getId(), "id");

        Instant now = Instant.now();

        if (expiresAt == null) {
            job.setEndDate(now);
            job.setError(new ExecutionError(ORPHANED_APPROVAL_REASON, List.of()));
            job.setStatus(Job.Status.FAILED);
        } else {
            JobSuspends.failExpiredApproval(job, expiresAt, now);
        }

        String reason = expiresAt == null ? ORPHANED_APPROVAL_REASON : JobSuspends.approvalExpiredReason(expiresAt);

        try {
            jobService.update(job);
        } catch (OptimisticLockingFailureException optimisticLockingFailureException) {
            log.info("Job {} was resolved concurrently; not failing it", jobId);

            return true;
        } catch (RuntimeException runtimeException) {
            log.warn("Could not fail job {}; retrying next sweep", jobId, runtimeException);

            return false;
        }

        if (taskExecution != null) {
            failTaskExecution(taskExecution, jobId, now, reason);
        }

        deleteTaskState(job);

        incrementExpiredCounter();

        log.warn("Failed job {}: {}", jobId, reason);

        eventPublisher.publishEvent(new JobStatusApplicationEvent(jobId, Job.Status.FAILED));
        eventPublisher.publishEvent(new JobApprovalClosedEvent(jobId));

        return true;
    }

    private void failTaskExecution(TaskExecution taskExecution, long jobId, Instant now, String reason) {
        TaskExecution.Status status = taskExecution.getStatus();

        if (status != null && status.isTerminated()) {
            return;
        }

        taskExecution.setStatus(TaskExecution.Status.FAILED);
        taskExecution.setEndDate(now);
        taskExecution.setError(new ExecutionError(reason, List.of()));

        try {
            taskExecutionService.update(taskExecution);
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Could not mark suspended task execution {} of job {} failed", taskExecution.getId(), jobId,
                runtimeException);
        }
    }

    private void deleteTaskState(Job job) {
        if (taskStateService == null) {
            return;
        }

        Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        if (jobResumeId == null) {
            return;
        }

        try {
            taskStateService.delete(JobResumeId.parse(jobResumeId.toString()));
        } catch (RuntimeException runtimeException) {
            log.warn("Could not delete task state for expired job {}", job.getId(), runtimeException);
        }
    }

    private void incrementExpiredCounter() {
        MeterRegistry meterRegistry = meterRegistryObjectProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter("bytechef_approval_expired", "source", "sweep")
            .increment();
    }

    private void registerPendingGaugeIfNeeded() {
        if (gaugeRegistered) {
            return;
        }

        MeterRegistry meterRegistry = meterRegistryObjectProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        meterRegistry.gauge("bytechef_approval_pending", pendingApprovalCount);

        gaugeRegistered = true;
    }
}
