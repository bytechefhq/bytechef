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

package com.bytechef.component.microsoft.outlook.util;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ATTENDEES;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CALENDAR;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_RANGE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.END;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_ONLINE_MEETING;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.I_CAL_UID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REMINDER_MINUTES_BEFORE_START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365Utils {

    public static List<Map<?, ?>> getItemsFromNextPage(String link, Context context) {
        List<Map<?, ?>> otherItems = new ArrayList<>();

        while (link != null && !link.isEmpty()) {
            String finalLink = link;

            Map<String, Object> body = context.http(http -> http.get(finalLink))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get(VALUE) instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        otherItems.add(map);
                    }
                }
            }

            link = (String) body.get(ODATA_NEXT_LINK);
        }

        return otherItems;
    }

    public static CustomEvent createCustomEvent(Map<?, ?> eventMap) {
        boolean isOnlineMeeting = (Boolean) eventMap.get(IS_ONLINE_MEETING);

        String onlineMeetingUrl = isOnlineMeeting && eventMap.get("onlineMeeting") instanceof Map<?, ?> onlineMeetingMap
            ? (String) onlineMeetingMap.get("joinUrl")
            : "";

        List<String> attendees = new ArrayList<>();
        if (eventMap.get(ATTENDEES) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map1 && map1.get(EMAIL_ADDRESS) instanceof Map<?, ?> map2) {
                    attendees.add((String) map2.get(ADDRESS));
                }
            }
        }

        return new CustomEvent(
            (String) eventMap.get(I_CAL_UID), (String) eventMap.get(ID), (String) eventMap.get(SUBJECT),
            parseLocalDateTime(eventMap, START), parseLocalDateTime(eventMap, END), attendees, isOnlineMeeting,
            onlineMeetingUrl, (Integer) eventMap.get(REMINDER_MINUTES_BEFORE_START));
    }

    private static LocalDateTime parseLocalDateTime(Map<?, ?> eventMap, String time) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.S");

        Map<?, ?> timeMap = (Map<?, ?>) eventMap.get(time);

        return LocalDateTime.parse(inputFormatter.format(LocalDateTime.parse((String) timeMap.get(DATE_TIME))));
    }

    public static List<CustomEvent> retrieveCustomEvents(Parameters inputParameters, ActionContext actionContext) {
        Map<String, Object> body = actionContext
            .http(http -> http.get("/calendars/" + inputParameters.getRequiredString(CALENDAR) + "/events"))
            .header("Prefer", "outlook.timezone=\"" + getMailboxTimeZone(actionContext) + "\"")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Map<?, ?>> events = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> map) {
                    events.add(map);
                }
            }
        }

        List<Map<?, ?>> eventsFromNextPage = getItemsFromNextPage((String) body.get(ODATA_NEXT_LINK), actionContext);

        events.addAll(eventsFromNextPage);

        Map<String, LocalDateTime> timePeriod = inputParameters.getMap(DATE_RANGE, LocalDateTime.class, Map.of());

        return convertToCustomEvents(filterEventsByTimePeriod(timePeriod.get(FROM), timePeriod.get(TO), events));
    }

    private static List<CustomEvent> convertToCustomEvents(List<Map<?, ?>> events) {
        return events.stream()
            .map(MicrosoftOutlook365Utils::createCustomEvent)
            .toList();
    }

    private static List<Map<?, ?>> filterEventsByTimePeriod(
        LocalDateTime from, LocalDateTime to, List<Map<?, ?>> items) {

        return items.stream()
            .filter(event -> isWithinTimePeriod(event, from, to))
            .toList();
    }

    private static boolean isWithinTimePeriod(Map<?, ?> event, LocalDateTime from, LocalDateTime to) {
        LocalDateTime start = parseLocalDateTime(event, START);
        LocalDateTime end = parseLocalDateTime(event, END);

        return (from == null || end.isAfter(from)) && (to == null || start.isBefore(to));
    }

    public static String getMailboxTimeZone(ActionContext actionContext) {
        Map<String, String> body = actionContext.http(http -> http.get("/mailboxSettings/timeZone"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return body.get(VALUE);
    }

    @SuppressFBWarnings("EI")
    public record CustomEvent(
        String iCalUId, String id, String subject, LocalDateTime startTime, LocalDateTime endTime,
        List<String> attendees, boolean isOnlineMeeting, String onlineMeetingUrl, int reminderMinutesBeforeStart) {
    }
}
