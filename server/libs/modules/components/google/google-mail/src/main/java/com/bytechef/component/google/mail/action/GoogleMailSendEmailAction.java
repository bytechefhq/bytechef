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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ATTACHMENTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BCC;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BODY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.CC;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.EMAIL_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FROM;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.REPLY_TO;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SEND_EMAIL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.activation.DataHandler;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.apache.commons.codec.binary.Base64;

/**
 * @author Monika Domiter
 */
public class GoogleMailSendEmailAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEND_EMAIL)
        .title("Send Email")
        .description("Sends the specified message to the recipients in the To, Cc, and Bcc headers.")
        .properties(
            string(FROM)
                .label("From")
                .description("Email address of the sender, the mailbox account.")
                .required(true),
            array(TO)
                .label("To")
                .description("Recipients email addresses.")
                .items(EMAIL_PROPERTY)
                .required(true),
            string(SUBJECT)
                .label("Subject")
                .description("Subject of the email.")
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
            array(REPLY_TO)
                .label("Reply to")
                .description("Reply-to email addresses.")
                .items(EMAIL_PROPERTY)
                .required(false),
            string(BODY)
                .label("Body")
                .description("Body text of the email")
                .required(true),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("A list of attachments to send with the email.")
                .items(fileEntry()))
        .outputSchema(MESSAGE_PROPERTY)
        .perform(GoogleMailSendEmailAction::perform);

    private GoogleMailSendEmailAction() {
    }

    public static Message perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws IOException, MessagingException {

        Gmail service = GoogleServices.getMail(connectionParameters);

        Properties properties = new Properties();

        Session session = Session.getDefaultInstance(properties, null);

        MimeMessage mimeMessage = new MimeMessage(session);

        mimeMessage.setFrom(new InternetAddress(inputParameters.getRequiredString(FROM)));
        mimeMessage.setRecipients(
            RecipientType.TO,
            InternetAddress.parse(String.join(",", inputParameters.getRequiredList(TO, String.class))));
        mimeMessage.setSubject(inputParameters.getRequiredString(SUBJECT));
        mimeMessage.setText(inputParameters.getRequiredString(BODY));
        mimeMessage.setRecipients(
            RecipientType.CC,
            InternetAddress.parse(String.join(",", inputParameters.getList(CC, String.class, List.of()))));

        mimeMessage.setRecipients(
            RecipientType.BCC,
            InternetAddress.parse(String.join(",", inputParameters.getList(BCC, String.class, List.of()))));
        mimeMessage.setReplyTo(
            InternetAddress.parse(String.join(",", inputParameters.getList(REPLY_TO, String.class, List.of()))));

        MimeBodyPart mimeBodyPart = new MimeBodyPart();

        mimeBodyPart.setContent(inputParameters.getRequiredString(BODY), "text/plain");

        Multipart multipart = new MimeMultipart();

        multipart.addBodyPart(mimeBodyPart);

        MimeBodyPart attachmentBodyPart = new MimeBodyPart();

        for (FileEntry fileEntry : inputParameters.getFileEntries(ATTACHMENTS, List.of())) {
            attachmentBodyPart.setDataHandler(
                new DataHandler(
                    new ByteArrayDataSource(
                        (InputStream) actionContext.file(file -> file.getStream(fileEntry)), fileEntry.getMimeType())));
            attachmentBodyPart.setFileName(fileEntry.getName());

            multipart.addBodyPart(attachmentBodyPart);
        }

        mimeMessage.setContent(multipart);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        mimeMessage.writeTo(buffer);

        byte[] rawMessageBytes = buffer.toByteArray();

        String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);

        Message message = new Message();

        message.setRaw(encodedEmail);

        return service
            .users()
            .messages()
            .send("me", message)
            .execute();
    }
}
