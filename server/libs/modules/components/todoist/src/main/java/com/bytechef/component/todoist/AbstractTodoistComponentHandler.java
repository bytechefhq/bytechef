/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.todoist;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.todoist.action.TodoistCreateProjectAction;
import com.bytechef.component.todoist.action.TodoistCreateTaskAction;
import com.bytechef.component.todoist.action.TodoistMarkTaskCompletedAction;
import com.bytechef.component.todoist.connection.TodoistConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractTodoistComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("todoist")
            .title("Todoist")
            .description(
                "Todoist is a task management application that helps users organize and prioritize their to-do lists."))
                    .actions(modifyActions(TodoistCreateTaskAction.ACTION_DEFINITION,
                        TodoistMarkTaskCompletedAction.ACTION_DEFINITION, TodoistCreateProjectAction.ACTION_DEFINITION))
                    .connection(modifyConnection(TodoistConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
