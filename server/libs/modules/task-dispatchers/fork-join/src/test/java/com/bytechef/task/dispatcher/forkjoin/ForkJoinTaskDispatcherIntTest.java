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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor.TaskDispatcherJobExecution;
import com.bytechef.task.dispatcher.fork.join.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.fork.join.completion.ForkJoinTaskCompletionHandler;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    public void testDispatchOutputTwoBranches() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("fork-join_v1-output-twoBranches"),
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

        Job job = jobExecution.job();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals("branch zero output", outputs.get("branch0Result"));
        Assertions.assertEquals("branch one output", outputs.get("branch1Result"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDispatchOutputNoOutputBranchKeyPresentWithNullValue() {
        // ConcurrentHashMap-backed TestVarTaskHandler cannot store a null value, so the last task of the
        // second branch (no "value" parameter) is handled by a null-tolerant consumer instead of the
        // shared testVarTaskHandler field.
        TestVarTaskHandler<Object, Object> nullTolerantVarTaskHandler = new TestVarTaskHandler<>(
            (valueMap, name, value) -> {
                if (value != null) {
                    valueMap.put(name, value);
                }
            });

        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("fork-join_v1-output-noOutputBranch"),
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
            () -> Map.of("var/v1/set", nullTolerantVarTaskHandler));

        TaskExecution forkJoinTaskExecution = jobExecution.taskExecutions()
            .stream()
            .filter(taskExecution -> "forkJoin_1".equals(taskExecution.getName()))
            .findFirst()
            .orElseThrow();

        Map<String, Object> branchOutputs = (Map<String, Object>) taskFileStorage.readTaskExecutionOutput(
            Objects.requireNonNull(forkJoinTaskExecution.getOutput()));

        Assertions.assertEquals("branch zero output", branchOutputs.get("branch_0"));
        Assertions.assertTrue(branchOutputs.containsKey("branch_1"));
        Assertions.assertNull(branchOutputs.get("branch_1"));
    }

    // Disabling every task of branch 0 leaves that branch empty. The join counter must be sized from the branches
    // actually dispatched, otherwise it never reaches zero and the job hangs rather than failing - hence the
    // explicit timeout.
    @Test
    @Timeout(60)
    public void testDispatchWithFullyDisabledBranch() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("fork-join_v1-disabled-branch"),
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

        Job job = jobExecution.job();

        Assertions.assertEquals(Job.Status.COMPLETED, job.getStatus());
        Assertions.assertTrue(jobExecution.getExecutionErrors()
            .isEmpty());

        Assertions.assertNull(testVarTaskHandler.get("skippedVar"));

        // The surviving branch keeps its original index, so the completion handler still resolves its second task
        // through branches[1] and chains it after the first one.
        Assertions.assertEquals(100, testVarTaskHandler.get("var2"));
        Assertions.assertEquals(112, testVarTaskHandler.get("sumVar2"));
    }
}
