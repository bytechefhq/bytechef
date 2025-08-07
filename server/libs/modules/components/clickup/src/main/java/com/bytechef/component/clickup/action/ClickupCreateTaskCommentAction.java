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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import java.util.Map;

import com.bytechef.component.OpenApiComponentHandler.PropertyType;
import com.bytechef.component.clickup.util.ClickupUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;

public class ClickupCreateTaskCommentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTaskComment")
            .title("Create Task Comment")
            .description("Create a new comment for specified class")
            .properties(
                    string("listId").label("List ID")
                            .description("ID of the list containing the task")
                            .required(true)
                            .options(
                                    (OptionsDataSource.ActionOptionsFunction<String>) ClickupUtils::getListIdOptions)

                            .metadata(Map.of("type", PropertyType.BODY)),
                    string("taskId").label("Task ID").description("ID of the task to which the comment will be added")
                            .required(true)
                            .options((OptionsDataSource.ActionOptionsFunction<String>) ClickupUtils::getListIdOptions)
                            .metadata(Map.of("type", PropertyType.PATH)),
                    string("commentText").label("Comment Text")
                            .description("Text of the comment to be added to the task")
                            .required(true).metadata(Map.of("type", PropertyType.BODY)),
                    bool("notifyAll").label("Notify All").description(
                            "Flag indicating whether notifications should be sent to all participants, including the creator of the comment ")
                            .required(true).metadata(Map.of("type", PropertyType.BODY)))

            .output(outputSchema(string()))
            .perform(ClickupCreateTaskCommentAction::perform);

    private ClickupCreateTaskCommentAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        // TODO

        return null;
    }
}
