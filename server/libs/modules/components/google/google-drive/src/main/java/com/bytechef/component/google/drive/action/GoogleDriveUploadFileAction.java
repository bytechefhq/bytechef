/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.google.drive.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ENTRY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_SAMPLE_OUTPUT;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Mario Cvjetojevic
 * @author Ivica Cardic
 * @author Monika Kušter
 */
public class GoogleDriveUploadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadFile")
        .title("Upload File")
        .description("Uploads a file in your Google Drive.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File Entry")
                .description("The object property which contains a reference to the file to upload.")
                .required(true),
            string(FOLDER_ID)
                .label("Parent Folder ID")
                .description(
                    "ID of the folder where the file will be uploaded; if no folder is selected, the file will be " +
                        "uploaded to the root folder.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER, true))
                .required(false))
        .output(outputSchema(GOOGLE_FILE_OUTPUT_PROPERTY), sampleOutput(GOOGLE_FILE_SAMPLE_OUTPUT))
        .perform(GoogleDriveUploadFileAction::perform);

    private GoogleDriveUploadFileAction() {
    }

    public static File perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Drive drive = GoogleServices.getDrive(connectionParameters);
        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE_ENTRY);
        String parentFolder = inputParameters.getString(FOLDER_ID);

        try {
            return drive
                .files()
                .create(
                    new File()
                        .setName(fileEntry.getName())
                        .setParents(parentFolder == null ? null : List.of(parentFolder)),
                    new FileContent(
                        fileEntry.getMimeType(),
                        context.file(file -> file.toTempFile(fileEntry))))
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }
}
