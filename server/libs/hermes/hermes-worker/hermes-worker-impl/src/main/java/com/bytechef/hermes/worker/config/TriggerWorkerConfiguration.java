
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

package com.bytechef.hermes.worker.config;

import com.bytechef.autoconfigure.annotation.ConditionalOnEnabled;
import com.bytechef.event.EventPublisher;
import com.bytechef.hermes.worker.TriggerWorker;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandler;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandlerRegistry;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandlerResolver;
import com.bytechef.message.broker.MessageBroker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.Executors;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEnabled("worker")
public class TriggerWorkerConfiguration {

    @Bean
    TriggerHandlerRegistry triggerHandlerRegistry(Map<String, TriggerHandler<?>> triggerHandlerMap) {
        return triggerHandlerMap::get;
    }

    @Bean
    TriggerHandlerResolver triggerHandlerResolver(TriggerHandlerRegistry triggerHandlerRegistry) {
        return new TriggerHandlerResolver(triggerHandlerRegistry);
    }

    @Bean
    TriggerWorker triggerWorker(
        EventPublisher eventPublisher, MessageBroker messageBroker, TriggerHandlerResolver triggerHandlerResolver) {

        return new TriggerWorker(
            eventPublisher, Executors.newCachedThreadPool(), messageBroker,
            triggerHandlerResolver);
    }
}
