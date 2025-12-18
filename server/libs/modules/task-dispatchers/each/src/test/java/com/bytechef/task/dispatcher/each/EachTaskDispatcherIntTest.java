/*
 * Copyright 2025 ByteChef
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

package com.bytechef.task.dispatcher.each;

import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.task.dispatcher.each.completion.EachTaskCompletionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
public class EachTaskDispatcherIntTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private TestVarTaskHandler<List<String>, String> testVarTaskHandler;

    @Autowired
    private TaskDispatcherJobTestExecutor taskDispatcherJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>(
            (valueMap, name, value) -> valueMap
                .computeIfAbsent(name, key -> Collections.synchronizedList(new ArrayList<>()))
                .add(value));
    }

    @Test
    public void testEachTaskDispatcher() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("each_v1"),
            (counterService, taskExecutionService) -> List.of(
                (taskCompletionHandler, taskDispatcher) -> new EachTaskCompletionHandler(
                    counterService, taskCompletionHandler, taskExecutionService)),
            (
                eventPublisher, contextService, counterService, taskExecutionService) -> List.of(
                    (taskDispatcher) -> new EachTaskDispatcher(
                        contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher,
                        taskExecutionService, taskFileStorage)),
            () -> Map.of("var/v1/set", testVarTaskHandler));

        List<String> expected = IntStream.rangeClosed(1, 25)
            .boxed()
            .flatMap(item1 -> IntStream.rangeClosed(1, 25)
                .mapToObj(item2 -> item1 + "_" + item2))
            .toList();

        List<String> actual = testVarTaskHandler.get("var1");

        Assertions.assertEquals(expected.size(), actual.size());
        Assertions.assertEquals(new HashSet<>(expected), new HashSet<>(actual));
    }
}
