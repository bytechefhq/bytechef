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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ID;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.ID;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.MIME_TYPE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.Collections;

/**
 * @author Mayank Madan
 */
public class GoogleDriveCopyFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("copyFile")
        .title("Copy File")
        .description("Copy a selected file to a different location within Google Drive.")
        .properties(
            string(FILE_ID)
                .label("File")
                .description("The id of the file to be copied.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER, false))
                .required(true),
            string(FILE_NAME)
                .label("New File Name")
                .description("The name of the new file created as a result of the copy operation.")
                .required(true),
            string(PARENT_FOLDER)
                .label("Destination Folder")
                .required(true)
                .description("The ID of the folder where the copied file will be stored.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER, true)))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID),
                        string("kind"),
                        string(MIME_TYPE),
                        string(NAME))))
        .perform(GoogleDriveCopyFileAction::perform);

    private GoogleDriveCopyFileAction() {
    }

    public static File perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);
        String fileId = inputParameters.getRequiredString(FILE_ID);

        File originalFile = drive.files()
            .get(fileId)
            .execute();

        File newFile = new File()
            .setName(inputParameters.getRequiredString(FILE_NAME))
            .setParents(Collections.singletonList(inputParameters.getRequiredString(PARENT_FOLDER)))
            .setMimeType(originalFile.getMimeType());

        return drive
            .files()
            .copy(fileId, newFile)
            .execute();
    }
}
