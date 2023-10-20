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

import com.bytechef.atlas.constants.WorkflowConstants;
import com.bytechef.atlas.coordinator.job.executor.JobExecutor;
import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.event.JobStatusWorkflowEvent;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.collection.MapUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apr 24, 2017
 */
public class DefaultTaskCompletionHandler implements TaskCompletionHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultTaskCompletionHandler.class);

    private final ContextService contextService;
    private final EventPublisher eventPublisher;
    private final JobExecutor jobExecutor;
    private final JobService jobService;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;
    private final WorkflowService workflowService;

    public DefaultTaskCompletionHandler(
            ContextService contextService,
            EventPublisher eventPublisher,
            JobExecutor jobExecutor,
            JobService jobService,
            TaskEvaluator taskEvaluator,
            TaskExecutionService taskExecutionService,
            WorkflowService workflowService) {
        this.contextService = contextService;
        this.eventPublisher = eventPublisher;
        this.jobExecutor = jobExecutor;
        this.jobService = jobService;
        this.taskEvaluator = taskEvaluator;
        this.taskExecutionService = taskExecutionService;
        this.workflowService = workflowService;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        return taskExecution.getParentId() == null;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        log.debug("Completing task {}", taskExecution.getId());

        Job job = jobService.getTaskExecutionJob(taskExecution.getId());

        if (job != null) {
            TaskExecution completedTaskExecution = new TaskExecution(taskExecution);

            completedTaskExecution.setStatus(TaskStatus.COMPLETED);

            taskExecutionService.update(completedTaskExecution);

            Job updateJob = new Job(job);

            if (completedTaskExecution.getOutput() != null && completedTaskExecution.getName() != null) {
                Context context = contextService.peek(job.getId());

                Context newContext = new Context(context);

                newContext.put(completedTaskExecution.getName(), completedTaskExecution.getOutput());

                contextService.push(job.getId(), newContext);
            }

            if (hasMoreTasks(updateJob)) {
                updateJob.setCurrentTask(updateJob.getCurrentTask() + 1);

                jobService.update(updateJob);
                jobExecutor.execute(updateJob);
            } else {
                complete(updateJob);
            }
        } else {
            log.error("Unknown job: {}", taskExecution.getJobId());
        }
    }

    private void complete(Job job) {
        Context context = contextService.peek(job.getId());
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        Map<String, Object> source = new HashMap<>();

        for (Map<String, Object> output : workflow.getOutputs()) {
            source.put(
                    MapUtils.getRequiredString(output, WorkflowConstants.NAME),
                    MapUtils.getRequiredString(output, WorkflowConstants.VALUE));
        }

        TaskExecution evaluatedJobTaskExecution =
                taskEvaluator.evaluate(new TaskExecution(new WorkflowTask(source)), context);
        Job updateJob = new Job(job);

        updateJob.setStatus(JobStatus.COMPLETED);
        updateJob.setEndTime(new Date());
        updateJob.setCurrentTask(-1);
        updateJob.setOutputs(evaluatedJobTaskExecution.getWorkflowTaskParameters());

        jobService.update(updateJob);
        eventPublisher.publishEvent(new JobStatusWorkflowEvent(job.getId(), updateJob.getStatus()));

        log.debug("Job {} completed successfully", job.getId());
    }

    private boolean hasMoreTasks(Job job) {
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        return job.getCurrentTask() + 1 < workflow.getTasks().size();
    }
}
