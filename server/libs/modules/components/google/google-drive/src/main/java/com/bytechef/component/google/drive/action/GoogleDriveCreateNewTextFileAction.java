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
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.MIME_TYPE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.TEXT;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.DRIVE_ID;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.IGNORE_DEFAULT_VISIBILITY;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.INCLUDE_LABELS;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.INCLUDE_PERMISSIONS_FOR_VIEW;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.KEEP_REVISION_FOREVER;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.OCR_LANGUAGE;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.SUPPORTS_ALL_DRIVES;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.USE_CONTENT_AS_INDEXABLE_TEXT;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.propertyMap;
import static com.bytechef.component.google.drive.properties.GoogleDriveOutputProperties.FILE_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 */
public final class GoogleDriveCreateNewTextFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_NEW_TEXT_FILE)
        .title("Create new text file")
        .description("Creates a new text file in google drive.")
        .properties(
            string(FILE_NAME)
                .label("File name")
                .description(
                    "The name of the new text file.")
                .required(true),
            string(TEXT)
                .label("Text")
                .description(
                    "The text content to add to file."),
            string(MIME_TYPE)
                .label("Content type")
                .description(
                    "Select file type.")
                .options(
                    option("Text", "plain/text"),
                    option("CSV", "text/csv"),
                    option("XML", "text/xml"))
                .defaultValue("plain/text"),
            propertyMap.get(DRIVE_ID),
            propertyMap.get(IGNORE_DEFAULT_VISIBILITY),
            propertyMap.get(KEEP_REVISION_FOREVER),
            propertyMap.get(OCR_LANGUAGE),
            propertyMap.get(SUPPORTS_ALL_DRIVES),
            propertyMap.get(USE_CONTENT_AS_INDEXABLE_TEXT),
            propertyMap.get(INCLUDE_PERMISSIONS_FOR_VIEW),
            propertyMap.get(INCLUDE_LABELS))
        .outputSchema(FILE_PROPERTY)
        .sampleOutput(Map.of("id", "1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr"))
        .perform(GoogleDriveCreateNewTextFileAction::perform);

    private GoogleDriveCreateNewTextFileAction() {
    }

    public static Map<String, File> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws Exception {

        Drive drive = GoogleDriveUtils.getDrive(connectionParameters);

        File googleFile = new File().setName(inputParameters.getRequiredString(FILE_NAME));

        String suffix = switch (inputParameters.getString(MIME_TYPE)) {
            case "text/csv" -> ".csv";
            case "text/xml" -> ".xml";
            default -> ".txt";
        };

        java.io.File file = java.io.File.createTempFile("New File", suffix);

        try (BufferedWriter bufferedWriter = new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            bufferedWriter.write(inputParameters.getString(TEXT));
        }

        return Map.of(
            "file",
            drive
                .files()
                .create(googleFile, new FileContent(inputParameters.getString(MIME_TYPE), file))
                .setFields("id")
                .setIgnoreDefaultVisibility(inputParameters.getBoolean(IGNORE_DEFAULT_VISIBILITY))
                .setKeepRevisionForever(inputParameters.getBoolean(KEEP_REVISION_FOREVER))
                .setOcrLanguage(inputParameters.getString(OCR_LANGUAGE))
                .setSupportsAllDrives(inputParameters.getBoolean(SUPPORTS_ALL_DRIVES))
                .setUseContentAsIndexableText(inputParameters.getBoolean(USE_CONTENT_AS_INDEXABLE_TEXT))
                .setIncludePermissionsForView(inputParameters.getString(INCLUDE_PERMISSIONS_FOR_VIEW))
                .setIncludeLabels(inputParameters.getString(INCLUDE_LABELS))
                .execute());
    }
}
