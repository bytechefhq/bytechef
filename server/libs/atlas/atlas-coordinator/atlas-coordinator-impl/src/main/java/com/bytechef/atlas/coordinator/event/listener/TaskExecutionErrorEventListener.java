/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.coordinator.event.listener;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.event.ErrorEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apr 10, 2017
 */
public class TaskExecutionErrorEventListener implements ErrorEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionErrorEventListener.class);

    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public TaskExecutionErrorEventListener(
        ApplicationEventPublisher eventPublisher, JobService jobService,
        TaskDispatcher<? super Task> taskDispatcher, TaskExecutionService taskExecutionService) {

        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void onErrorEvent(ErrorEvent errorEvent) {
        if (errorEvent instanceof TaskExecutionErrorEvent taskExecutionErrorEvent) {
            TaskExecution taskExecution = taskExecutionErrorEvent.getTaskExecution();

            ExecutionError error = taskExecution.getError();

            Validate.notNull(error, "'error' must not be null");

            logger.error(
                "Task id={}: message={}\nstackTrace={}", taskExecution.getId(), error.getMessage(),
                error.getStackTrace());

            // set task status to FAILED and persist

            taskExecution.setEndDate(LocalDateTime.now());
            taskExecution.setStatus(TaskExecution.Status.FAILED);

            taskExecution = taskExecutionService.update(taskExecution);

            // if the task is retryable, then retry it
            if (taskExecution.getRetryAttempts() < taskExecution.getMaxRetries()) {
                taskExecution.setStatus(TaskExecution.Status.CREATED);
                taskExecution.setError(null);
                taskExecution.setRetryAttempts(taskExecution.getRetryAttempts() + 1);

                taskExecution = taskExecutionService.create(taskExecution);

                taskDispatcher.dispatch(taskExecution);
            }
            // if it's not retryable then we're going fail the job
            else {
                while (taskExecution.getParentId() != null) { // mark parent tasks as FAILED as well
                    taskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

                    taskExecution.setEndDate(LocalDateTime.now());
                    taskExecution.setStatus(TaskExecution.Status.FAILED);

                    taskExecution = taskExecutionService.update(taskExecution);
                }

                Job job = jobService.getTaskExecutionJob(Validate.notNull(taskExecution.getId(), "id"));

                Validate.notNull(job, "No job found for task %s", taskExecution.getId());

                job.setStatus(Job.Status.FAILED);
                job.setEndDate(LocalDateTime.now());

                jobService.update(job);

                eventPublisher
                    .publishEvent(new JobStatusApplicationEvent(Validate.notNull(job.getId(), "id"), job.getStatus()));
            }
        }
    }
}
