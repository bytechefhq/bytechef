/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import com.integri.atlas.engine.core.task.Task;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Arik Cohen
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultTaskHandlerResolver implements TaskHandlerResolver {

    private final Map<String, TaskHandler<?>> taskHandlers;

    public DefaultTaskHandlerResolver(Map<String, TaskHandler<?>> aTaskHandlers) {
        taskHandlers = aTaskHandlers;
    }

    @Override
    public TaskHandler<?> resolve(Task aJobTask) {
        return taskHandlers.get(aJobTask.getType());
    }
}
