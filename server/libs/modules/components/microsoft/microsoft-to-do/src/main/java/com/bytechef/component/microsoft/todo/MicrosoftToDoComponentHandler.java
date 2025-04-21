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

package com.bytechef.component.microsoft.todo;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.microsoft.todo.action.MicrosoftToDoCreateTaskAction;
import com.bytechef.component.microsoft.todo.action.MicrosoftToDoCreateTaskListAction;
import com.bytechef.component.microsoft.todo.action.MicrosoftToDoGetTaskAction;
import com.bytechef.component.microsoft.todo.connection.MicrosoftToDoConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class MicrosoftToDoComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("microsoftToDo")
        .title("Microsoft To Do")
        .description(
            "Microsoft To Do is a cloud-based task management application that helps users organize, prioritize, and " +
                "track tasks across devices with features like lists, reminders, and collaboration.")
        .icon("path:assets/microsoft-to-do.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(MicrosoftToDoConnection.CONNECTION_DEFINITION)
        .actions(
            MicrosoftToDoCreateTaskAction.ACTION_DEFINITION,
            MicrosoftToDoCreateTaskListAction.ACTION_DEFINITION,
            MicrosoftToDoGetTaskAction.ACTION_DEFINITION)
        .clusterElements(
            tool(MicrosoftToDoCreateTaskAction.ACTION_DEFINITION),
            tool(MicrosoftToDoCreateTaskListAction.ACTION_DEFINITION),
            tool(MicrosoftToDoGetTaskAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
