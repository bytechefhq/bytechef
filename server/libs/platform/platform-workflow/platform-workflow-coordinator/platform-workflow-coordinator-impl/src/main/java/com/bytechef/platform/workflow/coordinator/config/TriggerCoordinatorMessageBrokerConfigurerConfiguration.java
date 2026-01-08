/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.workflow.coordinator.config;

import static com.bytechef.tenant.TenantContext.CURRENT_TENANT_ID;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Coordinator.Trigger.Subscriptions;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.event.MessageEventPostReceiveProcessor;
import com.bytechef.platform.workflow.coordinator.TriggerCoordinator;
import com.bytechef.platform.workflow.coordinator.event.ApplicationEvent;
import com.bytechef.platform.workflow.coordinator.event.ErrorEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionCompleteEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerPollEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent;
import com.bytechef.platform.workflow.coordinator.message.route.TriggerCoordinatorMessageRoute;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCoordinator
public class TriggerCoordinatorMessageBrokerConfigurerConfiguration {

    private final List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors;

    @SuppressFBWarnings("EI")
    public TriggerCoordinatorMessageBrokerConfigurerConfiguration(
        List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors) {

        this.messageEventPostReceiveProcessors = messageEventPostReceiveProcessors;
    }

    @Bean
    MessageBrokerConfigurer<?> triggerCoordinatorMessageBrokerConfigurer(
        TriggerCoordinator triggerCoordinator, ApplicationProperties applicationProperties) {

        TriggerCoordinatorDelegate triggerCoordinatorDelegate = new TriggerCoordinatorDelegate(
            messageEventPostReceiveProcessors, triggerCoordinator);

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            Subscriptions subscriptions = applicationProperties.getCoordinator()
                .getTrigger()
                .getSubscriptions();

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TriggerCoordinatorMessageRoute.APPLICATION_EVENTS,
                subscriptions.getApplicationEvents(), triggerCoordinatorDelegate, "onApplicationEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TriggerCoordinatorMessageRoute.ERROR_EVENTS,
                subscriptions.getTriggerExecutionErrorEvents(), triggerCoordinatorDelegate, "onErrorEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TriggerCoordinatorMessageRoute.TRIGGER_EXECUTION_COMPLETE_EVENTS,
                subscriptions.getTriggerExecutionCompleteEvents(), triggerCoordinatorDelegate,
                "onTriggerExecutionCompleteEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TriggerCoordinatorMessageRoute.TRIGGER_LISTENER_EVENTS,
                subscriptions.getTriggerListenerEvents(), triggerCoordinatorDelegate, "onTriggerListenerEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TriggerCoordinatorMessageRoute.TRIGGER_POLL_EVENTS,
                subscriptions.getTriggerPollEvents(), triggerCoordinatorDelegate, "onTriggerPollEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerCoordinatorMessageRoute.TRIGGER_WEBHOOK_EVENTS,
                subscriptions.getTriggerWebhookEvents(), triggerCoordinatorDelegate, "onTriggerWebhookEvent");
        };
    }

    private record TriggerCoordinatorDelegate(
        List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors,
        TriggerCoordinator triggerCoordinator) {

        public void onApplicationEvent(ApplicationEvent applicationEvent) {
            TenantContext.runWithTenantId(
                (String) applicationEvent.getMetadata(CURRENT_TENANT_ID),
                () -> triggerCoordinator.onApplicationEvent(applicationEvent));
        }

        public void onErrorEvent(ErrorEvent errorEvent) {
            TenantContext.runWithTenantId(
                (String) errorEvent.getMetadata(CURRENT_TENANT_ID),
                () -> triggerCoordinator.onErrorEvent((ErrorEvent) process(errorEvent)));
        }

        public void onTriggerExecutionCompleteEvent(TriggerExecutionCompleteEvent triggerExecutionCompleteEvent) {
            TenantContext.runWithTenantId(
                (String) triggerExecutionCompleteEvent.getMetadata(CURRENT_TENANT_ID),
                () -> triggerCoordinator.onTriggerExecutionCompleteEvent(
                    (TriggerExecutionCompleteEvent) process(triggerExecutionCompleteEvent)));
        }

        public void onTriggerListenerEvent(TriggerListenerEvent triggerListenerEvent) {
            TenantContext.runWithTenantId(
                (String) triggerListenerEvent.getMetadata(CURRENT_TENANT_ID),
                () -> triggerCoordinator.onTriggerListenerEvent((TriggerListenerEvent) process(triggerListenerEvent)));
        }

        public void onTriggerPollEvent(TriggerPollEvent triggerPollEvent) {
            TenantContext.runWithTenantId(
                (String) triggerPollEvent.getMetadata(CURRENT_TENANT_ID),
                () -> triggerCoordinator.onTriggerPollEvent((TriggerPollEvent) process(triggerPollEvent)));
        }

        public void onTriggerWebhookEvent(TriggerWebhookEvent triggerWebhookEvent) {
            TenantContext.runWithTenantId(
                (String) triggerWebhookEvent.getMetadata(CURRENT_TENANT_ID),
                () -> triggerCoordinator.onTriggerWebhookEvent((TriggerWebhookEvent) process(triggerWebhookEvent)));
        }

        private MessageEvent<?> process(MessageEvent<?> messageEvent) {
            for (MessageEventPostReceiveProcessor messageEventPostReceiveProcessor : messageEventPostReceiveProcessors) {
                messageEvent = messageEventPostReceiveProcessor.process(messageEvent);
            }

            return messageEvent;
        }
    }
}
