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

import com.integri.atlas.task.descriptor.handler.TaskAuthDescriptorHandler;
import com.integri.atlas.task.descriptor.handler.TaskAuthDescriptorHandlerResolver;
import com.integri.atlas.task.descriptor.repository.ExtTaskAuthDescriptorHandlerRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class TaskAuthDescriptorHandlerService {

    private final ExtTaskAuthDescriptorHandlerRepository extTaskAuthDescriptorHandlerRepository;
    private final TaskAuthDescriptorHandlerResolver taskAuthDescriptorHandlerResolver;

    public TaskAuthDescriptorHandlerService(
        ExtTaskAuthDescriptorHandlerRepository extTaskAuthDescriptorHandlerRepository,
        TaskAuthDescriptorHandlerResolver taskAuthDescriptorHandlerResolver
    ) {
        this.extTaskAuthDescriptorHandlerRepository = extTaskAuthDescriptorHandlerRepository;
        this.taskAuthDescriptorHandlerResolver = taskAuthDescriptorHandlerResolver;
    }

    public TaskAuthDescriptorHandler getTaskAuthDescriptorHandler(String name) {
        return taskAuthDescriptorHandlerResolver.resolve(name);
    }

    public List<TaskAuthDescriptorHandler> getTaskAuthDescriptorHandlers() {
        return taskAuthDescriptorHandlerResolver.getTaskAuthDescriptorHandlers();
    }

    public void registerExtTaskAuthDescriptorHandler(String name, String type) {
        extTaskAuthDescriptorHandlerRepository.create(name, type);
    }

    public void unregisterExtTaskAuthDescriptorHandler(String name) {
        extTaskAuthDescriptorHandlerRepository.delete(name);
    }
}
