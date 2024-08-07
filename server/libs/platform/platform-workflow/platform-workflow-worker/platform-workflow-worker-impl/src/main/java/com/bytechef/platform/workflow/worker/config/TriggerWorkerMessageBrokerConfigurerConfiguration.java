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

package com.bytechef.platform.workflow.worker.config;

import com.bytechef.atlas.worker.annotation.ConditionalOnWorker;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.event.MessageEventPostReceiveProcessor;
import com.bytechef.platform.workflow.worker.TriggerWorker;
import com.bytechef.platform.workflow.worker.trigger.event.TriggerExecutionEvent;
import com.bytechef.platform.workflow.worker.trigger.message.route.TriggerWorkerMessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnWorker
public class TriggerWorkerMessageBrokerConfigurerConfiguration {

    private final List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors;

    @SuppressFBWarnings("EI")
    public TriggerWorkerMessageBrokerConfigurerConfiguration(
        List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors) {

        this.messageEventPostReceiveProcessors = messageEventPostReceiveProcessors;
    }

    @Bean
    MessageBrokerConfigurer<?> triggerWorkerMessageBrokerConfigurer(TriggerWorker triggerWorker) {
        TaskWorkerDelegate taskWorkerDelegate =
            new TaskWorkerDelegate(messageEventPostReceiveProcessors, triggerWorker);

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerWorkerMessageRoute.CONTROL_EVENTS, 1, taskWorkerDelegate,
                "onCancelControlTriggerEvent");

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerWorkerMessageRoute.TRIGGER_EXECUTION_EVENTS, 1, taskWorkerDelegate,
                "onTriggerExecutionEvent");
        };
    }

    private record TaskWorkerDelegate(
        List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors, TriggerWorker triggerWorker) {

        public void onCancelControlTriggerEvent(MessageEvent<?> messageEvent) {
            process(messageEvent);

            triggerWorker.onCancelControlTriggerEvent(messageEvent);
        }

        public void onTriggerExecutionEvent(TriggerExecutionEvent triggerExecutionEvent) {
            process(triggerExecutionEvent);

            triggerWorker.onTriggerExecutionEvent(triggerExecutionEvent);
        }

        private void process(MessageEvent<?> messageEvent) {
            for (MessageEventPostReceiveProcessor messageEventPostReceiveProcessor : messageEventPostReceiveProcessors) {
                messageEventPostReceiveProcessor.process(messageEvent);
            }
        }
    }
}
