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

package com.bytechef.component.dropbox.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.CREATE_FOLDER;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxCreateNewFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_FOLDER)
        .title("Create new folder")
        .description("Create a folder at a given path.")
        .properties(
            string(PATH)
                .label("Folder path/name")
                .description("The path of the new folder. Root is /.")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("metadata")
                        .properties(
                            string("name"),
                            string("path_lower"),
                            string("path_display"),
                            string("id"))))
        .perform(DropboxCreateNewFolderAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_CREATE_FOLDER_CONTEXT_FUNCTION =
        http -> http.post("https://api.dropboxapi.com/2/files/create_folder_v2");

    private DropboxCreateNewFolderAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(POST_CREATE_FOLDER_CONTEXT_FUNCTION)
            .body(Http.Body.of(PATH, inputParameters.getRequired(PATH)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
