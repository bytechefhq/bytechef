
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

package com.bytechef.hermes.worker.config;

import com.bytechef.hermes.worker.TriggerWorker;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.hermes.workflow.message.broker.TriggerMessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration

public class TriggerWorkerMessageBrokerConfiguration {

    private final ApplicationContext applicationContext;

    @SuppressFBWarnings("EI")
    public TriggerWorkerMessageBrokerConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    MessageBrokerConfigurer<?> triggerWorkerMessageBrokerConfigurer() {
        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            TriggerWorker triggerWorker = applicationContext.getBean(TriggerWorker.class);

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerMessageRoute.TRIGGERS, 1, triggerWorker, "handle");

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, SystemMessageRoute.CONTROL, 1, triggerWorker, "handle");
        };
    }
}
