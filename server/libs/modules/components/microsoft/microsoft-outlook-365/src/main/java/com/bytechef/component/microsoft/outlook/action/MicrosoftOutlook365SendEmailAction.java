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
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ATTACHMENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BCC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REPLY_TO;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.createRecipientList;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.getAttachments;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.microsoft.outlook.constant.ContentType;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365SendEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendEmail")
        .title("Send Email")
        .description("Send the message.")
        .properties(
            array(TO_RECIPIENTS)
                .label("To Recipients")
                .description("The To: recipients for the message.")
                .items(string().controlType(ControlType.EMAIL))
                .required(true),
            string(SUBJECT)
                .label("Subject")
                .description("The subject of the message.")
                .required(true),
            array(BCC_RECIPIENTS)
                .label("Bcc Recipients")
                .description("The Bcc recipients for the message.")
                .items(string().controlType(ControlType.EMAIL))
                .required(false),
            array(CC_RECIPIENTS)
                .label("Cc Recipients")
                .description("The Cc recipients for the message.")
                .items(string().controlType(ControlType.EMAIL))
                .required(false),
            array(REPLY_TO)
                .label("Reply To")
                .description("The email addresses to use when replying.")
                .items(string().controlType(ControlType.EMAIL))
                .required(false),
            object(BODY)
                .label("Body")
                .description("The body of the message. It can be in HTML or text format.")
                .properties(
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
                        .description("The content of the item.")
                        .controlType(ControlType.RICH_TEXT)
                        .displayCondition("body.contentType == '%s'".formatted(ContentType.HTML))
                        .required(false),
                    string(CONTENT)
                        .label("Text Content")
                        .description("The content of the item.")
                        .controlType(ControlType.TEXT_AREA)
                        .displayCondition("body.contentType == '%s'".formatted(ContentType.TEXT))
                        .required(false))
                .required(true),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("A list of attachments to send with the email.")
                .items(fileEntry())
                .required(false))
        .perform(MicrosoftOutlook365SendEmailAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOutlook365SendEmailAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.post("/me/sendMail"))
            .body(
                Http.Body.of(
                    "message",
                    new Object[] {
                        SUBJECT, inputParameters.getRequiredString(SUBJECT),
                        BODY, inputParameters.get(BODY),
                        TO_RECIPIENTS, createRecipientList(inputParameters.getList(TO_RECIPIENTS, String.class)),
                        CC_RECIPIENTS, createRecipientList(inputParameters.getList(CC_RECIPIENTS, String.class)),
                        BCC_RECIPIENTS, createRecipientList(inputParameters.getList(BCC_RECIPIENTS, String.class)),
                        REPLY_TO, createRecipientList(inputParameters.getList(REPLY_TO, String.class)),
                        ATTACHMENTS, getAttachments(context, inputParameters.getList(ATTACHMENTS, FileEntry.class))
                    }))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return null;
    }
}
