
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

import com.bytechef.autoconfigure.annotation.ConditionalOnEnabled;
import com.bytechef.event.EventPublisher;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEnabled("worker")
@EnableConfigurationProperties(TaskWorkerProperties.class)
public class TaskWorkerConfiguration {

    private final List<TaskDispatcherAdapterFactory> taskDispatcherAdapterTaskHandlerFactories;

    @SuppressFBWarnings("EI")
    public TaskWorkerConfiguration(
        @Autowired(required = false) List<TaskDispatcherAdapterFactory> taskDispatcherAdapterTaskHandlerFactories) {

        this.taskDispatcherAdapterTaskHandlerFactories = taskDispatcherAdapterTaskHandlerFactories == null
            ? Collections.emptyList() : taskDispatcherAdapterTaskHandlerFactories;
    }

    @Bean
    TaskHandlerResolver defaultTaskHandlerResolver(TaskHandlerRegistry taskHandlerRegistry) {
        return new DefaultTaskHandlerResolver(taskHandlerRegistry);
    }

    @Bean
    TaskHandlerResolver taskDispatcherAdapterTaskHandlerResolver(TaskHandlerResolver taskHandlerResolver) {
        return new TaskDispatcherAdapterTaskHandlerResolver(
            taskDispatcherAdapterTaskHandlerFactories, taskHandlerResolver);
    }

    @Bean
    @Primary
    TaskHandlerResolver taskHandlerResolver(TaskHandlerRegistry taskHandlerRegistry) {
        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                taskDispatcherAdapterTaskHandlerResolver(taskHandlerResolverChain),
                defaultTaskHandlerResolver(taskHandlerRegistry)));

        return taskHandlerResolverChain;
    }

    @Bean
    TaskWorker taskWorker(
        EventPublisher eventPublisher, MessageBroker messageBroker, TaskHandlerResolver taskHandlerResolver) {

        return new TaskWorker(eventPublisher, messageBroker, taskHandlerResolver);
    }
}
