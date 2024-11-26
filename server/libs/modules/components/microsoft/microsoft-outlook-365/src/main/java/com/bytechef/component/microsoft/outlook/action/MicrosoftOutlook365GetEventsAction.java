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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CUSTOM_EVENT_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.getCustomEvents;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.CustomEvent;
import java.text.ParseException;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365GetEventsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getEvents")
        .title("Get Events")
        .description("Gets a list of events in specified calendar.")
        .properties(CALENDAR_ID_PROPERTY)
        .output(outputSchema(array().items(CUSTOM_EVENT_OUTPUT_PROPERTY)))
        .perform(MicrosoftOutlook365GetEventsAction::perform);

    private MicrosoftOutlook365GetEventsAction() {
    }

    public static List<CustomEvent> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws ParseException {

        return getCustomEvents(inputParameters, actionContext);
    }
}
