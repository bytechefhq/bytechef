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
import static com.bytechef.component.dropbox.constant.DropboxConstants.LISTAFOLDER;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SOURCE_FILENAME;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.ListFolderResult;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxListFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(LISTAFOLDER)
        .title("List folder")
        .description("Lists content of a folder.")
        .properties(
            string(SOURCE_FILENAME)
                .label("Path")
                .description("A unique identifier for the file. Must match pattern " +
                    "\" (/(.|[\\\\r\\\\n])*)?|id:.*|(ns:[0-9]+(/.*)?)\" and not be null.")
                .required(true))
        .outputSchema(
            object("result")
                .properties(
                    array("entries").items(
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
                                    .label("Entries"))),
                    string("cursor")
                        .label("Cursor")
                        .required(true),
                    bool("hasMore")
                        .label("Has more")
                        .required(true)))
        .perform(DropboxListFolderAction::perform);

    private DropboxListFolderAction() {
    }

    public static Map<String, ListFolderResult> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws DbxException {

        DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
            connectionParameters.getRequiredString(ACCESS_TOKEN));

        return Map.of("result", dbxUserFilesRequests.listFolder(inputParameters.getRequiredString(SOURCE_FILENAME)));
    }
}
