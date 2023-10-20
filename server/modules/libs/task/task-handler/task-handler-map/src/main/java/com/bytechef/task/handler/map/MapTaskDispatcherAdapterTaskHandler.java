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

package com.bytechef.task.handler.map;

import com.bytechef.atlas.context.domain.MapContext;
import com.bytechef.atlas.error.Error;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.repository.memory.context.InMemoryContextRepository;
import com.bytechef.atlas.repository.memory.counter.InMemoryCounterRepository;
import com.bytechef.atlas.repository.memory.task.execution.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.service.context.ContextService;
import com.bytechef.atlas.service.counter.CounterService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.concurrency.CurrentThreadExecutorService;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.task.dispatcher.map.MapTaskDispatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class MapTaskDispatcherAdapterTaskHandler implements TaskHandler<List<?>> {

    private final TaskHandlerResolver taskHandlerResolver;
    private final TaskEvaluator taskEvaluator;
    private final Worker.Builder builder;

    public MapTaskDispatcherAdapterTaskHandler(
            TaskHandlerResolver taskHandlerResolver, TaskEvaluator taskEvaluator, Worker.Builder builder) {
        this.taskHandlerResolver = Objects.requireNonNull(taskHandlerResolver);
        this.taskEvaluator = Objects.requireNonNull(taskEvaluator);
        this.builder = builder;
    }

    @Override
    public List<?> handle(TaskExecution aTask) throws Exception {
        List<Object> result = new ArrayList<>();

        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(Queues.COMPLETIONS, message -> {
            TaskExecution completion = (TaskExecution) message;

            result.add(completion.getOutput());
        });

        List<Error> errors = Collections.synchronizedList(new ArrayList<>());

        messageBroker.receive(Queues.ERRORS, message -> {
            TaskExecution erringTask = (TaskExecution) message;

            Error err = erringTask.getError();

            errors.add(err);
        });

        Worker worker = builder.withTaskHandlerResolver(taskHandlerResolver)
                .withMessageBroker(messageBroker)
                .withEventPublisher(e -> {})
                .withExecutors(new CurrentThreadExecutorService())
                .withTaskEvaluator(taskEvaluator)
                .build();

        ContextService contextService = new ContextService(new InMemoryContextRepository(), null);

        contextService.push(aTask.getId(), new MapContext());

        MapTaskDispatcher dispatcher = MapTaskDispatcher.builder()
                .contextService(contextService)
                .counterService(new CounterService(new InMemoryCounterRepository()))
                .messageBroker(messageBroker)
                .taskDispatcher(worker::handle)
                .taskExecutionService(new TaskExecutionService(new InMemoryTaskExecutionRepository()))
                .taskEvaluator(taskEvaluator)
                .build();

        dispatcher.dispatch(aTask);

        if (errors.size() > 0) {
            StringBuilder errorMessage = new StringBuilder();

            for (Error e : errors) {
                if (errorMessage.length() > 3000) {
                    errorMessage.append("\n").append("...");
                    break;
                }

                if (errorMessage.length() > 0) {
                    errorMessage.append("\n");
                }

                errorMessage
                        .append(e.getMessage())
                        .append("\n")
                        .append(String.join("\n", Arrays.asList(e.getStackTrace())));
            }

            throw new RuntimeException(errorMessage.toString());
        }

        return result;
    }
}
