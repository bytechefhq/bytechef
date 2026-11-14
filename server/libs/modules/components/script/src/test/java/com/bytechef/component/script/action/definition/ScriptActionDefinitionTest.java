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

import static com.bytechef.platform.component.definition.ScriptComponentDefinition.SCRIPT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.DOCKER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENV;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.GRAALVM;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.INPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.MODE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.OUTPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TIMEOUT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TRUSTED;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.script.action.ScriptJavaScriptAction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunner;
import com.bytechef.platform.component.runner.TaskRunnerCapability;
import com.bytechef.platform.component.runner.TaskRunnerNotEnabledException;
import com.bytechef.platform.component.runner.TaskRunnerRegistry;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class ScriptActionDefinitionTest {

    private static final String SOURCE = "function perform(input, context) { return null; }";

    private final TaskRunnerRegistry taskRunnerRegistry = mock(TaskRunnerRegistry.class);
    private final TaskRunner taskRunner = mock(TaskRunner.class);
    private final ScriptActionDefinition scriptActionDefinition = ScriptJavaScriptAction.of(taskRunnerRegistry);

    @BeforeEach
    void setUp() {
        when(taskRunner.getCapabilities()).thenReturn(Set.of(TaskRunnerCapability.COMPONENT_BRIDGE));
    }

    @Test
    public void testPerformResolvesGraalVmWhenTheWorkflowSelectsNoRunner() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(GRAALVM)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(TaskRunnerResult.ofOutput("output"));

        Object output = perform(Map.of(SCRIPT, SOURCE, "input", Map.of("factor", 3)));

        assertThat(output).isEqualTo("output");

        verify(taskRunnerRegistry).getTaskRunner(GRAALVM);
    }

    @Test
    public void testPerformResolvesGraalVmWhenTheSelectedTypeIsBlank() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(GRAALVM)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(TaskRunnerResult.ofOutput("output"));

        Object output = perform(Map.of(SCRIPT, SOURCE, TASK_RUNNER, Map.of(TYPE, "")));

        assertThat(output).isEqualTo("output");

        verify(taskRunnerRegistry).getTaskRunner(GRAALVM);
    }

    @Test
    public void testPerformResolvesTheSelectedRunnerType() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(DOCKER)).thenReturn(taskRunner);
        when(taskRunner.getCapabilities())
            .thenReturn(Set.of(TaskRunnerCapability.INLINE_SCRIPT, TaskRunnerCapability.COMMANDS));
        when(taskRunner.run(any())).thenReturn(new TaskRunnerResult(Map.of("greeting", "hi"), 0, "hi", "", Map.of()));

        Object output = perform(Map.of(SCRIPT, SOURCE, TASK_RUNNER, Map.of(TYPE, DOCKER)));

        assertThat(output).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) output;

        assertThat(result)
            .containsEntry("exitCode", 0)
            .containsEntry("stdout", "hi")
            .containsEntry("stderr", "")
            .containsEntry("vars", Map.of("greeting", "hi"))
            .containsEntry("outputFiles", Map.of());

        verify(taskRunnerRegistry).getTaskRunner(DOCKER);
    }

    @Test
    public void testPerformBuildsTheRequestFromBothParameterMaps() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(GRAALVM)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(TaskRunnerResult.ofOutput("output"));

        perform(
            Map.of(
                SCRIPT, SOURCE, "input", Map.of("factor", 3),
                ENV, Map.of("API_KEY", "secret"),
                INPUT_FILES, Map.of("data.csv", "a,b"),
                OUTPUT_FILES, List.of("*.csv"),
                TIMEOUT, 30,
                TASK_RUNNER, Map.of(TYPE, GRAALVM, MODE, TRUSTED)));

        ArgumentCaptor<TaskRunnerRequest> requestCaptor = ArgumentCaptor.forClass(TaskRunnerRequest.class);

        verify(taskRunner).run(requestCaptor.capture());

        TaskRunnerRequest taskRunnerRequest = requestCaptor.getValue();

        assertThat(taskRunnerRequest.languageId()).isEqualTo("js");
        assertThat(taskRunnerRequest.script()).isEqualTo(SOURCE);
        assertThat(taskRunnerRequest.commands()).isEmpty();
        assertThat(taskRunnerRequest.timeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(taskRunnerRequest.env()).containsEntry("API_KEY", "secret");
        assertThat(taskRunnerRequest.inputFiles()
            .get("data.csv")).isEqualTo("a,b");
        assertThat(taskRunnerRequest.outputFilePatterns()).containsExactly("*.csv");

        Map<String, ?> input = taskRunnerRequest.input();

        assertThat(input.get("factor")).isEqualTo(3);

        Parameters runnerParameters = taskRunnerRequest.runnerParameters();

        assertThat(runnerParameters.getString(MODE)).isEqualTo(TRUSTED);

        Parameters inputParameters = taskRunnerRequest.inputParameters();

        assertThat(inputParameters.getString(SCRIPT)).isEqualTo(SOURCE);
        assertThat(inputParameters.getString(MODE)).isNull();
    }

    @Test
    public void testPerformDefaultsTimeoutToNullWhenAbsent() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(GRAALVM)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(TaskRunnerResult.ofOutput("output"));

        perform(Map.of(SCRIPT, SOURCE));

        ArgumentCaptor<TaskRunnerRequest> requestCaptor = ArgumentCaptor.forClass(TaskRunnerRequest.class);

        verify(taskRunner).run(requestCaptor.capture());

        TaskRunnerRequest taskRunnerRequest = requestCaptor.getValue();

        assertThat(taskRunnerRequest.timeout()).isNull();
        assertThat(taskRunnerRequest.env()).isEmpty();
        assertThat(taskRunnerRequest.inputFiles()).isEmpty();
        assertThat(taskRunnerRequest.outputFilePatterns()).isEmpty();
    }

    @Test
    public void testPerformPropagatesTheAllowlistRejection() {
        when(taskRunnerRegistry.getTaskRunner(GRAALVM)).thenThrow(new TaskRunnerNotEnabledException(GRAALVM));

        assertThatThrownBy(() -> perform(Map.of(SCRIPT, SOURCE)))
            .isInstanceOf(TaskRunnerNotEnabledException.class)
            .hasMessageContaining(GRAALVM);
    }

    @Test
    public void testPerformRejectsAMissingScript() {
        when(taskRunnerRegistry.getTaskRunner(GRAALVM)).thenReturn(taskRunner);

        assertThatThrownBy(() -> perform(Map.of("input", Map.of("factor", 3))))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining(SCRIPT);

        verify(taskRunner, never()).run(any());
    }

    @Test
    public void testPerformReturnsTheBareValueForARunnerWithTheComponentBridge() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(GRAALVM)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(TaskRunnerResult.ofOutput("output"));

        Object output = perform(Map.of(SCRIPT, SOURCE));

        assertThat(output).isEqualTo("output");
    }

    @Test
    public void testPerformReturnsTheFullResultForARunnerWithoutTheComponentBridge() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(GRAALVM)).thenReturn(taskRunner);
        when(taskRunner.getCapabilities()).thenReturn(Set.of(TaskRunnerCapability.INLINE_SCRIPT));
        when(taskRunner.run(any())).thenReturn(
            new TaskRunnerResult(Map.of("total", 6), 0, "hello", "", Map.of()));

        Object output = perform(Map.of(SCRIPT, SOURCE));

        assertThat(output).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) output;

        assertThat(result)
            .containsEntry("exitCode", 0)
            .containsEntry("stdout", "hello")
            .containsEntry("stderr", "")
            .containsEntry("vars", Map.of("total", 6))
            .containsEntry("outputFiles", Map.of());
    }

    @Test
    public void testPerformReturnsTheFullResultWhenTheExitCodeIsNull() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(GRAALVM)).thenReturn(taskRunner);
        when(taskRunner.getCapabilities()).thenReturn(Set.of(TaskRunnerCapability.INLINE_SCRIPT));
        when(taskRunner.run(any())).thenReturn(TaskRunnerResult.ofOutput("output"));

        Object output = perform(Map.of(SCRIPT, SOURCE));

        assertThat(output).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) output;

        assertThat(result).containsKey("exitCode");
        assertThat(result.get("exitCode")).isNull();
    }

    private Object perform(Map<String, ?> inputParameters) throws Exception {
        MultipleConnectionsPerformFunction performFunction = scriptActionDefinition.getPerform()
            .orElseThrow();

        return performFunction.apply(
            ParametersFactory.create(inputParameters), Map.of(), ParametersFactory.create(Map.of()), null);
    }
}
