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
import com.integri.atlas.task.descriptor.repository.ExtTaskAuthDescriptorHandlerRepository;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractExtTaskAuthDescriptorHandlerResolver implements TaskAuthDescriptorHandlerResolver {

    private final ExtTaskAuthDescriptorHandlerRepository extTaskAuthDescriptorHandlerRepository;
    private final String type;

    protected AbstractExtTaskAuthDescriptorHandlerResolver(
        ExtTaskAuthDescriptorHandlerRepository extTaskAuthDescriptorHandlerRepository,
        String type
    ) {
        this.extTaskAuthDescriptorHandlerRepository = extTaskAuthDescriptorHandlerRepository;
        this.type = type;
    }

    @Override
    public TaskAuthDescriptorHandler resolve(String taskName) {
        TaskAuthDescriptorHandler taskAuthDescriptorHandler = null;

        if (extTaskAuthDescriptorHandlerRepository.existByTaskNameAndType(taskName, type)) {
            taskAuthDescriptorHandler = createTaskAuthDescriptorHandler(taskName);
        }

        return taskAuthDescriptorHandler;
    }

    @Override
    public List<TaskAuthDescriptorHandler> getTaskAuthDescriptorHandlers() {
        return extTaskAuthDescriptorHandlerRepository
            .findAllTaskNamesByType(type)
            .stream()
            .map(this::createTaskAuthDescriptorHandler)
            .toList();
    }

    protected abstract TaskAuthDescriptorHandler createTaskAuthDescriptorHandler(String name);
}
