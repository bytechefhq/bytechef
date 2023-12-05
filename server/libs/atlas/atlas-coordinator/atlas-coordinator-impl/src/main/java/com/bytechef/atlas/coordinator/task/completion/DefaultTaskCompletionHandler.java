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

package com.bytechef.atlas.coordinator.task.completion;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.domain.TaskExecution.Status;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apr 24, 2017
 */
public class DefaultTaskCompletionHandler implements TaskCompletionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskCompletionHandler.class);

    private final ApplicationEventPublisher eventPublisher;
    private final ContextService contextService;
    private final JobExecutor jobExecutor;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public DefaultTaskCompletionHandler(
        ContextService contextService, ApplicationEventPublisher eventPublisher,
        JobExecutor jobExecutor, JobService jobService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        this.contextService = contextService;
        this.eventPublisher = eventPublisher;
        this.jobExecutor = jobExecutor;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        return taskExecution.getParentId() == null;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        Validate.notNull(taskExecution, "'taskExecution' must not be null");
        Validate.notNull(taskExecution.getId(), "'taskExecution.id' must not be null");

        if (logger.isDebugEnabled()) {
            logger.debug("handle: taskExecution={}", taskExecution);
        }

        Job job = jobService.getTaskExecutionJob(Validate.notNull(taskExecution.getId(), "id"));

        if (job == null) {
            logger.error("Unknown job id={}", taskExecution.getJobId());
        } else {
            taskExecution.setStatus(Status.COMPLETED);

            taskExecution = taskExecutionService.update(taskExecution);

            if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
                Map<String, Object> newContext = new HashMap<>(
                    taskFileStorage.readContextValue(
                        contextService.peek(Validate.notNull(job.getId(), "id"), Context.Classname.JOB)));

                newContext.put(
                    taskExecution.getName(), taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()));

                contextService.push(
                    Validate.notNull(job.getId(), "id"), Context.Classname.JOB,
                    taskFileStorage.storeContextValue(
                        Validate.notNull(job.getId(), "id"), Context.Classname.JOB, newContext));
            }

            logger.debug(
                "Task id={}, type='{}', name='{}' completed",
                taskExecution.getId(), taskExecution.getType(), taskExecution.getName());

            if (hasMoreTasks(job)) {
                job.setCurrentTask(job.getCurrentTask() + 1);

                job = jobService.update(job);

                jobExecutor.execute(job);
            } else {
                complete(job);
            }
        }
    }

    private void complete(Job job) {
        Validate.notNull(job, "'job' must not be null");

        if (logger.isDebugEnabled()) {
            logger.debug("complete: job={}", job);
        }

        Map<String, ?> context = taskFileStorage.readContextValue(
            contextService.peek(Validate.notNull(job.getId(), "id"), Context.Classname.JOB));
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        Map<String, Object> source = MapUtils.toMap(
            workflow.getOutputs(), Workflow.Output::name, Workflow.Output::value);

        job.setCurrentTask(-1);
        job.setEndDate(LocalDateTime.now());
        job.setStatus(Job.Status.COMPLETED);
        job.setOutputs(
            taskFileStorage.storeJobOutputs(
                Validate.notNull(job.getId(), "id"), Evaluator.evaluate(source, context)));

        job = jobService.update(job);

        eventPublisher
            .publishEvent(new JobStatusApplicationEvent(Validate.notNull(job.getId(), "id"), job.getStatus()));

        if (logger.isDebugEnabled()) {
            logger.debug("Job id={}, label='{}' completed", job.getId(), job.getLabel());
        }
    }

    private boolean hasMoreTasks(Job job) {
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        List<WorkflowTask> workflowTasks = workflow.getTasks();

        return job.getCurrentTask() + 1 < workflowTasks.size();
    }
}
