/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.coordinator;

import com.bytechef.atlas.MapObject;
import com.bytechef.atlas.context.service.ContextService;
import com.bytechef.atlas.coordinator.job.executor.DefaultJobExecutor;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.atlas.job.service.JobService;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.WorkerImpl;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.atlas.workflow.repository.mapper.JSONWorkflowMapper;
import com.bytechef.atlas.workflow.repository.mapper.WorkflowMapper;
import com.bytechef.atlas.workflow.repository.mapper.YAMLWorkflowMapper;
import com.bytechef.atlas.workflow.repository.resource.ResourceBasedWorkflowRepository;
import com.bytechef.atlas.workflow.service.WorkflowService;
import com.bytechef.task.execution.repository.TaskExecutionRepository;
import com.bytechef.task.execution.service.TaskExecutionService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Arik Cohen
 */
@SpringBootTest
public class CoordinatorIntTest {

    @Autowired
    private ContextService contextService;

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Test
    public void testStartJob_JSON() {
        testStartJob("samples/hello.json", new JSONWorkflowMapper());
    }

    @Test
    public void testStartJob_YAML() {
        testStartJob("samples/hello.yaml", new YAMLWorkflowMapper());
    }

    public void testStartJob(String workflowId, WorkflowMapper workflowMapper) {
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
                .withEventPublisher(e -> {})
                .withTaskEvaluator(SpelTaskEvaluator.create())
                .build();

        coordinator.setContextService(contextService);
        coordinator.setJobService(jobService);

        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        syncMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));

        DefaultTaskDispatcher taskDispatcher = new DefaultTaskDispatcher(syncMessageBroker, List.of());

        coordinator.setEventPublisher(e -> {});
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
        taskCompletionHandler.setEventPublisher(e -> {});
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

        Job completedJob = jobService.getJob(job.getId());

        Assertions.assertEquals(JobStatus.COMPLETED, completedJob.getStatus());
    }

    @Test
    public void testRequiredParams() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CoordinatorImpl coordinator = new CoordinatorImpl();

            coordinator.setJobService(new JobService(
                    null, taskExecutionRepository, new ResourceBasedWorkflowRepository(new JSONWorkflowMapper())));

            coordinator.create(MapObject.of(Collections.singletonMap("workflowId", "samples/hello.json")));
        });
    }
}
