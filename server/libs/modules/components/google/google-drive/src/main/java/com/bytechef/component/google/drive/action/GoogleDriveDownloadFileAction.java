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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class GoogleDriveDownloadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("downloadFile")
        .title("Download File")
        .description("Download selected file from Google Drive.")
        .properties(
            string(FILE_ID)
                .label("File")
                .description("File to download.")
                .options((ActionOptionsFunction<String>) GoogleDriveUtils::getFileOptions)
                .required(true))
        .output(outputSchema(fileEntry()))
        .perform(GoogleDriveDownloadFileAction::perform);

    private GoogleDriveDownloadFileAction() {
    }

    public static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        String fileId = inputParameters.getRequiredString(FILE_ID);

        Drive.Files.Get get = drive.files()
            .get(fileId);

        File googleFile = get.execute();

        String mimeType = googleFile.getMimeType();
        String fileName = googleFile.getName();
        String exportMimeType = getExportMimeType(mimeType);

        if (exportMimeType != null) {
            try (InputStream inputStream = drive.files()
                .export(fileId, exportMimeType)
                .executeMediaAsInputStream()) {

                String extension = actionContext.mimeType(mimeType1 -> mimeType1.lookupExt(exportMimeType));

                String finalFileName = fileName + "." + extension;

                return actionContext.file(file -> file.storeContent(finalFileName, inputStream));
            }
        } else {
            String fileExtension = googleFile.getFileExtension();

            if (fileExtension == null || fileExtension.isEmpty()) {
                fileExtension =
                    actionContext.mimeType(
                        mimeType1 -> mimeType1.lookupExt(mimeType.equals("plain/text") ? "text/plain" : mimeType));
                if (fileExtension != null && !fileName.contains(fileExtension)) {
                    fileName += "." + fileExtension;
                }
            }

            try (InputStream inputStream = get.executeMediaAsInputStream()) {
                String finalFilename = fileName;

                return actionContext.file(file -> file.storeContent(finalFilename, inputStream));
            }
        }
    }

    private static String getExportMimeType(String mimeType) {
        String exportMimeType = null;

        if (mimeType.contains("application/vnd.google-apps.document")) {
            exportMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (mimeType.contains("application/vnd.google-apps.spreadsheet")) {
            exportMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (mimeType.contains("application/vnd.google-apps.presentation")) {
            exportMimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }

        return exportMimeType;
    }
}
