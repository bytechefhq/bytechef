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

package com.bytechef.component.google.mail.util;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ATTACHMENTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BCC;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BODY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.CC;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FROM;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.HISTORY_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.INTERNAL_DATE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL_IDS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MINIMAL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.NAME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.PAYLOAD;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.RAW;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.REPLY_TO;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SIMPLE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SIZE_ESTIMATE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SNIPPET;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.THREAD_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Thread;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.activation.DataHandler;
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
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.apache.commons.codec.binary.Base64;

/**
 * @author Monika Kušter
 */
public class GoogleMailUtils {

    protected static final ModifiableObjectProperty SIMPLE_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID),
            string(THREAD_ID),
            number(HISTORY_ID),
            string(SUBJECT),
            string(FROM),
            array(TO).items(string()),
            array(CC).items(string()),
            array(BCC).items(string()),
            string("bodyPlain"),
            string("bodyHtml"),
            array(ATTACHMENTS).items(fileEntry()));

    protected static final ModifiableObjectProperty RAW_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(HISTORY_ID),
            string(ID),
            number(INTERNAL_DATE),
            array(LABEL_IDS)
                .items(string()),
            string(RAW),
            integer(SIZE_ESTIMATE),
            string(SNIPPET),
            string(THREAD_ID));

    protected static final ModifiableObjectProperty MINIMAL_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(HISTORY_ID),
            string(ID),
            number(INTERNAL_DATE),
            array(LABEL_IDS).items(string()),
            integer(SIZE_ESTIMATE),
            string(SNIPPET),
            string(THREAD_ID));

    protected static final ModifiableObjectProperty METADATA_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(HISTORY_ID),
            string(ID),
            number(INTERNAL_DATE),
            array(LABEL_IDS).items(string()),
            object(PAYLOAD)
                .properties(
                    array(HEADERS)
                        .items(
                            object()
                                .properties(
                                    string(NAME),
                                    string(VALUE)))),
            integer(SIZE_ESTIMATE),
            string(SNIPPET),
            string(THREAD_ID));

    private GoogleMailUtils() {
    }

    public static String getEncodedEmail(
        Parameters inputParameters, ActionContext actionContext, Message messageToReply)
        throws MessagingException, IOException {

        MimeMessage mimeMessage = getMimeMessage(inputParameters, messageToReply);

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

        return Base64.encodeBase64URLSafeString(rawMessageBytes);
    }

    private static MimeMessage getMimeMessage(Parameters inputParameters, Message messageToReply)
        throws MessagingException {

        Properties properties = new Properties();

        Session session = Session.getDefaultInstance(properties, null);

        MimeMessage mimeMessage = new MimeMessage(session);

        mimeMessage.setRecipients(
            jakarta.mail.Message.RecipientType.TO,
            InternetAddress.parse(String.join(",", inputParameters.getRequiredList(TO, String.class))));

        mimeMessage.setText(inputParameters.getRequiredString(BODY));
        mimeMessage.setRecipients(
            jakarta.mail.Message.RecipientType.CC,
            InternetAddress.parse(String.join(",", inputParameters.getList(CC, String.class, List.of()))));

        mimeMessage.setRecipients(
            jakarta.mail.Message.RecipientType.BCC,
            InternetAddress.parse(String.join(",", inputParameters.getList(BCC, String.class, List.of()))));
        mimeMessage.setReplyTo(
            InternetAddress.parse(String.join(",", inputParameters.getList(REPLY_TO, String.class, List.of()))));

        if (messageToReply == null) {
            mimeMessage.setSubject(inputParameters.getRequiredString(SUBJECT));
        } else {
            MessagePart payload = messageToReply.getPayload();

            List<MessagePartHeader> messagePartHeaders = payload.getHeaders();

            String messageID = "";
            String subject = "";
            for (MessagePartHeader messagePartHeader : messagePartHeaders) {
                if (messagePartHeader.getName()
                    .equals("Message-ID")) {
                    messageID = messagePartHeader.getValue();
                } else if (messagePartHeader.getName()
                    .equals("Subject")) {
                    subject = messagePartHeader.getValue();
                }
            }

            mimeMessage.setSubject(subject);
            mimeMessage.setHeader("In-Reply-To", messageID);
            mimeMessage.setHeader("References", messageID);
            mimeMessage.setHeader("Subject", subject);
        }

        return mimeMessage;
    }

    public static List<Option<String>> getLabelIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context)
        throws IOException {

        List<Option<String>> options = new ArrayList<>();

        List<Label> labels = GoogleServices.getMail(connectionParameters)
            .users()
            .labels()
            .list(ME)
            .execute()
            .getLabels();

        for (Label label : labels) {
            options.add(option(label.getName(), label.getName()));
        }

        return options;
    }

    public static Message getMessage(Parameters inputParameters, Gmail service) throws IOException {
        String format = inputParameters.getString(FORMAT);

        return service.users()
            .messages()
            .get(ME, inputParameters.getRequiredString(ID))
            .setFormat(format == null || format.equals(SIMPLE) ? FULL : format)
            .setMetadataHeaders(inputParameters.getList(METADATA_HEADERS, String.class, List.of()))
            .execute();
    }

    public static List<Option<String>> getMessageIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context)
        throws IOException {

        List<Message> messages = GoogleServices.getMail(connectionParameters)
            .users()
            .messages()
            .list(ME)
            .execute()
            .getMessages();

        List<Option<String>> options = new ArrayList<>();

        for (Message message : messages) {
            options.add(option(message.getId(), message.getId()));
        }

        return options;
    }

    public static ModifiableObjectProperty getMessageOutputProperty(String format) {
        return switch (format) {
            case SIMPLE -> SIMPLE_MESSAGE_OUTPUT_PROPERTY;
            case RAW -> RAW_MESSAGE_OUTPUT_PROPERTY;
            case MINIMAL -> MINIMAL_MESSAGE_OUTPUT_PROPERTY;
            case METADATA -> METADATA_MESSAGE_OUTPUT_PROPERTY;
            default -> FULL_MESSAGE_OUTPUT_PROPERTY;
        };
    }

    public static SimpleMessage getSimpleMessage(Message message, ActionContext actionContext, Gmail service)
        throws IOException {
        MessagePart payload = message.getPayload();
        List<MessagePart> parts = payload.getParts();

        MessagePart multipartAlternative = parts.stream()
            .filter(part -> part.getMimeType()
                .startsWith("multipart/alternative"))
            .findFirst()
            .orElse(null);

        List<MessagePart> messageParts = (multipartAlternative != null && multipartAlternative.getParts() != null)
            ? multipartAlternative.getParts() : parts;

        String bodyPlain = "";
        String bodyHtml = "";

        List<MessagePartHeader> messagePartHeaders = payload.getHeaders();

        String contentType = messagePartHeaders.stream()
            .filter(header -> Objects.equals(header.getName(), "Content-Type"))
            .map(MessagePartHeader::getValue)
            .findFirst()
            .orElse("text/plain");

        if (contentType.startsWith("multipart/")) {
            for (MessagePart messagePart : messageParts) {
                String mimeType = messagePart.getMimeType();

                if (mimeType.equals("text/plain")) {
                    bodyPlain = new String(messagePart.getBody()
                        .decodeData(), StandardCharsets.UTF_8);
                } else if (mimeType.equals("text/html")) {
                    bodyHtml = new String(messagePart.getBody()
                        .decodeData(), StandardCharsets.UTF_8);
                }
            }
        } else {
            MessagePartBody messagePartBody = payload.getBody();

            bodyPlain = new String(messagePartBody.decodeData(), StandardCharsets.UTF_8);
        }

        List<FileEntry> fileEntries = getFileEntries(message, actionContext, service);

        return createSimpleMessage(message, messagePartHeaders, bodyPlain, bodyHtml, fileEntries);
    }

    private static SimpleMessage createSimpleMessage(
        Message message, List<MessagePartHeader> messagePartHeaders, String bodyPlain, String bodyHtml,
        List<FileEntry> fileEntries) {

        String subject = null;
        String from = null;
        List<String> to = new ArrayList<>();
        List<String> cc = new ArrayList<>();
        List<String> bcc = new ArrayList<>();

        for (MessagePartHeader messagePartHeader : messagePartHeaders) {
            if ("Subject".equals(messagePartHeader.getName())) {
                subject = messagePartHeader.getValue();
            } else if ("From".equals(messagePartHeader.getName())) {
                from = messagePartHeader.getValue();
            } else if ("To".equals(messagePartHeader.getName())) {
                to = Arrays.stream(messagePartHeader.getValue()
                    .split(","))
                    .toList();
            } else if ("Cc".equals(messagePartHeader.getName())) {
                cc = Arrays.stream(messagePartHeader.getValue()
                    .split(","))
                    .toList();
            } else if ("Bcc".equals(messagePartHeader.getName())) {
                bcc = Arrays.stream(messagePartHeader.getValue()
                    .split(","))
                    .toList();
            }
        }

        return new SimpleMessage(
            message.getId(), message.getThreadId(), message.getHistoryId(), subject, from, to, cc, bcc, bodyPlain,
            bodyHtml, fileEntries);
    }

    private static List<FileEntry> getFileEntries(
        Message message, ActionContext actionContext, Gmail service)
        throws IOException {

        MessagePart payload = message.getPayload();
        List<MessagePart> parts = payload.getParts();

        List<FileEntry> fileEntries = new ArrayList<>();

        for (MessagePart messagePart : parts) {
            String mimeType = messagePart.getMimeType();

            if (!mimeType.startsWith("multipart/alternative") &&
                !mimeType.startsWith("text/plain") &&
                !mimeType.startsWith("text/html")) {
                MessagePartBody messagePartBody = messagePart.getBody();

                MessagePartBody attachment = getAttachment(service, message.getId(),
                    messagePartBody.getAttachmentId());

                fileEntries.add(actionContext.file(
                    file -> file.storeContent(messagePart.getFilename(), attachment.getData())));
            }
        }
        return fileEntries;
    }

    private static MessagePartBody getAttachment(Gmail service, String messageId, String attachmentId)
        throws IOException {

        return service.users()
            .messages()
            .attachments()
            .get(ME, messageId, attachmentId)
            .execute();
    }

    public static List<Option<String>> getThreadIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) throws IOException {

        List<Thread> threads = GoogleServices.getMail(connectionParameters)
            .users()
            .threads()
            .list(ME)
            .execute()
            .getThreads();

        List<Option<String>> options = new ArrayList<>();

        for (Thread thread : threads) {
            options.add(option(thread.getId(), thread.getId()));
        }

        return options;
    }

    public static Message sendMail(Gmail service, Message message) throws IOException {
        return service
            .users()
            .messages()
            .send(ME, message)
            .execute();
    }

    @SuppressFBWarnings("EI")
    public record SimpleMessage(
        String id, String threadId, BigInteger historyId, String subject, String from, List<String> to,
        List<String> cc, List<String> bcc, String bodyPlain, String bodyHtml,
        List<FileEntry> attachments) {
    }
}
