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
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CREATE_QUICK_EVENT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TEXT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import java.io.IOException;

/**
 * @author Monika Domiter
 */
public class GoogleCalendarCreateQuickEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_QUICK_EVENT)
        .title("Create Quick Event")
        .description("Add Quick Calendar Event")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text describing the event to be created.")
                .required(true),
            SEND_UPDATES_PROPERTY)
        .outputSchema(EVENT_PROPERTY)
        .perform(GoogleCalendarCreateQuickEventAction::perform);

    private GoogleCalendarCreateQuickEventAction() {
    }

    public static Event perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        return calendar.events()
            .quickAdd("primary", inputParameters.getRequiredString(TEXT))
            .setSendUpdates(inputParameters.getString(SEND_UPDATES))
            .execute();
    }
}
