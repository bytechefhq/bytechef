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
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
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

    private final ArgumentCaptor<Calendar> calendarArgumentCaptor = ArgumentCaptor.forClass(Calendar.class);
    private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private final Event mockedEvent = mock(Event.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(DESCRIPTION, "desc", ATTENDEES, List.of("attendee1", "attendee2")));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {
        String calendarTimezone = "Europe/Zagreb";

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic = mockStatic(GoogleCalendarUtils.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getCalendar(parametersArgumentCaptor.capture()))
                .thenReturn(mockedCalendar);
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.getCalendarTimezone(calendarArgumentCaptor.capture()))
                .thenReturn(calendarTimezone);
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.updateEvent(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture(),
                    eventArgumentCaptor.capture()))
                .thenReturn(mockedEvent);
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.createCustomEvent(
                    eventArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedCustomEvent);

            EventAttendee oldAttendee = new EventAttendee().setEmail("oldAttendee");
            ArrayList<EventAttendee> objects = new ArrayList<>();
            objects.add(oldAttendee);

            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.getEvent(
                    parametersArgumentCaptor.capture(), calendarArgumentCaptor.capture()))
                .thenReturn(new Event().setAttendees(objects)
                    .setSummary("summary"));

            CustomEvent result = GoogleCalendarUpdateEventAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(mockedCustomEvent, result);

            Event expectedEvent = new Event()
                .setAttendees(
                    List.of(
                        new EventAttendee().setEmail("oldAttendee"), new EventAttendee().setEmail("attendee1"),
                        new EventAttendee().setEmail("attendee2")))
                .setDescription("desc")
                .setSummary("summary");

            assertEquals(List.of(expectedEvent, mockedEvent), eventArgumentCaptor.getAllValues());
            assertEquals(
                List.of(mockedParameters, mockedParameters, mockedParameters, mockedParameters),
                parametersArgumentCaptor.getAllValues());
            assertEquals(calendarTimezone, stringArgumentCaptor.getValue());
            assertEquals(mockedCalendar, calendarArgumentCaptor.getValue());
        }
    }
}
