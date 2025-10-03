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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ALL_DAY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ATTENDEES;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CALENDAR;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CUSTOM_EVENT_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.END;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_ONLINE_MEETING;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REMINDER_MINUTES_BEFORE_START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TIME_ZONE;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils.createCustomEvent;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.getMailboxTimeZone;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils.CustomEvent;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365CreateEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createEvent")
        .title("Create Event")
        .description("Creates an event in the specified calendar.")
        .properties(
            CALENDAR_ID_PROPERTY,
            string(SUBJECT)
                .label("Subject")
                .description("The subject of the event.")
                .required(false),
            bool(ALL_DAY)
                .label("All Day Event?")
                .defaultValue(false)
                .required(true),
            date(START)
                .label("Start Date")
                .description("The start date of the event.")
                .displayCondition("%s == true".formatted(ALL_DAY))
                .required(true),
            date(END)
                .label("End Date")
                .description("The end date of the event.")
                .displayCondition("%s == true".formatted(ALL_DAY))
                .required(true),
            dateTime(START)
                .label("Start Date Time")
                .description("The start time of the event.")
                .displayCondition("%s == false".formatted(ALL_DAY))
                .required(true),
            dateTime(END)
                .label("End Date Time")
                .description("The end time of the event.")
                .displayCondition("%s == false".formatted(ALL_DAY))
                .required(true),
            array(ATTENDEES)
                .label("Attendees")
                .description("The attendees of the event.")
                .items(
                    string()
                        .label("Email")
                        .description("The attendee's email address."))
                .required(false),
            bool(IS_ONLINE_MEETING)
                .label("Is Online Meeting?")
                .description("Is the event an online meeting?")
                .defaultValue(false)
                .required(false),
            integer(REMINDER_MINUTES_BEFORE_START)
                .label("Reminder Minutes Before Start")
                .description("The number of minutes before the event start time that the reminder alert occurs.")
                .defaultValue(15)
                .required(false))
        .output(outputSchema(CUSTOM_EVENT_OUTPUT_PROPERTY))
        .perform(MicrosoftOutlook365CreateEventAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOutlook365CreateEventAction() {
    }

    public static CustomEvent perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        boolean allDay = inputParameters.getRequiredBoolean(ALL_DAY);

        LocalDateTime startTime = allDay
            ? LocalDateTime.of(inputParameters.getRequiredLocalDate(START), LocalTime.MIN)
            : inputParameters.getRequiredLocalDateTime(START);

        LocalDateTime endTime = allDay
            ? LocalDateTime.of(inputParameters.getRequiredLocalDate(END)
                .plusDays(1), LocalTime.MIN)
            : inputParameters.getRequiredLocalDateTime(END);

        List<Map<String, Map<String, String>>> attendees = inputParameters.getList(ATTENDEES, String.class, List.of())
            .stream()
            .map(attendee -> Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, attendee)))
            .toList();

        String zone = getMailboxTimeZone(context);

        Map<String, Object> body =
            context
                .http(
                    http -> http.post("/me/calendars/%s/events".formatted(inputParameters.getRequiredString(CALENDAR))))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .body(
                    Http.Body.of(
                        SUBJECT, inputParameters.getString(SUBJECT),
                        START, Map.of(DATE_TIME, startTime, TIME_ZONE, zone),
                        END, Map.of(DATE_TIME, endTime, TIME_ZONE, zone),
                        ATTENDEES, attendees,
                        IS_ONLINE_MEETING, inputParameters.getBoolean(IS_ONLINE_MEETING),
                        REMINDER_MINUTES_BEFORE_START, inputParameters.getInteger(REMINDER_MINUTES_BEFORE_START)))
                .execute()
                .getBody(new TypeReference<>() {});

        return createCustomEvent(body);
    }
}
