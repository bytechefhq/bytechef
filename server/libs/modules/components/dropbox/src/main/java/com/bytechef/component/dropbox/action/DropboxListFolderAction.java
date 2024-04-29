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
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.LIST_FOLDER;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SOURCE_FILENAME;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.ListFolderResult;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxListFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(LIST_FOLDER)
        .title("List folder")
        .description("Lists content of a folder.")
        .properties(
            string(SOURCE_FILENAME)
                .label("Path")
                .description("A unique identifier for the file. Must match pattern " +
                    "\" (/(.|[\\\\r\\\\n])*)?|id:.*|(ns:[0-9]+(/.*)?)\" and not be null.")
                .required(false))
        .outputSchema(
            object()
                .properties(
                    array("entries")
                        .items(
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
                                                .required(true)))),
                    string("cursor")
                        .required(true),
                    bool("hasMore")
                        .required(true)))
        .perform(DropboxListFolderAction::perform);

    private DropboxListFolderAction() {
    }

    public static ListFolderResult perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws DbxException {

        DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
            connectionParameters.getRequiredString(ACCESS_TOKEN));

        String filePath = inputParameters.getString(SOURCE_FILENAME);

        return dbxUserFilesRequests.listFolder(filePath!=null ? filePath : "");
    }
}
