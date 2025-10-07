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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_ID;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import java.io.IOException;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarDeleteEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteEvent")
        .title("Delete Event")
        .description("Deletes an event from Google Calendar.")
        .properties(
            CALENDAR_ID_PROPERTY,
            string(EVENT_ID)
                .label("Event ID")
                .description("ID of the event to delete.")
                .options((ActionOptionsFunction<String>) GoogleCalendarUtils::getEventIdOptions)
                .optionsLookupDependsOn(CALENDAR_ID)
                .required(true))
        .perform(GoogleCalendarDeleteEventAction::perform);

    private GoogleCalendarDeleteEventAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        try {
            calendar.events()
                .delete(inputParameters.getRequiredString(CALENDAR_ID), inputParameters.getRequiredString(EVENT_ID))
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }

        return null;
    }

}
