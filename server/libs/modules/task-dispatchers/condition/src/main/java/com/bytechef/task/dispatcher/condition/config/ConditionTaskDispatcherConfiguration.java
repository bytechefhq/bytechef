
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

package com.bytechef.task.dispatcher.condition.config;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class ConditionTaskDispatcherConfiguration {

    @Autowired
    private ContextService contextService;

    @Autowired
    private MessageBroker messageBroker;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    @Qualifier("workflowAsyncFileStorageFacade")
    private WorkflowFileStorageFacade workflowFileStorageFacade;

    @Bean("conditionTaskCompletionHandlerFactory_v1")
    TaskCompletionHandlerFactory conditionTaskCompletionHandlerFactory() {
        return (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
            contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
    }

    @Bean("conditionTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory conditionTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new ConditionTaskDispatcher(
            contextService, messageBroker, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
    }
}
