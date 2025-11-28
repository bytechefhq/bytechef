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

package com.bytechef.component.google.mail.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.mail.definition.Format.FULL;
import static com.bytechef.component.google.mail.definition.Format.MINIMAL;
import static com.bytechef.component.google.mail.definition.Format.SIMPLE;

import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.google.mail.definition.Format;

/**
 * @author Monika Kušter
 */
public class GoogleMailConstants {

    private GoogleMailConstants() {
    }

    public static final String ATTACHMENTS = "attachments";
    public static final String BCC = "bcc";
    public static final String BODY = "body";
    public static final String BODY_TYPE = "bodyType";
    public static final String CATEGORY = "category";
    public static final String CC = "cc";
    public static final String FORMAT = "format";
    public static final String FROM = "from";
    public static final String HEADERS = "headers";
    public static final String HISTORY_ID = "historyId";
    public static final String ID = "id";
    public static final String INCLUDE_SPAM_TRASH = "includeSpamTrash";
    public static final String INTERNAL_DATE = "internalDate";
    public static final String LABEL_IDS = "labelIds";
    public static final String MAX_RESULTS = "maxResults";
    public static final String ME = "me";
    public static final String MESSAGES = "messages";
    public static final String METADATA_HEADERS = "metadataHeaders";
    public static final String NAME = "name";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String PAGE_TOKEN = "pageToken";
    public static final String PAYLOAD = "payload";
    public static final String RAW = "raw";
    public static final String REPLY_TO = "replyTo";
    public static final String RESULT_SIZE_ESTIMATE = "resultSizeEstimate";
    public static final String SIZE_ESTIMATE = "sizeEstimate";
    public static final String SNIPPET = "snippet";
    public static final String SUBJECT = "subject";
    public static final String THREAD_ID = "threadId";
    public static final String TO = "to";
    public static final String TOPIC_NAME = "topicName";
    public static final String VALUE = "value";

    public static final ModifiableStringProperty BODY_HTML_PROPERTY = string(BODY)
        .label("Body - HTML")
        .description("The body of the message in HTML format.")
        .controlType(ControlType.RICH_TEXT)
        .displayCondition("bodyType == '%s'".formatted("html"))
        .required(true);

    public static final ModifiableStringProperty BODY_TEXT_PROPERTY = string(BODY)
        .label("Body - Text")
        .description("The body of the message in text format.")
        .controlType(ControlType.TEXT_AREA)
        .displayCondition("bodyType == '%s'".formatted("plain"))
        .required(true);

    public static final ModifiableStringProperty BODY_TYPE_PROPERTY = string(BODY_TYPE)
        .label("Body Type")
        .options(
            option("Text", "plain"),
            option("HTML", "html"))
        .defaultValue("plain")
        .required(true);

    public static final ModifiableStringProperty EMAIL_PROPERTY = string()
        .label("Email address")
        .controlType(ControlType.EMAIL);

    public static final ModifiableStringProperty FORMAT_PROPERTY = string(FORMAT)
        .label("Format")
        .description("The format to return the message in.")
        .options(
            option("Simple", SIMPLE.name(), "Returns email message's from, to, subject, body and attachments."),
            option("Minimal", MINIMAL.name(),
                "Returns only email message ID and labels; does not return the email headers, body, or payload."),
            option("Full", FULL.name(),
                "Returns the full email message data with body content parsed in the payload field; the raw field is not used. Format cannot be used when accessing the api using the gmail.metadata scope."),
            option("Raw", Format.RAW.name(),
                "Returns the full email message data with body content in the raw field as a base64url encoded string; the payload field is not used. Format cannot be used when accessing the api using the gmail.metadata scope."),
            option("Metadata", "metadata", "Returns only email message ID, labels, and email headers."))
        .defaultValue(SIMPLE.name())
        .required(false);

    public static final ModifiableObjectProperty FULL_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The ID of the message."),
            string(THREAD_ID)
                .description("The ID of the thread the message belongs to."),
            array(LABEL_IDS)
                .description("List of IDs of labels applied to this message.")
                .items(string()),
            string(SNIPPET)
                .description("A short part of the message text."),
            string(HISTORY_ID)
                .description("The ID of the last history record that modified this message."),
            number(INTERNAL_DATE)
                .description(
                    "The internal message creation timestamp (epoch ms), which determines ordering in the inbox."),
            object(PAYLOAD)
                .description("The parsed email structure in the message parts.")
                .properties(
                    string("partId")
                        .description("The ID of the message part."),
                    string("mimeType")
                        .description("The MIME type of the message part."),
                    string("filename")
                        .description("The filename of the attachment."),
                    array(HEADERS)
                        .description("List of headers on the message part.")
                        .items(
                            object()
                                .properties(
                                    string(NAME)
                                        .description("The name of the header before the : separator."),
                                    string(VALUE)
                                        .description("The value of the header after the : separator."))),
                    object(BODY)
                        .description("The message part body for this part.")
                        .properties(
                            string("attachmentId")
                                .description(
                                    "ID of an external attachment that can be retrieved in a separate " +
                                        "messages.attachments.get request."),
                            integer("size")
                                .description("Number of bytes for the message part data (encoding notwithstanding)."),
                            string("data")
                                .description("The body data of a MIME message part as a base64url encoded string.")),
                    array("parts")
                        .description("The child MIME message parts of this part.")
                        .items()),
            integer(SIZE_ESTIMATE)
                .description("Estimated size in bytes of the message."),
            string(Format.RAW.name())
                .description("The entire email message in an RFC 2822 formatted and base64url encoded string."));

    public static final ModifiableArrayProperty METADATA_HEADERS_PROPERTY = array(METADATA_HEADERS)
        .label("Metadata headers")
        .description("When given and format is METADATA, only include headers specified.")
        .items(string())
        .required(false);
}
