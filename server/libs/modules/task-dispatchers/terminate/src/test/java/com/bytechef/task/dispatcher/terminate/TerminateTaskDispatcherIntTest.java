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

package com.bytechef.task.dispatcher.terminate;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.ControlTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.bytechef.task.dispatcher.loop.LoopTaskDispatcher;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.bytechef.task.dispatcher.on.error.OnErrorTaskDispatcher;
import com.bytechef.task.dispatcher.on.error.completition.OnErrorTaskCompletionHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Matija Petanjek
 */
@TaskDispatcherIntTest
public class TerminateTaskDispatcherIntTest {

    private TestVarTaskHandler<Object, Object> testVarTaskHandler;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected TaskExecutionService taskExecutionService;

    @Autowired
    protected TaskFileStorage taskFileStorage;

    @Autowired
    private TaskDispatcherJobTestExecutor taskDispatcherJobTestExecutor;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>(Map::put);
    }

    @Test
    public void testTerminateTaskDispatcherInSimpleFlow() {
        Job job = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("terminate_v1-simple-flow".getBytes(StandardCharsets.UTF_8)),
            Collections.emptyMap(),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals(Job.Status.STOPPED, job.getStatus());
        Assertions.assertEquals("start", testVarTaskHandler.get("startVar"));
        Assertions.assertNull(testVarTaskHandler.get("endVar"));
    }

    @Test
    public void testTerminateTaskDispatcherInOnErrorDispatcherErrorBranch() {
        Job job = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "terminate_v1-on-error-dispatcher-error-branch".getBytes(StandardCharsets.UTF_8)),
            Collections.emptyMap(),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals(Job.Status.STOPPED, job.getStatus());
        Assertions.assertEquals("main branch", testVarTaskHandler.get("mainBranchVar"));
        Assertions.assertEquals(
            "before terminate in error branch", testVarTaskHandler.get("beforeTerminateErrorBranchVar"));
        Assertions.assertNull(testVarTaskHandler.get("afterTerminateErrorBranchVar"));
        Assertions.assertNull(testVarTaskHandler.get("end"));
    }

    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        CounterService counterService, TaskExecutionService taskExecutionService) {

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
                taskFileStorage),
            (taskDispatcher) -> new TerminateTaskDispatcher(eventPublisher, taskExecutionService),
            (taskDispatcher) -> new ControlTaskDispatcher(eventPublisher));
    }

    private Map<String, TaskHandler<?>> getTaskHandlerMap() {
        return Map.of(
            "var/v1/set", testVarTaskHandler,
            "httpClient/v1/get", taskExecution -> {
                throw new TaskExecutionException("test exception");
            });
    }
}
