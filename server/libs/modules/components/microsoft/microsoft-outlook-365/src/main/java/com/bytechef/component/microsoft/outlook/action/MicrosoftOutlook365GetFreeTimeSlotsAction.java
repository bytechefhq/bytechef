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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_RANGE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils.retrieveCustomEvents;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils.CustomEvent;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365GetFreeTimeSlotsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getFreeTimeSlots")
        .title("Get Free Time Slots")
        .description("Get free time slots from the Microsoft Outlook 365 calendar.")
        .properties(
            CALENDAR_ID_PROPERTY,
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
                    .items(
                        object()
                            .properties(
                                dateTime("startTime")
                                    .description("Start time of the free time slot."),
                                dateTime("endTime")
                                    .description("End time of the free time slot.")))))
        .perform(MicrosoftOutlook365GetFreeTimeSlotsAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOutlook365GetFreeTimeSlotsAction() {
    }

    public static List<Interval> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<CustomEvent> customEvents = new ArrayList<>(retrieveCustomEvents(inputParameters, context));

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

            if (previousEndTime.isBefore(to)) {
                intervals.add(new Interval(previousEndTime, to));
            }
        }

        return intervals;
    }

    public record Interval(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
