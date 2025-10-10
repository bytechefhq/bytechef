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

package com.bytechef.component.email.action;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.email.constant.EmailConstants.PORT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.email.EmailProtocol;
import com.bytechef.component.email.commons.EmailUtils;
import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class SendEmailAction {

    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String CC = "cc";
    private static final String BCC = "bcc";
    private static final String REPLY_TO = "replyTo";
    private static final String SUBJECT = "subject";
    private static final String CONTENT = "content";
    private static final String ATTACHMENTS = "attachments";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("send")
        .title("Send")
        .description("Send an email to any address.")
        .properties(
            integer(PORT)
                .label("Port")
                .description("Defines the port to connect to the email server.")
                .required(true)
                .defaultValue(25),
            string(FROM)
                .label("From Email")
                .description("From who to send the email.")
                .required(true),
            array(TO)
                .label("To Email")
                .description("Who to send the email to.")
                .items(string())
                .required(true),
            array(CC)
                .label("CC Email")
                .description("Who to CC on the email.")
                .items(string()),
            array(BCC)
                .label("BCC Email")
                .description("Who to BCC on the email.")
                .items(string()),
            array(REPLY_TO)
                .label("Reply To")
                .description("When someone replies to this email, where should it go to?")
                .items(string()),
            string(SUBJECT)
                .label("Subject")
                .description("Your email subject.")
                .required(true),
            string(CONTENT)
                .label("Content")
                .description("Your email content. Will be sent as a HTML email.")
                .controlType(Property.ControlType.RICH_TEXT),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("A list of attachments to send with the email.")
                .items(fileEntry()))
        .perform(SendEmailAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws MessagingException, IOException {

        int port = inputParameters.getRequiredInteger(PORT);
        Session session;

        if (connectionParameters.containsKey(USERNAME)) {
            session =
                Session.getInstance(EmailUtils.getMailSessionProperties(port, EmailProtocol.smtp, connectionParameters),
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(
                                connectionParameters.getRequiredString(USERNAME),
                                connectionParameters.getRequiredString(PASSWORD));
                        }
                    });
        } else {
            session = Session
                .getInstance(EmailUtils.getMailSessionProperties(port, EmailProtocol.smtp, connectionParameters));
        }

        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(inputParameters.getRequiredString(FROM)));

        message.setRecipients(RecipientType.TO, InternetAddress.parse(
            String.join(",", inputParameters.getRequiredList(TO, String.class))));

        if (inputParameters.containsKey(CC)) {
            message.setRecipients(RecipientType.CC, InternetAddress.parse(
                String.join(",", inputParameters.getRequiredList(CC, String.class))));
        }

        if (inputParameters.containsKey(BCC)) {
            message.setRecipients(RecipientType.BCC, InternetAddress.parse(
                String.join(",", inputParameters.getRequiredList(BCC, String.class))));
        }

        if (inputParameters.containsKey(REPLY_TO)) {
            message.setReplyTo(InternetAddress.parse(
                String.join(",", inputParameters.getRequiredList(REPLY_TO, String.class))));
        }

        if (inputParameters.containsKey(SUBJECT)) {
            message.setSubject(inputParameters.getString(SUBJECT));
        }

        MimeBodyPart mimeBodyPart = new MimeBodyPart();

        mimeBodyPart.setContent(inputParameters.getRequiredString(CONTENT), "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();

        multipart.addBodyPart(mimeBodyPart);

        if (inputParameters.containsKey(ATTACHMENTS)) {
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();

            for (FileEntry fileEntry : inputParameters.getFileEntries(ATTACHMENTS, List.of())) {
                attachmentBodyPart.setDataHandler(
                    new DataHandler(new ByteArrayDataSource(
                        (InputStream) context.file(file -> file.getInputStream(fileEntry)), fileEntry.getMimeType())));

                multipart.addBodyPart(attachmentBodyPart);
            }
        }

        message.setContent(multipart);

        Transport.send(message);

        context.log(log -> log.debug(
            "Message sent: from:{}, to:{}, subject:{}",
            message.getFrom(), message.getRecipients(RecipientType.TO), message.getSubject()));

        return null;
    }
}
