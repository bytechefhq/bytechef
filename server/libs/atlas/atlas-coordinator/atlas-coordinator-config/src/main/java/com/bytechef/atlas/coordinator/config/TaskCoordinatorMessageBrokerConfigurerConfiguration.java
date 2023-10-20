
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
import com.bytechef.atlas.execution.message.broker.TaskMessageRoute;
import com.bytechef.autoconfigure.annotation.ConditionalOnEnabled;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEnabled("coordinator")
public class TaskCoordinatorMessageBrokerConfigurerConfiguration {

    @Bean
    MessageBrokerConfigurer<?> taskCoordinatorMessageBrokerConfigurer(
        TaskCoordinator taskCoordinator, TaskCoordinatorProperties taskCoordinatorProperties) {

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            TaskCoordinatorSubscriptions subscriptions = taskCoordinatorProperties.getSubscriptions();

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskMessageRoute.TASKS_COMPLETE, subscriptions.getTasksComplete(),
                taskCoordinator, "handleTasksComplete");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskMessageRoute.JOBS_START, subscriptions.getJobsStart(), taskCoordinator,
                "handleJobsStart");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskMessageRoute.JOBS_RESUME, subscriptions.getJobsResume(),
                taskCoordinator, "handleJobsResume");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskMessageRoute.JOBS_STOP, subscriptions.getJobsStop(),
                taskCoordinator, "handleJobsStop");
        };
    }
}
