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

package com.bytechef.component.wrike.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.wrike.constant.WrikeConstants.PARENT;
import static com.bytechef.component.wrike.constant.WrikeConstants.PARENT_ID;
import static com.bytechef.component.wrike.constant.WrikeConstants.PLAIN_TEXT;
import static com.bytechef.component.wrike.constant.WrikeConstants.TEXT;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.wrike.util.WrikeUtils;

/**
 * @author Nikolina Spehar
 */
public class WrikeCreateCommentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createComment")
        .title("Create Comment")
        .description("Create a comment in a folder or in a task.")
        .properties(
            string(PARENT)
                .label("Parent")
                .description("Choose whether the comment will be added to a task or to a folder.")
                .options(
                    option("Folder", "folders"),
                    option("Task", "tasks"))
                .required(true),
            string(PARENT_ID)
                .label("Parent ID")
                .description("ID of the parent folder or the parent task.")
                .optionsLookupDependsOn(PARENT)
                .options((OptionsFunction<String>) WrikeUtils::getParentIdOptions)
                .required(true),
            string(TEXT)
                .label("Text")
                .description("Comment text.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            bool(PLAIN_TEXT)
                .label("Plain Text")
                .description("Whether the comment will be treated as plain text or as HTML.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("kind")
                            .description("Kind of the object that was created."),
                        array("data")
                            .description("Data of the object that was created.")
                            .items(
                                object()
                                    .properties(
                                        string("id")
                                            .description("ID of the object that was created."),
                                        string("authorId")
                                            .description("ID of the author of the object."),
                                        string("text")
                                            .description("Text of the object that was created."),
                                        string("updatedDate")
                                            .description("Date when the object was updated."),
                                        string("createdDate")
                                            .description("Date when the object was created."),
                                        string("parentId")
                                            .description("ID of the parent folder or the parent task."),
                                        array("attachmentIds")
                                            .placeholder("ID of the attachments of the object."))))))
        .perform(WrikeCreateCommentAction::perform);

    private WrikeCreateCommentAction() {
    }

    protected static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(
            http -> http.post("/%s/%s/comments".formatted(
                inputParameters.getRequiredString(PARENT),
                inputParameters.getRequiredString(PARENT_ID))))
            .queryParameters(
                TEXT, inputParameters.getRequiredString(TEXT),
                PLAIN_TEXT, inputParameters.getBoolean(PLAIN_TEXT))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
