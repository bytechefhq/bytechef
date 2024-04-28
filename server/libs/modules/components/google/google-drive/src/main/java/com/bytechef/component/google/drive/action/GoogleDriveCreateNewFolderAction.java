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
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.CREATE_NEW_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FOLDER_NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_SAMPLE_OUTPUT;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveOptionUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
public final class GoogleDriveCreateNewFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_NEW_FOLDER)
        .title("Create new folder")
        .description("Creates a new empty folder in Google Drive.")
        .properties(
            string(FOLDER_NAME)
                .label("Folder name")
                .description("The name of the new folder.")
                .required(true),
            string(PARENT_FOLDER)
                .label("Parent folder")
                .description(
                    "Folder where the new folder will be created; if no folder is selected, the folder will be " +
                        "created in the root folder.")
                .options((ActionOptionsFunction<String>) GoogleDriveOptionUtils::getFolderOptions)
                .required(false))
        .outputSchema(GOOGLE_FILE_OUTPUT_PROPERTY)
        .sampleOutput(GOOGLE_FILE_SAMPLE_OUTPUT)
        .perform(GoogleDriveCreateNewFolderAction::perform);

    private GoogleDriveCreateNewFolderAction() {
    }

    public static File perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        File folderFile = new File()
            .setName(inputParameters.getRequiredString(FOLDER_NAME))
            .setMimeType("application/vnd.google-apps.folder")
            .setParents(inputParameters.getString(PARENT_FOLDER) == null
                ? null : List.of(inputParameters.getString(PARENT_FOLDER)));

        return drive
            .files()
            .create(folderFile)
            .execute();
    }
}
