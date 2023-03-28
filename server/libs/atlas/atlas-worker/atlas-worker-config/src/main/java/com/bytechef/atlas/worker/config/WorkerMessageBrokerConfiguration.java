
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

import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.autoconfigure.annotation.ConditionalOnWorker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnWorker
public class WorkerMessageBrokerConfiguration {

    private final ApplicationContext applicationContext;
    private final WorkerProperties workerProperties;

    @SuppressFBWarnings("EI2")
    public WorkerMessageBrokerConfiguration(
        ApplicationContext applicationContext, WorkerProperties workerProperties) {

        this.applicationContext = applicationContext;
        this.workerProperties = workerProperties;
    }

    @Bean
    MessageBrokerConfigurer<?> workerMessageBrokerConfigurer() {
        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            Worker worker = applicationContext.getBean(Worker.class);

            Map<String, Object> subscriptions = workerProperties.getSubscriptions();

            subscriptions.forEach((k, v) -> messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, k, Integer.parseInt((String) v), worker, "handle"));

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, Queues.CONTROL, 1, worker, "handle");
        };
    }
}
