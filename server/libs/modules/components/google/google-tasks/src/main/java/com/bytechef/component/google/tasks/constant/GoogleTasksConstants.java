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

package com.bytechef.component.google.tasks.constant;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Marija Horvat
 */
public class GoogleTasksConstants {

    public static final String CREATE_TASK = "createTask";
    public static final String CREATE_TASK_DESCRIPTION = "Creates a new task on the specified task list.";
    public static final String CREATE_TASK_TITLE = "Create Task";
    public static final String LIST_ID = "listId";
    public static final String LIST_TASKS = "listTasks";
    public static final String LIST_TASKS_DESCRIPTION = "Returns all tasks in the specified task list.";
    public static final String LIST_TASKS_TITLE = "List Tasks";
    public static final String NOTES = "notes";
    public static final String SHOW_COMPLETED = "showCompleted";
    public static final String STATUS = "status";
    public static final String TASK_ID = "taskId";
    public static final String TITLE = "title";
    public static final String UPDATE_TASK = "updateTask";
    public static final String UPDATE_TASK_DESCRIPTION = "Updates a specific task on the specified task list.";
    public static final String UPDATE_TASK_TITLE = "Update Task";

    public static final ModifiableObjectProperty TASK_OUTPUT_PROPERTY =
        object()
            .properties(
                string("id")
                    .description("Task identifier."),
                string(TITLE)
                    .description("Title of the task."),
                string(NOTES)
                    .description("Notes describing the task."),
                string(STATUS)
                    .description("Status of the task."));

    private GoogleTasksConstants() {
    }
}
