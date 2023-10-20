
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

package com.bytechef.hermes.task.dispatcher.test.workflow;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.event.EventPublisher;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Map;

public class TaskDispatcherWorkflowTestSupport {

    private final ContextService contextService;
    private final CounterService counterService;
    private final JobService jobService;
    private final EventPublisher eventPublisher;
    private final TaskExecutionService taskExecutionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TaskDispatcherWorkflowTestSupport(
        ContextService contextService, CounterService counterService, JobService jobService,
        EventPublisher eventPublisher, TaskExecutionService taskExecutionService, WorkflowService workflowService) {

        this.contextService = contextService;
        this.counterService = counterService;
        this.jobService = jobService;
        this.eventPublisher = eventPublisher;
        this.taskExecutionService = taskExecutionService;
        this.workflowService = workflowService;
    }

    public Job execute(
        String workflowId, TaskCompletionHandlerFactoriesFunction taskCompletionHandlerFactoriesFunction,
        TaskDispatcherResolverFactoriesFunction taskDispatcherResolverFactoriesFunction,
        TaskHandlerMapSupplier taskHandlerMapSupplier) {

        return execute(
            workflowId, Map.of(), taskCompletionHandlerFactoriesFunction, taskDispatcherResolverFactoriesFunction,
            taskHandlerMapSupplier);
    }

    public Job execute(
        String workflowId, Map<String, Object> inputs,
        TaskCompletionHandlerFactoriesFunction taskCompletionHandlerFactoriesFunction,
        TaskDispatcherResolverFactoriesFunction taskDispatcherResolverFactoriesFunction,
        TaskHandlerMapSupplier taskHandlerMapSupplier) {

        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        JobSyncExecutor jobSyncExecutor = JobSyncExecutor.builder()
            .contextService(contextService)
            .eventPublisher(eventPublisher)
            .jobService(jobService)
            .syncMessageBroker(syncMessageBroker)
            .taskCompletionHandlerFactories(
                taskCompletionHandlerFactoriesFunction.apply(counterService, taskExecutionService))
            .taskDispatcherResolverFactories(
                taskDispatcherResolverFactoriesFunction
                    .apply(contextService, counterService, syncMessageBroker, taskExecutionService))
            .taskExecutionService(taskExecutionService)
            .taskHandlerAccessor(taskHandlerMapSupplier.get()::get)
            .workflowService(workflowService)
            .build();

        return jobSyncExecutor.execute(new JobParameters(inputs, workflowId));
    }

    @FunctionalInterface
    public interface TaskCompletionHandlerFactoriesFunction {
        List<TaskCompletionHandlerFactory> apply(
            CounterService counterService, TaskExecutionService taskExecutionService);
    }

    @FunctionalInterface
    public interface TaskDispatcherResolverFactoriesFunction {
        List<TaskDispatcherResolverFactory> apply(
            ContextService contextService, CounterService counterService, MessageBroker messageBroker,
            TaskExecutionService taskExecutionService);
    }

    @FunctionalInterface
    public interface TaskHandlerMapSupplier {
        Map<String, TaskHandler<?>> get();
    }
}
