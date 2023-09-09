
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

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.execution.message.broker.TaskMessageRoute;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacadeImpl;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.component.map.concurrency.CurrentThreadExecutorService;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.commons.util.MapUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.bytechef.atlas.configuration.constant.WorkflowConstants.FINALIZE;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.NAME;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.PARAMETERS;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.POST;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.PRE;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.TYPE;

/**
 * @author Arik Cohen
 */
public class MapTaskDispatcherAdapterTaskHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
        }
    };

    private final WorkflowFileStorageFacade workflowFileStorageFacade = new WorkflowFileStorageFacadeImpl(
        new Base64FileStorageService(), objectMapper);

    @Test
    public void test1() {
        TaskHandlerResolver resolver = task -> t -> MapUtils.get(t.getParameters(), "value");
        MapTaskDispatcherAdapterTaskHandler taskHandler = new MapTaskDispatcherAdapterTaskHandler(
            objectMapper, resolver);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                WorkflowTask.of(
                    Map.of(
                        NAME, "name",
                        TYPE, "type",
                        PARAMETERS, Map.of(
                            "list", List.of(1, 2, 3),
                            "iteratee",
                            Map.of(
                                NAME, "name",
                                TYPE, "var",
                                PARAMETERS, Map.of("value", "${item}"))))))
            .build();

        taskExecution.setJobId(4567L);

        List<?> results = taskHandler.handle(taskExecution);

        Assertions.assertEquals(List.of(1, 2, 3), results);
    }

    @Test
    public void test2() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            TaskHandlerResolver taskHandlerResolver = task -> taskExecution -> {
                throw new ComponentExecutionException("i'm rogue");
            };
            MapTaskDispatcherAdapterTaskHandler taskHandler = new MapTaskDispatcherAdapterTaskHandler(
                objectMapper, taskHandlerResolver);

            TaskExecution taskExecution = TaskExecution.builder()
                .workflowTask(
                    WorkflowTask.of(
                        Map.of(
                            PARAMETERS,
                            Map.of("list", List.of(1, 2, 3), "iteratee", Map.of("type", "rogue")))))
                .build();

            taskExecution.setJobId(4567L);

            taskHandler.handle(taskExecution);
        });
    }

    @Test
    public void test3() {
        SyncMessageBroker messageBroker = new SyncMessageBroker(objectMapper);

        messageBroker.receive(TaskMessageRoute.TASKS_COMPLETE, t -> {
            TaskExecution taskExecution = (TaskExecution) t;

            Assertions.assertNull(taskExecution.getOutput());
        });

        messageBroker.receive(SystemMessageRoute.ERRORS, t -> {
            TaskExecution taskExecution = (TaskExecution) t;

            Assertions.assertNull(taskExecution.getError());
        });

        messageBroker.receive(SystemMessageRoute.EVENTS, t -> {});

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
                throw new ComponentExecutionException("unknown type: " + type);
            }
        };

        TaskWorker worker = new TaskWorker(
            e -> {}, new CurrentThreadExecutorService(), messageBroker, taskHandlerResolver,
            workflowFileStorageFacade);

        mapAdapterTaskHandlerRefs[0] = new MapTaskDispatcherAdapterTaskHandler(objectMapper, taskHandlerResolver);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                WorkflowTask.of(
                    Map.of(
                        NAME, "name",
                        TYPE, "pass",
                        PRE, List.of(
                            Map.of(
                                NAME, "output",
                                TYPE, "map",
                                PARAMETERS,
                                Map.of(
                                    "list", Arrays.asList(1, 2, 3),
                                    "iteratee",
                                    Map.of(
                                        NAME, "var",
                                        TYPE, "var",
                                        PARAMETERS, Map.of("value", "${item}"))))),
                        POST, List.of(
                            Map.of(
                                NAME, "output",
                                TYPE, "map",
                                PARAMETERS,
                                Map.of(
                                    "list", Arrays.asList(1, 2, 3),
                                    "iteratee",
                                    Map.of(
                                        NAME, "var",
                                        TYPE, "var",
                                        PARAMETERS, Map.of("value", "${item}"))))),
                        FINALIZE, List.of(Map.of(
                            NAME, "output",
                            TYPE, "map",
                            PARAMETERS,
                            Map.of(
                                "list", Arrays.asList(1, 2, 3),
                                "iteratee",
                                Map.of(
                                    NAME, "var",
                                    TYPE, "var",
                                    PARAMETERS, Map.of("value", "${item}"))))))))
            .build();

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.handle(taskExecution);
    }
}
