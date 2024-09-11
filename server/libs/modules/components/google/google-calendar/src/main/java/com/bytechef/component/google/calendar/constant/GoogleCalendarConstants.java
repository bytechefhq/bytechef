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

package com.bytechef.component.google.calendar.constant;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarConstants {

    private GoogleCalendarConstants() {
    }

    public static final String ALL_DAY = "allDay";
    public static final String ATTACHMENTS = "attachments";
    public static final String ATTENDEES = "attendees";
    public static final String CALENDAR_ID = "calendarId";
    public static final String DATE_RANGE = "dateRange";
    public static final String DESCRIPTION = "description";
    public static final String DISPLAY_NAME = "displayName";
    public static final String EMAIL = "email";
    public static final String END = "end";
    public static final String EVENT_TYPE = "eventType";
    public static final String GUEST_CAN_INVITE_OTHERS = "guestsCanInviteOthers";
    public static final String GUEST_CAN_MODIFY = "guestsCanModify";
    public static final String GUEST_CAN_SEE_OTHER_GUESTS = "guestsCanSeeOtherGuests";
    public static final String ICON_LINK = "iconLink";
    public static final String ID = "id";
    public static final String LOCATION = "location";
    public static final String MAX_RESULTS = "maxResults";
    public static final String METHOD = "method";
    public static final String MINUTES = "minutes";
    public static final String ORGANIZER = "organizer";
    public static final String Q = "q";
    public static final String REMINDERS = "reminders";
    public static final String RESOURCE_ID = "resourceId";
    public static final String SEND_UPDATES = "sendUpdates";
    public static final String SELF = "self";
    public static final String STATUS = "status";
    public static final String SUMMARY = "summary";
    public static final String TEXT = "text";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String TITLE = "title";
    public static final String START = "start";
    public static final String USE_DEFAULT = "useDefault";

    public static final ModifiableStringProperty CALENDAR_ID_PROPERTY = string(CALENDAR_ID)
        .label("Calendar identifier")
        .options((ActionOptionsFunction<String>) GoogleCalendarUtils::getCalendarIdOptions)
        .required(true);

    public static final ModifiableObjectProperty EVENT_OUTPUT_PROPERTY = object()
        .properties(
            string("iCalUID"),
            string(ID),
            string(SUMMARY),
            dateTime("startTime"),
            dateTime("endTime"),
            string("etag"),
            string(EVENT_TYPE),
            string("htmlLink"),
            string(STATUS),
            string(LOCATION),
            string("hangoutLink"),
            array(ATTENDEES)
                .items(
                    object()
                        .properties(
                            integer("additionalGuests"),
                            string("comment"),
                            string(DISPLAY_NAME),
                            string(EMAIL),
                            string(ID),
                            bool("optional"),
                            bool(ORGANIZER),
                            bool("resource"),
                            string("responseStatus"),
                            bool(SELF))),
            array(ATTACHMENTS)
                .items(
                    object()
                        .properties(string("fileId"),
                            string("fileUrl"),
                            string(ICON_LINK),
                            string("mimeType"),
                            string(TITLE))),
            object(REMINDERS)
                .properties(
                    array("overrides")
                        .items(
                            object()
                                .properties(
                                    string(METHOD),
                                    integer(MINUTES))),
                    bool(USE_DEFAULT)));

    public static final ModifiableStringProperty SEND_UPDATES_PROPERTY =
        string(SEND_UPDATES)
            .label("Send updates")
            .description(
                "Whether to send notifications about the creation of the new event. Note that some emails might " +
                    "still be sent.")
            .options(
                option("All", "all", "Notifications are sent to all guests."),
                option("External only", "externalOnly",
                    "Notifications are sent to non-Google Calendar guests only."),
                option("None", "none", "No notifications are sent."))
            .defaultValue("none")
            .required(false);

}
