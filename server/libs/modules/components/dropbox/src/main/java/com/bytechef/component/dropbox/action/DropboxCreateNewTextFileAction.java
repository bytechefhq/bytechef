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
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.CREATE_TEXT_FILE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.DESTINATION;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.ImportFormat;
import com.dropbox.core.v2.files.PaperCreateResult;
import com.dropbox.core.v2.files.PaperCreateUploader;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxCreateNewTextFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TEXT_FILE)
        .title("Create a new paper file")
        .description("Create a new .paper file on which you can write at a given path")
        .properties(
            string(DESTINATION)
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
                    string("url")
                        .required(true),
                    string("resultPath")
                        .required(true),
                    string("fileId")
                        .required(true),
                    integer("paperRevision")
                        .required(true)))
        .perform(DropboxCreateNewTextFileAction::perform);

    private DropboxCreateNewTextFileAction() {
    }

    public static PaperCreateResult perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws DbxException, IOException {

        DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
            connectionParameters.getRequiredString(ACCESS_TOKEN));

        String fileName = inputParameters.getRequiredString(FILENAME);

        fileName = fileName.endsWith(".paper") ? fileName : fileName + ".paper";

        String destination = inputParameters.getRequiredString(DESTINATION);

        destination = destination.endsWith("/") ? destination : destination + "/";

        try (PaperCreateUploader paperCreateUploader = dbxUserFilesRequests.paperCreate(
            destination + fileName, ImportFormat.PLAIN_TEXT)) {

            return paperCreateUploader.uploadAndFinish(InputStream.nullInputStream());
        }
    }
}
