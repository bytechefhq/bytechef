/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.worker.config;

import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.atlas.worker.annotation.ConditionalOnWorker;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import com.bytechef.evaluator.Evaluator;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnWorker
public class TaskWorkerConfiguration {

    private final List<TaskDispatcherAdapterFactory> taskDispatcherAdapterTaskHandlerFactories;

    public TaskWorkerConfiguration(
        @Autowired(required = false) List<TaskDispatcherAdapterFactory> taskDispatcherAdapterTaskHandlerFactories) {

        this.taskDispatcherAdapterTaskHandlerFactories = taskDispatcherAdapterTaskHandlerFactories == null
            ? Collections.emptyList() : taskDispatcherAdapterTaskHandlerFactories;
    }

    @Bean
    @Primary
    TaskHandlerResolver taskHandlerResolver(TaskHandlerRegistry taskHandlerRegistry) {
        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                new TaskDispatcherAdapterTaskHandlerResolver(
                    taskDispatcherAdapterTaskHandlerFactories, taskHandlerResolverChain),
                new DefaultTaskHandlerResolver(taskHandlerRegistry)));

        return taskHandlerResolverChain;
    }

    @Bean
    TaskWorker taskWorker(
        Evaluator evaluator, ApplicationEventPublisher eventPublisher, Executor taskExecutor,
        TaskFileStorage taskFileStorage, TaskHandlerResolver taskHandlerResolver) {

        return new TaskWorker(
            evaluator, eventPublisher, (AsyncTaskExecutor) taskExecutor, taskHandlerResolver, taskFileStorage);
    }
}
