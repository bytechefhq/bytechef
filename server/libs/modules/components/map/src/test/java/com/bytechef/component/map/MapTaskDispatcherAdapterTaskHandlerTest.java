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

import static com.bytechef.atlas.configuration.constant.WorkflowConstants.FINALIZE;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.NAME;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.PARAMETERS;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.POST;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.PRE;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.TYPE;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.memory.SyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Arik Cohen
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Disabled
public class MapTaskDispatcherAdapterTaskHandlerTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private final TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

    @Test
    public void test1() {
        TaskHandlerResolver resolver = task -> t -> MapUtils.get(t.getParameters(), "value");

        MapTaskDispatcherAdapterTaskHandler taskHandler = new MapTaskDispatcherAdapterTaskHandler(
            EVALUATOR, resolver);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        NAME, "map1",
                        TYPE, "type",
                        PARAMETERS, Map.of(
                            "items", List.of(1, 2, 3),
                            "iteratee",
                            List.of(
                                Map.of(
                                    NAME, "name",
                                    TYPE, "var",
                                    PARAMETERS, Map.of("value", "${map1.item}")))))))
            .build();

        taskExecution.setJobId(4567L);

        List<?> result = taskHandler.handle(taskExecution);

        Assertions.assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test2() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            TaskHandlerResolver taskHandlerResolver = task -> taskExecution -> {
                throw new IllegalStateException("i'm rogue");
            };
            MapTaskDispatcherAdapterTaskHandler taskHandler = new MapTaskDispatcherAdapterTaskHandler(
                EVALUATOR, taskHandlerResolver);

            TaskExecution taskExecution = TaskExecution.builder()
                .workflowTask(
                    new WorkflowTask(
                        Map.of(
                            PARAMETERS,
                            Map.of("list", List.of(1, 2, 3), "iteratee", List.of(Map.of("type", "rogue"))))))
                .build();

            taskExecution.setJobId(4567L);

            taskHandler.handle(taskExecution);
        });
    }

    @Test
    public void test3() {
        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        syncMessageBroker.receive(TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS, t -> {
            TaskExecution taskExecution = ((TaskExecutionCompleteEvent) t).getTaskExecution();

            Assertions.assertNull(taskExecution.getOutput());
        });

        syncMessageBroker
            .receive(TaskCoordinatorMessageRoute.ERROR_EVENTS, t -> {
                TaskExecution taskExecution = ((TaskExecutionErrorEvent) t).getTaskExecution();

                Assertions.assertNull(taskExecution.getError());
            });

        syncMessageBroker.receive(TaskCoordinatorMessageRoute.APPLICATION_EVENTS,
            t -> {});

        MapTaskDispatcherAdapterTaskHandler[] mapAdapterTaskHandlerRefs = new MapTaskDispatcherAdapterTaskHandler[1];

        TaskHandlerResolver taskHandlerResolver = t1 -> {
            String type = t1.getType();

            if ("var".equals(type)) {
                return t2 -> MapUtils.getRequired(t2.getParameters(), "value");
            }
            if ("pass".equals(type)) {
                return t2 -> null;
            }
            if ("map".equals(type)) {
                return mapAdapterTaskHandlerRefs[0];
            } else {
                throw new IllegalArgumentException("unknown type: " + type);
            }
        };

        TaskWorker worker = new TaskWorker(
            null, EVALUATOR, event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
            EXECUTOR_SERVICE::execute, taskHandlerResolver, taskFileStorage, List.of());

        mapAdapterTaskHandlerRefs[0] = new MapTaskDispatcherAdapterTaskHandler(
            EVALUATOR, taskHandlerResolver);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        NAME, "name",
                        TYPE, "pass",
                        PRE, List.of(
                            Map.of(
                                NAME, "output",
                                TYPE, "map",
                                PARAMETERS,
                                Map.of(
                                    "items", Arrays.asList(1, 2, 3),
                                    "iteratee",
                                    List.of(
                                        Map.of(
                                            NAME, "var",
                                            TYPE, "var",
                                            PARAMETERS, Map.of("value", "${item}")))))),
                        POST, List.of(
                            Map.of(
                                NAME, "output",
                                TYPE, "map",
                                PARAMETERS,
                                Map.of(
                                    "items", Arrays.asList(1, 2, 3),
                                    "iteratee",
                                    List.of(
                                        Map.of(
                                            NAME, "var",
                                            TYPE, "var",
                                            PARAMETERS, Map.of("value", "${item}")))))),
                        FINALIZE, List.of(Map.of(
                            NAME, "output",
                            TYPE, "map",
                            PARAMETERS,
                            Map.of(
                                "items", Arrays.asList(1, 2, 3),
                                "iteratee",
                                List.of(
                                    Map.of(
                                        NAME, "var",
                                        TYPE, "var",
                                        PARAMETERS, Map.of("value", "${item}")))))))))
            .build();

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution));
    }
}
