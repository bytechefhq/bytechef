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
import com.integri.atlas.task.definition.repository.ExtTaskDefinitionHandlerRepository;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractExtTaskDefinitionHandlerResolver implements TaskDefinitionHandlerResolver {

    private final ExtTaskDefinitionHandlerRepository extTaskDefinitionHandlerRepository;
    private final String type;

    protected AbstractExtTaskDefinitionHandlerResolver(
        ExtTaskDefinitionHandlerRepository extTaskDefinitionHandlerRepository,
        String type
    ) {
        this.extTaskDefinitionHandlerRepository = extTaskDefinitionHandlerRepository;
        this.type = type;
    }

    @Override
    public TaskDefinitionHandler resolve(String name) {
        TaskDefinitionHandler taskDefinitionHandler = null;

        if (extTaskDefinitionHandlerRepository.existByNameAndType(name, type)) {
            taskDefinitionHandler = createTaskDefinitionHandler(name);
        }

        return taskDefinitionHandler;
    }

    @Override
    public List<TaskDefinitionHandler> getTaskDefinitionHandlers() {
        return extTaskDefinitionHandlerRepository
            .findAllNamesByType(type)
            .stream()
            .map(this::createTaskDefinitionHandler)
            .toList();
    }

    protected abstract TaskDefinitionHandler createTaskDefinitionHandler(String name);
}
