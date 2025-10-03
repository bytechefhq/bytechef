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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ATTACHMENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BCC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.COMMENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.createRecipientList;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.getAttachments;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.microsoft.outlook.constant.ContentType;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365OptionUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365ReplyToEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("replyToEmail")
        .title("Reply to Email")
        .description("Creates a new reply to email.")
        .properties(
            string(ID)
                .label("Message ID")
                .description("Id of the message to reply to.")
                .options((ActionOptionsFunction<String>) MicrosoftOutlook365OptionUtils::getMessageIdOptions)
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
                .required(false),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("A list of attachments to send with the email.")
                .items(fileEntry())
                .required(false))
        .perform(MicrosoftOutlook365ReplyToEmailAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOutlook365ReplyToEmailAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> message = new HashMap<>();

        addRecipients(inputParameters, message, CC_RECIPIENTS);
        addRecipients(inputParameters, message, BCC_RECIPIENTS);
        addAttachments(inputParameters.getList(ATTACHMENTS, FileEntry.class), context, message);

        context.http(http -> http.post("/me/messages/%s/reply".formatted(inputParameters.getRequiredString(ID))))
            .body(
                Http.Body.of(
                    "message", message,
                    COMMENT, inputParameters.getString(CONTENT)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return null;
    }

    private static void addRecipients(Parameters inputParameters, Map<String, Object> message, String recipientType) {
        List<String> recipients = inputParameters.getList(recipientType, String.class);

        if (recipients != null && !recipients.isEmpty()) {
            message.put(recipientType, createRecipientList(recipients));
        }
    }

    private static void addAttachments(List<FileEntry> attachments, Context context, Map<String, Object> message) {
        if (attachments != null && !attachments.isEmpty()) {
            message.put(ATTACHMENTS, getAttachments(context, attachments));
        }
    }
}
