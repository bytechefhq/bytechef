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

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FOCUS_TIME;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.OUT_OF_OFFICE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SINGLE_EVENTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.WORKING_LOCATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.ColorDefinition;
import com.google.api.services.calendar.model.Colors;
import com.google.api.services.calendar.model.EventDateTime;
import java.io.IOException;
import java.time.LocalDate;
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

    private final Calendar.Colors mockedCalendarColors = mock(Calendar.Colors.class);
    private final Colors mockedColors = mock(Colors.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Calendar.Colors.Get mockedGet = mock(Calendar.Colors.Get.class);
    private final Calendar.CalendarList.List mockedList = mock(Calendar.CalendarList.List.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<String> minAccessRoleArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testConvertToDateViaSqlDate() {
        LocalDate dateToConvert = LocalDate.of(2010, 11, 10);

        Date date = GoogleCalendarUtils.convertToDateViaSqlDate(dateToConvert);
        java.util.Calendar calendar = java.util.Calendar.getInstance();

        calendar.setTime(date);

        assertEquals(2010, calendar.get(java.util.Calendar.YEAR));
        assertEquals(10, calendar.get(java.util.Calendar.MONTH));
        assertEquals(10, calendar.get(java.util.Calendar.DAY_OF_MONTH));
    }

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
    void testCreateEventDateTime() {
        GoogleCalendarUtils.EventDateTimeCustom eventDateTimeCustom =
            new GoogleCalendarUtils.EventDateTimeCustom(LocalDate.of(2010, 11, 10),
                LocalDateTime.of(2010, 11, 10, 8, 20), "timeZone");

        EventDateTime eventDateTime = GoogleCalendarUtils.createEventDateTime(eventDateTimeCustom);

        assertEquals(new DateTime(GoogleCalendarUtils.convertToDateViaSqlDate(eventDateTimeCustom.date())),
            eventDateTime.getDate());
        assertEquals(new DateTime(GoogleCalendarUtils.convertToDateViaSqlTimestamp(eventDateTimeCustom.dateTime())),
            eventDateTime.getDateTime());
        assertEquals("timeZone", eventDateTime.getTimeZone());
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
                GoogleCalendarUtils.getCalendarIdOptions(mockedParameters, mockedParameters, anyString(),
                    mockedContext);

            assertEquals("writer", minAccessRoleArgumentCaptor.getValue());

            assertEquals(1, result.size());

            Option<String> option = result.getFirst();

            assertEquals("summary", option.getLabel());
            assertEquals("id", option.getValue());
        }
    }

    @Test
    void testGetColorOptions() throws IOException {

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getCalendar(mockedParameters))
                .thenReturn(mockedCalendar);

            when(mockedCalendar.colors())
                .thenReturn(mockedCalendarColors);
            when(mockedCalendarColors.get())
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(
                    new Colors().setEvent(
                        Map.of("1", new ColorDefinition().setForeground("f")
                            .setBackground("b"))));

            List<Option<String>> result =
                GoogleCalendarUtils.getColorOptions(mockedParameters, mockedParameters, anyString(), mockedContext);

            assertEquals(1, result.size());

            Option<String> firstOption = result.getFirst();

            assertEquals("1", firstOption.getLabel());
            assertEquals("1", firstOption.getValue());
            assertEquals("Background: b, Foreground: f", firstOption.getDescription()
                .get());
        }
    }

    @Test
    void testGetEventTypePropertiesForFocusTime() {
        when(mockedParameters.getString(EVENT_TYPE))
            .thenReturn(FOCUS_TIME);

        List<? extends Property.ValueProperty<?>> focusTimeProperties =
            GoogleCalendarUtils.getEventTypeProperties(mockedParameters, mockedParameters, mockedContext);

        assertEquals(1, focusTimeProperties.size());
        assertEquals(GoogleCalendarUtils.focusTimeProperties, focusTimeProperties.getFirst());
    }

    @Test
    void testGetEventTypePropertiesForOutOfOffice() {
        when(mockedParameters.getString(EVENT_TYPE))
            .thenReturn(OUT_OF_OFFICE);

        List<? extends Property.ValueProperty<?>> focusTimeProperties =
            GoogleCalendarUtils.getEventTypeProperties(mockedParameters, mockedParameters, mockedContext);

        assertEquals(1, focusTimeProperties.size());
        assertEquals(GoogleCalendarUtils.outOfOfficeProperties, focusTimeProperties.getFirst());
    }

    @Test
    void tesGetEventTypePropertiesForWorkingLocation() {
        when(mockedParameters.getString(EVENT_TYPE))
            .thenReturn(WORKING_LOCATION);

        List<? extends Property.ValueProperty<?>> focusTimeProperties =
            GoogleCalendarUtils.getEventTypeProperties(mockedParameters, mockedParameters, mockedContext);

        assertEquals(1, focusTimeProperties.size());
        assertEquals(GoogleCalendarUtils.workingLocationProperties, focusTimeProperties.getFirst());
    }

    @Test
    void testGetOrderByOptions() {
        when(mockedParameters.getBoolean(SINGLE_EVENTS)).thenReturn(false);

        List<Option<String>> orderByOptions =
            GoogleCalendarUtils.getOrderByOptions(mockedParameters, mockedParameters, anyString(), mockedContext);

        assertEquals(1, orderByOptions.size());
        assertEquals("Updated", orderByOptions.getFirst().getLabel());
        assertEquals("updated", orderByOptions.getFirst().getValue());
        assertEquals("Order by last modification time (ascending).",
            orderByOptions.getFirst().getDescription().get());
    }

    @Test
    void tesGetOrderByOptionsForSingleEvents() {
        when(mockedParameters.getBoolean(SINGLE_EVENTS)).thenReturn(true);

        List<Option<String>> orderByOptions =
            GoogleCalendarUtils.getOrderByOptions(mockedParameters, mockedParameters, anyString(), mockedContext);

        assertEquals(2, orderByOptions.size());
        assertEquals("Updated", orderByOptions.getFirst().getLabel());
        assertEquals("updated", orderByOptions.getFirst().getValue());
        assertEquals("Order by last modification time (ascending).",
            orderByOptions.getFirst().getDescription().get());

        assertEquals("Start time", orderByOptions.get(1).getLabel());
        assertEquals("startTime", orderByOptions.get(1).getValue());
        assertEquals(
            "Order by the start date/time (ascending). This is only available when querying single events " +
                "(i.e. the parameter singleEvents is True)", orderByOptions.get(1).getDescription().get());
    }
}
