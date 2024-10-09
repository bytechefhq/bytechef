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

package com.bytechef.component.google.mail.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Property;

/**
 * @author Monika Kušter
 */
public class GoogleMailConstants {

    private GoogleMailConstants() {
    }

    public static final String ATTACHMENTS = "attachments";
    public static final String BCC = "bcc";
    public static final String BODY = "body";
    public static final String CATEGORY = "category";
    public static final String CC = "cc";
    public static final String FORMAT = "format";
    public static final String FROM = "from";
    public static final String FULL = "full";
    public static final String HEADERS = "headers";
    public static final String HISTORY_ID = "historyId";
    public static final String ID = "id";
    public static final String INCLUDE_SPAM_TRASH = "includeSpamTrash";
    public static final String INTERNAL_DATE = "internalDate";
    public static final String LABEL = "label";
    public static final String LABEL_IDS = "labelIds";
    public static final String MAX_RESULTS = "maxResults";
    public static final String ME = "me";
    public static final String MESSAGES = "messages";
    public static final String METADATA = "metadata";
    public static final String METADATA_HEADERS = "metadataHeaders";
    public static final String MINIMAL = "minimal";
    public static final String NAME = "name";
    public static final String NEW_EMAIL = "newEmail";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String PAGE_TOKEN = "pageToken";
    public static final String PAYLOAD = "payload";
    public static final String RAW = "raw";
    public static final String REPLY_TO = "replyTo";
    public static final String RESULT_SIZE_ESTIMATE = "resultSizeEstimate";
    public static final String SIMPLE = "simple";
    public static final String SIZE_ESTIMATE = "sizeEstimate";
    public static final String SNIPPET = "snippet";
    public static final String SUBJECT = "subject";
    public static final String THREAD_ID = "threadId";
    public static final String TO = "to";
    public static final String TOPIC_NAME = "topicName";
    public static final String VALUE = "value";

    public static final ModifiableStringProperty EMAIL_PROPERTY = string("email")
        .label("Email address")
        .controlType(Property.ControlType.EMAIL);

    public static final ModifiableStringProperty FORMAT_PROPERTY = string(FORMAT)
        .label("Format")
        .description("The format to return the message in.")
        .options(
            option("Simple", SIMPLE, "Returns email message's from, to, subject, body and attachments."),
            option("Minimal", MINIMAL,
                "Returns only email message ID and labels; does not return the email headers, body, or payload."),
            option("Full", FULL,
                "Returns the full email message data with body content parsed in the payload field; the raw field is not used. Format cannot be used when accessing the api using the gmail.metadata scope."),
            option("Raw", RAW,
                "Returns the full email message data with body content in the raw field as a base64url encoded string; the payload field is not used. Format cannot be used when accessing the api using the gmail.metadata scope."),
            option("Metadata", "metadata", "Returns only email message ID, labels, and email headers."))
        .defaultValue(SIMPLE)
        .required(false);

    public static final ModifiableObjectProperty FULL_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID),
            string(THREAD_ID),
            array(LABEL_IDS)
                .items(string()),
            string(SNIPPET),
            string(HISTORY_ID),
            number(INTERNAL_DATE),
            object(PAYLOAD)
                .properties(
                    string("partId"),
                    string("mimeType"),
                    string("filename"),
                    array(HEADERS)
                        .items(
                            object()
                                .properties(
                                    string(NAME),
                                    string(VALUE))),
                    object(BODY)
                        .properties(
                            string("attachmentId"),
                            integer("size"),
                            string("data")),
                    array("parts")
                        .items()),
            integer(SIZE_ESTIMATE),
            string(RAW));

    public static final ModifiableArrayProperty METADATA_HEADERS_PROPERTY = array(METADATA_HEADERS)
        .label("Metadata headers")
        .description("When given and format is METADATA, only include headers specified.")
        .items(string())
        .required(false);
}
