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

package com.bytechef.workflow.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.bytechef.workflow.definition.TaskDefinition.PerformFunction;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PerformFunctionCompatibilityTest {

    @Test
    void testDslAcceptsInputContextLambda() throws Exception {
        TaskContext expectedContext = new StubTaskContext();

        WorkflowDsl.ModifiableTaskDefinition taskDefinition = WorkflowDsl.task("task1")
            .perform((input, context) -> List.of(input.get("orderId"), context));

        PerformFunction performFunction = taskDefinition.getPerform();

        Object result = performFunction.apply(
            Map.of("orderId", "42"), Map.of("crm", Map.of("token", "secret")), expectedContext);

        assertEquals(List.of("42", expectedContext), result);
    }

    @Test
    void testDslAcceptsInputConnectionContextLambda() throws Exception {
        WorkflowDsl.ModifiableTaskDefinition taskDefinition = WorkflowDsl.task("task1")
            .perform((input, connection, context) -> List.of(input.get("orderId"), connection.get("crm")));

        PerformFunction performFunction = taskDefinition.getPerform();

        Object result = performFunction.apply(
            Map.of("orderId", "42"), Map.of("crm", Map.of("token", "secret")), new StubTaskContext());

        assertEquals(List.of("42", Map.of("token", "secret")), result);
    }

    @Test
    void testDslInputContextLambdaCallsComponentThroughContext() throws Exception {
        WorkflowDsl.ModifiableTaskDefinition taskDefinition = WorkflowDsl.task("task1")
            .perform((input, context) -> context.component("component1", "action1", Map.of(), "connection1"));

        PerformFunction performFunction = taskDefinition.getPerform();

        assertSame("result", performFunction.apply(Map.of(), Map.of(), new StubTaskContext()));
    }

    private static class StubTaskContext implements TaskContext {

        @Override
        public Object component(
            String componentName, String actionName, Map<String, ?> input, String connectionName,
            Map<String, ?> clusterElements) {

            return "result";
        }

        @Override
        public void log(LogLevel level, String message) {
        }
    }
}
