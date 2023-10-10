
/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.hermes.coordinator.TriggerCoordinator;
import com.bytechef.hermes.coordinator.config.TriggerCoordinatorProperties.TriggerCoordinatorSubscriptions;
import com.bytechef.hermes.coordinator.message.route.CoordinatorMessageRoute;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("'${bytechef.coordinator.enabled:true}' == 'true'")
public class TriggerCoordinatorMessageBrokerConfigurerConfiguration {

    @Bean
    MessageBrokerConfigurer<?> triggerCoordinatorMessageBrokerConfigurer(
        TriggerCoordinator triggerCoordinator, TriggerCoordinatorProperties triggerCoordinatorProperties) {

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            TriggerCoordinatorSubscriptions subscriptions = triggerCoordinatorProperties.getSubscriptions();

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                CoordinatorMessageRoute.APPLICATION_EVENTS,
                subscriptions.getApplicationEvents(), triggerCoordinator, "onApplicationEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                CoordinatorMessageRoute.ERROR_EVENTS,
                subscriptions.getTriggerExecutionErrorEvents(), triggerCoordinator, "onErrorEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                CoordinatorMessageRoute.TRIGGER_EXECUTION_COMPLETE_EVENTS,
                subscriptions.getTriggerExecutionCompleteEvents(), triggerCoordinator,
                "onTriggerExecutionCompleteEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                CoordinatorMessageRoute.TRIGGER_LISTENER_EVENTS,
                subscriptions.getTriggerListenerEvents(), triggerCoordinator, "onTriggerListenerEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                CoordinatorMessageRoute.TRIGGER_POLL_EVENTS,
                subscriptions.getTriggerPollEvents(), triggerCoordinator, "onTriggerPollEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, CoordinatorMessageRoute.TRIGGER_WEBHOOK_EVENTS,
                subscriptions.getTriggerWebhookEvents(), triggerCoordinator, "onTriggerWebhookEvent");
        };
    }
}
