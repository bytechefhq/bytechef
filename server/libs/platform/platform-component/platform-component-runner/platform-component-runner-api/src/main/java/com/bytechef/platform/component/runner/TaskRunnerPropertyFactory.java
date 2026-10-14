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

package com.bytechef.platform.component.runner;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.DOCKER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENV;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.GRAALVM;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.INPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.OUTPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PROCESS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TIMEOUT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TYPE;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableOption;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class TaskRunnerPropertyFactory {

    private static final List<String> PREFERRED_DEFAULT_TYPES = List.of(GRAALVM, PROCESS, DOCKER);

    private TaskRunnerPropertyFactory() {
    }

    public static ModifiableObjectProperty taskRunnerProperty(
        TaskRunnerRegistry taskRunnerRegistry, Set<TaskRunnerCapability> requiredCapabilities) {

        List<TaskRunner> taskRunners = taskRunnerRegistry.getTaskRunners(requiredCapabilities);

        List<ModifiableOption<String>> options = new ArrayList<>();
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        for (TaskRunner taskRunner : taskRunners) {
            String type = taskRunner.getType();

            options.add(option(taskRunner.getTitle(), type));

            for (ModifiableValueProperty<?, ?> property : getFreshProperties(taskRunner)) {
                property.displayCondition("%s.%s == '%s'".formatted(TASK_RUNNER, TYPE, type));

                properties.add(property);
            }
        }

        List<ModifiableValueProperty<?, ?>> allProperties = new ArrayList<>();

        allProperties.add(
            string(TYPE)
                .label("Type")
                .description("Where this task runs.")
                .options(options)
                .defaultValue(defaultTaskRunnerType(taskRunners))
                .required(!taskRunners.isEmpty()));

        allProperties.addAll(properties);

        return object(TASK_RUNNER)
            .label("Task Runner")
            .description("Selects the environment this task executes in.")
            .properties(allProperties)
            .required(false)
            .expressionEnabled(false);
    }

    public static List<ModifiableValueProperty<?, ?>> externalProperties(TaskRunnerRegistry taskRunnerRegistry) {
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        addIfSupported(
            properties, taskRunnerRegistry, TaskRunnerCapability.ENVIRONMENT,
            object(ENV)
                .label("Environment")
                .description("Environment variables the execution sees. The host's own environment is not inherited.")
                .additionalProperties(string())
                .required(false));

        addIfSupported(
            properties, taskRunnerRegistry, TaskRunnerCapability.INPUT_FILES,
            object(INPUT_FILES)
                .label("Input Files")
                .description(
                    "Files written into the working directory before the execution, keyed by file name. A value may " +
                        "be inline text or a file entry.")
                .additionalProperties(string(), fileEntry())
                .required(false));

        addIfSupported(
            properties, taskRunnerRegistry, TaskRunnerCapability.OUTPUT_FILES,
            array(OUTPUT_FILES)
                .label("Output Files")
                .description("Glob patterns matched against the output directory after the execution.")
                .items(string())
                .required(false));

        addForExternalRunners(
            properties, taskRunnerRegistry,
            integer(TIMEOUT)
                .label("Timeout (seconds)")
                .description("How long the execution may run before it is killed.")
                .required(false));

        return properties;
    }

    private static void addIfSupported(
        List<ModifiableValueProperty<?, ?>> properties, TaskRunnerRegistry taskRunnerRegistry,
        TaskRunnerCapability capability, ModifiableValueProperty<?, ?> property) {

        List<TaskRunner> taskRunners = taskRunnerRegistry.getTaskRunners(Set.of(capability));

        if (taskRunners.isEmpty()) {
            return;
        }

        String condition = taskRunners.stream()
            .map(taskRunner -> "%s.%s == '%s'".formatted(TASK_RUNNER, TYPE, taskRunner.getType()))
            .collect(Collectors.joining(" || "));

        property.displayCondition(condition);

        properties.add(property);
    }

    private static void addForExternalRunners(
        List<ModifiableValueProperty<?, ?>> properties, TaskRunnerRegistry taskRunnerRegistry,
        ModifiableValueProperty<?, ?> property) {

        List<TaskRunner> taskRunners = taskRunnerRegistry.getTaskRunners(Set.of())
            .stream()
            .filter(taskRunner -> {
                Set<TaskRunnerCapability> capabilities = taskRunner.getCapabilities();

                return !capabilities.contains(TaskRunnerCapability.COMPONENT_BRIDGE);
            })
            .toList();

        if (taskRunners.isEmpty()) {
            return;
        }

        String condition = taskRunners.stream()
            .map(taskRunner -> "%s.%s == '%s'".formatted(TASK_RUNNER, TYPE, taskRunner.getType()))
            .collect(Collectors.joining(" || "));

        property.displayCondition(condition);

        properties.add(property);
    }

    private static List<? extends ModifiableValueProperty<?, ?>> getFreshProperties(TaskRunner taskRunner) {
        List<? extends ModifiableValueProperty<?, ?>> properties = taskRunner.getProperties();
        List<? extends ModifiableValueProperty<?, ?>> otherProperties = taskRunner.getProperties();

        for (ModifiableValueProperty<?, ?> property : properties) {
            for (ModifiableValueProperty<?, ?> otherProperty : otherProperties) {
                if (property == otherProperty) {
                    throw new IllegalStateException(
                        "Task runner '%s' returned a cached property from getProperties(); it must return freshly constructed properties on every call, because a displayCondition is stamped onto them"
                            .formatted(taskRunner.getType()));
                }
            }
        }

        return properties;
    }

    private static String defaultTaskRunnerType(List<TaskRunner> taskRunners) {
        if (taskRunners.isEmpty()) {
            return null;
        }

        for (String preferredType : PREFERRED_DEFAULT_TYPES) {
            for (TaskRunner taskRunner : taskRunners) {
                if (preferredType.equals(taskRunner.getType())) {
                    return preferredType;
                }
            }
        }

        return taskRunners.getFirst()
            .getType();
    }
}
