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

import com.integri.atlas.task.descriptor.handler.TaskDescriptorHandler;
import com.integri.atlas.task.descriptor.handler.TaskDescriptorHandlerResolver;
import com.integri.atlas.task.descriptor.repository.ExtTaskDescriptorHandlerRepository;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractExtTaskDescriptorHandlerResolver implements TaskDescriptorHandlerResolver {

    private final ExtTaskDescriptorHandlerRepository extTaskDescriptorHandlerRepository;
    private final String type;

    protected AbstractExtTaskDescriptorHandlerResolver(
        ExtTaskDescriptorHandlerRepository extTaskDescriptorHandlerRepository,
        String type
    ) {
        this.extTaskDescriptorHandlerRepository = extTaskDescriptorHandlerRepository;
        this.type = type;
    }

    @Override
    public TaskDescriptorHandler resolve(String name, float version) {
        TaskDescriptorHandler taskDescriptorHandler = null;

        if (extTaskDescriptorHandlerRepository.existByNameAndVersionAndType(name, version, type)) {
            taskDescriptorHandler = createTaskDescriptorHandler(name, version);
        }

        return taskDescriptorHandler;
    }

    @Override
    public List<TaskDescriptorHandler> getTaskDescriptorHandlers() {
        return extTaskDescriptorHandlerRepository
            .findAllNamesByType(type)
            .stream()
            .flatMap(nameVersions ->
                nameVersions
                    .versions()
                    .stream()
                    .map(version -> createTaskDescriptorHandler(nameVersions.name(), version))
            )
            .toList();
    }

    protected abstract TaskDescriptorHandler createTaskDescriptorHandler(String name, float version);
}
