
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
import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.dto.JobParametersDTO;
import com.bytechef.atlas.error.ErrorHandler;
import com.bytechef.atlas.error.Errorable;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.event.JobStatusWorkflowEvent;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.CancelControlTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.execution.TaskStatus;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.Assert;

/**
 * The central class responsible for coordinating and executing jobs.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 12, 2016
 */
public class Coordinator {

    private final ContextService contextService;
    private final ErrorHandler errorHandler;
    private final EventPublisher eventPublisher;
    private final JobExecutor jobExecutor;
    private final JobService jobService;
    private final MessageBroker messageBroker;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher taskDispatcher;
    private final TaskExecutionService taskExecutionService;

    public Coordinator(
        ContextService contextService,
        ErrorHandler errorHandler,
        EventPublisher eventPublisher,
        JobExecutor jobExecutor,
        JobService jobService,
        MessageBroker messageBroker,
        TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher taskDispatcher,
        TaskExecutionService taskExecutionService) {
        this.contextService = contextService;
        this.errorHandler = errorHandler;
        this.eventPublisher = eventPublisher;
        this.jobExecutor = jobExecutor;
        this.jobService = jobService;
        this.messageBroker = messageBroker;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
    }

    /**
     * Starts a job instance.
     *
     * @param jobParametersDTO The Key-Value map representing the workflow parameters
     * @return The instance of the Job
     */
    public void create(JobParametersDTO jobParametersDTO) {
        Assert.notNull(jobParametersDTO, "request can't be null");

        Job job = jobService.add(jobParametersDTO);

        Context context = new Context(job.getInputs());

        contextService.push(job.getId(), context);

        eventPublisher.publishEvent(new JobStatusWorkflowEvent(job.getId(), job.getStatus()));

        messageBroker.send(Queues.JOBS, job.getId());
    }

    public void start(String jobId) {
        Job job = jobService.start(jobId);

        jobExecutor.execute(job);

        eventPublisher.publishEvent(new JobStatusWorkflowEvent(job.getId(), job.getStatus()));
    }

    /**
     * Stop a running job.
     *
     * @param jobId The id of the job to stop
     * @return The stopped {@link Job}
     */
    public Job stop(String jobId) {
        Job job = jobService.stop(jobId);

        eventPublisher.publishEvent(new JobStatusWorkflowEvent(job.getId(), job.getStatus()));

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

        if (taskExecutions.size() > 0) {
            TaskExecution currentTaskExecution = new TaskExecution(taskExecutions.get(taskExecutions.size() - 1));

            currentTaskExecution.setStatus(TaskStatus.CANCELLED);
            currentTaskExecution.setEndTime(LocalDateTime.now());

            taskExecutionService.update(currentTaskExecution);

            taskDispatcher.dispatch(
                new CancelControlTask(currentTaskExecution.getJobId(), currentTaskExecution.getId()));
        }

        return job;
    }

    /**
     * Resume a stopped or failed job.
     *
     * @param jobId The id of the job to resume.
     * @return The resumed job
     */
    public Job resume(String jobId) {
        Job job = jobService.resume(jobId);

        jobExecutor.execute(job);

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
            TaskExecution erroredTaskExecution = new TaskExecution(taskExecution);

            erroredTaskExecution.setError(
                new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            handleError(erroredTaskExecution);
        }
    }

    /**
     * Handle an application error.
     *
     * @param errorable The erring message.
     */
    @SuppressWarnings("unchecked")
    public void handleError(Errorable errorable) {
        errorHandler.handle(errorable);
    }
}
