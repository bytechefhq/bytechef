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

import static com.bytechef.component.dropbox.constant.DropboxConstants.DESTINATION_FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILE_ENTRY;
import static com.bytechef.component.dropbox.constant.DropboxConstants.UPLOADFILE;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
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

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(UPLOADFILE)
        .title("Upload file")
        .description("Create a new file up to a size of 150MB with the contents provided in the request.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description("The object property which contains a reference to the file to be written.")
                .required(true),
            string(DESTINATION_FILENAME)
                .label("Filename")
                .description("The path to which the file should be written.")
                .placeholder("/your_file.pdf")
                .required(true))
        .outputSchema(object().properties(integer("bytes")))
        .perform(DropboxUploadFileAction::perform);

    private DropboxUploadFileAction() {
    }

    public static FileMetadata perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext actionContext)
        throws ComponentExecutionException {

        String fileName = inputParameters.getRequiredString(DESTINATION_FILENAME);

        try (InputStream inputStream = actionContext.file(
            file -> file.getStream(inputParameters.getRequiredFileEntry(FILE_ENTRY)))) {

            DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
                connectionParameters.getRequiredString(ACCESS_TOKEN));

            UploadBuilder uploadBuilder = dbxUserFilesRequests.uploadBuilder(fileName);

            return uploadBuilder.uploadAndFinish(inputStream);
        } catch (IOException | DbxException exception) {
            throw new ComponentExecutionException("Unable to upload file " + inputParameters, exception);
        }
    }
}
