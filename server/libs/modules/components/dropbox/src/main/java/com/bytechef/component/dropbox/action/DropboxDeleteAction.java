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
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;
import static com.bytechef.component.dropbox.util.DropboxUtils.getFullPath;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxDeleteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("delete")
        .title("Delete")
        .description(
            "Delete the file or folder at a given path. If the path is a folder, all its contents will be deleted too.")
        .properties(
            string(PATH)
                .label("Path")
                .description("Path of the file or folder. Root is /.")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("Name of the file. Leave empty if you want to delete a folder.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("metadata")
                            .description("Metadata containing details about the deleted file or folder.")
                            .properties(
                                string("name")
                                    .description(
                                        "The name of the deleted file or folder, including its extension. This is " +
                                            "the last component of the path."),
                                string("path_lower")
                                    .description(
                                        "The full path to the deleted file or folder in lowercase, as stored in the " +
                                            "user's Dropbox."),
                                string("path_display")
                                    .description(
                                        "The display-friendly version of the path to the deleted file or folder, " +
                                            "preserving original casing."),
                                string("id")
                                    .description("ID of the deleted file or folder.")))))
        .perform(DropboxDeleteAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_DELETE_CONTEXT_FUNCTION =
        http -> http.post("https://api.dropboxapi.com/2/files/delete_v2");

    private DropboxDeleteAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(POST_DELETE_CONTEXT_FUNCTION)
            .body(
                Http.Body.of(
                    Map.of(
                        PATH,
                        getFullPath(
                            inputParameters.getRequiredString(PATH), inputParameters.getRequiredString(FILENAME)))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
