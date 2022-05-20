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

package com.integri.atlas.engine.worker.task.handler;

import com.integri.atlas.engine.task.Task;
import java.util.Map;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class TaskDispatcherAdapterTaskHandlerResolver implements TaskHandlerResolver {

    private final Map<String, TaskHandler<?>> taskHandlers;

    public TaskDispatcherAdapterTaskHandlerResolver(Map<String, TaskHandler<?>> taskDispatcherTaskHandlerAdapters) {
        taskHandlers = taskDispatcherTaskHandlerAdapters;
    }

    @Override
    public TaskHandler<?> resolve(Task aTask) {
        return taskHandlers.get(aTask.getType());
    }
}
