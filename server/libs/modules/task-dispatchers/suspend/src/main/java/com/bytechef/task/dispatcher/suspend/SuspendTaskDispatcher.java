/*
 * Copyright 2025 ByteChef
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
 */

package com.bytechef.task.dispatcher.suspend;

import static com.bytechef.task.dispatcher.suspend.constant.SuspendTaskDispatcherConstants.SUSPEND;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.domain.TaskExecution.Status;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class SuspendTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private static final Logger logger = LoggerFactory.getLogger(SuspendTaskDispatcher.class);

    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public SuspendTaskDispatcher(
        ApplicationEventPublisher eventPublisher, JobService jobService, TaskExecutionService taskExecutionService) {

        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        taskExecution.setStartDate(Instant.now());
        taskExecution.setStatus(Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        Job job = jobService.getJob(Objects.requireNonNull(taskExecution.getJobId()));

        job = jobService.setStatusToStopped(Objects.requireNonNull(job.getId()));

        eventPublisher.publishEvent(
            new JobStatusApplicationEvent(Objects.requireNonNull(job.getId()), job.getStatus()));

        if (logger.isDebugEnabled()) {
            logger.debug("Suspend task execution completed: {}", taskExecution);
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), SUSPEND + "/v1")) {
            return this;
        }

        return null;
    }
}
