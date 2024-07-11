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

package com.bytechef.task.dispatcher.subflow.config;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher;
import com.bytechef.task.dispatcher.subflow.event.listener.SubflowJobStatusEventListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class SubflowTaskDispatcherConfiguration {

    @Bean("subflowTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory subflowTaskDispatcherResolverFactory(JobFacade jobFacade) {
        return (taskDispatcher) -> new SubflowTaskDispatcher(jobFacade);
    }

    @Configuration
    public static class SubflowJobStatusEventListenerConfiguration {

        @Bean
        SubflowJobStatusEventListener subflowJobStatusEventListener(
            ApplicationEventPublisher eventPublisher, JobService jobService,
            TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

            return new SubflowJobStatusEventListener(
                eventPublisher, jobService, taskExecutionService, taskFileStorage);
        }
    }
}
