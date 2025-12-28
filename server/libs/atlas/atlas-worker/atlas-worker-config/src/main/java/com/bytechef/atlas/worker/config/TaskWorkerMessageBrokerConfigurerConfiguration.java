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

package com.bytechef.atlas.worker.config;

import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.atlas.worker.annotation.ConditionalOnWorker;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.message.route.TaskWorkerMessageRoute;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.event.MessageEventPostReceiveProcessor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnWorker
public class TaskWorkerMessageBrokerConfigurerConfiguration {

    private final List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors;

    @SuppressFBWarnings("EI")
    public TaskWorkerMessageBrokerConfigurerConfiguration(
        List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors) {

        this.messageEventPostReceiveProcessors = messageEventPostReceiveProcessors;
    }

    @Bean
    MessageBrokerConfigurer<?> taskWorkerMessageBrokerConfigurer(
        TaskWorker taskWorker, ApplicationProperties applicationProperties) {

        TaskWorkerDelegate taskWorkerDelegate = new TaskWorkerDelegate(messageEventPostReceiveProcessors, taskWorker);

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            Map<String, Integer> subscriptions = applicationProperties.getWorker()
                .getTask()
                .getSubscriptions();

            subscriptions.forEach((routeName, concurrency) -> messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskWorkerMessageRoute.ofTaskMessageRoute(routeName), concurrency,
                taskWorkerDelegate, "onTaskExecutionEvent"));

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TaskWorkerMessageRoute.CONTROL_EVENTS, 1, taskWorkerDelegate,
                "onCancelControlTaskEvent");
        };
    }

    private record TaskWorkerDelegate(
        List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors, TaskWorker taskWorker) {

        public void onTaskExecutionEvent(TaskExecutionEvent taskExecutionEvent) {
            process(taskExecutionEvent);

            taskWorker.onTaskExecutionEvent(taskExecutionEvent);
        }

        public void onCancelControlTaskEvent(MessageEvent<?> messageEvent) {
            process(messageEvent);

            taskWorker.onCancelControlTaskEvent(messageEvent);
        }

        private void process(MessageEvent<?> messageEvent) {
            for (MessageEventPostReceiveProcessor messageEventPostReceiveProcessor : messageEventPostReceiveProcessors) {
                messageEventPostReceiveProcessor.process(messageEvent);
            }
        }
    }
}
