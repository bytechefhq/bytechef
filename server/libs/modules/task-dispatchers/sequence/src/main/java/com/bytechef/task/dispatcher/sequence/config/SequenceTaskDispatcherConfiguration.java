
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

package com.bytechef.task.dispatcher.sequence.config;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.task.dispatcher.sequence.SequenceTaskDispatcher;
import com.bytechef.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class SequenceTaskDispatcherConfiguration {

    @Autowired
    private ContextService contextService;

    @Autowired
    private MessageBroker messageBroker;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    @Qualifier("workflowAsyncFileStorageFacade")
    private WorkflowFileStorageFacade workflowFileStorageFacade;

    @Bean("sequenceTaskCompletionHandlerFactory_v1")
    TaskCompletionHandlerFactory sequenceTaskCompletionHandlerFactory() {
        return (taskCompletionHandler, taskDispatcher) -> new SequenceTaskCompletionHandler(
            contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
    }

    @Bean("sequenceTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory sequenceTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new SequenceTaskDispatcher(
            contextService, messageBroker, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
    }
}
