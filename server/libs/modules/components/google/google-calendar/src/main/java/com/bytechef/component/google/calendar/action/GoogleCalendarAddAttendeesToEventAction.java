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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ADD_ATTENDEES_TO_EVENT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ADD_ATTENDEES_TO_EVENT_DESCRIPTION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ADD_ATTENDEES_TO_EVENT_TITLE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_OUTPUT_PROPERTY;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.createCustomEvent;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.getEvent;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.updateEvent;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.definition.BaseOutputDefinition.OutputSchema;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarAddAttendeesToEventAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        CALENDAR_ID_PROPERTY,
        string(EVENT_ID)
            .label("Event ID")
            .description("ID of the event to add attendees to.")
            .options((ActionOptionsFunction<String>) GoogleCalendarUtils::getEventIdOptions)
            .optionsLookupDependsOn(CALENDAR_ID)
            .required(true),
        array(ATTENDEES)
            .label("Attendees")
            .description("The attendees of the event.")
            .items(
                string()
                    .label("Email")
                    .description("The attendee's email address."))
            .required(true)
    };

    public static final OutputSchema<ObjectProperty> OUTPUT_SCHEMA = outputSchema(EVENT_OUTPUT_PROPERTY);

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ADD_ATTENDEES_TO_EVENT)
        .title(ADD_ATTENDEES_TO_EVENT_TITLE)
        .description(ADD_ATTENDEES_TO_EVENT_DESCRIPTION)
        .properties(PROPERTIES)
        .output(OUTPUT_SCHEMA)
        .perform(GoogleCalendarAddAttendeesToEventAction::perform);

    private GoogleCalendarAddAttendeesToEventAction() {
    }

    public static CustomEvent perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException {

        Event event = getEvent(inputParameters, connectionParameters);

        List<String> newAttendees = inputParameters.getList(ATTENDEES, String.class, List.of());

        List<EventAttendee> newEventAttendees = newAttendees
            .stream()
            .map(attendee -> new EventAttendee().setEmail(attendee))
            .toList();

        List<EventAttendee> attendees = event.getAttendees();

        if (attendees == null) {
            event.setAttendees(newEventAttendees);
        } else {
            event.getAttendees()
                .addAll(newEventAttendees);
        }

        return createCustomEvent(updateEvent(inputParameters, connectionParameters, event));
    }
}
