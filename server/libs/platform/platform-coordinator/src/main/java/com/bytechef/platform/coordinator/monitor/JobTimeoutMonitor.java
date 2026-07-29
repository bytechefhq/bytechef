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
import com.bytechef.error.ExecutionError;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author Ivica Cardic
 */
public class JobTimeoutMonitor {

    private static final Logger log = LoggerFactory.getLogger(JobTimeoutMonitor.class);

    @Nullable
    private final Duration defaultTimeout;

    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI2")
    public JobTimeoutMonitor(
        @Nullable Duration defaultTimeout, ApplicationEventPublisher eventPublisher, JobService jobService,
        TaskExecutionService taskExecutionService, TenantService tenantService) {

        this.defaultTimeout = defaultTimeout;
        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.tenantService = tenantService;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1M")
    public void timeOutLongRunningJobs() {
        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, this::timeOutLongRunningJobsForCurrentTenant);
            } catch (RuntimeException exception) {
                log.warn("Job-timeout sweep failed for tenant {}", tenantId, exception);
            }
        }
    }

    private void timeOutLongRunningJobsForCurrentTenant() {
        Duration timeout = defaultTimeout;

        if (timeout == null) {
            return;
        }

        Instant cutoff = Instant.now()
            .minus(timeout);

        List<Job> longRunningJobs;

        try {
            longRunningJobs = jobService.getLongRunningJobs(Job.Status.STARTED, cutoff);
        } catch (UnsupportedOperationException exception) {
            log.debug("Long-running-job query unsupported in this deployment; skipping timeout enforcement");

            return;
        }

        for (Job job : longRunningJobs) {
            Long jobId = job.getId();

            if (jobId == null) {
                continue;
            }

            try {
                timeOutJob(jobId, cutoff, timeout);
            } catch (RuntimeException exception) {
                log.warn("Failed to time out job {}", jobId, exception);
            }
        }
    }

    private void timeOutJob(long jobId, Instant cutoff, Duration timeout) {
        Job job = jobService.getJob(jobId);

        if (job.getStatus() != Job.Status.STARTED) {
            return;
        }

        Instant jobStartDate = job.getStartDate();

        if (jobStartDate == null || jobStartDate.isAfter(cutoff)) {
            return;
        }

        Instant now = Instant.now();

        List<TaskExecution> nonTerminalTaskExecutions = taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .filter(taskExecution -> taskExecution.getStatus() == null ||
                !taskExecution.getStatus()
                    .isTerminated())
            .toList();

        for (TaskExecution taskExecution : nonTerminalTaskExecutions) {
            taskExecution.setStatus(TaskExecution.Status.FAILED);
            taskExecution.setEndDate(now);
            taskExecution.setError(
                new ExecutionError(
                    "Execution timed out: the run exceeded the configured limit of %s".formatted(timeout),
                    List.of()));

            taskExecutionService.update(taskExecution);
        }

        job.setStatus(Job.Status.FAILED);
        job.setEndDate(now);

        jobService.update(job);

        log.warn(
            "Timed out job {} after exceeding the run limit of {}: marked {} task execution(s) and the job FAILED",
            jobId, timeout, nonTerminalTaskExecutions.size());

        eventPublisher.publishEvent(new JobStatusApplicationEvent(jobId, Job.Status.FAILED));
    }
}
