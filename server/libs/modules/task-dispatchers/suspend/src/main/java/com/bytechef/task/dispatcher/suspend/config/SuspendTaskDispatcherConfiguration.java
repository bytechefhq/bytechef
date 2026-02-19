/*
 * Copyright 2025 ByteChef
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

package com.bytechef.task.dispatcher.suspend.config;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.task.dispatcher.suspend.SuspendTaskDispatcher;
import com.bytechef.task.dispatcher.suspend.SuspendTaskDispatcherPreSendProcessor;
import com.bytechef.task.dispatcher.suspend.completion.SuspendTaskCompletionHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class SuspendTaskDispatcherConfiguration {

    private final ContextService contextService;
    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final TaskStateService taskStateService;

    @SuppressFBWarnings("EI")
    public SuspendTaskDispatcherConfiguration(
        ContextService contextService, ApplicationEventPublisher eventPublisher, JobService jobService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, TaskStateService taskStateService) {

        this.contextService = contextService;
        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.taskStateService = taskStateService;
    }

    @Bean("suspendTaskCompletionHandlerFactory")
    TaskCompletionHandlerFactory suspendTaskCompletionHandlerFactory() {
        return (taskCompletionHandler, taskDispatcher) -> new SuspendTaskCompletionHandler(
            contextService, eventPublisher, jobService, taskExecutionService, taskFileStorage, taskStateService);
    }

    @Bean
    SuspendTaskDispatcherPreSendProcessor suspendTaskDispatcherPreSendProcessor() {
        return new SuspendTaskDispatcherPreSendProcessor(jobService, taskStateService);
    }

    @Bean("suspendTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory suspendTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new SuspendTaskDispatcher(eventPublisher, jobService, taskExecutionService);
    }
}
