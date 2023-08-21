
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

package com.bytechef.hermes.webhook.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.component.map.MapTaskDispatcherAdapterTaskHandler;
import com.bytechef.component.map.constant.MapConstants;
import com.bytechef.event.EventPublisher;
import com.bytechef.event.listener.EventListener;
import com.bytechef.hermes.coordinator.instance.InstanceWorkflowAccessorRegistry;
import com.bytechef.hermes.webhook.executor.TriggerSyncExecutor;
import com.bytechef.hermes.webhook.executor.WebhookExecutor;
import com.bytechef.hermes.webhook.executor.WebhookExecutorImpl;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.sync.SyncMessageBroker;
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
import com.bytechef.task.dispatcher.subflow.event.SubflowJobStatusEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WebhookConfiguration {

    @Bean
    WebhookExecutor webhookSyncExecutor(
        ContextService contextService, CounterService counterService,
        InstanceWorkflowAccessorRegistry instanceWorkflowAccessorRegistry, JobFacade jobFacade,
        JobService jobService, MessageBroker messageBroker, TaskExecutionService taskExecutionService,
        TaskHandlerRegistry taskHandlerRegistry, TriggerSyncExecutor triggerSyncExecutor,
        WorkflowService workflowService) {

        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        return new WebhookExecutorImpl(
            instanceWorkflowAccessorRegistry,
            jobFacade, JobSyncExecutor.builder()
                .contextService(contextService)
                .eventPublisher(createEventPublisher(jobService, syncMessageBroker, taskExecutionService))
                .jobService(jobService)
                .syncMessageBroker(syncMessageBroker)
                .taskCompletionHandlerFactories(
                    getTaskCompletionHandlerFactories(contextService, counterService, taskExecutionService))
                .taskDispatcherAdapterFactories(getTaskDispatcherAdapterFactories())
                .taskDispatcherResolverFactories(
                    getTaskDispatcherResolverFactories(
                        contextService, counterService, syncMessageBroker, taskExecutionService))
                .taskExecutionService(taskExecutionService)
                .taskHandlerRegistry(taskHandlerRegistry)
                .workflowService(workflowService)
                .build(),
            messageBroker, triggerSyncExecutor);
    }

    private EventPublisher createEventPublisher(
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
        ContextService contextService, CounterService counterService, MessageBroker messageBroker,
        TaskExecutionService taskExecutionService) {

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
            (taskDispatcher) -> new SubflowTaskDispatcher(messageBroker));
    }
}
