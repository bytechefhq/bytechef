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
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.END;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_ONLINE_MEETING;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.I_CAL_UID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REMINDER_MINUTES_BEFORE_START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    public static CustomEvent createCustomEvent(Map<?, ?> map) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.S");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        boolean isOnlineMeeting = (Boolean) map.get(IS_ONLINE_MEETING);

        String onlineMeetingUrl = "";
        if (isOnlineMeeting && map.get("onlineMeeting") instanceof Map<?, ?> onlineMeetingMap) {
            onlineMeetingUrl = (String) onlineMeetingMap.get("joinUrl");
        }

        int reminderMinutesBeforeStart = (Integer) map.get(REMINDER_MINUTES_BEFORE_START);

        List<String> attendees = new ArrayList<>();
        if (map.get(ATTENDEES) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map1 && map1.get(EMAIL_ADDRESS) instanceof Map<?, ?> map2) {
                    attendees.add((String) map2.get(ADDRESS));
                }
            }
        }

        Map<?, ?> start = (Map<?, ?>) map.get(START);
        String startDateTime = (String) start.get(DATE_TIME);

        Map<?, ?> end = (Map<?, ?>) map.get(END);
        String endDateTime = (String) end.get(DATE_TIME);

        ZonedDateTime startZonedDateTime =
            LocalDateTime.parse(inputFormatter.format(LocalDateTime.parse(startDateTime)))
                .atZone(ZoneId.of((String) map.get("originalStartTimeZone")));
        ZonedDateTime endZonedDateTime =
            LocalDateTime.parse(inputFormatter.format(LocalDateTime.parse(endDateTime)))
                .atZone(ZoneId.of((String) map.get("originalEndTimeZone")));

        String formattedStart = startZonedDateTime.format(outputFormatter);
        String formattedEnd = endZonedDateTime.format(outputFormatter);

        return new CustomEvent(
            (String) map.get(I_CAL_UID), (String) map.get(ID), (String) map.get(SUBJECT),
            LocalDateTime.parse(formattedStart, outputFormatter), LocalDateTime.parse(formattedEnd, outputFormatter),
            attendees, isOnlineMeeting, onlineMeetingUrl, reminderMinutesBeforeStart);
    }

    public static List<CustomEvent> getCustomEvents(Parameters inputParameters, ActionContext actionContext) {
        Map<String, Object> body = actionContext
            .http(http -> http.get("/calendars/" + inputParameters.getRequiredString(CALENDAR) + "/events"))
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

        return events.stream()
            .map(MicrosoftOutlook365Utils::createCustomEvent)
            .toList();
    }

    @SuppressFBWarnings("EI")
    public record CustomEvent(
        String iCalUId, String id, String subject, LocalDateTime startTime, LocalDateTime endTime,
        List<String> attendees, boolean isOnlineMeeting, String onlineMeetingUrl, int reminderMinutesBeforeStart) {
    }
}
