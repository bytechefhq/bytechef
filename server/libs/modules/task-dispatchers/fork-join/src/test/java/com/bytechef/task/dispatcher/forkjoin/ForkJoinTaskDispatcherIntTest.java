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

package com.bytechef.task.dispatcher.forkjoin;

import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.task.dispatcher.fork.join.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.fork.join.completion.ForkJoinTaskCompletionHandler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
public class ForkJoinTaskDispatcherIntTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private TestVarTaskHandler<Object, Object> testVarTaskHandler;

    @Autowired
    private TaskDispatcherJobTestExecutor taskDispatcherJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>(Map::put);
    }

    @Test
    public void testDispatch() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("fork-join_v1"),
            (
                contextService, counterService, taskExecutionService) -> List.of(
                    (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                        contextService, counterService, EVALUATOR, taskExecutionService,
                        taskCompletionHandler, taskDispatcher, taskFileStorage)),
            (
                eventPublisher, contextService, counterService, taskExecutionService) -> List.of(
                    (taskDispatcher) -> new ForkJoinTaskDispatcher(
                        contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher,
                        taskExecutionService, taskFileStorage)),
            () -> Map.of("var/v1/set", testVarTaskHandler));

        Assertions.assertEquals(85, testVarTaskHandler.get("sumVar1"));
        Assertions.assertEquals(112, testVarTaskHandler.get("sumVar2"));
    }

    @Test
    @Timeout(60)
    public void testDispatchWithFailedBranchCancelsOtherBranch() throws InterruptedException {
        CountDownLatch releaseSlowTaskLatch = new CountDownLatch(1);
        CountDownLatch slowTaskFinishedLatch = new CountDownLatch(1);
        CountDownLatch slowTaskStartedLatch = new CountDownLatch(1);
        AtomicBoolean slowTaskInterrupted = new AtomicBoolean();

        TaskHandler<Object> failTaskHandler = taskExecution -> {
            try {
                slowTaskStartedLatch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();
            }

            throw new TaskExecutionException("branch failed");
        };

        TaskHandler<Object> slowTaskHandler = taskExecution -> {
            slowTaskStartedLatch.countDown();

            try {
                releaseSlowTaskLatch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                slowTaskInterrupted.set(true);

                Thread.currentThread()
                    .interrupt();
            } finally {
                slowTaskFinishedLatch.countDown();
            }

            return "slow task output";
        };

        try {
            ExecutionException executionException = Assertions.assertThrows(
                ExecutionException.class,
                () -> taskDispatcherJobTestExecutor.execute(
                    EncodingUtils.base64EncodeToString("fork-join_v1-failed-branch"),
                    (
                        contextService, counterService, taskExecutionService) -> List.of(
                            (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                                contextService, counterService, EVALUATOR, taskExecutionService,
                                taskCompletionHandler, taskDispatcher, taskFileStorage)),
                    (
                        eventPublisher, contextService, counterService, taskExecutionService) -> List.of(
                            (taskDispatcher) -> new ForkJoinTaskDispatcher(
                                contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher,
                                taskExecutionService, taskFileStorage)),
                    () -> Map.of(
                        "fail/v1", failTaskHandler, "slow/v1", slowTaskHandler, "var/v1/set", testVarTaskHandler)));

            Assertions.assertEquals("branch failed", executionException.getMessage());

            Assertions.assertTrue(slowTaskFinishedLatch.await(20, TimeUnit.SECONDS));
            Assertions.assertTrue(slowTaskInterrupted.get());
            Assertions.assertNull(testVarTaskHandler.get("afterSlowTask"));
        } finally {
            releaseSlowTaskLatch.countDown();
        }
    }
}
