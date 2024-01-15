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

package com.bytechef.component.google.calendar.util;

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.AUTO_DECLINE_MODE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.BUILDING_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CHAT_STATUS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CUSTOM_LOCATION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DECLINE_MESSAGE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DESK_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FLOOR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FLOOR_SECTION_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FOCUS_TIME;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FOCUS_TIME_PROPERTIES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.HOME_OFFICE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LABEL;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.OFFICE_LOCATION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.OUT_OF_OFFICE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.OUT_OF_OFFICE_PROPERTIES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SINGLE_EVENTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.WORKING_LOCATION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.WORKING_LOCATION_PROPERTIES;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.Parameters;
import com.bytechef.hermes.component.definition.Property;
import com.bytechef.hermes.definition.Option;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Preconditions;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ColorDefinition;
import com.google.api.services.calendar.model.Colors;
import com.google.api.services.calendar.model.EventDateTime;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class GoogleCalendarUtils {

    public static final ComponentDSL.ModifiableObjectProperty focusTimeProperties = object(FOCUS_TIME_PROPERTIES)
        .label("Focus time properties")
        .description("Focus Time event data. Used if eventType is focusTime.")
        .properties(
            string(AUTO_DECLINE_MODE)
                .label("Auto decline mode")
                .required(false),
            string(CHAT_STATUS)
                .label("Chat status")
                .required(false),
            string(DECLINE_MESSAGE)
                .label("Decline message")
                .required(false))
        .required(false);

    public static final ComponentDSL.ModifiableObjectProperty outOfOfficeProperties = object(OUT_OF_OFFICE_PROPERTIES)
        .label("Out of office properties")
        .description("Out of office event data. Used if eventType is outOfOffice.")
        .properties(
            string(AUTO_DECLINE_MODE)
                .label("Auto decline mode")
                .required(false),
            string(DECLINE_MESSAGE)
                .label("Decline message")
                .required(false))
        .required(false);

    public static final ComponentDSL.ModifiableObjectProperty workingLocationProperties =
        object(WORKING_LOCATION_PROPERTIES)
            .label("Working location properties")
            .description("Working location event data.")
            .properties(
                object(CUSTOM_LOCATION)
                    .label("Custom location")
                    .description("If present, specifies that the user is working from a custom location.")
                    .properties(
                        string(LABEL)
                            .label("Label")
                            .description("An optional extra label for additional information."))
                    .required(false),
                object(HOME_OFFICE)
                    .label("Home office")
                    .description("If present, specifies that the user is working at home.")
                    .required(false),
                object(OFFICE_LOCATION)
                    .label("Office Location")
                    .description("If present, specifies that the user is working from an office.")
                    .properties(
                        string(BUILDING_ID)
                            .label("")
                            .description(
                                "An optional building identifier. This should reference a building ID in the " +
                                    "organization's Resources database.")
                            .required(false),
                        string(DESK_ID)
                            .label("Desk ID")
                            .description("An optional desk identifier.")
                            .required(false),
                        string(FLOOR_ID)
                            .label("Floor ID")
                            .description("An optional floor identifier.")
                            .required(false),
                        string(FLOOR_SECTION_ID)
                            .label("Floor Section ID")
                            .description("An optional floor section identifier.")
                            .required(false),
                        string(LABEL)
                            .label("Label")
                            .description(
                                "The office name that's displayed in Calendar Web and Mobile clients. We " +
                                    "recommend you reference a building name in the organization's Resources " +
                                    "database.")
                            .required(false))
                    .required(false),
                string(TYPE)
                    .label("Type")
                    .description("Type of the working location.")
                    .options(
                        option("Home office", "homeOffice", "The user is working at home."),
                        option("Office location", "officeLocation", "The user is working from an office"),
                        option("Custom location", "customLocation",
                            "The user is working from a custom location."))
                    .required(true))
            .required(false);

    public static EventDateTime createEventDateTime(EventDateTimeCustom eventDateTimeCustom) {

        EventDateTime eventDateTime = new EventDateTime();

        eventDateTime.setDate(new DateTime(GoogleCalendarUtils.convertToDateViaSqlDate(eventDateTimeCustom.date)));
        eventDateTime.setTimeZone(eventDateTimeCustom.timeZone);
        eventDateTime
            .setDateTime(new DateTime(GoogleCalendarUtils.convertToDateViaSqlTimestamp(eventDateTimeCustom.dateTime)));

        return eventDateTime;
    }

    public static Date convertToDateViaSqlDate(LocalDate dateToConvert) {
        return java.sql.Date.valueOf(dateToConvert);
    }

    public static Date convertToDateViaSqlTimestamp(LocalDateTime dateToConvert) {
        return java.sql.Timestamp.valueOf(dateToConvert);
    }

    public static Calendar getCalendar(Parameters connectionParameters) {
        return new Calendar.Builder(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            new OAuthAuthentication(connectionParameters.getRequiredString(ACCESS_TOKEN)))
                .setApplicationName("Google Calendar Component")
                .build();
    }

    private record OAuthAuthentication(String token)
        implements HttpRequestInitializer, HttpExecuteInterceptor {

        private OAuthAuthentication(String token) {
            this.token = Preconditions.checkNotNull(token);
        }

        public void initialize(HttpRequest request) {
            request.setInterceptor(this);
        }

        public void intercept(HttpRequest request) {
            HttpHeaders httpHeaders = request.getHeaders();

            httpHeaders.set("Authorization", "Bearer " + token);
        }
    }

    public static List<Option<String>> getColorOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException {

        Calendar service = getCalendar(connectionParameters);

        Colors colors = service.colors()
            .get()
            .execute();

        List<Option<String>> options = new ArrayList<>();

        for (Map.Entry<String, ColorDefinition> color : colors.getEvent()
            .entrySet()) {

            options.add(option(color.getKey(), color.getKey(),
                "Background: " + color.getValue()
                    .getBackground() +
                    "Foreground: " + color.getValue()
                        .getForeground()));
        }

        return options;
    }

    public static List<? extends Property.ValueProperty<?>> getEventTypeProperties(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String eventType = inputParameters.getString(EVENT_TYPE);

        if (eventType.equals(FOCUS_TIME)) {
            return List.of(focusTimeProperties);
        }

        if (eventType.equals(OUT_OF_OFFICE)) {
            return List.of(outOfOfficeProperties);
        }

        if (eventType.equals(WORKING_LOCATION)) {
            return List.of(workingLocationProperties);
        }

        return List.of();
    }

    public static List<Option<String>> getOrderByOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {
        boolean singleEvents = inputParameters.getBoolean(SINGLE_EVENTS);

        List<Option<String>> options = new ArrayList<>();

        options.add(option("Updated", "updated", "Order by last modification time (ascending)."));

        if (singleEvents) {
            options.add(option("Start time", "startTime",
                "Order by the start date/time (ascending). This is only available when querying " +
                    "single events (i.e. the parameter singleEvents is True)"));
        }

        return options;
    }

    public record EventDateTimeCustom(LocalDate date, LocalDateTime dateTime, String timeZone) {
    }
}
