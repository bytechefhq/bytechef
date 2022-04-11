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

package com.integri.atlas.engine.worker.task.map;

import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.error.Error;
import com.integri.atlas.engine.core.message.broker.Queues;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.evaluator.TaskEvaluator;
import com.integri.atlas.engine.worker.Worker;
import com.integri.atlas.engine.worker.task.concurrency.CurrentThreadExecutorService;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.engine.worker.task.handler.TaskHandlerResolver;
import com.integri.atlas.engine.worker.task.repository.InMemoryContextRepository;
import com.integri.atlas.engine.worker.task.repository.InMemoryCounterRepository;
import com.integri.atlas.engine.worker.task.repository.InMemoryTaskExecutionRepository;
import com.integri.atlas.message.broker.sync.SyncMessageBroker;
import com.integri.atlas.task.dispatcher.map.MapTaskDispatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class MapTaskHandlerAdapter implements TaskHandler<List<?>> {

    private final TaskHandlerResolver taskHandlerResolver;
    private final TaskEvaluator taskEvaluator;

    public MapTaskHandlerAdapter(TaskHandlerResolver aResolver, TaskEvaluator aTaskEvaluator) {
        taskHandlerResolver = Objects.requireNonNull(aResolver);
        taskEvaluator = Objects.requireNonNull(aTaskEvaluator);
    }

    @Override
    public List<?> handle(TaskExecution aTask) throws Exception {
        List<Object> result = new ArrayList<>();

        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(
            Queues.COMPLETIONS,
            message -> {
                TaskExecution completion = (TaskExecution) message;
                result.add(completion.getOutput());
            }
        );

        List<Error> errors = Collections.synchronizedList(new ArrayList<>());

        messageBroker.receive(
            Queues.ERRORS,
            message -> {
                TaskExecution erringTask = (TaskExecution) message;
                Error err = erringTask.getError();
                errors.add(err);
            }
        );

        Worker worker = Worker
            .builder()
            .withTaskHandlerResolver(taskHandlerResolver)
            .withMessageBroker(messageBroker)
            .withEventPublisher(e -> {})
            .withExecutors(new CurrentThreadExecutorService())
            .withTaskEvaluator(taskEvaluator)
            .build();

        InMemoryContextRepository contextRepository = new InMemoryContextRepository();

        contextRepository.push(aTask.getId(), new MapContext());

        MapTaskDispatcher dispatcher = MapTaskDispatcher
            .builder()
            .contextRepository(contextRepository)
            .counterRepository(new InMemoryCounterRepository())
            .messageBroker(messageBroker)
            .taskDispatcher(worker::handle)
            .taskExecutionRepository(new InMemoryTaskExecutionRepository())
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
