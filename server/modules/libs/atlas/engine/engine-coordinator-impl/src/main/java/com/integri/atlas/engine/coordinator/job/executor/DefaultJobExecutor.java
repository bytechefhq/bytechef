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

package com.integri.atlas.engine.coordinator.job.executor;

import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.JobStatus;
import com.integri.atlas.engine.coordinator.workflow.Workflow;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowRepository;
import com.integri.atlas.engine.core.context.Context;
import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.task.WorkflowTask;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.evaluator.TaskEvaluator;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.engine.core.uuid.UUIDGenerator;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Arik Cohen
 * @since Apr 24, 2017
 */
public class DefaultJobExecutor implements JobExecutor {

    private WorkflowRepository workflowRepository;
    private TaskExecutionRepository taskExecutionRepository;
    private ContextRepository contextRepository;
    private TaskDispatcher taskDispatcher;
    private TaskEvaluator taskEvaluator;

    @Override
    public void execute(Job job) {
        Workflow workflow = workflowRepository.findOne(job.getWorkflowId());

        if (job.getStatus() != JobStatus.STARTED) {
            throw new IllegalStateException("should not be here");
        } else if (hasMoreTasks(job, workflow)) {
            executeNextTask(job, workflow);
        } else {
            throw new IllegalStateException("no tasks to execute!");
        }
    }

    public void setContextRepository(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    public void setTaskExecutionRepository(TaskExecutionRepository taskExecutionRepository) {
        this.taskExecutionRepository = taskExecutionRepository;
    }

    public void setTaskDispatcher(TaskDispatcher taskDispatcher) {
        this.taskDispatcher = taskDispatcher;
    }

    public void setTaskEvaluator(TaskEvaluator taskEvaluator) {
        this.taskEvaluator = taskEvaluator;
    }

    public void setWorkflowRepository(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
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
        Context context = new MapContext(contextRepository.peek(job.getId()));

        contextRepository.push(nextTaskExecution.getId(), context);

        TaskExecution evaluatedTaskExecution = taskEvaluator.evaluate(nextTaskExecution, context);

        taskExecutionRepository.create(evaluatedTaskExecution);
        taskDispatcher.dispatch(evaluatedTaskExecution);
    }
}
