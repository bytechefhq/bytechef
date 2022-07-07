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

package com.bytechef.atlas.coordinator.task.completion;

import com.bytechef.atlas.Accessor;
import com.bytechef.atlas.Constants;
import com.bytechef.atlas.context.domain.Context;
import com.bytechef.atlas.context.domain.MapContext;
import com.bytechef.atlas.coordinator.job.executor.JobExecutor;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.event.Events;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.atlas.job.domain.SimpleJob;
import com.bytechef.atlas.service.context.ContextService;
import com.bytechef.atlas.service.job.JobService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.service.workflow.WorkflowService;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.atlas.workflow.domain.Workflow;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arik Cohen
 * @since Apr 24, 2017
 */
public class DefaultTaskCompletionHandler implements TaskCompletionHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultTaskCompletionHandler.class);

    private JobService jobService;
    private WorkflowService workflowService;
    private TaskExecutionService taskExecutionService;
    private ContextService contextService;
    private JobExecutor jobExecutor;
    private EventPublisher eventPublisher;
    private TaskEvaluator taskEvaluator;

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        return taskExecution.getParentId() == null;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        log.debug("Completing task {}", taskExecution.getId());

        Job job = jobService.getTaskExecutionJob(taskExecution.getId());

        if (job != null) {
            SimpleTaskExecution completedTaskExecution = SimpleTaskExecution.of(taskExecution);

            completedTaskExecution.setStatus(TaskStatus.COMPLETED);

            taskExecutionService.merge(completedTaskExecution);

            SimpleJob simpleJob = new SimpleJob(job);

            if (completedTaskExecution.getOutput() != null && completedTaskExecution.getName() != null) {
                Context context = contextService.peek(job.getId());

                MapContext newContext = new MapContext(context.asMap());

                newContext.put(completedTaskExecution.getName(), completedTaskExecution.getOutput());

                contextService.push(job.getId(), newContext);
            }

            if (hasMoreTasks(simpleJob)) {
                simpleJob.setCurrentTask(simpleJob.getCurrentTask() + 1);

                jobService.merge(simpleJob);
                jobExecutor.execute(simpleJob);
            } else {
                complete(simpleJob);
            }
        } else {
            log.error("Unknown job: {}", taskExecution.getJobId());
        }
    }

    private void complete(Job job) {
        SimpleTaskExecution jobTaskExecution = new SimpleTaskExecution();
        Context context = contextService.peek(job.getId());
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        List<Accessor> outputs = workflow.getOutputs();

        for (Accessor output : outputs) {
            jobTaskExecution.set(output.getRequiredString(Constants.NAME), output.getRequiredString(Constants.VALUE));
        }

        TaskExecution evaluatedJobTaskExecution = taskEvaluator.evaluate(jobTaskExecution, context);
        SimpleJob simpleJob = new SimpleJob(job);

        simpleJob.setStatus(JobStatus.COMPLETED);
        simpleJob.setEndTime(new Date());
        simpleJob.setCurrentTask(-1);
        simpleJob.setOutputs(evaluatedJobTaskExecution);

        jobService.merge(simpleJob);
        eventPublisher.publishEvent(
                WorkflowEvent.of(Events.JOB_STATUS, "jobId", job.getId(), "status", simpleJob.getStatus()));

        log.debug("Job {} completed successfully", job.getId());
    }

    private boolean hasMoreTasks(Job job) {
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        return job.getCurrentTask() + 1 < workflow.getTasks().size();
    }

    public void setContextService(ContextService contextService) {
        this.contextService = contextService;
    }

    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void setJobExecutor(JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    public void setJobService(JobService jobService) {
        this.jobService = jobService;
    }

    public void setTaskEvaluator(TaskEvaluator taskEvaluator) {
        this.taskEvaluator = taskEvaluator;
    }

    public void setTaskExecutionService(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }
}
