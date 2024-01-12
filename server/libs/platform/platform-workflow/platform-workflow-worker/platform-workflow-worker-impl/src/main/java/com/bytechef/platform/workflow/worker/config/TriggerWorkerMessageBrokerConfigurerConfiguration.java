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

import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.platform.workflow.worker.TriggerWorker;
import com.bytechef.platform.workflow.worker.trigger.message.route.TriggerWorkerMessageRoute;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("'${bytechef.worker.enabled:true}' == 'true'")
public class TriggerWorkerMessageBrokerConfigurerConfiguration {

    @Bean
    MessageBrokerConfigurer<?> triggerWorkerMessageBrokerConfigurer(TriggerWorker triggerWorker) {
        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerWorkerMessageRoute.CONTROL_EVENTS, 1, triggerWorker,
                "onCancelControlTriggerEvent");

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, TriggerWorkerMessageRoute.TRIGGER_EXECUTION_EVENTS, 1, triggerWorker,
                "onTriggerExecutionEvent");
        };
    }
}
