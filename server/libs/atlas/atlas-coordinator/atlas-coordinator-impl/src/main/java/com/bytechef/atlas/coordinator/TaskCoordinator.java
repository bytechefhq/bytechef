
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

package com.bytechef.atlas.coordinator;

import com.bytechef.atlas.coordinator.job.executor.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.error.ExecutionError;
import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.event.JobStatusWorkflowEvent;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.CancelControlTask;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.bytechef.commons.util.ExceptionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The central class responsible for coordinating and executing jobs.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 12, 2016
 */
public class TaskCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TaskCoordinator.class);

    private final EventPublisher eventPublisher;
    private final JobExecutor jobExecutor;
    private final JobService jobService;
    private final MessageBroker messageBroker;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;

    private TaskCoordinator(Builder builder) {
        this.eventPublisher = builder.eventPublisher;
        this.jobExecutor = builder.jobExecutor;
        this.jobService = builder.jobService;
        this.messageBroker = builder.messageBroker;
        this.taskCompletionHandler = builder.taskCompletionHandler;
        this.taskDispatcher = builder.taskDispatcher;
        this.taskExecutionService = builder.taskExecutionService;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressFBWarnings("NP")
    public void start(Long jobId) {
        Job job = jobService.setStatusToStarted(jobId);

        jobExecutor.execute(job);

        eventPublisher.publishEvent(new JobStatusWorkflowEvent(Objects.requireNonNull(job.getId()), job.getStatus()));

        if (log.isDebugEnabled()) {
            log.debug("Job id={}, label='{}' started", job.getId(), job.getLabel());
        }
    }

    /**
     * Stop a running job.
     *
     * @param jobId The id of the job to stop
     */
    @SuppressFBWarnings("NP")
    public void stop(Long jobId) {
        Job job = jobService.setStatusToStopped(jobId);

        eventPublisher.publishEvent(new JobStatusWorkflowEvent(Objects.requireNonNull(job.getId()), job.getStatus()));

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

        if (taskExecutions.size() > 0) {
            TaskExecution currentTaskExecution = taskExecutions.get(taskExecutions.size() - 1);

            currentTaskExecution.setEndDate(LocalDateTime.now());
            currentTaskExecution.setStatus(TaskExecution.Status.CANCELLED);

            taskExecutionService.update(currentTaskExecution);

            taskDispatcher.dispatch(
                new CancelControlTask(
                    Objects.requireNonNull(currentTaskExecution.getJobId()),
                    Objects.requireNonNull(currentTaskExecution.getId())));
        }

        if (log.isDebugEnabled()) {
            log.debug("Job id={}, label='{}' stopped", job.getId(), job.getLabel());
        }
    }

    /**
     * Resume a stopped or failed job.
     *
     * @param jobId The id of the job to resume.
     */
    public void resume(Long jobId) {
        Job job = jobService.resumeToStatusStarted(jobId);

        jobExecutor.execute(job);

        if (log.isDebugEnabled()) {
            log.debug("Job id={}, label='{}' resumed", job.getId(), job.getLabel());
        }
    }

    /**
     * Complete a task of a given job.
     *
     * @param taskExecution The task to complete.
     */
    public void complete(TaskExecution taskExecution) {
        try {
            taskCompletionHandler.handle(taskExecution);
        } catch (Exception e) {
            taskExecution.setError(new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            messageBroker.send(SystemMessageRoute.ERRORS, taskExecution);
        }
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private EventPublisher eventPublisher;
        private JobExecutor jobExecutor;
        private JobService jobService;
        private MessageBroker messageBroker;
        private TaskCompletionHandler taskCompletionHandler;
        private TaskDispatcher<? super Task> taskDispatcher;
        private TaskExecutionService taskExecutionService;

        private Builder() {
        }

        public Builder eventPublisher(EventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
            return this;
        }

        public Builder jobExecutor(JobExecutor jobExecutor) {
            this.jobExecutor = jobExecutor;
            return this;
        }

        public Builder jobService(JobService jobService) {
            this.jobService = jobService;
            return this;
        }

        public Builder messageBroker(MessageBroker messageBroker) {
            this.messageBroker = messageBroker;
            return this;
        }

        public Builder taskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
            this.taskCompletionHandler = taskCompletionHandler;
            return this;
        }

        public Builder taskDispatcher(TaskDispatcher<? super Task> taskDispatcher) {
            this.taskDispatcher = taskDispatcher;
            return this;
        }

        public Builder taskExecutionService(TaskExecutionService taskExecutionService) {
            this.taskExecutionService = taskExecutionService;
            return this;
        }

        public TaskCoordinator build() {
            return new TaskCoordinator(this);
        }
    }
}
