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

package com.bytechef.atlas.execution.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.DeferredEvaluationParameterKeys;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class TaskExecutionTest {

    private static final Evaluator EVALUATOR = new TestEvaluator();

    private static final Evaluator SPEL_EVALUATOR = SpelEvaluator.create();

    static {
        JsonMapper.Builder builder = JsonMapper.builder();

        MapUtils.setObjectMapper(builder.build());

        DeferredEvaluationParameterKeys.register("condition/", "caseTrue", "caseFalse");
    }

    @Test
    void testEvaluateWithoutDeferredKeys() {
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "regularTask",
                        WorkflowConstants.TYPE, "regularType/v1",
                        WorkflowConstants.PARAMETERS,
                        Map.of("input", "${myVar}"))))
            .build();

        taskExecution.evaluate(Map.of("myVar", "resolvedValue"), EVALUATOR);

        Map<String, ?> parameters = taskExecution.getParameters();

        assertEquals("resolvedValue", parameters.get("input"));
    }

    @Test
    void testEvaluateDefersCaseTrueAndCaseFalseForConditionTask() {
        List<Map<String, Object>> caseTrueTasks = List.of(
            Map.of(
                WorkflowConstants.NAME, "trueTask",
                WorkflowConstants.TYPE, "var/v1",
                WorkflowConstants.PARAMETERS, Map.of("value", "${shouldNotResolve}")));

        List<Map<String, Object>> caseFalseTasks = List.of(
            Map.of(
                WorkflowConstants.NAME, "falseTask",
                WorkflowConstants.TYPE, "var/v1",
                WorkflowConstants.PARAMETERS, Map.of("value", "${alsoShouldNotResolve}")));

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "conditionTask",
                        WorkflowConstants.TYPE, "condition/v1",
                        WorkflowConstants.PARAMETERS,
                        Map.of(
                            "conditions", List.of(),
                            "caseTrue", caseTrueTasks,
                            "caseFalse", caseFalseTasks))))
            .build();

        Map<String, String> context = Map.of(
            "shouldNotResolve", "WRONG_VALUE",
            "alsoShouldNotResolve", "ALSO_WRONG");

        taskExecution.evaluate(context, EVALUATOR);

        Map<String, ?> parameters = taskExecution.getParameters();

        // caseTrue and caseFalse should retain their original unevaluated expressions
        @SuppressWarnings("unchecked")
        List<Map<String, ?>> evaluatedCaseTrue = (List<Map<String, ?>>) parameters.get("caseTrue");

        @SuppressWarnings("unchecked")
        List<Map<String, ?>> evaluatedCaseFalse = (List<Map<String, ?>>) parameters.get("caseFalse");

        Map<String, ?> first = evaluatedCaseTrue.getFirst();

        @SuppressWarnings("unchecked")
        Map<String, ?> trueTaskParams = (Map<String, ?>) first.get(WorkflowConstants.PARAMETERS);

        first = evaluatedCaseFalse.getFirst();

        @SuppressWarnings("unchecked")
        Map<String, ?> falseTaskParams = (Map<String, ?>) first.get(WorkflowConstants.PARAMETERS);

        assertEquals(
            "${shouldNotResolve}", trueTaskParams.get("value"),
            "caseTrue sub-task expressions must NOT be evaluated");

        assertEquals(
            "${alsoShouldNotResolve}", falseTaskParams.get("value"),
            "caseFalse sub-task expressions must NOT be evaluated");
    }

    @Test
    void testEvaluateStillEvaluatesConditionParameters() {
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "conditionTask",
                        WorkflowConstants.TYPE, "condition/v1",
                        WorkflowConstants.PARAMETERS,
                        Map.of(
                            "conditionValue", "${myConditionVar}",
                            "caseTrue", List.of(),
                            "caseFalse", List.of()))))
            .build();

        taskExecution.evaluate(Map.of("myConditionVar", "evaluated"), EVALUATOR);

        Map<String, ?> parameters = taskExecution.getParameters();

        assertEquals("evaluated", parameters.get("conditionValue"), "Non-deferred parameters must still be evaluated");
    }

    @Test
    void testEvaluatePreservesDeferredParameterTypes() {
        List<Map<String, Object>> caseTrueTasks = List.of(
            Map.of(
                WorkflowConstants.NAME, "task1",
                WorkflowConstants.TYPE, "type1/v1"));

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "conditionTask",
                        WorkflowConstants.TYPE, "condition/v1",
                        WorkflowConstants.PARAMETERS,
                        Map.of(
                            "caseTrue", caseTrueTasks,
                            "caseFalse", List.of()))))
            .build();

        taskExecution.evaluate(Map.of(), EVALUATOR);

        Map<String, ?> parameters = taskExecution.getParameters();

        assertInstanceOf(List.class, parameters.get("caseTrue"));
        assertInstanceOf(List.class, parameters.get("caseFalse"));
    }

    @Test
    void testEvaluateHandlesNonRegisteredTaskTypeNormally() {
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "loopTask",
                        WorkflowConstants.TYPE, "loop/v1",
                        WorkflowConstants.PARAMETERS,
                        Map.of("iteratee", "${loopVar}"))))
            .build();

        taskExecution.evaluate(Map.of("loopVar", "resolved"), EVALUATOR);

        Map<String, ?> parameters = taskExecution.getParameters();

        assertEquals(
            "resolved", parameters.get("iteratee"),
            "Non-registered task types should have all parameters evaluated");
    }

    @Test
    void testEvaluateLeavesScriptSourceVerbatimWhileEvaluatingSiblings() {
        String script = """
            function perform(input, context) {
                const greeting = `Hi ${name}`;

                return `${greeting}, total: ${1 + 2}`;
            }""";

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "myScript",
                        WorkflowConstants.TYPE, "script/v1",
                        WorkflowConstants.PARAMETERS,
                        Map.of(
                            "script", script,
                            "input", Map.of("who", "${customerName}"),
                            "taskRunner", Map.of("type", "${runnerType}")))))
            .build();

        Map<String, String> context = Map.of(
            "customerName", "Ada",
            "runnerType", "process",
            "name", "SUBSTITUTED",
            "greeting", "ALSO_SUBSTITUTED");

        taskExecution.evaluate(context, SPEL_EVALUATOR);

        Map<String, ?> parameters = taskExecution.getParameters();

        assertEquals(script, parameters.get("script"), "the script source must reach the worker verbatim");

        @SuppressWarnings("unchecked")
        Map<String, ?> input = (Map<String, ?>) parameters.get("input");

        assertEquals("Ada", input.get("who"), "input values are the data channel and must still be evaluated");

        @SuppressWarnings("unchecked")
        Map<String, ?> taskRunner = (Map<String, ?>) parameters.get("taskRunner");

        assertEquals("process", taskRunner.get("type"), "sibling parameters must still be evaluated");
    }

    @Test
    void testClusterElementScriptSourceIsStillEvaluated() {
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(clusterElementHostTaskMap("return `Hi ${name}`;")))
            .build();

        taskExecution.evaluate(Map.of("name", "SUBSTITUTED"), SPEL_EVALUATOR);

        WorkflowTask workflowTask = taskExecution.getWorkflowTask();

        Map<String, ?> extensions = workflowTask.getExtensions();

        assertEquals("return `Hi SUBSTITUTED`;", clusterElementScript(extensions));
    }

    @Test
    void testClusterElementScriptSourceStillFailsTheTaskOnAnArithmeticTemplateLiteral() {
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(clusterElementHostTaskMap("return `Total: ${a + b}`;")))
            .build();

        assertThrowsExactly(
            IllegalArgumentException.class, () -> taskExecution.evaluate(Map.of(), SPEL_EVALUATOR));
    }

    private static Map<String, Object> clusterElementHostTaskMap(String script) {
        return Map.of(
            WorkflowConstants.NAME, "agent",
            WorkflowConstants.TYPE, "aiAgent/v1",
            WorkflowConstants.PARAMETERS, Map.of(),
            "clusterElements",
            Map.of(
                "tools",
                List.of(
                    Map.of(
                        WorkflowConstants.NAME, "scriptTool",
                        WorkflowConstants.TYPE, "script/v1/TOOLS/javascript",
                        WorkflowConstants.PARAMETERS, Map.of("script", script)))));
    }

    @SuppressWarnings("unchecked")
    private static Object clusterElementScript(Map<String, ?> extensions) {
        Map<String, ?> clusterElements = (Map<String, ?>) extensions.get("clusterElements");

        List<Map<String, ?>> tools = (List<Map<String, ?>>) clusterElements.get("tools");

        Map<String, ?> tool = tools.getFirst();

        Map<String, ?> parameters = (Map<String, ?>) tool.get(WorkflowConstants.PARAMETERS);

        return parameters.get("script");
    }

    private static class TestEvaluator implements Evaluator {

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> evaluate(Map<String, ?> map, Map<String, ?> context) {
            Map<String, Object> result = new LinkedHashMap<>();

            for (Map.Entry<String, ?> entry : map.entrySet()) {
                Object value = entry.getValue();

                switch (value) {
                    case String string when string.startsWith("${") -> {
                        String variableName = string.substring(2, string.length() - 1);
                        Object resolved = ((Map<String, Object>) context).getOrDefault(variableName, string);

                        result.put(entry.getKey(), resolved);
                    }
                    case Map<?, ?> nestedMap ->
                        result.put(entry.getKey(), evaluate((Map<String, ?>) nestedMap, context));
                    case List<?> list -> result.put(
                        entry.getKey(),
                        list.stream()
                            .map(
                                item -> item instanceof Map
                                    ? evaluate((Map<String, ?>) item, context)
                                    : item)
                            .toList());
                    case null, default -> result.put(entry.getKey(), value);
                }
            }

            return result;
        }
    }
}
