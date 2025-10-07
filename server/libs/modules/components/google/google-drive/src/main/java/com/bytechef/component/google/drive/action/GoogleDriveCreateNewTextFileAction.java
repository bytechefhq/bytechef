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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_SAMPLE_OUTPUT;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.MIME_TYPE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.TEXT;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class GoogleDriveCreateNewTextFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createNewTextFile")
        .title("Create New Text File")
        .description("Creates a new text file in Google Drive.")
        .properties(
            string(FILE_NAME)
                .label("File Name")
                .description("The name of the new text file.")
                .required(true),
            string(TEXT)
                .label("Text")
                .description("The text content to add to file.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            string(MIME_TYPE)
                .label("File Type")
                .description("Select file type.")
                .options(
                    option("Text", "plain/text"),
                    option("CSV", "text/csv"),
                    option("XML", "text/xml"))
                .defaultValue("plain/text")
                .required(true),
            string(FOLDER_ID)
                .label("Parent Folder ID")
                .description(
                    "ID of the folder where the file should be created; if no folder is selected, the file will be " +
                        "created in the root folder.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER, true))
                .required(false))
        .output(outputSchema(GOOGLE_FILE_OUTPUT_PROPERTY), sampleOutput(GOOGLE_FILE_SAMPLE_OUTPUT))
        .perform(GoogleDriveCreateNewTextFileAction::perform);

    private GoogleDriveCreateNewTextFileAction() {
    }

    public static File perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException {
        Drive drive = GoogleServices.getDrive(connectionParameters);

        File newFile = new File()
            .setName(inputParameters.getRequiredString(FILE_NAME))
            .setParents(inputParameters.getString(FOLDER_ID) == null
                ? null : List.of(inputParameters.getString(FOLDER_ID)));

        String mimeType = inputParameters.getRequiredString(MIME_TYPE);

        String suffix = switch (mimeType) {
            case "text/csv" -> ".csv";
            case "text/xml" -> ".xml";
            default -> ".txt";
        };

        java.io.File file = java.io.File.createTempFile("New File", suffix);

        try (BufferedWriter bufferedWriter = new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            bufferedWriter.write(inputParameters.getRequiredString(TEXT));
        }

        try {
            return drive
                .files()
                .create(newFile, new FileContent(mimeType, file))
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }
}
