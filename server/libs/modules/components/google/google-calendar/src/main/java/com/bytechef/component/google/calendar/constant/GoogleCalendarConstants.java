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

package com.bytechef.component.google.calendar.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import java.time.LocalTime;

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
    public static final String END = "end";
    public static final String EVENT_ID = "eventId";
    public static final String EVENT_TYPE = "eventType";
    public static final String GUEST_CAN_INVITE_OTHERS = "guestsCanInviteOthers";
    public static final String GUEST_CAN_MODIFY = "guestsCanModify";
    public static final String GUEST_CAN_SEE_OTHER_GUESTS = "guestsCanSeeOtherGuests";
    public static final String ID = "id";
    public static final String LOCATION = "location";
    public static final String MAX_RESULTS = "maxResults";
    public static final String METHOD = "method";
    public static final LocalTime LOCAL_TIME_MAX = LocalTime.MAX;
    public static final LocalTime LOCAL_TIME_MIN = LocalTime.MIN;
    public static final String MINUTES = "minutes";
    public static final String Q = "q";
    public static final String REMINDERS = "reminders";
    public static final String RESOURCE_ID = "resourceId";
    public static final String SEND_UPDATES = "sendUpdates";
    public static final String SUMMARY = "summary";
    public static final String TEXT = "text";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String START = "start";
    public static final String USE_DEFAULT = "useDefault";

    public static final ModifiableStringProperty CALENDAR_ID_PROPERTY = string(CALENDAR_ID)
        .label("Calendar Identifier")
        .options((OptionsFunction<String>) GoogleCalendarUtils::getCalendarIdOptions)
        .required(true);

    public static final ModifiableObjectProperty EVENT_OUTPUT_PROPERTY = object()
        .properties(
            string("iCalUID")
                .description(
                    "Event unique identifier as defined in RFC5545. It is used to uniquely identify events across " +
                        "calendaring systems."),
            string(ID)
                .description("Identifier of the event."),
            string(SUMMARY)
                .description("Title of the event."),
            dateTime("startTime")
                .description("Start time of the event."),
            dateTime("endTime")
                .description("End time of the event."),
            string("etag")
                .description("ETag of the resource."),
            string(EVENT_TYPE)
                .description("Specific type of the event."),
            string("htmlLink")
                .description("An absolute link to this event in the Google Calendar Web UI."),
            string("status")
                .description("Status of the event."),
            string(LOCATION)
                .description("Geographic location of the event as free-form text."),
            string("hangoutLink")
                .description("An absolute link to the Google Hangout associated with this event."),
            array(ATTENDEES)
                .description("The attendees of the event.")
                .items(
                    object()
                        .properties(
                            integer("additionalGuests")
                                .description("Number of additional guests."),
                            string("comment")
                                .description("The attendee's response comment."),
                            string("displayName")
                                .description("The attendee's name."),
                            string("email")
                                .description("The attendee's email address."),
                            string(ID)
                                .description("The attendee's Profile ID."),
                            bool("optional")
                                .description("Whether this is an optional attendee."),
                            bool("organizer")
                                .description("Whether the attendee is the organizer of the event."),
                            bool("resource")
                                .description("Whether the attendee is a resource. "),
                            string("responseStatus")
                                .description("The attendee's response status."),
                            bool("self")
                                .description(
                                    "Whether this entry represents the calendar on which this copy of the event appears."))),
            array(ATTACHMENTS)
                .description("File attachments for the event.")
                .items(
                    object()
                        .properties(
                            string("fileId")
                                .description("ID of the attached file. "),
                            string("fileUrl")
                                .description("URL link to the attachment."),
                            string("iconLink")
                                .description("URL link to the attachment's icon."),
                            string("mimeType")
                                .description("Internet media type (MIME type) of the attachment."),
                            string("title")
                                .description("Attachment title."))),
            object(REMINDERS)
                .description("Information about the event's reminders for the authenticated user.")
                .properties(
                    array("overrides")
                        .items(
                            object()
                                .properties(
                                    string(METHOD)
                                        .description("The method used by this reminder."),
                                    integer(MINUTES)
                                        .description(
                                            "Number of minutes before the start of the event when the reminder " +
                                                "should trigger."))),
                    bool(USE_DEFAULT)
                        .description("Whether the default reminders of the calendar apply to the event.")));

    public static final ModifiableStringProperty SEND_UPDATES_PROPERTY =
        string(SEND_UPDATES)
            .label("Send Updates")
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
