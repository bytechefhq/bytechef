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

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.worker.event.CancelControlTaskEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
final class JobMonitorSupport {

    private static final Logger log = LoggerFactory.getLogger(JobMonitorSupport.class);

    private static final Set<Job.Status> NON_TERMINAL_JOB_STATUSES = EnumSet.of(
        Job.Status.CREATED, Job.Status.STARTED, Job.Status.STOPPED);

    private JobMonitorSupport() {
    }

    static boolean hasNonTerminalChildJob(JobService jobService, long jobId) {
        List<Long> childJobIds;

        try {
            childJobIds = jobService.getChildJobIds(jobId);
        } catch (UnsupportedOperationException exception) {
            if (log.isTraceEnabled()) {
                log.trace("Child-job lookup is not supported in this deployment", exception);
            }

            return true;
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

    static List<TaskExecution> getNonTerminalTaskExecutions(TaskExecutionService taskExecutionService, long jobId) {
        return taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .filter(taskExecution -> taskExecution.getStatus() == null ||
                !taskExecution.getStatus()
                    .isTerminated())
            .toList();
    }

    static void failAndCancelTaskExecutions(
        long jobId, List<TaskExecution> taskExecutions, String errorMessage,
        TaskExecutionService taskExecutionService, ApplicationEventPublisher eventPublisher) {

        List<Long> failedTaskExecutionIds = new ArrayList<>();

        for (TaskExecution taskExecution : taskExecutions) {
            long taskExecutionId = Objects.requireNonNull(taskExecution.getId(), "id");

            if (taskExecutionService.failIfUnfinished(taskExecutionId, errorMessage)) {
                failedTaskExecutionIds.add(taskExecutionId);
            }
        }

        for (long failedTaskExecutionId : failedTaskExecutionIds) {
            CancelControlTask cancelControlTask = new CancelControlTask(jobId, failedTaskExecutionId);

            eventPublisher.publishEvent(new CancelControlTaskEvent(cancelControlTask));
        }
    }
}
