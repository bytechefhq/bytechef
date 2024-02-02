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

package com.bytechef.component.email.action;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.email.constant.EmailConstants.HOST;
import static com.bytechef.component.email.constant.EmailConstants.PORT;
import static com.bytechef.component.email.constant.EmailConstants.SEND;
import static com.bytechef.component.email.constant.EmailConstants.TLS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
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
import java.util.Objects;
import java.util.Properties;

/**
 * @author Ivica Cardic
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

    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action(SEND)
        .title("Send")
        .description("Send an email to any address.")
        .properties(
            integer(FROM)
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
                .description("Your email content. Will be sent as a HTML email."),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("A list of attachments to send with the email.")
                .items(fileEntry()))
        .perform(SendEmailAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws MessagingException, IOException {

        Properties properties = new Properties();

        properties.put("mail.smtp.host", connectionParameters.getRequiredString(HOST));
        properties.put("mail.smtp.port", connectionParameters.getRequiredInteger(PORT));

        if (Objects.equals(connectionParameters.getBoolean(TLS), false)) {
            properties.put("mail.smtp.starttls.enable", "true");
//            prop.put("mail.smtp.ssl.trust", MapUtils.getRequiredString(context.getConnectionParameters(), HOST));
        }

        Session session;

        if (connectionParameters.containsKey(USERNAME)) {
            properties.put("mail.smtp.auth", true);

            session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        connectionParameters.getRequiredString(USERNAME),
                        connectionParameters.getRequiredString(PASSWORD));
                }
            });
        } else {
            session = Session.getInstance(properties);
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
                        (InputStream) context.file(file -> file.getStream(fileEntry)), fileEntry.getMimeType())));

                multipart.addBodyPart(attachmentBodyPart);
            }
        }

        message.setContent(multipart);

        Transport.send(message);

        context.logger(logger -> logger.debug(
            "Message sent: from:{}, to:{}, subject:{}",
            message.getFrom(), message.getRecipients(RecipientType.TO), message.getSubject()));

        return null;
    }
}
