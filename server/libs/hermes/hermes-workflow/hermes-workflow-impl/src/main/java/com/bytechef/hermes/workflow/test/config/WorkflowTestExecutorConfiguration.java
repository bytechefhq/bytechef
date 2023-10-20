
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

package com.bytechef.hermes.workflow.test.config;

import com.bytechef.atlas.coordinator.event.EventListener;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.repository.WorkflowRepository;
import com.bytechef.atlas.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.ContextServiceImpl;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.CounterServiceImpl;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.JobServiceImpl;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.service.WorkflowServiceImpl;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandlerAccessor;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.component.map.MapTaskDispatcherAdapterTaskHandler;
import com.bytechef.component.map.constant.MapConstants;
import com.bytechef.hermes.workflow.test.executor.WorkflowTestExecutor;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.workflow.test.executor.WorkflowTestExecutorImpl;
import com.bytechef.task.dispatcher.branch.BranchTaskDispatcher;
import com.bytechef.task.dispatcher.branch.completion.BranchTaskCompletionHandler;
import com.bytechef.task.dispatcher.each.EachTaskDispatcher;
import com.bytechef.task.dispatcher.each.completion.EachTaskCompletionHandler;
import com.bytechef.task.dispatcher.forkjoin.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.forkjoin.completion.ForkJoinTaskCompletionHandler;
import com.bytechef.task.dispatcher.if_.IfTaskDispatcher;
import com.bytechef.task.dispatcher.if_.completion.IfTaskCompletionHandler;
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
public class WorkflowTestExecutorConfiguration {

    private static final TaskEvaluator taskEvaluator = TaskEvaluator.create();

    @Bean
    WorkflowTestExecutor workflowTestExecutor(
        ObjectMapper objectMapper, TaskHandlerAccessor taskHandlerAccessor,
        List<WorkflowRepository> workflowRepositories) {

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());

        CounterService counterService = new CounterServiceImpl(new InMemoryCounterRepository());

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        JobService jobService = new JobServiceImpl(
            new InMemoryJobRepository(taskExecutionRepository, objectMapper), workflowRepositories);

        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        WorkflowService workflowService = new WorkflowServiceImpl(
            new ConcurrentMapCacheManager(), Collections.emptyList(), workflowRepositories);

        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        return new WorkflowTestExecutorImpl(
            JobSyncExecutor.builder()
                .contextService(contextService)
                .eventPublisher(getEventPublisher(jobService, syncMessageBroker, taskExecutionService))
                .jobService(jobService)
                .taskCompletionHandlerFactories(
                    getTaskCompletionHandlerFactories(
                        contextService, counterService, taskEvaluator, taskExecutionService))
                .taskDispatcherAdapterFactories(getTaskDispatcherAdapterFactories())
                .taskDispatcherResolverFactories(
                    getTaskDispatcherResolverFactories(
                        contextService, counterService, syncMessageBroker, taskEvaluator, taskExecutionService))
                .syncMessageBroker(syncMessageBroker)
                .taskExecutionService(taskExecutionService)
                .taskHandlerAccessor(taskHandlerAccessor)
                .workflowService(workflowService)
                .build(),
            taskExecutionService);
    }

    private EventPublisher getEventPublisher(
        JobService jobService, MessageBroker messageBroker, TaskExecutionService taskExecutionService) {

        List<EventListener> eventListeners = List.of(
            new SubflowJobStatusEventListener(
                jobService, messageBroker, taskExecutionService, taskEvaluator));

        return workflowEvent -> {
            for (EventListener eventListener : eventListeners) {
                eventListener.onApplicationEvent(workflowEvent);
            }
        };
    }

    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskEvaluator taskEvaluator,
        TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new BranchTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new EachTaskCompletionHandler(
                taskExecutionService, taskCompletionHandler, counterService),
            (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                taskExecutionService, taskCompletionHandler, counterService, taskDispatcher, contextService,
                taskEvaluator),
            (taskCompletionHandler, taskDispatcher) -> new IfTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new LoopTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new MapTaskCompletionHandler(taskExecutionService,
                taskCompletionHandler, counterService),
            (taskCompletionHandler, taskDispatcher) -> new ParallelTaskCompletionHandler(counterService,
                taskCompletionHandler, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new SequenceTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService));
    }

    private List<TaskDispatcherAdapterFactory> getTaskDispatcherAdapterFactories() {
        return List.of(
            new TaskDispatcherAdapterFactory() {

                @Override
                public TaskHandler<?> create(TaskHandlerResolver taskHandlerResolver, TaskEvaluator taskEvaluator) {
                    return new MapTaskDispatcherAdapterTaskHandler(taskHandlerResolver, taskEvaluator);
                }

                @Override
                public String getName() {
                    return MapConstants.MAP + "/v1";
                }
            });
    }

    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ContextService contextService, CounterService counterService, MessageBroker messageBroker,
        TaskEvaluator taskEvaluator, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskDispatcher) -> new BranchTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService),
            (taskDispatcher) -> new EachTaskDispatcher(
                taskDispatcher, taskExecutionService, messageBroker, contextService, counterService, taskEvaluator),
            (taskDispatcher) -> new ForkJoinTaskDispatcher(
                contextService, counterService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService),
            (taskDispatcher) -> new IfTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService),
            (taskDispatcher) -> new LoopBreakTaskDispatcher(messageBroker, taskExecutionService),
            (taskDispatcher) -> new LoopTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService),
            (taskDispatcher) -> MapTaskDispatcher.builder()
                .taskDispatcher(taskDispatcher)
                .taskExecutionService(taskExecutionService)
                .messageBroker(messageBroker)
                .contextService(contextService)
                .counterService(counterService)
                .taskEvaluator(taskEvaluator)
                .build(),
            (taskDispatcher) -> new ParallelTaskDispatcher(
                contextService, counterService, messageBroker, taskDispatcher, taskExecutionService),
            (taskDispatcher) -> new SequenceTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService),
            (taskDispatcher) -> new SubflowTaskDispatcher(messageBroker));
    }
}
