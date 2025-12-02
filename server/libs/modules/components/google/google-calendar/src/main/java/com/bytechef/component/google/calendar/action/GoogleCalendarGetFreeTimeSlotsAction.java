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

package com.bytechef.component.google.calendar.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DATE_RANGE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FROM;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LOCAL_TIME_MIN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TO;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.getCustomEvents;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarGetFreeTimeSlotsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getFreeTimeSlots")
        .title("Get Free Time Slots")
        .description("Get free time slots from Google Calendar.")
        .properties(
            string(CALENDAR_ID)
                .label("Calendar Identifier")
                .options(GoogleCalendarUtils.getCalendarIdOptions(null))
                .required(true),
            object(DATE_RANGE)
                .label("Date Range")
                .description("Date range to find free time.")
                .properties(
                    dateTime(FROM)
                        .label("From")
                        .description("Start of the time range.")
                        .required(true),
                    dateTime(TO)
                        .label("To")
                        .description("End of the time range.")
                        .required(true))
                .required(true))
        .output(
            outputSchema(
                array()
                    .description("Free time slots.")
                    .items(
                        object()
                            .properties(
                                dateTime("startTime")
                                    .description("Start time of the free time slot."),
                                dateTime("endTime")
                                    .description("End time of the free time slot.")))))
        .perform(GoogleCalendarGetFreeTimeSlotsAction::perform);

    private GoogleCalendarGetFreeTimeSlotsAction() {
    }

    public static List<Interval> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<CustomEvent> customEvents = new ArrayList<>(getCustomEvents(inputParameters, connectionParameters));

        customEvents.sort(Comparator.comparing(CustomEvent::startTime, (s1, s2) -> {
            if (s1 instanceof LocalDateTime l1 && s2 instanceof LocalDateTime l2) {
                return l1.compareTo(l2);
            }
            if (s1 instanceof LocalDateTime l1 && s2 instanceof LocalDate l2) {
                return l1.compareTo(LocalDateTime.of(l2, LOCAL_TIME_MIN));
            }
            if (s1 instanceof LocalDate l1 && s2 instanceof LocalDateTime l2) {
                return LocalDateTime.of(l1, LOCAL_TIME_MIN)
                    .compareTo(l2);
            } else if (s1 instanceof LocalDate l1 && s2 instanceof LocalDate l2) {
                return l1.compareTo(l2);
            } else {
                throw new IllegalArgumentException("Unsupported type");
            }
        }));

        return getIntervals(customEvents, inputParameters.getMap(DATE_RANGE, LocalDateTime.class, Map.of()));
    }

    private static List<Interval> getIntervals(List<CustomEvent> customEvents, Map<String, LocalDateTime> timePeriod) {
        LocalDateTime from = timePeriod.get(FROM);
        LocalDateTime to = timePeriod.get(TO);

        List<Interval> intervals = new ArrayList<>();

        if (customEvents.isEmpty()) {
            intervals.add(new Interval(from, to));
        } else {
            LocalDateTime previousEndTime = from;

            for (CustomEvent customEvent : customEvents) {
                Temporal startTime = customEvent.startTime();
                Temporal endTime = customEvent.endTime();

                if (startTime instanceof LocalDateTime start && endTime instanceof LocalDateTime end) {
                    if (start.isAfter(previousEndTime)) {
                        intervals.add(new Interval(previousEndTime, start));
                    }

                    previousEndTime = previousEndTime.isAfter(end) ? previousEndTime : end;
                } else if (startTime instanceof LocalDate start && endTime instanceof LocalDate end) {

                    if (LocalDateTime.of(start, LOCAL_TIME_MIN)
                        .isAfter(previousEndTime)) {

                        intervals.add(new Interval(previousEndTime, LocalDateTime.of(start, LOCAL_TIME_MIN)));
                    }

                    previousEndTime = previousEndTime.isAfter(LocalDateTime.of(end, LOCAL_TIME_MIN))
                        ? previousEndTime : LocalDateTime.of(end, LOCAL_TIME_MIN);
                }
            }

            if (previousEndTime.isBefore(to)) {
                intervals.add(new Interval(previousEndTime, to));
            }
        }

        return intervals;
    }

    public record Interval(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
