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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.COMMENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.createRecipientList;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.microsoft.outlook.constant.ContentType;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365OptionUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365ForwardEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("forwardEmail")
        .title("Forward Email")
        .description("Forwards an email message to another recipient.")
        .help("", "https://docs.bytechef.io/reference/components/microsoft-outlook-365_v1#forward-email")
        .properties(
            string(ID)
                .label("Message ID")
                .description("The ID of the message to forward.")
                .options((OptionsFunction<String>) MicrosoftOutlook365OptionUtils::getMessageIdOptions)
                .required(true),
            array(TO_RECIPIENTS)
                .label("To Recipients")
                .description("The To: recipients for the message.")
                .items(string().controlType(ControlType.EMAIL))
                .required(true),
            string(CONTENT_TYPE)
                .label("Content Type")
                .description("The type of the content.")
                .options(
                    option("Text", ContentType.TEXT.name()),
                    option("HTML", ContentType.HTML.name()))
                .defaultValue(ContentType.TEXT.name())
                .required(false),
            string(CONTENT)
                .label("HTML Content")
                .description("Body text of the email in HTML format.")
                .controlType(ControlType.RICH_TEXT)
                .displayCondition("contentType == '%s'".formatted(ContentType.HTML))
                .required(false),
            string(CONTENT)
                .label("Text Content")
                .description("Body text of the email.")
                .controlType(ControlType.TEXT_AREA)
                .displayCondition("contentType == '%s'".formatted(ContentType.TEXT))
                .required(false))
        .perform(MicrosoftOutlook365ForwardEmailAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOutlook365ForwardEmailAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.post("/me/messages/%s/forward".formatted(inputParameters.getRequiredString(ID))))
            .body(
                Http.Body.of(
                    TO_RECIPIENTS, createRecipientList(inputParameters.getList(TO_RECIPIENTS, String.class)),
                    COMMENT, inputParameters.getString(CONTENT)))
            .execute();

        return null;
    }
}
