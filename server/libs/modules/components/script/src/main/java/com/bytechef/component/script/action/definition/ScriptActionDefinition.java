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
import static com.bytechef.platform.component.runner.TaskRunnerConstants.GRAALVM;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.INPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.OUTPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PROCESS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TIMEOUT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TYPE;

import com.bytechef.commons.util.JsonUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public class ScriptActionDefinition extends AbstractActionDefinitionWrapper {

    private static final String BASH = "bash";
    private static final Pattern INPUT_VARIABLE_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final String INPUT_VARIABLE_PREFIX = "INPUT_";

    private final String languageId;
    private final @Nullable Set<TaskRunnerCapability> requiredCapabilities;
    private final TaskRunnerRegistry taskRunnerRegistry;

    public ScriptActionDefinition(
        ActionDefinition actionDefinition, String languageId, TaskRunnerRegistry taskRunnerRegistry) {

        this(actionDefinition, languageId, null, taskRunnerRegistry);
    }

    public ScriptActionDefinition(
        ActionDefinition actionDefinition, String languageId,
        @Nullable Set<TaskRunnerCapability> requiredCapabilities, TaskRunnerRegistry taskRunnerRegistry) {

        super(actionDefinition);

        this.languageId = languageId;
        this.requiredCapabilities = requiredCapabilities == null ? null : Set.copyOf(requiredCapabilities);
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

        Map<String, Object> input = inputParameters.getMap(INPUT, Object.class, Map.of());

        TaskRunnerRequest taskRunnerRequest = new TaskRunnerRequest(
            languageId, inputParameters.getRequiredString(SCRIPT), List.of(), input,
            getEnvironment(inputParameters, input),
            inputParameters.getMap(INPUT_FILES, Object.class, Map.of()),
            inputParameters.getList(OUTPUT_FILES, String.class, List.of()), inputParameters, runnerParameters,
            timeoutSeconds == null ? null : Duration.ofSeconds(timeoutSeconds), connectionParameters, context);

        TaskRunnerResult taskRunnerResult = taskRunner.run(taskRunnerRequest);

        Set<TaskRunnerCapability> capabilities = taskRunner.getCapabilities();

        if (capabilities.contains(TaskRunnerCapability.COMPONENT_BRIDGE)) {
            return taskRunnerResult.output();
        }

        return taskRunnerResult.toExternalMap();
    }

    private Map<String, String> getEnvironment(Parameters inputParameters, Map<String, Object> input) {
        Map<String, String> environment = inputParameters.getMap(ENV, String.class, Map.of());

        if (!BASH.equals(languageId)) {
            return environment;
        }

        Map<String, String> bashEnvironment = new HashMap<>();

        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String name = entry.getKey();
            Matcher matcher = INPUT_VARIABLE_NAME.matcher(name);

            if (matcher.matches()) {

                Object value = entry.getValue();

                bashEnvironment.put(
                    INPUT_VARIABLE_PREFIX + name,
                    value == null ? "" : value instanceof String string ? string : JsonUtils.write(value));
            }
        }

        bashEnvironment.putAll(environment);

        return bashEnvironment;
    }

    private String defaultTaskRunnerType() {
        if (requiredCapabilities == null) {
            return GRAALVM;
        }

        List<TaskRunner> taskRunners = taskRunnerRegistry.getTaskRunners(requiredCapabilities);

        if (taskRunners.isEmpty()) {
            throw new IllegalStateException(
                ("No enabled task runner can run %s scripts. An operator must enable one with " +
                    "bytechef.script.runners.<type>.enabled=true - for the built-in '%s' runner that is " +
                    "bytechef.script.runners.%s.enabled=true.").formatted(languageId, PROCESS, PROCESS));
        }

        return taskRunners.getFirst()
            .getType();
    }
}
