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

package com.bytechef.component.google.calendar.util;

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ALL_DAY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.START;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.EventDateTime;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleCalendarUtilsTest {

    private final Calendar mockedCalendar = mock(Calendar.class);
    private final Calendar.CalendarList mockedCalendarList = mock(Calendar.CalendarList.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Calendar.CalendarList.List mockedList = mock(Calendar.CalendarList.List.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<String> minAccessRoleArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testConvertToDateViaSqlTimestamp() {
        LocalDateTime dateToConvert = LocalDateTime.of(2010, 11, 10, 8, 20);

        Date date = GoogleCalendarUtils.convertToDateViaSqlTimestamp(dateToConvert);
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date);

        assertEquals(2010, calendar.get(java.util.Calendar.YEAR));
        assertEquals(10, calendar.get(java.util.Calendar.MONTH));
        assertEquals(10, calendar.get(java.util.Calendar.DAY_OF_MONTH));
        assertEquals(8, calendar.get(java.util.Calendar.HOUR));
        assertEquals(20, calendar.get(java.util.Calendar.MINUTE));
        assertEquals(0, calendar.get(java.util.Calendar.SECOND));
    }

    @Test
    void testCreateEventDateTimeForAllDayEvent() {
        Date date = new Date();

        when(mockedParameters.getRequiredBoolean(ALL_DAY))
            .thenReturn(true);
        when(mockedParameters.getRequiredDate(START))
            .thenReturn(date);

        EventDateTime eventDateTime = GoogleCalendarUtils.createEventDateTime(mockedParameters, START);

        assertEquals(new DateTime(date), eventDateTime.getDate());
        assertNull(eventDateTime.getDateTime());
        assertNull(eventDateTime.getTimeZone());
    }

    @Test
    void testCreateEventDateTimeForNotAllDayEvent() {
        LocalDateTime localDateTime = LocalDateTime.of(2000, 11, 11, 22, 20);

        when(mockedParameters.getRequiredBoolean(ALL_DAY))
            .thenReturn(false);
        when(mockedParameters.getRequiredLocalDateTime(START))
            .thenReturn(localDateTime);

        EventDateTime eventDateTime = GoogleCalendarUtils.createEventDateTime(mockedParameters, START);

        assertEquals(new DateTime(GoogleCalendarUtils.convertToDateViaSqlTimestamp(localDateTime)),
            eventDateTime.getDateTime());
        assertNull(eventDateTime.getDate());
        assertNull(eventDateTime.getTimeZone());
    }

    @Test
    void testGetCalendarIdOptions() throws IOException {
        List<CalendarListEntry> calendarListEntries =
            List.of(new CalendarListEntry().setSummary("summary")
                .setId("id"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getCalendar(mockedParameters))
                .thenReturn(mockedCalendar);

            when(mockedCalendar.calendarList())
                .thenReturn(mockedCalendarList);
            when(mockedCalendarList.list())
                .thenReturn(mockedList);
            when(mockedList.setMinAccessRole(minAccessRoleArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.execute())
                .thenReturn(new CalendarList().setItems(calendarListEntries));

            List<Option<String>> result =
                GoogleCalendarUtils.getCalendarIdOptions(
                    mockedParameters, mockedParameters, Map.of(), anyString(), mockedContext);

            assertEquals("writer", minAccessRoleArgumentCaptor.getValue());

            assertEquals(1, result.size());

            Option<String> option = result.getFirst();

            assertEquals("summary", option.getLabel());
            assertEquals("id", option.getValue());
        }
    }

}
