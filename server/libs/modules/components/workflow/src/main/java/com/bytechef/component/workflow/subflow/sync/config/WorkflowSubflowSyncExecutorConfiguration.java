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

package com.bytechef.component.workflow.subflow.sync.config;

import static com.bytechef.tenant.constant.TenantConstants.CURRENT_TENANT_ID;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.map.MapTaskDispatcherAdapterTaskHandler;
import com.bytechef.component.map.constant.MapConstants;
import com.bytechef.component.workflow.subflow.sync.SubflowSyncExecutor;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.job.sync.executor.SyncExecutionSuspendRejectingTaskCompletionHandler;
import com.bytechef.platform.job.sync.file.storage.InMemoryTaskFileStorage;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
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
import com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher;
import com.bytechef.task.dispatcher.subflow.event.listener.AgentSubflowLauncher;
import com.bytechef.task.dispatcher.subflow.event.listener.SubflowJobStatusEventListener;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkflowSubflowSyncExecutorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WorkflowSubflowSyncExecutorConfiguration.class);

    private static final String SUSPENDING_TASK_MESSAGE =
        "A sub-workflow called synchronously (in the editor, or from a workflow that returns a synchronous " +
            "response) can't pause. Step '%s' (%s) would pause it. Run the calling workflow asynchronously, or " +
            "remove the step.";

    @Bean
    SubflowSyncExecutor subflowSyncExecutor(
        ApplicationProperties applicationProperties, ChildJobPrincipalFactory childJobPrincipalFactory,
        ContextService contextService, CounterService counterService, TaskFileStorage durableTaskFileStorage,
        Environment environment, Evaluator evaluator, JobService jobService, SubflowResolver subflowResolver,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors, TaskExecutionService taskExecutionService,
        TaskHandlerRegistry taskHandlerRegistry, WorkflowService workflowService) {

        int maxSubflowDepth = getMaxSubflowDepth(applicationProperties);

        AsyncMessageBroker asyncMessageBroker = new AsyncMessageBroker(environment);

        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

        TaskFileStorage taskFileStorage = new InMemoryTaskFileStorage(durableTaskFileStorage);

        ApplicationEventPublisher coordinatorEventPublisher = createEventPublisher(asyncMessageBroker);

        JobSyncExecutor jobSyncExecutor = new JobSyncExecutor(
            contextService, evaluator, jobService, -1, asyncMessageBroker,
            getAdditionalApplicationEventListeners(
                evaluator, coordinatorEventPublisher, jobService, taskExecutionService, taskFileStorage),
            getTaskCompletionHandlerFactories(
                contextService, coordinatorEventPublisher, counterService, evaluator, jobService, taskExecutionService,
                taskFileStorage),
            getTaskDispatcherAdapterFactories(evaluator), taskDispatcherPreSendProcessors,
            getTaskDispatcherResolverFactories(
                childJobPrincipalFactory, contextService, counterService, coordinatorEventPublisher, evaluator,
                jobService, subflowResolver, taskExecutionService, taskFileStorage),
            taskExecutionService, taskExecutor, taskHandlerRegistry, taskFileStorage, 300, workflowService);

        return new SubflowSyncExecutor() {

            @Override
            public @Nullable Object execute(
                String workflowUuid, String triggerName, Map<String, ?> inputs, boolean editorEnvironment,
                @Nullable Long callerJobId) {

                int depth = getSubflowDepth(jobService, callerJobId) + 1;

                if (depth > maxSubflowDepth) {
                    throw new IllegalStateException(AgentSubflowLauncher.tooDeepMessage(maxSubflowDepth));
                }

                Subflow subflow = subflowResolver.resolveSubflow(workflowUuid, triggerName, editorEnvironment);

                Map<String, Object> jobInputs = Map.of(subflow.inputsName(), inputs);

                Job job = jobSyncExecutor.execute(
                    new JobParametersDTO(
                        subflow.workflowId(), jobInputs, Map.of(SubflowRequestConstants.SUBFLOW_DEPTH, depth)),
                    true);

                if (job.getOutputs() == null) {
                    return null;
                }

                return getCallableResponseOutput(job, taskExecutionService, taskFileStorage)
                    .orElseGet(() -> taskFileStorage.readJobOutputs(job.getOutputs()));
            }

            @Override
            public boolean isSyncJob(long jobId) {
                return jobService.fetchJob(jobId)
                    .map(job -> MapUtils.getBoolean(job.getMetadata(), MetadataConstants.SYNC_EXECUTION, false))
                    .orElse(false);
            }
        };
    }

    static String suspendingTaskMessage(String taskName, String taskType) {
        return SUSPENDING_TASK_MESSAGE.formatted(taskName, taskType);
    }

    private static int getMaxSubflowDepth(ApplicationProperties applicationProperties) {
        ApplicationProperties.Workflow workflow = applicationProperties.getWorkflow();
        ApplicationProperties.Workflow.Execution execution = workflow.getExecution();
        ApplicationProperties.Workflow.Execution.Agent agent = execution.getAgent();

        return agent.getMaxSubflowDepth();
    }

    private static int getSubflowDepth(JobService jobService, @Nullable Long callerJobId) {
        if (callerJobId == null) {
            return 0;
        }

        return jobService.fetchJob(callerJobId)
            .map(job -> MapUtils.getInteger(job.getMetadata(), SubflowRequestConstants.SUBFLOW_DEPTH, 0))
            .orElse(0);
    }

    private static ApplicationEventPublisher createEventPublisher(MessageBroker messageBroker) {
        return event -> {
            MessageEvent<?> messageEvent = (MessageEvent<?>) event;

            messageEvent.putMetadata(CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(((MessageEvent<?>) event).getRoute(), event);
        };
    }

    private static List<ApplicationEventListener> getAdditionalApplicationEventListeners(
        Evaluator evaluator, ApplicationEventPublisher coordinatorEventPublisher, JobService jobService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        return List.of(
            new SubflowJobStatusEventListener(
                evaluator, coordinatorEventPublisher, jobService, taskExecutionService, taskFileStorage));
    }

    private static Optional<Object> getCallableResponseOutput(
        Job job, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        return taskExecutionService.fetchLastJobTaskExecution(Objects.requireNonNull(job.getId()))
            .filter(
                lastTaskExecution -> {
                    Map<String, ?> metadata = lastTaskExecution.getMetadata();

                    return metadata.containsKey(MetadataConstants.CALLABLE_RESPONSE);
                })
            .map(lastTaskExecution -> {
                ActionDefinition.CallableResponse callableResponse = ConvertUtils.convertValue(
                    taskFileStorage.readTaskExecutionOutput(lastTaskExecution.getOutput()),
                    ActionDefinition.CallableResponse.class);

                return callableResponse.output();
            });
    }

    List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, ApplicationEventPublisher eventPublisher, CounterService counterService,
        Evaluator evaluator, JobService jobService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new SyncExecutionSuspendRejectingTaskCompletionHandler(
                eventPublisher, jobService, taskExecutionService,
                WorkflowSubflowSyncExecutorConfiguration::suspendingTaskMessage),
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

    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ChildJobPrincipalFactory childJobPrincipalFactory, ContextService contextService, CounterService counterService,
        ApplicationEventPublisher eventPublisher, Evaluator evaluator, JobService jobService,
        SubflowResolver subflowResolver, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

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
