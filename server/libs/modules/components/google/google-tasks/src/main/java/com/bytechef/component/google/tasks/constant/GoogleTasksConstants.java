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

    public static final String LIST_ID = "listId";
    public static final String NOTES = "notes";
    public static final String SHOW_COMPLETED = "showCompleted";
    public static final String STATUS = "status";
    public static final String TASK_ID = "taskId";
    public static final String TITLE = "title";

    public static final ModifiableObjectProperty OUTPUT_PROPERTY =
        object()
            .properties(
                string(TITLE),
                string(NOTES),
                string(STATUS));

    private GoogleTasksConstants() {
    }
}
