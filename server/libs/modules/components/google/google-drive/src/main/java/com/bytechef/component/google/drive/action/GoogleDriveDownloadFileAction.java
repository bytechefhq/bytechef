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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
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
                .label("File ID")
                .description("ID of the file to download.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER, false))
                .required(true))
        .output(outputSchema(fileEntry()))
        .perform(GoogleDriveDownloadFileAction::perform);

    private GoogleDriveDownloadFileAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Drive drive = GoogleServices.getDrive(connectionParameters);

        String fileId = inputParameters.getRequiredString(FILE_ID);

        Drive.Files.Get get;
        File googleFile;

        try {
            get = drive
                .files()
                .get(fileId);

            googleFile = get
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }

        String mimeType = googleFile.getMimeType();
        String fileName = googleFile.getName();
        String exportMimeType = getExportMimeType(mimeType);

        if (exportMimeType != null) {
            try (InputStream inputStream = drive.files()
                .export(fileId, exportMimeType)
                .executeMediaAsInputStream()) {

                String extension = context.mimeType(mimeType1 -> mimeType1.lookupExt(exportMimeType));

                String finalFileName = fileName + "." + extension;

                return context.file(file -> file.storeContent(finalFileName, inputStream));
            } catch (IOException e) {
                throw translateGoogleIOException(e);
            }
        } else {
            String fileExtension = googleFile.getFileExtension();

            if (fileExtension == null || fileExtension.isEmpty()) {
                fileExtension =
                    context.mimeType(
                        mimeType1 -> mimeType1.lookupExt(mimeType.equals("plain/text") ? "text/plain" : mimeType));
                if (fileExtension != null && !fileName.contains(fileExtension)) {
                    fileName += "." + fileExtension;
                }
            }

            try (InputStream inputStream = get.executeMediaAsInputStream()) {
                String finalFilename = fileName;

                return context.file(file -> file.storeContent(finalFilename, inputStream));
            } catch (IOException e) {
                throw translateGoogleIOException(e);
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
