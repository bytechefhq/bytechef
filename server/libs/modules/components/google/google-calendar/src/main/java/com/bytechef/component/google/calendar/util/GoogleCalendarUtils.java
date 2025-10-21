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

package com.bytechef.component.google.calendar.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ALL_DAY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DATE_RANGE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.END;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FROM;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LOCAL_TIME_MAX;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LOCAL_TIME_MIN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_RESULTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.Q;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TO;
import static com.bytechef.google.commons.GoogleUtils.getCalendarTimezone;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.bytechef.component.definition.Context;
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarUtils {

    private GoogleCalendarUtils() {
    }

    public static Date convertLocalDateTimeToDateInTimezone(LocalDateTime dateToConvert, String timezone) {
        if (dateToConvert == null) {
            return null;
        } else {
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime zonedDateTime = dateToConvert.atZone(zoneId);

            return Date.from(zonedDateTime.toInstant());
        }
    }

    public static Temporal convertToTemporalFromEventDateTime(EventDateTime eventDateTime, String timezone) {
        if (eventDateTime != null) {
            DateTime dateTime = eventDateTime.getDateTime();

            if (dateTime != null) {
                return LocalDateTime.ofInstant(Instant.parse(dateTime.toString()), ZoneId.of(timezone));
            }

            DateTime allDayDate = eventDateTime.getDate();

            return LocalDate.parse(allDayDate.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
        } else {
            return null;
        }
    }

    public static EventDateTime createEventDateTime(Parameters inputParameters, String time, String timezone) {
        EventDateTime eventDateTime = new EventDateTime();

        if (inputParameters.getRequiredBoolean(ALL_DAY)) {
            Date date = inputParameters.getRequiredDate(time);

            if (time.equals(END)) {
                java.util.Calendar javaCalendar = java.util.Calendar.getInstance();
                javaCalendar.setTime(date);
                javaCalendar.add(java.util.Calendar.DAY_OF_MONTH, 1);

                date = javaCalendar.getTime();
            }

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

            eventDateTime.setDate(new DateTime(simpleDateFormat.format(date)));
        } else {
            eventDateTime.setDateTime(
                new DateTime(
                    convertLocalDateTimeToDateInTimezone(inputParameters.getRequiredLocalDateTime(time), timezone)));
        }

        return eventDateTime;
    }

    public static CustomEvent createCustomEvent(Event event, String timezone) {
        return new CustomEvent(
            event.getICalUID(), event.getId(), event.getSummary(), event.getDescription(),
            convertToTemporalFromEventDateTime(event.getStart(), timezone),
            convertToTemporalFromEventDateTime(event.getEnd(), timezone),
            event.getEtag(), event.getEventType(), event.getHtmlLink(), event.getStatus(), event.getLocation(),
            event.getHangoutLink(), event.getAttendees(), event.getAttachments(), event.getReminders());
    }

    public static List<Option<String>> getCalendarIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);
        List<CalendarListEntry> calendarListEntries = fetchAllCalendarListEntries(calendar);

        return calendarListEntries.stream()
            .map(entry -> option(entry.getSummary(), entry.getId()))
            .collect(Collectors.toList());
    }

    private static List<CalendarListEntry> fetchAllCalendarListEntries(Calendar calendar) {
        List<CalendarListEntry> calendarListEntries = new ArrayList<>();
        String nextPageToken = null;

        do {
            CalendarList calendarList = null;
            try {
                calendarList = calendar
                    .calendarList()
                    .list()
                    .setMinAccessRole("writer")
                    .setMaxResults(250)
                    .setPageToken(nextPageToken)
                    .execute();
            } catch (IOException e) {
                throw translateGoogleIOException(e);
            }

            calendarListEntries.addAll(calendarList.getItems());
            nextPageToken = calendarList.getNextPageToken();
        } while (nextPageToken != null);

        return calendarListEntries;
    }

    public static List<CustomEvent> getCustomEvents(Parameters inputParameters, Parameters connectionParameters) {
        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        List<Event> items;
        try {
            items = calendar.events()
                .list(inputParameters.getRequiredString(CALENDAR_ID))
                .setEventTypes(inputParameters.getList(EVENT_TYPE, String.class, List.of()))
                .setMaxResults(inputParameters.getInteger(MAX_RESULTS))
                .setQ(inputParameters.getString(Q))
                .execute()
                .getItems();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }

        String calendarTimezone = getCalendarTimezone(calendar);
        Map<String, LocalDateTime> timePeriod = inputParameters.getMap(DATE_RANGE, LocalDateTime.class, Map.of());

        LocalDateTime from = timePeriod.get(FROM);
        LocalDateTime to = timePeriod.get(TO);

        return convertToCustomEvents(filterEvents(from, to, items, calendarTimezone), calendarTimezone);
    }

    private static List<Event> filterEvents(LocalDateTime from, LocalDateTime to, List<Event> items, String timezone) {
        if (from == null && to == null) {
            return items;
        } else if (from != null && to == null) {
            return items.stream()
                .filter(event -> isAfter(event.getEnd(), from, timezone))
                .toList();
        } else if (from == null) {
            return items.stream()
                .filter(event -> isBefore(event.getStart(), to, timezone))
                .toList();
        } else {
            return items.stream()
                .filter(event -> isWithinRange(event, from, to, timezone))
                .toList();
        }
    }

    private static boolean isAfter(EventDateTime eventDateTime, LocalDateTime from, String timezone) {
        Temporal temporal = convertToTemporalFromEventDateTime(eventDateTime, timezone);

        return temporal instanceof LocalDateTime localDateTime
            ? localDateTime.isAfter(from)
            : LocalDateTime.of(((LocalDate) temporal).minusDays(1), LOCAL_TIME_MAX)
                .isAfter(from);
    }

    private static boolean isBefore(EventDateTime eventDateTime, LocalDateTime to, String timezone) {
        Temporal temporal = convertToTemporalFromEventDateTime(eventDateTime, timezone);

        return temporal instanceof LocalDateTime localDateTime
            ? localDateTime.isBefore(to)
            : LocalDateTime.of((LocalDate) temporal, LOCAL_TIME_MIN)
                .isBefore(to);
    }

    private static boolean isWithinRange(Event event, LocalDateTime from, LocalDateTime to, String timezone) {
        Temporal start = convertToTemporalFromEventDateTime(event.getStart(), timezone);
        Temporal end = convertToTemporalFromEventDateTime(event.getEnd(), timezone);

        if (start instanceof LocalDateTime startLDT && end instanceof LocalDateTime endLDT) {
            return (startLDT.isAfter(from) && startLDT.isBefore(to)) ||
                (endLDT.isAfter(from) && endLDT.isBefore(to)) ||
                (startLDT.isBefore(from) && endLDT.isAfter(to));
        } else if (start instanceof LocalDate startLD && end instanceof LocalDate endLD) {
            LocalDateTime startMin = LocalDateTime.of(startLD, LOCAL_TIME_MIN);
            LocalDateTime endMax = LocalDateTime.of(endLD.minusDays(1), LOCAL_TIME_MAX);

            return (startMin.isAfter(from) && startMin.isBefore(to)) ||
                (endMax.isAfter(from) && endMax.isBefore(to)) ||
                (startMin.isBefore(from) && endMax.isAfter(to));
        }

        return false;
    }

    private static List<CustomEvent> convertToCustomEvents(List<Event> eventList, String timezone) {
        return eventList.stream()
            .map(event -> createCustomEvent(event, timezone))
            .toList();
    }

    public static Event getEvent(Parameters inputParameters, Calendar calendar) {
        try {
            return calendar.events()
                .get(inputParameters.getRequiredString(CALENDAR_ID), inputParameters.getRequiredString(EVENT_ID))
                .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Option<String>> getEventIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);
        List<Event> events = fetchAllCalendarEvents(calendar, inputParameters.getRequiredString(CALENDAR_ID));

        return events.stream()
            .map(event -> {
                String summary = event.getSummary();
                String id = event.getId();

                return option(summary != null && !summary.isEmpty() ? summary : id, id);
            })
            .collect(Collectors.toList());
    }

    private static List<Event> fetchAllCalendarEvents(Calendar calendar, String calendarId) {
        List<Event> allEvents = new ArrayList<>();
        String nextPageToken = null;

        do {
            Events events;
            try {
                events = calendar.events()
                    .list(calendarId)
                    .setMaxResults(2500)
                    .setPageToken(nextPageToken)
                    .execute();
            } catch (IOException e) {
                throw translateGoogleIOException(e);
            }

            allEvents.addAll(events.getItems());
            nextPageToken = events.getNextPageToken();
        } while (nextPageToken != null);

        return allEvents;
    }

    public static Event updateEvent(Parameters inputParameters, Parameters connectionParameters, Event event) {
        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        try {
            return calendar
                .events()
                .update(inputParameters.getRequiredString(CALENDAR_ID), inputParameters.getRequiredString(EVENT_ID),
                    event)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }

    @SuppressFBWarnings("EI")
    public record CustomEvent(
        String iCalUID, String id, String summary, String description, Temporal startTime, Temporal endTime,
        String etag, String eventType, String htmlLink, String status, String location, String hangoutLink,
        List<EventAttendee> attendeeList, List<EventAttachment> attachments, Event.Reminders reminders) {
    }
}
