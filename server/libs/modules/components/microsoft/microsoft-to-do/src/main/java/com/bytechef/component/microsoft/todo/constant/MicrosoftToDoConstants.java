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

package com.bytechef.component.microsoft.todo.constant;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public class MicrosoftToDoConstants {

    public static final String DISPLAY_NAME = "displayName";
    public static final String ID = "id";
    public static final String IMPORTANCE = "importance";
    public static final String IS_REMINDER_ON = "isReminderOn";
    public static final String TASK_ID = "taskId";
    public static final String TASK_LIST_ID = "taskListId";
    public static final String TITLE = "title";
    public static final String VALUE = "value";

    public static final ModifiableObjectProperty OUTPUT_TASK_PROPERTY = object()
        .properties(
            string("@odata.etag"),
            string(IMPORTANCE)
                .description("Importance of the task."),
            bool(IS_REMINDER_ON)
                .description("Indicates whether an alert is set to reminder the user of the task."),
            string("status")
                .description("State or progress of the task."),
            string(TITLE)
                .description("Title of the task."),
            string("categories")
                .description("The categories associated with the task."),
            string(ID)
                .description("ID of the task."),
            object("body")
                .description("Body of the task containing information about the task.")
                .properties(
                    string("content")
                        .description("Content of the task."),
                    string("contentType")
                        .description("The type of the content.")),
            object("linkedResources")
                .properties(
                    string(ID),
                    string("webUrl"),
                    string("applicationName"),
                    string(DISPLAY_NAME)));

    private MicrosoftToDoConstants() {
    }
}
