
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

package com.bytechef.event.listener.config;

import com.bytechef.event.listener.EventListener;
import com.bytechef.message.broker.Queues;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class EventListenerMessageBrokerConfiguration {

    private final ApplicationContext applicationContext;
    private final EventListenerProperties eventListenerProperties;

    @SuppressFBWarnings("EI")
    public EventListenerMessageBrokerConfiguration(
        ApplicationContext applicationContext, EventListenerProperties eventListenerProperties) {

        this.applicationContext = applicationContext;
        this.eventListenerProperties = eventListenerProperties;
    }

    @Bean
    MessageBrokerConfigurer<?> eventListenerMessageBrokerConfigurer() {
        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            EventListener eventListener = applicationContext.getBean(EventListener.class);

            EventListenerProperties.EventListenerSubscriptions subscriptions =
                eventListenerProperties.getSubscriptions();

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                Queues.EVENTS,
                subscriptions.getEvents(),
                eventListener,
                "onApplicationEvent");
        };
    }
}
