
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

package com.bytechef.task.dispatcher.subflow.event;

import com.bytechef.atlas.coordinator.CoordinatorManager;
import com.bytechef.atlas.coordinator.event.EventListener;
import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.event.JobStatusWorkflowEvent;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
 * an {@link EventListener} which is used for listening to subflow job status events. When a sub-flow completes/fails or
 * stops its parent job and its parent task needs to be informed so as to resume its execution.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Sep 06, 2018
 * @see com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher
 */
public class SubflowJobStatusEventListener implements EventListener {

    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final CoordinatorManager coordinatorManager;
    private final TaskEvaluator taskEvaluator;

    @SuppressFBWarnings("EI2")
    public SubflowJobStatusEventListener(
        JobService jobService,
        TaskExecutionService taskExecutionService,
        CoordinatorManager coordinatorManager,
        TaskEvaluator taskEvaluator) {
        this.jobService = Objects.requireNonNull(jobService);
        this.taskExecutionService = Objects.requireNonNull(taskExecutionService);
        this.coordinatorManager = Objects.requireNonNull(coordinatorManager);
        this.taskEvaluator = Objects.requireNonNull(taskEvaluator);
    }

    @Override
    public void onApplicationEvent(WorkflowEvent workflowEvent) {
        String type = workflowEvent.getType();

        if (type.equals(JobStatusWorkflowEvent.JOB_STATUS)) {
            JobStatusWorkflowEvent jobStatusWorkflowEvent = (JobStatusWorkflowEvent) workflowEvent;

            Job.Status status = jobStatusWorkflowEvent.getJobStatus();
            Job job = jobService.getJob(jobStatusWorkflowEvent.getJobId());

            if (job.getParentTaskExecutionId() == null) {
                return; // not a subflow -- nothing to do
            }

            switch (status) {
                case CREATED:
                case STARTED:
                    break;
                case STOPPED: {
                    TaskExecution subflowTaskExecution = taskExecutionService
                        .getTaskExecution(job.getParentTaskExecutionId());

                    coordinatorManager.stop(subflowTaskExecution.getJobId());

                    break;
                }
                case FAILED: {
                    TaskExecution erroredTaskExecution = taskExecutionService
                        .getTaskExecution(job.getParentTaskExecutionId());

                    erroredTaskExecution.setError(new ExecutionError("An error occurred with subflow", List.of()));

                    coordinatorManager.handleError(erroredTaskExecution);

                    break;
                }
                case COMPLETED: {
                    TaskExecution completionTaskExecution = taskExecutionService
                        .getTaskExecution(job.getParentTaskExecutionId());
                    Object output = job.getOutputs();

                    if (completionTaskExecution.getOutput() == null) {
                        completionTaskExecution.setOutput(output);
                    } else {
                        // TODO check, it seems wrong
                        completionTaskExecution.evaluate(
                            taskEvaluator, new Context("execution", new Context("output", output)));
                    }

                    coordinatorManager.complete(completionTaskExecution);

                    break;
                }
                default:
                    throw new IllegalStateException("Unknown status: " + status);
            }
        }
    }
}
