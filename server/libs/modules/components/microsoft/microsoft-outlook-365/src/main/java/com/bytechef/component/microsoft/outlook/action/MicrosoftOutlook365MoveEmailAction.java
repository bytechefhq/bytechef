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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DESTINATION_ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365OptionUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365MoveEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("moveEmail")
        .title("Move Email")
        .description("Moves a email to another folder within the user's mailbox. ")
        .properties(
            string(ID)
                .label("Message ID")
                .description("ID of the message to move.")
                .options((OptionsFunction<String>) MicrosoftOutlook365OptionUtils::getMessageIdOptions)
                .required(true),
            string(DESTINATION_ID)
                .label("Folder ID")
                .description("The destination folder ID.")
                .options((OptionsFunction<String>) MicrosoftOutlook365OptionUtils::getFolderIdOptions)
                .required(true))
        .output()
        .perform(MicrosoftOutlook365MoveEmailAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOutlook365MoveEmailAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/me/messages/%s/move".formatted(inputParameters.getRequiredString(ID))))
            .body(Http.Body.of(Map.of(DESTINATION_ID, inputParameters.getRequiredString(DESTINATION_ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
