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

package com.bytechef.component.clickup.action;

import static com.bytechef.component.clickup.constant.ClickupConstants.LIST_ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.TASK_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import java.util.Map;

import com.bytechef.component.OpenApiComponentHandler.PropertyType;
import com.bytechef.component.clickup.util.ClickupUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.OptionsDataSource;

public class ClickupCreateTaskCommentAction {
    public static final String ACTION_NAME = "createTaskComment";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ACTION_NAME)
            .title("Create Task Comment")
            .description("Create a new comment for specified class.")
            .metadata(Map.of("method", "POST", "path", "/task/{taskId}/comment", "bodyContentType",
                    BodyContentType.JSON, "mimeType", "application/json"))
            .properties(
                    string(LIST_ID).label("List ID")
                            .description("ID of the list containing the task.")
                            .required(true)
                            .options(
                                    (OptionsDataSource.ActionOptionsFunction<String>) ClickupUtils::getListIdOptions)

                            .metadata(Map.of("type", PropertyType.BODY)),
                    string(TASK_ID).label("Task ID").description("ID of the task to which the comment will be added.")
                            .required(true)
                            .options((OptionsDataSource.ActionOptionsFunction<String>) ClickupUtils::getTaskIdOptions)
                            .metadata(Map.of("type", PropertyType.PATH)),
                    string("comment_text").label("Comment Text")
                            .description("Text of the comment to be added to the task.")
                            .required(true).metadata(Map.of("type", PropertyType.BODY)),
                    bool("notify_all").label("Notify All").description(
                            "Flag indicating whether notifications should be sent to all participants, including the creator of the comment.")
                            .required(true).metadata(Map.of("type", PropertyType.BODY)))

            .output(outputSchema(
                    object().properties(string("id").description("The ID of newly created comment.").required(true),
                            string("hist_id").description("The hist ID of newly created comment.").required(true),
                            integer("date").description("The date of the newly created comment.").required(true))
                            .metadata(Map.of("responseType", ResponseType.JSON))));

    private ClickupCreateTaskCommentAction() {
    }
}
