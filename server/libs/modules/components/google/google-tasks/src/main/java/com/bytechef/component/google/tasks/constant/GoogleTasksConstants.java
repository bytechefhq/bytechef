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

package com.bytechef.component.google.tasks.constant;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Marija Horvat
 */
public class GoogleTasksConstants {

    public static final String ALL_TASKS = "allTasks";
    public static final String ID = "id";
    public static final String LIST_ID = "listId";
    public static final String MAX_RESULTS = "maxResults";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String NOTES = "notes";
    public static final String PAGE_TOKEN = "pageToken";
    public static final String SHOW_COMPLETED = "showCompleted";
    public static final String STATUS = "status";
    public static final String TASK_ID = "taskId";
    public static final String TITLE = "title";

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
