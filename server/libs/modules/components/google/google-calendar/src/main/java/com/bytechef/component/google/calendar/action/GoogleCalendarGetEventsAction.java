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
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ACCESS_ROLE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DEFAULT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DEFAULT_REMINDERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DESCRIPTION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ETAG;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FOCUS_TIME;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GET_EVENTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.KIND;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_RESULTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.METHOD;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MINUTES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.NEXT_SYNC_TOKEN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.OUT_OF_OFFICE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.Q;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SUMMARY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_MAX;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_MIN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_ZONE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.UPDATED;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class GoogleCalendarGetEventsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_EVENTS)
        .title("Get events")
        .description("Returns events on the specified calendar.")
        .properties(
            CALENDAR_ID_PROPERTY,
            array(EVENT_TYPE)
                .label("Event type")
                .description("Event types to return.")
                .items(
                    string()
                        .options(
                            option("Default", DEFAULT),
                            option("Out of office", OUT_OF_OFFICE),
                            option("Focus time", FOCUS_TIME)))
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
            dateTime(TIME_MAX)
                .label("Time max")
                .description(
                    "Upper bound (exclusive) for an event's start time to filter by. The default is not to filter " +
                        "by start time. Must be an RFC3339 timestamp with mandatory time zone offset, for example, " +
                        "2011-06-03T10:00:00-07:00, 2011-06-03T10:00:00Z. Milliseconds may be provided but are " +
                        "ignored.")
                .required(false),
            dateTime(TIME_MIN)
                .label("Time min")
                .description(
                    "Lower bound (exclusive) for an event's end time to filter by. The default is not to " +
                        "filter by end time. Must be an RFC3339 timestamp with mandatory time zone offset, for " +
                        "example, 2011-06-03T10:00:00-07:00, 2011-06-03T10:00:00Z. Milliseconds may be provided but " +
                        "are ignored.")
                .required(false))
        .outputSchema(
            object()
                .properties(
                    string(KIND),
                    string(ETAG),
                    string(SUMMARY),
                    string(DESCRIPTION),
                    dateTime(UPDATED),
                    string(TIME_ZONE),
                    string(ACCESS_ROLE),
                    array(DEFAULT_REMINDERS)
                        .items(
                            object()
                                .properties(
                                    string(METHOD),
                                    integer(MINUTES))),
                    string(NEXT_PAGE_TOKEN),
                    string(NEXT_SYNC_TOKEN),
                    array(EVENT)
                        .items(EVENT_PROPERTY)))
        .perform(GoogleCalendarGetEventsAction::perform);

    private GoogleCalendarGetEventsAction() {
    }

    public static Events perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        LocalDateTime timeMax = inputParameters.getLocalDateTime(TIME_MAX);
        LocalDateTime timeMin = inputParameters.getLocalDateTime(TIME_MIN);

        return calendar.events()
            .list(inputParameters.getRequiredString(CALENDAR_ID))
            .setEventTypes(inputParameters.getList(EVENT_TYPE, String.class, List.of()))
            .setMaxResults(inputParameters.getInteger(MAX_RESULTS))
            .setQ(inputParameters.getString(Q))
            .setTimeMax(
                timeMax == null ? null : new DateTime(GoogleCalendarUtils.convertToDateViaSqlTimestamp(timeMax)))
            .setTimeMin(
                timeMin == null ? null : new DateTime(GoogleCalendarUtils.convertToDateViaSqlTimestamp(timeMin)))
            .execute();
    }

}
