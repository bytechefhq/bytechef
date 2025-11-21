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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ATTACHMENTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BCC;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BODY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.CC;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.EMAIL_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL_IDS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.THREAD_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.getEncodedEmail;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.getMessage;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.sendMail;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import java.io.IOException;

/**
 * @author Monika Kušter
 */
public class GoogleMailReplyToEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("replyToEmail")
        .title("Reply to Email")
        .description("Send a reply to an email message.")
        .properties(
            string(ID)
                .label("Message ID")
                .description("The ID of the message to reply to.")
                .required(true),
            array(TO)
                .label("To")
                .description("Recipients email addresses.")
                .items(EMAIL_PROPERTY)
                .required(true),
            array(BCC)
                .label("Bcc")
                .description("Bcc recipients email addresses.")
                .items(EMAIL_PROPERTY)
                .required(false),
            array(CC)
                .label("Cc")
                .description("Cc recipients email addresses.")
                .items(EMAIL_PROPERTY)
                .required(false),
            string(BODY)
                .label("Body")
                .description("Body text of the email")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("A list of attachments to send with the email.")
                .items(fileEntry()))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("The ID of the message."),
                        string(THREAD_ID)
                            .description("The ID of the thread the message belongs to."),
                        array(LABEL_IDS)
                            .description("List of IDs of labels applied to this message.")
                            .items(string()))))
        .perform(GoogleMailReplyToEmailAction::perform);

    private GoogleMailReplyToEmailAction() {
    }

    public static Message perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException, MessagingException {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        Message message = getMessage(inputParameters, gmail);

        Message newMessage = new Message();

        newMessage.setRaw(getEncodedEmail(inputParameters, context, message));
        newMessage.setThreadId(message.getThreadId());
        newMessage.setHistoryId(message.getHistoryId());

        return sendMail(gmail, newMessage);
    }
}
