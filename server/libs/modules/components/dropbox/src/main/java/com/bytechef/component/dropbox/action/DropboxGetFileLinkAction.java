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
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.GET_FILE_LINK;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SOURCE;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.GetTemporaryLinkResult;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxGetFileLinkAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_FILE_LINK)
        .title("Get file link")
        .description(
            "Get a temporary link to stream content of a file. This link will expire in four hours and afterwards " +
                "you will get 410 Gone. This URL should not be used to display content directly in the browser. " +
                "The Content-Type of the link is determined automatically by the file's mime type.")
        .properties(
            string(SOURCE)
                .label("Path to the file")
                .description(
                    "The path to the file you want a temporary link to.  Root is /.")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("Name of the file with the extension.")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("metadata")
                        .properties(
                            string("id")
                                .label("ID")
                                .required(true),
                            date("clientModified")
                                .label("Client modified")
                                .required(true),
                            date("serverModified")
                                .label("Server modified")
                                .required(true),
                            string("rev").label("Rev")
                                .required(true),
                            integer("size").label("Size")
                                .required(true),
                            object("symlinkInfo")
                                .properties(
                                    string("target")
                                        .label("Target")
                                        .required(true))
                                .label("Sym link info"),
                            object("sharingInfo")
                                .properties(
                                    string("parentSharedFolderId")
                                        .label("Parent shared folder ID")
                                        .required(true),
                                    string("modifiedBy")
                                        .label("Modified by")
                                        .required(true))
                                .label("Sharing info"),
                            bool("isDownloadable")
                                .label("Is downloadable")
                                .required(true),
                            object("exportInfo")
                                .properties(
                                    string("exportAs")
                                        .label("Export as")
                                        .required(true),
                                    array("exportOptions")
                                        .items(string())
                                        .label("Export options"))
                                .label("Export info"),
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
                                        .required(true))
                                .label("Property groups"),
                            bool("hasExplicitSharedMembers")
                                .label("Has explicit shared members")
                                .required(true),
                            string("contentHash")
                                .label("Content hash")
                                .required(true),
                            object("fileLockInfo")
                                .properties(
                                    bool("isLockholder")
                                        .label("Is lockholder")
                                        .required(true),
                                    string("lockholderName")
                                        .label("Lockholder name")
                                        .required(true),
                                    string("lockholderAccountId")
                                        .label("Lockholder account ID")
                                        .required(true),
                                    date("created")
                                        .label("Created")
                                        .required(true)
                                        .required(true))
                                .label("File lock info"))
                        .label("Metadata"),
                    string("link")
                        .label("Link")
                        .required(true)))
        .perform(DropboxGetFileLinkAction::perform);

    private DropboxGetFileLinkAction() {
    }

    public static GetTemporaryLinkResult perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws DbxException {

        DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
            connectionParameters.getRequiredString(ACCESS_TOKEN));

        String source = inputParameters.getRequiredString(SOURCE);

        return dbxUserFilesRequests.getTemporaryLink(
            (source.endsWith("/") ? source : source + "/") + inputParameters.getRequiredString(FILENAME));
    }
}
