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

package com.bytechef.hermes.descriptor.service;

import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.repository.ExtTaskDescriptorHandlerRepository;
import com.bytechef.hermes.descriptor.resolver.TaskDescriptorHandlerResolverChain;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
public class TaskDescriptorHandlerServiceImpl implements TaskDescriptorHandlerService {

    private final ExtTaskDescriptorHandlerRepository extTaskDescriptorHandlerRepository;
    private final TaskDescriptorHandlerResolverChain taskDescriptorHandlerResolverChain;

    public TaskDescriptorHandlerServiceImpl(
            ExtTaskDescriptorHandlerRepository extTaskDescriptorHandlerRepository,
            TaskDescriptorHandlerResolverChain taskDescriptorHandlerResolverChain) {
        this.extTaskDescriptorHandlerRepository = extTaskDescriptorHandlerRepository;
        this.taskDescriptorHandlerResolverChain = taskDescriptorHandlerResolverChain;
    }

    @Override
    public TaskDescriptorHandler getTaskDescriptorHandler(String name, float version) {
        Assert.notNull(name, "name cannot be null");

        return taskDescriptorHandlerResolverChain.resolve(name, version);
    }

    @Override
    public List<TaskDescriptorHandler> getTaskDescriptorHandlers() {
        return taskDescriptorHandlerResolverChain.getTaskDescriptorHandlers();
    }

    @Override
    public void registerExtTaskDescriptorHandler(String name, float version, String type) {
        extTaskDescriptorHandlerRepository.create(name, version, type);
    }

    @Override
    public void unregisterExtTaskDescriptorHandler(String name, float version) {
        extTaskDescriptorHandlerRepository.delete(name, version);
    }
}
