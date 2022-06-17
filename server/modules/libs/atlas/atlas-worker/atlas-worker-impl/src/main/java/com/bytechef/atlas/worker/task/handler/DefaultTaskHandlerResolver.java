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

package com.bytechef.atlas.worker.task.handler;

import com.bytechef.atlas.task.Task;
import java.util.Map;

/**
 * @author Arik Cohen
 */
public class DefaultTaskHandlerResolver implements TaskHandlerResolver {

    private final Map<String, TaskHandler<?>> taskHandlers;

    public DefaultTaskHandlerResolver(Map<String, TaskHandler<?>> taskHandlers) {
        this.taskHandlers = taskHandlers;
    }

    @Override
    public TaskHandler<?> resolve(Task task) {
        return taskHandlers.get(task.getType());
    }
}
