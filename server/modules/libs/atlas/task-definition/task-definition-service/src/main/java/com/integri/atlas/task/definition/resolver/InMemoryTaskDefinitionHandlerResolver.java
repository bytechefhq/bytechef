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

package com.integri.atlas.task.definition.resolver;

import com.integri.atlas.task.definition.handler.TaskDefinitionHandler;
import com.integri.atlas.task.definition.handler.TaskDefinitionHandlerResolver;
import com.integri.atlas.task.definition.model.TaskDefinition;
import java.util.List;
import java.util.Objects;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(1)
public class InMemoryTaskDefinitionHandlerResolver implements TaskDefinitionHandlerResolver {

    public static final String IN_MEMORY = "IN_MEMORY";

    private final List<TaskDefinitionHandler> taskDefinitionHandlers;

    public InMemoryTaskDefinitionHandlerResolver(List<TaskDefinitionHandler> taskDefinitionHandlers) {
        this.taskDefinitionHandlers = taskDefinitionHandlers;
    }

    @Override
    public List<TaskDefinitionHandler> getTaskDefinitionHandlers() {
        return taskDefinitionHandlers;
    }

    @Override
    public TaskDefinitionHandler resolve(String name) {
        return taskDefinitionHandlers
            .stream()
            .filter(taskDefinitionHandler -> {
                TaskDefinition taskDefinition = taskDefinitionHandler.getTaskDefinition();

                return Objects.equals(taskDefinition.getName(), name);
            })
            .findFirst()
            .orElse(null);
    }
}
