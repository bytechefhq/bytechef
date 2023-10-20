/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.config;

import com.integri.atlas.engine.core.event.EventPublisher;
import com.integri.atlas.engine.core.messagebroker.MessageBroker;
import com.integri.atlas.engine.core.task.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.core.task.evaluator.spel.TempDir;
import com.integri.atlas.engine.worker.Worker;
import com.integri.atlas.engine.worker.annotation.ConditionalOnWorker;
import com.integri.atlas.engine.worker.task.TaskDispatcherAdapterTaskHandlerResolver;
import com.integri.atlas.engine.worker.task.TaskHandlerResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnWorker
public class WorkerConfiguration {

    @Bean
    Worker worker(
        TaskHandlerResolver aTaskHandlerResolver,
        MessageBroker aMessageBroker,
        @Lazy EventPublisher aEventPublisher,
        Environment aEnvironment
    ) {
        return Worker
            .builder()
            .withTaskHandlerResolver(aTaskHandlerResolver)
            .withMessageBroker(aMessageBroker)
            .withEventPublisher(aEventPublisher)
            .withTaskEvaluator(
                SpelTaskEvaluator.builder().methodExecutor("tempDir", new TempDir()).environment(aEnvironment).build()
            )
            .build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    TaskHandlerResolver taskDispatcherTaskHandlerResolverAdapter(
        @Lazy TaskHandlerResolver aResolver,
        Environment aEnvironment
    ) {
        return new TaskDispatcherAdapterTaskHandlerResolver(
            aResolver,
            SpelTaskEvaluator.builder().methodExecutor("tempDir", new TempDir()).environment(aEnvironment).build()
        );
    }
}
