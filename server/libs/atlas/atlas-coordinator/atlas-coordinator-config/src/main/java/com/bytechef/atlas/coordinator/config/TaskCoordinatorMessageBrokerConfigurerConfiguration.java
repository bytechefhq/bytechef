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
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.ErrorEvent;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Coordinator.Task.Subscriptions;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.event.MessageEventPostReceiveProcessor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCoordinator
public class TaskCoordinatorMessageBrokerConfigurerConfiguration {

    private final List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors;

    @SuppressFBWarnings("EI")
    public TaskCoordinatorMessageBrokerConfigurerConfiguration(
        List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors) {

        this.messageEventPostReceiveProcessors = messageEventPostReceiveProcessors;
    }

    @Bean
    MessageBrokerConfigurer<?> taskCoordinatorMessageBrokerConfigurer(
        TaskCoordinator taskCoordinator, ApplicationProperties applicationProperties) {

        TaskCoordinatorDelegate taskCoordinatorDelegate = new TaskCoordinatorDelegate(
            messageEventPostReceiveProcessors, taskCoordinator);

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {

            Subscriptions subscriptions = applicationProperties.getCoordinator()
                .getTask()
                .getSubscriptions();

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskCoordinatorMessageRoute.APPLICATION_EVENTS,
                subscriptions.getApplicationEvents(), taskCoordinatorDelegate, "onApplicationEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskCoordinatorMessageRoute.ERROR_EVENTS,
                subscriptions.getTaskExecutionErrorEvents(), taskCoordinatorDelegate, "onErrorEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskCoordinatorMessageRoute.JOB_RESUME_EVENTS,
                subscriptions.getResumeJobEvents(), taskCoordinatorDelegate, "onResumeJobEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskCoordinatorMessageRoute.JOB_START_EVENTS,
                subscriptions.getStartJobEvents(), taskCoordinatorDelegate, "onStartJobEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskCoordinatorMessageRoute.JOB_STOP_EVENTS,
                subscriptions.getStopJobEvents(), taskCoordinatorDelegate, "onStopJobEvent");
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
                subscriptions.getTaskExecutionCompleteEvents(), taskCoordinatorDelegate,
                "onTaskExecutionCompleteEvent");
        };
    }

    private record TaskCoordinatorDelegate(
        List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors, TaskCoordinator taskCoordinator) {

        public void onApplicationEvent(ApplicationEvent applicationEvent) {
            process(applicationEvent);

            taskCoordinator.onApplicationEvent(applicationEvent);
        }

        public void onErrorEvent(ErrorEvent errorEvent) {
            process(errorEvent);

            taskCoordinator.onErrorEvent(errorEvent);
        }

        public void onResumeJobEvent(ResumeJobEvent resumeJobEvent) {
            process(resumeJobEvent);

            taskCoordinator.onResumeJobEvent(resumeJobEvent);
        }

        public void onStartJobEvent(StartJobEvent startJobEvent) {
            process(startJobEvent);

            taskCoordinator.onStartJobEvent(startJobEvent);
        }

        public void onStopJobEvent(StopJobEvent stopJobEvent) {
            process(stopJobEvent);

            taskCoordinator.onStopJobEvent(stopJobEvent);
        }

        public void onTaskExecutionCompleteEvent(TaskExecutionCompleteEvent taskExecutionCompleteEvent) {
            process(taskExecutionCompleteEvent);

            taskCoordinator.onTaskExecutionCompleteEvent(taskExecutionCompleteEvent);
        }

        private void process(MessageEvent<?> messageEvent) {
            for (MessageEventPostReceiveProcessor messageEventPostReceiveProcessor : messageEventPostReceiveProcessors) {
                messageEventPostReceiveProcessor.process(messageEvent);
            }
        }
    }
}
