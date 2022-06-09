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

package com.integri.atlas.task.descriptor.service;

import com.integri.atlas.task.descriptor.handler.TaskDescriptorHandler;
import com.integri.atlas.task.descriptor.handler.TaskDescriptorHandlerResolver;
import com.integri.atlas.task.descriptor.repository.ExtTaskDescriptorHandlerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
public class TaskDescriptorHandlerService {

    private final ExtTaskDescriptorHandlerRepository extTaskDescriptorHandlerRepository;
    private final TaskDescriptorHandlerResolver taskDescriptorHandlerResolver;

    public TaskDescriptorHandlerService(
        ExtTaskDescriptorHandlerRepository extTaskDescriptorHandlerRepository,
        TaskDescriptorHandlerResolver taskDescriptorHandlerResolver
    ) {
        this.extTaskDescriptorHandlerRepository = extTaskDescriptorHandlerRepository;
        this.taskDescriptorHandlerResolver = taskDescriptorHandlerResolver;
    }

    public TaskDescriptorHandler getTaskDescriptorHandler(String name) {
        Assert.notNull(name, "Name cannot be null");

        return taskDescriptorHandlerResolver.resolve(name);
    }

    public List<TaskDescriptorHandler> getTaskDescriptorHandlers() {
        return taskDescriptorHandlerResolver.getTaskDescriptorHandlers();
    }

    public void registerExtTaskDescriptorHandler(String name, String type) {
        if (extTaskDescriptorHandlerRepository.findTypeByName(name) == null) {
            extTaskDescriptorHandlerRepository.create(name, type);
        } else {
            extTaskDescriptorHandlerRepository.update(name, type);
        }
    }

    public void unregisterExtTaskDescriptorHandler(String name) {
        extTaskDescriptorHandlerRepository.delete(name);
    }
}
