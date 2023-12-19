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

package com.bytechef.task.dispatcher.fork.join.config;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.task.dispatcher.fork.join.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.fork.join.completion.ForkJoinTaskCompletionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class ForkJoinTaskDispatcherConfiguration {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ContextService contextService;

    @Autowired
    private CounterService counterService;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Bean("forkJoinTaskCompletionHandlerFactory_v1")
    TaskCompletionHandlerFactory forkTaskCompletionHandlerFactory() {
        return (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
            taskExecutionService, taskCompletionHandler, counterService, taskDispatcher, contextService,
            taskFileStorage);
    }

    @Bean("forkJoinTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory forkTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new ForkJoinTaskDispatcher(
            eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
            taskFileStorage);
    }
}
