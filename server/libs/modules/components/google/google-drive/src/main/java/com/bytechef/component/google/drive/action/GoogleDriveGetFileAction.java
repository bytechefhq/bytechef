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
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GET_FILE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GET_FILE_DESCRIPTION;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GET_FILE_TITLE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_OUTPUT_PROPERTY;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputSchema;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;

/**
 * @author Arina Kolodeznikova
 */
public class GoogleDriveGetFileAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        string(FILE_ID)
            .label("File ID")
            .description("ID of the file to be retrieved.")
            .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER, false))
            .required(true)
    };

    public static final OutputSchema<ObjectProperty> OUTPUT_SCHEMA = outputSchema(GOOGLE_FILE_OUTPUT_PROPERTY);

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_FILE)
        .title(GET_FILE_TITLE)
        .description(GET_FILE_DESCRIPTION)
        .properties(PROPERTIES)
        .output(OUTPUT_SCHEMA)
        .perform(GoogleDriveGetFileAction::perform);

    private GoogleDriveGetFileAction() {
    }

    public static File perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        return drive
            .files()
            .get(inputParameters.getRequiredString(FILE_ID))
            .execute();
    }
}
