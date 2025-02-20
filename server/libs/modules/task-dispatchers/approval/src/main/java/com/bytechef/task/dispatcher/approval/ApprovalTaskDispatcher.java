/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.task.dispatcher.approval;

import static com.bytechef.task.dispatcher.approval.constant.ApprovalTaskDispatcherConstants.APPROVAL;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.TaskExecution;
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
public class ApprovalTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalTaskDispatcher.class);

    private final ApplicationEventPublisher eventPublisher;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public ApprovalTaskDispatcher(
        ApplicationEventPublisher eventPublisher, TaskExecutionService taskExecutionService) {

        this.eventPublisher = eventPublisher;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        taskExecution.setEndDate(Instant.now());
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        eventPublisher.publishEvent(new StopJobEvent(Objects.requireNonNull(taskExecution.getJobId())));

        if (logger.isDebugEnabled()) {
            logger.debug("Approval task execution completed: {}", taskExecution);
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), APPROVAL + "/v1")) {
            return this;
        }

        return null;
    }
}
