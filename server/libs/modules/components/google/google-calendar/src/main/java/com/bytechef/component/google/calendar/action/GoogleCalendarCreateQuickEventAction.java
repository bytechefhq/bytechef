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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_OUTPUT_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TEXT;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.createCustomEvent;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import java.io.IOException;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarCreateQuickEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createQuickEvent")
        .title("Create Quick Event")
        .description("Creates a quick event in Google Calendar.")
        .properties(
            CALENDAR_ID_PROPERTY,
            string(TEXT)
                .label("Text")
                .description("The text describing the event to be created.")
                .required(true),
            SEND_UPDATES_PROPERTY)
        .output(outputSchema(EVENT_OUTPUT_PROPERTY))
        .perform(GoogleCalendarCreateQuickEventAction::perform);

    private GoogleCalendarCreateQuickEventAction() {
    }

    public static CustomEvent perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Calendar calendar = GoogleServices.getCalendar(connectionParameters);
        Event event;

        try {
            event = calendar.events()
                .quickAdd(inputParameters.getRequiredString(CALENDAR_ID),
                    inputParameters.getRequiredString(TEXT))
                .setSendUpdates(inputParameters.getString(SEND_UPDATES))
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }

        return createCustomEvent(event, GoogleUtils.getCalendarTimezone(calendar));
    }
}
