
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

import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.config.TaskCoordinatorProperties.TaskCoordinatorSubscriptions;
import com.bytechef.atlas.message.broker.TaskMessageRoute;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
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
public class TaskCoordinatorMessageBrokerConfiguration {

    private final ApplicationContext applicationContext;
    private final TaskCoordinatorProperties taskCoordinatorProperties;

    @SuppressFBWarnings("EI2")
    public TaskCoordinatorMessageBrokerConfiguration(
        ApplicationContext applicationContext, TaskCoordinatorProperties taskCoordinatorProperties) {

        this.applicationContext = applicationContext;
        this.taskCoordinatorProperties = taskCoordinatorProperties;
    }

    @Bean
    MessageBrokerConfigurer<?> taskCoordinatorMessageBrokerConfigurer() {
        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            TaskCoordinator coordinator = applicationContext.getBean(TaskCoordinator.class);

            TaskCoordinatorSubscriptions subscriptions = taskCoordinatorProperties.getSubscriptions();

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskMessageRoute.TASKS_COMPLETIONS,
                subscriptions.getCompletions(),
                coordinator,
                "complete");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskMessageRoute.TASKS_JOBS,
                subscriptions.getJobs(),
                coordinator,
                "start");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskMessageRoute.TASKS_RESTARTS,
                subscriptions.getJobs(),
                coordinator,
                "resume");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskMessageRoute.TASKS_SUBFLOWS,
                subscriptions.getJobs(),
                coordinator,
                "create");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                TaskMessageRoute.TASKS_STOPS,
                subscriptions.getJobs(),
                coordinator,
                "stop");
        };
    }
}
