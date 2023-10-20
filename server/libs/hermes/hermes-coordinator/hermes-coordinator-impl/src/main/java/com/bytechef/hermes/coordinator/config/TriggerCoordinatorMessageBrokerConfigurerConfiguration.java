
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
import com.bytechef.hermes.coordinator.config.TriggerCoordinatorProperties.TriggerCoordinatorSubscriptions;
import com.bytechef.hermes.execution.message.broker.TriggerMessageRoute;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEnabled("coordinator")
public class TriggerCoordinatorMessageBrokerConfigurerConfiguration {

    @Bean
    MessageBrokerConfigurer<?> triggerCoordinatorMessageBrokerConfigurer(
        TriggerCoordinator triggerCoordinator, TriggerCoordinatorProperties triggerCoordinatorProperties) {
        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            TriggerCoordinatorSubscriptions subscriptions = triggerCoordinatorProperties.getSubscriptions();

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerMessageRoute.LISTENERS, subscriptions.getListeners(),
                triggerCoordinator, "handleListeners");

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerMessageRoute.POLLS, subscriptions.getPolls(),
                triggerCoordinator, "handlePolls");

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerMessageRoute.TRIGGERS_COMPLETE,
                subscriptions.getTriggersComplete(), triggerCoordinator, "handleTriggersComplete");

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerMessageRoute.WEBHOOKS, subscriptions.getWebhooks(),
                triggerCoordinator, "handleWebhooks");
        };
    }
}
