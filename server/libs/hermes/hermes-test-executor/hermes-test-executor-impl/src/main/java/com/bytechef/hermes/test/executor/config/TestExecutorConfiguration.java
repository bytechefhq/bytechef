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
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
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
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.atlas.worker.task.factory.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.map.MapTaskDispatcherAdapterTaskHandler;
import com.bytechef.component.map.constant.MapConstants;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.configuration.domain.WorkflowConnection;
import com.bytechef.hermes.configuration.facade.WorkflowConnectionFacade;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;

/**
 * @author Ivica Cardic
 */
@Configuration
public class TestExecutorConfiguration {

    @Bean
    JobTestExecutor jobTestExecutor(
        ComponentDefinitionService componentDefinitionService, ObjectMapper objectMapper,
        TaskHandlerRegistry taskHandlerRegistry, WorkflowConnectionFacade workflowConnectionFacade,
        WorkflowService workflowService) {

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
        CounterService counterService = new CounterServiceImpl(new InMemoryCounterRepository());
        SyncMessageBroker syncMessageBroker = new SyncMessageBroker(objectMapper);

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        JobService jobService = new JobServiceImpl(
            new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        TaskFileStorage taskFileStorage = new TaskFileStorageImpl(
            new Base64FileStorageService(), objectMapper);

        return new JobTestExecutorImpl(
            componentDefinitionService, contextService,
            new JobSyncExecutor(
                contextService, jobService, syncMessageBroker,
                getTaskCompletionHandlerFactories(
                    contextService, counterService, taskExecutionService, taskFileStorage),
                getTaskDispatcherAdapterFactories(objectMapper),
                getTaskDispatcherPreSendProcessors(jobService, workflowConnectionFacade),
                getTaskDispatcherResolverFactories(
                    syncMessageBroker, contextService, counterService, taskExecutionService, taskFileStorage),
                taskExecutionService, taskHandlerRegistry, taskFileStorage, workflowService),
            taskExecutionService, taskFileStorage);
    }

    private List<TaskDispatcherPreSendProcessor> getTaskDispatcherPreSendProcessors(
        JobService jobService, WorkflowConnectionFacade workflowConnectionFacade) {

        return List.of(
            taskExecution -> {
                Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

                Map<String, Long> connectionIdMap;
                Map<String, Map<String, Map<String, Long>>> jobTaskConnectionMap = getJobTaskConnectionMap(job);

                if (jobTaskConnectionMap.containsKey(taskExecution.getName())) {

                    // directly coming from .../workflow-tests POST endpoint

                    connectionIdMap = getConnectionIdMap(jobTaskConnectionMap.get(taskExecution.getName()));
                } else {

                    // defined in the workflow definition

                    connectionIdMap = getConnectionIdMap(
                        workflowConnectionFacade.getWorkflowConnections(taskExecution.getWorkflowTask()));
                }

                if (connectionIdMap != null) {
                    taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);
                }

                return taskExecution;
            });
    }

    private static Map<String, Long> getConnectionIdMap(Map<String, Map<String, Long>> taskConnectionMap) {
        return MapUtils.toMap(
            taskConnectionMap, Map.Entry::getKey, entry -> MapUtils.getLong(entry.getValue(), WorkflowConnection.ID));
    }

    private static Map<String, Long> getConnectionIdMap(List<WorkflowConnection> workflowConnections) {
        return MapUtils.toMap(
            workflowConnections, WorkflowConnection::getKey,
            workflowConnection -> OptionalUtils.get(workflowConnection.getId()));
    }

    private static ApplicationEventPublisher getEventPublisher(SyncMessageBroker syncMessageBroker) {
        return event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event);
    }

    private static Map<String, Map<String, Map<String, Long>>> getJobTaskConnectionMap(Job job) {
        return MapUtils.getMap(
            job.getMetadata(), MetadataConstants.CONNECTIONS, new ParameterizedTypeReference<>() {}, Map.of());
    }

    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new BranchTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new EachTaskCompletionHandler(
                counterService, taskCompletionHandler, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                taskExecutionService, taskCompletionHandler, counterService, taskDispatcher, contextService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new LoopTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new MapTaskCompletionHandler(taskExecutionService,
                taskCompletionHandler, counterService, taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new ParallelTaskCompletionHandler(counterService,
                taskCompletionHandler, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new SequenceTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage));
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
        CounterService counterService, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        ApplicationEventPublisher eventPublisher = getEventPublisher(syncMessageBroker);

        return List.of(
            (taskDispatcher) -> new BranchTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskDispatcher) -> new ConditionTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskDispatcher) -> new EachTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new ForkJoinTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new LoopBreakTaskDispatcher(eventPublisher, taskExecutionService),
            (taskDispatcher) -> new LoopTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskDispatcher) -> new MapTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new ParallelTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new SequenceTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorage));
    }
}
