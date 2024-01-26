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

package com.bytechef.component.microsoft.outlook.constant;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableDateTimeProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;

/**
 * @author Monika Domiter
 */
public class MicrosoftOutlook365Constants {

    private MicrosoftOutlook365Constants() {
    }

    public static final String MICROSOFT_OUTLOOK_365 = "microsoftOutlook365";
    public static final String ADDRESS = "address";
    public static final String BCC_RECIPIENTS = "bccRecipients";
    public static final String BODY = "body";
    public static final String BODY_PREVIEW = "bodyPreview";
    public static final String CC_RECIPIENTS = "ccRecipients";
    public static final String COMPLETED_DATE_TIME = "completedDateTime";
    public static final String CONTENT = "content";
    public static final String CONTENT_TYPE = "contentType";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String DATE_TIME = "dateTime";
    public static final String DUE_DATE_TIME = "dueDateTime";
    public static final String EMAIL_ADDRESS = "emailAddress";
    public static final String FLAG = "flag";
    public static final String FLAG_STATUS = "flagStatus";
    public static final String FROM = "from";
    public static final String GET_MAIL = "getMail";
    public static final String HAS_ATTACHMENTS = "hasAttachments";
    public static final String ID = "id";
    public static final String IMPORTANCE = "importance";
    public static final String INFERENCE_CLASSIFICATION = "inferenceClassification";
    public static final String INTERNET_MESSAGE_HEADERS = "internetMessageHeaders";
    public static final String INTERNET_MESSAGE_ID = "internetMessageId";
    public static final String IS_DELIVERY_RECEIPT_REQUESTED = "isDeliveryReceiptRequested";
    public static final String IS_DRAFT = "isDraft";
    public static final String IS_READ = "isRead";
    public static final String IS_READ_RECEIPT_REQUESTED = "isReadReceiptRequested";
    public static final String MESSAGE = "message";
    public static final String NAME = "name";
    public static final String PARENT_FOLDER_ID = "parentFolderId";
    public static final String RECIPIENT = "recipient";
    public static final String RECEIVED_DATE_TIME = "receivedDateTime";
    public static final String REPLY_TO = "replyTo";
    public static final String SAVE_TO_SENT_ITEMS = "saveToSentItems";
    public static final String SEARCH_EMAIL = "searchEmail";
    public static final String SEND_EMAIL = "sendEmail";
    public static final String SENDER = "sender";
    public static final String SENT_DATE_TIME = "sentDateTime";
    public static final String START_DATE_TIME = "startDateTime";
    public static final String SUBJECT = "subject";
    public static final String TIME_ZONE = "timeZone";
    public static final String TO_RECIPIENTS = "toRecipients";
    public static final String UNIQUE_BODY = "uniqueBody";
    public static final String VALUE = "value";
    public static final String WEB_LINK = "webLink";
    public static final String ZONE_ID = "zoneId";
    public static final ModifiableStringProperty CONTENT_PROPERTY = string(CONTENT)
            .label("Content")
            .description("The content of the item.")
            .required(false);

    public static final ModifiableStringProperty CONTENT_TYPE_PROPERTY = string(CONTENT_TYPE)
            .label("Content type")
            .description("The type of the content.")
            .options(
                option("Text", "TEXT"),
                option("Html", "HTML"))
            .required(false);

    public static final ModifiableDateTimeProperty DATE_TIME_PROPERTY = dateTime(DATE_TIME)
            .label("Date Time")
            .description(
                "A single point of time in a combined date and time representation.")
            .required(false);

    public static final ModifiableObjectProperty RECIPIENT_PROPERTY = object(RECIPIENT)
            .label("Recipient")
            .properties(
                object(EMAIL_ADDRESS)
                    .properties(
                        string(ADDRESS)
                            .label("Address")
                            .description("The email address of the person or entity.")
                            .required(false),
                        string(NAME)
                            .label("Name")
                            .description("The display name of the person or entity.")
                            .required(false)));

    public static final ModifiableStringProperty TIME_ZONE_PROPERTY = string(TIME_ZONE)
            .label("Time Zone")
            .description("Represents a time zone, for example, 'Pacific Standard Time'.")
            .required(false);

    public static final ModifiableObjectProperty MESSAGE_OUTPUT_PROPERTY = object()
            .properties(
                array(BCC_RECIPIENTS)
                    .items(
                        object(RECIPIENT)
                            .properties(
                                object(EMAIL_ADDRESS)
                                    .properties(
                                        string(ADDRESS),
                                        string(NAME)))),
                object(BODY)
                    .properties(
                        string(CONTENT),
                        string(CONTENT_TYPE)),
                string(BODY_PREVIEW),
                array(CC_RECIPIENTS)
                    .items(
                        object(RECIPIENT)
                            .properties(
                                object(EMAIL_ADDRESS)
                                    .properties(
                                        string(ADDRESS),
                                        string(NAME)))),
                string(CONVERSATION_ID),
                object(FLAG)
                    .properties(
                        object(COMPLETED_DATE_TIME)
                            .properties(
                                dateTime(DATE_TIME),
                                string(TIME_ZONE)),
                        object(DUE_DATE_TIME)
                            .properties(
                                dateTime(DATE_TIME),
                                string(TIME_ZONE)),
                        string(FLAG_STATUS),
                        object(START_DATE_TIME)
                            .properties(
                                dateTime(DATE_TIME),
                                string(TIME_ZONE))),
                object(FROM)
                    .properties(
                        object(RECIPIENT)
                            .properties(
                                object(EMAIL_ADDRESS)
                                    .properties(
                                        string(ADDRESS),
                                        string(NAME)))),
                bool(HAS_ATTACHMENTS),
                string(IMPORTANCE),
                string(INFERENCE_CLASSIFICATION),
                array(INTERNET_MESSAGE_HEADERS)
                    .items(
                        object()
                            .properties(
                                string(NAME),
                                string(VALUE))),
                string(INTERNET_MESSAGE_ID),
                bool(IS_DELIVERY_RECEIPT_REQUESTED),
                bool(IS_DRAFT),
                bool(IS_READ),
                bool(IS_READ_RECEIPT_REQUESTED),
                string(PARENT_FOLDER_ID),
                object(RECEIVED_DATE_TIME)
                    .properties(
                        dateTime(DATE_TIME),
                        string(ZONE_ID)),
                array(REPLY_TO)
                    .items(
                        object(RECIPIENT)
                            .properties(
                                object(EMAIL_ADDRESS)
                                    .properties(
                                        string(ADDRESS),
                                        string(NAME)))),
                object(SENDER)
                    .properties(
                        object(RECIPIENT)
                            .properties(
                                object(EMAIL_ADDRESS)
                                    .properties(
                                        string(ADDRESS),
                                        string(NAME))),
                        string(SUBJECT),
                        array(TO_RECIPIENTS)
                            .items(
                                object(RECIPIENT)
                                    .properties(
                                        object(EMAIL_ADDRESS)
                                            .properties(
                                                string(ADDRESS),
                                                string(NAME)))),
                        object(UNIQUE_BODY)
                            .properties(
                                string(CONTENT),
                                string(CONTENT_TYPE)),
                        string(WEB_LINK)));
}
