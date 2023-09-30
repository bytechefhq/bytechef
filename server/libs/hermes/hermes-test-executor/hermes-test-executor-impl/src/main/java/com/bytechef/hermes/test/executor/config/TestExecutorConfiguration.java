
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

package com.bytechef.hermes.test.executor.config;

import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.facade.RemoteJobFacade;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacadeImpl;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.component.registry.service.RemoteComponentDefinitionService;
import com.bytechef.hermes.test.executor.JobTestExecutorImpl;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.RemoteCounterService;
import com.bytechef.atlas.execution.service.CounterServiceImpl;
import com.bytechef.atlas.execution.service.RemoteJobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.configuration.service.RemoteWorkflowService;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.atlas.worker.task.factory.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.component.map.MapTaskDispatcherAdapterTaskHandler;
import com.bytechef.component.map.constant.MapConstants;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.test.executor.JobTestExecutor;
import com.bytechef.message.event.MessageEvent;
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
import com.bytechef.task.dispatcher.subflow.event.listener.SubflowJobStatusEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Configuration
public class TestExecutorConfiguration {

    @Bean
    JobTestExecutor jobTestExecutor(
        RemoteComponentDefinitionService componentDefinitionService, RemoteJobFacade jobFacade,
        ObjectMapper objectMapper, TaskHandlerRegistry taskHandlerRegistry, RemoteWorkflowService workflowService) {

        RemoteContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
        RemoteCounterService counterService = new CounterServiceImpl(new InMemoryCounterRepository());
        SyncMessageBroker syncMessageBroker = new SyncMessageBroker(objectMapper);

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        RemoteJobService jobService = new JobServiceImpl(
            new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        RemoteTaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        WorkflowFileStorageFacade workflowFileStorageFacade = new WorkflowFileStorageFacadeImpl(
            new Base64FileStorageService(), objectMapper);

        return new JobTestExecutorImpl(
            componentDefinitionService, contextService,
            new JobSyncExecutor(
                getApplicationEventListeners(
                    jobService, syncMessageBroker, taskExecutionService, workflowFileStorageFacade),
                contextService, jobService, syncMessageBroker,
                getTaskCompletionHandlerFactories(
                    contextService, counterService, taskExecutionService, workflowFileStorageFacade),
                getTaskDispatcherAdapterFactories(objectMapper),
                getTaskDispatcherResolverFactories(
                    syncMessageBroker, contextService, counterService, jobFacade, taskExecutionService,
                    workflowFileStorageFacade),
                taskExecutionService, taskHandlerRegistry, workflowFileStorageFacade, workflowService),
            taskExecutionService, workflowFileStorageFacade);
    }

    private List<ApplicationEventListener> getApplicationEventListeners(
        RemoteJobService jobService, SyncMessageBroker syncMessageBroker,
        RemoteTaskExecutionService taskExecutionService, WorkflowFileStorageFacade workflowFileStorageFacade) {

        ApplicationEventPublisher eventPublisher = getEventPublisher(syncMessageBroker);

        SubflowJobStatusEventListener subflowJobStatusEventListener = new SubflowJobStatusEventListener(
            eventPublisher, jobService, taskExecutionService, workflowFileStorageFacade);

        return List.of(subflowJobStatusEventListener);
    }

    private static ApplicationEventPublisher getEventPublisher(SyncMessageBroker syncMessageBroker) {
        return event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event);
    }

    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        RemoteContextService contextService, RemoteCounterService counterService,
        RemoteTaskExecutionService taskExecutionService,
        WorkflowFileStorageFacade workflowFileStorageFacade) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new BranchTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, workflowFileStorageFacade),
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, workflowFileStorageFacade),
            (taskCompletionHandler, taskDispatcher) -> new EachTaskCompletionHandler(
                counterService, taskCompletionHandler, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                taskExecutionService, taskCompletionHandler, counterService, taskDispatcher, contextService,
                workflowFileStorageFacade),
            (taskCompletionHandler, taskDispatcher) -> new LoopTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, workflowFileStorageFacade),
            (taskCompletionHandler, taskDispatcher) -> new MapTaskCompletionHandler(taskExecutionService,
                taskCompletionHandler, counterService, workflowFileStorageFacade),
            (taskCompletionHandler, taskDispatcher) -> new ParallelTaskCompletionHandler(counterService,
                taskCompletionHandler, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new SequenceTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService,
                workflowFileStorageFacade));
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
        SyncMessageBroker syncMessageBroker, RemoteContextService contextService,
        RemoteCounterService counterService, RemoteJobFacade jobFacade, RemoteTaskExecutionService taskExecutionService,
        WorkflowFileStorageFacade workflowFileStorageFacade) {

        ApplicationEventPublisher eventPublisher = getEventPublisher(syncMessageBroker);

        return List.of(
            (taskDispatcher) -> new BranchTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, workflowFileStorageFacade),
            (taskDispatcher) -> new ConditionTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, workflowFileStorageFacade),
            (taskDispatcher) -> new EachTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                workflowFileStorageFacade),
            (taskDispatcher) -> new ForkJoinTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                workflowFileStorageFacade),
            (taskDispatcher) -> new LoopBreakTaskDispatcher(eventPublisher, taskExecutionService),
            (taskDispatcher) -> new LoopTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, workflowFileStorageFacade),
            (taskDispatcher) -> new MapTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                workflowFileStorageFacade),
            (taskDispatcher) -> new ParallelTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                workflowFileStorageFacade),
            (taskDispatcher) -> new SequenceTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, workflowFileStorageFacade),
            (taskDispatcher) -> new SubflowTaskDispatcher(jobFacade));
    }
}
