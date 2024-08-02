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
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.LIST_FOLDER;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxListFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(LIST_FOLDER)
        .title("List folder")
        .description("List the contents of a folder.")
        .properties(
            string(PATH)
                .label("Path")
                .description("Path of the filename. Inputting nothing searches root.")
                .required(false))
        .outputSchema(
            object()
                .properties(
                    array("entries")
                        .items(
                            object()
                                .properties(
                                    object("f")
                                        .properties(
                                            string(".tag"),
                                            string("name"),
                                            string("path_lower"),
                                            string("path_Display"),
                                            string("id"))))))
        .perform(DropboxListFolderAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_LIST_FOLDER_CONTEXT_FUNCTION =
        http -> http.post("https://api.dropboxapi.com/2/files/list_folder");

    private DropboxListFolderAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(POST_LIST_FOLDER_CONTEXT_FUNCTION)
            .body(
                Http.Body.of(
                    Map.of(
                        PATH, inputParameters.getString(PATH, ""))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
