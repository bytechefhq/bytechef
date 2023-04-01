
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
import com.bytechef.atlas.dto.JobParametersDTO;
import com.bytechef.atlas.error.ErrorHandler;
import com.bytechef.atlas.error.Errorable;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.event.JobStatusWorkflowEvent;
import com.bytechef.atlas.job.JobFactory;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.CancelControlTask;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.execution.TaskStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.bytechef.commons.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The central class responsible for coordinating and executing jobs.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 12, 2016
 */
public class Coordinator {

    private static final Logger log = LoggerFactory.getLogger(Coordinator.class);

    private final ErrorHandler<? super Errorable> errorHandler;
    private final EventPublisher eventPublisher;
    private final JobExecutor jobExecutor;
    private final JobFactory jobFactory;
    private final JobService jobService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;

    private Coordinator(Builder builder) {
        this.errorHandler = builder.errorHandler;
        this.eventPublisher = builder.eventPublisher;
        this.jobExecutor = builder.jobExecutor;
        this.jobFactory = builder.jobFactory;
        this.jobService = builder.jobService;
        this.taskCompletionHandler = builder.taskCompletionHandler;
        this.taskDispatcher = builder.taskDispatcher;
        this.taskExecutionService = builder.taskExecutionService;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Starts a job instance.
     *
     * @param jobParametersDTO The Key-Value map representing the workflow parameters
     */
    public void create(JobParametersDTO jobParametersDTO) {
        long jobId = jobFactory.create(jobParametersDTO);

        if (log.isDebugEnabled()) {
            Job job = jobService.getJob(jobId);

            log.debug("Job id={}, label='{}' created", job.getId(), job.getLabel());
        }
    }

    public void start(Long jobId) {
        Job job = jobService.start(jobId);

        jobExecutor.execute(job);

        eventPublisher.publishEvent(new JobStatusWorkflowEvent(job.getId(), job.getStatus()));

        if (log.isDebugEnabled()) {
            log.debug("Job id={}, label='{}' started", job.getId(), job.getLabel());
        }
    }

    /**
     * Stop a running job.
     *
     * @param jobId The id of the job to stop
     * @return The stopped {@link Job}
     */
    public Job stop(Long jobId) {
        Job job = jobService.stop(jobId);

        eventPublisher.publishEvent(new JobStatusWorkflowEvent(job.getId(), job.getStatus()));

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

        if (taskExecutions.size() > 0) {
            TaskExecution currentTaskExecution = taskExecutions.get(taskExecutions.size() - 1);

            currentTaskExecution.setEndDate(LocalDateTime.now());
            currentTaskExecution.setStatus(TaskStatus.CANCELLED);

            taskExecutionService.update(currentTaskExecution);

            taskDispatcher.dispatch(
                new CancelControlTask(currentTaskExecution.getJobId(), currentTaskExecution.getId()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Job id={}, label='{}' stoped", job.getId(), job.getLabel());
        }

        return job;
    }

    /**
     * Resume a stopped or failed job.
     *
     * @param jobId The id of the job to resume.
     * @return The resumed job
     */
    public Job resume(Long jobId) {
        Job job = jobService.resume(jobId);

        jobExecutor.execute(job);

        if (log.isDebugEnabled()) {
            log.debug("Job id={}, label='{}' resumed", job.getId(), job.getLabel());
        }

        return job;
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

            handleError(taskExecution);
        }
    }

    /**
     * Handle an application error.
     *
     * @param errorable The erring message.
     */
    public void handleError(Errorable errorable) {
        errorHandler.handle(errorable);
    }

    public static final class Builder {
        private ErrorHandler<? super Errorable> errorHandler;
        private EventPublisher eventPublisher;
        private JobExecutor jobExecutor;
        private JobFactory jobFactory;
        private JobService jobService;
        private TaskCompletionHandler taskCompletionHandler;
        private TaskDispatcher<? super Task> taskDispatcher;
        private TaskExecutionService taskExecutionService;

        private Builder() {
        }

        public Builder errorHandler(ErrorHandler<? super Errorable> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public Builder eventPublisher(EventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
            return this;
        }

        public Builder jobExecutor(JobExecutor jobExecutor) {
            this.jobExecutor = jobExecutor;
            return this;
        }

        public Builder jobFactory(JobFactory jobFactory) {
            this.jobFactory = jobFactory;
            return this;
        }

        public Builder jobService(JobService jobService) {
            this.jobService = jobService;
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

        public Coordinator build() {
            return new Coordinator(this);
        }
    }
}
