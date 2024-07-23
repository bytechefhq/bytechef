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
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FROM_PATH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.MOVE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.TO_PATH;
import static com.bytechef.component.dropbox.util.DropboxUtils.getFullPath;

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
public class DropboxMoveAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(MOVE)
        .title("Move")
        .description(
            "Move a file or folder to a different location in the user's Dropbox. If the source path is a folder all " +
                "its contents will be moved. Note that we do not currently support case-only renaming.")
        .properties(
            string(FILENAME)
                .label("Filename")
                .description("Name of the file with the extension. Don't fill in if you want a folder.")
                .required(false),
            string(FROM_PATH)
                .label("Source path")
                .description("Path in the user's Dropbox to be moved.  Root is /.")
                .required(true),
            string(TO_PATH)
                .label("Destination path")
                .description("Path in the user's Dropbox that is the destination. Root is /.")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("metadata")
                        .properties(
                            string(".tag"),
                            string("name"),
                            string("path_lower"),
                            string("path_display"),
                            string("id"))))
        .perform(DropboxMoveAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_MOVE_CONTEXT_FUNCTION =
        http -> http.post("https://api.dropboxapi.com/2/files/move_v2");

    private DropboxMoveAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        String filename = inputParameters.getRequiredString(FILENAME);

        return actionContext.http(POST_MOVE_CONTEXT_FUNCTION)
            .body(
                Http.Body.of(
                    FROM_PATH, getFullPath(inputParameters.getRequiredString(FROM_PATH), filename),
                    TO_PATH, getFullPath(inputParameters.getRequiredString(TO_PATH), filename)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
