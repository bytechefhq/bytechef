
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.coordinator.error;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.error.ErrorHandler;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.event.JobStatusWorkflowEvent;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.utils.UUIDUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apr 10, 2017
 */
public class TaskExecutionErrorHandler implements ErrorHandler<TaskExecution> {

    private final EventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskDispatcher<TaskExecution> taskDispatcher;
    private final TaskExecutionService taskExecutionService;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @SuppressFBWarnings("EI2")
    public TaskExecutionErrorHandler(
        EventPublisher eventPublisher,
        JobService jobService,
        TaskDispatcher<TaskExecution> taskDispatcher,
        TaskExecutionService taskExecutionService) {
        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        ExecutionError error = taskExecution.getError();

        Assert.notNull(error, "error must not be null");

        logger.error("Task {}: {}\n{}", taskExecution.getId(), error.getMessage(), error.getStackTrace());

        // set task status to FAILED and persist
        TaskExecution erroredTaskExecution = new TaskExecution(taskExecution);

        erroredTaskExecution.setStatus(TaskStatus.FAILED);
        erroredTaskExecution.setEndTime(LocalDateTime.now());

        taskExecutionService.update(erroredTaskExecution);

        // if the task is retryable, then retry it
        if (taskExecution.getRetryAttempts() < taskExecution.getRetry()) {
            TaskExecution retryTaskExecution = new TaskExecution(taskExecution);

            retryTaskExecution.setId(UUIDUtils.generate());
            retryTaskExecution.setStatus(TaskStatus.CREATED);
            retryTaskExecution.setError(null);
            retryTaskExecution.setRetryAttempts(taskExecution.getRetryAttempts() + 1);

            retryTaskExecution = taskExecutionService.add(retryTaskExecution);

            taskDispatcher.dispatch(retryTaskExecution);
        }
        // if it's not retryable then we're gonna fail the job
        else {
            while (erroredTaskExecution.getParentId() != null) { // mark parent tasks as FAILED as well
                erroredTaskExecution = new TaskExecution(
                    taskExecutionService.getTaskExecution(erroredTaskExecution.getParentId()));
                erroredTaskExecution.setStatus(TaskStatus.FAILED);
                erroredTaskExecution.setEndTime(LocalDateTime.now());

                taskExecutionService.update(erroredTaskExecution);
            }

            Job job = jobService.getTaskExecutionJob(erroredTaskExecution.getId());

            Assert.notNull(job, "job not found for task: " + erroredTaskExecution.getId());

            Job updateJob = new Job(job);

            Assert.notNull(updateJob, String.format("No job found for task %s ", erroredTaskExecution.getId()));

            updateJob.setStatus(JobStatus.FAILED);
            updateJob.setEndTime(new Date());

            jobService.update(updateJob);
            eventPublisher.publishEvent(new JobStatusWorkflowEvent(updateJob.getId(), updateJob.getStatus()));
        }
    }
}
