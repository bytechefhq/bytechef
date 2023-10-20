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
import com.bytechef.hermes.descriptor.model.TaskDescriptor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author Ivica Cardic
 */
@Component
@Order(2)
public class RemoteExtTaskDescriptorExtHandlerResolver extends AbstractTaskDescriptorExtHandlerResolver {

    public static final String REMOTE = "REMOTE";

    public RemoteExtTaskDescriptorExtHandlerResolver(DescriptorExtHandlerService descriptorExtHandlerService) {
        super(descriptorExtHandlerService, REMOTE);
    }

    @Override
    protected TaskDescriptorHandler createTaskDescriptorHandler(
            DescriptorExtHandler descriptorExtHandler, double version) {
        return new RemoteTaskDescriptorHandlerProxy(
                descriptorExtHandler.getName(),
                version,
                descriptorExtHandler.getProperty("hostAddress"),
                descriptorExtHandler.getProperty("port"));
    }

    private static class RemoteTaskDescriptorHandlerProxy implements TaskDescriptorHandler {

        private static final RestTemplate restTemplate = new RestTemplate();

        private final String url;

        private RemoteTaskDescriptorHandlerProxy(String name, double version, String hostAddress, int port) {
            this.url = "http://%s:%s/task-descriptors/%s/%s".formatted(hostAddress, port, name, version);
        }

        @Override
        public TaskDescriptor getTaskDescriptor() {
            return restTemplate.getForObject(url, TaskDescriptor.class);
        }
    }
}
