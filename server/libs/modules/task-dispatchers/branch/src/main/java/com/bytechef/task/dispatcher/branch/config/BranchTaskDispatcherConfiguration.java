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

package com.bytechef.task.dispatcher.branch.config;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.task.dispatcher.branch.BranchTaskDispatcher;
import com.bytechef.task.dispatcher.branch.completion.BranchTaskCompletionHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class BranchTaskDispatcherConfiguration {

    private final ApplicationEventPublisher eventPublisher;
    private final ContextService contextService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public BranchTaskDispatcherConfiguration(
        ApplicationEventPublisher eventPublisher, ContextService contextService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        this.eventPublisher = eventPublisher;
        this.contextService = contextService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Bean("branchTaskCompletionHandlerFactory_v1")
    TaskCompletionHandlerFactory branchTaskCompletionHandlerFactory() {
        return (taskCompletionHandler, taskDispatcher) -> new BranchTaskCompletionHandler(
            contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, taskFileStorage);
    }

    @Bean("branchTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory branchTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new BranchTaskDispatcher(
            eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorage);
    }
}
