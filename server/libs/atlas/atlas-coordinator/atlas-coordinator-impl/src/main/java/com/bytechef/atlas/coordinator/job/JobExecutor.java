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

package com.bytechef.atlas.coordinator.job;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.util.WorkflowTaskUtils;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apr 24, 2017
 */
public class JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);

    private final ContextService contextService;
    private final Evaluator evaluator;
    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskDispatcher<? super TaskExecution> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public JobExecutor(
        ContextService contextService, Evaluator evaluator, ApplicationEventPublisher eventPublisher,
        JobService jobService, TaskDispatcher<? super TaskExecution> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        this.contextService = contextService;
        this.evaluator = evaluator;
        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    public void execute(Job job) {
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        List<WorkflowTask> workflowTasks = WorkflowTaskUtils.removeDisabledTasks(workflow.getTasks());

        if (job.getStatus() != Job.Status.STARTED) {
            throw new IllegalStateException("Should not be here");
        } else if (job.getCurrentTask() < workflowTasks.size()) {
            executeNextTask(job, workflow, workflowTasks);
        } else {
            completeJob(job);
        }
    }

    /**
     * Evaluates the workflow outputs against the current job context, marks the job completed and announces the status
     * change. The single definition of what completing a job means: {@code DefaultTaskCompletionHandler} calls it once
     * the last task execution has finished, and {@link #execute(Job)} calls it directly when there is nothing to
     * dispatch in the first place — a workflow with no tasks, or one whose every task is disabled, leaves the effective
     * (disabled-stripped) task list exhausted before the first dispatch.
     *
     * <p>
     * Public only because {@code DefaultTaskCompletionHandler} sits in a sibling package; this is engine-internal and
     * not meant to be called from outside the coordinator.
     */
    public void completeJob(Job job) {
        if (log.isTraceEnabled()) {
            log.trace("completeJob: job={}", job);
        }

        Map<String, ?> context = taskFileStorage.readContextValue(
            contextService.peek(Validate.notNull(job.getId(), "id"), Context.Classname.JOB));
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        Map<String, Object> source = MapUtils.toMap(
            workflow.getOutputs(), Workflow.Output::name, Workflow.Output::value);

        job.setCurrentTask(-1);
        job.setEndDate(Instant.now());
        job.setStatus(Job.Status.COMPLETED);
        job.setOutputs(
            taskFileStorage.storeJobOutputs(Validate.notNull(job.getId(), "id"), evaluator.evaluate(source, context)));

        job = jobService.update(job);

        eventPublisher.publishEvent(
            new JobStatusApplicationEvent(Validate.notNull(job.getId(), "id"), job.getStatus()));

        if (log.isDebugEnabled()) {
            log.debug("Job id={}, label='{}' completed", job.getId(), job.getLabel());
        }
    }

    private void executeNextTask(Job job, Workflow workflow, List<WorkflowTask> workflowTasks) {
        Assert.notNull(job.getId(), "'job.id' must not be null");

        if (log.isTraceEnabled()) {
            log.trace("executeNextTask: job={}, workflow={}", job, workflow);
        }

        Map<String, ?> context = taskFileStorage.readContextValue(
            contextService.peek(Validate.notNull(job.getId(), "id"), Context.Classname.JOB));
        TaskExecution nextTaskExecution = nextTaskExecution(job, workflow, workflowTasks);

        nextTaskExecution = taskExecutionService.create(nextTaskExecution.evaluate(context, evaluator));

        contextService.push(
            Validate.notNull(nextTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION,
            taskFileStorage.storeContextValue(
                Validate.notNull(nextTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION, context));

        taskDispatcher.dispatch(nextTaskExecution);

        if (log.isDebugEnabled()) {
            log.debug(
                "Task id={}, type='{}', name='{}' executed",
                nextTaskExecution.getId(), nextTaskExecution.getType(), nextTaskExecution.getName());
        }
    }

    private TaskExecution nextTaskExecution(Job job, Workflow workflow, List<WorkflowTask> workflowTasks) {
        WorkflowTask workflowTask = workflowTasks.get(job.getCurrentTask());

        Assert.notNull(job.getId(), "'job.id' must not be null");

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(job.getId())
            .priority(job.getPriority())
            .workflowTask(workflowTask)
            .build();

        if ((workflow.getMaxRetries() > 0 || workflowTask.getMaxRetries() > 0) && taskExecution.getMaxRetries() < 1) {
            if (workflowTask.getMaxRetries() > 0) {
                taskExecution.setMaxRetries(workflowTask.getMaxRetries());
            } else {
                taskExecution.setMaxRetries(workflow.getMaxRetries());
            }
        }

        return taskExecution;
    }
}
