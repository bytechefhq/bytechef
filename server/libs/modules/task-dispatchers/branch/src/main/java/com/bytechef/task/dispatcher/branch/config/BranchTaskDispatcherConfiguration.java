
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
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.task.dispatcher.branch.BranchTaskDispatcher;
import com.bytechef.task.dispatcher.branch.completion.BranchTaskCompletionHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class BranchTaskDispatcherConfiguration {

    private final ApplicationEventPublisher eventPublisher;
    private final RemoteContextService contextService;
    private final RemoteTaskExecutionService taskExecutionService;
    private final WorkflowFileStorageFacade workflowFileStorageFacade;

    @SuppressFBWarnings("EI")
    public BranchTaskDispatcherConfiguration(
        ApplicationEventPublisher eventPublisher, RemoteContextService contextService,
        RemoteTaskExecutionService taskExecutionService,
        @Qualifier("workflowAsyncFileStorageFacade") WorkflowFileStorageFacade workflowFileStorageFacade) {

        this.eventPublisher = eventPublisher;
        this.contextService = contextService;
        this.taskExecutionService = taskExecutionService;
        this.workflowFileStorageFacade = workflowFileStorageFacade;
    }

    @Bean("branchTaskCompletionHandlerFactory_v1")
    TaskCompletionHandlerFactory branchTaskCompletionHandlerFactory() {
        return (taskCompletionHandler, taskDispatcher) -> new BranchTaskCompletionHandler(
            contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
    }

    @Bean("branchTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory branchTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new BranchTaskDispatcher(
            eventPublisher, contextService, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
    }
}
