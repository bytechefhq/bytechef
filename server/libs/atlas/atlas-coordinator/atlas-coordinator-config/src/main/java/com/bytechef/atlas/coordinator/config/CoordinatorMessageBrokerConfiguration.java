
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

package com.bytechef.atlas.coordinator.config;

import com.bytechef.atlas.coordinator.Coordinator;
import com.bytechef.atlas.coordinator.event.EventListener;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.autoconfigure.annotation.ConditionalOnCoordinator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCoordinator
public class CoordinatorMessageBrokerConfiguration implements ApplicationContextAware {

    private final CoordinatorProperties coordinatorProperties;

    private ApplicationContext applicationContext;

    @SuppressFBWarnings("EI2")
    public CoordinatorMessageBrokerConfiguration(CoordinatorProperties coordinatorProperties) {
        this.coordinatorProperties = coordinatorProperties;
    }

    @Bean
    MessageBrokerConfigurer<?> coordinatorMessageBrokerConfigurer() {
        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            Coordinator coordinator = applicationContext.getBean(Coordinator.class);

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                Queues.COMPLETIONS,
                coordinatorProperties.getSubscriptions()
                    .getCompletions(),
                coordinator,
                "complete");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                Queues.ERRORS,
                coordinatorProperties.getSubscriptions()
                    .getErrors(),
                coordinator,
                "handleError");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                Queues.EVENTS,
                coordinatorProperties.getSubscriptions()
                    .getEvents(),
                applicationContext.getBean(EventListener.class),
                "onApplicationEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                Queues.JOBS,
                coordinatorProperties.getSubscriptions()
                    .getJobs(),
                coordinator,
                "start");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                Queues.RESTARTS,
                coordinatorProperties.getSubscriptions()
                    .getRequests(),
                coordinator,
                "resume");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                Queues.STOPS,
                coordinatorProperties.getSubscriptions()
                    .getRequests(),
                coordinator,
                "stop");
        };
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
