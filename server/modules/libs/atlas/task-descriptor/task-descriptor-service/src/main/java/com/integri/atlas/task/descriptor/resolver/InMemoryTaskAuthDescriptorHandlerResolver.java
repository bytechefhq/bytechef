/*
 * Copyright 2021 <your company/name>.
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

package com.integri.atlas.task.descriptor.resolver;

import com.integri.atlas.task.descriptor.handler.TaskAuthDescriptorHandler;
import com.integri.atlas.task.descriptor.handler.TaskAuthDescriptorHandlerResolver;
import com.integri.atlas.task.descriptor.model.TaskAuthDescriptors;
import java.util.List;
import java.util.Objects;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(1)
public class InMemoryTaskAuthDescriptorHandlerResolver implements TaskAuthDescriptorHandlerResolver {

    public static final String IN_MEMORY = "IN_MEMORY";

    private final List<TaskAuthDescriptorHandler> taskAuthDescriptorHandlers;

    public InMemoryTaskAuthDescriptorHandlerResolver(List<TaskAuthDescriptorHandler> taskAuthDescriptorHandlers) {
        this.taskAuthDescriptorHandlers = taskAuthDescriptorHandlers;
    }

    @Override
    public List<TaskAuthDescriptorHandler> getTaskAuthDescriptorHandlers() {
        return taskAuthDescriptorHandlers;
    }

    @Override
    public TaskAuthDescriptorHandler resolve(String taskName) {
        return taskAuthDescriptorHandlers
            .stream()
            .filter(taskDescriptorHandler -> {
                TaskAuthDescriptors taskAuthDescriptors = taskDescriptorHandler.getTaskAuthDescriptors();

                return Objects.equals(taskAuthDescriptors.getTaskName(), taskName);
            })
            .findFirst()
            .orElse(null);
    }
}
