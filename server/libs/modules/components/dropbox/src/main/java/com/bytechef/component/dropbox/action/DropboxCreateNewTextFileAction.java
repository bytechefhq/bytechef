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
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.CREATE_TEXT_FILE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.TO_PATH;

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
public class DropboxCreateNewTextFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TEXT_FILE)
        .title("Create a new paper file")
        .description("Create a new .paper file on which you can write at a given path")
        .properties(
            string(TO_PATH)
                .label("Paper path/name")
                .description("The path of the new paper file. Starts with / as root.")
                .placeholder("/directory/")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("Name of the paper file")
                .placeholder("New paper file")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string("url"),
                    string("resultPath"),
                    string("fileId"),
                    integer("paperRevision")))
        .perform(DropboxCreateNewTextFileAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_FILES_UPLOAD_CONTEXT_FUNCTION =
        http -> http.post("https://api.dropboxapi.com/2/files/upload");

    private DropboxCreateNewTextFileAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(POST_FILES_UPLOAD_CONTEXT_FUNCTION)
            .body(
                Http.Body.of(
                // TODO
                ))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
