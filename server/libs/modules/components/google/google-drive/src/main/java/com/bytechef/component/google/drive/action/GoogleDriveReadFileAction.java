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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ID;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.READ_FILE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveOptionUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
public final class GoogleDriveReadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(READ_FILE)
        .title("Read file")
        .description("Read a selected file from Google Drive file.")
        .properties(
            string(FILE_ID)
                .label("File")
                .description("The id of a file to read.")
                .options((ActionOptionsFunction<String>) GoogleDriveOptionUtils::getFileOptions)
                .required(true))
        .output()
        .perform(GoogleDriveReadFileAction::perform);

    private GoogleDriveReadFileAction() {
    }

    public static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        String fileId = inputParameters.getRequiredString(FILE_ID);

        Drive.Files files = drive.files();

        Drive.Files.Get get = files.get(fileId);

        try (InputStream inputStream = get.executeMediaAsInputStream()) {
            String fileName = getFileName(files, fileId);

            return actionContext.file(file -> file.storeContent(fileName, inputStream));
        }
    }

    private static String getFileName(Drive.Files files, String fileId) throws IOException {
        return files.list()
            .setQ("mimeType != 'application/vnd.google-apps.folder'")
            .execute()
            .getFiles()
            .stream()
            .filter(file -> fileId.equals(file.getId()))
            .map(File::getName)
            .findFirst()
            .orElse("fileName");
    }
}
