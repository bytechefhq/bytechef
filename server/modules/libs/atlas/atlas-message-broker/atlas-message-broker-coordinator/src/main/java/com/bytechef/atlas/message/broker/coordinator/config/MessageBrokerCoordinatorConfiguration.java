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

package com.bytechef.atlas.message.broker.coordinator.config;

import com.bytechef.atlas.config.AtlasProperties;
import com.bytechef.atlas.config.CoordinatorProperties;
import com.bytechef.atlas.coordinator.Coordinator;
import com.bytechef.atlas.event.EventListener;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.config.MessageBrokerConfigurer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class MessageBrokerCoordinatorConfiguration implements ApplicationContextAware {

    @Autowired
    private AtlasProperties atlasProperties;

    private ApplicationContext applicationContext;

    @Bean
    MessageBrokerConfigurer coordinatorMessageBrokerConfigurator() {
        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            CoordinatorProperties coordinatorProperties = atlasProperties.getCoordinator();

            if (coordinatorProperties.isEnabled()) {
                Coordinator coordinator = applicationContext.getBean(Coordinator.class);

                messageBrokerListenerRegistrar.registerListenerEndpoint(
                        listenerEndpointRegistrar,
                        Queues.COMPLETIONS,
                        coordinatorProperties.getSubscriptions().getCompletions(),
                        coordinator,
                        "complete");
                messageBrokerListenerRegistrar.registerListenerEndpoint(
                        listenerEndpointRegistrar,
                        Queues.ERRORS,
                        coordinatorProperties.getSubscriptions().getErrors(),
                        coordinator,
                        "handleError");
                messageBrokerListenerRegistrar.registerListenerEndpoint(
                        listenerEndpointRegistrar,
                        Queues.EVENTS,
                        coordinatorProperties.getSubscriptions().getEvents(),
                        applicationContext.getBean(EventListener.class),
                        "onApplicationEvent");
                messageBrokerListenerRegistrar.registerListenerEndpoint(
                        listenerEndpointRegistrar,
                        Queues.JOBS,
                        coordinatorProperties.getSubscriptions().getJobs(),
                        coordinator,
                        "start");
                messageBrokerListenerRegistrar.registerListenerEndpoint(
                        listenerEndpointRegistrar,
                        Queues.RESTARTS,
                        coordinatorProperties.getSubscriptions().getRequests(),
                        coordinator,
                        "resume");
                messageBrokerListenerRegistrar.registerListenerEndpoint(
                        listenerEndpointRegistrar,
                        Queues.REQUESTS,
                        coordinatorProperties.getSubscriptions().getRequests(),
                        coordinator,
                        "create");
                messageBrokerListenerRegistrar.registerListenerEndpoint(
                        listenerEndpointRegistrar,
                        Queues.STOPS,
                        coordinatorProperties.getSubscriptions().getRequests(),
                        coordinator,
                        "stop");
                messageBrokerListenerRegistrar.registerListenerEndpoint(
                        listenerEndpointRegistrar,
                        Queues.SUBFLOWS,
                        coordinatorProperties.getSubscriptions().getSubflows(),
                        coordinator,
                        "create");
            }
        };
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
