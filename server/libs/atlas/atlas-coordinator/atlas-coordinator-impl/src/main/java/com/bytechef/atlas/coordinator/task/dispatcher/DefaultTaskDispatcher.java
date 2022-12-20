
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
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import java.util.List;
import java.util.Objects;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 */
public class DefaultTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final MessageBroker messageBroker;
    private final List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors;

    private static final String DEFAULT_QUEUE = Queues.TASKS;

    public DefaultTaskDispatcher(
        MessageBroker messageBroker, List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors) {
        this.messageBroker = Objects.requireNonNull(messageBroker);
        this.taskDispatcherPreSendProcessors = taskDispatcherPreSendProcessors == null ? List.of()
            : taskDispatcherPreSendProcessors;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        Assert.notNull(messageBroker, "message broker not configured");

        for (TaskDispatcherPreSendProcessor taskDispatcherPreSendProcessor : taskDispatcherPreSendProcessors) {
            taskExecution = taskDispatcherPreSendProcessor.process(taskExecution);
        }

        messageBroker.send(calculateRoutingKey(taskExecution), taskExecution);
    }

    private String calculateRoutingKey(Task task) {
        TaskExecution taskExecution = (TaskExecution) task;

        return taskExecution.getNode() != null ? taskExecution.getNode() : DEFAULT_QUEUE;
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (task instanceof TaskExecution) {
            return this;
        }

        return null;
    }
}
