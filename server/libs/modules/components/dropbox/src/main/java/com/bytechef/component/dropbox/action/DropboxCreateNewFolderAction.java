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

import static com.bytechef.component.dropbox.constant.DropboxConstants.CREATENEWFOLDER;
import static com.bytechef.component.dropbox.constant.DropboxConstants.DESTINATION_FILENAME;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.Parameters;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.CreateFolderResult;
import com.dropbox.core.v2.files.DbxUserFilesRequests;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxCreateNewFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATENEWFOLDER)
        .title("Create new folder")
        .description("Create a folder at a given path.")
        .properties(string(DESTINATION_FILENAME)
            .label("Destination path")
            .description("Path to create a folder on.")
            .required(true))
        .outputSchema(
            object().properties(
                object("folderMetadata")
                    .properties(
                        string("id")
                            .label("ID")
                            .required(true),
                        string("sharedFolderId")
                            .label("Shared folder ID")
                            .required(true),
                        object("sharingInfo")
                            .properties(
                                string("parentSharedFolderId")
                                    .label("Parent shared folder ID")
                                    .required(true),
                                string("sharedFolderId")
                                    .label("Shared folder ID")
                                    .required(true),
                                bool("traverseOnly")
                                    .label("Traverse only"),
                                bool("noAccess")
                                    .label("No access"))
                            .label("Sharing info")
                            .required(true),
                        array("propertyGroups")
                            .items(
                                object()
                                    .properties(
                                        string("templateId")
                                            .label("Template ID")
                                            .required(true),
                                        array("fields")
                                            .items(
                                                object()
                                                    .properties(
                                                        string("name")
                                                            .label("Name")
                                                            .required(true),
                                                        string("value")
                                                            .label("Value")
                                                            .required(true))
                                                    .label("Fields")))
                                    .label("Property groups")
                                    .required(true)))
                    .label("Metadata"))
                .label("Create folder result"))
        .perform(DropboxCreateNewFolderAction::perform);

    private DropboxCreateNewFolderAction() {
    }

    public static CreateFolderResult perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws DbxException {

        DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
            connectionParameters.getRequiredString(ACCESS_TOKEN));

        return dbxUserFilesRequests.createFolderV2(inputParameters.getRequiredString(DESTINATION_FILENAME));
    }
}
