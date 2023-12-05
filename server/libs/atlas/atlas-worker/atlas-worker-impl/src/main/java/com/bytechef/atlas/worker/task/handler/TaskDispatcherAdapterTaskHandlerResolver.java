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

package com.bytechef.atlas.worker.task.handler;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.worker.task.factory.TaskDispatcherAdapterFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherAdapterTaskHandlerResolver implements TaskHandlerResolver {

    private final Map<String, TaskHandler<?>> taskHandlers;

    public TaskDispatcherAdapterTaskHandlerResolver(
        List<TaskDispatcherAdapterFactory> taskDispatcherAdapterFactories, TaskHandlerResolver taskHandlerResolver) {

        taskHandlers = taskDispatcherAdapterFactories.stream()
            .collect(
                Collectors.toMap(
                    TaskDispatcherAdapterFactory::getName,
                    taskDispatcherAdapterFactory -> taskDispatcherAdapterFactory.create(taskHandlerResolver)));
    }

    @Override
    public TaskHandler<?> resolve(Task task) {
        return taskHandlers.get(task.getType());
    }
}
