
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

package com.bytechef.atlas.coordinator.config;

import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.config.TaskCoordinatorProperties.TaskCoordinatorSubscriptions;
import com.bytechef.atlas.coordinator.message.route.CoordinatorMessageRoute;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("'${bytechef.coordinator.enabled:true}' == 'true'")
public class TaskCoordinatorMessageBrokerConfigurerConfiguration {

    @Bean
    MessageBrokerConfigurer<?> taskCoordinatorMessageBrokerConfigurer(
        TaskCoordinator taskCoordinator, TaskCoordinatorProperties taskCoordinatorProperties) {

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            TaskCoordinatorSubscriptions subscriptions = taskCoordinatorProperties.getSubscriptions();

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, CoordinatorMessageRoute.APPLICATION_EVENTS,
                subscriptions.getApplicationEvents(), taskCoordinator, "onApplicationEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, CoordinatorMessageRoute.ERROR_EVENTS,
                subscriptions.getTaskExecutionErrorEvents(), taskCoordinator, "onErrorEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, CoordinatorMessageRoute.JOB_RESUME_EVENTS,
                subscriptions.getJobResumeEvents(),
                taskCoordinator, "onJobResumeEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, CoordinatorMessageRoute.JOB_START_EVENTS, subscriptions.getJobStartEvents(),
                taskCoordinator, "onJobStartEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, CoordinatorMessageRoute.JOB_STOP_EVENTS, subscriptions.getJobStopEvents(),
                taskCoordinator, "onJobStopEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, CoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
                subscriptions.getTaskExecutionCompleteEvents(), taskCoordinator, "onTaskExecutionCompleteEvent");
        };
    }
}
