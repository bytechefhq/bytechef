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

package com.bytechef.platform.job.sync.executor;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.workflow.execution.JobSuspends;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.function.BiFunction;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Fails a synchronously executed job when one of its steps tries to suspend it. It claims the same task executions as
 * the suspend completion handler, so it must be registered ahead of it.
 *
 * @author Ivica Cardic
 */
public class SyncExecutionSuspendRejectingTaskCompletionHandler implements TaskCompletionHandler {

    private final BiFunction<String, String, String> errorMessageFunction;
    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;

    /**
     * @param errorMessageFunction builds the failed step's error from the step's name and type
     */
    @SuppressFBWarnings("EI")
    public SyncExecutionSuspendRejectingTaskCompletionHandler(
        ApplicationEventPublisher eventPublisher, JobService jobService, TaskExecutionService taskExecutionService,
        BiFunction<String, String, String> errorMessageFunction) {

        this.errorMessageFunction = errorMessageFunction;
        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        return JobSuspends.isSuspending(taskExecution);
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        taskExecution.setHandled(true);

        failJob(taskExecution, errorMessageFunction, eventPublisher, jobService, taskExecutionService);
    }

    static void failJob(
        TaskExecution taskExecution, BiFunction<String, String, String> errorMessageFunction,
        ApplicationEventPublisher eventPublisher, JobService jobService, TaskExecutionService taskExecutionService) {

        Instant endDate = Instant.now();

        ExecutionError executionError = new ExecutionError(
            errorMessageFunction.apply(taskExecution.getName(), taskExecution.getType()), List.of());

        taskExecution.setEndDate(endDate);
        taskExecution.setError(executionError);
        taskExecution.setStatus(TaskExecution.Status.FAILED);

        taskExecution = taskExecutionService.update(taskExecution);

        while (taskExecution.getParentId() != null) {
            taskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

            taskExecution.setEndDate(endDate);
            taskExecution.setStatus(TaskExecution.Status.FAILED);

            taskExecution = taskExecutionService.update(taskExecution);
        }

        Job job = jobService.getTaskExecutionJob(Validate.notNull(taskExecution.getId(), "id"));

        Validate.notNull(job, "No job found for task execution %s", taskExecution.getId());

        job.setEndDate(endDate);
        job.setError(executionError);
        job.setStatus(Job.Status.FAILED);

        job = jobService.update(job);

        eventPublisher
            .publishEvent(new JobStatusApplicationEvent(Validate.notNull(job.getId(), "id"), job.getStatus()));
    }
}
