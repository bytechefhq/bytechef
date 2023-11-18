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

package com.bytechef.task.dispatcher.parallel.config;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.task.dispatcher.parallel.ParallelTaskDispatcher;
import com.bytechef.task.dispatcher.parallel.completion.ParallelTaskCompletionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class ParallelTaskDispatcherConfiguration {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ContextService contextService;

    @Autowired
    private CounterService counterService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Bean("parallelTaskCompletionHandlerFactory_v1")
    TaskCompletionHandlerFactory parallelTaskCompletionHandlerFactory() {
        return (taskCompletionHandler, taskDispatcher) -> new ParallelTaskCompletionHandler(counterService,
            taskCompletionHandler, taskExecutionService);
    }

    @Bean("parallelTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory parallelTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new ParallelTaskDispatcher(
            eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
            taskFileStorage);
    }
}
