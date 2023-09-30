
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

package com.bytechef.component.map;

import com.bytechef.atlas.coordinator.message.route.CoordinatorMessageRoute;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacadeImpl;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.CounterServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.component.map.concurrency.CurrentThreadExecutorService;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.task.dispatcher.map.MapTaskDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Feb, 21 2020
 */
public class MapTaskDispatcherAdapterTaskHandler implements TaskHandler<List<?>> {

    private final ObjectMapper objectMapper;
    private final TaskHandlerResolver taskHandlerResolver;

    @SuppressFBWarnings("EI")
    public MapTaskDispatcherAdapterTaskHandler(ObjectMapper objectMapper, TaskHandlerResolver taskHandlerResolver) {
        this.objectMapper = objectMapper;

        this.taskHandlerResolver = taskHandlerResolver;
    }

    @Override
    @SuppressFBWarnings("NP")
    public List<?> handle(TaskExecution taskExecution) {
        List<Object> result = new ArrayList<>();

        SyncMessageBroker syncMessageBroker = new SyncMessageBroker(objectMapper);
        WorkflowFileStorageFacade workflowFileStorageFacade = new WorkflowFileStorageFacadeImpl(
            new Base64FileStorageService(), objectMapper);

        syncMessageBroker.receive(CoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS, message -> {
            TaskExecution completionTaskExecution = ((TaskExecutionCompleteEvent) message).getTaskExecution();

            result.add(workflowFileStorageFacade.readTaskExecutionOutput(completionTaskExecution.getOutput()));
        });

        List<ExecutionError> errors = Collections.synchronizedList(new ArrayList<>());

        syncMessageBroker.receive(
            CoordinatorMessageRoute.ERROR_EVENTS, message -> {
                TaskExecution erroredTaskExecution = ((TaskExecutionErrorEvent) message).getTaskExecution();

                ExecutionError error = erroredTaskExecution.getError();

                errors.add(error);
            });

        syncMessageBroker.receive(CoordinatorMessageRoute.APPLICATION_EVENTS,
            e -> {});

        TaskWorker worker = new TaskWorker(
            getEventPublisher(syncMessageBroker), new CurrentThreadExecutorService(),
            taskHandlerResolver, workflowFileStorageFacade);

        RemoteTaskExecutionService taskExecutionService =
            new TaskExecutionServiceImpl(new InMemoryTaskExecutionRepository());

        taskExecution = taskExecutionService.create(taskExecution);

        RemoteContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());

        contextService.push(
            Objects.requireNonNull(taskExecution.getId()), Context.Classname.TASK_EXECUTION,
            workflowFileStorageFacade.storeTaskExecutionOutput(taskExecution.getId(), Collections.emptyMap()));

        MapTaskDispatcher mapTaskDispatcher = new MapTaskDispatcher(
            event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
            contextService, new CounterServiceImpl(new InMemoryCounterRepository()),
            curTaskExecution -> worker.onTaskExecutionEvent(new TaskExecutionEvent(curTaskExecution)),
            taskExecutionService, workflowFileStorageFacade);

        mapTaskDispatcher.dispatch(taskExecution);

        if (!errors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();

            for (ExecutionError error : errors) {
                if (errorMessage.length() > 3000) {
                    errorMessage
                        .append("\n")
                        .append("...");

                    break;
                }

                if (!errorMessage.isEmpty()) {
                    errorMessage.append("\n");
                }

                errorMessage
                    .append(error.getMessage())
                    .append("\n")
                    .append(String.join("\n", error.getStackTrace()));
            }

            throw new RuntimeException(errorMessage.toString());
        }

        return result;
    }

    private static ApplicationEventPublisher getEventPublisher(SyncMessageBroker syncMessageBroker) {
        return event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event);
    }
}
