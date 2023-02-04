
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
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.utils.MapUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
        ContextService contextService, EventPublisher eventPublisher, JobExecutor jobExecutor, JobService jobService,
        TaskEvaluator taskEvaluator, TaskExecutionService taskExecutionService, WorkflowService workflowService) {

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
    @SuppressFBWarnings("NP")
    public void handle(TaskExecution taskExecution) {
        Assert.notNull(taskExecution.getId(), "'taskExecution.id' must not be null.");

        if (!StringUtils.hasText(taskExecution.getName())) {
            log.debug("Task {}: '{}' completed.", taskExecution.getId(), taskExecution.getType());
        } else {
            log.debug(
                "Task {}: '{}/{}' completed.", taskExecution.getId(), taskExecution.getType(), taskExecution.getName());
        }

        Job job = jobService.getTaskExecutionJob(taskExecution.getId());

        if (job == null) {
            log.error("Unknown job: {}.", taskExecution.getJobId());
        } else {
            taskExecution.setStatus(TaskStatus.COMPLETED);

            taskExecution = taskExecutionService.update(taskExecution);

            if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
                Assert.notNull(job.getId(), "'job.id' must not be null.");

                Map<String, Object> newContext = new HashMap<>(contextService.peek(job.getId(), Context.Classname.JOB));

                newContext.put(taskExecution.getName(), taskExecution.getOutput());

                contextService.push(job.getId(), Context.Classname.JOB, newContext);
            }

            if (hasMoreTasks(job)) {
                job.setCurrentTask(job.getCurrentTask() + 1);

                job = jobService.update(job);

                jobExecutor.execute(job);
            } else {
                complete(job);
            }
        }
    }

    @SuppressFBWarnings("NP")
    private void complete(Job job) {
        Assert.notNull(job.getId(), "'job.id' must not be null.");

        Map<String, Object> context = contextService.peek(job.getId(), Context.Classname.JOB);
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        Map<String, Object> source = new HashMap<>();

        for (Map<String, Object> output : workflow.getOutputs()) {
            source.put(
                MapUtils.getRequiredString(output, WorkflowConstants.NAME),
                MapUtils.getRequiredString(output, WorkflowConstants.VALUE));
        }

        job.setStatus(Job.Status.COMPLETED);
        job.setEndTime(new Date());
        job.setCurrentTask(-1);

        TaskExecution evaluatedTaskExecution = taskEvaluator.evaluate(
            new TaskExecution(new WorkflowTask(source)), context);

        job.setOutputs(evaluatedTaskExecution.getParameters());

        job = jobService.update(job);

        eventPublisher.publishEvent(new JobStatusWorkflowEvent(job.getId(), job.getStatus()));

        log.debug("Job '{}: {}' completed.", job.getId(), job.getLabel());
    }

    private boolean hasMoreTasks(Job job) {
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        List<WorkflowTask> workflowTasks = workflow.getTasks();

        return job.getCurrentTask() + 1 < workflowTasks.size();
    }
}
