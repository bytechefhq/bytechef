/*
 * Copyright 2021 <your company/name>.
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
 */

package com.integri.atlas.task.dispatcher.if_;

import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.core.context.Context;
import com.integri.atlas.engine.core.message.broker.MessageBroker;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.core.task.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.dispatcher.if_.completion.IfTaskCompletionHandler;
import com.integri.atlas.task.handler.core.Var;
import com.integri.atlas.test.task.handler.BaseTaskIntTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Matija Petanjek
 */
@SpringBootTest
public class IfTaskDispatcherIntTest extends BaseTaskIntTest {

    @Test
    public void testIfTaskDispatcher_Boolean() {
        Job job = startJob("samples/if-conditions-boolean.yaml", Map.of("value1", "true", "value2", "false"));

        Context context = contextRepository.peek(job.getId());

        Assertions.assertEquals("false branch", context.get("equalsResult"));
        Assertions.assertEquals("true branch", context.get("notEqualsResult"));
    }

    @Test
    public void testIfTaskDispatcher_DateTime() {
        Job job = startJob(
            "samples/if-conditions-dateTime.yaml",
            Map.of("value1", "2022-01-01T00:00:00", "value2", "2022-01-01T00:00:01")
        );

        Context context = contextRepository.peek(job.getId());

        Assertions.assertEquals("false branch", context.get("afterResult"));
        Assertions.assertEquals("true branch", context.get("beforeResult"));
    }

    @Test
    public void testIfTaskDispatcher_Number() {
        Job job = startJob("samples/if-conditions-number.yaml", Map.of("value1", 100, "value2", 200));

        Context context = contextRepository.peek(job.getId());

        Assertions.assertEquals("false branch", context.get("equalsResult"));
        Assertions.assertEquals("true branch", context.get("notEqualsResult"));
        Assertions.assertEquals("false branch", context.get("greaterResult"));
        Assertions.assertEquals("true branch", context.get("lessResult"));
        Assertions.assertEquals("false branch", context.get("greaterEqualsResult"));
        Assertions.assertEquals("true branch", context.get("lessEqualsResult"));
    }

    @Test
    public void testIfTaskDispatcher_String() {
        Job job = startJob("samples/if-conditions-string.yaml", Map.of("value1", "Hello World", "value2", "Hello"));

        Context context = contextRepository.peek(job.getId());

        Assertions.assertEquals("false branch", context.get("equalsResult"));
        Assertions.assertEquals("true branch", context.get("notEqualsResult"));
        Assertions.assertEquals("true branch", context.get("containsResult"));
        Assertions.assertEquals("false branch", context.get("notContainsResult"));
        Assertions.assertEquals("true branch", context.get("startsWithResult"));
        Assertions.assertEquals("false branch", context.get("endsWithResult"));
        Assertions.assertEquals("false branch", context.get("isEmptyResult"));
        Assertions.assertEquals("false branch", context.get("regexResult"));
    }

    @Override
    protected List<TaskCompletionHandler> getTaskCompletionHandlers(
        TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher taskDispatcher
    ) {
        return List.of(
            new IfTaskCompletionHandler(
                contextRepository,
                taskCompletionHandler,
                taskDispatcher,
                SpelTaskEvaluator.create(),
                taskExecutionRepository
            )
        );
    }

    @Override
    protected List<TaskDispatcherResolver> getTaskDispatcherResolvers(
        MessageBroker coordinatorMessageBroker,
        TaskDispatcher taskDispatcher
    ) {
        return List.of(
            new IfTaskDispatcher(
                contextRepository,
                coordinatorMessageBroker,
                taskDispatcher,
                SpelTaskEvaluator.create(),
                taskExecutionRepository
            )
        );
    }

    @Override
    protected Map<String, TaskHandler<?>> getTaskHandlerResolverMap() {
        return Map.of("core/var", new Var());
    }
}
