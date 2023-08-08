
/*
 * Copyright 2021 <your company/name>.
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
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.task.dispatcher.branch.BranchTaskDispatcher;
import com.bytechef.task.dispatcher.branch.completion.BranchTaskCompletionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnExpression("'${spring.application.name}'=='server-app' or '${spring.application.name}'=='coordinator-service-app'")
public class BranchTaskDispatcherConfiguration {

    @Autowired
    private ContextService contextService;

    @Autowired
    private MessageBroker messageBroker;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Bean("branchTaskCompletionHandlerFactory_v1")
    TaskCompletionHandlerFactory branchTaskCompletionHandlerFactory() {
        return (taskCompletionHandler, taskDispatcher) -> new BranchTaskCompletionHandler(
            contextService, taskCompletionHandler, taskDispatcher, taskExecutionService);
    }

    @Bean("branchTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory branchTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new BranchTaskDispatcher(
            contextService, messageBroker, taskDispatcher, taskExecutionService);
    }
}
