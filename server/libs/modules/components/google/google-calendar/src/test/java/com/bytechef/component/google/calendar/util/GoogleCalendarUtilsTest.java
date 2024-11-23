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

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ALL_DAY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DATE_RANGE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FROM;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_RESULTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.Q;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.START;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TO;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.convertToDateViaSqlTimestamp;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.createCustomEvent;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.createEventDateTime;
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
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttachment;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleCalendarUtilsTest {

    private final ArgumentCaptor<String> calendarIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final Calendar.Events mockedEvents = mock(Calendar.Events.class);
    private final Calendar.Events.List mockedEventsList = mock(Calendar.Events.List.class);
    private final Calendar.CalendarList mockedCalendarList = mock(Calendar.CalendarList.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Calendar.CalendarList.List mockedList = mock(Calendar.CalendarList.List.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<String> minAccessRoleArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> eventTypesArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<Integer> maxResultsArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final Calendar.Events mockedCalendarEvents = mock(Calendar.Events.class);
    private final ArgumentCaptor<String> qArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testConvertToTemporalFromEventDateTimeForLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.of(2000, 1, 1, 1, 1, 1);

        EventDateTime eventDateTime =
            new EventDateTime().setDateTime(new DateTime(convertToDateViaSqlTimestamp(localDateTime)));

        Temporal convertedDateTime = GoogleCalendarUtils.convertToTemporalFromEventDateTime(eventDateTime);

        assertEquals(localDateTime, convertedDateTime);
    }

    @Test
    void testConvertToTemporalFromEventDateTimeForLocalDate() {
        LocalDate localDate = LocalDate.of(2000, 1, 1);

        EventDateTime eventDateTime =
            new EventDateTime().setDate(new DateTime("2000-01-01"));

        Temporal convertedDateTime = GoogleCalendarUtils.convertToTemporalFromEventDateTime(eventDateTime);

        assertEquals(localDate, convertedDateTime);
    }

    @Test
    void testConvertToDateViaSqlTimestamp() {
        LocalDateTime dateToConvert = LocalDateTime.of(2010, 11, 10, 8, 20);

        Date date = convertToDateViaSqlTimestamp(dateToConvert);
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

        EventDateTime eventDateTime = createEventDateTime(mockedParameters, START);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        assertEquals(new DateTime(simpleDateFormat.format(date)), eventDateTime.getDate());
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

        EventDateTime eventDateTime = createEventDateTime(mockedParameters, START);

        assertEquals(new DateTime(convertToDateViaSqlTimestamp(localDateTime)),
            eventDateTime.getDateTime());
        assertNull(eventDateTime.getDate());
        assertNull(eventDateTime.getTimeZone());
    }

    @Test
    void testCreateCustomEvent() {
        LocalDateTime startTime = LocalDateTime.of(2000, 1, 1, 1, 1, 1);
        LocalDateTime endTime = LocalDateTime.of(2000, 2, 2, 2, 2, 2);

        Event event = new Event()
            .setICalUID("icaluid")
            .setId("id")
            .setSummary("summary")
            .setDescription("description")
            .setStart(new EventDateTime().setDateTime(new DateTime(convertToDateViaSqlTimestamp(startTime))))
            .setEnd(new EventDateTime().setDateTime(new DateTime(convertToDateViaSqlTimestamp(endTime))))
            .setEtag("etag")
            .setEventType("eventType")
            .setHtmlLink("htmlLink")
            .setHangoutLink("hangoutLink")
            .setStatus("status")
            .setAttachments(List.of(new EventAttachment().setTitle("title")))
            .setAttendees(List.of(new EventAttendee()))
            .setReminders(new Event.Reminders().setUseDefault(false));

        GoogleCalendarUtils.CustomEvent customEvent = createCustomEvent(event);

        assertEquals(event.getICalUID(), customEvent.iCalUID());
        assertEquals(event.getId(), customEvent.id());
        assertEquals(event.getSummary(), customEvent.summary());
        assertEquals(event.getDescription(), customEvent.description());
        assertEquals(startTime, customEvent.startTime());
        assertEquals(endTime, customEvent.endTime());
        assertEquals(event.getEtag(), customEvent.etag());
        assertEquals(event.getEventType(), customEvent.eventType());
        assertEquals(event.getHtmlLink(), customEvent.htmlLink());
        assertEquals(event.getHangoutLink(), customEvent.hangoutLink());
        assertEquals(event.getStatus(), customEvent.status());
        assertEquals(event.getAttachments(), customEvent.attachments());
        assertEquals(event.getAttendees(), customEvent.attendeeList());
        assertEquals(event.getReminders(), customEvent.reminders());
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

    @Test
    void testGetCustomEvents() throws IOException {
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
        Event e9 = new Event()
            .setStart(new EventDateTime().setDate(new DateTime("2024-09-04")))
            .setEnd(new EventDateTime().setDate(new DateTime("2024-09-04")));

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
                .thenReturn(mockedEventsList);
            when(mockedEventsList.setEventTypes(eventTypesArgumentCaptor.capture()))
                .thenReturn(mockedEventsList);
            when(mockedEventsList.setMaxResults(maxResultsArgumentCaptor.capture()))
                .thenReturn(mockedEventsList);
            when(mockedEventsList.setQ(qArgumentCaptor.capture()))
                .thenReturn(mockedEventsList);
            when(mockedEventsList.execute())
                .thenReturn(new Events().setItems(List.of(e1, e2, e3, e4, e5, e6, e7, e8, e9)));

            List<GoogleCalendarUtils.CustomEvent> result =
                GoogleCalendarUtils.getCustomEvents(mockedParameters, mockedParameters);

            assertEquals(
                List.of(createCustomEvent(e1), createCustomEvent(e2), createCustomEvent(e3), createCustomEvent(e4),
                    createCustomEvent(e9)),
                result);
            assertEquals("calendarId", calendarIdArgumentCaptor.getValue());
            assertEquals(10, maxResultsArgumentCaptor.getValue());
            assertEquals(eventTypes, eventTypesArgumentCaptor.getValue());
            assertEquals("q", qArgumentCaptor.getValue());
        }
    }

    @Test
    void testGetEventIdOptions() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getCalendar(mockedParameters))
                .thenReturn(mockedCalendar);

            when(mockedCalendar.events())
                .thenReturn(mockedEvents);
            when(mockedEvents.list(calendarIdArgumentCaptor.capture()))
                .thenReturn(mockedEventsList);
            Events events = new Events().setItems(
                List.of(
                    new Event().setId("123")
                        .setSummary("summary"),
                    new Event().setId("abc")));
            when(mockedEventsList.execute())
                .thenReturn(events);
            List<Option<String>> result =
                GoogleCalendarUtils.getEventIdOptions(
                    mockedParameters, mockedParameters, Map.of(), anyString(), mockedContext);

            List<Option<String>> expectedResult = new ArrayList<>();

            expectedResult.add(option("summary", "123"));
            expectedResult.add(option("abc", "abc"));

            assertEquals(expectedResult, result);
        }
    }
}
