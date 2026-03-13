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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.bytechef.google.commons.GoogleUtils;

/**
 * @author Marija Horvat
 */
public class GoogleDriveShareFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("shareFile")
        .title("Share File")
        .description("Share a specified file from your Google Drive.")
        .help("", "https://docs.bytechef.io/reference/components/google-drive_v1#share-file")
        .properties(
            string(FILE_ID)
                .label("File ID")
                .description("ID of the file to be shared.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER, false))
                .required(true))
        .output(
            outputSchema(
                string()
                    .description("A link for opening the file in a relevant Google editor or viewer in a browser.")))
        .perform(GoogleDriveShareFileAction::perform);

    private GoogleDriveShareFileAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return GoogleDriveUtils.getFileWebViewLink(connectionParameters, inputParameters.getRequiredString(FILE_ID));
    }
}
