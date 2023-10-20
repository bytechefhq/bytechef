
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

package com.bytechef.task.dispatcher.loop.config;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.task.dispatcher.loop.LoopBreakTaskDispatcher;
import com.bytechef.task.dispatcher.loop.LoopTaskDispatcher;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class LoopTaskDispatcherConfiguration {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RemoteContextService contextService;

    @Autowired
    private RemoteTaskExecutionService taskExecutionService;

    @Autowired
    @Qualifier("workflowAsyncFileStorageFacade")
    private WorkflowFileStorageFacade workflowFileStorageFacade;

    @Bean
    TaskCompletionHandlerFactory loopTaskCompletionHandlerFactory() {
        return (taskCompletionHandler, taskDispatcher) -> new LoopTaskCompletionHandler(
            contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
    }

    @Bean("loopBreakTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory loopBreakTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new LoopBreakTaskDispatcher(eventPublisher, taskExecutionService);
    }

    @Bean("loopTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory loopTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new LoopTaskDispatcher(
            eventPublisher, contextService, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
    }
}
