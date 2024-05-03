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
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.DESTINATION;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILE_ENTRY;
import static com.bytechef.component.dropbox.constant.DropboxConstants.UPLOAD_FILE;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadBuilder;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxUploadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(UPLOAD_FILE)
        .title("Upload file")
        .description("Create a new file up to a size of 150MB with the contents provided in the request.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description("The object property which contains a reference to the file to be written.")
                .required(true),
            string(DESTINATION)
                .label("Destination path")
                .description("The path to which the file should be written.")
                .placeholder("/directory/")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("Name of the file. Needs to have the appropriate extension.")
                .placeholder("your_file.pdf")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string("id")
                        .required(true),
                    date("clientModified")
                        .required(true),
                    date("serverModified")
                        .required(true),
                    string("rev")
                        .required(true),
                    integer("size")
                        .required(true),
                    object("symlinkInfo")
                        .properties(
                            string("target")
                                .required(true))
                        .label("Sym link info"),
                    object("sharingInfo")
                        .properties(
                            string("parentSharedFolderId")
                                .required(true),
                            string("modifiedBy")
                                .required(true))
                        .label("Sharing info"),
                    bool("isDownloadable")
                        .required(true),
                    object("exportInfo")
                        .properties(
                            string("exportAs")
                                .required(true),
                            array("exportOptions")
                                .items(string()))
                        .label("Export info"),
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
                                                        .required(true))
                                                .label("Fields")))
                                .required(true)),
                    bool("hasExplicitSharedMembers")
                        .required(true),
                    string("contentHash")
                        .required(true),
                    object("fileLockInfo")
                        .properties(
                            bool("isLockholder")
                                .required(true),
                            string("lockholderName")
                                .required(true),
                            string("lockholderAccountId")
                                .required(true),
                            date("created")
                                .required(true))))
        .perform(DropboxUploadFileAction::perform);

    private DropboxUploadFileAction() {
    }

    public static FileMetadata perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws DbxException, IOException {

        String destination = inputParameters.getRequiredString(DESTINATION);
        String filePath = (destination.endsWith("/") ? destination : destination+"/") + inputParameters.getRequiredString(FILENAME);

        try (InputStream inputStream = actionContext.file(
            file -> file.getStream(inputParameters.getRequiredFileEntry(FILE_ENTRY)))) {

            DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
                connectionParameters.getRequiredString(ACCESS_TOKEN));

            UploadBuilder uploadBuilder = dbxUserFilesRequests.uploadBuilder(filePath);

            return uploadBuilder.uploadAndFinish(inputStream);
        }
    }
}
