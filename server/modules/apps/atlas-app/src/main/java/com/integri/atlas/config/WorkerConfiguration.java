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
import com.integri.atlas.engine.core.message.broker.MessageBroker;
import com.integri.atlas.engine.core.task.evaluator.TaskEvaluator;
import com.integri.atlas.engine.worker.Worker;
import com.integri.atlas.engine.worker.WorkerImpl;
import com.integri.atlas.engine.worker.annotation.ConditionalOnWorker;
import com.integri.atlas.engine.worker.task.handler.DefaultTaskHandlerResolver;
import com.integri.atlas.engine.worker.task.handler.TaskDispatcherAdapterTaskHandlerResolver;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.engine.worker.task.handler.TaskHandlerResolver;
import com.integri.atlas.engine.worker.task.handler.TaskHandlerResolverChain;
import com.integri.atlas.task.handler.map.MapTaskDispatcherAdapterTaskHandler;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnWorker
public class WorkerConfiguration {

    @Bean
    DefaultTaskHandlerResolver defaultTaskHandlerResolver(Map<String, TaskHandler<?>> taskHandlers) {
        return new DefaultTaskHandlerResolver(taskHandlers);
    }

    @Bean
    TaskDispatcherAdapterTaskHandlerResolver taskDispatcherTaskHandlerResolverAdapter(
        TaskHandlerResolver taskHandlerResolver,
        TaskEvaluator taskEvaluator
    ) {
        return new TaskDispatcherAdapterTaskHandlerResolver(
            Map.of(
                "map",
                new MapTaskDispatcherAdapterTaskHandler(taskHandlerResolver, taskEvaluator, WorkerImpl.builder())
            )
        );
    }

    @Bean
    @Primary
    TaskHandlerResolver taskHandlerResolver(TaskEvaluator taskEvaluator, Map<String, TaskHandler<?>> taskHandlers) {
        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                taskDispatcherTaskHandlerResolverAdapter(taskHandlerResolverChain, taskEvaluator),
                defaultTaskHandlerResolver(taskHandlers)
            )
        );

        return taskHandlerResolverChain;
    }

    @Bean
    Worker worker(
        TaskHandlerResolver aTaskHandlerResolver,
        MessageBroker aMessageBroker,
        EventPublisher aEventPublisher,
        TaskEvaluator taskEvaluator
    ) {
        return WorkerImpl
            .builder()
            .withTaskHandlerResolver(aTaskHandlerResolver)
            .withMessageBroker(aMessageBroker)
            .withEventPublisher(aEventPublisher)
            .withTaskEvaluator(taskEvaluator)
            .build();
    }
}
