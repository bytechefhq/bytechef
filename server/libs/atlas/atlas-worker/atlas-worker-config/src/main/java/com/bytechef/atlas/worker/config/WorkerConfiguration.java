
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

import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistrar;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import com.bytechef.autoconfigure.annotation.ConditionalOnWorker;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnWorker
@EnableConfigurationProperties(WorkerProperties.class)
public class WorkerConfiguration {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired(required = false)
    List<TaskDispatcherAdapterFactory> taskDispatcherAdapterTaskHandlerFactories = Collections.emptyList();

    @Autowired(required = false)
    private List<TaskHandlerRegistrar> taskHandlerRegistrars = List.of();

    @Autowired
    private TaskEvaluator taskEvaluator;

    @PostConstruct
    public void afterPropertiesSet() {
        for (TaskHandlerRegistrar taskHandlerRegistrar : taskHandlerRegistrars) {
            taskHandlerRegistrar.registerTaskHandlers(beanFactory);
        }
    }

    @Bean
    TaskHandlerResolver defaultTaskHandlerResolver(Map<String, TaskHandler<?>> taskHandlers) {
        return new DefaultTaskHandlerResolver(taskHandlers == null ? Map.of() : taskHandlers);
    }

    @Bean
    TaskHandlerResolver taskDispatcherAdapterTaskHandlerResolver(TaskHandlerResolver taskHandlerResolver) {
        return new TaskDispatcherAdapterTaskHandlerResolver(
            taskDispatcherAdapterTaskHandlerFactories, taskHandlerResolver, taskEvaluator);
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
    Worker worker(TaskHandlerResolver taskHandlerResolver, MessageBroker messageBroker, EventPublisher eventPublisher) {
        return Worker.builder()
            .withTaskHandlerResolver(taskHandlerResolver)
            .withMessageBroker(messageBroker)
            .withEventPublisher(eventPublisher)
            .withTaskEvaluator(taskEvaluator)
            .build();
    }
}
