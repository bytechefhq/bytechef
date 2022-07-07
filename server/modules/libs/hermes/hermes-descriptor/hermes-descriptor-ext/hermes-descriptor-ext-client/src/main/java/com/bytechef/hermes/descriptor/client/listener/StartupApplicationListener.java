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

package com.bytechef.hermes.descriptor.client.listener;

import com.bytechef.hermes.descriptor.ext.domain.DescriptorExtHandler;
import com.bytechef.hermes.descriptor.ext.service.DescriptorExtHandlerService;
import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandlerResolver;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandlerResolver;
import com.bytechef.hermes.descriptor.model.TaskDescriptor;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class StartupApplicationListener implements ApplicationListener<ContextRefreshedEvent> {
    private final AuthenticationDescriptorHandlerResolver authenticationDescriptorHandlerResolver;
    private final DescriptorExtHandlerService extDescriptorHandlerClient;
    private final int port;
    private final TaskDescriptorHandlerResolver taskDescriptorHandlerResolver;

    public StartupApplicationListener(
            AuthenticationDescriptorHandlerResolver authenticationDescriptorHandlerResolver,
            DescriptorExtHandlerService descriptorExtHandlerService,
            @Value("${server.port}") int port,
            TaskDescriptorHandlerResolver taskDescriptorHandlerResolver) {
        this.authenticationDescriptorHandlerResolver = authenticationDescriptorHandlerResolver;
        this.extDescriptorHandlerClient = descriptorExtHandlerService;
        this.port = port;
        this.taskDescriptorHandlerResolver = taskDescriptorHandlerResolver;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Map<String, DescriptorExtHandler> extDescriptorHandlerMap = new HashMap<>();

        for (TaskDescriptorHandler taskDescriptorHandler : taskDescriptorHandlerResolver.getTaskDescriptorHandlers()) {
            TaskDescriptor taskDescriptor = taskDescriptorHandler.getTaskDescriptor();

            extDescriptorHandlerMap.computeIfAbsent(
                    taskDescriptor.getName(),
                    key -> new DescriptorExtHandler(
                            taskDescriptor.getName(),
                            (double) taskDescriptor.getVersion(),
                            authenticationDescriptorHandlerResolver.resolve(taskDescriptor.getName()) != null,
                            "REMOTE",
                            Map.of(
                                    "hostAddress",
                                    InetAddress.getLoopbackAddress().getHostAddress(),
                                    "port",
                                    port)));

            extDescriptorHandlerMap.computeIfPresent(taskDescriptor.getName(), (key, oldExtDescriptorHandler) -> {
                oldExtDescriptorHandler.addVersion((double) taskDescriptor.getVersion());

                return oldExtDescriptorHandler;
            });
        }

        extDescriptorHandlerClient.save(new ArrayList<>(extDescriptorHandlerMap.values()));
    }
}
