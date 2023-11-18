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

package com.bytechef.hermes.worker.config;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.file.storage.TriggerFileStorage;
import com.bytechef.hermes.worker.TriggerWorker;
import com.bytechef.hermes.worker.executor.TriggerWorkerExecutor;
import com.bytechef.hermes.worker.trigger.factory.TriggerHandlerMapFactory;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandler;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandlerRegistry;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandlerResolver;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "worker.enabled", matchIfMissing = true)
public class TriggerWorkerConfiguration {

    @Bean
    TriggerHandlerRegistry triggerHandlerRegistry(
        Map<String, TriggerHandler> triggerHandlerMap,
        @Autowired(required = false) TriggerHandlerMapFactory triggerHandlerMapFactory) {

        return MapUtils.concat(
            triggerHandlerMap,
            triggerHandlerMapFactory.getTriggerHandlerMap() == null
                ? Map.of() : triggerHandlerMapFactory.getTriggerHandlerMap())::get;
    }

    @Bean
    TriggerHandlerResolver triggerHandlerResolver(TriggerHandlerRegistry triggerHandlerRegistry) {
        return new TriggerHandlerResolver(triggerHandlerRegistry);
    }

    @Bean
    TriggerWorker triggerWorker(
        ApplicationEventPublisher eventPublisher, TriggerFileStorage triggerFileStorage,
        TriggerWorkerExecutor triggerWorkerExecutor, TriggerHandlerResolver triggerHandlerResolver) {

        return new TriggerWorker(eventPublisher, triggerFileStorage, triggerWorkerExecutor,
            triggerHandlerResolver);
    }
}
