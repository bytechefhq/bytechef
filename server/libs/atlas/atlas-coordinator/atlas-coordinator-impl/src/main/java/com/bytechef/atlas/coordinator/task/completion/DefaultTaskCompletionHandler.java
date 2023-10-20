
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

import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.domain.TaskExecution.Status;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.execution.event.JobStatusEvent;
import com.bytechef.atlas.execution.service.RemoteJobService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.configuration.service.RemoteWorkflowService;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.evaluator.Evaluator;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskCompletionHandler.class);

    private final RemoteContextService contextService;
    private final EventPublisher eventPublisher;
    private final JobExecutor jobExecutor;
    private final RemoteJobService jobService;
    private final RemoteTaskExecutionService taskExecutionService;
    private final WorkflowFileStorageFacade workflowFileStorageFacade;
    private final RemoteWorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public DefaultTaskCompletionHandler(
        RemoteContextService contextService, EventPublisher eventPublisher, JobExecutor jobExecutor,
        RemoteJobService jobService,
        RemoteTaskExecutionService taskExecutionService, WorkflowFileStorageFacade workflowFileStorageFacade,
        RemoteWorkflowService workflowService) {

        this.contextService = contextService;
        this.eventPublisher = eventPublisher;
        this.jobExecutor = jobExecutor;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.workflowFileStorageFacade = workflowFileStorageFacade;
        this.workflowService = workflowService;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        return taskExecution.getParentId() == null;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void handle(TaskExecution taskExecution) {
        Assert.notNull(taskExecution, "'taskExecution' must not be null");
        Assert.notNull(taskExecution.getId(), "'taskExecution.id' must not be null");

        if (logger.isDebugEnabled()) {
            if (!StringUtils.hasText(taskExecution.getName())) {
                logger.debug("Task id={}, type='{}' completed", taskExecution.getId(), taskExecution.getType());
            } else {
                logger.debug(
                    "Task id={}, type='{}', name='{}' completed",
                    taskExecution.getId(), taskExecution.getType(), taskExecution.getName());
            }
        }

        Job job = jobService.getTaskExecutionJob(taskExecution.getId());

        if (job == null) {
            logger.error("Unknown job id={}", taskExecution.getJobId());
        } else {
            taskExecution.setStatus(Status.COMPLETED);

            taskExecution = taskExecutionService.update(taskExecution);

            if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
                Map<String, Object> newContext = new HashMap<>(
                    workflowFileStorageFacade.readContextValue(
                        contextService.peek(Objects.requireNonNull(job.getId()), Context.Classname.JOB)));

                newContext.put(
                    taskExecution.getName(),
                    workflowFileStorageFacade.readTaskExecutionOutput(taskExecution.getOutput()));

                contextService.push(
                    job.getId(), Context.Classname.JOB,
                    workflowFileStorageFacade.storeContextValue(job.getId(), Context.Classname.JOB, newContext));
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
        Assert.notNull(job.getId(), "'job.id' must not be null");

        Map<String, ?> context = workflowFileStorageFacade.readContextValue(
            contextService.peek(job.getId(), Context.Classname.JOB));
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        Map<String, Object> source = MapUtils.toMap(
            workflow.getOutputs(), Workflow.Output::name, Workflow.Output::value);

        job.setCurrentTask(-1);
        job.setEndDate(LocalDateTime.now());
        job.setStatus(Job.Status.COMPLETED);
        job.setOutputs(workflowFileStorageFacade.storeJobOutputs(job.getId(), Evaluator.evaluate(source, context)));

        job = jobService.update(job);

        eventPublisher.publishEvent(new JobStatusEvent(Objects.requireNonNull(job.getId()), job.getStatus()));

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
