
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

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apr 24, 2017
 */
public class JobExecutor {

    private final ContextService contextService;
    private final TaskDispatcher<? super TaskExecution> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskEvaluator taskEvaluator;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public JobExecutor(
        ContextService contextService, TaskDispatcher<? super TaskExecution> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskEvaluator taskEvaluator, WorkflowService workflowService) {

        this.contextService = contextService;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskEvaluator = taskEvaluator;
        this.workflowService = workflowService;
    }

    public void execute(Job job) {
        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        if (job.getStatus() != Job.Status.STARTED) {
            throw new IllegalStateException("should not be here");
        } else if (hasMoreTasks(job, workflow)) {
            executeNextTask(job, workflow);
        } else {
            throw new IllegalStateException("no tasks to execute!");
        }
    }

    @SuppressFBWarnings("NP")
    private void executeNextTask(Job job, Workflow workflow) {
        Assert.notNull(job.getId(), "'job.id' must not be null");

        Map<String, Object> context = contextService.peek(job.getId(), Context.Classname.JOB);
        TaskExecution nextTaskExecution = nextTaskExecution(job, workflow);

        nextTaskExecution = taskExecutionService.create(nextTaskExecution);

        nextTaskExecution = taskEvaluator.evaluate(nextTaskExecution, context);

        Assert.notNull(nextTaskExecution.getId(), "'nextTaskExecution.id' must not be null");

        contextService.push(nextTaskExecution.getId(), Context.Classname.TASK_EXECUTION, context);

        taskDispatcher.dispatch(nextTaskExecution);
    }

    private boolean hasMoreTasks(Job job, Workflow workflow) {
        List<WorkflowTask> workflowTasks = workflow.getTasks();

        return job.getCurrentTask() < workflowTasks.size();
    }

    @SuppressFBWarnings("NP")
    private TaskExecution nextTaskExecution(Job job, Workflow workflow) {
        List<WorkflowTask> workflowTasks = workflow.getTasks();

        WorkflowTask workflowTask = workflowTasks.get(job.getCurrentTask());

        Assert.notNull(job.getId(), "'job.id' must not be null");

        TaskExecution taskExecution = TaskExecution.of(job.getId(), job.getPriority(), workflowTask);

        if (workflow.getRetry() > 0 && taskExecution.getRetry() < 1) {
            taskExecution.setRetry(workflow.getRetry());
        }

        return taskExecution;
    }
}
