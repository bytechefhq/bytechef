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

package com.integri.atlas.task.definition.service;

import com.integri.atlas.task.definition.handler.TaskDefinitionHandler;
import com.integri.atlas.task.definition.handler.TaskDefinitionHandlerResolver;
import com.integri.atlas.task.definition.repository.ExtTaskDefinitionHandlerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
public class TaskDefinitionHandlerService {

    private final ExtTaskDefinitionHandlerRepository extTaskDefinitionHandlerRepository;
    private final TaskDefinitionHandlerResolver taskDefinitionHandlerResolver;

    public TaskDefinitionHandlerService(
        ExtTaskDefinitionHandlerRepository extTaskDefinitionHandlerRepository,
        TaskDefinitionHandlerResolver taskDefinitionHandlerResolver
    ) {
        this.extTaskDefinitionHandlerRepository = extTaskDefinitionHandlerRepository;
        this.taskDefinitionHandlerResolver = taskDefinitionHandlerResolver;
    }

    public TaskDefinitionHandler getTaskDefinitionHandler(String name) {
        Assert.notNull(name, "Name cannot be null");

        return taskDefinitionHandlerResolver.resolve(name);
    }

    public List<TaskDefinitionHandler> getTaskDefinitionHandlers() {
        return taskDefinitionHandlerResolver.getTaskDefinitionHandlers();
    }

    public void registerExtTaskDefinitionHandler(String name, String type) {
        if (extTaskDefinitionHandlerRepository.findTypeByName(name) == null) {
            extTaskDefinitionHandlerRepository.create(name, type);
        } else {
            extTaskDefinitionHandlerRepository.update(name, type);
        }
    }

    public void unregisterExtTaskDefinitionHandler(String name) {
        extTaskDefinitionHandlerRepository.delete(name);
    }
}
