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

import com.bytechef.atlas.error.Error;
import com.bytechef.atlas.error.ErrorHandler;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.event.Events;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.atlas.job.domain.SimpleJob;
import com.bytechef.atlas.service.job.JobService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.uuid.UUIDGenerator;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @since Apr 10, 2017
 */
public class TaskExecutionErrorHandler implements ErrorHandler<TaskExecution> {

    private JobService jobService;
    private TaskExecutionService taskExecutionService;
    private TaskDispatcher taskDispatcher;
    private EventPublisher eventPublisher;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void handle(TaskExecution aTask) {
        Error error = aTask.getError();
        Assert.notNull(error, "error must not be null");
        logger.debug("Erring task {}: {}\n{}", aTask.getId(), error.getMessage());

        // set task status to failed and persist
        SimpleTaskExecution mtask = SimpleTaskExecution.of(aTask);
        mtask.setStatus(TaskStatus.FAILED);
        mtask.setEndTime(new Date());
        taskExecutionService.merge(mtask);

        // if the task is retryable, then retry it
        if (aTask.getRetryAttempts() < aTask.getRetry()) {
            SimpleTaskExecution retryTask = new SimpleTaskExecution(aTask.asMap());
            retryTask.setId(UUIDGenerator.generate());
            retryTask.setCreateTime(new Date());
            retryTask.setStatus(TaskStatus.CREATED);
            retryTask.setError(null);
            retryTask.setRetryAttempts(aTask.getRetryAttempts() + 1);
            taskExecutionService.create(retryTask);
            taskDispatcher.dispatch(retryTask);
        }
        // if it's not retryable then we're gonna fail the job
        else {
            while (mtask.getParentId() != null) { // mark parent tasks as FAILED as well
                mtask = SimpleTaskExecution.of(taskExecutionService.getTaskExecution(mtask.getParentId()));
                mtask.setStatus(TaskStatus.FAILED);
                mtask.setEndTime(new Date());
                taskExecutionService.merge(mtask);
            }
            Job job = jobService.getTaskExecutionJob(mtask.getId());
            Assert.notNull(job, "job not found for task: " + mtask.getId());
            SimpleJob mjob = new SimpleJob(job);
            Assert.notNull(mjob, String.format("No job found for task %s ", mtask.getId()));
            mjob.setStatus(JobStatus.FAILED);
            mjob.setEndTime(new Date());
            jobService.merge(mjob);
            eventPublisher.publishEvent(
                    WorkflowEvent.of(Events.JOB_STATUS, "jobId", mjob.getId(), "status", mjob.getStatus()));
        }
    }

    public void setJobService(JobService aJobRepository) {
        jobService = aJobRepository;
    }

    public void setTaskExecutionService(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    public void setTaskDispatcher(TaskDispatcher aTaskDispatcher) {
        taskDispatcher = aTaskDispatcher;
    }

    public void setEventPublisher(EventPublisher aEventPublisher) {
        eventPublisher = aEventPublisher;
    }
}
