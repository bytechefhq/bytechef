/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.JobStatus;
import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.messagebroker.Queues;
import com.integri.atlas.engine.core.messagebroker.SyncMessageBroker;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.repository.yaml.workflow.ResourceBasedWorkflowRepository;
import com.integri.atlas.engine.worker.task.DefaultTaskHandlerResolver;
import com.integri.atlas.engine.core.task.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.TaskHandler;
import com.integri.atlas.engine.coordinator.task.WorkTaskDispatcher;
import com.integri.atlas.taskhandler.io.Print;
import com.integri.atlas.taskhandler.random.RandomInt;
import com.integri.atlas.taskhandler.time.Sleep;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.integri.atlas.engine.worker.Worker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CoordinatorIntTest {

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskExecutionRepository taskRepository;

    @Test
    public void testStartJob() throws SQLException {
        Coordinator coordinator = new Coordinator();

        SyncMessageBroker messageBroker = new SyncMessageBroker();
        messageBroker.receive(Queues.COMPLETIONS, o -> coordinator.complete((TaskExecution) o));
        messageBroker.receive(Queues.JOBS, o -> coordinator.start((Job) o));

        Map<String, TaskHandler<?>> handlers = new HashMap<>();
        handlers.put("io/print", new Print());
        handlers.put("random/int", new RandomInt());
        handlers.put("time/sleep", new Sleep());

        DefaultTaskHandlerResolver taskHandlerResolver = new DefaultTaskHandlerResolver(handlers);

        Worker worker = Worker
            .builder()
            .withTaskHandlerResolver(taskHandlerResolver)
            .withMessageBroker(messageBroker)
            .withEventPublisher(e -> {})
            .withTaskEvaluator(SpelTaskEvaluator.create())
            .build();

        coordinator.setContextRepository(contextRepository);

        coordinator.setJobRepository(jobRepository);
        coordinator.setWorkflowRepository(new ResourceBasedWorkflowRepository());
        coordinator.setJobTaskRepository(taskRepository);

        SyncMessageBroker coordinatorMessageBroker = new SyncMessageBroker();
        coordinatorMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));
        WorkTaskDispatcher taskDispatcher = new WorkTaskDispatcher(coordinatorMessageBroker);
        coordinator.setTaskDispatcher(taskDispatcher);
        coordinator.setEventPublisher(e -> {});

        DefaultJobExecutor jobExecutor = new DefaultJobExecutor();
        jobExecutor.setContextRepository(contextRepository);
        jobExecutor.setJobTaskRepository(taskRepository);
        jobExecutor.setWorkflowRepository(new ResourceBasedWorkflowRepository());
        jobExecutor.setTaskDispatcher(taskDispatcher);
        jobExecutor.setTaskEvaluator(SpelTaskEvaluator.create());
        coordinator.setJobExecutor(jobExecutor);

        DefaultTaskCompletionHandler taskCompletionHandler = new DefaultTaskCompletionHandler();
        taskCompletionHandler.setContextRepository(contextRepository);
        taskCompletionHandler.setJobExecutor(jobExecutor);
        taskCompletionHandler.setJobRepository(jobRepository);
        taskCompletionHandler.setJobTaskRepository(taskRepository);
        taskCompletionHandler.setWorkflowRepository(new ResourceBasedWorkflowRepository());
        taskCompletionHandler.setEventPublisher(e -> {});
        taskCompletionHandler.setTaskEvaluator(SpelTaskEvaluator.create());
        coordinator.setTaskCompletionHandler(taskCompletionHandler);
        coordinator.setMessageBroker(messageBroker);

        Job job = coordinator.create(
            MapObject.of(
                ImmutableMap.of("workflowId", "demo/hello", "inputs", Collections.singletonMap("yourName", "me"))
            )
        );

        Job completedJob = jobRepository.getById(job.getId());

        Assertions.assertEquals(JobStatus.COMPLETED, completedJob.getStatus());
    }

    @Test
    public void testRequiredParams() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> {
                Coordinator coordinator = new Coordinator();
                coordinator.setWorkflowRepository(new ResourceBasedWorkflowRepository());
                coordinator.create(MapObject.of(Collections.singletonMap("workflowId", "demo/hello")));
            }
        );
    }
}
