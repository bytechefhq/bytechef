
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

package com.bytechef.hermes.workflow.config;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.repository.WorkflowRepository;
import com.bytechef.atlas.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.ContextServiceImpl;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.JobServiceImpl;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.service.WorkflowServiceImpl;
import com.bytechef.atlas.sync.executor.WorkflowSyncExecutor;
import com.bytechef.hermes.workflow.executor.WorkflowTestExecutor;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkflowTestExecutorConfiguration {

    @Bean
    WorkflowTestExecutor workflowTestExecutor(
        ObjectMapper objectMapper, List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories,
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories,
        Map<String, TaskHandler<?>> taskHandlerMap, List<WorkflowRepository> workflowRepositories) {

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        JobService jobService = new JobServiceImpl(
            new InMemoryJobRepository(taskExecutionRepository, objectMapper), workflowRepositories);

        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        WorkflowService workflowService = new WorkflowServiceImpl(
            new ConcurrentMapCacheManager(), Collections.emptyList(), workflowRepositories);

        return new WorkflowTestExecutor(
            new WorkflowSyncExecutor(
                contextService, jobService, e -> {}, taskCompletionHandlerFactories, taskDispatcherResolverFactories,
                taskExecutionService, taskHandlerMap, workflowService));
    }
}
