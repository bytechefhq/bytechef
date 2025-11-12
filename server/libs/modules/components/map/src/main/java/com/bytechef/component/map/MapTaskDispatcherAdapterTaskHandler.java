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
 * Modifications copyright (C) 2025 ByteChef
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
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.memory.SyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.task.dispatcher.map.MapTaskDispatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Feb, 21 2020
 */
public class MapTaskDispatcherAdapterTaskHandler implements TaskHandler<List<?>> {

    private final CacheManager cacheManager;
    private final CurrentThreadExecutorService currentThreadExecutorService = new CurrentThreadExecutorService();
    private final Evaluator evaluator;
    private final TaskHandlerResolver taskHandlerResolver;

    public MapTaskDispatcherAdapterTaskHandler(
        CacheManager cacheManager, Evaluator evaluator, TaskHandlerResolver taskHandlerResolver) {

        this.cacheManager = cacheManager;
        this.evaluator = evaluator;
        this.taskHandlerResolver = taskHandlerResolver;
    }

    @Override
    public List<?> handle(TaskExecution taskExecution) {
        List<Object> result = new ArrayList<>();

        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();
        TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

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
            new TaskExecutionServiceImpl(new InMemoryTaskExecutionRepository(cacheManager));

        taskExecution = taskExecutionService.create(taskExecution);

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository(cacheManager));

        contextService.push(
            Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION,
            taskFileStorage.storeTaskExecutionOutput(
                Validate.notNull(taskExecution.getId(), "id"), Collections.emptyMap()));

        TaskWorker taskWorker = new TaskWorker(
            evaluator, getEventPublisher(syncMessageBroker), currentThreadExecutorService::execute, taskHandlerResolver,
            taskFileStorage);

        MapTaskDispatcher mapTaskDispatcher = new MapTaskDispatcher(
            contextService, new CounterServiceImpl(new InMemoryCounterRepository(cacheManager)), evaluator,
            event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
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
