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

package com.bytechef.task.dispatcher.on.error;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.task.dispatcher.loop.LoopTaskDispatcher;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.bytechef.task.dispatcher.on.error.completition.OnErrorTaskCompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Matija Petanjek
 */
@TaskDispatcherIntTest
public class OnErrorTaskDispatcherIntTest {

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
    public void testOnErrorTaskDispatcherWhenNoException() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("on-error_v1-no-exception".getBytes(StandardCharsets.UTF_8)),
            Collections.emptyMap(),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("main branch", testVarTaskHandler.get("mainBranchVar"));
        Assertions.assertNull(testVarTaskHandler.get("errorBranchVar"));
        Assertions.assertEquals("end", testVarTaskHandler.get("endVar"));
    }

    @Test
    public void testOnErrorTaskDispatcherWhenExceptionInMainBranch() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("on-error_v1-exception-main-branch".getBytes(StandardCharsets.UTF_8)),
            Collections.emptyMap(),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals(
            "main branch before exception", testVarTaskHandler.get("beforeExceptionMainBranchVar"));
        Assertions.assertNull(testVarTaskHandler.get("afterExceptionMainBranchVar"));
        Assertions.assertEquals("error branch", testVarTaskHandler.get("errorBranchVar"));
        Assertions.assertEquals("end", testVarTaskHandler.get("endVar"));
    }

    @Test
    public void testOnErrorTaskDispatcherWhenExceptionInOnErrorBranch() {
        ExecutionException executionException = Assertions.assertThrows(
            ExecutionException.class, () -> taskDispatcherJobTestExecutor.execute(
                EncodingUtils
                    .base64EncodeToString("on-error_v1-exception-error-branch".getBytes(StandardCharsets.UTF_8)),
                Collections.emptyMap(),
                this::getTaskCompletionHandlerFactories,
                this::getTaskDispatcherResolverFactories,
                this::getTaskHandlerMap));

        Assertions.assertEquals("test exception", executionException.getMessage());

        Assertions.assertEquals("main branch", testVarTaskHandler.get("mainBranchVar"));
        Assertions.assertEquals(
            "before exception in error branch", testVarTaskHandler.get("beforeExceptionErrorBranchVar"));
        Assertions.assertNull(testVarTaskHandler.get("afterExceptionErrorBranchVar"));
        Assertions.assertNull(testVarTaskHandler.get("endVar"));
    }

    @Test
    public void testOnErrorTaskDispatcherInLoopTaskDispatcherWhenExceptionInMainBranch() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils
                .base64EncodeToString("on-error_v1-exception-main-branch-in-loop_v1".getBytes(StandardCharsets.UTF_8)),
            Collections.emptyMap(),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals(
            "main branch before exception, iteration number 2",
            testVarTaskHandler.get("beforeExceptionMainBranchVar"));
        Assertions.assertNull(testVarTaskHandler.get("afterExceptionMainBranchVar"));
        Assertions.assertEquals("error branch iteration number 2", testVarTaskHandler.get("errorBranchVar"));
        Assertions.assertEquals("end", testVarTaskHandler.get("endVar"));
    }

    @Test
    public void testOnErrorTaskDispatcherInLoopTaskDispatcherWhenExceptionInErrorBranch() {
        ExecutionException executionException = Assertions.assertThrows(
            ExecutionException.class, () -> taskDispatcherJobTestExecutor.execute(
                EncodingUtils.base64EncodeToString(
                    "on-error_v1-exception-error-branch-in-loop_v1".getBytes(StandardCharsets.UTF_8)),
                Collections.emptyMap(),
                this::getTaskCompletionHandlerFactories,
                this::getTaskDispatcherResolverFactories,
                this::getTaskHandlerMap));

        Assertions.assertEquals("test exception", executionException.getMessage());

        Assertions.assertEquals(
            "main branch before exception, iteration number 1",
            testVarTaskHandler.get("beforeExceptionMainBranchVar"));
        Assertions.assertNull(testVarTaskHandler.get("afterExceptionMainBranchVar"));
        Assertions.assertEquals(
            "error branch before exception, iteration number 1",
            testVarTaskHandler.get("beforeExceptionErrorBranchVar"));
        Assertions.assertNull(testVarTaskHandler.get("afterExceptionErrorBranchVar"));
        Assertions.assertNull(testVarTaskHandler.get("endVar"));
    }

    @SuppressWarnings("PMD")
    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new LoopTaskCompletionHandler(
                contextService, SpelEvaluator.create(), taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new OnErrorTaskCompletionHandler(
                contextService, SpelEvaluator.create(), taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ApplicationEventPublisher eventPublisher, ContextService contextService,
        CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of((taskDispatcher) -> new LoopTaskDispatcher(
            contextService, SpelEvaluator.create(), eventPublisher, taskDispatcher, taskExecutionService,
            taskFileStorage),
            (taskDispatcher) -> new OnErrorTaskDispatcher(
                contextService, SpelEvaluator.create(), eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    private Map<String, TaskHandler<?>> getTaskHandlerMap() {
        return Map.of(
            "var/v1/set", testVarTaskHandler,
            "httpClient/v1/get", taskExecution -> {
                throw new TaskExecutionException("test exception");
            });
    }
}
