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

package com.bytechef.component.commands.action.definition;

import static com.bytechef.component.commands.constant.CommandsConstants.COMMANDS;
import static com.bytechef.component.commands.constant.CommandsConstants.WARN_ON_STD_ERR;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.DOCKER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENV;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.INPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.OUTPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PROCESS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TIMEOUT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.commands.action.CommandsShellAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CommandsActionDefinitionTest {

    private static final List<String> LINES = List.of("echo hi");

    private final TaskRunnerRegistry taskRunnerRegistry = mock(TaskRunnerRegistry.class);
    private final TaskRunner taskRunner = mock(TaskRunner.class);
    private final CommandsActionDefinition commandsActionDefinition = CommandsShellAction.of(taskRunnerRegistry);

    @Test
    void testPerformResolvesTheFirstCommandsCapableRunnerWhenTheWorkflowSelectsNoRunner() throws Exception {
        when(taskRunner.getType()).thenReturn(DOCKER);
        when(taskRunnerRegistry.getTaskRunners(Set.of(TaskRunnerCapability.COMMANDS))).thenReturn(List.of(taskRunner));
        when(taskRunnerRegistry.getTaskRunner(DOCKER)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(new TaskRunnerResult(Map.of(), 0, "hi", "", Map.of()));

        perform(Map.of(COMMANDS, LINES));

        verify(taskRunnerRegistry).getTaskRunner(DOCKER);
    }

    @Test
    void testPerformResolvesTheFirstCommandsCapableRunnerWhenTheSelectedTypeIsBlank() throws Exception {
        when(taskRunner.getType()).thenReturn(DOCKER);
        when(taskRunnerRegistry.getTaskRunners(Set.of(TaskRunnerCapability.COMMANDS))).thenReturn(List.of(taskRunner));
        when(taskRunnerRegistry.getTaskRunner(DOCKER)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(new TaskRunnerResult(Map.of(), 0, "hi", "", Map.of()));

        perform(Map.of(COMMANDS, LINES, TASK_RUNNER, Map.of(TYPE, "")));

        verify(taskRunnerRegistry).getTaskRunner(DOCKER);
    }

    @Test
    void testPerformResolvesTheSelectedRunnerType() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(new TaskRunnerResult(Map.of(), 0, "hi", "", Map.of()));

        clearInvocations(taskRunnerRegistry);

        perform(Map.of(COMMANDS, LINES, TASK_RUNNER, Map.of(TYPE, PROCESS)));

        verify(taskRunnerRegistry).getTaskRunner(PROCESS);
        verify(taskRunnerRegistry, never()).getTaskRunners(any());
    }

    @Test
    void testPerformFailsWithAnActionableMessageWhenNoRunnerDeclaresCommands() {
        when(taskRunnerRegistry.getTaskRunners(Set.of(TaskRunnerCapability.COMMANDS))).thenReturn(List.of());

        assertThatThrownBy(() -> perform(Map.of(COMMANDS, LINES)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No enabled task runner can run commands")
            .hasMessageContaining("bytechef.script.runners.process.enabled=true")
            .hasMessageNotContaining("runners.null.enabled");

        verify(taskRunnerRegistry, never()).getTaskRunner(any());
    }

    @Test
    void testPerformBuildsTheRequestFromBothParameterMaps() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(new TaskRunnerResult(Map.of(), 0, "hi", "", Map.of()));

        perform(
            Map.of(
                COMMANDS, LINES,
                ENV, Map.of("API_KEY", "secret"),
                INPUT_FILES, Map.of("data.csv", "a,b"),
                OUTPUT_FILES, List.of("*.csv"),
                TIMEOUT, 30,
                TASK_RUNNER, Map.of(TYPE, PROCESS, "interpreter", "/bin/bash")));

        ArgumentCaptor<TaskRunnerRequest> requestCaptor = ArgumentCaptor.forClass(TaskRunnerRequest.class);

        verify(taskRunner).run(requestCaptor.capture());

        TaskRunnerRequest taskRunnerRequest = requestCaptor.getValue();

        assertThat(taskRunnerRequest.languageId()).isEqualTo("shell");
        assertThat(taskRunnerRequest.script()).isNull();
        assertThat(taskRunnerRequest.commands()).isEqualTo(LINES);
        assertThat(taskRunnerRequest.timeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(taskRunnerRequest.env()).containsEntry("API_KEY", "secret");
        assertThat(taskRunnerRequest.inputFiles()
            .get("data.csv")).isEqualTo("a,b");
        assertThat(taskRunnerRequest.outputFilePatterns()).containsExactly("*.csv");

        Parameters runnerParameters = taskRunnerRequest.runnerParameters();

        assertThat(runnerParameters.getString("interpreter")).isEqualTo("/bin/bash");

        Parameters inputParameters = taskRunnerRequest.inputParameters();

        assertThat(inputParameters.getList(COMMANDS, String.class)).isEqualTo(LINES);
    }

    @Test
    void testPerformDefaultsTimeoutToNullWhenAbsent() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(new TaskRunnerResult(Map.of(), 0, "", "", Map.of()));

        perform(Map.of(COMMANDS, LINES, TASK_RUNNER, Map.of(TYPE, PROCESS)));

        ArgumentCaptor<TaskRunnerRequest> requestCaptor = ArgumentCaptor.forClass(TaskRunnerRequest.class);

        verify(taskRunner).run(requestCaptor.capture());

        TaskRunnerRequest taskRunnerRequest = requestCaptor.getValue();

        assertThat(taskRunnerRequest.timeout()).isNull();
        assertThat(taskRunnerRequest.env()).isEmpty();
        assertThat(taskRunnerRequest.inputFiles()).isEmpty();
        assertThat(taskRunnerRequest.outputFilePatterns()).isEmpty();
    }

    @Test
    void testPerformPropagatesTheAllowlistRejection() {
        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenThrow(new TaskRunnerNotEnabledException(PROCESS));

        assertThatThrownBy(() -> perform(Map.of(COMMANDS, LINES, TASK_RUNNER, Map.of(TYPE, PROCESS))))
            .isInstanceOf(TaskRunnerNotEnabledException.class)
            .hasMessageContaining(PROCESS);
    }

    @Test
    void testPerformRejectsMissingCommands() {
        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(taskRunner);

        assertThatThrownBy(() -> perform(Map.of(TASK_RUNNER, Map.of(TYPE, PROCESS))))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining(COMMANDS);

        verify(taskRunner, never()).run(any());
    }

    @Test
    void testPerformAlwaysReturnsTheFullResultMap() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(taskRunner);
        when(taskRunner.getCapabilities())
            .thenReturn(Set.of(TaskRunnerCapability.COMMANDS, TaskRunnerCapability.COMPONENT_BRIDGE));
        when(taskRunner.run(any())).thenReturn(new TaskRunnerResult(Map.of("total", 6), 0, "hello", "", Map.of()));

        Object output = perform(Map.of(COMMANDS, LINES, TASK_RUNNER, Map.of(TYPE, PROCESS)));

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
    void testPerformReturnsTheFullResultWhenTheExitCodeIsNull() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(new TaskRunnerResult("output", null, "", "", Map.of()));

        Object output = perform(Map.of(COMMANDS, LINES, TASK_RUNNER, Map.of(TYPE, PROCESS)));

        assertThat(output).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) output;

        assertThat(result).containsKey("exitCode");
        assertThat(result.get("exitCode")).isNull();
    }

    @Test
    void testPerformLogsAWarningWhenStdErrIsNonEmptyAndWarnOnStdErrIsTrue() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(new TaskRunnerResult(Map.of(), 1, "", "boom", Map.of()));

        Context.Log log = mock(Context.Log.class);
        ActionContext context = mock(ActionContext.class);

        doAnswer(invocation -> {
            Context.ContextConsumer<Context.Log> consumer = invocation.getArgument(0);

            consumer.accept(log);

            return null;
        }).when(context)
            .log(any());

        perform(Map.of(COMMANDS, LINES, TASK_RUNNER, Map.of(TYPE, PROCESS)), context);

        verify(log).warn("boom");
    }

    @Test
    void testPerformDoesNotLogWhenStdErrIsEmpty() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(new TaskRunnerResult(Map.of(), 0, "", "", Map.of()));

        ActionContext context = mock(ActionContext.class);

        perform(Map.of(COMMANDS, LINES, TASK_RUNNER, Map.of(TYPE, PROCESS)), context);

        verify(context, never()).log(any());
    }

    @Test
    void testPerformDoesNotLogWhenWarnOnStdErrIsFalse() throws Exception {
        when(taskRunnerRegistry.getTaskRunner(PROCESS)).thenReturn(taskRunner);
        when(taskRunner.run(any())).thenReturn(new TaskRunnerResult(Map.of(), 1, "", "boom", Map.of()));

        ActionContext context = mock(ActionContext.class);

        perform(Map.of(COMMANDS, LINES, TASK_RUNNER, Map.of(TYPE, PROCESS), WARN_ON_STD_ERR, false), context);

        verify(context, never()).log(any());
    }

    private Object perform(Map<String, ?> inputParameters) throws Exception {
        return perform(inputParameters, null);
    }

    private Object perform(Map<String, ?> inputParameters, ActionContext context) throws Exception {
        MultipleConnectionsPerformFunction performFunction = commandsActionDefinition.getPerform()
            .orElseThrow();

        return performFunction.apply(
            ParametersFactory.create(inputParameters), Map.of(), ParametersFactory.create(Map.of()), context);
    }
}
