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
 * @author Monika Domiter
 */
public class GoogleCalendarConstants {

    private GoogleCalendarConstants() {
    }

    public static final String ACCESS_ROLE = "accessRole";
    public static final String ALL_DAY = "allDay";
    public static final String ANYONE_CAN_ADD_SELF = "anyoneCanAddSelf";
    public static final String ATTACHMENTS = "attachments";
    public static final String ATTENDEES = "attendees";
    public static final String AUTO_DECLINE_MODE = "autoDeclineMode";
    public static final String CALENDAR_ID = "calendarId";
    public static final String CREATE_EVENT = "createEvent";
    public static final String CREATE_QUICK_EVENT = "createQuickEvent";
    public static final String DATE = "date";
    public static final String DATE_TIME = "dateTime";
    public static final String DECLINE_MESSAGE = "declineMessage";
    public static final String DEFAULT = "default";
    public static final String DEFAULT_REMINDERS = "defaultReminders";
    public static final String DESCRIPTION = "description";
    public static final String DISPLAY_NAME = "displayName";
    public static final String EMAIL = "email";
    public static final String END = "end";
    public static final String ETAG = "etag";
    public static final String EVENT = "event";
    public static final String EVENT_TYPE = "eventType";
    public static final String FOCUS_TIME = "focusTime";
    public static final String FIND_EVENTS = "findEvents";
    public static final String GOOGLE_CALENDAR = "googleCalendar";
    public static final String GUEST_CAN_INVITE_OTHERS = "guestsCanInviteOthers";
    public static final String GUEST_CAN_MODIFY = "guestsCanModify";
    public static final String GUEST_CAN_SEE_OTHER_GUESTS = "guestsCanSeeOtherGuests";
    public static final String ICON_LINK = "iconLink";
    public static final String ID = "id";
    public static final String KIND = "kind";
    public static final String LABEL = "label";
    public static final String LOCATION = "location";
    public static final String MAX_RESULTS = "maxResults";
    public static final String METHOD = "method";
    public static final String MINUTES = "minutes";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String NEXT_SYNC_TOKEN = "nextSyncToken";
    public static final String ORGANIZER = "organizer";
    public static final String OUT_OF_OFFICE = "outOfOffice";
    public static final String PARAMETERS = "parameters";
    public static final String Q = "q";
    public static final String REMINDERS = "reminders";
    public static final String SEND_UPDATES = "sendUpdates";
    public static final String SELF = "self";
    public static final String STATUS = "status";
    public static final String SUMMARY = "summary";
    public static final String TEXT = "text";
    public static final String TIME_MAX = "timeMax";
    public static final String TIME_MIN = "timeMin";
    public static final String TIME_ZONE = "timeZone";
    public static final String TITLE = "title";
    public static final String TIME = "time";
    public static final String TYPE = "type";
    public static final String START = "start";
    public static final String UPDATED = "updated";
    public static final String USE_DEFAULT = "useDefault";

    public static final ModifiableStringProperty CALENDAR_ID_PROPERTY = string(CALENDAR_ID)
        .label("Calendar identifier")
        .options((ActionOptionsFunction<String>) GoogleCalendarUtils::getCalendarIdOptions)
        .required(true);

    public static final ModifiableObjectProperty EVENT_PROPERTY = object()
        .properties(
            bool(ANYONE_CAN_ADD_SELF),
            array(ATTACHMENTS)
                .items(
                    object()
                        .properties(string("fileId"),
                            string("fileUrl"),
                            string(ICON_LINK),
                            string("mimeType"),
                            string(TITLE))),
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
            bool("attendeesOmitted"),
            string("colorId"),
            object("conferenceData")
                .properties(
                    string("conferenceId"),
                    object("conferenceSolution")
                        .properties(
                            string("iconUri"),
                            object("key")
                                .properties(
                                    string(TYPE)),
                            string("name")),
                    object("createRequest")
                        .properties(
                            object("conferenceSolutionKey")
                                .properties(
                                    string(TYPE)),
                            string("requestId"),
                            object(STATUS)
                                .properties(
                                    string("statusCode"))),
                    array("entryPoints")
                        .items(
                            object()
                                .properties(
                                    string("accessCode"),
                                    array("entryPointFeatures")
                                        .items(string()),
                                    string("entryPointType"),
                                    string(LABEL),
                                    string("meetingCode"),
                                    string("passcode"),
                                    string("password"),
                                    string("pin"),
                                    string("regionCode"),
                                    string("uri"))),
                    string("notes"),
                    object(PARAMETERS)
                        .properties(
                            object("addOnParameters")
                                .properties(
                                    object(PARAMETERS)
                                        .additionalProperties(string()))),
                    string("signature")),
            dateTime("created"),
            object("creator")
                .properties(
                    string(DISPLAY_NAME),
                    string(EMAIL),
                    string(ID),
                    bool(SELF)),
            string(DESCRIPTION),
            object(END)
                .properties(
                    dateTime(DATE),
                    dateTime(DATE_TIME),
                    string(TIME_ZONE)),
            bool("endTimeUnspecified"),
            string("etag"),
            string(EVENT_TYPE),
            object("extendedProperties")
                .properties(
                    object("private")
                        .additionalProperties(string()),
                    object("shared")
                        .additionalProperties(string())),
            object("focusTimeProperties")
                .properties(
                    string(AUTO_DECLINE_MODE),
                    string("chatStatus"),
                    string(DECLINE_MESSAGE)),
            object("gadget")
                .properties(
                    string("display"),
                    integer("height"),
                    string(ICON_LINK),
                    string("link"),
                    object("preferences")
                        .additionalProperties(string()),
                    string(TITLE),
                    string(TYPE),
                    string("width")),
            bool(GUEST_CAN_INVITE_OTHERS),
            bool(GUEST_CAN_MODIFY),
            bool(GUEST_CAN_SEE_OTHER_GUESTS),
            string("hangoutLink"),
            string("htmlLink"),
            string("iCalUID"),
            string(ID),
            string(KIND),
            string(LOCATION),
            bool("locked"),
            object(ORGANIZER)
                .properties(
                    string(DISPLAY_NAME),
                    string(EMAIL),
                    string(ID),
                    bool(SELF)),
            object("originalStartTime")
                .properties(
                    dateTime(DATE),
                    dateTime(DATE_TIME),
                    string(TIME_ZONE)),
            object("outOfOfficeProperties")
                .properties(
                    string(AUTO_DECLINE_MODE),
                    string(DECLINE_MESSAGE)),
            bool("privateCopy"),
            array("recurrence")
                .items(string()),
            string("recurringEventId"),
            object(REMINDERS)
                .properties(
                    array("overrides")
                        .items(
                            object()
                                .properties(
                                    string(METHOD),
                                    integer(MINUTES))),
                    bool(USE_DEFAULT)),
            integer("sequence"),
            object("source")
                .properties(
                    string(TITLE),
                    string("url")),
            object(START)
                .properties(
                    dateTime(DATE),
                    dateTime(DATE_TIME),
                    string(TIME_ZONE)),
            string(STATUS),
            string(SUMMARY),
            string("transparency"),
            dateTime(UPDATED),
            string("visibility"),
            object("workingLocationProperties")
                .properties(
                    object("customLocation")
                        .properties(
                            string(LABEL)),
                    object("homeOffice"),
                    object("officeLocation")
                        .properties(
                            string("buildingId"),
                            string("deskId"),
                            string("floorId"),
                            string("floorSectionId"),
                            string(LABEL)),
                    string(TYPE)));

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
