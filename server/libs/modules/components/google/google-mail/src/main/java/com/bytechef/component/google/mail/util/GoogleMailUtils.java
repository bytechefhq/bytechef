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

package com.bytechef.component.google.mail.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ATTACHMENTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BCC;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BODY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BODY_TYPE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.CC;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FROM;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.HISTORY_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.INTERNAL_DATE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL_IDS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.NAME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.PAYLOAD;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.RAW;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.REPLY_TO;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SIZE_ESTIMATE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SNIPPET;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.THREAD_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.VALUE;
import static com.bytechef.component.google.mail.definition.Format.FULL;
import static com.bytechef.component.google.mail.definition.Format.SIMPLE;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.definition.Format;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
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
import java.io.ByteArrayInputStream;
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
            string(ID)
                .description("The ID of the message."),
            string(THREAD_ID)
                .description("The ID of the thread the message belongs to."),
            string(HISTORY_ID)
                .description("The ID of the last history record that modified this message."),
            string(SUBJECT)
                .description("The subject of the message."),
            string(FROM)
                .description("The sender of the message."),
            array(TO)
                .description("List of recipients of the message.")
                .items(string()),
            array(CC)
                .description("List of CC recipients of the message.")
                .items(string()),
            array(BCC)
                .description("List of BCC recipients of the message.")
                .items(string()),
            string("bodyPlain")
                .description("The plain text body of the message."),
            string("bodyHtml")
                .description("The HTML body of the message."),
            array(ATTACHMENTS)
                .description("List of attachments of the message.")
                .items(fileEntry()));

    protected static final ModifiableObjectProperty RAW_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(HISTORY_ID)
                .description("The ID of the last history record that modified this message."),
            string(ID)
                .description("The ID of the message."),
            number(INTERNAL_DATE)
                .description(
                    "The internal message creation timestamp (epoch ms), which determines ordering in the inbox."),
            array(LABEL_IDS)
                .description("List of IDs of labels applied to this message.")
                .items(string()),
            string(RAW)
                .description("The entire email message in an RFC 2822 formatted and base64url encoded string."),
            integer(SIZE_ESTIMATE)
                .description("Estimated size in bytes of the message."),
            string(SNIPPET)
                .description("A short part of the message text."),
            string(THREAD_ID)
                .description("The ID of the thread the message belongs to."));

    protected static final ModifiableObjectProperty MINIMAL_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(HISTORY_ID)
                .description("The ID of the last history record that modified this message."),
            string(ID)
                .description("The ID of the message."),
            number(INTERNAL_DATE)
                .description(
                    "The internal message creation timestamp (epoch ms), which determines ordering in the inbox."),
            array(LABEL_IDS)
                .description("List of IDs of labels applied to this message.")
                .items(string()),
            integer(SIZE_ESTIMATE)
                .description("Estimated size in bytes of the message."),
            string(SNIPPET)
                .description("A short part of the message text."),
            string(THREAD_ID)
                .description("The ID of the thread the message belongs to."));

    protected static final ModifiableObjectProperty METADATA_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(HISTORY_ID)
                .description("A short part of the message text."),
            string(ID)
                .description("The ID of the message."),
            number(INTERNAL_DATE)
                .description(
                    "The internal message creation timestamp (epoch ms), which determines ordering in the inbox."),
            array(LABEL_IDS)
                .description("List of IDs of labels applied to this message.")
                .items(string()),
            object(PAYLOAD)
                .description("The parsed email structure in the message parts.")
                .properties(
                    array(HEADERS)
                        .description("List of headers on the message part.")
                        .items(
                            object()
                                .properties(
                                    string(NAME)
                                        .description("The name of the header before the : separator."),
                                    string(VALUE)
                                        .description("The value of the header after the : separator."))),
                    integer(SIZE_ESTIMATE)
                        .description("Estimated size in bytes of the message."),
                    string(SNIPPET)
                        .description("A short part of the message text."),
                    string(THREAD_ID)
                        .description("The ID of the thread the message belongs to.")));

    private GoogleMailUtils() {
    }

    public static String getEncodedEmail(Parameters inputParameters, Context context, Message messageToReply)
        throws MessagingException, IOException {

        MimeMessage mimeMessage = getMimeMessage(inputParameters, messageToReply);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();

        mimeBodyPart.setText(
            inputParameters.getRequiredString(BODY), StandardCharsets.UTF_8.name(),
            inputParameters.getRequiredString(BODY_TYPE));

        Multipart multipart = new MimeMultipart();

        multipart.addBodyPart(mimeBodyPart);

        for (FileEntry fileEntry : inputParameters.getFileEntries(ATTACHMENTS, List.of())) {
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();

            attachmentBodyPart.setDataHandler(
                new DataHandler(
                    new ByteArrayDataSource(
                        (InputStream) context.file(file -> file.getInputStream(fileEntry)), fileEntry.getMimeType())));
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

        mimeMessage.setRecipients(
            jakarta.mail.Message.RecipientType.CC,
            InternetAddress.parse(String.join(",", inputParameters.getList(CC, String.class, List.of()))));

        mimeMessage.setRecipients(
            jakarta.mail.Message.RecipientType.BCC,
            InternetAddress.parse(String.join(",", inputParameters.getList(BCC, String.class, List.of()))));
        mimeMessage.setReplyTo(
            InternetAddress.parse(String.join(",", inputParameters.getList(REPLY_TO, String.class, List.of()))));

        if (messageToReply == null) {
            mimeMessage.setSubject(inputParameters.getRequiredString(SUBJECT), StandardCharsets.UTF_8.name());
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

            mimeMessage.setSubject(subject, StandardCharsets.UTF_8.name());
            mimeMessage.setHeader("In-Reply-To", messageID);
            mimeMessage.setHeader("References", messageID);
        }

        return mimeMessage;
    }

    public static List<Option<String>> getLabelOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext context) {

        List<Option<String>> options = new ArrayList<>();

        List<Label> labels = null;
        try {
            labels = GoogleServices.getMail(connectionParameters)
                .users()
                .labels()
                .list(ME)
                .execute()
                .getLabels();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }

        for (Label label : labels) {
            options.add(option(label.getName(), label.getId()));
        }

        return options;
    }

    public static Message getMessage(Parameters inputParameters, Gmail service) {
        Format format = inputParameters.get(FORMAT, Format.class, SIMPLE);

        try {
            return service.users()
                .messages()
                .get(ME, inputParameters.getRequiredString(ID))
                .setFormat(format == null || format == SIMPLE ? FULL.getMapping() : format.getMapping())
                .setMetadataHeaders(inputParameters.getList(METADATA_HEADERS, String.class, List.of()))
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }

    public static OutputResponse getMessageOutput(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return OutputResponse.of(getMessageOutputProperty(inputParameters.get(FORMAT, Format.class, SIMPLE)));
    }

    public static ModifiableObjectProperty getMessageOutputProperty(Format format) {
        return switch (format) {
            case SIMPLE -> SIMPLE_MESSAGE_OUTPUT_PROPERTY;
            case RAW -> RAW_MESSAGE_OUTPUT_PROPERTY;
            case MINIMAL -> MINIMAL_MESSAGE_OUTPUT_PROPERTY;
            case METADATA -> METADATA_MESSAGE_OUTPUT_PROPERTY;
            default -> FULL_MESSAGE_OUTPUT_PROPERTY;
        };
    }

    public static SimpleMessage getSimpleMessage(Message message, Context context, Gmail service) {
        MessagePart payload = message.getPayload();

        List<MessagePart> parts = payload.getParts();

        MessagePart multipartAlternative = null;

        if (parts != null) {
            multipartAlternative = parts.stream()
                .filter(part -> part.getMimeType()
                    .startsWith("multipart/alternative"))
                .findFirst()
                .orElse(null);
        }

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

            if (messagePartBody.decodeData() != null) {
                bodyPlain = new String(messagePartBody.decodeData(), StandardCharsets.UTF_8);
            }
        }

        List<FileEntry> fileEntries = getFileEntries(message, context, service);

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
            String value = messagePartHeader.getValue();

            if ("Subject".equals(messagePartHeader.getName())) {
                subject = value;
            } else if ("From".equals(messagePartHeader.getName())) {
                from = value;
            } else if ("To".equals(messagePartHeader.getName())) {
                to = Arrays.stream(value.split(","))
                    .toList();
            } else if ("Cc".equals(messagePartHeader.getName())) {
                cc = Arrays.stream(value.split(","))
                    .toList();
            } else if ("Bcc".equals(messagePartHeader.getName())) {
                bcc = Arrays.stream(value.split(","))
                    .toList();
            }
        }

        return new SimpleMessage(
            message.getId(), message.getThreadId(), message.getHistoryId(), subject, from, to, cc, bcc, bodyPlain,
            bodyHtml, fileEntries, "https://mail.google.com/mail/u/0/#all/" + message.getId());
    }

    private static List<FileEntry> getFileEntries(Message message, Context context, Gmail service) {
        MessagePart payload = message.getPayload();
        List<MessagePart> parts = payload.getParts();

        if (parts == null) {
            return List.of();
        }

        List<FileEntry> fileEntries = new ArrayList<>();

        for (MessagePart messagePart : parts) {
            String mimeType = messagePart.getMimeType();

            if (!mimeType.startsWith("multipart/alternative") && !mimeType.startsWith("text/plain") &&
                !mimeType.startsWith("text/html")) {

                MessagePartBody messagePartBody = messagePart.getBody();

                if (messagePartBody.getAttachmentId() == null) {
                    continue;
                }

                MessagePartBody attachment = getAttachment(service, message.getId(), messagePartBody.getAttachmentId());

                fileEntries.add(
                    context.file(
                        file -> file.storeContent(
                            messagePart.getFilename(), new ByteArrayInputStream(attachment.decodeData()))));
            }
        }

        return fileEntries;
    }

    private static MessagePartBody getAttachment(Gmail service, String messageId, String attachmentId) {
        try {
            return service.users()
                .messages()
                .attachments()
                .get(ME, messageId, attachmentId)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }

    public static Message sendMail(Gmail service, Message message) {
        try {
            return service.users()
                .messages()
                .send(ME, message)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }

    @SuppressFBWarnings("EI")
    public record SimpleMessage(
        String id, String threadId, BigInteger historyId, String subject, String from, List<String> to,
        List<String> cc, List<String> bcc, String bodyPlain, String bodyHtml,
        List<FileEntry> attachments, String webLink) {
    }
}
