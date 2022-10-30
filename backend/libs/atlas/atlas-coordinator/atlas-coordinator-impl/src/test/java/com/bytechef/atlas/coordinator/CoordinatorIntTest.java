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

import com.bytechef.atlas.constants.WorkflowConstants;
import com.bytechef.atlas.coordinator.job.executor.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.repository.JobRepository;
import com.bytechef.atlas.repository.classpath.ClassPathResourceWorkflowRepository;
import com.bytechef.atlas.repository.workflow.mapper.JsonWorkflowMapper;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowMapper;
import com.bytechef.atlas.repository.workflow.mapper.YamlWorkflowMapper;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.service.impl.JobServiceImpl;
import com.bytechef.atlas.task.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@SpringBootTest
public class CoordinatorIntTest {

    private static final Logger logger = LoggerFactory.getLogger(CoordinatorIntTest.class);

    @Autowired
    private ContextService contextService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private WorkflowService workflowService;

    @Test
    public void testExecuteWorkflowJson() {
        Job completedJob = executeWorkflow("hello", new JsonWorkflowMapper());

        Assertions.assertEquals(JobStatus.COMPLETED, completedJob.getStatus());
    }

    @Test
    public void testExecuteWorkflowYaml() {
        Job completedJob = executeWorkflow("hello", new YamlWorkflowMapper());

        Assertions.assertEquals(JobStatus.COMPLETED, completedJob.getStatus());
    }

    @Test
    public void testRequiredParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Coordinator coordinator = new Coordinator(
                    contextService,
                    null,
                    null,
                    null,
                    new JobServiceImpl(
                            jobRepository, new ClassPathResourceWorkflowRepository(new JsonWorkflowMapper())),
                    null,
                    null,
                    null,
                    taskExecutionService);

            coordinator.create(Collections.singletonMap("workflowId", "hello"));
        });
    }

    public Job executeWorkflow(String workflowId, WorkflowMapper workflowMapper) {
        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(Queues.ERRORS, message -> {
            TaskExecution erringTask = (TaskExecution) message;

            ExecutionError error = erringTask.getError();

            logger.error(error.getMessage());
        });

        Map<String, TaskHandler<?>> taskHandlerMap = new HashMap<>();

        taskHandlerMap.put("randomHelper/v1/randomInt", taskExecution -> null);

        TaskHandlerResolver taskHandlerResolver = new DefaultTaskHandlerResolver(taskHandlerMap);

        Worker worker = Worker.builder()
                .withTaskHandlerResolver(taskHandlerResolver)
                .withMessageBroker(messageBroker)
                .withEventPublisher(e -> {})
                .withTaskEvaluator(SpelTaskEvaluator.create())
                .build();

        SyncMessageBroker coordinatorMessageBroker = new SyncMessageBroker();

        coordinatorMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));

        DefaultTaskDispatcher taskDispatcher = new DefaultTaskDispatcher(coordinatorMessageBroker, List.of());

        JobExecutor jobExecutor = new JobExecutor(
                contextService, taskDispatcher, taskExecutionService, SpelTaskEvaluator.create(), workflowService);

        JobService jobService =
                new JobServiceImpl(jobRepository, new ClassPathResourceWorkflowRepository(workflowMapper));

        DefaultTaskCompletionHandler taskCompletionHandler = new DefaultTaskCompletionHandler(
                contextService,
                e -> {},
                jobExecutor,
                jobService,
                SpelTaskEvaluator.create(),
                taskExecutionService,
                workflowService);

        Coordinator coordinator = new Coordinator(
                contextService,
                e -> {},
                e -> {},
                jobExecutor,
                jobService,
                messageBroker,
                taskCompletionHandler,
                taskDispatcher,
                taskExecutionService);

        messageBroker.receive(Queues.COMPLETIONS, o -> coordinator.complete((TaskExecution) o));
        messageBroker.receive(Queues.JOBS, jobId -> coordinator.start((String) jobId));

        String jobId = UUID.randomUUID().toString().replaceAll("-", "");

        coordinator.create(Map.of(
                WorkflowConstants.ID,
                jobId,
                "workflowId",
                workflowId,
                "inputs",
                Collections.singletonMap("yourName", "me")));

        return jobService.getJob(jobId);
    }
}
