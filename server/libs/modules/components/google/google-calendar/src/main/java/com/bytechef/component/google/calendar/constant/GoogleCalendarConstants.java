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
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import java.util.function.Function;

/**
 * @author Monika Domiter
 */
public class GoogleCalendarConstants {

    private GoogleCalendarConstants() {
    }

    public static final String ACCESS_CODE = "accessCode";
    public static final String ACCESS_ROLE = "accessRole";
    public static final String ADD_ON_PARAMETERS = "addOnParameters";
    public static final String ADDITIONAL_GUESTS = "additionalGuests";
    public static final String ALWAYS_INCLUDE_EMAIL = "alwaysIncludeEmail";
    public static final String ANYONE_CAN_ADD_SELF = "anyoneCanAddSelf";
    public static final String ATTACHMENTS = "attachments";
    public static final String ATTENDEES = "attendees";
    public static final String ATTENDEES_OMMITTED = "attendeesOmitted";
    public static final String AUTO_DECLINE_MODE = "autoDeclineMode";
    public static final String BUILDING_ID = "buildingId";
    public static final String CHAT_STATUS = "chatStatus";
    public static final String COLOR_ID = "colorId";
    public static final String COMMENT = "comment";
    public static final String CONFERENCE_DATA = "conferenceData";
    public static final String CONFERENCE_DATA_VERSION = "conferenceDataVersion";
    public static final String CONFERENCE_ID = "conferenceId";
    public static final String CONFERENCE_SOLUTION = "conferenceSolution";
    public static final String CONFERENCE_SOLUTION_KEY = "conferenceSolutionKey";
    public static final String CREATED = "created";
    public static final String CREATE_EVENT = "createEvent";
    public static final String CREATE_REQUEST = "createRequest";
    public static final String CREATE_QUICK_EVENT = "createQuickEvent";
    public static final String CREATOR = "creator";
    public static final String CUSTOM_LOCATION = "customLocation";
    public static final String DATE = "date";
    public static final String DATE_TIME = "dateTime";
    public static final String DECLINE_MESSAGE = "declineMessage";
    public static final String DEFAULT = "default";
    public static final String DEFAULT_REMINDERS = "defaultReminders";
    public static final String DESCRIPTION = "description";
    public static final String DESK_ID = "deskId";
    public static final String DISPLAY = "display";
    public static final String DISPLAY_NAME = "displayName";
    public static final String EMAIL = "email";
    public static final String END = "end";
    public static final String END_TIME_UNSPECIFIED = "endTimeUnspecified";
    public static final String ENTRY_POINTS = "entryPoints";
    public static final String ENTRY_POINT_FEATURES = "entryPointFeatures";
    public static final String ENTRY_POINT_TYPE = "entryPointType";
    public static final String ETAG = "etag";
    public static final String EVENT = "event";
    public static final String EVENT_TYPE = "eventType";
    public static final String EVENT_TYPE_PROPERTIES = "eventTypeProperties";
    public static final String EXTENDED_PROPERTIES = "extendedProperties";
    public static final String FILE_ID = "fileId";
    public static final String FILE_URL = "fileUrl";
    public static final String FLOOR_ID = "floorId";
    public static final String FLOOR_SECTION_ID = "floorSectionId";
    public static final String FOCUS_TIME_PROPERTIES = "focusTimeProperties";
    public static final String FOCUS_TIME = "focusTime";
    public static final String GADGET = "gadget";
    public static final String GET_EVENTS = "getEvents";
    public static final String GOOGLE_CALENDAR = "googleCalendar";
    public static final String GUEST_CAN_INVITE_OTHERS = "guestsCanInviteOthers";
    public static final String GUEST_CAN_MODIFY = "guestsCanModify";
    public static final String GUEST_CAN_SEE_OTHER_GUESTS = "guestsCanSeeOtherGuests";
    public static final String HANGOUT_LINK = "hangoutLink";
    public static final String HEIGHT = "height";
    public static final String HOME_OFFICE = "homeOffice";
    public static final String HTML_LINK = "htmlLink";
    public static final String ICAL_UID = "iCalUID";
    public static final String ICON_LINK = "iconLink";
    public static final String ICON_URI = "iconUri";
    public static final String ID = "id";
    public static final String KEY = "key";
    public static final String KIND = "kind";
    public static final String LABEL = "label";
    public static final String LINK = "link";
    public static final String LOCATION = "location";
    public static final String LOCKED = "locked";
    public static final String MAX_ATTENDEES = "maxAttendees";
    public static final String MAX_RESULTS = "maxResults";
    public static final String METHOD = "method";
    public static final String MEETING_CODE = "meetingCode";
    public static final String MIME_TYPE = "mimeType";
    public static final String MINUTES = "minutes";
    public static final String NAME = "name";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String NEXT_SYNC_TOKEN = "nextSyncToken";
    public static final String NOTES = "notes";
    public static final String OFFICE_LOCATION = "officeLocation";
    public static final String OPTIONAL = "optional";
    public static final String ORIGINAL_START_TIME = "originalStartTime";
    public static final String ORGANIZER = "organizer";
    public static final String OUT_OF_OFFICE = "outOfOffice";
    public static final String OUT_OF_OFFICE_PROPERTIES = "outOfOfficeProperties";
    public static final String OVERRIDES = "overrides";
    public static final String PAGE_TOKEN = "pageToken";
    public static final String PARAMETERS = "parameters";
    public static final String PASSCODE = "passcode";
    public static final String PASSWORD = "password";
    public static final String PIN = "pin";
    public static final String PREFERENCES = "preferences";
    public static final String PRIVATE = "private";
    public static final String PRIVATE_COPY = "privateCopy";
    public static final String PRIVATE_EXTENDED_PROPERTY = "privateExtendedProperty";
    public static final String Q = "q";
    public static final String ORDER_BY = "orderBy";
    public static final String RECURRENCE = "recurrence";
    public static final String RECURRING_EVENT_ID = "recurringEventId";
    public static final String REGION_CODE = "regionCode";
    public static final String REMINDERS = "reminders";
    public static final String REQUEST_ID = "requestId";
    public static final String RESOURCE = "resource";
    public static final String RESPONSE_STATUS = "responseStatus";
    public static final String SEND_UPDATES = "sendUpdates";
    public static final String SELF = "self";
    public static final String SHARED = "shared";
    public static final String SHARED_EXTENDED_PROPERTY = "sharedExtendedProperty";
    public static final String SHOW_DELETED = "showDeleted";
    public static final String SHOW_HIDDEN_INVITATIONS = "showHiddenInvitations";
    public static final String SIGNATURE = "signature";
    public static final String SINGLE_EVENTS = "singleEvents";
    public static final String SEQUENCE = "sequence";
    public static final String SYNC_TOKEN = "syncToken";
    public static final String SOURCE = "source";
    public static final String STATUS = "status";
    public static final String STATUS_CODE = "statusCode";
    public static final String SUMMARY = "summary";
    public static final String SUPPORTS_ATTACHMENTS = "supportsAttachments";
    public static final String TEXT = "text";
    public static final String TIME_MAX = "timeMax";
    public static final String TIME_MIN = "timeMin";
    public static final String TIME_ZONE = "timeZone";
    public static final String TENTATIVE = "tentative";
    public static final String TITLE = "title";
    public static final String TRANSPARENCY = "transparency";
    public static final String TYPE = "type";
    public static final String START = "start";
    public static final String UPDATED = "updated";
    public static final String URI = "uri";
    public static final String URL = "url";
    public static final String USE_DEFAULT = "useDefault";
    public static final String UPDATE_MIN = "updatedMin";
    public static final String VISIBILITY = "visibility";
    public static final String WIDTH = "width";
    public static final String WORKING_LOCATION = "workingLocation";
    public static final String WORKING_LOCATION_PROPERTIES = "workingLocationProperties";

