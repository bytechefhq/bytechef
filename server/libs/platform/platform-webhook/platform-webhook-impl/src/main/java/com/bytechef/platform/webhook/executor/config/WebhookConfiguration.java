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

import static com.bytechef.tenant.constant.TenantConstants.CURRENT_TENANT_ID;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
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
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.job.sync.file.storage.InMemoryTaskFileStorage;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutorImpl;
import com.bytechef.platform.webhook.executor.WebhookWorkflowSyncExecutor;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.task.dispatcher.approval.WaitForApprovalTaskDispatcher;
import com.bytechef.task.dispatcher.branch.BranchTaskDispatcher;
import com.bytechef.task.dispatcher.branch.completion.BranchTaskCompletionHandler;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import com.bytechef.task.dispatcher.each.EachTaskDispatcher;
import com.bytechef.task.dispatcher.each.completion.EachTaskCompletionHandler;
import com.bytechef.task.dispatcher.fork.join.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.fork.join.completion.ForkJoinTaskCompletionHandler;
import com.bytechef.task.dispatcher.graph.GraphTaskDispatcher;
import com.bytechef.task.dispatcher.graph.completion.GraphTaskCompletionHandler;
import com.bytechef.task.dispatcher.loop.LoopBreakTaskDispatcher;
import com.bytechef.task.dispatcher.loop.LoopTaskDispatcher;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.bytechef.task.dispatcher.map.MapTaskDispatcher;
import com.bytechef.task.dispatcher.map.completion.MapTaskCompletionHandler;
import com.bytechef.task.dispatcher.parallel.ParallelTaskDispatcher;
import com.bytechef.task.dispatcher.parallel.completion.ParallelTaskCompletionHandler;
import com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher;
import com.bytechef.task.dispatcher.subflow.event.listener.SubflowJobStatusEventListener;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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

    /**
     * Unlimited task executions — the sync executors here run real workflows, not bounded test runs.
     */
    private static final int UNLIMITED_TASK_EXECUTIONS = -1;

    /**
     * Completion timeout, in seconds, for synchronous webhook execution.
     */
    private static final long SYNC_EXECUTION_TIMEOUT = 300;

    @Bean
    SseStreamBridgeRegistry sseStreamBridgeRegistry() {
        return new SseStreamBridgeRegistry();
    }

    @Bean
    WebhookWorkflowExecutor webhookExecutor(
        ChildJobPrincipalFactory childJobPrincipalFactory, ContextService contextService,
        CounterService counterService, TaskFileStorage durableTaskFileStorage, Environment environment,
        Evaluator evaluator, ApplicationEventPublisher eventPublisher, JobCompletionAwaiter jobCompletionAwaiter,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, PrincipalJobFacade principalJobFacade,
        JobService jobService, List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors,
        ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider,
        SseStreamBridgeRegistry sseStreamBridgeRegistry, SubflowResolver subflowResolver,
        TaskExecutionService taskExecutionService, @Qualifier("taskExecutor") TaskExecutor taskExecutor,
        TaskHandlerRegistry taskHandlerRegistry, TriggerDefinitionService triggerDefinitionService,
        WebhookWorkflowSyncExecutor triggerSyncExecutor,
        WorkflowService workflowService) {

        TaskFileStorage syncJobTaskFileStorage = new InMemoryTaskFileStorage(durableTaskFileStorage);

        JobSyncExecutor jobSyncExecutor = createJobSyncExecutor(
            childJobPrincipalFactory, contextService, counterService, environment, evaluator, jobService,
            taskDispatcherPreSendProcessors, subflowResolver, taskExecutionService, taskExecutor, taskHandlerRegistry,
            syncJobTaskFileStorage, SYNC_EXECUTION_TIMEOUT, workflowService);

        return new WebhookWorkflowExecutorImpl(
            eventPublisher, jobCompletionAwaiter, jobPrincipalAccessorRegistry, jobSyncExecutor,
            planLimitsProviderObjectProvider, principalJobFacade, sseStreamBridgeRegistry, taskExecutionService,
            durableTaskFileStorage, syncJobTaskFileStorage, triggerDefinitionService, triggerSyncExecutor,
            workflowService, JobCompletionAwaiter.DEFAULT_SYNC_TIMEOUT);
    }

    /**
     * Builds a {@link JobSyncExecutor} over its own in-process {@link AsyncMessageBroker}, so each caller gets an
     * isolated event bus. {@code workflowService} is a parameter rather than a fixed bean because the WebSocket
     * sub-flow executor needs an {@link EphemeralWorkflowService} in its place.
     */
    private JobSyncExecutor createJobSyncExecutor(
        ChildJobPrincipalFactory childJobPrincipalFactory, ContextService contextService,
        CounterService counterService, Environment environment, Evaluator evaluator, JobService jobService,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors, SubflowResolver subflowResolver,
        TaskExecutionService taskExecutionService, TaskExecutor taskExecutor, TaskHandlerRegistry taskHandlerRegistry,
        TaskFileStorage syncJobTaskFileStorage, long timeout, WorkflowService workflowService) {

        AsyncMessageBroker asyncMessageBroker = new AsyncMessageBroker(environment);

        ApplicationEventPublisher coordinatorEventPublisher = createEventPublisher(asyncMessageBroker);

        return new JobSyncExecutor(
            contextService, evaluator, jobService, UNLIMITED_TASK_EXECUTIONS, asyncMessageBroker,
            getAdditionalApplicationEventListeners(
                evaluator, coordinatorEventPublisher, jobService, taskExecutionService, syncJobTaskFileStorage),
            getTaskCompletionHandlerFactories(
                contextService, counterService, evaluator, taskExecutionService, syncJobTaskFileStorage),
            getTaskDispatcherAdapterFactories(evaluator), taskDispatcherPreSendProcessors,
            getTaskDispatcherResolverFactories(
                childJobPrincipalFactory, contextService, counterService, coordinatorEventPublisher, evaluator,
                jobService, subflowResolver, taskExecutionService, syncJobTaskFileStorage),
            taskExecutionService, taskExecutor, taskHandlerRegistry, syncJobTaskFileStorage, timeout,
            workflowService);
    }

    private static ApplicationEventPublisher createEventPublisher(MessageBroker messageBroker) {
        return event -> {
            MessageEvent<?> messageEvent = (MessageEvent<?>) event;

            messageEvent.putMetadata(CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(((MessageEvent<?>) event).getRoute(), event);
        };
    }

    List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
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
            (taskCompletionHandler, taskDispatcher) -> new GraphTaskCompletionHandler(
                contextService, counterService, evaluator, taskCompletionHandler, taskDispatcher, taskExecutionService,
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

    private List<TaskDispatcherAdapterFactory> getTaskDispatcherAdapterFactories(Evaluator evaluator) {
        return List.of(
            new TaskDispatcherAdapterFactory() {

                @Override
                public TaskHandler<?> create(TaskHandlerResolver taskHandlerResolver) {
                    return new MapTaskDispatcherAdapterTaskHandler(evaluator, taskHandlerResolver);
                }

                @Override
                public String getName() {
                    return MapConstants.MAP + "/v1";
                }
            });
    }

    private static List<ApplicationEventListener> getAdditionalApplicationEventListeners(
        Evaluator evaluator, ApplicationEventPublisher coordinatorEventPublisher, JobService jobService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        return List.of(
            new SubflowJobStatusEventListener(
                evaluator, coordinatorEventPublisher, jobService, taskExecutionService, taskFileStorage));
    }

    List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ChildJobPrincipalFactory childJobPrincipalFactory, ContextService contextService,
        CounterService counterService, ApplicationEventPublisher eventPublisher,
        Evaluator evaluator, JobService jobService, SubflowResolver subflowResolver,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        return List.of(
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
            (taskDispatcher) -> new GraphTaskDispatcher(
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
                taskFileStorage),
            (taskDispatcher) -> new SubflowTaskDispatcher(childJobPrincipalFactory, jobService, subflowResolver),
            (taskDispatcher) -> new WaitForApprovalTaskDispatcher(eventPublisher, jobService, taskExecutionService));
    }
}
