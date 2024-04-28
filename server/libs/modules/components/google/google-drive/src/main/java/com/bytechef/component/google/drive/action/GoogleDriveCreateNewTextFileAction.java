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
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.CREATE_NEW_TEXT_FILE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_SAMPLE_OUTPUT;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.MIME_TYPE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.TEXT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.google.drive.util.GoogleDriveOptionUtils;
import com.bytechef.google.commons.GoogleServices;
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
 * @author Monika Domiter
 */
public final class GoogleDriveCreateNewTextFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_NEW_TEXT_FILE)
        .title("Create new text file")
        .description("Creates a new text file in Google Drive.")
        .properties(
            string(FILE_NAME)
                .label("File name")
                .description("The name of the new text file.")
                .required(true),
            string(TEXT)
                .label("Text")
                .description("The text content to add to file.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            string(MIME_TYPE)
                .label("File type")
                .description("Select file type.")
                .options(
                    option("Text", "plain/text"),
                    option("CSV", "text/csv"),
                    option("XML", "text/xml"))
                .defaultValue("plain/text")
                .required(true),
            string(PARENT_FOLDER)
                .label("Parent folder")
                .description(
                    "Folder where the file should be created; if no folder is selected, the file will be created " +
                        "in the root folder.")
                .options((ActionOptionsFunction<String>) GoogleDriveOptionUtils::getFolderOptions)
                .required(false))
        .outputSchema(GOOGLE_FILE_OUTPUT_PROPERTY)
        .sampleOutput(GOOGLE_FILE_SAMPLE_OUTPUT)
        .perform(GoogleDriveCreateNewTextFileAction::perform);

    private GoogleDriveCreateNewTextFileAction() {
    }

    public static File perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        File newFile = new File()
            .setName(inputParameters.getRequiredString(FILE_NAME))
            .setParents(inputParameters.getString(PARENT_FOLDER) == null
                ? null : List.of(inputParameters.getString(PARENT_FOLDER)));

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

        return drive
            .files()
            .create(newFile, new FileContent(mimeType, file))
            .execute();
    }
}
