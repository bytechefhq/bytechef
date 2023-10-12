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

package com.bytechef.hermes.test.executor.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.facade.JobFacade;
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
import com.bytechef.atlas.file.storage.facade.TaskFileStorageFacade;
import com.bytechef.atlas.file.storage.facade.TaskFileStorageFacadeImpl;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.atlas.worker.task.factory.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.component.map.MapTaskDispatcherAdapterTaskHandler;
import com.bytechef.component.map.constant.MapConstants;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.test.executor.JobTestExecutor;
import com.bytechef.hermes.test.executor.JobTestExecutorImpl;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.task.dispatcher.branch.BranchTaskDispatcher;
import com.bytechef.task.dispatcher.branch.completion.BranchTaskCompletionHandler;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import com.bytechef.task.dispatcher.each.EachTaskDispatcher;
import com.bytechef.task.dispatcher.each.completion.EachTaskCompletionHandler;
import com.bytechef.task.dispatcher.forkjoin.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.forkjoin.completion.ForkJoinTaskCompletionHandler;
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
import com.bytechef.task.dispatcher.subflow.event.listener.SubflowJobStatusEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class TestExecutorConfiguration {

    @Bean
    JobTestExecutor jobTestExecutor(
        ComponentDefinitionService componentDefinitionService, JobFacade jobFacade,
        ObjectMapper objectMapper, TaskHandlerRegistry taskHandlerRegistry, WorkflowService workflowService) {

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
        CounterService counterService = new CounterServiceImpl(new InMemoryCounterRepository());
        SyncMessageBroker syncMessageBroker = new SyncMessageBroker(objectMapper);

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        JobService jobService = new JobServiceImpl(
            new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        TaskFileStorageFacade taskFileStorageFacade = new TaskFileStorageFacadeImpl(
            new Base64FileStorageService(), objectMapper);

        return new JobTestExecutorImpl(
            componentDefinitionService, contextService,
            new JobSyncExecutor(
                getApplicationEventListeners(
                    jobService, syncMessageBroker, taskExecutionService, taskFileStorageFacade),
                contextService, jobService, syncMessageBroker,
                getTaskCompletionHandlerFactories(
                    contextService, counterService, taskExecutionService, taskFileStorageFacade),
                getTaskDispatcherAdapterFactories(objectMapper),
                getTaskDispatcherResolverFactories(
                    syncMessageBroker, contextService, counterService, jobFacade, taskExecutionService,
                    taskFileStorageFacade),
                taskExecutionService, taskHandlerRegistry, taskFileStorageFacade, workflowService),
            taskExecutionService, taskFileStorageFacade);
    }

    private List<ApplicationEventListener> getApplicationEventListeners(
        JobService jobService, SyncMessageBroker syncMessageBroker,
        TaskExecutionService taskExecutionService, TaskFileStorageFacade taskFileStorageFacade) {

        ApplicationEventPublisher eventPublisher = getEventPublisher(syncMessageBroker);

        SubflowJobStatusEventListener subflowJobStatusEventListener = new SubflowJobStatusEventListener(
            eventPublisher, jobService, taskExecutionService, taskFileStorageFacade);

        return List.of(subflowJobStatusEventListener);
    }

    private static ApplicationEventPublisher getEventPublisher(SyncMessageBroker syncMessageBroker) {
        return event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event);
    }

    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService,
        TaskFileStorageFacade taskFileStorageFacade) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new BranchTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, taskFileStorageFacade),
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, taskFileStorageFacade),
            (taskCompletionHandler, taskDispatcher) -> new EachTaskCompletionHandler(
                counterService, taskCompletionHandler, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                taskExecutionService, taskCompletionHandler, counterService, taskDispatcher, contextService,
                taskFileStorageFacade),
            (taskCompletionHandler, taskDispatcher) -> new LoopTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, taskFileStorageFacade),
            (taskCompletionHandler, taskDispatcher) -> new MapTaskCompletionHandler(taskExecutionService,
                taskCompletionHandler, counterService, taskFileStorageFacade),
            (taskCompletionHandler, taskDispatcher) -> new ParallelTaskCompletionHandler(counterService,
                taskCompletionHandler, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new SequenceTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorageFacade));
    }

    private List<TaskDispatcherAdapterFactory> getTaskDispatcherAdapterFactories(ObjectMapper objectMapper) {
        return List.of(
            new TaskDispatcherAdapterFactory() {

                @Override
                public TaskHandler<?> create(TaskHandlerResolver taskHandlerResolver) {
                    return new MapTaskDispatcherAdapterTaskHandler(objectMapper, taskHandlerResolver);
                }

                @Override
                public String getName() {
                    return MapConstants.MAP + "/v1";
                }
            });
    }

    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        SyncMessageBroker syncMessageBroker, ContextService contextService,
        CounterService counterService, JobFacade jobFacade, TaskExecutionService taskExecutionService,
        TaskFileStorageFacade taskFileStorageFacade) {

        ApplicationEventPublisher eventPublisher = getEventPublisher(syncMessageBroker);

        return List.of(
            (taskDispatcher) -> new BranchTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorageFacade),
            (taskDispatcher) -> new ConditionTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorageFacade),
            (taskDispatcher) -> new EachTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                taskFileStorageFacade),
            (taskDispatcher) -> new ForkJoinTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                taskFileStorageFacade),
            (taskDispatcher) -> new LoopBreakTaskDispatcher(eventPublisher, taskExecutionService),
            (taskDispatcher) -> new LoopTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorageFacade),
            (taskDispatcher) -> new MapTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                taskFileStorageFacade),
            (taskDispatcher) -> new ParallelTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                taskFileStorageFacade),
            (taskDispatcher) -> new SequenceTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorageFacade),
            (taskDispatcher) -> new SubflowTaskDispatcher(jobFacade));
    }
}
