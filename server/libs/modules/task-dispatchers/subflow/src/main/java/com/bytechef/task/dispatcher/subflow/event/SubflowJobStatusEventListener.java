
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

import com.bytechef.atlas.execution.message.broker.TaskMessageRoute;
import com.bytechef.event.listener.EventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import com.bytechef.atlas.execution.event.JobStatusWorkflowEvent;
import com.bytechef.event.WorkflowEvent;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * an {@link EventListener} which is used for listening to subflow job status events. When a sub-flow completes/fails or
 * stops its parent job and its parent task needs to be informed to resume its execution.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Sep 06, 2018
 * @see com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher
 */
public class SubflowJobStatusEventListener implements EventListener {

    private final JobService jobService;
    private final MessageBroker messageBroker;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public SubflowJobStatusEventListener(
        JobService jobService, MessageBroker messageBroker, TaskExecutionService taskExecutionService) {

        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.messageBroker = messageBroker;
    }

    @Override
    public void onApplicationEvent(WorkflowEvent workflowEvent) {
        if (JobStatusWorkflowEvent.JOB_STATUS.equals(workflowEvent.getType())) {
            JobStatusWorkflowEvent jobStatusWorkflowEvent = (JobStatusWorkflowEvent) workflowEvent;

            Job.Status status = jobStatusWorkflowEvent.getStatus();
            Job job = jobService.getJob(jobStatusWorkflowEvent.getJobId());

            if (job.getParentTaskExecutionId() == null) {
                return; // not a subflow -- nothing to do
            }

            switch (status) {
                case CREATED:
                case STARTED:
                    break;
                case STOPPED: {
                    TaskExecution subflowTaskExecution = taskExecutionService.getTaskExecution(
                        job.getParentTaskExecutionId());

                    messageBroker.send(TaskMessageRoute.TASKS_STOPS, subflowTaskExecution.getJobId());

                    break;
                }
                case FAILED: {
                    TaskExecution erroredTaskExecution = taskExecutionService.getTaskExecution(
                        job.getParentTaskExecutionId());

                    erroredTaskExecution.setError(new ExecutionError("An error occurred with subflow", List.of()));

                    messageBroker.send(SystemMessageRoute.ERRORS, erroredTaskExecution);

                    break;
                }
                case COMPLETED: {
                    TaskExecution completionTaskExecution = taskExecutionService.getTaskExecution(
                        job.getParentTaskExecutionId());

                    Object output = job.getOutputs();

                    if (completionTaskExecution.getOutput() == null) {
                        completionTaskExecution.setOutput(output);
                    } else {
                        // TODO check, it seems wrong
                        completionTaskExecution.evaluate(Map.of("execution", Map.of("output", output)));
                    }

                    messageBroker.send(TaskMessageRoute.TASKS_COMPLETIONS, completionTaskExecution);

                    break;
                }
                default:
                    throw new IllegalStateException("Unknown status: %s".formatted(status));
            }
        }
    }
}
