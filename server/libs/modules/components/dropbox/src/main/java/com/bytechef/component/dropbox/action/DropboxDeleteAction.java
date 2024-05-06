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

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.DELETE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SOURCE;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.DeleteResult;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxDeleteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DELETE)
        .title("Delete")
        .description(
            "Delete the file or folder at a given path. If the path is a folder, all its contents will be deleted too.")
        .properties(
            string(SOURCE)
                .label("Path")
                .description("Path of the file or folder. Root is /.")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("Name of the file. Leave empty if you want to delete a folder.")
                .required(false))
        .outputSchema(
            object()
                .properties(
                    object("metadata")
                        .properties(
                            string("name")
                                .required(true),
                            string("pathLower")
                                .required(true),
                            string("pathDisplay")
                                .required(true),
                            string("parentSharedFolderId")
                                .required(true),
                            string("previewUrl")
                                .required(true))
                        .label("Metadata")))
        .perform(DropboxDeleteAction::perform);

    private DropboxDeleteAction() {
    }

    public static DeleteResult perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws DbxException {

        DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
            connectionParameters.getRequiredString(ACCESS_TOKEN));

        String source = inputParameters.getRequiredString(SOURCE);

        return dbxUserFilesRequests.deleteV2(
            (source.endsWith("/") ? source : source + "/") + inputParameters.getRequiredString(FILENAME));
    }
}
