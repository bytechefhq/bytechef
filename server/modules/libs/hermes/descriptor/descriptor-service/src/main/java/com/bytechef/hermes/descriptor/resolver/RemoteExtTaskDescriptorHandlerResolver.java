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

import com.bytechef.hermes.descriptor.domain.TaskDescriptor;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.repository.ExtTaskDescriptorHandlerRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(2)
public class RemoteExtTaskDescriptorHandlerResolver extends AbstractTaskDescriptorHandlerResolver {

    public static final String REMOTE = "REMOTE";

    public RemoteExtTaskDescriptorHandlerResolver(
            ExtTaskDescriptorHandlerRepository extTaskDescriptorHandlerRepository) {
        super(extTaskDescriptorHandlerRepository, REMOTE);
    }

    @Override
    protected TaskDescriptorHandler createTaskDescriptorHandler(String name, float version) {
        return new RemoteTaskDescriptorHandlerProxy(name, version);
    }

    private static class RemoteTaskDescriptorHandlerProxy implements TaskDescriptorHandler {

        private final String name;
        private final float version;

        private RemoteTaskDescriptorHandlerProxy(String name, float version) {
            this.name = name;
            this.version = version;
        }

        @Override
        public TaskDescriptor getTaskDescriptor() {
            throw new UnsupportedOperationException();
        }
    }
}
