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
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ACCESS_ROLE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ALWAYS_INCLUDE_EMAIL;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DEFAULT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DEFAULT_REMINDERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DESCRIPTION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ETAG;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FOCUS_TIME;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GET_EVENTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ICAL_UID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.KIND;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_ATTENDEES_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_RESULTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.METHOD;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MINUTES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.NEXT_SYNC_TOKEN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ORDER_BY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.OUT_OF_OFFICE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.PAGE_TOKEN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.PRIVATE_EXTENDED_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.Q;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SHARED_EXTENDED_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SHOW_DELETED;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SHOW_HIDDEN_INVITATIONS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SINGLE_EVENTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SUMMARY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SYNC_TOKEN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_MAX;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_MIN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_ZONE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.UPDATED;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.UPDATE_MIN;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class GoogleCalendarGetEventsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_EVENTS)
        .title("Get events")
        .description("Returns events on the specified calendar. ")
        .properties(
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
            string(ICAL_UID)
                .label("iCalUID")
                .description(
                    "Specifies an event ID in the iCalendar format to be provided in the response. Use this if you " +
                        "want to search for an event by its iCalendar ID.")
                .required(false),
            MAX_ATTENDEES_PROPERTY,
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
            string(ORDER_BY)
                .label("Order by")
                .description("The order of the events returned in the result.")
                .options((OptionsDataSource.ActionOptionsFunction) GoogleCalendarUtils::getOrderByOptions)
                .required(false),
            string(PAGE_TOKEN)
                .label("Page token")
                .description("Token specifying which result page to return.")
                .required(false),
            array(PRIVATE_EXTENDED_PROPERTY)
                .label("Private extended property")
                .description(
                    "Extended properties constraint specified as propertyName=value. Matches only private " +
                        "properties. This parameter might be repeated multiple times to return events that match all " +
                        "given constraints.")
                .items(string())
                .required(false),
            string(Q)
                .label("q")
                .description(
                    "Free text search terms to find events that match these terms in the following fields: summary, " +
                        "description, location, attendee's displayName, attendee's email, " +
                        "workingLocationProperties.officeLocation.buildingId, " +
                        "workingLocationProperties.officeLocation.deskId, " +
                        "workingLocationProperties.officeLocation.label and " +
                        "workingLocationProperties.customLocation.label")
                .required(false),
            array(SHARED_EXTENDED_PROPERTY)
                .label("Shared extended properties")
                .description(
                    "Extended properties constraint specified as propertyName=value. Matches only shared " +
                        "properties. This parameter might be repeated multiple times to return events that match all " +
                        "given constraints.")
                .items(string())
                .required(false),
            bool(SHOW_DELETED)
                .label("Show deleted")
                .description(
                    "Whether to include deleted events (with status equals \"cancelled\") in the result. Cancelled " +
                        "instances of recurring events (but not the underlying recurring event) will still be " +
                        "included if showDeleted and singleEvents are both False. If showDeleted and singleEvents " +
                        "are both True, only single instances of deleted events (but not the underlying recurring " +
                        "events) are returned.")
                .defaultValue(false)
                .required(false),
            bool(SHOW_HIDDEN_INVITATIONS)
                .label("Show hidden invitations")
                .description("Whether to include hidden invitations in the result.")
                .defaultValue(false)
                .required(false),
            bool(SINGLE_EVENTS)
                .label("Single events")
                .description(
                    "Whether to expand recurring events into instances and only return single one-off events and " +
                        "instances of recurring events, but not the underlying recurring events themselves.")
                .defaultValue(false)
                .required(false),
            string(SYNC_TOKEN)
                .label("Sync token")
                .description("")
                .required(false),
            dateTime(TIME_MAX)
                .label("Time max")
                .description(
                    "Upper bound (exclusive) for an event's start time to filter by. The default is not to filter " +
                        "by start time. Must be an RFC3339 timestamp with mandatory time zone offset, for example, " +
                        "2011-06-03T10:00:00-07:00, 2011-06-03T10:00:00Z. Milliseconds may be provided but are " +
                        "ignored. If timeMin is set, timeMax must be greater than timeMin.")
                .required(false),
            dateTime(TIME_MIN)
                .label("Time min")
                .description(
                    "Lower bound (exclusive) for an event's end time to filter by. Optional. The default is not to " +
                        "filter by end time. Must be an RFC3339 timestamp with mandatory time zone offset, for " +
                        "example, 2011-06-03T10:00:00-07:00, 2011-06-03T10:00:00Z. Milliseconds may be provided but " +
                        "are ignored. If timeMax is set, timeMin must be smaller than timeMax.")
                .required(false),
            string(TIME_ZONE)
                .label("Time zone")
                .description("Time zone used in the response.")
                .required(false),
            dateTime(UPDATE_MIN)
                .label("Update min")
                .description(
                    "Lower bound for an event's last modification time (as a RFC3339 timestamp) to filter by. When " +
                        "specified, entries deleted since this time will always be included regardless of showDeleted.")
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

        Calendar service = GoogleServices.getCalendar(connectionParameters);

        return service.events()
            .list("primary")
            .setAlwaysIncludeEmail(inputParameters.getBoolean(ALWAYS_INCLUDE_EMAIL))
            .setEventTypes(inputParameters.getList(EVENT_TYPE, String.class, List.of()))
            .setICalUID(inputParameters.getString(ICAL_UID))
            .setMaxAttendees(inputParameters.getInteger(MAX_ATTENDEES))
            .setMaxResults(inputParameters.getInteger(MAX_RESULTS))
            .setOrderBy(inputParameters.getString(ORDER_BY))
            .setPageToken(inputParameters.getString(PAGE_TOKEN))
            .setPrivateExtendedProperty(inputParameters.getList(PRIVATE_EXTENDED_PROPERTY, String.class, List.of()))
            .setQ(inputParameters.getString(Q))
            .setSharedExtendedProperty(inputParameters.getList(SHARED_EXTENDED_PROPERTY, String.class, List.of()))
            .setShowHiddenInvitations(inputParameters.getBoolean(SHOW_HIDDEN_INVITATIONS))
            .setSingleEvents(inputParameters.getBoolean(SINGLE_EVENTS))
            .setSyncToken(inputParameters.getString(SYNC_TOKEN))
            .setTimeMax(
                new DateTime(
                    GoogleCalendarUtils.convertToDateViaSqlTimestamp(inputParameters.getLocalDateTime(TIME_MAX))))
            .setTimeMin(
                new DateTime(
                    GoogleCalendarUtils.convertToDateViaSqlTimestamp(inputParameters.getLocalDateTime(TIME_MIN))))
            .setTimeZone(inputParameters.getString(TIME_ZONE))
            .setUpdatedMin(
                new DateTime(
                    GoogleCalendarUtils.convertToDateViaSqlTimestamp(inputParameters.getLocalDateTime(UPDATE_MIN))))
            .execute();
    }

}
