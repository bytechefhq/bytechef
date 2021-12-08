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

package com.integri.atlas.task.handler;

import com.integri.atlas.engine.coordinator.Coordinator;
import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.executor.DefaultJobExecutor;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.engine.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.integri.atlas.engine.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.integri.atlas.engine.coordinator.workflow.repository.JSONWorkflowMapper;
import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.binary.BinaryHelper;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.error.Error;
import com.integri.atlas.engine.core.json.JSONHelper;
import com.integri.atlas.engine.core.message.broker.Queues;
import com.integri.atlas.engine.core.storage.StorageService;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.engine.worker.Worker;
import com.integri.atlas.engine.worker.task.handler.DefaultTaskHandlerResolver;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.message.broker.sync.SyncMessageBroker;
import com.integri.atlas.workflow.repository.resource.ResourceBasedWorkflowRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
public class BaseTaskHandlerIntTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseTaskHandlerIntTest.class);

    @Autowired
    protected BinaryHelper binaryHelper;

    @Autowired
    protected ContextRepository contextRepository;

    @Autowired
    protected JobRepository jobRepository;

    @Autowired
    protected JSONHelper jsonHelper;

    @Autowired
    protected StorageService storageService;

    @Autowired
    protected TaskExecutionRepository taskRepository;

    protected Job startJob(String workflowId, Map<String, TaskHandler<?>> handlers, Map<String, ?> inputs) {
        Coordinator coordinator = new Coordinator();

        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(Queues.COMPLETIONS, o -> coordinator.complete((TaskExecution) o));
        messageBroker.receive(Queues.JOBS, o -> coordinator.start((Job) o));
        messageBroker.receive(
            Queues.ERRORS,
            message -> {
                TaskExecution erringTask = (TaskExecution) message;

                Error error = erringTask.getError();

                logger.error(error.getMessage());
            }
        );

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
        coordinator.setWorkflowRepository(new ResourceBasedWorkflowRepository(new JSONWorkflowMapper()));
        coordinator.setJobTaskRepository(taskRepository);

        SyncMessageBroker coordinatorMessageBroker = new SyncMessageBroker();

        coordinatorMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));

        DefaultTaskDispatcher taskDispatcher = new DefaultTaskDispatcher(coordinatorMessageBroker);

        coordinator.setTaskDispatcher(taskDispatcher);
        coordinator.setEventPublisher(e -> {});

        DefaultJobExecutor jobExecutor = new DefaultJobExecutor();

        jobExecutor.setContextRepository(contextRepository);
        jobExecutor.setJobTaskRepository(taskRepository);
        jobExecutor.setWorkflowRepository(new ResourceBasedWorkflowRepository(new JSONWorkflowMapper()));
        jobExecutor.setTaskDispatcher(taskDispatcher);
        jobExecutor.setTaskEvaluator(SpelTaskEvaluator.create());

        coordinator.setJobExecutor(jobExecutor);

        DefaultTaskCompletionHandler taskCompletionHandler = new DefaultTaskCompletionHandler();

        taskCompletionHandler.setContextRepository(contextRepository);
        taskCompletionHandler.setJobExecutor(jobExecutor);
        taskCompletionHandler.setJobRepository(jobRepository);
        taskCompletionHandler.setJobTaskRepository(taskRepository);
        taskCompletionHandler.setWorkflowRepository(new ResourceBasedWorkflowRepository(new JSONWorkflowMapper()));
        taskCompletionHandler.setEventPublisher(e -> {});
        taskCompletionHandler.setTaskEvaluator(SpelTaskEvaluator.create());

        coordinator.setTaskCompletionHandler(taskCompletionHandler);
        coordinator.setMessageBroker(messageBroker);

        Job job = coordinator.create(MapObject.of(Map.of("workflowId", workflowId, "inputs", inputs)));

        return jobRepository.getById(job.getId());
    }
}
