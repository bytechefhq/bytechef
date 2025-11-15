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
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apr 24, 2017
 */
public class JobExecutor {

    private static final Logger logger = LoggerFactory.getLogger(JobExecutor.class);

    private final ContextService contextService;
    private final Evaluator evaluator;
    private final TaskDispatcher<? super TaskExecution> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public JobExecutor(
        ContextService contextService, Evaluator evaluator, TaskDispatcher<? super TaskExecution> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        this.contextService = contextService;
        this.evaluator = evaluator;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    public void execute(Job job) {
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        if (job.getStatus() != Job.Status.STARTED) {
            throw new IllegalStateException("Should not be here");
        } else if (hasMoreTasks(job, workflow)) {
            executeNextTask(job, workflow);
        } else {
            throw new IllegalStateException("No tasks to execute!");
        }
    }

    private void executeNextTask(Job job, Workflow workflow) {
        Assert.notNull(job.getId(), "'job.id' must not be null");

        if (logger.isTraceEnabled()) {
            logger.trace("executeNextTask: job={}, workflow={}", job, workflow);
        }

        Map<String, ?> context = taskFileStorage.readContextValue(
            contextService.peek(Validate.notNull(job.getId(), "id"), Context.Classname.JOB));
        TaskExecution nextTaskExecution = nextTaskExecution(job, workflow);

        nextTaskExecution = taskExecutionService.create(nextTaskExecution.evaluate(context, evaluator));

        contextService.push(
            Validate.notNull(nextTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION,
            taskFileStorage.storeContextValue(
                Validate.notNull(nextTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION, context));

        taskDispatcher.dispatch(nextTaskExecution);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Task id={}, type='{}', name='{}' executed",
                nextTaskExecution.getId(), nextTaskExecution.getType(), nextTaskExecution.getName());
        }
    }

    private boolean hasMoreTasks(Job job, Workflow workflow) {
        List<WorkflowTask> workflowTasks = workflow.getTasks();

        return job.getCurrentTask() < workflowTasks.size();
    }

    private TaskExecution nextTaskExecution(Job job, Workflow workflow) {
        List<WorkflowTask> workflowTasks = workflow.getTasks();

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
