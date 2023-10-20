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
import com.integri.atlas.task.descriptor.model.TaskAuthDescriptors;
import com.integri.atlas.task.descriptor.repository.ExtTaskAuthDescriptorHandlerRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(2)
public class RemoteExtTaskAuthDescriptorHandlerResolver extends AbstractExtTaskAuthDescriptorHandlerResolver {

    public static final String REMOTE = "REMOTE";

    public RemoteExtTaskAuthDescriptorHandlerResolver(
        ExtTaskAuthDescriptorHandlerRepository extTaskAuthDescriptorHandlerRepository
    ) {
        super(extTaskAuthDescriptorHandlerRepository, REMOTE);
    }

    @Override
    protected TaskAuthDescriptorHandler createTaskAuthDescriptorHandler(String name) {
        return new RemoteExtTaskAuthDescriptorHandlerProxy(name);
    }

    private static class RemoteExtTaskAuthDescriptorHandlerProxy implements TaskAuthDescriptorHandler {

        private final String name;

        private RemoteExtTaskAuthDescriptorHandlerProxy(String name) {
            this.name = name;
        }

        @Override
        public TaskAuthDescriptors getTaskAuthDescriptors() {
            throw new UnsupportedOperationException();
        }
    }
}
