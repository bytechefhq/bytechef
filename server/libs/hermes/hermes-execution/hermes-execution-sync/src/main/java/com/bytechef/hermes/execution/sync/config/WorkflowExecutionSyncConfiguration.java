
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

package com.bytechef.hermes.execution.sync.config;

import com.bytechef.atlas.execution.factory.JobFactory;
import com.bytechef.atlas.execution.factory.JobFactoryImpl;
import com.bytechef.event.listener.EventListener;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.event.EventPublisher;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.execution.sync.WorkflowTestExecutorImpl;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.CounterServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.sync.JobSyncExecutor;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.component.map.MapTaskDispatcherAdapterTaskHandler;
import com.bytechef.component.map.constant.MapConstants;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.execution.sync.WorkflowTestExecutor;
import com.bytechef.task.dispatcher.branch.BranchTaskDispatcher;
import com.bytechef.task.dispatcher.branch.completion.BranchTaskCompletionHandler;
import com.bytechef.task.dispatcher.each.EachTaskDispatcher;
import com.bytechef.task.dispatcher.each.completion.EachTaskCompletionHandler;
import com.bytechef.task.dispatcher.forkjoin.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.forkjoin.completion.ForkJoinTaskCompletionHandler;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import com.bytechef.task.dispatcher.loop.LoopBreakTaskDispatcher;
import com.bytechef.task.dispatcher.loop.LoopTaskDispatcher;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.bytechef.task.dispatcher.map.MapTaskDispatcher;
import com.bytechef.task.dispatcher.map.completion.MapTaskCompletionHandler;
import com.bytechef.task.dispatcher.parallel.ParallelTaskDispatcher;
import com.bytechef.task.dispatcher.parallel.completion.ParallelTaskCompletionHandler;
import com.bytechef.task.dispatcher.sequence.SequenceTaskDispatcher;
import com.bytechef.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;
import com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher;
import com.bytechef.task.dispatcher.subflow.event.SubflowJobStatusEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkflowExecutionSyncConfiguration {

    @Bean
    WorkflowTestExecutor workflowTestExecutor(
        ComponentDefinitionService componentDefinitionService, ObjectMapper objectMapper,
        TaskHandlerRegistry taskHandlerRegistry, List<WorkflowRepository> workflowRepositories) {

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());

        CounterService counterService = new CounterServiceImpl(new InMemoryCounterRepository());

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        JobService jobService = new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();
        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        EventPublisher eventPublisher = getEventPublisher(jobService, syncMessageBroker, taskExecutionService);

        WorkflowService workflowService = new WorkflowServiceImpl(
            new ConcurrentMapCacheManager(), Collections.emptyList(), workflowRepositories);

        JobFactory jobFactory = new JobFactoryImpl(
            contextService, eventPublisher, jobService, syncMessageBroker, workflowService);

        return new WorkflowTestExecutorImpl(
            componentDefinitionService, contextService,
            JobSyncExecutor.builder()
                .contextService(contextService)
                .eventPublisher(eventPublisher)
                .jobService(jobService)
                .syncMessageBroker(syncMessageBroker)
                .taskCompletionHandlerFactories(
                    getTaskCompletionHandlerFactories(contextService, counterService, taskExecutionService))
                .taskDispatcherAdapterFactories(getTaskDispatcherAdapterFactories())
                .taskDispatcherResolverFactories(
                    getTaskDispatcherResolverFactories(
                        contextService, counterService, jobFactory, syncMessageBroker, taskExecutionService))
                .taskExecutionService(taskExecutionService)
                .taskHandlerRegistry(taskHandlerRegistry)
                .workflowService(workflowService)
                .build(),
            taskExecutionService);
    }

    private EventPublisher getEventPublisher(
        JobService jobService, MessageBroker messageBroker, TaskExecutionService taskExecutionService) {

        List<EventListener> eventListeners = List.of(
            new SubflowJobStatusEventListener(jobService, messageBroker, taskExecutionService));

        return workflowEvent -> {
            for (EventListener eventListener : eventListeners) {
                eventListener.onApplicationEvent(workflowEvent);
            }
        };
    }

    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new BranchTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new EachTaskCompletionHandler(
                taskExecutionService, taskCompletionHandler, counterService),
            (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                taskExecutionService, taskCompletionHandler, counterService, taskDispatcher, contextService),
            (taskCompletionHandler, taskDispatcher) -> new LoopTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new MapTaskCompletionHandler(taskExecutionService,
                taskCompletionHandler, counterService),
            (taskCompletionHandler, taskDispatcher) -> new ParallelTaskCompletionHandler(counterService,
                taskCompletionHandler, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new SequenceTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService));
    }

    private List<TaskDispatcherAdapterFactory> getTaskDispatcherAdapterFactories() {
        return List.of(
            new TaskDispatcherAdapterFactory() {

                @Override
                public TaskHandler<?> create(TaskHandlerResolver taskHandlerResolver) {
                    return new MapTaskDispatcherAdapterTaskHandler(taskHandlerResolver);
                }

                @Override
                public String getName() {
                    return MapConstants.MAP + "/v1";
                }
            });
    }

    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ContextService contextService, CounterService counterService, JobFactory jobFactory,
        MessageBroker messageBroker, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskDispatcher) -> new BranchTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskExecutionService),
            (taskDispatcher) -> new ConditionTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskExecutionService),
            (taskDispatcher) -> new EachTaskDispatcher(
                taskDispatcher, taskExecutionService, messageBroker, contextService, counterService),
            (taskDispatcher) -> new ForkJoinTaskDispatcher(
                contextService, counterService, messageBroker, taskDispatcher, taskExecutionService),
            (taskDispatcher) -> new LoopBreakTaskDispatcher(messageBroker, taskExecutionService),
            (taskDispatcher) -> new LoopTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskExecutionService),
            (taskDispatcher) -> MapTaskDispatcher.builder()
                .taskDispatcher(taskDispatcher)
                .taskExecutionService(taskExecutionService)
                .messageBroker(messageBroker)
                .contextService(contextService)
                .counterService(counterService)
                .build(),
            (taskDispatcher) -> new ParallelTaskDispatcher(
                contextService, counterService, messageBroker, taskDispatcher, taskExecutionService),
            (taskDispatcher) -> new SequenceTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskExecutionService),
            (taskDispatcher) -> new SubflowTaskDispatcher(jobFactory));
    }
}
