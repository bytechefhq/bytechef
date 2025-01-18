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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Property;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365OptionUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365Constants {

    public static final String ADDRESS = "address";
    public static final String ALL_DAY = "allDay";
    public static final String ATTENDEES = "attendees";
    public static final String BCC_RECIPIENTS = "bccRecipients";
    public static final String BODY = "body";
    public static final String CALENDAR = "calendar";
    public static final String CATEGORY = "category";
    public static final String CC_RECIPIENTS = "ccRecipients";
    public static final String COMMENT = "comment";
    public static final String CONTENT = "content";
    public static final String CONTENT_TYPE = "contentType";
    public static final String DATE_RANGE = "dateRange";
    public static final String DATE_TIME = "dateTime";
    public static final String END = "end";
    public static final String EMAIL_ADDRESS = "emailAddress";
    public static final String EVENT = "event";
    public static final String FROM = "from";
    public static final String I_CAL_UID = "iCalUId";
    public static final String ID = "id";
    public static final String IS_ONLINE_MEETING = "isOnlineMeeting";
    public static final String NAME = "name";
    public static final String ODATA_NEXT_LINK = "@odata.nextLink";
    public static final String RECIPIENT = "recipient";
    public static final String REMINDER_MINUTES_BEFORE_START = "reminderMinutesBeforeStart";
    public static final String REPLY_TO = "replyTo";
    public static final String START = "start";
    public static final String SUBJECT = "subject";
    public static final String TIME_ZONE = "timeZone";
    public static final String TO = "to";
    public static final String TO_RECIPIENTS = "toRecipients";
    public static final String VALUE = "value";

    public static final ModifiableStringProperty CALENDAR_ID_PROPERTY = string(CALENDAR)
        .label("Calendar")
        .options((ActionOptionsFunction<String>) MicrosoftOutlook365OptionUtils::getCalendarOptions)
        .required(true);

    public static final ModifiableStringProperty CONTENT_PROPERTY_HTML = string(CONTENT)
        .label("HTML Content")
        .description("The content of the item.")
        .controlType(Property.ControlType.RICH_TEXT)
        .displayCondition("body.contentType == '%s'".formatted(ContentType.HTML))
        .required(false);

    public static final ModifiableStringProperty CONTENT_PROPERTY_TEXT = string(CONTENT)
        .label("Text Content")
        .description("The content of the item.")
        .controlType(Property.ControlType.TEXT_AREA)
        .displayCondition("body.contentType == '%s'".formatted(ContentType.TEXT))
        .required(false);

    public static final ModifiableStringProperty CONTENT_TYPE_PROPERTY = string(CONTENT_TYPE)
        .label("Content Type")
        .description("The type of the content.")
        .options(
            option("Text", ContentType.TEXT.name()),
            option("HTML", ContentType.HTML.name()))
        .defaultValue(ContentType.TEXT.name())
        .required(false);

    public static final ModifiableObjectProperty CUSTOM_EVENT_OUTPUT_PROPERTY = object()
        .properties(
            string(I_CAL_UID),
            string(ID),
            string(SUBJECT),
            dateTime("startTime"),
            dateTime("endTime"),
            array(ATTENDEES)
                .items(string()),
            bool(IS_ONLINE_MEETING),
            string("onlineMeetingUrl"),
            bool(REMINDER_MINUTES_BEFORE_START));

    public static final ModifiableObjectProperty MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID),
            string(SUBJECT),
            string("bodyPreview"),
            object(BODY)
                .properties(
                    string(CONTENT_TYPE),
                    string(CONTENT)),
            object(FROM)
                .properties(
                    object(EMAIL_ADDRESS)
                        .properties(
                            string(NAME),
                            string(ADDRESS))));

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

    private MicrosoftOutlook365Constants() {
    }
}
