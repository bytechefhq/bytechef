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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ALL_DAY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DESCRIPTION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.END;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_OUTPUT_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.START;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SUMMARY;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.createCustomEvent;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.createEventDateTime;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.getCalendarTimezone;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.getEvent;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.updateEvent;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarUpdateEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateEvent")
        .title("Update Event")
        .description("Updates event in Google Calendar.")
        .properties(
            CALENDAR_ID_PROPERTY,
            string(EVENT_ID)
                .label("Event ID")
                .description("ID of the event to update.")
                .options((OptionsFunction<String>) GoogleCalendarUtils::getEventIdOptions)
                .optionsLookupDependsOn(CALENDAR_ID)
                .required(true),
            string(SUMMARY)
                .label("Title")
                .description("New title of the event.")
                .required(false),
            bool(ALL_DAY)
                .label("All Day Event?")
                .required(false),
            date(START)
                .label("Start Date")
                .description("New start date of the event.")
                .displayCondition("%s == true".formatted(ALL_DAY))
                .required(true),
            date(END)
                .label("End Date")
                .description("New end date of the event.")
                .displayCondition("%s == true".formatted(ALL_DAY))
                .required(true),
            dateTime(START)
                .label("Start Date Time")
                .description(
                    "New (inclusive) start time of the event. For a recurring event, this is the start time of the " +
                        "first instance.")
                .displayCondition("%s == false".formatted(ALL_DAY))
                .required(true),
            dateTime(END)
                .label("End Date Time")
                .description(
                    "New (exclusive) end time of the event. For a recurring event, this is the end time of the " +
                        "first instance.")
                .displayCondition("%s == false".formatted(ALL_DAY))
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("New description of the event. Can contain HTML.")
                .required(false),
            array(ATTENDEES)
                .label("Attendees")
                .description("New attendees of the event.")
                .items(
                    string()
                        .label("Email")
                        .description("The attendee's email address."))
                .required(false))
        .output(outputSchema(EVENT_OUTPUT_PROPERTY))
        .perform(GoogleCalendarUpdateEventAction::perform);

    private GoogleCalendarUpdateEventAction() {
    }

    public static CustomEvent perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Calendar calendar = GoogleServices.getCalendar(connectionParameters);
        Event event = getEvent(inputParameters, calendar);

        List<String> attendees = inputParameters.getList(ATTENDEES, String.class, List.of());

        if (!attendees.isEmpty()) {
            List<EventAttendee> eventAttendees = attendees.stream()
                .map(attendee -> new EventAttendee().setEmail(attendee))
                .toList();

            List<EventAttendee> existingAttendees = event.getAttendees();
            if (existingAttendees == null || existingAttendees.isEmpty()) {
                event.setAttendees(eventAttendees);
            } else {
                event.getAttendees()
                    .addAll(eventAttendees);
            }
        }

        String description = inputParameters.getString(DESCRIPTION);

        if (description != null) {
            event.setDescription(description);
        }

        String summary = inputParameters.getString(SUMMARY);

        if (summary != null) {
            event.setSummary(summary);
        }

        String calendarTimezone = getCalendarTimezone(calendar);

        if (inputParameters.getBoolean(ALL_DAY) != null) {
            event.setEnd(createEventDateTime(inputParameters, END, calendarTimezone))
                .setStart(createEventDateTime(inputParameters, START, calendarTimezone));
        }

        return createCustomEvent(updateEvent(inputParameters, connectionParameters, event), calendarTimezone);
    }
}
