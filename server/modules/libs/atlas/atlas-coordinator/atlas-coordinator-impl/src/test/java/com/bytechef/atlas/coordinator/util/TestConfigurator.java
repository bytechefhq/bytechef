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

package com.bytechef.atlas.coordinator.util;

import com.bytechef.atlas.MapObject;
import com.bytechef.atlas.coordinator.CoordinatorImpl;
import com.bytechef.atlas.coordinator.job.executor.DefaultJobExecutor;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.repository.resource.workflow.ResourceBasedWorkflowRepository;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowMapper;
import com.bytechef.atlas.service.context.ContextService;
import com.bytechef.atlas.service.job.JobService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.service.workflow.WorkflowService;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.WorkerImpl;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TestConfigurator {

    @Autowired
    private ContextService contextService;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    public Job startJob(String workflowId, WorkflowMapper workflowMapper) {
        CoordinatorImpl coordinator = new CoordinatorImpl();

        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(Queues.COMPLETIONS, o -> coordinator.complete((TaskExecution) o));
        messageBroker.receive(Queues.JOBS, o -> coordinator.start((Job) o));

        Map<String, TaskHandler<?>> taskHandlerMap = new HashMap<>();

        taskHandlerMap.put("io/print", taskExecution -> null);
        taskHandlerMap.put("random/int", taskExecution -> null);
        taskHandlerMap.put("time/sleep", taskExecution -> null);

        TaskHandlerResolver taskHandlerResolver = new DefaultTaskHandlerResolver(taskHandlerMap);

        Worker worker = WorkerImpl.builder()
                .withTaskHandlerResolver(taskHandlerResolver)
                .withMessageBroker(messageBroker)
                .withEventPublisher(eventPublisher)
                .withTaskEvaluator(SpelTaskEvaluator.create())
                .build();

        coordinator.setContextService(contextService);
        coordinator.setJobService(jobService);

        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        syncMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));

        DefaultTaskDispatcher taskDispatcher = new DefaultTaskDispatcher(syncMessageBroker, List.of());

        coordinator.setEventPublisher(eventPublisher);
        coordinator.setTaskDispatcher(taskDispatcher);

        DefaultJobExecutor jobExecutor = new DefaultJobExecutor();

        jobExecutor.setContextService(contextService);
        jobExecutor.setTaskExecutionService(taskExecutionService);
        jobExecutor.setTaskDispatcher(taskDispatcher);
        jobExecutor.setTaskEvaluator(SpelTaskEvaluator.create());
        jobExecutor.setWorkflowService(new WorkflowService(new ResourceBasedWorkflowRepository(workflowMapper)));

        coordinator.setJobExecutor(jobExecutor);

        DefaultTaskCompletionHandler taskCompletionHandler = new DefaultTaskCompletionHandler();

        taskCompletionHandler.setContextService(contextService);
        taskCompletionHandler.setEventPublisher(eventPublisher);
        taskCompletionHandler.setJobExecutor(jobExecutor);
        taskCompletionHandler.setJobService(jobService);
        taskCompletionHandler.setTaskExecutionService(taskExecutionService);
        taskCompletionHandler.setTaskEvaluator(SpelTaskEvaluator.create());
        taskCompletionHandler.setWorkflowService(
                new WorkflowService(new ResourceBasedWorkflowRepository(workflowMapper)));

        coordinator.setMessageBroker(messageBroker);
        coordinator.setTaskCompletionHandler(taskCompletionHandler);

        Job job = coordinator.create(
                MapObject.of(Map.of("workflowId", workflowId, "inputs", Collections.singletonMap("yourName", "me"))));

        return jobService.getJob(job.getId());
    }
}
