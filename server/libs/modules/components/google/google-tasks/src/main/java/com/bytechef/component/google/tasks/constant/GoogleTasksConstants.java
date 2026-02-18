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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
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
                string("kind")
                    .description("Type of the resource."),
                string("id")
                    .description("Task identifier."),
                string("etag")
                    .description("ETag of the resource."),
                string(TITLE)
                    .description("Title of the task. Maximum length allowed: 1024 characters."),
                string("updated")
                    .description("Last modification time of the task (as a RFC 3339 timestamp)."),
                string("selfLink")
                    .description("URL pointing to this task. Used to retrieve, update, or delete this task."),
                string("parent")
                    .description("Parent task identifier."),
                string("position")
                    .description(
                        "String indicating the position of the task among its sibling tasks under the same parent " +
                            "task or at the top level."),
                string(NOTES)
                    .description("Notes describing the task."),
                string(STATUS)
                    .description("Status of the task."),
                string("due")
                    .description("Scheduled date for the task (as an RFC 3339 timestamp)."),
                string("completed")
                    .description("Completion date of the task (as a RFC 3339 timestamp)."),
                bool("deleted")
                    .description("Flag indicating whether the task has been deleted."),
                bool("hidden")
                    .description("Flag indicating whether the task is hidden."),
                array("links")
                    .description("Collection of links.")
                    .items(
                        object()
                            .properties(
                                string("type")
                                    .description(
                                        "Type of the link, e.g. \"email\", \"generic\", \"chat_message\", " +
                                            "\"keep_note\"."),
                                string("description")
                                    .description("The description (might be empty)."),
                                string("link")
                                    .description("The URL."))),
                string("webViewLink")
                    .description("An absolute link to the task in the Google Tasks Web UI."),
                object("assignmentInfo")
                    .description("Context information for assigned tasks.")
                    .properties(
                        string("linkToTask")
                            .description(
                                "An absolute link to the original task in the surface of assignment (Docs, Chat " +
                                    "spaces, etc.)."),
                        string("surfaceType")
                            .description("The type of surface this assigned task originates from."),
                        object("DriveResourceInfo")
                            .description("Information about the Drive file where this task originates from."),
                        object("spaceInfo")
                            .description("Information about the Chat Space where this task originates from.")));

    private GoogleTasksConstants() {
    }
}
