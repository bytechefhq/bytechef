
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

package com.bytechef.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.TaskMessageRoute;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.task.Task;

import java.util.List;
import java.util.Objects;

import com.bytechef.message.broker.MessageRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class DefaultTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private static final Logger log = LoggerFactory.getLogger(DefaultTaskDispatcher.class);

    private final MessageBroker messageBroker;
    private final List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors;

    private static final MessageRoute DEFAULT_MESSAGE_ROUTE = TaskMessageRoute.TASKS;

    public DefaultTaskDispatcher(
        MessageBroker messageBroker, List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors) {
        this.messageBroker = Objects.requireNonNull(messageBroker);
        this.taskDispatcherPreSendProcessors = taskDispatcherPreSendProcessors == null
            ? List.of()
            : taskDispatcherPreSendProcessors;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        for (TaskDispatcherPreSendProcessor taskDispatcherPreSendProcessor : taskDispatcherPreSendProcessors) {
            taskExecution = taskDispatcherPreSendProcessor.process(taskExecution);
        }

        MessageRoute messageRoute = calculateQueueName(taskExecution);

        if (log.isDebugEnabled()) {
            log.debug(
                "Task id={}, type='{}' sent to route='{}'", taskExecution.getId(), taskExecution.getType(),
                messageRoute);
        }

        messageBroker.send(messageRoute, taskExecution);
    }

    private MessageRoute calculateQueueName(Task task) {
        TaskExecution taskExecution = (TaskExecution) task;

        return taskExecution.getNode() != null
            ? TaskMessageRoute.ofRoute(taskExecution.getNode())
            : DEFAULT_MESSAGE_ROUTE;
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (task instanceof TaskExecution) {
            return this;
        }

        return null;
    }
}
