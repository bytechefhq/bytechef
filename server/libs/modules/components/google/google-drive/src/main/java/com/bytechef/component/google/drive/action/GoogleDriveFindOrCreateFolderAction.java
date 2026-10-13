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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FOLDER_NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.ID;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.MIME_TYPE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.NAME;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Igor Beslic
 */
public class GoogleDriveFindOrCreateFolderAction {

    protected static final String ROOT_FOLDER_ALIAS = "root";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("findOrCreateFolder")
        .title("Find or Create Folder")
        .description(
            "Finds a folder by name inside the parent folder and returns it; if no such folder exists, creates it " +
                "and returns the new folder. Safe to call repeatedly, e.g. inside a loop, without producing " +
                "duplicate folders.")
        .help("", "https://docs.bytechef.io/reference/components/google-drive_v1#find-or-create-folder")
        .properties(
            string(FOLDER_NAME)
                .label("Folder Name")
                .description(
                    "The exact name of the folder to find, or to create if it does not exist. If several folders " +
                        "share this name in the parent folder, the oldest one is returned.")
                .required(true),
            string(FOLDER_ID)
                .label("Parent Folder ID")
                .description(
                    "ID of the folder in which to look for the folder, and in which it will be created if missing; " +
                        "if no folder is selected, the root folder is used.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER, true))
                .required(false))
        .output(
            outputSchema(GOOGLE_FILE_OUTPUT_PROPERTY),
            sampleOutput(
                Map.of(
                    ID, "1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr", MIME_TYPE, APPLICATION_VND_GOOGLE_APPS_FOLDER,
                    NAME, "new-folder")))
        .perform(GoogleDriveFindOrCreateFolderAction::perform);

    private GoogleDriveFindOrCreateFolderAction() {
    }

    public static File perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Drive drive = GoogleServices.getDrive(connectionParameters);
        String folderName = inputParameters.getRequiredString(FOLDER_NAME);
        String parentFolderId = inputParameters.getString(FOLDER_ID);

        if (StringUtils.isBlank(parentFolderId)) {
            parentFolderId = null;
        }

        try {
            File existingFolder = findFolder(drive, folderName, parentFolderId);

            if (existingFolder != null) {
                return existingFolder;
            }

            File folderFile = new File()
                .setName(folderName)
                .setMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER)
                .setParents(parentFolderId == null ? null : List.of(parentFolderId));

            return drive
                .files()
                .create(folderFile)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }

    private static String escapeQueryValue(String value) {
        String escapedValue = value.replace("\\", "\\\\");

        return escapedValue.replace("'", "\\'");
    }

    private static File findFolder(Drive drive, String folderName, String parentFolderId) throws IOException {
        String query = "mimeType = '%s' and name = '%s' and '%s' in parents and trashed = false".formatted(
            APPLICATION_VND_GOOGLE_APPS_FOLDER, escapeQueryValue(folderName),
            parentFolderId == null ? ROOT_FOLDER_ALIAS : escapeQueryValue(parentFolderId));

        FileList fileList = drive
            .files()
            .list()
            .setQ(query)
            .setFields("files(id, kind, mimeType, name)")
            .setOrderBy("createdTime")
            .setPageSize(1)
            .execute();

        List<File> files = fileList.getFiles();

        if (files == null || files.isEmpty()) {
            return null;
        }

        return files.getFirst();
    }
}
