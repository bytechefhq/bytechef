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

package com.bytechef.hermes.descriptor.resolver;

import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandlerResolver;
import com.bytechef.hermes.descriptor.model.TaskDescriptor;
import java.util.List;
import java.util.Objects;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(1)
public class InMemoryTaskDescriptorHandlerResolver implements TaskDescriptorHandlerResolver {

    public static final String IN_MEMORY = "IN_MEMORY";

    private final List<TaskDescriptorHandler> taskDescriptorHandlers;

    public InMemoryTaskDescriptorHandlerResolver(List<TaskDescriptorHandler> taskDescriptorHandlers) {
        this.taskDescriptorHandlers = taskDescriptorHandlers;
    }

    @Override
    public List<TaskDescriptorHandler> getTaskDescriptorHandlers() {
        return taskDescriptorHandlers;
    }

    @Override
    public TaskDescriptorHandler resolve(String name, float version) {
        return taskDescriptorHandlers.stream()
                .filter(taskDescriptorHandler -> {
                    TaskDescriptor taskDescriptor = taskDescriptorHandler.getTaskDescriptor();

                    return Objects.equals(taskDescriptor.getName(), name);
                })
                .findFirst()
                .orElse(null);
    }
}
