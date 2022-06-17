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
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Primary
public class TaskDescriptorHandlerResolverChain implements TaskDescriptorHandlerResolver {

    private final List<TaskDescriptorHandlerResolver> taskDescriptorHandlerResolvers;

    public TaskDescriptorHandlerResolverChain(List<TaskDescriptorHandlerResolver> taskDescriptorHandlerResolvers) {
        this.taskDescriptorHandlerResolvers = taskDescriptorHandlerResolvers;
    }

    @Override
    public List<TaskDescriptorHandler> getTaskDescriptorHandlers() {
        return taskDescriptorHandlerResolvers.stream()
                .flatMap(taskDescriptorHandlerResolver ->
                        taskDescriptorHandlerResolver.getTaskDescriptorHandlers().stream())
                .toList();
    }

    @Override
    public TaskDescriptorHandler resolve(String name, float version) {
        TaskDescriptorHandler taskDescriptorHandler = null;

        for (TaskDescriptorHandlerResolver taskDescriptorHandlerResolver : taskDescriptorHandlerResolvers) {
            taskDescriptorHandler = taskDescriptorHandlerResolver.resolve(name, version);

            if (taskDescriptorHandler != null) {
                break;
            }
        }

        return taskDescriptorHandler;
    }
}
