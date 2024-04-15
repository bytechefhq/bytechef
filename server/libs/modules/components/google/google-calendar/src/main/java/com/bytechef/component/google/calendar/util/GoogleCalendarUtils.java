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

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ALL_DAY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.END;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.METHOD;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MINUTES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.REMINDERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.START;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.USE_DEFAULT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableDateProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableDateTimeProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.EventDateTime;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class GoogleCalendarUtils {

    protected static final ModifiableDateProperty END_DATE_PROPERTY = date(END)
        .label("End date")
        .description("The end date of the event.")
        .required(true);

    protected static final ModifiableDateTimeProperty END_DATE_TIME_PROPERTY = dateTime(END)
        .label("End date time")
        .description(
            "The (exclusive) end time of the event. For a recurring event, this is the end time of the " +
                "first instance.")
        .required(true);

    protected static final ModifiableArrayProperty REMINDERS_PROPERTY = array(REMINDERS)
        .label("Reminders")
        .items(
            object()
                .properties(
                    string(METHOD)
                        .label("How is reminder sent?")
                        .options(
                            option("Email", "email", "Reminders are sent via email."),
                            option("Popup", "popup", "Reminders are sent via a UI popup."))
                        .required(true),
                    integer(MINUTES)
                        .label("Minutes before reminder")
                        .description(
                            "Number of minutes before the start of the event when the reminder " +
                                "should trigger.")
                        .minValue(0)
                        .maxValue(40320)
                        .required(true)))
        .required(false);

    protected static final ModifiableDateProperty START_DATE_PROPERTY = date(START)
        .label("Start date")
        .description("The start date of the event.")
        .required(true);

    protected static final ModifiableDateTimeProperty START_DATE_TIME_PROPERTY = dateTime(START)
        .label("Start date time")
        .description(
            "The (inclusive) start time of the event. For a recurring event, this is the start time of the " +
                "first instance.")
        .required(true);

    private GoogleCalendarUtils() {
    }

    public static Date convertToDateViaSqlTimestamp(LocalDateTime dateToConvert) {
        return dateToConvert == null ? null : java.sql.Timestamp.valueOf(dateToConvert);
    }

    public static EventDateTime createEventDateTime(Parameters inputParameters, String time) {
        EventDateTime eventDateTime = new EventDateTime();

        Map<String, String> timeMap = inputParameters.getMap(TIME, String.class);

        if (inputParameters.getRequiredBoolean(ALL_DAY)) {
            eventDateTime.setDate(new DateTime(timeMap.get(time)));
        } else {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

            eventDateTime.setDateTime(
                new DateTime(
                    convertToDateViaSqlTimestamp(LocalDateTime.parse(timeMap.get(time), dateTimeFormatter))));
        }

        return eventDateTime;
    }

    public static List<? extends ValueProperty<?>> createRemindersProperties(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return inputParameters.getRequiredBoolean(USE_DEFAULT) ? List.of() : List.of(REMINDERS_PROPERTY);
    }

    public static List<? extends ValueProperty<?>> createTimeProperties(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return inputParameters.getRequiredBoolean(ALL_DAY) ? List.of(START_DATE_PROPERTY, END_DATE_PROPERTY)
            : List.of(START_DATE_TIME_PROPERTY, END_DATE_TIME_PROPERTY);
    }

    public static List<Option<String>> getCalendarIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException {

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        List<Option<String>> options = new ArrayList<>();

        List<CalendarListEntry> calendarListEntries = calendar
            .calendarList()
            .list()
            .setMinAccessRole("writer")
            .execute()
            .getItems();

        for (CalendarListEntry calendarListEntry : calendarListEntries) {
            options.add(
                option(calendarListEntry.getSummary(), calendarListEntry.getId()));
        }

        return options;
    }
}
