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
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FORMAT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FORMAT_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.definition.Format.SIMPLE;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.createSimpleMessage;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.outlook.definition.Format;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365OptionUtils;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365GetEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getEmail")
        .title("Get Email")
        .description("Gets the specified email message.")
        .help("", "https://docs.bytechef.io/reference/components/microsoft-outlook-365_v1#get-email")
        .properties(
            string(ID)
                .label("Message ID")
                .description("The ID of the message to retrieve.")
                .options((OptionsFunction<String>) MicrosoftOutlook365OptionUtils::getMessageIdOptions)
                .required(true),
            FORMAT_PROPERTY)
        .output(MicrosoftOutlook365Utils::getMessageOutput)
        .perform(MicrosoftOutlook365GetEmailAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOutlook365GetEmailAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String id = inputParameters.getRequiredString(ID);

        Map<String, Object> messageBody = context.http(http -> http.get("/me/messages/%s".formatted(id)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Format format = inputParameters.getRequired(FORMAT, Format.class);

        if (format.equals(SIMPLE)) {
            return createSimpleMessage(context, messageBody);
        } else {
            return messageBody;
        }
    }
}
