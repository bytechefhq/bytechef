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

package com.bytechef.component.commands.action;

import static com.bytechef.component.commands.constant.CommandsConstants.COMMANDS;
import static com.bytechef.component.commands.constant.CommandsConstants.SHELL;
import static com.bytechef.component.commands.constant.CommandsConstants.WARN_ON_STD_ERR;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.commands.action.definition.CommandsActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.platform.component.runner.TaskRunnerCapability;
import com.bytechef.platform.component.runner.TaskRunnerPropertyFactory;
import com.bytechef.platform.component.runner.TaskRunnerRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public class CommandsShellAction {

    public static CommandsActionDefinition of(TaskRunnerRegistry taskRunnerRegistry) {
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        properties.add(
            array(COMMANDS)
                .label("Commands")
                .description(
                    "The lines to run, in order. They are written into one script file and run by " +
                        "/bin/sh, so a variable set on one line is visible on the next. A failing line " +
                        "does not stop the lines after it unless the first line is set -e; the task fails " +
                        "when the script exits with a non-zero code. Write a JSON object to output.json " +
                        "in the folder named by the BYTECHEF_OUTPUT_DIR environment variable to return it " +
                        "as the task's vars.")
                .items(string())
                .required(true)
                .expressionEnabled(false));

        properties.add(
            TaskRunnerPropertyFactory.taskRunnerProperty(
                taskRunnerRegistry, Set.of(TaskRunnerCapability.COMMANDS)));
        properties.addAll(TaskRunnerPropertyFactory.externalProperties(taskRunnerRegistry));
        properties.add(
            bool(WARN_ON_STD_ERR)
                .label("Warn On Standard Error")
                .description(
                    "Logs a warning when the code writes anything to standard error, which also comes " +
                        "back in the result's stderr. On by default; turn it off for tools that print " +
                        "progress there.")
                .defaultValue(true)
                .required(false));

        return new CommandsActionDefinition(
            action(SHELL)
                .title("Shell")
                .description("Runs shell commands in the selected task runner.")
                .properties(properties)
                .output(),
            SHELL, taskRunnerRegistry);
    }

    private CommandsShellAction() {
    }
}
