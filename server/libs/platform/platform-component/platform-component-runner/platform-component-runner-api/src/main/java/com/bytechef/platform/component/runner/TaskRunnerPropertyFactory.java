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
import static com.bytechef.platform.component.runner.TaskRunnerConstants.MODE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.OUTPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PROCESS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TIMEOUT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TRUSTED;
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
                .description(
                    "Where this task runs. GraalVM runs JavaScript or Python inside the server and is " +
                        "the only runner that gives the code a context, with component and log. Process " +
                        "starts the interpreter as a program on the server's machine. Docker, Kubernetes " +
                        "and AWS Batch run the code in a container. Bash scripts and commands need " +
                        "Process or a container runner.")
                .options(options)
                .defaultValue(defaultTaskRunnerType(taskRunners))
                .required(!taskRunners.isEmpty()));

        allProperties.addAll(properties);

        return object(TASK_RUNNER)
            .label("Task Runner")
            .description(
                "Chooses where and how this task's code runs. Each runner type shows its own " +
                    "settings, and only the runners an operator has enabled are offered.")
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
                .description(
                    "Environment variables the script or commands see, as name and value pairs. The " +
                        "host's own environment is not passed on unless the Process runner's Inherit " +
                        "Environment option is on, so add every variable the code needs here. ByteChef " +
                        "also sets BYTECHEF_WORKING_DIR (the folder the code runs in), " +
                        "BYTECHEF_OUTPUT_DIR, BYTECHEF_INPUT_FILE and, when the task has connections, " +
                        "BYTECHEF_CONNECTIONS_FILE.")
                .additionalProperties(string())
                .required(false));

        addIfSupported(
            properties, taskRunnerRegistry, TaskRunnerCapability.INPUT_FILES,
            object(INPUT_FILES)
                .label("Input Files")
                .description(
                    "Files placed in the working folder (BYTECHEF_WORKING_DIR) before the code runs, " +
                        "keyed by file name. A name may include folders, such as data/orders.csv. A value " +
                        "is either inline text, written as is, or a file from an earlier step, such as " +
                        "another task's output file, whose content is copied in. The code reads them by " +
                        "name, relative to the working folder.")
                .additionalProperties(string(), fileEntry())
                .required(false));

        addIfSupported(
            properties, taskRunnerRegistry, TaskRunnerCapability.OUTPUT_FILES,
            array(OUTPUT_FILES)
                .label("Output Files")
                .description(
                    "Glob patterns for the files to keep from the output folder (BYTECHEF_OUTPUT_DIR) " +
                        "after the code finishes, such as *.csv or charts/summary.png. Each matching " +
                        "file is stored and returned in the task's outputFiles, keyed by its path " +
                        "relative to the output folder, so a later step can use it, for example " +
                        "task_1.outputFiles['report.csv']. Without patterns no file is kept and " +
                        "outputFiles is empty. Files that match no pattern are discarded, and output.json " +
                        "is never kept as a file because it becomes the task's vars.")
                .items(string())
                .required(false));

        addTimeout(
            properties, taskRunnerRegistry,
            integer(TIMEOUT)
                .label("Timeout (seconds)")
                .description(
                    "How many seconds the code may run before it is stopped and the task fails. When " +
                        "empty, Process and GraalVM's Trusted mode stop it after 5 minutes, and container " +
                        "runners use Wait Until Completion. For container runners the operator's " +
                        "max-wait caps it.")
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

    private static void addTimeout(
        List<ModifiableValueProperty<?, ?>> properties, TaskRunnerRegistry taskRunnerRegistry,
        ModifiableValueProperty<?, ?> property) {

        List<TaskRunner> taskRunners = taskRunnerRegistry.getTaskRunners(Set.of());
        List<String> conditions = new ArrayList<>();

        for (TaskRunner taskRunner : taskRunners) {
            Set<TaskRunnerCapability> capabilities = taskRunner.getCapabilities();

            if (!capabilities.contains(TaskRunnerCapability.COMPONENT_BRIDGE)) {
                conditions.add("%s.%s == '%s'".formatted(TASK_RUNNER, TYPE, taskRunner.getType()));
            }
        }

        boolean hasGraalVmTaskRunner = taskRunners.stream()
            .anyMatch(taskRunner -> GRAALVM.equals(taskRunner.getType()));

        if (hasGraalVmTaskRunner) {
            conditions.add(
                "(%s.%s == '%s' && %s.%s == '%s')".formatted(
                    TASK_RUNNER, TYPE, GRAALVM, TASK_RUNNER, MODE, TRUSTED));
        }

        if (conditions.isEmpty()) {
            return;
        }

        property.displayCondition(String.join(" || ", conditions));

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

    public static String defaultTaskRunnerType(List<TaskRunner> taskRunners) {
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