    public static final ModifiableObjectProperty CONFERENCE_SOLUTION_KEY_PROPERTY =
        object(CONFERENCE_SOLUTION_KEY)
            .label("Key")
            .properties(
                string(TYPE)
                    .label("Type")
                    .required(false))
            .required(false);

    public static final ComponentDSL.ModifiableDateProperty DATE_PROPERTY = date(DATE)
        .label("Date")
        .description("The date, in the format \"yyyy-mm-dd\", if this is an all-day event.")
        .required(false);

    public static final ComponentDSL.ModifiableDateTimeProperty DATE_TIME_PROPERTY = dateTime(DATE_TIME)
        .label("Datetime")
        .description(
            "The time, as a combined date-time value (formatted according to RFC3339). A time " +
                "zone offset is required unless a time zone is explicitly specified in timeZone.")
        .required(false);

    public static final Function<String, ModifiableObjectProperty> EVENT_PROPERTY_FUNCTION = (name) -> object(name)
        .properties(
            bool(ANYONE_CAN_ADD_SELF),
            array(ATTACHMENTS)
                .items(
                    string(FILE_ID),
                    string(FILE_URL),
                    string(ICON_LINK),
                    string(MIME_TYPE),
                    string(TITLE)),
            array(ATTENDEES)
                .items(
                    integer(ADDITIONAL_GUESTS),
                    string(COMMENT),
                    string(DISPLAY_NAME),
                    string(EMAIL),
                    string(ID),
                    bool(OPTIONAL),
                    bool(ORGANIZER),
                    bool(RESOURCE),
                    string(RESPONSE_STATUS),
                    bool(SELF)),
            bool(ATTENDEES_OMMITTED),
            string(COLOR_ID),
            object(CONFERENCE_DATA)
                .properties(
                    string(CONFERENCE_ID),
                    object(CONFERENCE_SOLUTION)
                        .properties(
                            string(ICON_URI),
                            object(KEY)
                                .properties(
                                    string(TYPE)),
                            string(NAME)),
                    object(CREATE_REQUEST)
                        .properties(
                            object(CONFERENCE_SOLUTION_KEY)
                                .properties(
                                    string(TYPE)),
                            string(REQUEST_ID),
                            object(STATUS)
                                .properties(
                                    string(STATUS_CODE))),
                    array(ENTRY_POINTS)
                        .items(
                            string(ACCESS_CODE),
                            array(ENTRY_POINT_FEATURES),
                            string(ENTRY_POINT_TYPE),
                            string(LABEL),
                            string(MEETING_CODE),
                            string(PASSCODE),
                            string(PASSWORD),
                            string(PIN),
                            string(REGION_CODE),
                            string(URI)),
                    string(NOTES),
                    object(PARAMETERS),
                    string(SIGNATURE)),
            dateTime(CREATED),
            object(CREATOR)
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
            bool(END_TIME_UNSPECIFIED),
            string(ETAG),
            string(EVENT_TYPE),
            object(EXTENDED_PROPERTIES)
                .properties(
                    object(PRIVATE),
                    object(SHARED)),
            object(FOCUS_TIME_PROPERTIES)
                .properties(
                    string(AUTO_DECLINE_MODE),
                    string(CHAT_STATUS),
                    string(DECLINE_MESSAGE)),
            object(GADGET)
                .properties(
                    string(DISPLAY),
                    integer(HEIGHT),
                    string(ICON_LINK),
                    string(LINK),
                    object(PREFERENCES),
                    string(TITLE),
                    string(TYPE),
                    string(WIDTH)),
            bool(GUEST_CAN_INVITE_OTHERS),
            bool(GUEST_CAN_MODIFY),
            bool(GUEST_CAN_SEE_OTHER_GUESTS),
            string(HANGOUT_LINK),
            string(HTML_LINK),
            string(ICAL_UID),
            string(ID),
            string(KIND),
            string(LOCATION),
            bool(LOCKED),
            object(ORGANIZER)
                .properties(
                    string(DISPLAY_NAME),
                    string(EMAIL),
                    string(ID),
                    bool(SELF)),
            object(ORIGINAL_START_TIME)
                .properties(
                    dateTime(DATE),
                    dateTime(DATE_TIME),
                    string(TIME_ZONE)),
            object(OUT_OF_OFFICE_PROPERTIES)
                .properties(
                    string(AUTO_DECLINE_MODE),
                    string(DECLINE_MESSAGE)),
            bool(PRIVATE_COPY),
            array(RECURRENCE),
            string(RECURRING_EVENT_ID),
            object(REMINDERS)
                .properties(
                    array(OVERRIDES)
                        .items(
                            string(METHOD),
                            integer(MINUTES)),
                    bool(USE_DEFAULT)),
            integer(SEQUENCE),
            object(SOURCE)
                .properties(
                    string(TITLE),
                    string(URL)),
            object(START)
                .properties(
                    dateTime(DATE),
                    dateTime(DATE_TIME),
                    string(TIME_ZONE)),
            string(STATUS),
            string(SUMMARY),
            string(TRANSPARENCY),
            dateTime(UPDATED),
            string(VISIBILITY),
            object(WORKING_LOCATION_PROPERTIES)
                .properties(
                    object(CUSTOM_LOCATION)
                        .properties(
                            string(LABEL)),
                    object(HOME_OFFICE),
                    object(OFFICE_LOCATION)
                        .properties(
                            string(BUILDING_ID),
                            string(DESK_ID),
                            string(FLOOR_ID),
                            string(FLOOR_SECTION_ID),
                            string(LABEL)),
                    string(TYPE)));

    public static final ComponentDSL.ModifiableIntegerProperty MAX_ATTENDEES_PROPERTY =
        integer(MAX_ATTENDEES)
            .label("Max attendees")
            .description(
                "The maximum number of attendees to include in the response. If there are more than the " +
                    "specified number of attendees, only the participant is returned.")
            .required(false);

    public static final ComponentDSL.ModifiableStringProperty SEND_UPDATES_PROPERTY =
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
            .required(false);

    public static final ComponentDSL.ModifiableStringProperty TIME_ZONE_PROPERTY = string(TIME_ZONE)
        .label("Time zone")
        .description(
            "The time zone in which the time is specified. (Formatted as an IANA Time Zone Database name, e.g. " +
                "\"Europe/Zurich\".) For recurring events this field is required and specifies the time zone in " +
                "which the recurrence is expanded. For single events this field is optional and indicates a custom " +
                "time zone for the event start/end.")
        .required(false);
}
