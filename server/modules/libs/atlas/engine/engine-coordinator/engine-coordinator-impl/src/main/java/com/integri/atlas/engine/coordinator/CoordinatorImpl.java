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

package com.integri.atlas.engine.coordinator;

import com.integri.atlas.context.service.ContextService;
import com.integri.atlas.engine.MapObject;
import com.integri.atlas.engine.context.MapContext;
import com.integri.atlas.engine.coordinator.job.executor.JobExecutor;
import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.error.ErrorHandler;
import com.integri.atlas.engine.error.ErrorObject;
import com.integri.atlas.engine.error.Errorable;
import com.integri.atlas.engine.event.EventPublisher;
import com.integri.atlas.engine.event.Events;
import com.integri.atlas.engine.event.WorkflowEvent;
import com.integri.atlas.engine.job.Job;
import com.integri.atlas.engine.job.service.JobService;
import com.integri.atlas.engine.message.broker.MessageBroker;
import com.integri.atlas.engine.message.broker.Queues;
import com.integri.atlas.engine.task.CancelTask;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.task.execution.TaskStatus;
import com.integri.atlas.engine.task.execution.servic.TaskExecutionService;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.Assert;

/**
 * The central class responsible for coordinating and executing jobs.
 *
 * @author Arik Cohen
 * @since Jun 12, 2016
 */
public class CoordinatorImpl implements Coordinator {

    private ContextService contextService;
    private ErrorHandler errorHandler;
    private EventPublisher eventPublisher;
    private MessageBroker messageBroker;
    private JobExecutor jobExecutor;
    private JobService jobService;
    private TaskExecutionService taskExecutionService;
    private TaskDispatcher taskDispatcher;
    private TaskCompletionHandler taskCompletionHandler;

    private static final String INPUTS = "inputs";

    /**
     * Starts a job instance.
     *
     * @param jobParamsMap The Key-Value map representing the job parameters
     * @return The instance of the Job
     */
    @Override
    public Job create(Map<String, Object> jobParamsMap) {
        Assert.notNull(jobParamsMap, "request can't be null");

        MapObject jobParams = MapObject.of(jobParamsMap);

        Job job = jobService.create(jobParams);

        MapContext context = new MapContext(jobParams.getMap(INPUTS, Collections.EMPTY_MAP));

        contextService.push(job.getId(), context);

        eventPublisher.publishEvent(
            WorkflowEvent.of(Events.JOB_STATUS, "jobId", job.getId(), "status", job.getStatus())
        );

        messageBroker.send(Queues.JOBS, job);

        return job;
    }

    @Override
    public void start(Job job) {
        job = jobService.start(job);

        jobExecutor.execute(job);

        eventPublisher.publishEvent(
            WorkflowEvent.of(Events.JOB_STATUS, "jobId", job.getId(), "status", job.getStatus())
        );
    }

    /**
     * Stop a running job.
     *
     * @param jobId The id of the job to stop
     *
     * @return The stopped {@link Job}
     */
    public Job stop(String jobId) {
        Job job = jobService.stop(jobId);

        eventPublisher.publishEvent(
            WorkflowEvent.of(Events.JOB_STATUS, "jobId", job.getId(), "status", job.getStatus())
        );

        if (job.getExecutions().size() > 0) {
            SimpleTaskExecution currentTask = SimpleTaskExecution.of(
                job.getExecutions().get(job.getExecutions().size() - 1)
            );

            currentTask.setStatus(TaskStatus.CANCELLED);
            currentTask.setEndTime(new Date());

            taskExecutionService.merge(currentTask);

            taskDispatcher.dispatch(new CancelTask(currentTask.getJobId(), currentTask.getId()));
        }

        return job;
    }

    /**
     * Resume a stopped or failed job.
     *
     * @param jobId  The id of the job to resume.
     * @return The resumed job
     */
    @Override
    public Job resume(String jobId) {
        Job job = jobService.resume(jobId);

        jobExecutor.execute(job);

        return job;
    }

    /**
     * Complete a task of a given job.
     *
     * @param aTask The task to complete.
     */
    public void complete(TaskExecution aTask) {
        try {
            taskCompletionHandler.handle(aTask);
        } catch (Exception e) {
            SimpleTaskExecution taskExecution = SimpleTaskExecution.of(aTask);

            taskExecution.setError(new ErrorObject(e.getMessage(), ExceptionUtils.getStackFrames(e)));

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

    public void setContextService(ContextService contextService) {
        this.contextService = contextService;
    }

    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void setJobService(JobService jobService) {
        this.jobService = jobService;
    }

    public void setTaskDispatcher(TaskDispatcher taskDispatcher) {
        this.taskDispatcher = taskDispatcher;
    }

    public void setTaskExecutionService(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        this.taskCompletionHandler = taskCompletionHandler;
    }

    public void setJobExecutor(JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    public void setMessageBroker(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }
}
