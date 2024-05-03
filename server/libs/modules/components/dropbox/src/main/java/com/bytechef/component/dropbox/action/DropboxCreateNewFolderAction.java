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
import static com.bytechef.component.dropbox.constant.DropboxConstants.CREATE_FOLDER;
import static com.bytechef.component.dropbox.constant.DropboxConstants.DESTINATION;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.CreateFolderResult;
import com.dropbox.core.v2.files.DbxUserFilesRequests;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxCreateNewFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_FOLDER)
        .title("Create new folder")
        .description("Create a folder at a given path.")
        .properties(string(DESTINATION)
            .label("Folder path/name")
            .description("The path of the new folder. Root is /.")
            .required(true))
        .outputSchema(
            object()
                .properties(
                    object("folderMetadata")
                        .properties(
                            string("id")
                                .required(true),
                            string("sharedFolderId")
                                .required(true),
                            object("sharingInfo")
                                .properties(
                                    string("parentSharedFolderId")
                                        .required(true),
                                    string("sharedFolderId")
                                        .required(true),
                                    bool("traverseOnly"),
                                    bool("noAccess")
                                        .required(true),
                                    array("propertyGroups")
                                        .items(
                                            object()
                                                .properties(
                                                    string("templateId")
                                                        .required(true),
                                                    array("fields")
                                                        .items(
                                                            object()
                                                                .properties(
                                                                    string("name")
                                                                        .required(true),
                                                                    string("value")
                                                                        .required(true))))
                                                .required(true)))))
                .label("Create folder result"))
        .perform(DropboxCreateNewFolderAction::perform);

    private DropboxCreateNewFolderAction() {
    }

    public static CreateFolderResult perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws DbxException {

        DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
            connectionParameters.getRequiredString(ACCESS_TOKEN));

        return dbxUserFilesRequests.createFolderV2(
            inputParameters.getRequiredString(DESTINATION));
    }
}
