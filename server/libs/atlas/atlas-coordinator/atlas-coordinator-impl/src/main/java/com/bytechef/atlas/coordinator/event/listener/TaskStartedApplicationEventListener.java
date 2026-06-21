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

package com.bytechef.atlas.coordinator.event.listener;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.domain.TaskExecution.Status;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apt 9, 2017
 */
public class TaskStartedApplicationEventListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(TaskStartedApplicationEventListener.class);

    private static final int MAX_PARENT_CHAIN_DEPTH = 10_000;

    private final TaskExecutionService taskExecutionService;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final JobService jobService;

    @SuppressFBWarnings("EI2")
    public TaskStartedApplicationEventListener(
        TaskExecutionService taskExecutionService, TaskDispatcher<? super Task> taskDispatcher,
        JobService jobService) {

        this.taskExecutionService = taskExecutionService;
        this.taskDispatcher = taskDispatcher;
        this.jobService = jobService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof TaskStartedApplicationEvent taskStartedApplicationEvent)) {
            return;
        }

        long taskExecutionId = taskStartedApplicationEvent.getTaskExecutionId();
        Instant createDate = taskStartedApplicationEvent.getCreateDate();

        // Walk up the task parent chain iteratively (rather than re-entering this method per parent) with a hard depth
        // bound, so a malformed or maliciously deep parent chain cannot exhaust the stack and crash the coordinator.
        for (int depth = 0; depth < MAX_PARENT_CHAIN_DEPTH; depth++) {
            TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "Task id={}, name='{}', type='{}' started", taskExecution.getId(), taskExecution.getName(),
                    taskExecution.getType());
            }

            Job job = jobService.getTaskExecutionJob(taskExecutionId);

            if (taskExecution.getStatus() == Status.CANCELLED || job.getStatus() != Job.Status.STARTED) {
                taskDispatcher.dispatch(
                    new CancelControlTask(
                        Validate.notNull(taskExecution.getJobId(), "jobId"),
                        Validate.notNull(taskExecution.getId(), "id")));

                return;
            }

            if (taskExecution.getStartDate() == null && taskExecution.getStatus() != Status.STARTED) {
                taskExecution.setStartDate(createDate);
                taskExecution.setStatus(Status.STARTED);

                taskExecution = taskExecutionService.update(taskExecution);
            }

            Long parentId = taskExecution.getParentId();

            if (parentId == null) {
                return;
            }

            taskExecutionId = parentId;
            createDate = Instant.now();
        }

        log.warn(
            "Task parent chain exceeded the maximum depth of {}; stopping the walk at task execution id={}",
            MAX_PARENT_CHAIN_DEPTH, taskExecutionId);
    }
}
