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

package com.bytechef.component.microsoft.one.drive.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.FOLDER_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.NAME;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;
import static com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils.getFolderId;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOneDriveCreateFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createNewFolder")
        .title("Create New Folder")
        .description("Creates a new empty folder in Microsoft OneDrive.")
        .properties(
            string(NAME)
                .label("Folder Name")
                .description("The name of the new folder.")
                .required(true),
            string(PARENT_ID)
                .label("Parent Folder ID")
                .description(
                    "ID of the folder where the new folder will be created; if no folder is selected, the folder " +
                        "will be created in the root folder.")
                .options((ActionOptionsFunction<String>) MicrosoftOneDriveUtils::getFolderIdOptions)
                .required(false))
        .output(outputSchema(FOLDER_OUTPUT_PROPERTY))
        .perform(MicrosoftOneDriveCreateFolderAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOneDriveCreateFolderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post(
                "/me/drive/items/%s/children".formatted(getFolderId(inputParameters.getString(PARENT_ID)))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(Map.of(NAME, inputParameters.getRequiredString(NAME), "folder", Map.of())))
            .execute()
            .getBody();
    }
}
