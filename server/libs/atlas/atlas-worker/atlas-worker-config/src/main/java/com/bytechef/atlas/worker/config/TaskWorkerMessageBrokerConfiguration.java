
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

package com.bytechef.atlas.worker.config;

import com.bytechef.atlas.message.broker.TaskMessageRoute;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration

public class TaskWorkerMessageBrokerConfiguration {

    private final ApplicationContext applicationContext;
    private final TaskWorkerProperties taskWorkerProperties;

    @SuppressFBWarnings("EI")
    public TaskWorkerMessageBrokerConfiguration(
        ApplicationContext applicationContext, TaskWorkerProperties taskWorkerProperties) {

        this.applicationContext = applicationContext;
        this.taskWorkerProperties = taskWorkerProperties;
    }

    @Bean
    MessageBrokerConfigurer<?> taskWorkerMessageBrokerConfigurer() {
        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            TaskWorker taskWorker = applicationContext.getBean(TaskWorker.class);

            Map<String, Object> subscriptions = taskWorkerProperties.getSubscriptions();

            subscriptions.forEach((routeName, concurrency) -> messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskMessageRoute.ofRoute(routeName), Integer.parseInt((String) concurrency),
                taskWorker, "handle"));

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, SystemMessageRoute.CONTROL, 1, taskWorker, "handle");
        };
    }
}
