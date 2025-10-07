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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.drive.util.GoogleDriveUtils.listFiles;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.drive.model.File;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleDriveListFoldersAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listFolders")
        .title("List Folders")
        .description("List folders in a Google Drive folder.")
        .properties(
            string(FOLDER_ID)
                .label("Parent Folder ID")
                .description(
                    "ID of the Folder from which you want to list folders. If no folder is specified, the root " +
                        "folder will be used.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER, true))
                .required(false))
        .output(
            outputSchema(
                array()
                    .items(GOOGLE_FILE_OUTPUT_PROPERTY)))
        .perform(GoogleDriveListFoldersAction::perform);

    private GoogleDriveListFoldersAction() {
    }

    public static List<File> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return listFiles(inputParameters.getString(FOLDER_ID, "root"), true, connectionParameters);
    }
}
