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
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link PerformFunction} stays backward compatible after gaining a {@link TaskContext}-aware default
 * method, and that both {@link WorkflowDsl.ModifiableTaskDefinition#perform} overloads work.
 *
 * @author Ivica Cardic
 */
class PerformFunctionCompatibilityTest {

    @Test
    void testZeroArgLambdaRunsViaApplyWithContext() throws Exception {
        PerformFunction performFunction = () -> "x";

        TaskContext context = new StubTaskContext();

        assertEquals("x", performFunction.apply(context));
    }

    @Test
    void testContextConsumingImplementationReceivesPassedContext() throws Exception {
        TaskContext expectedContext = new StubTaskContext();

        PerformFunction performFunction = new PerformFunction() {

            @Override
            public Object apply() {
                return null;
            }

            @Override
            public Object apply(TaskContext context) {
                return context;
            }
        };

        assertSame(expectedContext, performFunction.apply(expectedContext));
    }

    @Test
    void testDslAcceptsZeroArgLambda() throws Exception {
        WorkflowDsl.ModifiableTaskDefinition taskDefinition = WorkflowDsl.task("task1")
            .perform(() -> "x");

        PerformFunction performFunction = taskDefinition.getPerform();

        assertEquals("x", performFunction.apply(new StubTaskContext()));
    }

    @Test
    void testDslAcceptsContextConsumingLambda() throws Exception {
        WorkflowDsl.ModifiableTaskDefinition taskDefinition = WorkflowDsl.task("task1")
            .perform(context -> context.component("component1", "action1", Map.of(), "connection1"));

        PerformFunction performFunction = taskDefinition.getPerform();

        assertEquals("result", performFunction.apply(new StubTaskContext()));
    }

    private static class StubTaskContext implements TaskContext {

        @Override
        public Object component(
            String componentName, String actionName, Map<String, ?> input, String connectionName,
            Map<String, ?> clusterElements) {

            return "result";
        }

        @Override
        public Map<String, ?> input() {
            return Map.of();
        }

        @Override
        public Object input(String name) {
            return null;
        }

        @Override
        public Map<String, ?> parameters() {
            return Map.of();
        }

        @Override
        public Map<String, ?> connection(String connectionName) {
            return Map.of();
        }

        @Override
        public void log(LogLevel level, String message) {
        }
    }
}
