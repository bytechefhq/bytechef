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

package com.bytechef.hermes.descriptor.ext.resolver;

import com.bytechef.hermes.descriptor.ext.domain.DescriptorExtHandler;
import com.bytechef.hermes.descriptor.ext.service.DescriptorExtHandlerService;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandlerResolver;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractTaskDescriptorExtHandlerResolver implements TaskDescriptorHandlerResolver {

    private final DescriptorExtHandlerService descriptorExtHandlerService;
    private final String type;

    protected AbstractTaskDescriptorExtHandlerResolver(
            DescriptorExtHandlerService descriptorExtHandlerService, String type) {
        this.descriptorExtHandlerService = descriptorExtHandlerService;
        this.type = type;
    }

    @Override
    public List<TaskDescriptorHandler> getTaskDescriptorHandlers() {
        return descriptorExtHandlerService.getExtDescriptorHandlers(type).stream()
                .flatMap(extDescriptorHandler -> extDescriptorHandler.getVersions().stream()
                        .map(version -> createTaskDescriptorHandler(extDescriptorHandler, version)))
                .toList();
    }

    @Override
    public TaskDescriptorHandler resolve(String name, float version) {
        TaskDescriptorHandler taskDescriptorHandler = null;

        Optional<DescriptorExtHandler> extDescriptorHandlerOptional =
                descriptorExtHandlerService.fetchExtDescriptorHandler(name);

        if (extDescriptorHandlerOptional.isPresent()) {
            DescriptorExtHandler descriptorExtHandler = extDescriptorHandlerOptional.get();

            if (descriptorExtHandler.getVersions().contains(version)
                    && Objects.equals(descriptorExtHandler.getType(), type)) {
                taskDescriptorHandler = createTaskDescriptorHandler(descriptorExtHandler, version);
            }
        }

        return taskDescriptorHandler;
    }

    protected abstract TaskDescriptorHandler createTaskDescriptorHandler(
            DescriptorExtHandler descriptorExtHandler, double version);
}
