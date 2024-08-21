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
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ATTACHMENTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FROM;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.GET_MAIL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MINIMAL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.PARSED;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.RAW;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Monika Kušter
 */
public class GoogleMailGetMailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_MAIL)
        .title("Get Mail")
        .description("Get an email from your Gmail account via Id")
        .properties(
            string(ID)
                .label("Message ID")
                .description("The ID of the message to retrieve.")
                .options((ActionOptionsFunction<String>) GoogleMailUtils::getMessageIdOptions)
                .required(true),
            string(FORMAT)
                .label("Format")
                .description("The format to return the message in.")
                .options(
                    option("Parsed", PARSED, "Returns email message's from, to, subject, body and attachments."),
                    option("Minimal", MINIMAL,
                        "Returns only email message ID and labels; does not return the email headers, body, or payload."),
                    option("Full", FULL,
                        "Returns the full email message data with body content parsed in the payload field; the raw field is not used. Format cannot be used when accessing the api using the gmail.metadata scope."),
                    option("Raw", RAW,
                        "Returns the full email message data with body content in the raw field as a base64url encoded string; the payload field is not used. Format cannot be used when accessing the api using the gmail.metadata scope."),
                    option("Metadata", METADATA, "Returns only email message ID, labels, and email headers."))
                .defaultValue(PARSED)
                .required(true),
            METADATA_HEADERS_PROPERTY)
        .output(GoogleMailUtils.getOutput())
        .perform(GoogleMailGetMailAction::perform);

    private GoogleMailGetMailAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {
        Gmail service = GoogleServices.getMail(connectionParameters);

        String format = inputParameters.getRequiredString(FORMAT);

        if (format.equals(PARSED)) {
            Message message = getMessage(inputParameters, service);
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

            List<FileEntry> fileEntries = getFileEntries(inputParameters, actionContext, parts, service);

            return createResponseMap(messagePartHeaders, bodyPlain, bodyHtml, fileEntries);
        } else {
            return getMessage(inputParameters, service);
        }
    }

    private static Message getMessage(Parameters inputParameters, Gmail service) throws IOException {
        String format = inputParameters.getRequiredString(FORMAT);

        return service.users()
            .messages()
            .get("me", inputParameters.getRequiredString(ID))
            .setFormat(format.equals(PARSED) ? FULL : format)
            .setMetadataHeaders(inputParameters.getList(METADATA_HEADERS, String.class, List.of()))
            .execute();
    }

    private static List<FileEntry> getFileEntries(
        Parameters inputParameters, ActionContext actionContext, List<MessagePart> parts, Gmail service)
        throws IOException {

        List<FileEntry> fileEntries = new ArrayList<>();

        for (MessagePart messagePart : parts) {
            String mimeType = messagePart.getMimeType();

            if (!mimeType.startsWith("multipart/alternative") &&
                !mimeType.startsWith("text/plain") &&
                !mimeType.startsWith("text/html")) {
                MessagePartBody messagePartBody = messagePart.getBody();

                MessagePartBody attachment = getAttachment(service, inputParameters.getRequiredString(ID),
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
            .get("me", messageId, attachmentId)
            .execute();
    }

    private static Map<String, Object> createResponseMap(
        List<MessagePartHeader> messagePartHeaders, String bodyPlain, String bodyHtml, List<FileEntry> fileEntries) {

        Map<String, Object> responseMap = new LinkedHashMap<>();

        responseMap.put(SUBJECT, "");
        responseMap.put(FROM, "");
        responseMap.put(TO, "");

        for (MessagePartHeader messagePartHeader : messagePartHeaders) {
            if ("Subject".equals(messagePartHeader.getName())) {
                responseMap.put(SUBJECT, messagePartHeader.getValue());
            } else if ("From".equals(messagePartHeader.getName())) {
                responseMap.put(FROM, messagePartHeader.getValue());
            } else if ("To".equals(messagePartHeader.getName())) {
                responseMap.put(TO, messagePartHeader.getValue());
            }
        }

        responseMap.put("body_plain", bodyPlain);
        responseMap.put("body_html", bodyHtml);
        responseMap.put(ATTACHMENTS, fileEntries);

        return responseMap;
    }

}
