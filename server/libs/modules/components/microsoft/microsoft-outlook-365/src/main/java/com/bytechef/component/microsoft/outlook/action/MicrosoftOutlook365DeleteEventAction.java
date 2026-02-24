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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CALENDAR;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EVENT;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365OptionUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365DeleteEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteEvent")
        .title("Delete Event")
        .description("Deletes an event from the specified calendar.")
        .help("", "https://docs.bytechef.io/reference/components/microsoft-outlook-365_v1#delete-event")
        .properties(
            string(CALENDAR)
                .label("Calendar ID")
                .description("The ID of the calendar to delete the event from.")
                .options((OptionsFunction<String>) MicrosoftOutlook365OptionUtils::getCalendarIdOptions)
                .required(true),
            string(EVENT)
                .label("Event ID")
                .description("ID of the event to delete.")
                .options((OptionsFunction<String>) MicrosoftOutlook365OptionUtils::getEventIdOptions)
                .optionsLookupDependsOn(CALENDAR)
                .required(true))
        .perform(MicrosoftOutlook365DeleteEventAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOutlook365DeleteEventAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context
            .http(http -> http.delete(
                "/me/calendars/%s/events/%s"
                    .formatted(inputParameters.getRequiredString(CALENDAR), inputParameters.getRequiredString(EVENT))))
            .execute();

        return null;
    }
}
