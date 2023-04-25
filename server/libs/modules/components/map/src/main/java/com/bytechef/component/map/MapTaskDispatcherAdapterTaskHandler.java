
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

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.TaskMessageRoute;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.error.ExecutionError;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.ContextServiceImpl;
import com.bytechef.atlas.service.CounterServiceImpl;
import com.bytechef.atlas.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.component.map.concurrency.CurrentThreadExecutorService;
import com.bytechef.task.dispatcher.map.MapTaskDispatcher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class MapTaskDispatcherAdapterTaskHandler implements TaskHandler<List<?>> {

    private final TaskHandlerResolver taskHandlerResolver;

    public MapTaskDispatcherAdapterTaskHandler(TaskHandlerResolver taskHandlerResolver) {
        this.taskHandlerResolver = Objects.requireNonNull(taskHandlerResolver);
    }

    @Override
    @SuppressFBWarnings("NP")
    public List<?> handle(TaskExecution taskExecution) {
        List<Object> result = new ArrayList<>();

        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(TaskMessageRoute.TASKS_COMPLETIONS, message -> {
            TaskExecution completionTaskExecution = (TaskExecution) message;

            result.add(completionTaskExecution.getOutput());
        });

        List<ExecutionError> errors = Collections.synchronizedList(new ArrayList<>());

        messageBroker.receive(SystemMessageRoute.ERRORS, message -> {
            TaskExecution erroredTaskExecution = (TaskExecution) message;

            ExecutionError error = erroredTaskExecution.getError();

            errors.add(error);
        });

        TaskWorker worker = TaskWorker.builder()
            .taskHandlerResolver(taskHandlerResolver)
            .messageBroker(messageBroker)
            .eventPublisher(e -> {})
            .executorService(new CurrentThreadExecutorService())
            .build();

        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(new InMemoryTaskExecutionRepository());

        taskExecution = taskExecutionService.create(taskExecution);

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());

        contextService.push(taskExecution.getId(), Context.Classname.TASK_EXECUTION, Collections.emptyMap());

        MapTaskDispatcher mapTaskDispatcher = MapTaskDispatcher.builder()
            .contextService(contextService)
            .counterService(new CounterServiceImpl(new InMemoryCounterRepository()))
            .messageBroker(messageBroker)
            .taskDispatcher(worker::handle)
            .taskExecutionService(taskExecutionService)
            .build();

        mapTaskDispatcher.dispatch(taskExecution);

        if (errors.size() > 0) {
            StringBuilder errorMessage = new StringBuilder();

            for (ExecutionError error : errors) {
                if (errorMessage.length() > 3000) {
                    errorMessage.append("\n")
                        .append("...");

                    break;
                }

                if (errorMessage.length() > 0) {
                    errorMessage.append("\n");
                }

                errorMessage.append(error.getMessage())
                    .append("\n")
                    .append(String.join("\n", error.getStackTrace()));
            }

            throw new RuntimeException(errorMessage.toString());
        }

        return result;
    }
}
