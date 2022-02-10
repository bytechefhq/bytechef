/*
 * Copyright 2021 <your company/name>.
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
 */

package com.integri.atlas.task.dispatcher.if_;

import com.google.common.collect.ImmutableMap;
import com.integri.atlas.engine.coordinator.Coordinator;
import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.executor.DefaultJobExecutor;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.engine.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandlerChain;
import com.integri.atlas.engine.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.integri.atlas.engine.coordinator.task.dispatcher.TaskDispatcherChain;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowMapper;
import com.integri.atlas.engine.coordinator.workflow.repository.YAMLWorkflowMapper;
import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.context.Context;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.message.broker.Queues;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.engine.worker.Worker;
import com.integri.atlas.engine.worker.task.handler.DefaultTaskHandlerResolver;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.engine.worker.task.handler.TaskHandlerResolverChain;
import com.integri.atlas.message.broker.sync.SyncMessageBroker;
import com.integri.atlas.task.dispatcher.if_.completion.IfTaskCompletionHandler;
import com.integri.atlas.task.handler.core.Var;
import com.integri.atlas.workflow.repository.resource.ResourceBasedWorkflowRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Matija Petanjek
 */
@SpringBootTest
public class IfTaskDispatcherTest {

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskExecutionRepository taskRepository;

    private Coordinator coordinator;

    @BeforeEach
    public void setUp() {
        coordinator = new Coordinator();

        SyncMessageBroker messageBroker = new SyncMessageBroker();
        messageBroker.receive(Queues.COMPLETIONS, o -> coordinator.complete((TaskExecution) o));
        messageBroker.receive(Queues.JOBS, o -> coordinator.start((Job) o));

        Map<String, TaskHandler<?>> handlers = new HashMap<>();
        handlers.put("core/var", new Var());

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain(
            Arrays.asList(new DefaultTaskHandlerResolver(handlers))
        );

        Worker worker = Worker
            .builder()
            .withTaskHandlerResolver(taskHandlerResolverChain)
            .withMessageBroker(messageBroker)
            .withEventPublisher(e -> {})
            .withTaskEvaluator(SpelTaskEvaluator.create())
            .build();

        WorkflowMapper workflowMapper = new YAMLWorkflowMapper();

        coordinator.setContextRepository(contextRepository);
        coordinator.setJobRepository(jobRepository);
        coordinator.setWorkflowRepository(new ResourceBasedWorkflowRepository(workflowMapper));
        coordinator.setJobTaskRepository(taskRepository);

        SyncMessageBroker coordinatorMessageBroker = new SyncMessageBroker();
        coordinatorMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setResolvers(
            Arrays.asList(
                new IfTaskDispatcher(
                    contextRepository,
                    coordinatorMessageBroker,
                    taskDispatcherChain,
                    taskRepository,
                    SpelTaskEvaluator.create()
                ),
                new DefaultTaskDispatcher(coordinatorMessageBroker)
            )
        );

        coordinator.setTaskDispatcher(taskDispatcherChain);
        coordinator.setEventPublisher(e -> {});

        DefaultJobExecutor jobExecutor = new DefaultJobExecutor();
        jobExecutor.setContextRepository(contextRepository);
        jobExecutor.setJobTaskRepository(taskRepository);
        jobExecutor.setWorkflowRepository(new ResourceBasedWorkflowRepository(workflowMapper));
        jobExecutor.setTaskDispatcher(taskDispatcherChain);
        jobExecutor.setTaskEvaluator(SpelTaskEvaluator.create());
        coordinator.setJobExecutor(jobExecutor);

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        IfTaskCompletionHandler ifTaskCompletionHandler = new IfTaskCompletionHandler(
            taskRepository,
            taskCompletionHandlerChain,
            taskDispatcherChain,
            contextRepository,
            SpelTaskEvaluator.create()
        );

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler();
        defaultTaskCompletionHandler.setContextRepository(contextRepository);
        defaultTaskCompletionHandler.setJobExecutor(jobExecutor);
        defaultTaskCompletionHandler.setJobRepository(jobRepository);
        defaultTaskCompletionHandler.setJobTaskRepository(taskRepository);
        defaultTaskCompletionHandler.setWorkflowRepository(new ResourceBasedWorkflowRepository(workflowMapper));
        defaultTaskCompletionHandler.setEventPublisher(e -> {});
        defaultTaskCompletionHandler.setTaskEvaluator(SpelTaskEvaluator.create());

        taskCompletionHandlerChain.setTaskCompletionHandlers(
            Arrays.asList(ifTaskCompletionHandler, defaultTaskCompletionHandler)
        );

        coordinator.setTaskCompletionHandler(taskCompletionHandlerChain);
        coordinator.setMessageBroker(messageBroker);
    }

    @Test
    public void testIfTaskDispatcher_Boolean() {
        Job job = coordinator.create(
            MapObject.of(
                ImmutableMap.of(
                    "workflowId",
                    "conditions-boolean.yaml",
                    "inputs",
                    Map.of("value1", "true", "value2", "false")
                )
            )
        );

        Context context = contextRepository.peek(job.getId());

        Assertions.assertEquals("false branch", context.get("equalsResult"));
        Assertions.assertEquals("true branch", context.get("notEqualsResult"));
    }

    @Test
    public void testIfTaskDispatcher_DateTime() {
        Job job = coordinator.create(
            MapObject.of(
                ImmutableMap.of(
                    "workflowId",
                    "conditions-dateTime.yaml",
                    "inputs",
                    Map.of("value1", "2022-01-01T00:00:00", "value2", "2022-01-01T00:00:01")
                )
            )
        );

        Context context = contextRepository.peek(job.getId());

        Assertions.assertEquals("false branch", context.get("afterResult"));
        Assertions.assertEquals("true branch", context.get("beforeResult"));
    }

    @Test
    public void testIfTaskDispatcher_Number() {
        Job job = coordinator.create(
            MapObject.of(
                ImmutableMap.of("workflowId", "conditions-number.yaml", "inputs", Map.of("value1", 100, "value2", 200))
            )
        );

        Context context = contextRepository.peek(job.getId());

        Assertions.assertEquals("false branch", context.get("equalsResult"));
        Assertions.assertEquals("true branch", context.get("notEqualsResult"));
        Assertions.assertEquals("false branch", context.get("greaterResult"));
        Assertions.assertEquals("true branch", context.get("lessResult"));
        Assertions.assertEquals("false branch", context.get("greaterEqualsResult"));
        Assertions.assertEquals("true branch", context.get("lessEqualsResult"));
    }

    @Test
    public void testIfTaskDispatcher_String() {
        Job job = coordinator.create(
            MapObject.of(
                ImmutableMap.of(
                    "workflowId",
                    "conditions-string.yaml",
                    "inputs",
                    Map.of("value1", "Hello World", "value2", "Hello")
                )
            )
        );

        Context context = contextRepository.peek(job.getId());

        Assertions.assertEquals("false branch", context.get("equalsResult"));
        Assertions.assertEquals("true branch", context.get("notEqualsResult"));
        Assertions.assertEquals("true branch", context.get("containsResult"));
        Assertions.assertEquals("false branch", context.get("notContainsResult"));
        Assertions.assertEquals("true branch", context.get("startsWithResult"));
        Assertions.assertEquals("false branch", context.get("endsWithResult"));
        Assertions.assertEquals("false branch", context.get("isEmptyResult"));
        Assertions.assertEquals("false branch", context.get("regexResult"));
    }
}
