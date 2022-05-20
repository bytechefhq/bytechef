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
import java.util.List;
import java.util.Objects;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class TaskHandlerResolverChain implements TaskHandlerResolver {

    private List<TaskHandlerResolver> taskHandlerResolvers;

    @Override
    public TaskHandler<?> resolve(Task aTask) {
        for (TaskHandlerResolver taskHandlerResolver : taskHandlerResolvers) {
            TaskHandler<?> handler = taskHandlerResolver.resolve(aTask);
            if (handler != null) {
                return handler;
            }
        }
        throw new IllegalArgumentException("Unknown task handler: " + aTask.getType());
    }

    public void setTaskHandlerResolvers(List<TaskHandlerResolver> taskHandlerResolvers) {
        this.taskHandlerResolvers = Objects.requireNonNull(taskHandlerResolvers);
    }
}
