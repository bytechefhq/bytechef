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
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FROM;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LOCAL_TIME_MAX;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LOCAL_TIME_MIN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_RESULTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.Q;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TO;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttachment;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarUtils {

    private GoogleCalendarUtils() {
    }

    public static Date convertToDateViaSqlTimestamp(LocalDateTime dateToConvert) {
        return dateToConvert == null ? null : java.sql.Timestamp.valueOf(dateToConvert);
    }

    public static Temporal convertToTemporalFromEventDateTime(EventDateTime eventDateTime) {
        DateTime dateTime = eventDateTime.getDateTime();

        if (dateTime != null) {
            return LocalDateTime.ofInstant(Instant.parse(dateTime.toString()), ZoneId.systemDefault());
        }

        DateTime allDayDate = eventDateTime.getDate();

        return LocalDate.parse(allDayDate.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static EventDateTime createEventDateTime(Parameters inputParameters, String time) {
        EventDateTime eventDateTime = new EventDateTime();

        if (inputParameters.getRequiredBoolean(ALL_DAY)) {
            Date date = inputParameters.getRequiredDate(time);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

            eventDateTime.setDate(new DateTime(simpleDateFormat.format(date)));
        } else {
            eventDateTime.setDateTime(
                new DateTime(convertToDateViaSqlTimestamp(inputParameters.getRequiredLocalDateTime(time))));
        }

        return eventDateTime;
    }

    public static CustomEvent createCustomEvent(Event event) {
        return new CustomEvent(
            event.getICalUID(), event.getId(), event.getSummary(), event.getDescription(),
            convertToTemporalFromEventDateTime(event.getStart()), convertToTemporalFromEventDateTime(event.getEnd()),
            event.getEtag(), event.getEventType(), event.getHtmlLink(), event.getStatus(), event.getLocation(),
            event.getHangoutLink(), event.getAttendees(), event.getAttachments(), event.getReminders());
    }

    public static List<Option<String>> getCalendarIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) throws IOException {

        List<CalendarListEntry> calendarListEntries = GoogleServices.getCalendar(connectionParameters)
            .calendarList()
            .list()
            .setMinAccessRole("writer")
            .execute()
            .getItems();

        List<Option<String>> options = new ArrayList<>();

        for (CalendarListEntry calendarListEntry : calendarListEntries) {
            options.add(
                option(calendarListEntry.getSummary(), calendarListEntry.getId()));
        }

        return options;
    }

    public static List<CustomEvent> getCustomEvents(Parameters inputParameters, Parameters connectionParameters)
        throws IOException {

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        List<Event> items = calendar.events()
            .list(inputParameters.getRequiredString(CALENDAR_ID))
            .setEventTypes(inputParameters.getList(EVENT_TYPE, String.class, List.of()))
            .setMaxResults(inputParameters.getInteger(MAX_RESULTS))
            .setQ(inputParameters.getString(Q))
            .execute()
            .getItems();

        Map<String, LocalDateTime> timePeriod = inputParameters.getMap(DATE_RANGE, LocalDateTime.class, Map.of());

        LocalDateTime from = timePeriod.get(FROM);
        LocalDateTime to = timePeriod.get(TO);

        return convertToCustomEvents(filterEvents(from, to, items));
    }

    private static List<Event> filterEvents(LocalDateTime from, LocalDateTime to, List<Event> items) {
        if (from == null && to == null) {
            return items;
        } else if (from != null && to == null) {

            return items.stream()
                .filter(event -> isAfter(event.getEnd(), from))
                .toList();

        } else if (from == null) {

            return items.stream()
                .filter(event -> isBefore(event.getStart(), to))
                .toList();
        } else {
            return items.stream()
                .filter(event -> isWithinRange(event, from, to))
                .toList();
        }
    }

    private static boolean isAfter(EventDateTime eventDateTime, LocalDateTime from) {
        Temporal temporal = convertToTemporalFromEventDateTime(eventDateTime);

        return temporal instanceof LocalDateTime localDateTime ? localDateTime.isAfter(from)
            : LocalDateTime.of(((LocalDate) temporal).minusDays(1), LOCAL_TIME_MAX)
                .isAfter(from);
    }

    private static boolean isBefore(EventDateTime eventDateTime, LocalDateTime to) {
        Temporal temporal = convertToTemporalFromEventDateTime(eventDateTime);

        return temporal instanceof LocalDateTime localDateTime ? localDateTime.isBefore(to)
            : LocalDateTime.of((LocalDate) temporal, LOCAL_TIME_MIN)
                .isBefore(to);
    }

    private static boolean isWithinRange(Event event, LocalDateTime from, LocalDateTime to) {
        Temporal start = convertToTemporalFromEventDateTime(event.getStart());
        Temporal end = convertToTemporalFromEventDateTime(event.getEnd());

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

    private static List<CustomEvent> convertToCustomEvents(List<Event> eventList) {
        return eventList.stream()
            .map(GoogleCalendarUtils::createCustomEvent)
            .toList();
    }

    public static Event getEvent(Parameters inputParameters, Parameters connectionParameters) throws IOException {
        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        return calendar
            .events()
            .get(inputParameters.getRequiredString(CALENDAR_ID), inputParameters.getRequiredString(EVENT_ID))
            .execute();
    }

    public static List<Option<String>> getEventIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) throws IOException {

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        List<Event> events = calendar
            .events()
            .list(inputParameters.getRequiredString(CALENDAR_ID))
            .execute()
            .getItems();

        List<Option<String>> options = new ArrayList<>();

        for (Event event : events) {
            String id = event.getId();
            String summary = event.getSummary();

            options.add(option(summary != null && !summary.isEmpty() ? summary : id, id));
        }

        return options;
    }

    public static Event updateEvent(Parameters inputParameters, Parameters connectionParameters, Event event)
        throws IOException {
        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        return calendar
            .events()
            .update(inputParameters.getRequiredString(CALENDAR_ID), inputParameters.getRequiredString(EVENT_ID), event)
            .execute();
    }

    @SuppressFBWarnings("EI")
    public record CustomEvent(
        String iCalUID, String id, String summary, String description, Temporal startTime, Temporal endTime,
        String etag, String eventType, String htmlLink, String status, String location, String hangoutLink,
        List<EventAttendee> attendeeList, List<EventAttachment> attachments, Event.Reminders reminders) {
    }
}
