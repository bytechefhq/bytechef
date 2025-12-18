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

package com.bytechef.platform.webhook.executor.config;

import static com.bytechef.platform.coordinator.job.JobSyncExecutor.MemoryMessageFactory.Role.COORDINATOR;
import static com.bytechef.tenant.constant.TenantConstants.CURRENT_TENANT_ID;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.component.map.MapTaskDispatcherAdapterTaskHandler;
import com.bytechef.component.map.constant.MapConstants;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.coordinator.job.JobSyncExecutor;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutorImpl;
import com.bytechef.platform.webhook.executor.WebhookWorkflowSyncExecutor;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.task.dispatcher.approval.WaitForApprovalTaskDispatcher;
import com.bytechef.task.dispatcher.branch.BranchTaskDispatcher;
import com.bytechef.task.dispatcher.branch.completion.BranchTaskCompletionHandler;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import com.bytechef.task.dispatcher.each.EachTaskDispatcher;
import com.bytechef.task.dispatcher.each.completion.EachTaskCompletionHandler;
import com.bytechef.task.dispatcher.fork.join.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.fork.join.completion.ForkJoinTaskCompletionHandler;
import com.bytechef.task.dispatcher.loop.LoopBreakTaskDispatcher;
import com.bytechef.task.dispatcher.loop.LoopTaskDispatcher;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.bytechef.task.dispatcher.map.MapTaskDispatcher;
import com.bytechef.task.dispatcher.map.completion.MapTaskCompletionHandler;
import com.bytechef.task.dispatcher.parallel.ParallelTaskDispatcher;
import com.bytechef.task.dispatcher.parallel.completion.ParallelTaskCompletionHandler;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WebhookConfiguration {

    @Bean
    WebhookWorkflowExecutor webhookExecutor(
        CacheManager cacheManager, ContextService contextService, CounterService counterService,
        Environment environment, Evaluator evaluator, ApplicationEventPublisher eventPublisher,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, PrincipalJobFacade principalJobFacade,
        JobService jobService, List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors,
        TaskExecutionService taskExecutionService, TaskExecutor taskExecutor, TaskHandlerRegistry taskHandlerRegistry,
        WebhookWorkflowSyncExecutor triggerSyncExecutor, TaskFileStorage taskFileStorage,
        WorkflowService workflowService) {

        AsyncMessageBroker asyncMessageBroker = new AsyncMessageBroker(environment);

        return new WebhookWorkflowExecutorImpl(
            eventPublisher, jobPrincipalAccessorRegistry,
            principalJobFacade,
            new JobSyncExecutor(
                contextService, evaluator, jobService, -1,
                role -> (role == COORDINATOR) ? asyncMessageBroker : new AsyncMessageBroker(environment),
                getTaskCompletionHandlerFactories(
                    contextService, counterService, evaluator, taskExecutionService, taskFileStorage),
                getTaskDispatcherAdapterFactories(cacheManager, evaluator), taskDispatcherPreSendProcessors,
                getTaskDispatcherResolverFactories(
                    contextService, counterService, evaluator, jobService, asyncMessageBroker, taskExecutionService,
                    taskFileStorage),
                taskExecutionService, taskExecutor,
                taskHandlerRegistry, taskFileStorage, 300, workflowService),
            triggerSyncExecutor, taskFileStorage);
    }

    private static ApplicationEventPublisher createEventPublisher(MessageBroker messageBroker) {
        return event -> {
            MessageEvent<?> messageEvent = (MessageEvent<?>) event;

            messageEvent.putMetadata(CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(((MessageEvent<?>) event).getRoute(), event);
        };
    }

    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, Evaluator evaluator,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new BranchTaskCompletionHandler(
                contextService, evaluator, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, evaluator, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new EachTaskCompletionHandler(
                counterService, taskCompletionHandler, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                contextService, counterService, evaluator, taskExecutionService, taskCompletionHandler, taskDispatcher,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new LoopTaskCompletionHandler(
                contextService, evaluator, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new MapTaskCompletionHandler(
                contextService, counterService, evaluator, taskDispatcher, taskCompletionHandler, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new ParallelTaskCompletionHandler(
                counterService, taskCompletionHandler, taskExecutionService));
    }

    private List<TaskDispatcherAdapterFactory> getTaskDispatcherAdapterFactories(
        CacheManager cacheManager, Evaluator evaluator) {

        return List.of(
            new TaskDispatcherAdapterFactory() {

                @Override
                public TaskHandler<?> create(TaskHandlerResolver taskHandlerResolver) {
                    return new MapTaskDispatcherAdapterTaskHandler(cacheManager, evaluator, taskHandlerResolver);
                }

                @Override
                public String getName() {
                    return MapConstants.MAP + "/v1";
                }
            });
    }

    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ContextService contextService, CounterService counterService, Evaluator evaluator, JobService jobService,
        AsyncMessageBroker asyncMessageBroker, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        ApplicationEventPublisher eventPublisher = createEventPublisher(asyncMessageBroker);

        return List.of(
            (taskDispatcher) -> new WaitForApprovalTaskDispatcher(eventPublisher, jobService, taskExecutionService),
            (taskDispatcher) -> new BranchTaskDispatcher(
                contextService, evaluator, eventPublisher, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskDispatcher) -> new ConditionTaskDispatcher(
                contextService, evaluator, eventPublisher, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskDispatcher) -> new EachTaskDispatcher(
                contextService, counterService, evaluator, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new ForkJoinTaskDispatcher(
                contextService, counterService, evaluator, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new LoopBreakTaskDispatcher(eventPublisher, taskExecutionService),
            (taskDispatcher) -> new LoopTaskDispatcher(
                contextService, evaluator, eventPublisher, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskDispatcher) -> new MapTaskDispatcher(
                contextService, counterService, evaluator, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new ParallelTaskDispatcher(
                contextService, counterService, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }
}
