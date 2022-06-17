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

import com.bytechef.atlas.context.domain.MapContext;
import com.bytechef.atlas.coordinator.Coordinator;
import com.bytechef.atlas.coordinator.event.EventListener;
import com.bytechef.atlas.error.ErrorObject;
import com.bytechef.atlas.event.Events;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.atlas.job.service.JobService;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher;
import com.bytechef.task.execution.service.TaskExecutionService;
import java.util.Objects;

/**
 * an {@link EventListener} which is used for listening to subflow job status events. When a
 * sub-flow completes/fails or stops its parent job and its parent task needs to be informed so as
 * to resume its execution.
 *
 * @author Arik Cohen
 * @since Sep 06, 2018
 * @see SubflowTaskDispatcher
 */
public class SubflowJobStatusEventListener implements EventListener {

    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final Coordinator coordinator;
    private final TaskEvaluator taskEvaluator;

    public SubflowJobStatusEventListener(
            JobService jobService,
            TaskExecutionService taskExecutionService,
            Coordinator coordinator,
            TaskEvaluator taskEvaluator) {
        this.jobService = Objects.requireNonNull(jobService);
        this.taskExecutionService = Objects.requireNonNull(taskExecutionService);
        this.coordinator = Objects.requireNonNull(coordinator);
        this.taskEvaluator = Objects.requireNonNull(taskEvaluator);
    }

    @Override
    public void onApplicationEvent(WorkflowEvent aEvent) {
        if (aEvent.getType().equals(Events.JOB_STATUS)) {
            String jobId = aEvent.getRequiredString("jobId");
            JobStatus status = JobStatus.valueOf(aEvent.getRequiredString("status"));
            Job job = jobService.getJob(jobId);

            if (job.getParentTaskExecutionId() == null) {
                return; // not a subflow -- nothing to do
            }

            switch (status) {
                case CREATED:
                case STARTED:
                    break;
                case STOPPED: {
                    TaskExecution subflowTask = taskExecutionService.getTaskExecution(job.getParentTaskExecutionId());
                    coordinator.stop(subflowTask.getJobId());
                    break;
                }
                case FAILED: {
                    SimpleTaskExecution errorable = SimpleTaskExecution.of(
                            taskExecutionService.getTaskExecution(job.getParentTaskExecutionId()));
                    errorable.setError(new ErrorObject("An error occured with subflow", new String[0]));
                    coordinator.handleError(errorable);
                    break;
                }
                case COMPLETED: {
                    SimpleTaskExecution completion = SimpleTaskExecution.of(
                            taskExecutionService.getTaskExecution(job.getParentTaskExecutionId()));
                    Object output = job.getOutputs();
                    if (completion.getOutput() != null) {
                        TaskExecution evaluated = taskEvaluator.evaluate(
                                completion, new MapContext("execution", new MapContext("output", output)));
                        completion = SimpleTaskExecution.of(evaluated);
                    } else {
                        completion.setOutput(output);
                    }
                    coordinator.complete(completion);
                    break;
                }
                default:
                    throw new IllegalStateException("Unnown status: " + status);
            }
        }
    }
}
