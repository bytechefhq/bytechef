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

import static com.bytechef.component.google.calendar.action.GoogleCalendarFindEventsAction.DATE_RANGE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FROM;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_RESULTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.Q;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TO;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.convertToDateViaSqlTimestamp;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.createCustomEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleCalendarFindEventsActionTest {

    private final ArgumentCaptor<String> calendarIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> eventTypesArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<Integer> maxResultsArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final Calendar.Events mockedCalendarEvents = mock(Calendar.Events.class);
    private final Calendar.Events.List mockedList = mock(Calendar.Events.List.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<String> qArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() throws IOException {
        List<String> eventTypes = List.of("default", "focusTime");

        Event e1 = new Event()
            .setStart(new EventDateTime()
                .setDateTime(new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 3, 11, 30)))))
            .setEnd(new EventDateTime()
                .setDateTime(new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 3, 12, 30)))));
        Event e2 = new Event()
            .setStart(new EventDateTime()
                .setDateTime(new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 3, 7, 0)))))
            .setEnd(new EventDateTime()
                .setDateTime(new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 5, 9, 0)))));
        Event e3 = new Event()
            .setStart(new EventDateTime()
                .setDateTime(new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 5, 8, 0)))))
            .setEnd(new EventDateTime()
                .setDateTime(new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 5, 9, 0)))));
        Event e4 = new Event()
            .setStart(new EventDateTime()
                .setDateTime(new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 5, 9, 30)))))
            .setEnd(new EventDateTime()
                .setDateTime(new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 5, 10, 30)))));
        Event e5 = new Event()
            .setStart(new EventDateTime().setDateTime(
                new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 7, 23, 6, 30)))))
            .setEnd(new EventDateTime().setDateTime(
                new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 7, 23, 7, 30)))));
        Event e6 = new Event()
            .setStart(new EventDateTime().setDateTime(
                new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 1, 9, 0)))))
            .setEnd(new EventDateTime().setDateTime(
                new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 1, 10, 0)))));
        Event e7 = new Event()
            .setStart(new EventDateTime().setDateTime(
                new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 2, 4, 30)))))
            .setEnd(new EventDateTime().setDateTime(
                new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 2, 5, 30)))));
        Event e8 = new Event()
            .setStart(new EventDateTime().setDateTime(
                new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 5, 11, 0)))))
            .setEnd(new EventDateTime().setDateTime(
                new DateTime(convertToDateViaSqlTimestamp(LocalDateTime.of(2024, 9, 5, 12, 0)))));

        when(mockedParameters.getRequiredString(CALENDAR_ID))
            .thenReturn("calendarId");
        when(mockedParameters.getList(EVENT_TYPE, String.class, List.of()))
            .thenReturn(eventTypes);
        when(mockedParameters.getInteger(MAX_RESULTS))
            .thenReturn(10);
        when(mockedParameters.getString(Q))
            .thenReturn("q");
        when(mockedParameters.getMap(DATE_RANGE, LocalDateTime.class, Map.of()))
            .thenReturn(
                Map.of(
                    FROM, LocalDateTime.of(2024, Month.SEPTEMBER, 3, 12, 0, 0),
                    TO, LocalDateTime.of(2024, Month.SEPTEMBER, 5, 10, 0, 0)));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic.when(() -> GoogleServices.getCalendar(mockedParameters))
                .thenReturn(mockedCalendar);

            when(mockedCalendar.events())
                .thenReturn(mockedCalendarEvents);
            when(mockedCalendarEvents.list(calendarIdArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setEventTypes(eventTypesArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setMaxResults(maxResultsArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setQ(qArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.execute())
                .thenReturn(new Events().setItems(List.of(e1, e2, e3, e4, e5, e6, e7, e8)));

            List<CustomEvent> result =
                GoogleCalendarFindEventsAction.perform(mockedParameters, mockedParameters, mock(ActionContext.class));

            assertEquals(
                List.of(createCustomEvent(e1), createCustomEvent(e2), createCustomEvent(e3), createCustomEvent(e4)),
                result);
            assertEquals("calendarId", calendarIdArgumentCaptor.getValue());
            assertEquals(10, maxResultsArgumentCaptor.getValue());
            assertEquals(eventTypes, eventTypesArgumentCaptor.getValue());
            assertEquals("q", qArgumentCaptor.getValue());
        }
    }
}
