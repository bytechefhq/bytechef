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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BCC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_PROPERTY_HTML;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_PROPERTY_TEXT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_TYPE_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.NAME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.RECIPIENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.RECIPIENT_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REPLY_TO;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO_RECIPIENTS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class MicrosoftOutlook365SendEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendEmail")
        .title("Send Email")
        .description("Send the message.")
        .properties(
            object(FROM)
                .label("From")
                .description(
                    "The owner of the mailbox from which the message is sent. In most cases, this value is " +
                        "the same as the sender property, except for sharing or delegation scenarios. The " +
                        "value must correspond to the actual mailbox used.")
                .properties(
                    object(RECIPIENT)
                        .label("Recipient")
                        .properties(
                            object(EMAIL_ADDRESS)
                                .properties(
                                    string(ADDRESS)
                                        .label("Address")
                                        .description("The email address of the person or entity.")
                                        .required(false),
                                    string(NAME)
                                        .label("Name")
                                        .description("The display name of the person or entity.")
                                        .required(false)))),
            array(TO_RECIPIENTS)
                .label("To Recipients")
                .description("The To: recipients for the message.")
                .items(RECIPIENT_PROPERTY)
                .required(true),
            string(SUBJECT)
                .label("Subject")
                .description("The subject of the message.")
                .required(true),
            array(BCC_RECIPIENTS)
                .label("Bcc Recipients")
                .description("The Bcc recipients for the message.")
                .items(RECIPIENT_PROPERTY)
                .required(false),
            array(CC_RECIPIENTS)
                .label("Cc Recipients")
                .description("The Cc recipients for the message.")
                .items(RECIPIENT_PROPERTY)
                .required(false),
            array(REPLY_TO)
                .label("Reply To")
                .description("The email addresses to use when replying.")
                .items(RECIPIENT_PROPERTY)
                .required(false),
            object(BODY)
                .label("Body")
                .description("The body of the message. It can be in HTML or text format.")
                .properties(
                    CONTENT_TYPE_PROPERTY,
                    CONTENT_PROPERTY_HTML,
                    CONTENT_PROPERTY_TEXT)
                .required(true))
        .perform(MicrosoftOutlook365SendEmailAction::perform);

    private MicrosoftOutlook365SendEmailAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        actionContext.http(http -> http.post("/sendMail"))
            .body(
                Http.Body.of(
                    "message",
                    new Object[] {
                        FROM, inputParameters.get(FROM),
                        SUBJECT, inputParameters.getRequiredString(SUBJECT),
                        BODY, inputParameters.get(BODY),
                        TO_RECIPIENTS, inputParameters.getArray(TO_RECIPIENTS),
                        CC_RECIPIENTS, inputParameters.getArray(CC_RECIPIENTS),
                        BCC_RECIPIENTS, inputParameters.getArray(BCC_RECIPIENTS),
                        REPLY_TO, inputParameters.getArray(REPLY_TO)
                    }))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return null;
    }
}
