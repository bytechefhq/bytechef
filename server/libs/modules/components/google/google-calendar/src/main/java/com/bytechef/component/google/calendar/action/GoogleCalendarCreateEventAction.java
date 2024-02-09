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

package com.bytechef.component.google.calendar.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dynamicProperties;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ACCESS_CODE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ADDITIONAL_GUESTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ADD_ON_PARAMETERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ANYONE_CAN_ADD_SELF;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ATTACHMENTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.COLOR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.COMMENT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CONFERENCE_DATA;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CONFERENCE_DATA_VERSION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CONFERENCE_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CONFERENCE_SOLUTION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CONFERENCE_SOLUTION_KEY_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CREATE_EVENT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CREATE_REQUEST;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DATE_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DATE_TIME_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DEFAULT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DESCRIPTION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DISPLAY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DISPLAY_NAME;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EMAIL;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.END;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ENTRY_POINTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ENTRY_POINT_FEATURES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ENTRY_POINT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE_PROPERTIES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EXTENDED_PROPERTIES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FOCUS_TIME;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FOCUS_TIME_PROPERTIES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GADGET;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_INVITE_OTHERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_MODIFY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_SEE_OTHER_GUESTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.HEIGHT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ICON_LINK;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ICON_URI;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LABEL;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LINK;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LOCATION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_ATTENDEES_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MEETING_CODE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.METHOD;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MINUTES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.NAME;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.NOTES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.OPTIONAL;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ORIGINAL_START_TIME;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.OUT_OF_OFFICE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.OUT_OF_OFFICE_PROPERTIES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.OVERRIDES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.PARAMETERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.PASSCODE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.PASSWORD;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.PIN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.PREFERENCES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.PRIVATE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.RECURRENCE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.REGION_CODE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.REMINDERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.REQUEST_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.RESOURCE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.RESPONSE_STATUS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEQUENCE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SHARED;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SIGNATURE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SOURCE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.START;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.STATUS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.STATUS_CODE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SUMMARY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SUPPORTS_ATTACHMENTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TENTATIVE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_ZONE_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TITLE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TRANSPARENCY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.URI;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.URL;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.USE_DEFAULT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.VISIBILITY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.WIDTH;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.WORKING_LOCATION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.WORKING_LOCATION_PROPERTIES;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttachment;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventFocusTimeProperties;
import com.google.api.services.calendar.model.EventOutOfOfficeProperties;
import com.google.api.services.calendar.model.EventWorkingLocationProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class GoogleCalendarCreateEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_EVENT)
        .title("Create event")
        .description("Creates an event")
        .properties(
            CALENDAR_ID_PROPERTY,
            integer(CONFERENCE_DATA_VERSION)
                .label("Conference date version")
                .description("")
                .options(
                    option("0", 0,
                        "Version 0 assumes no conference data support and ignores conference data in the " +
                            "event's body. "),
                    option("1", 1,
                        "Version 1 enables support for copying of ConferenceData as well as for creating " +
                            "new conferences using the createRequest field of conferenceData. "))
                .defaultValue(0)
                .required(false),
            MAX_ATTENDEES_PROPERTY,
            SEND_UPDATES_PROPERTY,
            bool(SUPPORTS_ATTACHMENTS)
                .label("Supports attachments")
                .description("Whether API client performing operation supports event attachments.")
                .defaultValue(false)
                .required(false),
            object(START)
                .label("Start time")
                .description(
                    "The (inclusive) start time of the event. For a recurring event, this is the start time of the " +
                        "first instance.")
                .properties(
                    DATE_PROPERTY,
                    DATE_TIME_PROPERTY,
                    TIME_ZONE_PROPERTY)
                .required(true),
            object(END)
                .label("End time")
                .description(
                    "The (exclusive) end time of the event. For a recurring event, this is the end time of the " +
                        "first instance.")
                .properties(
                    DATE_PROPERTY,
                    DATE_TIME_PROPERTY,
                    TIME_ZONE_PROPERTY)
                .required(true),
            bool(ANYONE_CAN_ADD_SELF)
                .label("Anyone can add self")
                .description("Whether anyone can invite themselves to the event (deprecated).")
                .defaultValue(false)
                .required(false),
            array(ATTACHMENTS)
                .label("Attachments")
                .items(
                    fileEntry())
                .required(false),
            array(ATTENDEES)
                .label("Attendees")
                .description("The attendees of the event.")
                .items(
                    object()
                        .properties(
                            integer(ADDITIONAL_GUESTS)
                                .label("Additional guests")
                                .description("Number of additional guests.")
                                .defaultValue(0)
                                .required(false),
                            string(COMMENT)
                                .label("Comment")
                                .description("The attendee's response comment.")
                                .required(false),
                            string(DISPLAY_NAME)
                                .label("Display name")
                                .description("The attendee's name, if available.")
                                .required(false),
                            string(EMAIL)
                                .label("Email")
                                .description(
                                    "The attendee's email address, if available. This field must be present when " +
                                        "adding an attendee. It must be a valid email address as per RFC5322.")
                                .required(true),
                            bool(OPTIONAL)
                                .label("Optional")
                                .description("Whether this is an optional attendee.")
                                .defaultValue(false)
                                .required(false),
                            bool(RESOURCE)
                                .label("Resource")
                                .description(
                                    "Whether the attendee is a resource. Can only be set when the attendee is " +
                                        "added to the event for the first time. Subsequent modifications are " +
                                        "ignored.")
                                .defaultValue(false)
                                .required(false),
                            string(RESPONSE_STATUS)
                                .label("Response status")
                                .description("The attendee's response status.")
                                .options(
                                    option("Needs action", "needsAction",
                                        "The attendee has not responded to the invitation (recommended for new events)."),
                                    option("Declined", "declined", "The attendee has declined the invitation."),
                                    option("Tentative", TENTATIVE,
                                        "The attendee has tentatively accepted the invitation."),
                                    option("Accepted", "accepted", "The attendee has accepted the invitation."))))
                .required(false),
            string(COLOR_ID)
                .label("Color ID")
                .description(
                    "The color of the event. This is an ID referring to an entry in the event section of the " +
                        "colors definition")
                .options((ActionOptionsFunction<String>) GoogleCalendarUtils::getColorOptions)
                .required(false),
            object(CONFERENCE_DATA)
                .label("Conference data")
                .description(
                    "The conference-related information, such as details of a Google Meet conference. To create " +
                        "new conference details use the createRequest field. To persist your changes, " +
                        "remember to set the conferenceDataVersion request parameter to 1 for all event " +
                        "modification requests.")
                .properties(
                    string(CONFERENCE_ID)
                        .label("Conference ID")
                        .required(false),
                    object(CONFERENCE_SOLUTION)
                        .label("Conference solution")
                        .properties(
                            string(ICON_URI)
                                .label("Icon uri")
                                .required(false),
                            CONFERENCE_SOLUTION_KEY_PROPERTY,
                            string(NAME)
                                .label("Name")
                                .required(false))
                        .required(false),
                    object(CREATE_REQUEST)
                        .label("Create conference request")
                        .properties(
                            CONFERENCE_SOLUTION_KEY_PROPERTY,
                            string(REQUEST_ID)
                                .label("Request ID")
                                .required(false),
                            object(STATUS)
                                .label("Conference request status")
                                .properties(
                                    string(STATUS_CODE)
                                        .label("Status code")
                                        .required(false))
                                .required(false))
                        .required(false),
                    array(ENTRY_POINTS)
                        .label("Entry points")
                        .items(
                            object()
                                .properties(
                                    string(ACCESS_CODE)
                                        .label("Access code")
                                        .required(false),
                                    array(ENTRY_POINT_FEATURES)
                                        .label("Entry point features")
                                        .items(string())
                                        .required(false),
                                    string(ENTRY_POINT_TYPE)
                                        .label("Entry point type")
                                        .required(false),
                                    string(LABEL)
                                        .label("Label")
                                        .required(false),
                                    string(MEETING_CODE)
                                        .label("Meeting code")
                                        .required(false),
                                    string(PASSCODE)
                                        .label("Passcode")
                                        .required(false),
                                    string(PASSWORD)
                                        .label("Password")
                                        .required(false),
                                    string(PIN)
                                        .label("Pin")
                                        .required(false),
                                    string(REGION_CODE)
                                        .label("Region code")
                                        .required(false),
                                    string(URI)
                                        .label("Uri")
                                        .required(false)))
                        .required(false),
                    string(NOTES)
                        .label("Notes")
                        .required(false),
                    object(PARAMETERS)
                        .label("Parameters")
                        .properties(
                            object(ADD_ON_PARAMETERS)
                                .properties(
                                    object(PARAMETERS)
                                        .additionalProperties(string())))
                        .required(false),
                    string(SIGNATURE)
                        .label("Signature")
                        .required(false))
                .required(false),
            string(DESCRIPTION)
                .label("Description")
                .description("Description of the event. Can contain HTML.")
                .required(false),
            string(EVENT_TYPE)
                .label("Event type")
                .description("Specific type of the event. This cannot be modified after the event is created. ")
                .options(
                    option("Default", DEFAULT, "A regular event or not further specified."),
                    option("Out of office", OUT_OF_OFFICE, "An out-of-office event."),
                    option("Focus time", FOCUS_TIME, "A focus-time event."),
                    option("Working location", WORKING_LOCATION, "A working location event."))
                .required(false),
            dynamicProperties(EVENT_TYPE_PROPERTIES)
                .loadPropertiesDependsOn(EVENT_TYPE)
                .properties(GoogleCalendarUtils::getEventTypeProperties)
                .required(true),
            object(EXTENDED_PROPERTIES)
                .label("Extended properties")
                .properties(
                    object(PRIVATE)
                        .label("Private")
                        .description(
                            "Properties that are private to the copy of the event that appears on this calendar.")
                        .additionalProperties(string())
                        .required(false),
                    object(SHARED)
                        .label("Shared")
                        .description(
                            "Properties that are shared between copies of the event on other attendees' calendars.")
                        .additionalProperties(string())
                        .required(false))
                .required(false),
            object(GADGET)
                .label("Gadget")
                .properties(
                    string(DISPLAY)
                        .label("Display")
                        .description("The gadget's display mode. Deprecated.")
                        .options(
                            option("Icon", "icon",
                                "The gadget displays next to the event's title in the calendar view."),
                            option("Chip", "chip", "The gadget displays when the event is clicked."))
                        .required(false),
                    integer(HEIGHT)
                        .label("Height")
                        .description("The gadget's height in pixels.")
                        .minValue(0)
                        .required(false),
                    string(ICON_LINK)
                        .label("Icon link")
                        .description("The gadget's icon URL. The URL scheme must be HTTPS. Deprecated.")
                        .required(false),
                    string(LINK)
                        .label("Link")
                        .description("The gadget's URL. The URL scheme must be HTTPS. Deprecated.")
                        .required(false),
                    object(PREFERENCES)
                        .label("Preferences")
                        .description("Preferences.")
                        .additionalProperties(string())
                        .required(false),
                    string(TITLE)
                        .label("Title")
                        .description("The gadget's title. Deprecated.")
                        .required(false),
                    string(TYPE)
                        .label("Type")
                        .description("The gadget's type. Deprecated.")
                        .required(false),
                    integer(WIDTH)
                        .label("Width")
                        .description("The gadget's width in pixels.")
                        .minValue(0)
                        .required(false))
                .required(false),
            bool(GUEST_CAN_INVITE_OTHERS)
                .label("Guest can invite others")
                .description("Whether attendees other than the organizer can invite others to the event.")
                .defaultValue(true)
                .required(false),
            bool(GUEST_CAN_MODIFY)
                .label("Guest can modify")
                .description("Whether attendees other than the organizer can modify the event.")
                .defaultValue(false)
                .required(false),
            bool(GUEST_CAN_SEE_OTHER_GUESTS)
                .label("Guest can see other guests")
                .description("Whether attendees other than the organizer can see who the event's attendees are.")
                .defaultValue(true)
                .required(false),
            string(ID)
                .label("ID")
                .description(
                    "Opaque identifier of the event. If you do not specify an ID, it will be automatically generated " +
                        "by the server.")
                .minLength(5)
                .maxLength(1024)
                .required(false),
            string(LOCATION)
                .label("Location")
                .description("Geographic location of the event as free-form text.")
                .required(false),
            object(ORIGINAL_START_TIME)
                .label("Original start time")
                .properties(
                    DATE_PROPERTY,
                    DATE_TIME_PROPERTY,
                    TIME_ZONE_PROPERTY)
                .required(false),
            array(RECURRENCE)
                .label("Recurrence")
                .description(
                    "List of RRULE, EXRULE, RDATE and EXDATE lines for a recurring event, as specified in " +
                        "RFC5545. Note that DTSTART and DTEND lines are not allowed in this field; event start " +
                        "and end times are specified in the start and end fields. This field is omitted for " +
                        "single events or instances of recurring events.")
                .items(string())
                .required(false),
            object(REMINDERS)
                .label("Reminders")
                .properties(
                    array(OVERRIDES)
                        .label("Overrides")
                        .description(
                            "If the event doesn't use the default reminders, this lists the reminders specific to " +
                                "the event, or, if not set, indicates that no reminders are set for this event.")
                        .items(
                            object()
                                .properties(
                                    string(METHOD)
                                        .label("Method")
                                        .description("The method used by this reminder.")
                                        .options(
                                            option("Email", "email", "Reminders are sent via email."),
                                            option("Popup", "popup", "Reminders are sent via a UI popup."))
                                        .required(true),
                                    integer(MINUTES)
                                        .label("Minutes")
                                        .description(
                                            "Number of minutes before the start of the event when the reminder " +
                                                "should trigger.")
                                        .minValue(0)
                                        .maxValue(40320)
                                        .required(true)))
                        .required(false),
                    bool(USE_DEFAULT)
                        .label("Use default")
                        .description("Whether the default reminders of the calendar apply to the event.")
                        .required(false))
                .required(false),
            integer(SEQUENCE)
                .label("Sequence")
                .description("Sequence number as per iCalendar.")
                .required(false),
            object(SOURCE)
                .label("Source")
                .properties(
                    string(TITLE)
                        .label("Title")
                        .description("Title of the source; for example a title of a web page or an email subject.")
                        .required(false),
                    string(URL)
                        .label("URL")
                        .description(
                            "URL of the source pointing to a resource. The URL scheme must be HTTP or HTTPS.")
                        .required(false))
                .required(false),
            string(STATUS)
                .label("Status")
                .description("Status of the event.")
                .options(
                    option("Confirmed", "confirmed", "The event is confirmed."),
                    option("Tentative", TENTATIVE, "The event is tentatively confirmed"),
                    option("Cancelled", "cancelled", "The event is cancelled (deleted)."))
                .defaultValue("confirmed")
                .required(false),
            string(SUMMARY)
                .label("Summary")
                .description("Title of the event.")
                .required(false),
            string(TRANSPARENCY)
                .label("Transparency")
                .description("Whether the event blocks time on the calendar.")
                .options(
                    option("opaque", "opaque",
                        "The event does block time on the calendar. This is equivalent to setting Show me as to Busy " +
                            "in the Calendar UI."),
                    option("transparent", "transparent",
                        "The event does not block time on the calendar. This is equivalent to setting Show me as to " +
                            "Available in the Calendar UI."))
                .required(false),
            string(VISIBILITY)
                .label("Visibility")
                .description("Visibility of the event.")
                .options(
                    option(DEFAULT, DEFAULT, "Uses the default visibility for events on the calendar."),
                    option("public", "public",
                        "The event is public and event details are visible to all readers of the calendar."),
                    option("private", "private",
                        "The event is private and only event attendees may view event details."),
                    option("confidential", "confidential",
                        "The event is private. This value is provided for compatibility reasons."))
                .defaultValue(DEFAULT)
                .required(false))
        .outputSchema(EVENT_PROPERTY)
        .perform(GoogleCalendarCreateEventAction::perform);

    private GoogleCalendarCreateEventAction() {
    }

    public static Event perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws IOException {

        GoogleCalendarUtils.EventDateTimeCustom startEventDateTimeCustom = inputParameters.get(
            START, GoogleCalendarUtils.EventDateTimeCustom.class);

        GoogleCalendarUtils.EventDateTimeCustom originalStartTimeEventDateTimeCustom = inputParameters.get(
            ORIGINAL_START_TIME, GoogleCalendarUtils.EventDateTimeCustom.class);

        GoogleCalendarUtils.EventDateTimeCustom endEventDateTimeCustom = inputParameters.get(
            END, GoogleCalendarUtils.EventDateTimeCustom.class);

        EventDateTime startEventDateTime = GoogleCalendarUtils.createEventDateTime(startEventDateTimeCustom);
        EventDateTime originalStartEventDateTime = GoogleCalendarUtils.createEventDateTime(
            originalStartTimeEventDateTimeCustom);
        EventDateTime endEventDateTime = GoogleCalendarUtils.createEventDateTime(endEventDateTimeCustom);

        List<FileEntry> fileEntries = inputParameters.getFileEntries(ATTACHMENTS, List.of());

        List<EventAttachment> eventAttachments = new ArrayList<>();

        for (FileEntry fileEntry : fileEntries) {
            eventAttachments.add(
                new EventAttachment()
                    .setTitle(fileEntry.getName())
                    .setMimeType(fileEntry.getMimeType())
                    .setFileUrl(fileEntry.getUrl()));
        }

        Event event = new Event()
            .setAnyoneCanAddSelf(inputParameters.getBoolean(ANYONE_CAN_ADD_SELF))
            .setAttachments(eventAttachments)
            .setAttendees(inputParameters.getList(ATTENDEES, EventAttendee.class, List.of()))
            .setColorId(inputParameters.getString(COLOR_ID))
            .setConferenceData(inputParameters.get(CONFERENCE_DATA, ConferenceData.class))
            .setDescription(inputParameters.getString(DESCRIPTION))
            .setEnd(endEventDateTime)
            .setEventType(inputParameters.getString(EVENT_TYPE))
            .setExtendedProperties(inputParameters.get(EXTENDED_PROPERTIES, Event.ExtendedProperties.class))
            .setFocusTimeProperties(inputParameters.get(FOCUS_TIME_PROPERTIES, EventFocusTimeProperties.class))
            .setGadget(inputParameters.get(GADGET, Event.Gadget.class))
            .setGuestsCanInviteOthers(inputParameters.getBoolean(GUEST_CAN_INVITE_OTHERS))
            .setGuestsCanModify(inputParameters.getBoolean(GUEST_CAN_MODIFY))
            .setGuestsCanSeeOtherGuests(inputParameters.getBoolean(GUEST_CAN_SEE_OTHER_GUESTS))
            .setId(inputParameters.getString(ID))
            .setLocation(inputParameters.getString(LOCATION))
            .setOutOfOfficeProperties(inputParameters.get(OUT_OF_OFFICE_PROPERTIES, EventOutOfOfficeProperties.class))
            .setOriginalStartTime(originalStartEventDateTime)
            .setRecurrence(inputParameters.getList(RECURRENCE, String.class, List.of()))
            .setReminders(inputParameters.get(REMINDERS, Event.Reminders.class))
            .setSequence(inputParameters.getInteger(SEQUENCE))
            .setSource(inputParameters.get(SOURCE, Event.Source.class))
            .setStart(startEventDateTime)
            .setStatus(inputParameters.getString(STATUS))
            .setSummary(inputParameters.getString(SUMMARY))
            .setTransparency(inputParameters.getString(TRANSPARENCY))
            .setVisibility(inputParameters.getString(VISIBILITY))
            .setWorkingLocationProperties(
                inputParameters.get(WORKING_LOCATION_PROPERTIES, EventWorkingLocationProperties.class));

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        return calendar.events()
            .insert(inputParameters.getRequiredString(CALENDAR_ID), event)
            .setConferenceDataVersion(inputParameters.getInteger(CONFERENCE_DATA_VERSION))
            .setMaxAttendees(inputParameters.getInteger(MAX_ATTENDEES))
            .setSendUpdates(inputParameters.getString(SEND_UPDATES))
            .setSupportsAttachments(inputParameters.getBoolean(SUPPORTS_ATTACHMENTS))
            .execute();
    }
}
