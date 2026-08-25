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

package com.bytechef.task.dispatcher.condition;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestTemporalTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor.TaskDispatcherJobExecution;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
public class ConditionTaskDispatcherIntTest {

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
    public void testDispatchBoolean() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-boolean".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "true", "value2", "false"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("notEqualsResult"));
    }

    @Test
    public void testDispatchDateTime() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-dateTime".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "2022-01-01T00:00:00", "value2", "2022-01-01T00:00:01"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("afterResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("beforeResult"));
    }

    @Test
    public void testDispatchExpression() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-expression".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", 100, "value2", 200),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
    }

    @Test
    public void testDispatchMultipleConditions() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-conditions-multiple-conditions".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "Hello World", "value2", "Hello"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("true branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("notEqualsResult"));
    }

    @Test
    public void testDispatchNumber() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-number".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", 100, "value2", 200),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("notEqualsResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("greaterResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("lessResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("greaterEqualsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("lessEqualsResult"));
    }

    @Test
    public void testDispatchString() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-string".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "Hello World", "value2", "Hello"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("notEqualsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("containsResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("notContainsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("startsWithResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("endsWithResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("isEmptyResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("regexResult"));

        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-string".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "Hello World's", "value2", "Hello World's"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("true branch", testVarTaskHandler.get("equalsResult"));
    }

    @Test
    public void testDispatchRawExpressionDateComparison() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-rawExpression-dateComparison".getBytes(StandardCharsets.UTF_8)),
            Map.of("restDate", "2026-08-24T22:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("true branch", testVarTaskHandler.get("comparisonResult"));

        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-rawExpression-dateComparison".getBytes(StandardCharsets.UTF_8)),
            Map.of("restDate", "2026-08-27T22:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("comparisonResult"));
    }

    @Test
    public void testDispatchRawExpressionUncomparableOperandsFailsWithEvaluationError() {
        ExecutionException executionException = Assertions.assertThrows(
            ExecutionException.class,
            () -> taskDispatcherJobTestExecutor.execute(
                EncodingUtils.base64EncodeToString(
                    "condition_v1-rawExpression-uncomparableOperands".getBytes(StandardCharsets.UTF_8)),
                Map.of("restDate", "2026-08-24T22:00:00Z"),
                this::getTaskCompletionHandlerFactories,
                this::getTaskDispatcherResolverFactories,
                this::getTaskHandlerMap));

        Assertions.assertTrue(
            executionException.getMessage()
                .contains("Cannot compare instances of class java.lang.String and class java.time.ZonedDateTime"),
            executionException.getMessage());
    }

    @Test
    public void testDispatchTemporalOutputComparesWithoutParseDate() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-temporalOutput-noParseDate".getBytes(StandardCharsets.UTF_8)),
            Map.of("dbDate", "2026-08-26T00:00:00Z", "restDate", "2026-08-24T22:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("true branch", testVarTaskHandler.get("comparisonResult"));

        TaskExecution dbDateTaskExecution = jobExecution.taskExecutions()
            .stream()
            .filter(taskExecution -> "dbDate".equals(taskExecution.getName()))
            .findFirst()
            .orElseThrow();

        Assertions.assertInstanceOf(
            ZonedDateTime.class,
            taskFileStorage.readTaskExecutionOutput(Objects.requireNonNull(dbDateTaskExecution.getOutput())));

        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-temporalOutput-noParseDate".getBytes(StandardCharsets.UTF_8)),
            Map.of("dbDate", "2026-08-23T00:00:00Z", "restDate", "2026-08-24T22:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("comparisonResult"));
    }

    @Test
    public void testDispatchStringOutputStaysAString() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-temporalOutput-stringStaysString".getBytes(StandardCharsets.UTF_8)),
            Map.of("restDate", "2026-08-26T00:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("true branch", testVarTaskHandler.get("stringResult"));

        TaskExecution restDateTaskExecution = jobExecution.taskExecutions()
            .stream()
            .filter(taskExecution -> "restDate".equals(taskExecution.getName()))
            .findFirst()
            .orElseThrow();

        Assertions.assertInstanceOf(
            String.class,
            taskFileStorage.readTaskExecutionOutput(Objects.requireNonNull(restDateTaskExecution.getOutput())));
    }

    @Test
    public void testDispatchOutputCaseTrue() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-output-caseTrue".getBytes(StandardCharsets.UTF_8)),
            Map.of(),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Job job = jobExecution.job();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals("last task output", outputs.get("result"));
    }

    @Test
    public void testDispatchOutputEmptyCase() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-output-emptyCase".getBytes(StandardCharsets.UTF_8)),
            Map.of(),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Job job = jobExecution.job();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertNull(outputs.get("result"));
    }

    /**
     * Pins the {@code condition/v1} deferred-evaluation contract through the real atlas engine, the way
     * {@code GraphTaskDispatcherIntTest#testDispatchBackJumpCycle} pins {@code graph/v1}'s. Without
     * {@code ConditionTaskDispatcherConfiguration}'s static {@code DeferredEvaluationParameterKeys.register(...)} call
     * for {@code caseTrue}/{@code caseFalse}, the condition task's own {@code TaskExecution#evaluate} would eagerly
     * resolve {@code caseTrue}'s {@code "=redirectExpression"} formula BEFORE the branch is even selected, using
     * whatever context exists at that point -- freezing it to {@code redirectExpression}'s raw value,
     * {@code "${payload}"}. {@link com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher#doDispatch} then
     * evaluates the (already frozen) sub-task a second time before dispatching it: with deferred evaluation disabled,
     * this second pass re-interprets the frozen {@code "${payload}"} string as a brand new accessor expression and
     * resolves it to {@code payload}'s value -- a double-evaluation corruption that silently substitutes the wrong
     * value. With deferred evaluation correctly registered, {@code caseTrue} is untouched by the first pass, so the
     * dispatcher's own evaluate is the only one that ever runs, and {@code "=redirectExpression"} resolves exactly once
     * to the raw, unreinterpreted string {@code "${payload}"}.
     */
    @Test
    public void testDispatchDeferredEvaluationDoesNotDoubleEvaluateCaseTrue() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-deferredEvaluation".getBytes(StandardCharsets.UTF_8)),
            Map.of("redirectExpression", "${payload}", "payload", "resolved-by-mistake"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals(
            "${payload}", testVarTaskHandler.get("lastTask"),
            "caseTrue's formula expression must resolve exactly once, to redirectExpression's raw value, "
                + "not be re-interpreted as a second, accessor-style expression");

        Job job = jobExecution.job();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals("${payload}", outputs.get("result"));
    }

    @Test
    public void testDispatchDateTimeConditionOverTaskOutputs() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-dateTime-temporalOutput".getBytes(StandardCharsets.UTF_8)),
            Map.of("dbDate", "2026-08-26T00:00:00Z", "restDate", "2026-08-24T22:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("true branch", testVarTaskHandler.get("dateTimeResult"));

        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-dateTime-temporalOutput".getBytes(StandardCharsets.UTF_8)),
            Map.of("dbDate", "2026-08-23T00:00:00Z", "restDate", "2026-08-24T22:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("dateTimeResult"));
    }

    @SuppressWarnings("PMD")
    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, EVALUATOR, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ApplicationEventPublisher eventPublisher, ContextService contextService,
        CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskDispatcher) -> new ConditionTaskDispatcher(
                contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    private Map<String, TaskHandler<?>> getTaskHandlerMap() {
        return Map.of("var/v1/set", testVarTaskHandler, "temporal/v1/set", new TestTemporalTaskHandler());
    }
}
