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
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EMAIL;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_OUTPUT_PROPERTY;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.createCustomEvent;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarAddAttendeesToEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addAttendeesToEvent")
        .title("Add Attendees to Event")
        .description("Invites one or more person to an existing event.")
        .properties(
            CALENDAR_ID_PROPERTY,
            string(EVENT_ID)
                .label("Event")
                .description("Event to add attendees to.")
                .options((ActionOptionsFunction<String>) GoogleCalendarUtils::getEventIdOptions)
                .optionsLookupDependsOn(CALENDAR_ID)
                .required(true),
            array(ATTENDEES)
                .label("Attendees")
                .description("The attendees of the event.")
                .items(
                    string(EMAIL)
                        .label("Email")
                        .description("The attendee's email address."))
                .required(true))
        .output(outputSchema(EVENT_OUTPUT_PROPERTY))
        .perform(GoogleCalendarAddAttendeesToEventAction::perform);

    private GoogleCalendarAddAttendeesToEventAction() {
    }

    public static CustomEvent perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        String calendarId = inputParameters.getRequiredString(CALENDAR_ID);
        String eventId = inputParameters.getRequiredString(EVENT_ID);

        Event event = calendar
            .events()
            .get(calendarId, eventId)
            .execute();

        List<String> newAttendees = inputParameters.getList(ATTENDEES, String.class, List.of());

        event.getAttendees()
            .addAll(
                newAttendees
                    .stream()
                    .map(attendee -> new EventAttendee().setEmail(attendee))
                    .toList());

        Event updatedEvent = calendar
            .events()
            .update(calendarId, eventId, event)
            .execute();

        return createCustomEvent(updatedEvent);
    }
}
