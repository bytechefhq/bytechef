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
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Fails runs whose pending human approval expired. A suspended run sits STOPPED with a {@code jobResumeId} in its job
 * metadata and the suspend expiry ({@code suspend.expiresAt}) on the suspended task execution; without this sweep an
 * abandoned approval keeps the run STOPPED until retention purges it, silently. Expired runs are marked FAILED — task
 * execution and job, mirroring {@link OrphanedJobRecoveryMonitor}'s semantics — which fires the normal job-status
 * fan-out (notifications, SSE, alert rules). The resume endpoint independently rejects expired resumes; this sweep is
 * the janitor for runs nobody ever came back to.
 *
 * <p>
 * Metrics: {@code bytechef_approval_expired{source=sweep}} counts expired runs; the {@code bytechef_approval_pending}
 * gauge tracks the number of runs currently paused on an (unexpired) approval, refreshed on every sweep across all
 * tenants. In the distributed EE deployment the stale-STOPPED-jobs finder is served over REST by the execution app (see
 * {@code RemoteJobServiceClient#getStaleJobs}), so the sweep runs there too; the {@code UnsupportedOperationException}
 * catch below is a defensive fallback for any finder implementation that does not support it.
 * </p>
 *
 * @author Ivica Cardic
 */
public class ApprovalExpiryMonitor {

    private static final Logger log = LoggerFactory.getLogger(ApprovalExpiryMonitor.class);

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
            } catch (UnsupportedOperationException unsupportedOperationException) {
                if (log.isDebugEnabled()) {
                    log.debug("Stale-job finder unavailable for tenant {}; skipping approval-expiry sweep", tenantId);
                }
            } catch (Exception exception) {
                log.warn(
                    "Approval-expiry sweep failed for tenant {}: {}", tenantId, exception.getMessage());
            }
        }

        pendingApprovalCount.set(pending);
    }

    /**
     * Sweeps the current tenant's STOPPED jobs: fails those whose suspend expired, and returns the count of runs still
     * legitimately pending an approval (feeding the pending gauge).
     */
    private long sweepCurrentTenant() {
        List<Job> stoppedJobs = jobService.getStaleJobs(Job.Status.STOPPED, Instant.now());

        long pending = 0;

        for (Job job : stoppedJobs) {
            if (job.getMetadata(MetadataConstants.JOB_RESUME_ID) == null) {
                continue;
            }

            Instant expiresAt = readSuspendExpiry(job);

            if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
                try {
                    failExpiredJob(job, expiresAt);

                    incrementExpiredCounter();
                } catch (Exception exception) {
                    // A concurrent resume or a competing replica may have already moved this job (optimistic-lock
                    // failure). That is fine — another actor got there first; log and keep sweeping the rest of the
                    // tenant's jobs instead of aborting the whole cycle.
                    log.warn(
                        "Could not fail expired job {}; skipping it this cycle: {}", job.getId(),
                        exception.getMessage());
                }
            } else {
                pending++;
            }
        }

        return pending;
    }

    private @Nullable Instant readSuspendExpiry(Job job) {
        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId == null) {
            return null;
        }

        try {
            TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionResumeId);

            return SuspendUtils.extractSuspendExpiresAt(taskExecution.getMetadata());
        } catch (Exception exception) {
            return null;
        }
    }

    private void failExpiredJob(Job job, Instant expiresAt) {
        long jobId = Objects.requireNonNull(job.getId(), "id");

        Instant now = Instant.now();

        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId != null) {
            try {
                TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionResumeId);

                TaskExecution.Status status = taskExecution.getStatus();

                if (status == null || !status.isTerminated()) {
                    taskExecution.setStatus(TaskExecution.Status.FAILED);
                    taskExecution.setEndDate(now);
                    taskExecution.setError(
                        new ExecutionError(
                            "The pending approval expired at %s without being resolved".formatted(expiresAt),
                            List.of()));

                    taskExecutionService.update(taskExecution);
                }
            } catch (Exception exception) {
                log.warn(
                    "Could not mark suspended task execution {} of job {} failed: {}", taskExecutionResumeId, jobId,
                    exception.getMessage());
            }
        }

        job.setStatus(Job.Status.FAILED);
        job.setEndDate(now);

        jobService.update(job);

        // The suspended-task state row (keyed by jobResumeId) is otherwise deleted only on resume, so an expired
        // approval would leak it — and the stored suspend payload (the rendered approval request) with it. Delete it
        // best-effort: a cleanup failure must never keep the run STOPPED.
        deleteTaskState(job);

        log.warn("Failed job {}: its pending approval expired at {}", jobId, expiresAt);

        eventPublisher.publishEvent(new JobStatusApplicationEvent(jobId, Job.Status.FAILED));
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
        } catch (Exception exception) {
            log.warn(
                "Could not delete task state for expired job {}: {}", job.getId(), exception.getMessage());
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
