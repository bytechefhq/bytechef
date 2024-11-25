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

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DESCRIPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleCalendarUpdateEventActionTest {

    private final ArgumentCaptor<Parameters> parameters1ArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<Parameters> parameters2ArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<Event> event1ArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private final ArgumentCaptor<Event> event2ArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private final Event mockedEvent = mock(Event.class);
    private final Parameters parameters = MockParametersFactory.create(
        Map.of(DESCRIPTION, "desc", ATTENDEES, List.of("attendee1", "attendee2")));

    @Test
    void testPerform() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic = mockStatic(GoogleCalendarUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getCalendar(parameters))
                .thenReturn(mockedCalendar);
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.updateEvent(parameters1ArgumentCaptor.capture(),
                    parameters2ArgumentCaptor.capture(), event2ArgumentCaptor.capture()))
                .thenReturn(mockedEvent);
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.createCustomEvent(event1ArgumentCaptor.capture()))
                .thenReturn(mockedCustomEvent);

            EventAttendee oldAttendee = new EventAttendee().setEmail("oldAttendee");
            ArrayList<EventAttendee> objects = new ArrayList<>();
            objects.add(oldAttendee);

            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.getEvent(parameters, parameters))
                .thenReturn(new Event().setAttendees(objects)
                    .setSummary("summary"));

            CustomEvent result =
                GoogleCalendarUpdateEventAction.perform(parameters, parameters, mockedActionContext);

            assertEquals(mockedCustomEvent, result);

            ArrayList<EventAttendee> newAttendees = new ArrayList<>();

            newAttendees.add(new EventAttendee().setEmail("oldAttendee"));
            newAttendees.add(new EventAttendee().setEmail("attendee1"));
            newAttendees.add(new EventAttendee().setEmail("attendee2"));

            Event updatedEvent = event2ArgumentCaptor.getValue();

            assertEquals(newAttendees, updatedEvent.getAttendees());
            assertEquals("desc", updatedEvent.getDescription());
            assertEquals("summary", updatedEvent.getSummary());

            assertEquals(mockedEvent, event1ArgumentCaptor.getValue());
            assertEquals(parameters, parameters1ArgumentCaptor.getValue());
            assertEquals(parameters, parameters2ArgumentCaptor.getValue());
        }
    }
}
