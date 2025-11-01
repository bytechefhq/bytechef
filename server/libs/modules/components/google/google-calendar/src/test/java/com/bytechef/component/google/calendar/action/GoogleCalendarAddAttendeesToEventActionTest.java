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
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
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

    private static final String CALENDAR_TIMEZONE = "Europe/Zagreb";
    private final ArgumentCaptor<Calendar> calendarArgumentCaptor = ArgumentCaptor.forClass(Calendar.class);
    private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic;
    private MockedStatic<GoogleServices> googleServicesMockedStatic;
    private MockedStatic<GoogleUtils> googleUtilsMockedStatic;
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private final Event mockedEvent = mock(Event.class);
    private final Parameters parameters = MockParametersFactory.create(
        Map.of(CALENDAR_ID, "calendarId", EVENT_ID, "eventId", ATTENDEES, List.of("attendee1", "attendee2")));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    void beforeEach() {
        googleCalendarUtilsMockedStatic = mockStatic(GoogleCalendarUtils.class);
        googleServicesMockedStatic = mockStatic(GoogleServices.class);
        googleUtilsMockedStatic = mockStatic(GoogleUtils.class);

        googleServicesMockedStatic.when(() -> GoogleServices.getCalendar(parametersArgumentCaptor.capture()))
            .thenReturn(mockedCalendar);
        googleCalendarUtilsMockedStatic
            .when(() -> GoogleCalendarUtils.updateEvent(
                parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture(), eventArgumentCaptor.capture()))
            .thenReturn(mockedEvent);
        googleUtilsMockedStatic
            .when(() -> GoogleUtils.getCalendarTimezone(calendarArgumentCaptor.capture()))
            .thenReturn(CALENDAR_TIMEZONE);
        googleCalendarUtilsMockedStatic
            .when(() -> GoogleCalendarUtils.createCustomEvent(
                eventArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedCustomEvent);
    }

    @AfterEach
    void afterEach() {
        googleCalendarUtilsMockedStatic.close();
        googleServicesMockedStatic.close();
        googleUtilsMockedStatic.close();
    }

    @Test
    void testPerform() {
        EventAttendee oldAttendee = new EventAttendee().setEmail("oldAttendee");
        List<EventAttendee> objects = new ArrayList<>();
        objects.add(oldAttendee);

        googleCalendarUtilsMockedStatic
            .when(() -> GoogleCalendarUtils.getEvent(
                parametersArgumentCaptor.capture(), calendarArgumentCaptor.capture()))
            .thenReturn(new Event().setAttendees(objects));

        CustomEvent result =
            GoogleCalendarAddAttendeesToEventAction.perform(parameters, parameters, mockedActionContext);

        assertEquals(mockedCustomEvent, result);
        assertEquals(List.of(parameters, parameters, parameters, parameters), parametersArgumentCaptor.getAllValues());
        assertEquals(List.of(mockedCalendar, mockedCalendar), calendarArgumentCaptor.getAllValues());
        assertEquals(CALENDAR_TIMEZONE, stringArgumentCaptor.getValue());

        List<EventAttendee> newAttendees = new ArrayList<>();

        newAttendees.add(new EventAttendee().setEmail("oldAttendee"));
        newAttendees.add(new EventAttendee().setEmail("attendee1"));
        newAttendees.add(new EventAttendee().setEmail("attendee2"));

        assertEquals(List.of(new Event().setAttendees(newAttendees), mockedEvent), eventArgumentCaptor.getAllValues());
    }

    @Test
    void testPerformForNoAttendees() {
        googleCalendarUtilsMockedStatic
            .when(() -> GoogleCalendarUtils.getEvent(
                parametersArgumentCaptor.capture(), calendarArgumentCaptor.capture()))
            .thenReturn(new Event());

        CustomEvent result = GoogleCalendarAddAttendeesToEventAction.perform(
            parameters, parameters, mockedActionContext);

        assertEquals(mockedCustomEvent, result);
        assertEquals(List.of(parameters, parameters, parameters, parameters), parametersArgumentCaptor.getAllValues());
        assertEquals(List.of(mockedCalendar, mockedCalendar), calendarArgumentCaptor.getAllValues());
        assertEquals(CALENDAR_TIMEZONE, stringArgumentCaptor.getValue());

        ArrayList<EventAttendee> newAttendees = new ArrayList<>();

        newAttendees.add(new EventAttendee().setEmail("attendee1"));
        newAttendees.add(new EventAttendee().setEmail("attendee2"));

        assertEquals(List.of(new Event().setAttendees(newAttendees), mockedEvent), eventArgumentCaptor.getAllValues());
    }
}
