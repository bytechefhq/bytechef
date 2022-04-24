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
import com.integri.atlas.engine.core.task.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.core.task.evaluator.spel.TempDir;
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
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnWorker
public class WorkerConfiguration {

    @Autowired
    private Environment environment;

    private TaskEvaluator taskEvaluator;

    @PostConstruct
    private void afterPropertiesSet() {
        taskEvaluator =
            SpelTaskEvaluator.builder().environment(environment).methodExecutor("tempDir", new TempDir()).build();
    }

    @Bean
    DefaultTaskHandlerResolver defaultTaskHandlerResolver(Map<String, TaskHandler<?>> taskHandlers) {
        return new DefaultTaskHandlerResolver(taskHandlers);
    }

    @Bean
    TaskDispatcherAdapterTaskHandlerResolver taskDispatcherTaskHandlerResolverAdapter(
        TaskHandlerResolver taskHandlerResolver
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
    TaskHandlerResolver taskHandlerResolver(Map<String, TaskHandler<?>> taskHandlers) {
        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                taskDispatcherTaskHandlerResolverAdapter(taskHandlerResolverChain),
                defaultTaskHandlerResolver(taskHandlers)
            )
        );

        return taskHandlerResolverChain;
    }

    @Bean
    Worker worker(TaskHandlerResolver taskHandlerResolver, MessageBroker messageBroker, EventPublisher eventPublisher) {
        return WorkerImpl
            .builder()
            .withTaskHandlerResolver(taskHandlerResolver)
            .withMessageBroker(messageBroker)
            .withEventPublisher(eventPublisher)
            .withTaskEvaluator(taskEvaluator)
            .build();
    }
}
