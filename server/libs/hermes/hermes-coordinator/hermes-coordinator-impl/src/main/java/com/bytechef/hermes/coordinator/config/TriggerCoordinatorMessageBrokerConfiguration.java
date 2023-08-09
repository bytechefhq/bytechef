
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

package com.bytechef.hermes.coordinator.config;

import com.bytechef.autoconfigure.annotation.ConditionalOnEnabled;
import com.bytechef.hermes.coordinator.TriggerCoordinator;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.hermes.execution.message.broker.TriggerMessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEnabled("coordinator")
public class TriggerCoordinatorMessageBrokerConfiguration {

    private final ApplicationContext applicationContext;

    @SuppressFBWarnings("EI")
    public TriggerCoordinatorMessageBrokerConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    MessageBrokerConfigurer<?> triggerCoordinatorMessageBrokerConfigurer() {
        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            TriggerCoordinator triggerCoordinator = applicationContext.getBean(TriggerCoordinator.class);

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerMessageRoute.TRIGGERS_COMPLETIONS, 1, triggerCoordinator,
                "complete");

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerMessageRoute.TRIGGERS_POLLS, 1, triggerCoordinator,
                "poll");
        };
    }
}
