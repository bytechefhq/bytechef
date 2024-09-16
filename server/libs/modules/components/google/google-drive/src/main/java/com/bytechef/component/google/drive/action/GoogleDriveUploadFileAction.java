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

package com.bytechef.component.google.drive.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ENTRY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_SAMPLE_OUTPUT;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.bytechef.google.commons.GoogleServices;
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
        .title("Upload file")
        .description("Uploads a file in your Google Drive")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description("The object property which contains a reference to the file to upload.")
                .required(true),
            string(PARENT_FOLDER)
                .label("Parent folder")
                .description(
                    "Folder where the file will be uploaded; if no folder is selected, the file will be uploaded to " +
                        "the root folder.")
                .options((ActionOptionsFunction<String>) GoogleDriveUtils::getFolderOptions)
                .required(false))
        .output(outputSchema(GOOGLE_FILE_OUTPUT_PROPERTY), sampleOutput(GOOGLE_FILE_SAMPLE_OUTPUT))
        .perform(GoogleDriveUploadFileAction::perform);

    private GoogleDriveUploadFileAction() {
    }

    public static File perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);
        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE_ENTRY);
        String parentFolder = inputParameters.getString(PARENT_FOLDER);

        return drive
            .files()
            .create(
                new File()
                    .setName(fileEntry.getName())
                    .setParents(parentFolder == null ? null : List.of(parentFolder)),
                new FileContent(
                    fileEntry.getMimeType(),
                    actionContext.file(actionContextFile -> actionContextFile.toTempFile(fileEntry))))
            .execute();
    }
}
