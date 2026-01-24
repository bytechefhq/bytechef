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

package com.bytechef.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.message.route.TaskWorkerMessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class DefaultTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskDispatcher.class);

    private final ApplicationEventPublisher eventPublisher;
    private final List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors;

    private static final TaskWorkerMessageRoute DEFAULT_MESSAGE_ROUTE = TaskWorkerMessageRoute.TASK_EXECUTION_EVENTS;

    @SuppressFBWarnings("EI")
    public DefaultTaskDispatcher(
        ApplicationEventPublisher eventPublisher,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors) {

        this.eventPublisher = eventPublisher;
        this.taskDispatcherPreSendProcessors = taskDispatcherPreSendProcessors;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        taskExecution = preProcess(taskExecution);

        if (taskExecution.getStatus() == TaskExecution.Status.CANCELLED) {
            logger.debug("Task id={} is not eligible for dispatching", taskExecution.getId());

            return;
        }

        TaskWorkerMessageRoute messageRoute = calculateQueueName(taskExecution);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Task id={}, type='{}' sent to route='{}'", taskExecution.getId(), taskExecution.getType(),
                messageRoute);
        }

        eventPublisher.publishEvent(new TaskExecutionEvent(messageRoute, taskExecution));
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (task instanceof TaskExecution) {
            return this;
        }

        return null;
    }

    private TaskWorkerMessageRoute calculateQueueName(Task task) {
        TaskExecution taskExecution = (TaskExecution) task;

        return taskExecution.getNode() != null
            ? TaskWorkerMessageRoute.ofTaskMessageRoute(taskExecution.getNode())
            : DEFAULT_MESSAGE_ROUTE;
    }

    private TaskExecution preProcess(TaskExecution taskExecution) {
        for (TaskDispatcherPreSendProcessor taskDispatcherPreSendProcessor : taskDispatcherPreSendProcessors) {
            if (taskDispatcherPreSendProcessor.canProcess(taskExecution)) {

                taskExecution = taskDispatcherPreSendProcessor.process(taskExecution);

                if (taskExecution.getStatus() == TaskExecution.Status.CANCELLED) {
                    break;
                }
            }
        }

        return taskExecution;
    }
}
