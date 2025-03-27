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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.COPY_FILE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.COPY_FILE_DESCRIPTION;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.COPY_FILE_TITLE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_OUTPUT_PROPERTY;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputSchema;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.drive.model.File;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;

/**
 * @author Mayank Madan
 */
public class GoogleDriveCopyFileAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        string(FILE_ID)
            .label("File ID")
            .description("The id of the file to be copied.")
            .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER, false))
            .required(true),
        string(FILE_NAME)
            .label("New File Name")
            .description("The name of the new file created as a result of the copy operation.")
            .required(true),
        string(FOLDER_ID)
            .label("Destination Folder ID")
            .description("The ID of the folder where the copied file will be stored.")
            .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER, true))
            .required(true)
    };

    public static final OutputSchema<ObjectProperty> OUTPUT_SCHEMA = outputSchema(GOOGLE_FILE_OUTPUT_PROPERTY);

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(COPY_FILE)
        .title(COPY_FILE_TITLE)
        .description(COPY_FILE_DESCRIPTION)
        .properties(PROPERTIES)
        .output(OUTPUT_SCHEMA)
        .perform(GoogleDriveCopyFileAction::perform);

    private GoogleDriveCopyFileAction() {
    }

    public static File perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException {

        return GoogleUtils.copyFileOnGoogleDrive(connectionParameters, inputParameters);
    }
}
