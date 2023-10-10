
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

package com.bytechef.atlas.coordinator;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.ErrorEvent;
import com.bytechef.atlas.coordinator.event.JobResumeEvent;
import com.bytechef.atlas.coordinator.event.JobStartEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStopEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.ErrorEventListener;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.configuration.task.CancelControlTask;
import com.bytechef.atlas.configuration.task.Task;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.bytechef.commons.util.ExceptionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * The central class responsible for coordinating and executing jobs.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 12, 2016
 */
public class TaskCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(TaskCoordinator.class);

    private final List<ApplicationEventListener> applicationEventListeners;
    private final List<ErrorEventListener> errorEventListeners;
    private final ApplicationEventPublisher eventPublisher;
    private final JobExecutor jobExecutor;
    private final JobService jobService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public TaskCoordinator(
        List<ApplicationEventListener> applicationEventListeners, List<ErrorEventListener> errorEventListeners,
        ApplicationEventPublisher eventPublisher,
        JobExecutor jobExecutor, JobService jobService, TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<? super Task> taskDispatcher, TaskExecutionService taskExecutionService) {

        this.applicationEventListeners = applicationEventListeners;
        this.errorEventListeners = errorEventListeners;
        this.eventPublisher = eventPublisher;
        this.jobExecutor = jobExecutor;
        this.jobService = jobService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
    }

    /**
     *
     * @param applicationEvent
     */
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        for (ApplicationEventListener applicationEventListener : applicationEventListeners) {
            applicationEventListener.onApplicationEvent(applicationEvent);
        }
    }

    public void onErrorEvent(ErrorEvent errorEvent) {
        for (ErrorEventListener errorEventListener : errorEventListeners) {
            errorEventListener.onErrorEvent(errorEvent);
        }
    }

    /**
     * Resume a stopped or failed job.
     *
     * @param jobResumeEvent The job resume vent.
     */
// TODO @Transactional
    public void onJobResumeEvent(JobResumeEvent jobResumeEvent) {
        if (logger.isDebugEnabled()) {
            logger.debug("Resuming job id={}", jobResumeEvent.getJobId());
        }

        Job job = jobService.resumeToStatusStarted(jobResumeEvent.getJobId());

        jobExecutor.execute(job);
    }

    /**
     * Start a running job.
     *
     * @param jobStartEvent The job start event
     */
// TODO @Transactional
    public void onJobStartEvent(JobStartEvent jobStartEvent) {
        if (logger.isDebugEnabled()) {
            logger.debug("Starting job id={}", jobStartEvent.getJobId());
        }

        Job job = jobService.setStatusToStarted(jobStartEvent.getJobId());

        jobExecutor.execute(job);

        eventPublisher.publishEvent(
            new JobStatusApplicationEvent(Validate.notNull(job.getId(), "id"), job.getStatus()));
    }

    /**
     * Stop a running job.
     *
     * @param jobStopEvent The job stop event
     */
// TODO @Transactional
    public void onJobStopEvent(JobStopEvent jobStopEvent) {
        if (logger.isDebugEnabled()) {
            logger.debug("Stopping job id={}", jobStopEvent.getJobId());
        }

        Job job = jobService.setStatusToStopped(jobStopEvent.getJobId());

        eventPublisher.publishEvent(
            new JobStatusApplicationEvent(Validate.notNull(job.getId(), "id"), job.getStatus()));

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobStopEvent.getJobId());

        if (!taskExecutions.isEmpty()) {
            TaskExecution currentTaskExecution = taskExecutions.get(taskExecutions.size() - 1);

            currentTaskExecution.setEndDate(LocalDateTime.now());
            currentTaskExecution.setStatus(TaskExecution.Status.CANCELLED);

            taskExecutionService.update(currentTaskExecution);

            taskDispatcher.dispatch(
                new CancelControlTask(
                    Validate.notNull(currentTaskExecution.getJobId(), "jobId"),
                    Validate.notNull(currentTaskExecution.getId(), "id")));
        }
    }

    /**
     * Complete a task of a given job.
     *
     * @param taskExecutionCompleteEvent The task to complete.
     */
// TODO @Transactional
    public void onTaskExecutionCompleteEvent(TaskExecutionCompleteEvent taskExecutionCompleteEvent) {
        TaskExecution taskExecution = taskExecutionCompleteEvent.getTaskExecution();

        try {
            taskCompletionHandler.handle(taskExecution);
        } catch (Exception e) {
            taskExecution.setError(new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            eventPublisher.publishEvent(new TaskExecutionErrorEvent(taskExecution));
        }
    }
}
