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
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.CREATE_NEW_TEXT_FILE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.DRIVE_ID;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.IGNORE_DEFAULT_VISIBILITY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.INCLUDE_LABELS;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.INCLUDE_PERMISSIONS_FOR_VIEW;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.KEEP_REVISION_FOREVER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.MIME_TYPE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.OCR_LANGUAGE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PROPERTY_MAP;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.SUPPORTS_ALL_DRIVES;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.TEXT;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.USE_CONTENT_AS_INDEXABLE_TEXT;

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
            PROPERTY_MAP.get(DRIVE_ID),
            PROPERTY_MAP.get(IGNORE_DEFAULT_VISIBILITY),
            PROPERTY_MAP.get(KEEP_REVISION_FOREVER),
            PROPERTY_MAP.get(OCR_LANGUAGE),
            PROPERTY_MAP.get(SUPPORTS_ALL_DRIVES),
            PROPERTY_MAP.get(USE_CONTENT_AS_INDEXABLE_TEXT),
            PROPERTY_MAP.get(INCLUDE_PERMISSIONS_FOR_VIEW),
            PROPERTY_MAP.get(INCLUDE_LABELS))
        .outputSchema(object().properties(string("id")))
        .sampleOutput(Map.of("id", "1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr"))
        .perform(GoogleDriveCreateNewTextFileAction::perform);

    private GoogleDriveCreateNewTextFileAction() {
    }

    public static File perform(
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

        return drive
            .files()
            .create(googleFile, new FileContent(inputParameters.getString(MIME_TYPE), file))
            .setFields("id")
            .setIgnoreDefaultVisibility(inputParameters.getBoolean("ignoreDefaultVisibility"))
            .setKeepRevisionForever(inputParameters.getBoolean("keepRevisionForever"))
            .setOcrLanguage(inputParameters.getString("ocrLanguage"))
            .setSupportsAllDrives(inputParameters.getBoolean("supportsAllDrives"))
            .setUseContentAsIndexableText(inputParameters.getBoolean("useContentAsIndexableText"))
            .setIncludePermissionsForView(inputParameters.getString("includePermissionsForView"))
            .setIncludeLabels(inputParameters.getString("includeLabels"))
            .execute();
    }
}
