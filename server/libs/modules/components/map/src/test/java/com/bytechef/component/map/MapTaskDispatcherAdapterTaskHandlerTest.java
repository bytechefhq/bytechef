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

import static com.bytechef.hermes.component.definition.Action.ACTION;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.task.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 */
public class MapTaskDispatcherAdapterTaskHandlerTest {

    @Test
    public void test1() {
        TaskHandlerResolver resolver = task -> t -> t.get("value");
        MapTaskDispatcherAdapterTaskHandler taskHandler =
                new MapTaskDispatcherAdapterTaskHandler(resolver, SpelTaskEvaluator.create());

        TaskExecution taskExecution = new TaskExecution(
                Map.of("list", List.of(1, 2, 3), "iteratee", Map.of("type", "var", "value", "${item}"), ACTION, "map"));

        taskExecution.setId("1234");
        taskExecution.setJobId("4567");

        List<?> results = taskHandler.handle(taskExecution);

        Assertions.assertEquals(List.of(1, 2, 3), results);
    }

    @Test
    public void test2() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            TaskHandlerResolver resolver = task -> t -> {
                throw new IllegalArgumentException("i'm rogue");
            };
            MapTaskDispatcherAdapterTaskHandler taskHandler =
                    new MapTaskDispatcherAdapterTaskHandler(resolver, SpelTaskEvaluator.create());

            TaskExecution taskExecution = new TaskExecution(Map.of(
                    "list", List.of(1, 2, 3),
                    "iteratee", Map.of("type", "rogue")));

            taskExecution.setId("1234");
            taskExecution.setJobId("4567");

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
                return t2 -> t2.getRequired("value");
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

        Worker worker = Worker.builder()
                .withTaskHandlerResolver(taskHandlerResolver)
                .withMessageBroker(messageBroker)
                .withEventPublisher(e -> {})
                .withTaskEvaluator(SpelTaskEvaluator.create())
                .build();

        mapAdapterTaskHandlerRefs[0] =
                new MapTaskDispatcherAdapterTaskHandler(taskHandlerResolver, SpelTaskEvaluator.create());

        TaskExecution taskExecution = new TaskExecution(Map.of(
                "finalize",
                List.of(Map.of(
                        "name",
                        "output",
                        "type",
                        "map",
                        "list",
                        Arrays.asList(1, 2, 3),
                        "iteratee",
                        Map.of("type", "var", "value", "${item}"),
                        ACTION,
                        "map")),
                "post",
                List.of(Map.of(
                        "name",
                        "output",
                        "type",
                        "map",
                        "list",
                        Arrays.asList(1, 2, 3),
                        "iteratee",
                        Map.of("type", "var", "value", "${item}"),
                        ACTION,
                        "map")),
                "pre",
                List.of(Map.of(
                        "name",
                        "output",
                        "type",
                        "map",
                        "list",
                        Arrays.asList(1, 2, 3),
                        "iteratee",
                        Map.of("type", "var", "value", "${item}"),
                        ACTION,
                        "map")),
                "type",
                "pass"));

        taskExecution.setId("1234");
        taskExecution.setJobId("4567");

        worker.handle(taskExecution);
    }
}
