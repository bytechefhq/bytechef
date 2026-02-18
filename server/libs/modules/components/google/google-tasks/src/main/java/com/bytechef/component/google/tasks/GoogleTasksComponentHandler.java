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

package com.bytechef.component.google.tasks;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.tasks.action.GoogleTasksCreateTaskAction;
import com.bytechef.component.google.tasks.action.GoogleTasksListTasksAction;
import com.bytechef.component.google.tasks.action.GoogleTasksUpdateTaskAction;
import com.bytechef.component.google.tasks.connection.GoogleTasksConnection;
import com.bytechef.component.google.tasks.trigger.GoogleTasksNewTaskTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class GoogleTasksComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleTasks")
        .title("Google Tasks")
        .description(
            "Google Tasks is a cloud-based task management tool that allows users to create, edit, and organize " +
                "to-do lists, set deadlines, and track tasks across devices in real-time.")
        .customAction(true)
        .icon("path:assets/google-tasks.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .customAction(true)
        .customActionHelp(
            "Google Tasks API documentation",
            "https://developers.google.com/workspace/tasks/reference/rest")
        .connection(GoogleTasksConnection.CONNECTION_DEFINITION)
        .actions(
            GoogleTasksCreateTaskAction.ACTION_DEFINITION,
            GoogleTasksListTasksAction.ACTION_DEFINITION,
            GoogleTasksUpdateTaskAction.ACTION_DEFINITION)
        .triggers(GoogleTasksNewTaskTrigger.TRIGGER_DEFINITION)
        .clusterElements(
            tool(GoogleTasksCreateTaskAction.ACTION_DEFINITION),
            tool(GoogleTasksListTasksAction.ACTION_DEFINITION),
            tool(GoogleTasksUpdateTaskAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
