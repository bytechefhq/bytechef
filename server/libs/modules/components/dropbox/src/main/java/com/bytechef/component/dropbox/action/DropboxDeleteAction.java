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
import static com.bytechef.component.dropbox.constant.DropboxConstants.SOURCE_FILENAME;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.DeleteResult;
import java.util.Map;

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
            object("result")
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

    public static Map<String, DeleteResult> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws DbxException {

        DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
            connectionParameters.getRequiredString(ACCESS_TOKEN));

        DeleteResult deleteResult = dbxUserFilesRequests.deleteV2(inputParameters.getRequiredString(SOURCE_FILENAME));

        return Map.of("result", deleteResult);
    }
}
