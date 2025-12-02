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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DATE_RANGE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_OUTPUT_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FROM;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_RESULTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.Q;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TO;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarGetEventsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getEvents")
        .title("Get Events")
        .description("List events from the specified Google Calendar.")
        .properties(
            string(CALENDAR_ID)
                .label("Calendar Identifier")
                .options(GoogleCalendarUtils.getCalendarIdOptions(null))
                .required(true),
            array(EVENT_TYPE)
                .label("Event Type")
                .description("Event types to return.")
                .items(
                    string()
                        .options(
                            option("Default", "default"),
                            option("Out of office", "outOfOffice"),
                            option("Focus time", "focusTime")))
                .required(false),
            integer(MAX_RESULTS)
                .label("Max Results")
                .description(
                    "Maximum number of events returned on one result page. The number of events in the resulting " +
                        "page may be less than this value, or none at all, even if there are more events matching " +
                        "the query. Incomplete pages can be detected by a non-empty nextPageToken field in the " +
                        "response.")
                .defaultValue(250)
                .maxValue(2500)
                .required(false),
            string(Q)
                .label("Search Terms")
                .description(
                    "Free text search terms to find events that match these terms in the following fields: summary, " +
                        "description, location, attendee's displayName, attendee's email, " +
                        "workingLocationProperties.officeLocation.buildingId, " +
                        "workingLocationProperties.officeLocation.deskId, " +
                        "workingLocationProperties.officeLocation.label and " +
                        "workingLocationProperties.customLocation.label")
                .required(false),
            object(DATE_RANGE)
                .label("Date Range")
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
                    .description("List of events.")
                    .items(EVENT_OUTPUT_PROPERTY)))
        .perform(GoogleCalendarGetEventsAction::perform);

    private GoogleCalendarGetEventsAction() {
    }

    public static List<CustomEvent> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return GoogleCalendarUtils.getCustomEvents(inputParameters, connectionParameters);
    }
}
