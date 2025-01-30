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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CALENDAR;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EVENT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365OptionUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365DeleteEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteEvent")
        .title("Delete Event")
        .description("Deletes an event from the specified calendar.")
        .properties(
            CALENDAR_ID_PROPERTY,
            string(EVENT)
                .label("Event ID")
                .description("Id of the event to delete.")
                .options((ActionOptionsFunction<String>) MicrosoftOutlook365OptionUtils::getEventOptions)
                .optionsLookupDependsOn(CALENDAR)
                .required(true))
        .perform(MicrosoftOutlook365DeleteEventAction::perform);

    private MicrosoftOutlook365DeleteEventAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        actionContext
            .http(http -> http.delete(
                "/calendars/" + inputParameters.getRequiredString(CALENDAR) + "/events/" +
                    inputParameters.getRequiredString(EVENT)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return null;
    }
}
