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

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_RESULTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.Q;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_MAX;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_MIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleCalendarFindEventsActionTest extends AbstractGoogleCalendarActionTest {

    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> eventTypesArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<Integer> maxResultsArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final Events mockedEvents = mock(Events.class);
    private final Calendar.Events mockedCalendarEvents = mock(Calendar.Events.class);
    private final Calendar.Events.List mockedList = mock(Calendar.Events.List.class);
    private final ArgumentCaptor<String> qArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<DateTime> timeMaxArgumentCaptor = ArgumentCaptor.forClass(DateTime.class);
    private final ArgumentCaptor<DateTime> timeMinArgumentCaptor = ArgumentCaptor.forClass(DateTime.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() throws IOException {
        List<String> eventTypes = List.of("default", "focusTime");

        LocalDateTime localDateTime = LocalDateTime.of(2015, Month.AUGUST, 15, 10, 10, 10);
        Date date = GoogleCalendarUtils.convertToDateViaSqlTimestamp(localDateTime);

        when(mockedParameters.getList(EVENT_TYPE, String.class, List.of()))
            .thenReturn(eventTypes);
        when(mockedParameters.getInteger(MAX_RESULTS))
            .thenReturn(10);
        when(mockedParameters.getString(Q))
            .thenReturn("q");
        when(mockedParameters.getLocalDateTime(TIME_MAX))
            .thenReturn(localDateTime);
        when(mockedParameters.getLocalDateTime(TIME_MIN))
            .thenReturn(localDateTime);

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
        when(mockedList.setTimeMax(timeMaxArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setTimeMin(timeMinArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.execute())
            .thenReturn(mockedEvents);

        try (MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic =
            mockStatic(GoogleCalendarUtils.class)) {
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.convertToDateViaSqlTimestamp(any()))
                .thenReturn(date);

            Events result = GoogleCalendarFindEventsAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedEvents, result);
            assertEquals(10, maxResultsArgumentCaptor.getValue());
            assertEquals(eventTypes, eventTypesArgumentCaptor.getValue());
            assertEquals("q", qArgumentCaptor.getValue());
            assertEquals(new DateTime(date), timeMaxArgumentCaptor.getValue());
            assertEquals(new DateTime(date), timeMinArgumentCaptor.getValue());
        }
    }
}
