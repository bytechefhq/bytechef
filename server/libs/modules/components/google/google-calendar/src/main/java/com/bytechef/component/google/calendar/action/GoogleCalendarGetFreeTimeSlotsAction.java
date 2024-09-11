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

package com.bytechef.component.google.calendar.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DATE_RANGE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FROM;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TO;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.getCustomEvents;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import java.io.IOException;
import java.time.LocalDateTime;
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
            CALENDAR_ID_PROPERTY,
            object(DATE_RANGE)
                .label("Date range")
                .description("Date range to find free time.")
                .properties(
                    dateTime(FROM)
                        .label("From")
                        .description("Start of the time range.")
                        .required(false),
                    dateTime(TO)
                        .label("To")
                        .description("End of the time range.")
                        .required(false))
                .required(false))
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                dateTime("startTime"),
                                dateTime("endTime")))))
        .perform(GoogleCalendarGetFreeTimeSlotsAction::perform);

    private GoogleCalendarGetFreeTimeSlotsAction() {
    }

    public static List<Interval> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        List<CustomEvent> customEvents = new ArrayList<>(getCustomEvents(inputParameters, connectionParameters));

        customEvents.sort(Comparator.comparing(CustomEvent::startTime));

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
                LocalDateTime startTime = customEvent.startTime();
                LocalDateTime endTime = customEvent.endTime();

                if (startTime.isAfter(previousEndTime)) {
                    intervals.add(new Interval(previousEndTime, startTime));
                }

                previousEndTime = previousEndTime.isAfter(endTime) ? previousEndTime : endTime;
            }
        }
        return intervals;
    }

    public record Interval(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
