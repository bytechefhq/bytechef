
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

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.commons.util.MapValueUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.bytechef.hermes.component.exception.ComponentExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 */
public class MapTaskDispatcherAdapterTaskHandlerTest {

    @Test
    public void test1() {
        TaskHandlerResolver resolver = task -> t -> MapValueUtils.get(t.getParameters(), "value");
        MapTaskDispatcherAdapterTaskHandler taskHandler = new MapTaskDispatcherAdapterTaskHandler(resolver,
            TaskEvaluator.create());

        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of(
            Map.of(
                WorkflowConstants.TYPE, "type",
                WorkflowConstants.PARAMETERS, Map.of(
                    "list", List.of(1, 2, 3), "iteratee", Map.of(
                        "type", "var", WorkflowConstants.PARAMETERS, Map.of("value", "${item}"))))));

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
                taskHandlerResolver, TaskEvaluator.create());

            TaskExecution taskExecution = new TaskExecution(
                WorkflowTask.of(
                    Map.of(
                        WorkflowConstants.PARAMETERS,
                        Map.of("list", List.of(1, 2, 3), "iteratee", Map.of("type", "rogue")))));

            taskExecution.setJobId(4567L);

            taskHandler.handle(taskExecution);
        });
    }

    @Test
    public void test3() {
        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(Queues.COMPLETIONS, t -> {
            TaskExecution taskExecution = (TaskExecution) t;

            Assertions.assertNull(taskExecution.getOutput());
        });

        messageBroker.receive(Queues.ERRORS, t -> {
            TaskExecution taskExecution = (TaskExecution) t;

            Assertions.assertNull(taskExecution.getError());
        });

        messageBroker.receive(Queues.EVENTS, t -> {});

        MapTaskDispatcherAdapterTaskHandler[] mapAdapterTaskHandlerRefs = new MapTaskDispatcherAdapterTaskHandler[1];

        TaskHandlerResolver taskHandlerResolver = t1 -> {
            String type = t1.getType();

            if ("var".equals(type)) {
                return t2 -> MapValueUtils.getRequired(t2.getParameters(), "value");
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

        Worker worker = Worker.builder()
            .withTaskHandlerResolver(taskHandlerResolver)
            .withMessageBroker(messageBroker)
            .withEventPublisher(e -> {})
            .withTaskEvaluator(TaskEvaluator.create())
            .build();

        mapAdapterTaskHandlerRefs[0] = new MapTaskDispatcherAdapterTaskHandler(taskHandlerResolver,
            TaskEvaluator.create());

        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of(Map.of(
            "finalize",
            List.of(Map.of(
                "name",
                "output",
                "type",
                "map",
                WorkflowConstants.PARAMETERS,
                Map.of(
                    "list", Arrays.asList(1, 2, 3),
                    "iteratee", Map.of("type", "var", WorkflowConstants.PARAMETERS, Map.of("value", "${item}"))))),
            "post",
            List.of(Map.of(
                "name",
                "output",
                "type",
                "map",
                WorkflowConstants.PARAMETERS,
                Map.of(
                    "list", Arrays.asList(1, 2, 3),
                    "iteratee", Map.of("type", "var", WorkflowConstants.PARAMETERS, Map.of("value", "${item}"))))),
            "pre",
            List.of(Map.of(
                "name",
                "output",
                "type",
                "map",
                WorkflowConstants.PARAMETERS,
                Map.of(
                    "list", Arrays.asList(1, 2, 3),
                    "iteratee", Map.of("type", "var", WorkflowConstants.PARAMETERS, Map.of("value", "${item}"))))),
            "type",
            "pass")));

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.handle(taskExecution);
    }
}
