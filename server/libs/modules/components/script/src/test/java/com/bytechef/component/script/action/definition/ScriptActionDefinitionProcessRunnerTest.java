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

package com.bytechef.component.script.action.definition;

import static com.bytechef.component.script.constant.ScriptConstants.INPUT;
import static com.bytechef.platform.component.definition.ScriptComponentDefinition.SCRIPT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENV;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PROCESS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.script.action.ScriptBashAction;
import com.bytechef.component.script.action.ScriptJavaScriptAction;
import com.bytechef.component.script.action.ScriptPythonAction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerCapability;
import com.bytechef.platform.component.runner.TaskRunnerRegistry;
import com.bytechef.platform.component.runner.external.ProcessTaskRunner;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ScriptActionDefinitionProcessRunnerTest {

    private final TaskRunnerRegistry taskRunnerRegistry = mock(TaskRunnerRegistry.class);
    private final ProcessTaskRunner processTaskRunner = new ProcessTaskRunner(new ApplicationProperties());

    @Test
    void testTheJavaScriptActionRunsThroughTheProcessRunner() throws Exception {
        assumeTrue(isOnPath("node"));

        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(processTaskRunner);

        Object output = perform(
            ScriptJavaScriptAction.of(taskRunnerRegistry),
            "function perform(input, context) { return {doubled: input.value * 2}; }");

        assertThat(output).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) output;

        assertThat(result)
            .containsEntry("exitCode", 0)
            .containsEntry("vars", Map.of("doubled", 42));
    }

    @Test
    void testThePythonActionRunsThroughTheProcessRunner() throws Exception {
        assumeTrue(isOnPath("python3"));

        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(processTaskRunner);

        Object output = perform(
            ScriptPythonAction.of(taskRunnerRegistry),
            "def perform(input, context):\n    return {\"doubled\": input[\"value\"] * 2}\n");

        assertThat(output).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) output;

        assertThat(result)
            .containsEntry("exitCode", 0)
            .containsEntry("vars", Map.of("doubled", 42));
    }

    @Test
    void testTheBashActionRunsThroughTheProcessRunner() throws Exception {
        assumeTrue(isOnPath("bash"));

        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(processTaskRunner);

        Object output = perform(
            ScriptBashAction.of(taskRunnerRegistry),
            """
                set -euo pipefail
                declare -a values=(20 22)
                echo "sum=$(( values[0] + values[1] ))"
                tr -d ' ' < "$BYTECHEF_INPUT_FILE" > "$BYTECHEF_OUTPUT_DIR/output.json"
                """);

        assertThat(output).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) output;

        assertThat(result)
            .containsEntry("exitCode", 0)
            .containsEntry("stdout", "sum=42\n")
            .containsEntry("vars", Map.of("value", 21));
    }

    @Test
    void testTheBashActionExportsEachInputValueAsAnInputVariable() throws Exception {
        assumeTrue(isOnPath("bash"));

        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(processTaskRunner);

        MultipleConnectionsPerformFunction performFunction = ScriptBashAction.of(taskRunnerRegistry)
            .getPerform()
            .orElseThrow();

        Object output = performFunction.apply(
            ParametersFactory.create(
                Map.of(
                    SCRIPT, "printf '%s|%s|%s|%s' \"$INPUT_channel\" \"$INPUT_count\" \"$INPUT_tags\" \"$REGION\"",
                    INPUT, Map.of("channel", "#general", "count", 3, "tags", List.of("a"), "not valid", "x"),
                    ENV, Map.of("REGION", "eu"), TASK_RUNNER, Map.of(TYPE, PROCESS))),
            Map.of(), ParametersFactory.create(Map.of()), mock(ActionContext.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) output;

        assertThat(result).containsEntry("stdout", "#general|3|[\"a\"]|eu");
    }

    @Test
    void testTheBashActionFallsBackToTheFirstEnabledExternalRunner() throws Exception {
        assumeTrue(isOnPath("bash"));

        when(taskRunnerRegistry
            .getTaskRunners(Set.of(TaskRunnerCapability.INLINE_SCRIPT, TaskRunnerCapability.COMMANDS)))
                .thenReturn(List.of(processTaskRunner));
        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(processTaskRunner);

        MultipleConnectionsPerformFunction performFunction = ScriptBashAction.of(taskRunnerRegistry)
            .getPerform()
            .orElseThrow();

        Object output = performFunction.apply(
            ParametersFactory.create(Map.of(SCRIPT, "echo ok")), Map.of(), ParametersFactory.create(Map.of()),
            mock(ActionContext.class));

        assertThat(output).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) output;

        assertThat(result).containsEntry("stdout", "ok\n");
    }

    @Test
    void testTheBashActionFailsWhenNoExternalRunnerIsEnabled() {
        when(taskRunnerRegistry
            .getTaskRunners(Set.of(TaskRunnerCapability.INLINE_SCRIPT, TaskRunnerCapability.COMMANDS)))
                .thenReturn(List.of());

        MultipleConnectionsPerformFunction performFunction = ScriptBashAction.of(taskRunnerRegistry)
            .getPerform()
            .orElseThrow();

        assertThatThrownBy(() -> performFunction.apply(
            ParametersFactory.create(Map.of(SCRIPT, "echo ok")), Map.of(), ParametersFactory.create(Map.of()),
            mock(ActionContext.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bytechef.script.runners.process.enabled=true");
    }

    private static boolean isOnPath(String command) {
        String path = System.getenv("PATH");

        if (path == null) {
            return false;
        }

        return Stream.of(path.split(File.pathSeparator))
            .filter(entry -> !entry.isBlank())
            .map(entry -> Path.of(entry)
                .resolve(command))
            .anyMatch(Files::isExecutable);
    }

    private static Object perform(ScriptActionDefinition scriptActionDefinition, String script) throws Exception {
        MultipleConnectionsPerformFunction performFunction = scriptActionDefinition.getPerform()
            .orElseThrow();

        return performFunction.apply(
            ParametersFactory.create(
                Map.of(
                    SCRIPT, script, INPUT, Map.of("value", 21), TASK_RUNNER, Map.of(TYPE, PROCESS))),
            Map.of(), ParametersFactory.create(Map.of()), mock(ActionContext.class));
    }
}
