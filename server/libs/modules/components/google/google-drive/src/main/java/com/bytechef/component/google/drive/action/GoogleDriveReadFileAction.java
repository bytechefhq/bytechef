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
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.ACKNOWLEDGE_ABUSE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ID;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.READ_FILE;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.INCLUDE_LABELS;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.INCLUDE_PERMISSIONS_FOR_VIEW;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.SUPPORTS_ALL_DRIVES;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.propertyMap;
import static com.bytechef.component.google.drive.properties.GoogleDriveOutputProperties.FILE_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableOption;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 */
public final class GoogleDriveReadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(READ_FILE)
        .title("Read file")
        .description("Read a selected file from google drive file.")
        .properties(
            string(FILE_ID)
                .label("File")
                .description(
                    "The id of a file to read.")
                .options((ActionOptionsFunction<String>) GoogleDriveReadFileAction::getFileOptions)
                .required(true),
            bool(ACKNOWLEDGE_ABUSE)
                .label("Acknowledge abuse")
                .description(
                    "Whether the user is acknowledging the risk of downloading known malware or other " +
                        "abusive files. This is only applicable when alt=media."),
            propertyMap.get(SUPPORTS_ALL_DRIVES),
            propertyMap.get(INCLUDE_PERMISSIONS_FOR_VIEW),
            propertyMap.get(INCLUDE_LABELS))
        .outputSchema(FILE_PROPERTY)
        .sampleOutput(Map.of("id", "1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr"))
        .perform(GoogleDriveReadFileAction::perform);

    private GoogleDriveReadFileAction() {
    }

    public static File perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws Exception {

        Drive drive = GoogleDriveUtils.getDrive(connectionParameters);

        return drive.files()
            .get(inputParameters.getRequiredString(FILE_ID))
            .setAcknowledgeAbuse(inputParameters.getBoolean(ACKNOWLEDGE_ABUSE))
            .setSupportsAllDrives(inputParameters.getBoolean(SUPPORTS_ALL_DRIVES))
            .setIncludePermissionsForView(inputParameters.getString(INCLUDE_PERMISSIONS_FOR_VIEW))
            .setIncludeLabels(inputParameters.getString(INCLUDE_LABELS))
            .execute();
    }

    private static List<ModifiableOption<String>> getFileOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException {

        Drive drive = GoogleDriveUtils.getDrive(connectionParameters);

        return drive
            .files()
            .list()
            .execute()
            .getFiles()
            .stream()
            .map(file -> option(file.getName(), file.getId()))
            .toList();
    }
}
