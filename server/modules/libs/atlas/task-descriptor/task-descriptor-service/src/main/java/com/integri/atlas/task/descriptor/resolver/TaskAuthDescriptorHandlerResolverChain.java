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
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Primary
public class TaskAuthDescriptorHandlerResolverChain implements TaskAuthDescriptorHandlerResolver {

    private final List<TaskAuthDescriptorHandlerResolver> taskAuthDescriptorHandlerResolvers;

    public TaskAuthDescriptorHandlerResolverChain(
        List<TaskAuthDescriptorHandlerResolver> taskAuthDescriptorHandlerResolvers
    ) {
        this.taskAuthDescriptorHandlerResolvers = taskAuthDescriptorHandlerResolvers;
    }

    @Override
    public TaskAuthDescriptorHandler resolve(String name) {
        TaskAuthDescriptorHandler taskAuthDescriptorHandler = null;

        for (TaskAuthDescriptorHandlerResolver taskAuthDescriptorHandlerResolver : taskAuthDescriptorHandlerResolvers) {
            taskAuthDescriptorHandler = taskAuthDescriptorHandlerResolver.resolve(name);

            if (taskAuthDescriptorHandler != null) {
                break;
            }
        }

        return taskAuthDescriptorHandler;
    }

    @Override
    public List<TaskAuthDescriptorHandler> getTaskAuthDescriptorHandlers() {
        return taskAuthDescriptorHandlerResolvers
            .stream()
            .flatMap(taskAuthDescriptorHandlerResolver ->
                taskAuthDescriptorHandlerResolver.getTaskAuthDescriptorHandlers().stream()
            )
            .toList();
    }
}
