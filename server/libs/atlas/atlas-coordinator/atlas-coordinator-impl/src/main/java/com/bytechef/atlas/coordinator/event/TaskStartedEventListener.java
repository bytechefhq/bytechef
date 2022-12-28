
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

package com.bytechef.atlas.coordinator.event;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.event.TaskStartedWorkflowEvent;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.CancelControlTask;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.execution.TaskStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apt 9, 2017
 */
public class TaskStartedEventListener implements EventListener {

    private final TaskExecutionService taskExecutionService;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final JobService jobService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @SuppressFBWarnings("EI2")
    public TaskStartedEventListener(
        TaskExecutionService taskExecutionService, TaskDispatcher<? super Task> taskDispatcher, JobService jobService) {
        this.taskExecutionService = taskExecutionService;
        this.taskDispatcher = taskDispatcher;
        this.jobService = jobService;
    }

    @Override
    public void onApplicationEvent(WorkflowEvent workflowEvent) {
        if (TaskStartedWorkflowEvent.TASK_STARTED.equals(workflowEvent.getType())) {
            String taskId = ((TaskStartedWorkflowEvent) workflowEvent).getTaskId();

            TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskId);

            if (taskExecution == null) {
                logger.error("Unknown task: {}", taskId);

                return;
            }

            Job job = jobService.getTaskExecutionJob(taskId);

            if (taskExecution.getStatus() == TaskStatus.CANCELLED || job.getStatus() != Job.Status.STARTED) {
                taskDispatcher.dispatch(new CancelControlTask(taskExecution.getJobId(), taskExecution.getId()));
            } else {
                if (taskExecution.getStartTime() == null && taskExecution.getStatus() != TaskStatus.STARTED) {
                    taskExecution.setStartTime(workflowEvent.getCreatedDate());
                    taskExecution.setStatus(TaskStatus.STARTED);

                    taskExecutionService.update(taskExecution);
                }

                if (taskExecution.getParentId() != null) {
                    onApplicationEvent(new TaskStartedWorkflowEvent(taskExecution.getParentId()));
                }
            }
        }
    }
}
