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
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author Ivica Cardic
 */
public class OrphanedJobRecoveryMonitor {

    private static final Logger log = LoggerFactory.getLogger(OrphanedJobRecoveryMonitor.class);

    public static final String AUTO_RECOVERY_ATTEMPTS = "autoRecoveryAttempts";

    private static final Set<Job.Status> NON_TERMINAL_JOB_STATUSES = EnumSet.of(
        Job.Status.CREATED, Job.Status.STARTED, Job.Status.STOPPED);

    private final boolean autoResume;
    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final int maxAutoResumeAttempts;
    private final Duration stalenessThreshold;
    private final TaskExecutionService taskExecutionService;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI2")
    public OrphanedJobRecoveryMonitor(
        boolean autoResume, ApplicationEventPublisher eventPublisher, JobService jobService,
        int maxAutoResumeAttempts, Duration stalenessThreshold, TaskExecutionService taskExecutionService,
        TenantService tenantService) {

        this.autoResume = autoResume;
        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.maxAutoResumeAttempts = maxAutoResumeAttempts;
        this.stalenessThreshold = stalenessThreshold;
        this.taskExecutionService = taskExecutionService;
        this.tenantService = tenantService;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1M")
    public void recoverOrphanedJobs() {
        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, this::recoverOrphanedJobsForCurrentTenant);
            } catch (RuntimeException exception) {
                log.warn("Orphaned-job sweep failed for tenant {}", tenantId, exception);
            }
        }
    }

    private void recoverOrphanedJobsForCurrentTenant() {
        Instant cutoff = Instant.now()
            .minus(stalenessThreshold);

        Set<Long> candidateJobIds = new LinkedHashSet<>();

        try {
            for (TaskExecution taskExecution : taskExecutionService.getStaleTaskExecutions(
                TaskExecution.Status.STARTED, cutoff)) {

                if (taskExecution.getJobId() != null) {
                    candidateJobIds.add(taskExecution.getJobId());
                }
            }

            for (Job job : jobService.getStaleJobs(Job.Status.STARTED, cutoff)) {
                candidateJobIds.add(job.getId());
            }
        } catch (UnsupportedOperationException exception) {
            if (log.isTraceEnabled()) {
                log.trace("Orphan detection is not supported in this deployment", exception);
            }

            return;
        }

        for (Long jobId : candidateJobIds) {
            try {
                recoverJob(jobId, cutoff);
            } catch (RuntimeException exception) {
                log.warn("Failed to recover orphaned job {}", jobId, exception);
            }
        }
    }

    private boolean hasNonTerminalChildJob(long jobId) {
        List<Long> childJobIds;

        try {
            childJobIds = jobService.getChildJobIds(jobId);
        } catch (UnsupportedOperationException exception) {
            if (log.isTraceEnabled()) {
                log.trace("Child-job lookup is not supported in this deployment", exception);
            }

            return false;
        }

        if (childJobIds.isEmpty()) {
            return false;
        }

        for (Job childJob : jobService.getJobs(childJobIds)) {
            if (NON_TERMINAL_JOB_STATUSES.contains(childJob.getStatus())) {
                return true;
            }
        }

        return false;
    }

    private void recoverJob(long jobId, Instant cutoff) {
        Job job = jobService.getJob(jobId);

        if (job.getStatus() != Job.Status.STARTED) {
            return;
        }

        Instant jobLastModifiedDate = job.getLastModifiedDate();

        if (jobLastModifiedDate != null && jobLastModifiedDate.isAfter(cutoff)) {
            return;
        }

        List<TaskExecution> nonTerminalTaskExecutions = taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .filter(taskExecution -> taskExecution.getStatus() == null ||
                !taskExecution.getStatus()
                    .isTerminated())
            .toList();

        boolean anyFresh = nonTerminalTaskExecutions.stream()
            .anyMatch(taskExecution -> taskExecution.getLastModifiedDate() != null &&
                taskExecution.getLastModifiedDate()
                    .isAfter(cutoff));

        if (anyFresh) {
            return;
        }

        if (hasNonTerminalChildJob(jobId)) {
            return;
        }

        Instant now = Instant.now();

        for (TaskExecution taskExecution : nonTerminalTaskExecutions) {
            taskExecution.setStatus(TaskExecution.Status.FAILED);
            taskExecution.setEndDate(now);
            taskExecution.setError(
                new ExecutionError(
                    "Orphaned execution: no heartbeat received before %s; the executing node likely crashed"
                        .formatted(cutoff),
                    List.of()));

            taskExecutionService.update(taskExecution);
        }

        int autoRecoveryAttempts = getAutoRecoveryAttempts(job);
        boolean resume = autoResume && job.getParentTaskExecutionId() == null &&
            autoRecoveryAttempts < maxAutoResumeAttempts;

        if (resume) {
            Map<String, Object> metadata = new HashMap<>(job.getMetadata());

            metadata.put(AUTO_RECOVERY_ATTEMPTS, autoRecoveryAttempts + 1);

            job.setMetadata(metadata);
        }

        job.setStatus(Job.Status.FAILED);
        job.setEndDate(now);

        jobService.update(job);

        log.warn(
            "Recovered orphaned job {}: marked {} task execution(s) and the job FAILED{}", jobId,
            nonTerminalTaskExecutions.size(), resume ? "; auto-resume attempt " + (autoRecoveryAttempts + 1) : "");

        eventPublisher.publishEvent(new JobStatusApplicationEvent(jobId, Job.Status.FAILED));

        if (resume) {
            eventPublisher.publishEvent(new ResumeJobEvent(jobId));
        }
    }

    private static int getAutoRecoveryAttempts(Job job) {
        Object attempts = job.getMetadata(AUTO_RECOVERY_ATTEMPTS);

        if (attempts instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }
}
