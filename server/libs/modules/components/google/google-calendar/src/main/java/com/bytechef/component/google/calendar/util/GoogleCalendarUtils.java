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

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ALL_DAY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.EventDateTime;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class GoogleCalendarUtils {

    private GoogleCalendarUtils() {
    }

    public static Date convertToDateViaSqlTimestamp(LocalDateTime dateToConvert) {
        return dateToConvert == null ? null : java.sql.Timestamp.valueOf(dateToConvert);
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

    public static List<Option<String>> getCalendarIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) throws IOException {

        List<Option<String>> options = new ArrayList<>();

        List<CalendarListEntry> calendarListEntries = GoogleServices.getCalendar(connectionParameters)
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
