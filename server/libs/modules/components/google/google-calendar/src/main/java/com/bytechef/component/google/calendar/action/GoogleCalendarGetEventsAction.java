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
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_OUTPUT_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FROM;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_RESULTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.Q;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TO;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.convertEventDateTimeToLocalDateTime;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarGetEventsAction {

    protected static final String DATE_RANGE = "dateRange";
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getEvents")
        .title("Get Events")
        .description("List events from the specified Google Calendar.")
        .properties(
            CALENDAR_ID_PROPERTY,
            array(EVENT_TYPE)
                .label("Event type")
                .description("Event types to return.")
                .items(
                    string()
                        .options(
                            option("Default", "default"),
                            option("Out of office", "outOfOffice"),
                            option("Focus time", "focusTime")))
                .required(false),
            integer(MAX_RESULTS)
                .label("Max results")
                .description(
                    "Maximum number of events returned on one result page. The number of events in the resulting " +
                        "page may be less than this value, or none at all, even if there are more events matching " +
                        "the query. Incomplete pages can be detected by a non-empty nextPageToken field in the " +
                        "response.")
                .defaultValue(250)
                .maxValue(2500)
                .required(false),
            string(Q)
                .label("Search terms")
                .description(
                    "Free text search terms to find events that match these terms in the following fields: summary, " +
                        "description, location, attendee's displayName, attendee's email, " +
                        "workingLocationProperties.officeLocation.buildingId, " +
                        "workingLocationProperties.officeLocation.deskId, " +
                        "workingLocationProperties.officeLocation.label and " +
                        "workingLocationProperties.customLocation.label")
                .required(false),
            object(DATE_RANGE)
                .label("Date range")
                .description("Date range to find events that exist in this range.")
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
                    .items(EVENT_OUTPUT_PROPERTY)))
        .perform(GoogleCalendarGetEventsAction::perform);

    private GoogleCalendarGetEventsAction() {
    }

    public static List<CustomEvent> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

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

        if (from == null && to == null) {
            return getEvents(items);
        } else if (from != null && to == null) {
            List<Event> result = items.stream()
                .filter(event -> convertEventDateTimeToLocalDateTime(event.getEnd()).isAfter(from))
                .toList();

            return getEvents(result);

        } else if (from == null) {
            List<Event> result = items.stream()
                .filter(event -> convertEventDateTimeToLocalDateTime(event.getStart()).isBefore(to))
                .toList();

            return getEvents(result);
        } else {
            List<Event> result = new ArrayList<>();

            for (Event event : items) {
                LocalDateTime startLocalDateTime = convertEventDateTimeToLocalDateTime(event.getStart());
                LocalDateTime endLocalDateTime = convertEventDateTimeToLocalDateTime(event.getEnd());

                if ((startLocalDateTime.isAfter(from) && startLocalDateTime.isBefore(to)) ||
                    (endLocalDateTime.isAfter(from) && endLocalDateTime.isBefore(to)) ||
                    (startLocalDateTime.isBefore(from) && endLocalDateTime.isAfter(to))) {
                    result.add(event);
                }
            }

            return getEvents(result);
        }
    }

    private static List<CustomEvent> getEvents(List<Event> eventList) {
        return eventList.stream()
            .map(GoogleCalendarUtils::createCustomEvent)
            .toList();
    }
}
