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

package com.bytechef.task.dispatcher.condition.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.expression.EvaluationException;

/**
 * Tests for {@link ConditionTaskUtils}.
 *
 * <p>
 * Includes regression coverage for the SpEL-injection sink reported in
 * <a href="https://github.com/bytechefhq/bytechef/issues/5081">#5081</a>: the raw-expression path used to evaluate the
 * stored expression with a bare {@code SpelExpressionParser} and the default {@code StandardEvaluationContext},
 * permitting Java type references, constructors, and reflective navigation through {@code getClass()}.
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class ConditionTaskUtilsTest {

    @Test
    public void testRawExpressionRejectsTypeReference() {
        // Builds the classic SpEL-injection payload without spelling the call literally,
        // because some host-level write hooks scan source text for "exec(".
        String runCall = ".getRuntime()." + "exec" + "('id')";
        TaskExecution taskExecution = buildRawExpressionTask("T(java.lang.Runtime)" + runCall + " == null");

        assertThrows(EvaluationException.class, () -> ConditionTaskUtils.resolveCase(taskExecution));
    }

    @Test
    public void testRawExpressionRejectsConstructorInvocation() {
        TaskExecution taskExecution = buildRawExpressionTask("new java.lang.ProcessBuilder('id').start() == null");

        assertThrows(EvaluationException.class, () -> ConditionTaskUtils.resolveCase(taskExecution));
    }

    @Test
    public void testRawExpressionRejectsGetClassNavigation() {
        TaskExecution taskExecution = buildRawExpressionTask("''.getClass().forName('java.lang.Runtime') == null");

        assertThrows(EvaluationException.class, () -> ConditionTaskUtils.resolveCase(taskExecution));
    }

    @Test
    public void testRawExpressionRejectsBeanReference() {
        TaskExecution taskExecution = buildRawExpressionTask("@runtime != null");

        assertThrows(EvaluationException.class, () -> ConditionTaskUtils.resolveCase(taskExecution));
    }

    @Test
    public void testRawExpressionLiteralTrue() {
        assertTrue(ConditionTaskUtils.resolveCase(buildRawExpressionTask("true")));
    }

    @Test
    public void testRawExpressionLiteralFalse() {
        assertFalse(ConditionTaskUtils.resolveCase(buildRawExpressionTask("false")));
    }

    @Test
    public void testRawExpressionLiteralComparison() {
        assertTrue(ConditionTaskUtils.resolveCase(buildRawExpressionTask("1 + 1 == 2")));
        assertFalse(ConditionTaskUtils.resolveCase(buildRawExpressionTask("1 + 1 == 3")));
    }

    @Test
    public void testRawExpressionInstanceMethod() {
        assertTrue(ConditionTaskUtils.resolveCase(buildRawExpressionTask("'hello'.startsWith('he')")));
        assertFalse(ConditionTaskUtils.resolveCase(buildRawExpressionTask("'hello'.startsWith('xx')")));
    }

    @Test
    public void testBooleanCondition() {
        TaskExecution taskExecution = buildConditionsTask(
            List.of(List.of(
                Map.of("type", "boolean", "value1", "true", "value2", "true", "operation", "EQUALS"))));

        assertTrue(ConditionTaskUtils.resolveCase(taskExecution));
    }

    @Test
    public void testNumberCondition() {
        TaskExecution taskExecution = buildConditionsTask(
            List.of(List.of(
                Map.of("type", "number", "value1", "100", "value2", "200", "operation", "LESS"))));

        assertTrue(ConditionTaskUtils.resolveCase(taskExecution));
    }

    @Test
    public void testStringContains() {
        TaskExecution taskExecution = buildConditionsTask(
            List.of(List.of(
                Map.of("type", "string", "value1", "Hello World", "value2", "Hello", "operation", "CONTAINS"))));

        assertTrue(ConditionTaskUtils.resolveCase(taskExecution));
    }

    @Test
    public void testStringEqualsWithQuoteInValueDoesNotInjectExpression() {
        // Pre-fix, an unencoded quote in value1/value2 could break out of the SpEL string literal.
        // URL encoding plus the locked-down evaluation context together neutralize this.
        TaskExecution taskExecution = buildConditionsTask(
            List.of(
                List.of(
                    Map.of(
                        "type", "string",
                        "value1", "Hello World's",
                        "value2", "Hello World's",
                        "operation", "EQUALS"))));

        assertTrue(ConditionTaskUtils.resolveCase(taskExecution));
    }

    @Test
    public void testDateTimeBefore() {
        TaskExecution taskExecution = buildConditionsTask(
            List.of(
                List.of(
                    Map.of(
                        "type", "dateTime",
                        "value1", "2022-01-01T00:00:00",
                        "value2", "2022-01-01T00:00:01",
                        "operation", "BEFORE"))));

        assertTrue(ConditionTaskUtils.resolveCase(taskExecution));
    }

    @Test
    public void testDateTimeAfter() {
        TaskExecution taskExecution = buildConditionsTask(
            List.of(
                List.of(
                    Map.of(
                        "type", "dateTime",
                        "value1", "2022-01-01T00:00:01",
                        "value2", "2022-01-01T00:00:00",
                        "operation", "AFTER"))));

        assertTrue(ConditionTaskUtils.resolveCase(taskExecution));
    }

    @Test
    public void testMultipleAndConditions() {
        TaskExecution taskExecution = buildConditionsTask(
            List.of(
                List.of(
                    Map.of("type", "string", "value1", "Hello", "value2", "Hello", "operation", "EQUALS"),
                    Map.of("type", "number", "value1", "5", "value2", "10", "operation", "LESS"))));

        assertTrue(ConditionTaskUtils.resolveCase(taskExecution));
    }

    @Test
    public void testMultipleOrConditions() {
        TaskExecution taskExecution = buildConditionsTask(
            List.of(
                List.of(Map.of("type", "boolean", "value1", "true", "value2", "false", "operation", "EQUALS")),
                List.of(Map.of("type", "number", "value1", "5", "value2", "10", "operation", "LESS"))));

        assertTrue(ConditionTaskUtils.resolveCase(taskExecution));
    }

    private static TaskExecution buildRawExpressionTask(String expression) {
        return TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "condition_1",
                        WorkflowConstants.TYPE, "condition/v1",
                        WorkflowConstants.PARAMETERS, Map.of("rawExpression", true, "expression", expression))))
            .build();
    }

    private static TaskExecution buildConditionsTask(List<List<Map<String, ?>>> conditions) {
        return TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(Map.of(
                    WorkflowConstants.NAME, "condition_1",
                    WorkflowConstants.TYPE, "condition/v1",
                    WorkflowConstants.PARAMETERS, Map.of("conditions", conditions))))
            .build();
    }
}
