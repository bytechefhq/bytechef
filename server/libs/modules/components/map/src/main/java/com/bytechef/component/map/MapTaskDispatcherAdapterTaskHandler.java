/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.component.map;

import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.CounterServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.component.map.concurrency.CurrentThreadExecutorService;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.task.dispatcher.map.MapTaskDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;

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
    public List<?> handle(TaskExecution taskExecution) {
        List<Object> result = new ArrayList<>();

        SyncMessageBroker syncMessageBroker = new SyncMessageBroker(objectMapper);
        TaskFileStorage taskFileStorage = new TaskFileStorageImpl(
            new Base64FileStorageService(), objectMapper);

        syncMessageBroker.receive(TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS, message -> {
            TaskExecution completionTaskExecution = ((TaskExecutionCompleteEvent) message).getTaskExecution();

            result.add(taskFileStorage.readTaskExecutionOutput(completionTaskExecution.getOutput()));
        });

        List<ExecutionError> errors = Collections.synchronizedList(new ArrayList<>());

        syncMessageBroker.receive(
            TaskCoordinatorMessageRoute.ERROR_EVENTS, message -> {
                TaskExecution erroredTaskExecution = ((TaskExecutionErrorEvent) message).getTaskExecution();

                ExecutionError error = erroredTaskExecution.getError();

                errors.add(error);
            });

        syncMessageBroker.receive(TaskCoordinatorMessageRoute.APPLICATION_EVENTS, e -> {});

        TaskExecutionService taskExecutionService =
            new TaskExecutionServiceImpl(new InMemoryTaskExecutionRepository());

        taskExecution = taskExecutionService.create(taskExecution);

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());

        contextService.push(
            Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION,
            taskFileStorage.storeTaskExecutionOutput(
                Validate.notNull(taskExecution.getId(), "id"), Collections.emptyMap()));

        TaskWorker taskWorker = new TaskWorker(
            getEventPublisher(syncMessageBroker), new CurrentThreadExecutorService(), taskHandlerResolver,
            taskFileStorage);

        MapTaskDispatcher mapTaskDispatcher = new MapTaskDispatcher(
            event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
            contextService, new CounterServiceImpl(new InMemoryCounterRepository()),
            curTaskExecution -> taskWorker.onTaskExecutionEvent(new TaskExecutionEvent(curTaskExecution)),
            taskExecutionService, taskFileStorage);

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
