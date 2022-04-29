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

package com.integri.atlas.engine.coordinator.error;

import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.JobStatus;
import com.integri.atlas.engine.coordinator.job.SimpleJob;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.engine.core.error.Error;
import com.integri.atlas.engine.core.error.ErrorHandler;
import com.integri.atlas.engine.core.event.EventPublisher;
import com.integri.atlas.engine.core.event.Events;
import com.integri.atlas.engine.core.event.WorkflowEvent;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.engine.core.uuid.UUIDGenerator;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 *
 * @author Arik Cohen
 * @since Apr 10, 2017
 */
public class TaskExecutionErrorHandler implements ErrorHandler<TaskExecution> {

    private JobRepository jobRepository;
    private TaskExecutionRepository jobTaskRepository;
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
        jobTaskRepository.merge(mtask);

        // if the task is retryable, then retry it
        if (aTask.getRetryAttempts() < aTask.getRetry()) {
            SimpleTaskExecution retryTask = new SimpleTaskExecution(aTask.asMap());
            retryTask.setId(UUIDGenerator.generate());
            retryTask.setCreateTime(new Date());
            retryTask.setStatus(TaskStatus.CREATED);
            retryTask.setError(null);
            retryTask.setRetryAttempts(aTask.getRetryAttempts() + 1);
            jobTaskRepository.create(retryTask);
            taskDispatcher.dispatch(retryTask);
        }
        // if it's not retryable then we're gonna fail the job
        else {
            while (mtask.getParentId() != null) { // mark parent tasks as FAILED as well
                mtask = SimpleTaskExecution.of(jobTaskRepository.findOne(mtask.getParentId()));
                mtask.setStatus(TaskStatus.FAILED);
                mtask.setEndTime(new Date());
                jobTaskRepository.merge(mtask);
            }
            Job job = jobRepository.getByTaskId(mtask.getId());
            Assert.notNull(job, "job not found for task: " + mtask.getId());
            SimpleJob mjob = new SimpleJob(job);
            Assert.notNull(mjob, String.format("No job found for task %s ", mtask.getId()));
            mjob.setStatus(JobStatus.FAILED);
            mjob.setEndTime(new Date());
            jobRepository.merge(mjob);
            eventPublisher.publishEvent(
                WorkflowEvent.of(Events.JOB_STATUS, "jobId", mjob.getId(), "status", mjob.getStatus())
            );
        }
    }

    public void setJobRepository(JobRepository aJobRepository) {
        jobRepository = aJobRepository;
    }

    public void setJobTaskRepository(TaskExecutionRepository aJobTaskRepository) {
        jobTaskRepository = aJobTaskRepository;
    }

    public void setTaskDispatcher(TaskDispatcher aTaskDispatcher) {
        taskDispatcher = aTaskDispatcher;
    }

    public void setEventPublisher(EventPublisher aEventPublisher) {
        eventPublisher = aEventPublisher;
    }
}
