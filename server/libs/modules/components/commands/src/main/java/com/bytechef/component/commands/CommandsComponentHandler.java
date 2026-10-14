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

package com.bytechef.component.commands;

import static com.bytechef.component.commands.constant.CommandsConstants.COMMANDS;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.commands.action.CommandsNodeAction;
import com.bytechef.component.commands.action.CommandsPythonAction;
import com.bytechef.component.commands.action.CommandsShellAction;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.runner.TaskRunnerRegistry;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(COMMANDS + "_v1_ComponentHandler")
public class CommandsComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public CommandsComponentHandler(TaskRunnerRegistry taskRunnerRegistry) {
        this.componentDefinition = new CommandsComponentDefinitionImpl(taskRunnerRegistry);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class CommandsComponentDefinitionImpl extends AbstractComponentDefinitionWrapper {

        private CommandsComponentDefinitionImpl(TaskRunnerRegistry taskRunnerRegistry) {
            super(
                component(COMMANDS)
                    .title("Commands")
                    .description(
                        "Runs shell, Python or Node commands in an external task runner, outside this JVM.")
                    .icon("path:assets/commands.svg")
                    .categories(ComponentCategory.HELPERS, ComponentCategory.DEVELOPER_TOOLS)
                    .actions(
                        CommandsShellAction.of(taskRunnerRegistry),
                        CommandsPythonAction.of(taskRunnerRegistry),
                        CommandsNodeAction.of(taskRunnerRegistry)));
        }
    }
}
