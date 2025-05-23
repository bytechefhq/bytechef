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

package com.bytechef.component.dropbox.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxCreateNewFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createFolder")
        .title("Create New Folder")
        .description("Create a folder at a given path.")
        .properties(
            string(PATH)
                .label("Folder Path/Name")
                .description("The path of the new folder. Root is /.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("metadata")
                            .description("Metadata containing details about the newly created folder.")
                            .properties(
                                string("name")
                                    .description(
                                        "The name of the newly created folder. This is the last component of the path."),
                                string("path_lower")
                                    .description(
                                        "The full path to the newly created folder in lowercase, as stored in the " +
                                            "user's Dropbox."),
                                string("path_display")
                                    .description(
                                        "The display-friendly version of the path to the newly created folder, " +
                                            "preserving original casing."),
                                string("id")
                                    .description("ID for the newly created folder within Dropbox.")))))
        .perform(DropboxCreateNewFolderAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_CREATE_FOLDER_CONTEXT_FUNCTION =
        http -> http.post("https://api.dropboxapi.com/2/files/create_folder_v2");

    private DropboxCreateNewFolderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(POST_CREATE_FOLDER_CONTEXT_FUNCTION)
            .body(Http.Body.of(PATH, inputParameters.getRequired(PATH)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
