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
import com.bytechef.platform.component.constant.MetadataConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Fails a synchronously executed job when one of its steps tries to suspend it. It claims the same task executions as
 * the suspend completion handler, so it must be registered ahead of it.
 *
 * @author Ivica Cardic
 */
public class SyncExecutionSuspendRejectingTaskCompletionHandler implements TaskCompletionHandler {

    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final String messageFormat;
    private final TaskExecutionService taskExecutionService;

    /**
     * @param messageFormat a {@link String#format} pattern for the failed step's error, given the step's name and then
     *                      its type
     */
    @SuppressFBWarnings("EI")
    public SyncExecutionSuspendRejectingTaskCompletionHandler(
        ApplicationEventPublisher eventPublisher, JobService jobService, TaskExecutionService taskExecutionService,
        String messageFormat) {

        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.messageFormat = messageFormat;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        return taskExecution.getMetadata()
            .containsKey(MetadataConstants.JOB_RESUME_ID);
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        taskExecution.setHandled(true);

        Instant endDate = Instant.now();

        ExecutionError executionError = new ExecutionError(
            String.format(messageFormat, taskExecution.getName(), taskExecution.getType()), List.of());

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
