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
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENV;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.INPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.OUTPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PROCESS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TIMEOUT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunner;
import com.bytechef.platform.component.runner.TaskRunnerCapability;
import com.bytechef.platform.component.runner.TaskRunnerRegistry;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public class CommandsActionDefinition extends AbstractActionDefinitionWrapper {

    private final String languageId;
    private final TaskRunnerRegistry taskRunnerRegistry;

    public CommandsActionDefinition(
        ActionDefinition actionDefinition, String languageId, TaskRunnerRegistry taskRunnerRegistry) {

        super(actionDefinition);

        this.languageId = languageId;
        this.taskRunnerRegistry = taskRunnerRegistry;
    }

    @Override
    public Optional<MultipleConnectionsPerformFunction> getPerform() {
        return Optional.of(this::perform);
    }

    protected Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
        Parameters extensions, ActionContext context) {

        Parameters runnerParameters = ParametersFactory.create(
            inputParameters.getMap(TASK_RUNNER, Object.class, Map.of()));

        String type = runnerParameters.getString(TYPE);

        if (type == null || type.isBlank()) {
            type = defaultTaskRunnerType();
        }

        TaskRunner taskRunner = taskRunnerRegistry.getTaskRunner(type);

        Integer timeoutSeconds = inputParameters.getInteger(TIMEOUT);

        TaskRunnerRequest taskRunnerRequest = new TaskRunnerRequest(
            languageId, null, inputParameters.getRequiredList(COMMANDS, String.class), Map.of(),
            inputParameters.getMap(ENV, String.class, Map.of()),
            inputParameters.getMap(INPUT_FILES, Object.class, Map.of()),
            inputParameters.getList(OUTPUT_FILES, String.class, List.of()), inputParameters, runnerParameters,
            timeoutSeconds == null ? null : Duration.ofSeconds(timeoutSeconds), connectionParameters, context);

        TaskRunnerResult taskRunnerResult = taskRunner.run(taskRunnerRequest);

        if (inputParameters.getBoolean(WARN_ON_STD_ERR, true) && !taskRunnerResult.stderr()
            .isEmpty()) {

            context.log(log -> log.warn(taskRunnerResult.stderr()));
        }

        return taskRunnerResult.toExternalMap();
    }

    private String defaultTaskRunnerType() {
        List<TaskRunner> taskRunners = taskRunnerRegistry.getTaskRunners(Set.of(TaskRunnerCapability.COMMANDS));

        if (taskRunners.isEmpty()) {
            throw new IllegalStateException(
                ("No enabled task runner can run commands. An operator must enable one with " +
                    "bytechef.script.runners.<type>.enabled=true - for the built-in '%s' runner that is " +
                    "bytechef.script.runners.%s.enabled=true.").formatted(PROCESS, PROCESS));
        }

        return taskRunners.getFirst()
            .getType();
    }
}
