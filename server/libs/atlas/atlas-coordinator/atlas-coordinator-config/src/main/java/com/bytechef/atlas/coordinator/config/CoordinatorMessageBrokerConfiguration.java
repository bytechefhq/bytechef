
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
import com.bytechef.atlas.message.broker.TaskQueues;
import com.bytechef.atlas.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.autoconfigure.annotation.ConditionalOnCoordinator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCoordinator
public class CoordinatorMessageBrokerConfiguration {

    private final ApplicationContext applicationContext;
    private final CoordinatorProperties coordinatorProperties;

    @SuppressFBWarnings("EI2")
    public CoordinatorMessageBrokerConfiguration(
        ApplicationContext applicationContext, CoordinatorProperties coordinatorProperties) {

        this.applicationContext = applicationContext;
        this.coordinatorProperties = coordinatorProperties;
    }

    @Bean
    MessageBrokerConfigurer<?> coordinatorMessageBrokerConfigurer() {
        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            Coordinator coordinator = applicationContext.getBean(Coordinator.class);

            CoordinatorProperties.CoordinatorSubscriptions coordinatorSubscriptions = coordinatorProperties
                .getSubscriptions();

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskQueues.COMPLETIONS,
                coordinatorSubscriptions.getCompletions(),
                coordinator,
                "complete");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskQueues.ERRORS,
                coordinatorSubscriptions.getErrors(),
                coordinator,
                "handleError");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskQueues.EVENTS,
                coordinatorSubscriptions.getEvents(),
                applicationContext.getBean(EventListener.class),
                "onApplicationEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskQueues.JOBS,
                coordinatorSubscriptions.getJobs(),
                coordinator,
                "start");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskQueues.RESTARTS,
                coordinatorSubscriptions.getRequests(),
                coordinator,
                "resume");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskQueues.SUBFLOWS,
                coordinatorSubscriptions.getJobs(),
                coordinator,
                "create");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskQueues.STOPS,
                coordinatorSubscriptions.getRequests(),
                coordinator,
                "stop");
        };
    }
}
