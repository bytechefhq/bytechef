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
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_ID;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleCalendarAddAttendeesToEventActionTest {

    private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic;
    private MockedStatic<GoogleServices> googleServicesMockedStatic;
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private final Event mockedEvent = mock(Event.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final Parameters parameters = MockParametersFactory.create(
        Map.of(CALENDAR_ID, "calendarId", EVENT_ID, "eventId", ATTENDEES, List.of("attendee1", "attendee2")));

    @BeforeEach
    void beforeEach() {
        googleCalendarUtilsMockedStatic = mockStatic(GoogleCalendarUtils.class);
        googleServicesMockedStatic = mockStatic(GoogleServices.class);

        googleServicesMockedStatic.when(() -> GoogleServices.getCalendar(parametersArgumentCaptor.capture()))
            .thenReturn(mockedCalendar);
        googleCalendarUtilsMockedStatic
            .when(() -> GoogleCalendarUtils.updateEvent(
                parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture(), eventArgumentCaptor.capture()))
            .thenReturn(mockedEvent);
        googleCalendarUtilsMockedStatic
            .when(() -> GoogleCalendarUtils.createCustomEvent(eventArgumentCaptor.capture()))
            .thenReturn(mockedCustomEvent);
    }

    @AfterEach
    void afterEach() {
        googleCalendarUtilsMockedStatic.close();
        googleServicesMockedStatic.close();
    }

    @Test
    void testPerform() throws IOException {
        EventAttendee oldAttendee = new EventAttendee().setEmail("oldAttendee");
        List<EventAttendee> objects = new ArrayList<>();
        objects.add(oldAttendee);

        googleCalendarUtilsMockedStatic
            .when(() -> GoogleCalendarUtils.getEvent(
                parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture()))
            .thenReturn(new Event().setAttendees(objects));

        CustomEvent result =
            GoogleCalendarAddAttendeesToEventAction.perform(parameters, parameters, mockedActionContext);

        assertEquals(mockedCustomEvent, result);

        assertEquals(List.of(parameters, parameters, parameters, parameters), parametersArgumentCaptor.getAllValues());

        List<EventAttendee> newAttendees = new ArrayList<>();

        newAttendees.add(new EventAttendee().setEmail("oldAttendee"));
        newAttendees.add(new EventAttendee().setEmail("attendee1"));
        newAttendees.add(new EventAttendee().setEmail("attendee2"));

        assertEquals(List.of(new Event().setAttendees(newAttendees), mockedEvent), eventArgumentCaptor.getAllValues());
    }

    @Test
    void testPerformForNoAttendees() throws IOException {
        googleCalendarUtilsMockedStatic
            .when(() -> GoogleCalendarUtils.getEvent(
                parametersArgumentCaptor.capture(),
                parametersArgumentCaptor.capture()))
            .thenReturn(new Event());

        CustomEvent result =
            GoogleCalendarAddAttendeesToEventAction.perform(parameters, parameters, mockedActionContext);

        assertEquals(mockedCustomEvent, result);
        assertEquals(List.of(parameters, parameters, parameters, parameters), parametersArgumentCaptor.getAllValues());

        ArrayList<EventAttendee> newAttendees = new ArrayList<>();

        newAttendees.add(new EventAttendee().setEmail("attendee1"));
        newAttendees.add(new EventAttendee().setEmail("attendee2"));

        assertEquals(List.of(new Event().setAttendees(newAttendees), mockedEvent), eventArgumentCaptor.getAllValues());
    }
}
