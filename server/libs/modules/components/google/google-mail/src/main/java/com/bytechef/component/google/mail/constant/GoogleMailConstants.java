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

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;

/**
 * @author Monika Domiter
 */
public class GoogleMailConstants {

    private GoogleMailConstants() {
    }

    public static final String ATTACHMENTS = "attachments";
    public static final String ATTACHMENT_ID = "attachmentId";
    public static final String BCC = "bcc";
    public static final String BODY = "body";
    public static final String CC = "cc";
    public static final String DATA = "data";
    public static final String EMAIL = "email";
    public static final String EMAIL_ADDRESS = "Email address";
    public static final String FILENAME = "filename";
    public static final String FORMAT = "format";
    public static final String FROM = "from";
    public static final String GET_MAIL = "getMail";
    public static final String GET_THREAD = "getThread";
    public static final String GOOGLE_MAIL = "googleMail";
    public static final String HEADERS = "headers";
    public static final String HISTORY_ID = "historyId";
    public static final String ID = "id";
    public static final String INCLUDE_SPAM_TRASH = "includeSpamTrash";
    public static final String INTERNAL_DATE = "internalDate";
    public static final String LABEL_IDS = "labelIds";
    public static final String MAX_RESULTS = "maxResults";
    public static final String MESSAGES = "messages";
    public static final String METADATA_HEADERS = "metadataHeaders";
    public static final String MIME_TYPE = "mimeType";
    public static final String NAME = "name";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String PAGE_TOKEN = "pageToken";
    public static final String PARTS = "parts";
    public static final String PART_ID = "partId";
    public static final String PAYLOAD = "payload";
    public static final String Q = "q";
    public static final String RAW = "raw";
    public static final String REPLY_TO = "replyTo";
    public static final String RESULT_SIZE_ESTIMATE = "resultSizeEstimate";
    public static final String SEARCH_EMAIL = "searchEmail";
    public static final String SEND_EMAIL = "sendEmail";
    public static final String SIZE = "size";
    public static final String SIZE_ESTIMATE = "sizeEstimate";
    public static final String SNIPPET = "snippet";
    public static final String SUBJECT = "subject";
    public static final String THREAD_ID = "threadId";
    public static final String TO = "to";
    public static final String VALUE = "value";

    public static final ComponentDSL.ModifiableStringProperty FORMAT_PROPERTY =
        string(FORMAT)
            .label("Format")
            .description("The format to return the message in.")
            .options(
                option("Minimal", "minimal",
                    "Returns only email message ID and labels; does not return the email headers, body, or payload."),
                option("Full", "full",
                    "Returns the full email message data with body content parsed in the payload field; the raw field is not used. Format cannot be used when accessing the api using the gmail.metadata scope."),
                option("Raw", "raw",
                    "Returns the full email message data with body content in the raw field as a base64url encoded string; the payload field is not used. Format cannot be used when accessing the api using the gmail.metadata scope."),
                option("Metadata", "metadata", "Returns only email message ID, labels, and email headers."))
            .required(false);

    public static final ModifiableObjectProperty MESSAGE_PROPERTY =
        object()
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
                        string(PART_ID),
                        string(MIME_TYPE),
                        string(FILENAME),
                        array(HEADERS)
                            .items(
                                string(NAME),
                                string(VALUE)),
                        object(BODY)
                            .properties(
                                string(ATTACHMENT_ID),
                                integer(SIZE),
                                string(DATA)),
                        array(PARTS)
                            .items()),
                integer(SIZE_ESTIMATE),
                string(RAW));
}
