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

package com.integri.atlas.engine.worker.task;

import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.worker.task.map.MapTaskHandlerAdapter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MapTaskHandlerAdapterTest {

    @Test
    public void test1() throws Exception {
        TaskHandlerResolver resolver = task -> t -> t.get("value");
        MapTaskHandlerAdapter adapter = new MapTaskHandlerAdapter(resolver, SpelTaskEvaluator.create());
        SimpleTaskExecution task = new SimpleTaskExecution();
        task.setId("1234");
        task.setJobId("4567");
        task.set("list", List.of(1, 2, 3));
        task.set("iteratee", Map.of("type", "var", "value", "${item}"));
        List<?> results = adapter.handle(task);
        Assertions.assertEquals(List.of(1, 2, 3), results);
    }

    @Test
    public void test2() throws Exception {
        Assertions.assertThrows(
            RuntimeException.class,
            () -> {
                TaskHandlerResolver resolver = task ->
                    t -> {
                        throw new IllegalArgumentException("i'm rogue");
                    };
                MapTaskHandlerAdapter adapter = new MapTaskHandlerAdapter(resolver, SpelTaskEvaluator.create());
                SimpleTaskExecution task = new SimpleTaskExecution();
                task.setId("1234");
                task.setJobId("4567");
                task.set("list", List.of(1, 2, 3));
                task.set("iteratee", Map.of("type", "rogue"));
                adapter.handle(task);
            }
        );
    }
}
