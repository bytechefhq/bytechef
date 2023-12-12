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

import static com.bytechef.component.dropbox.constant.DropboxConstants.DELETE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SOURCE_FILENAME;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
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
            string(SOURCE_FILENAME)
                .label("Path")
                .description("Path in the user's Dropbox to delete. Must match pattern " +
                    "\"(/(.|[\\\\r\\\\n])*)|(ns:[0-9]+(/.*)?)|(id:.*)\" and not be null.")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("metadata")
                        .properties(
                            string("name")
                                .label("Name")
                                .required(true),
                            string("pathLower")
                                .label("Path lowercase")
                                .required(true),
                            string("pathDisplay")
                                .label("Path display")
                                .required(true),
                            string("parentSharedFolderId")
                                .label("Parent shared folder")
                                .required(true),
                            string("previewUrl")
                                .label("Preview URL")
                                .required(true))
                        .label("Metadata")))
        .perform(DropboxDeleteAction::perform);

    private DropboxDeleteAction() {
    }

    public static DeleteResult perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext actionContext)
        throws ComponentExecutionException {

        try {
            DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
                connectionParameters.getRequiredString(ACCESS_TOKEN));

            return dbxUserFilesRequests.deleteV2(inputParameters.getRequiredString(SOURCE_FILENAME));
        } catch (DbxException dbxException) {
            throw new ComponentExecutionException("Unable to delete " + inputParameters, dbxException);
        }
    }
}
