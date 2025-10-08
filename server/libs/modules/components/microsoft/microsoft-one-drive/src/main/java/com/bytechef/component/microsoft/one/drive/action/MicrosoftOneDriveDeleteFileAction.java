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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftOneDriveDeleteFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteFile")
        .title("Delete File")
        .description("Delete a selected file from Microsoft One Drive.")
        .properties(
            string(ID)
                .label("File ID")
                .description("The id of a file to delete.")
                .options((OptionsFunction<String>) MicrosoftOneDriveUtils::getFileIdOptions)
                .required(true))
        .perform(MicrosoftOneDriveDeleteFileAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOneDriveDeleteFileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.delete("/me/drive/items/%s".formatted(inputParameters.getRequiredString(ID))))
            .execute();

        return null;
    }
}
