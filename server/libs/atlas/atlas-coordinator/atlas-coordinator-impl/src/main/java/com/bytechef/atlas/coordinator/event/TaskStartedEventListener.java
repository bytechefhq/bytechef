
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
import com.bytechef.atlas.domain.TaskExecution.Status;
import com.bytechef.atlas.event.TaskStartedWorkflowEvent;
import com.bytechef.event.listener.EventListener;
import com.bytechef.event.WorkflowEvent;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.CancelControlTask;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apt 9, 2017
 */
public class TaskStartedEventListener implements EventListener {

    private static final Logger logger = LoggerFactory.getLogger(TaskStartedEventListener.class);

    private final TaskExecutionService taskExecutionService;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final JobService jobService;

    @SuppressFBWarnings("EI2")
    public TaskStartedEventListener(
        TaskExecutionService taskExecutionService, TaskDispatcher<? super Task> taskDispatcher, JobService jobService) {
        this.taskExecutionService = taskExecutionService;
        this.taskDispatcher = taskDispatcher;
        this.jobService = jobService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void onApplicationEvent(WorkflowEvent workflowEvent) {
        if (TaskStartedWorkflowEvent.TASK_STARTED.equals(workflowEvent.getType())) {
            long taskExecutionId = ((TaskStartedWorkflowEvent) workflowEvent).getTaskExecutionId();

            TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

            if (logger.isDebugEnabled()) {
                if (StringUtils.hasText(taskExecution.getName())) {
                    logger.debug(
                        "Task id={}, name='{}', type='{}' started", taskExecution.getId(), taskExecution.getName(),
                        taskExecution.getType());
                } else {
                    logger.debug("Task id={}, type='{}' started", taskExecution.getId(), taskExecution.getType());
                }
            }

            Job job = jobService.getTaskExecutionJob(taskExecutionId);

            if (taskExecution.getStatus() == Status.CANCELLED || job.getStatus() != Job.Status.STARTED) {
                taskDispatcher.dispatch(new CancelControlTask(taskExecution.getJobId(), taskExecution.getId()));
            } else {
                if (taskExecution.getStartDate() == null && taskExecution.getStatus() != Status.STARTED) {
                    taskExecution.setStartDate(workflowEvent.getCreatedDate());
                    taskExecution.setStatus(Status.STARTED);

                    taskExecution = taskExecutionService.update(taskExecution);
                }

                if (taskExecution.getParentId() != null) {
                    onApplicationEvent(new TaskStartedWorkflowEvent(taskExecution.getParentId()));
                }
            }
        }
    }
}
