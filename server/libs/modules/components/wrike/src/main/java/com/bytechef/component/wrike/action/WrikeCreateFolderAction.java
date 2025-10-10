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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.wrike.constant.WrikeConstants.PARENT_ID;
import static com.bytechef.component.wrike.constant.WrikeConstants.TITLE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.wrike.util.WrikeUtils;

/**
 * @author Nikolina Spehar
 */
public class WrikeCreateFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createFolder")
        .title("Create Folder")
        .description("Create a folder.")
        .properties(
            string(PARENT_ID)
                .label("Parent ID")
                .description("ID of the parent folder.")
                .options((OptionsFunction<String>) WrikeUtils::getParentIdOptions)
                .required(true),
            string(TITLE)
                .label("Title")
                .description("The title of the folder.")
                .required(true))
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
                                        string("accountId")
                                            .description("ID of the account that created the object."),
                                        string("title")
                                            .description("Title of the object that was created."),
                                        string("createdDate")
                                            .description("Date when the object was created."),
                                        string("updatedDate")
                                            .description("Date when the object was updated."),
                                        string("description")
                                            .description("Description of the object."),
                                        array("sharedIds")
                                            .description("Shared IDs of the object.")
                                            .items(
                                                string("sharedId")
                                                    .description("Shared ID.")),
                                        array("parentIds")
                                            .description("Parent IDs of the object.")
                                            .items(
                                                string("parentId")
                                                    .description("Parent ID.")),
                                        array("childIds")
                                            .description("Child IDs of the object.")
                                            .items(
                                                string("childId")
                                                    .description("Child ID.")),
                                        array("superParentIds")
                                            .description("Super Parent IDs of the object.")
                                            .items(
                                                string("superParentId")
                                                    .description("Super parent ID.")),
                                        string("scope")
                                            .description("Scope of the object."),
                                        bool("hasAttachments")
                                            .description("Whether the object has attachments"),
                                        string("permalink")
                                            .description("Permalink of the object."),
                                        string("workflowId")
                                            .description("Workflow ID."),
                                        array("metadata")
                                            .description("Metadata of the object."),
                                        array("customFields")
                                            .description("Custom fields of the object."))))))
        .perform(WrikeCreateFolderAction::perform);

    private WrikeCreateFolderAction() {
    }

    protected static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(
            http -> http.post("/folders/%s/folders".formatted(inputParameters.getRequiredString(PARENT_ID))))
            .queryParameter(TITLE, inputParameters.getRequiredString(TITLE))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
