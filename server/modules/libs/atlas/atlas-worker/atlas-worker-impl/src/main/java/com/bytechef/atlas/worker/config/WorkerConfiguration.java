/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.worker.config;

import com.bytechef.atlas.annotation.ConditionalOnWorker;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.task.execution.evaluator.spel.TempDir;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.WorkerImpl;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    @Autowired(required = false)
    List<TaskDispatcherAdapterFactory> taskDispatcherAdapterTaskHandlerFactories = Collections.emptyList();

    private TaskEvaluator taskEvaluator;

    @PostConstruct
    private void afterPropertiesSet() {
        taskEvaluator = SpelTaskEvaluator.builder()
                .environment(environment)
                .methodExecutor("tempDir", new TempDir())
                .build();
    }

    @Bean
    TaskHandlerResolver defaultTaskHandlerResolver(Map<String, TaskHandler<?>> taskHandlers) {
        return new DefaultTaskHandlerResolver(taskHandlers == null ? Map.of() : taskHandlers);
    }

    @Bean
    TaskHandlerResolver taskDispatcherAdapterTaskHandlerResolver(TaskHandlerResolver taskHandlerResolver) {
        return new DefaultTaskHandlerResolver(taskDispatcherAdapterTaskHandlerFactories.stream()
                .collect(Collectors.toMap(
                        TaskDispatcherAdapterFactory::getName,
                        taskDispatcherAdapterFactory -> taskDispatcherAdapterFactory.create(
                                taskHandlerResolver, taskEvaluator, WorkerImpl.builder()))));
    }

    @Bean
    @Primary
    TaskHandlerResolver taskHandlerResolver(Map<String, TaskHandler<?>> taskHandlers) {
        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(List.of(
                taskDispatcherAdapterTaskHandlerResolver(taskHandlerResolverChain),
                defaultTaskHandlerResolver(taskHandlers)));

        return taskHandlerResolverChain;
    }

    @Bean
    Worker worker(
            Environment environment,
            TaskHandlerResolver taskHandlerResolver,
            MessageBroker messageBroker,
            EventPublisher eventPublisher) {
        return WorkerImpl.builder()
                .withTaskHandlerResolver(taskHandlerResolver)
                .withMessageBroker(messageBroker)
                .withEventPublisher(eventPublisher)
                .withTaskEvaluator(SpelTaskEvaluator.builder()
                        .environment(environment)
                        .methodExecutor("tempDir", new TempDir())
                        .build())
                .build();
    }
}
