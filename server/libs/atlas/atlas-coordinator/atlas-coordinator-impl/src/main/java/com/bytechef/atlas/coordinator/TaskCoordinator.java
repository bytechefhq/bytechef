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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.coordinator;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.ErrorEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.ErrorEventListener;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
        ApplicationEventPublisher eventPublisher, JobExecutor jobExecutor, JobService jobService,
        TaskCompletionHandler taskCompletionHandler, TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService) {

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
        if (logger.isTraceEnabled()) {
            logger.trace("onApplicationEvent: applicationEvent={}", applicationEvent);
        }

        for (ApplicationEventListener applicationEventListener : applicationEventListeners) {
            applicationEventListener.onApplicationEvent(applicationEvent);
        }
    }

    public void onErrorEvent(ErrorEvent errorEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onErrorEvent: errorEvent={}", errorEvent);
        }

        for (ErrorEventListener errorEventListener : errorEventListeners) {
            errorEventListener.onErrorEvent(errorEvent);
        }
    }

    /**
     * Resume a stopped or failed job.
     *
     * @param resumeJobEvent The job resume vent.
     */
// TODO @Transactional
    public void onResumeJobEvent(ResumeJobEvent resumeJobEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onResumeJobEvent: resumeJobEvent={}", resumeJobEvent);
        }

        Job job = jobService.resumeToStatusStarted(resumeJobEvent.getJobId());

        if (logger.isDebugEnabled()) {
            logger.debug("Job id={} resumed", resumeJobEvent.getJobId());
        }

        try {
            jobExecutor.execute(job);
        } catch (Exception exception) {
            handleJobExecutionException(job, exception);
        }
    }

    /**
     * Start a running job.
     *
     * @param startJobEvent The job start event
     */
// TODO @Transactional
    public void onStartJobEvent(StartJobEvent startJobEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onStartJobEvent: startJobEvent={}", startJobEvent);
        }

        Job job = jobService.setStatusToStarted(startJobEvent.getJobId());

        if (logger.isDebugEnabled()) {
            logger.debug("Job id={}, label='{}' started", job.getId(), job.getLabel());
        }

        try {
            jobExecutor.execute(job);
        } catch (Exception exception) {
            handleJobExecutionException(job, exception);

            return;
        }

        eventPublisher.publishEvent(
            new JobStatusApplicationEvent(Validate.notNull(job.getId(), "id"), job.getStatus()));
    }

    /**
     * Stop a running job.
     *
     * @param stopJobEvent The job stop event
     */
// TODO @Transactional
    public void onStopJobEvent(StopJobEvent stopJobEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onStopJobEvent: stopJobEvent={}", stopJobEvent);
        }

        Job job = jobService.setStatusToStopped(stopJobEvent.getJobId());

        eventPublisher.publishEvent(
            new JobStatusApplicationEvent(Validate.notNull(job.getId(), "id"), job.getStatus()));

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(stopJobEvent.getJobId());

        if (!taskExecutions.isEmpty()) {
            TaskExecution currentTaskExecution = taskExecutions.get(taskExecutions.size() - 1);

            currentTaskExecution.setEndDate(Instant.now());
            currentTaskExecution.setStatus(TaskExecution.Status.CANCELLED);

            taskExecutionService.update(currentTaskExecution);

            taskDispatcher.dispatch(
                new CancelControlTask(
                    Validate.notNull(currentTaskExecution.getJobId(), "jobId"),
                    Validate.notNull(currentTaskExecution.getId(), "id")));
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Job id={} stopped", stopJobEvent.getJobId());
        }
    }

    /**
     * Complete a task of a given job.
     *
     * @param taskExecutionCompleteEvent The task to complete.
     */
// TODO @Transactional
    public void onTaskExecutionCompleteEvent(TaskExecutionCompleteEvent taskExecutionCompleteEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onTaskExecutionCompleteEvent: taskExecutionCompleteEvent={}", taskExecutionCompleteEvent);
        }

        TaskExecution taskExecution = taskExecutionCompleteEvent.getTaskExecution();

        try {
            taskCompletionHandler.handle(taskExecution);
        } catch (Exception e) {
            taskExecution.setError(new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            eventPublisher.publishEvent(new TaskExecutionErrorEvent(taskExecution));
        }
    }

    private void handleJobExecutionException(Job job, Exception exception) {
        long jobId = Validate.notNull(job.getId(), "id");

        Optional<TaskExecution> taskExecutionOptional = taskExecutionService.fetchLastJobTaskExecution(jobId);

        if (taskExecutionOptional.isPresent()) {
            TaskExecution taskExecution = taskExecutionOptional.get();

            taskExecution.setError(
                new ExecutionError(
                    exception.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(exception))));

            eventPublisher.publishEvent(new TaskExecutionErrorEvent(taskExecution));
        } else {
            job.setStatus(Job.Status.FAILED);
            job.setEndDate(Instant.now());

            jobService.update(job);

            eventPublisher.publishEvent(new JobStatusApplicationEvent(jobId, Job.Status.FAILED));
        }
    }
}
