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

package com.bytechef.atlas.coordinator.job.executor;

import com.bytechef.atlas.context.domain.Context;
import com.bytechef.atlas.context.domain.MapContext;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.atlas.service.context.ContextService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.service.workflow.WorkflowService;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.atlas.uuid.UUIDGenerator;
import com.bytechef.atlas.workflow.domain.Workflow;
import java.util.Date;
import java.util.List;

/**
 * @author Arik Cohen
 * @since Apr 24, 2017
 */
public class DefaultJobExecutor implements JobExecutor {

    private WorkflowService workflowService;
    private TaskExecutionService taskExecutionService;
    private ContextService contextService;
    private TaskDispatcher taskDispatcher;
    private TaskEvaluator taskEvaluator;

    @Override
    public void execute(Job job) {
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        if (job.getStatus() != JobStatus.STARTED) {
            throw new IllegalStateException("should not be here");
        } else if (hasMoreTasks(job, workflow)) {
            executeNextTask(job, workflow);
        } else {
            throw new IllegalStateException("no tasks to execute!");
        }
    }

    public void setContextService(ContextService contextService) {
        this.contextService = contextService;
    }

    public void setTaskExecutionService(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    public void setTaskDispatcher(TaskDispatcher taskDispatcher) {
        this.taskDispatcher = taskDispatcher;
    }

    public void setTaskEvaluator(TaskEvaluator taskEvaluator) {
        this.taskEvaluator = taskEvaluator;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    private boolean hasMoreTasks(Job aJob, Workflow aWorkflow) {
        return aJob.getCurrentTask() < aWorkflow.getTasks().size();
    }

    private SimpleTaskExecution nextTaskExecution(Job job, Workflow workflow) {
        List<WorkflowTask> workflowTasks = workflow.getTasks();

        WorkflowTask workflowTask = workflowTasks.get(job.getCurrentTask());

        SimpleTaskExecution taskExecution = new SimpleTaskExecution(workflowTask.asMap());

        taskExecution.setCreateTime(new Date());
        taskExecution.setId(UUIDGenerator.generate());
        taskExecution.setJobId(job.getId());
        taskExecution.setPriority(job.getPriority());
        taskExecution.setStatus(TaskStatus.CREATED);

        if (workflow.getRetry() > 0 && taskExecution.getRetry() < 1) {
            taskExecution.setRetry(workflow.getRetry());
        }

        return taskExecution;
    }

    private void executeNextTask(Job job, Workflow workflow) {
        TaskExecution nextTaskExecution = nextTaskExecution(job, workflow);
        Context context = new MapContext(contextService.peek(job.getId()));

        contextService.push(nextTaskExecution.getId(), context);

        TaskExecution evaluatedTaskExecution = taskEvaluator.evaluate(nextTaskExecution, context);

        taskExecutionService.create(evaluatedTaskExecution);
        taskDispatcher.dispatch(evaluatedTaskExecution);
    }
}
