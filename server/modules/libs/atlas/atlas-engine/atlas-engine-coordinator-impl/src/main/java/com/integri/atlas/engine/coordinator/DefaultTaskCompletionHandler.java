/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import com.integri.atlas.engine.coordinator.workflow.Workflow;
import com.integri.atlas.engine.core.Accessor;
import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.context.Context;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.event.EventPublisher;
import com.integri.atlas.engine.core.event.Events;
import com.integri.atlas.engine.core.event.WorkflowEvent;
import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.engine.coordinator.job.JobStatus;
import com.integri.atlas.engine.coordinator.job.SimpleJob;
import com.integri.atlas.engine.coordinator.workflow.WorkflowRepository;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskEvaluator;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.engine.core.task.TaskStatus;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Arik Cohen
 * @since Apr 24, 2017
 */
public class DefaultTaskCompletionHandler implements TaskCompletionHandler {

    private Logger log = LoggerFactory.getLogger(getClass());

    private JobRepository jobRepository;
    private WorkflowRepository workflowRepository;
    private TaskExecutionRepository jobTaskRepository;
    private ContextRepository contextRepository;
    private JobExecutor jobExecutor;
    private EventPublisher eventPublisher;
    private TaskEvaluator taskEvaluator;

    @Override
    public void handle(TaskExecution aTask) {
        log.debug("Completing task {}", aTask.getId());
        Job job = jobRepository.getByTaskId(aTask.getId());
        if (job != null) {
            SimpleTaskExecution task = SimpleTaskExecution.of(aTask);
            task.setStatus(TaskStatus.COMPLETED);
            jobTaskRepository.merge(task);
            SimpleJob mjob = new SimpleJob(job);
            if (task.getOutput() != null && task.getName() != null) {
                Context context = contextRepository.peek(job.getId());
                MapContext newContext = new MapContext(context.asMap());
                newContext.put(task.getName(), task.getOutput());
                contextRepository.push(job.getId(), newContext);
            }
            if (hasMoreTasks(mjob)) {
                mjob.setCurrentTask(mjob.getCurrentTask() + 1);
                jobRepository.merge(mjob);
                jobExecutor.execute(mjob);
            } else {
                complete(mjob);
            }
        } else {
            log.error("Unknown job: {}", aTask.getJobId());
        }
    }

    @Override
    public boolean canHandle(TaskExecution aJobTask) {
        return aJobTask.getParentId() == null;
    }

    private boolean hasMoreTasks(Job aJob) {
        Workflow workflow = workflowRepository.findOne(aJob.getWorkflowId());
        return aJob.getCurrentTask() + 1 < workflow.getTasks().size();
    }

    private void complete(SimpleJob aJob) {
        Workflow workflow = workflowRepository.findOne(aJob.getWorkflowId());
        List<Accessor> outputs = workflow.getOutputs();
        Context context = contextRepository.peek(aJob.getId());
        SimpleTaskExecution jobOutput = new SimpleTaskExecution();
        for (Accessor output : outputs) {
            jobOutput.set(output.getRequiredString(DSL.NAME), output.getRequiredString(DSL.VALUE));
        }
        TaskExecution evaledjobOutput = taskEvaluator.evaluate(jobOutput, context);
        SimpleJob job = new SimpleJob((Job) aJob);
        job.setStatus(JobStatus.COMPLETED);
        job.setEndTime(new Date());
        job.setCurrentTask(-1);
        job.setOutputs(evaledjobOutput);
        jobRepository.merge(job);
        eventPublisher.publishEvent(WorkflowEvent.of(Events.JOB_STATUS, "jobId", aJob.getId(), "status", job.getStatus()));
        log.debug("Job {} completed successfully", aJob.getId());
    }

    public void setJobRepository(JobRepository aJobRepository) {
        jobRepository = aJobRepository;
    }

    public void setWorkflowRepository(WorkflowRepository aWorkflowRepository) {
        workflowRepository = aWorkflowRepository;
    }

    public void setJobTaskRepository(TaskExecutionRepository aJobTaskRepository) {
        jobTaskRepository = aJobTaskRepository;
    }

    public void setContextRepository(ContextRepository aContextRepository) {
        contextRepository = aContextRepository;
    }

    public void setJobExecutor(JobExecutor aJobExecutor) {
        jobExecutor = aJobExecutor;
    }

    public void setEventPublisher(EventPublisher aEventPublisher) {
        eventPublisher = aEventPublisher;
    }

    public void setTaskEvaluator(TaskEvaluator aTaskEvaluator) {
        taskEvaluator = aTaskEvaluator;
    }
}
