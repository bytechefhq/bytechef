
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

import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.RemoteJobFacade;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.error.ExecutionError;
import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.execution.event.JobStatusEvent;
import com.bytechef.atlas.execution.service.RemoteJobService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.configuration.task.CancelControlTask;
import com.bytechef.atlas.configuration.task.Task;
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

    private static final Logger logger = LoggerFactory.getLogger(TaskCoordinator.class);

    private final EventPublisher eventPublisher;
    private final JobExecutor jobExecutor;
    private final RemoteJobFacade jobFacade;
    private final RemoteJobService jobService;
    private final MessageBroker messageBroker;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final RemoteTaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public TaskCoordinator(
        EventPublisher eventPublisher, JobExecutor jobExecutor, RemoteJobFacade jobFacade,
        RemoteJobService jobService, MessageBroker messageBroker, TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<? super Task> taskDispatcher, RemoteTaskExecutionService taskExecutionService) {

        this.eventPublisher = eventPublisher;
        this.jobExecutor = jobExecutor;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.messageBroker = messageBroker;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
    }

    /**
     * Creates a job instance.
     *
     * @param jobParameters The job parameters
     */
    public void handleJobsCreate(JobParameters jobParameters) {
        this.jobFacade.createJob(jobParameters);
    }

    /**
     * Complete a task of a given job.
     *
     * @param taskExecution The task to complete.
     */
    public void handleTasksComplete(TaskExecution taskExecution) {
        try {
            taskCompletionHandler.handle(taskExecution);
        } catch (Exception e) {
            taskExecution.setError(new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            messageBroker.send(SystemMessageRoute.ERRORS, taskExecution);
        }
    }

    /**
     * Start a running job.
     *
     * @param jobId The id of the job to start
     */
    @SuppressFBWarnings("NP")
    public void handleJobsStart(Long jobId) {
        Job job = jobService.setStatusToStarted(jobId);

        if (logger.isDebugEnabled()) {
            logger.debug("Starting job id={}", jobId);
        }

        jobExecutor.execute(job);

        eventPublisher.publishEvent(new JobStatusEvent(Objects.requireNonNull(job.getId()), job.getStatus()));
    }

    /**
     * Resume a stopped or failed job.
     *
     * @param jobId The id of the job to resume.
     */
    public void handleJobsResume(Long jobId) {
        Job job = jobService.resumeToStatusStarted(jobId);

        if (logger.isDebugEnabled()) {
            logger.debug("Resuming job id={}", jobId);
        }

        jobExecutor.execute(job);
    }

    /**
     * Stop a running job.
     *
     * @param jobId The id of the job to stop
     */
    @SuppressFBWarnings("NP")
    public void handleJobsStop(Long jobId) {
        if (logger.isDebugEnabled()) {
            logger.debug("Stopping job id={}", jobId);
        }

        Job job = jobService.setStatusToStopped(jobId);

        eventPublisher.publishEvent(new JobStatusEvent(Objects.requireNonNull(job.getId()), job.getStatus()));

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

        if (!taskExecutions.isEmpty()) {
            TaskExecution currentTaskExecution = taskExecutions.get(taskExecutions.size() - 1);

            currentTaskExecution.setEndDate(LocalDateTime.now());
            currentTaskExecution.setStatus(TaskExecution.Status.CANCELLED);

            taskExecutionService.update(currentTaskExecution);

            taskDispatcher.dispatch(
                new CancelControlTask(
                    Objects.requireNonNull(currentTaskExecution.getJobId()),
                    Objects.requireNonNull(currentTaskExecution.getId())));
        }
    }
}
