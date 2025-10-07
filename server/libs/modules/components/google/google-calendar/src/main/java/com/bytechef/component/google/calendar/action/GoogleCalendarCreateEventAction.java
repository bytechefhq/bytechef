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

package com.bytechef.component.google.calendar.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ALL_DAY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ATTACHMENTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DESCRIPTION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.END;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_OUTPUT_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_INVITE_OTHERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_MODIFY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_SEE_OTHER_GUESTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LOCATION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.METHOD;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MINUTES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.REMINDERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.START;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SUMMARY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.USE_DEFAULT;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.createCustomEvent;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.createEventDateTime;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.constant.GoogleCalendarConstants;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttachment;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventReminder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarCreateEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createEvent")
        .title("Create Event")
        .description("Creates a new event in Google Calendar.")
        .properties(
            CALENDAR_ID_PROPERTY,
            string(SUMMARY)
                .label("Title")
                .description("Title of the event.")
                .required(false),
            bool(ALL_DAY)
                .label("All Day Event?")
                .defaultValue(false)
                .required(true),
            date(START)
                .label("Start Date")
                .description("The start date of the event.")
                .displayCondition("%s == true".formatted(ALL_DAY))
                .required(true),
            date(END)
                .label("End Date")
                .description("The end date of the event.")
                .displayCondition("%s == true".formatted(ALL_DAY))
                .required(true),
            dateTime(START)
                .label("Start Date Time")
                .description(
                    "The (inclusive) start time of the event. For a recurring event, this is the start time of the " +
                        "first instance.")
                .displayCondition("%s == false".formatted(ALL_DAY))
                .required(true),
            dateTime(END)
                .label("End Date Time")
                .description(
                    "The (exclusive) end time of the event. For a recurring event, this is the end time of the " +
                        "first instance.")
                .displayCondition("%s == false".formatted(ALL_DAY))
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Description of the event. Can contain HTML.")
                .required(false),
            string(LOCATION)
                .label("Location")
                .description("Geographic location of the event as free-form text.")
                .required(false),
            array(ATTACHMENTS)
                .label("Attachments")
                .items(fileEntry())
                .required(false),
            array(ATTENDEES)
                .label("Attendees")
                .description("The attendees of the event.")
                .items(
                    string()
                        .label("Email")
                        .description("The attendee's email address."))
                .required(false),
            bool(GUEST_CAN_INVITE_OTHERS)
                .label("Guest Can Invite Others")
                .description("Whether attendees other than the organizer can invite others to the event.")
                .defaultValue(true)
                .required(false),
            bool(GUEST_CAN_MODIFY)
                .label("Guest Can Modify")
                .description("Whether attendees other than the organizer can modify the event.")
                .defaultValue(false)
                .required(false),
            bool(GUEST_CAN_SEE_OTHER_GUESTS)
                .label("Guest Can See Other Guests")
                .description("Whether attendees other than the organizer can see who the event's attendees are.")
                .defaultValue(true)
                .required(false),
            SEND_UPDATES_PROPERTY,
            bool(USE_DEFAULT)
                .label("Use Default Reminders")
                .description("Whether the default reminders of the calendar apply to the event.")
                .defaultValue(true)
                .required(true),
            array(REMINDERS)
                .label("Reminders")
                .displayCondition("%s == false".formatted(USE_DEFAULT))
                .items(
                    object()
                        .properties(
                            string(METHOD)
                                .label("How Is Reminder Sent?")
                                .options(
                                    option("Email", "email", "Reminders are sent via email."),
                                    option("Popup", "popup", "Reminders are sent via a UI popup."))
                                .required(true),
                            integer(MINUTES)
                                .label("Minutes before reminder")
                                .description(
                                    "Number of minutes before the start of the event when the reminder " +
                                        "should trigger.")
                                .minValue(0)
                                .maxValue(40320)
                                .required(true)))
                .required(false))
        .output(outputSchema(EVENT_OUTPUT_PROPERTY))
        .perform(GoogleCalendarCreateEventAction::perform);

    private GoogleCalendarCreateEventAction() {
    }

    public static CustomEvent perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<EventAttachment> eventAttachments = new ArrayList<>();

        for (FileEntry fileEntry : inputParameters.getFileEntries(ATTACHMENTS, List.of())) {
            eventAttachments.add(
                new EventAttachment()
                    .setTitle(fileEntry.getName())
                    .setMimeType(fileEntry.getMimeType())
                    .setFileUrl(fileEntry.getUrl()));
        }

        List<EventAttendee> eventAttendees = inputParameters.getList(ATTENDEES, String.class, List.of())
            .stream()
            .map(attendee -> new EventAttendee().setEmail(attendee))
            .toList();

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);
        String timezone = GoogleCalendarUtils.getCalendarTimezone(calendar);

        Event event = new Event()
            .setAttachments(eventAttachments)
            .setAttendees(eventAttendees)
            .setDescription(inputParameters.getString(DESCRIPTION))
            .setEnd(createEventDateTime(inputParameters, END, timezone))
            .setGuestsCanInviteOthers(inputParameters.getBoolean(GUEST_CAN_INVITE_OTHERS))
            .setGuestsCanModify(inputParameters.getBoolean(GUEST_CAN_MODIFY))
            .setGuestsCanSeeOtherGuests(inputParameters.getBoolean(GUEST_CAN_SEE_OTHER_GUESTS))
            .setLocation(inputParameters.getString(LOCATION))
            .setReminders(
                new Event.Reminders()
                    .setUseDefault(inputParameters.getRequiredBoolean(USE_DEFAULT))
                    .setOverrides(inputParameters.getList(REMINDERS, EventReminder.class, List.of())))
            .setStart(createEventDateTime(inputParameters, START, timezone))
            .setSummary(inputParameters.getString(SUMMARY));

        Event newEvent;
        try {
            newEvent = calendar.events()
                .insert(inputParameters.getRequiredString(GoogleCalendarConstants.CALENDAR_ID), event)
                .setSendUpdates(inputParameters.getString(GoogleCalendarConstants.SEND_UPDATES))
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }

        return createCustomEvent(newEvent, timezone);
    }
}
