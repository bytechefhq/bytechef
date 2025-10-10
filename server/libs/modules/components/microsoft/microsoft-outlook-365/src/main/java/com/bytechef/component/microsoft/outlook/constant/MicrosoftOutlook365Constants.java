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

package com.bytechef.component.microsoft.outlook.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.outlook.definition.Format.FULL;
import static com.bytechef.component.microsoft.outlook.definition.Format.SIMPLE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365OptionUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365Constants {

    public static final String ADDRESS = "address";
    public static final String ALL_DAY = "allDay";
    public static final String ATTACHMENTS = "attachments";
    public static final String ATTENDEES = "attendees";
    public static final String BCC_RECIPIENTS = "bccRecipients";
    public static final String BODY = "body";
    public static final String CALENDAR = "calendar";
    public static final String CATEGORY = "category";
    public static final String CC_RECIPIENTS = "ccRecipients";
    public static final String COMMENT = "comment";
    public static final String CONTENT = "content";
    public static final String CONTENT_BYTES = "contentBytes";
    public static final String CONTENT_TYPE = "contentType";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String DATE_RANGE = "dateRange";
    public static final String DATE_TIME = "dateTime";
    public static final String DISPLAY_NAME = "displayName";
    public static final String END = "end";
    public static final String EMAIL_ADDRESS = "emailAddress";
    public static final String EVENT = "event";
    public static final String DESTINATION_ID = "destinationId";
    public static final String FORMAT = "format";
    public static final String FROM = "from";
    public static final String I_CAL_UID = "iCalUId";
    public static final String ID = "id";
    public static final String IS_ONLINE_MEETING = "isOnlineMeeting";
    public static final String NAME = "name";
    public static final String ODATA_NEXT_LINK = "@odata.nextLink";
    public static final String REMINDER_MINUTES_BEFORE_START = "reminderMinutesBeforeStart";
    public static final String REPLY_TO = "replyTo";
    public static final String START = "start";
    public static final String SUBJECT = "subject";
    public static final String TIME_ZONE = "timeZone";
    public static final String TO = "to";
    public static final String TO_RECIPIENTS = "toRecipients";
    public static final String VALUE = "value";

    public static final ModifiableStringProperty CALENDAR_ID_PROPERTY = string(CALENDAR)
        .label("Calendar ID")
        .options((OptionsFunction<String>) MicrosoftOutlook365OptionUtils::getCalendarOptions)
        .required(true);

    public static final ModifiableObjectProperty CUSTOM_EVENT_OUTPUT_PROPERTY = object()
        .properties(
            string(I_CAL_UID)
                .description("ID for an event across calendars,"),
            string(ID)
                .description("ID of the event."),
            string(SUBJECT)
                .description("The text of the event's subject line."),
            dateTime("startTime")
                .description("Start time of the event."),
            dateTime("endTime")
                .description("End time of the event."),
            array(ATTENDEES)
                .description("The attendees for the event.")
                .items(string().description("The email address of the person or entity.")),
            bool(IS_ONLINE_MEETING)
                .description("Indicates whether the event is an online meeting."),
            string("onlineMeetingUrl")
                .description("URL for an online meeting."),
            bool(REMINDER_MINUTES_BEFORE_START)
                .description("The number of minutes before the event start time that the reminder alert occurs."));

    public static final ModifiableStringProperty FORMAT_PROPERTY = string(FORMAT)
        .label("Format")
        .description("The format to return the message in.")
        .options(
            option("Simple", SIMPLE.name(), "Returns email message's from, to, subject, body and attachments."),
            option("Full", FULL.name(), "Returns all properties of the email message."))
        .defaultValue(SIMPLE.name())
        .required(true);

    public static final ModifiableObjectProperty FULL_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            array(BCC_RECIPIENTS)
                .description("The Bcc: recipients for the message.")
                .items(object()
                    .properties(
                        object(EMAIL_ADDRESS)
                            .description("The recipient's email address.")
                            .properties(
                                string(NAME)
                                    .description("The display name of the recipient."),
                                string(ADDRESS)
                                    .description("The email address of the recipient.")))),
            object(BODY)
                .description("The body of the message. It can be in HTML or text format.")
                .properties(
                    string(CONTENT_TYPE)
                        .description("The content type of the message body."),
                    string(CONTENT)
                        .description("The content of the message body.")),
            string("bodyPreview")
                .description("The first 255 characters of the message body. It is in text format."),
            array(CC_RECIPIENTS)
                .description("The Cc: recipients for the message.")
                .items(
                    object()
                        .properties(
                            object(EMAIL_ADDRESS)
                                .description("The recipient's email address.")
                                .properties(
                                    string(NAME)
                                        .description("The display name of the recipient."),
                                    string(ADDRESS)
                                        .description("The email address of the recipient.")))),
            string(CONVERSATION_ID)
                .description("ID of the conversation the email belongs to."),
            string("conversationIndex")
                .description("Indicates the position of the message within the conversation."),
            string("createdDateTime")
                .description("The date and time the message was created."),
            object(FROM)
                .description("The owner of the mailbox from which the message is sent.")
                .properties(
                    object(EMAIL_ADDRESS)
                        .description("The recipient's email address.")
                        .properties(
                            string(NAME)
                                .description("The display name of the sender."),
                            string(ADDRESS)
                                .description("The email address of the sender."))),
            bool("hasAttachments")
                .description("Indicates whether the message has attachments."),
            string(ID)
                .description("ID of the message."),
            string("importance")
                .description("The importance of the message: low, normal, high."),
            bool("isDeliveryReceiptRequested")
                .description("Indicates whether a delivery receipt is requested for the message."),
            bool("isDraft")
                .description(
                    "Indicates whether the message is a draft. A message is a draft if it hasn't been sent yet."),
            bool("isReadReceiptRequested")
                .description("Indicates whether a read receipt is requested for the message."),
            string("lastModifiedDateTime")
                .description("The date and time the message was last changed."),
            string("parentFolderId")
                .description("The unique identifier for the message's parent mailFolder."),
            string("receivedDateTime")
                .description("The date and time the message was received."),
            array(REPLY_TO)
                .description("The email addresses to use when replying.")
                .items(
                    object()
                        .properties(
                            object(EMAIL_ADDRESS)
                                .description("The recipient's email address.")
                                .properties(
                                    string(NAME)
                                        .description("The display name of the recipient."),
                                    string(ADDRESS)
                                        .description("The email address of the recipient.")))),
            object("sender")
                .description(
                    "The account that is used to generate the message. In most cases, this value is the same as " +
                        "the from property.")
                .properties(
                    object(EMAIL_ADDRESS)
                        .description("The recipient's email address.")
                        .properties(
                            string(NAME)
                                .description("The display name of the sender."),
                            string(ADDRESS)
                                .description("The email address of the sender."))),
            string("sendDateTime")
                .description("The date and time the message was sent."),
            string(SUBJECT)
                .description("Subject of the message."),
            array(TO_RECIPIENTS)
                .description("The Cc: recipients for the message.")
                .items(
                    object()
                        .properties(
                            object(EMAIL_ADDRESS)
                                .description("The recipient's email address.")
                                .properties(
                                    string(NAME)
                                        .description("The display name of the recipient."),
                                    string(ADDRESS)
                                        .description("The email address of the recipient.")))),
            string("webLink")
                .description("The URL to open the message in Outlook on the web."));

    public static final ModifiableObjectProperty SIMPLE_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("ID of the message."),
            string(CONVERSATION_ID)
                .description("ID of the conversation the email belongs to."),
            string(SUBJECT)
                .description("Subject of the message."),
            string(FROM)
                .description("The email address of the sender."),
            string("bodyPreview")
                .description("The first 255 characters of the message body."),
            array(TO)
                .description("The email addresses of the recipients.")
                .items(string()),
            array("cc")
                .description("The email addresses of the cc recipients.")
                .items(string()),
            array("bcc")
                .description("The email addresses of the bcc recipients.")
                .items(string()),
            string("bodyPlain")
                .description("The plain text content of the message body."),
            string("bodyHtml")
                .description("The HTML content of the message body."),
            array(ATTACHMENTS)
                .items(fileEntry()),
            string("webLink")
                .description("The URL to open the message in Outlook on the web."));

    private MicrosoftOutlook365Constants() {
    }
}
