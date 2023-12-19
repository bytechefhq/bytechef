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

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ENTRY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.UPLOAD_FILE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.component.google.drive.util.GoogleUtils;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ActionContext.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.hermes.component.definition.OptionsDataSource.OptionsResponse;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.definition.Option;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Mario Cvjetojevic
 * @author Ivica Cardic
 */
public final class GoogleDriveUploadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(UPLOAD_FILE)
        .title("Upload file")
        .description("Uploads a file to google drive.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description(
                    "The object property which contains a reference to the file to upload.")
                .required(true),
            string(FOLDER)
                .label("Folder")
                .description(
                    "The directory where the file is uploaded.")
                .options(getOptionsFunction())
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string("id")))
        .sampleOutput(Map.of("id", "1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr"))
        .perform(GoogleDriveUploadAction::perform);

    private GoogleDriveUploadAction() {
    }

    public static ActionOptionsFunction getOptionsFunction() {
        return (inputParameters, connectionParameters, searchText, context) -> {
            List<Option<String>> options;

            Drive service = GoogleUtils.getDrive(connectionParameters);

            try {
                List<com.google.api.services.drive.model.Drive> drives = service
                    .drives()
                    .list()
                    .execute()
                    .getDrives();

                options = drives.stream()
                    .filter(drive -> !StringUtils.isNotEmpty(searchText) ||
                        StringUtils.startsWith(drive.getName(), searchText))
                    .map(drive -> (Option<String>)option(drive.getName(), drive.getId()))
                    .toList();
            } catch (IOException e) {
                throw new ComponentExecutionException(e);
            }

            return new OptionsResponse(options);
        };
    }

    public static File perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext actionContext)
        throws ComponentExecutionException {

        Drive drive = GoogleUtils.getDrive(connectionParameters);
        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE_ENTRY);

        try {
            return drive
                .files()
                .create(
                    new File().setName(fileEntry.getName() + "." + fileEntry.getExtension()),
                    new FileContent(
                        fileEntry.getMimeType(),
                        actionContext.file(file -> file.toTempFile(fileEntry))))
                .setFields("id")
                .execute();
        } catch (IOException ioException) {
            throw new ComponentExecutionException(ioException.getMessage(), ioException);
        }
    }
}
