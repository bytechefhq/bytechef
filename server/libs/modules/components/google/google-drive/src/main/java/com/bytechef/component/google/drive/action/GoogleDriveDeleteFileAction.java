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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import java.io.IOException;

/**
 * @author Mayank Madan
 */
public class GoogleDriveDeleteFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteFile")
        .title("Delete File")
        .description("Delete a selected file from Google Drive.")
        .properties(
            string(FILE_ID)
                .label("File")
                .description("The id of a file to delete.")
                .options((ActionOptionsFunction<String>) GoogleDriveUtils::getFileOptions)
                .required(true))
        .perform(GoogleDriveDeleteFileAction::perform);

    private GoogleDriveDeleteFileAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        return drive
            .files()
            .delete(inputParameters.getRequiredString(FILE_ID))
            .execute();
    }
}
