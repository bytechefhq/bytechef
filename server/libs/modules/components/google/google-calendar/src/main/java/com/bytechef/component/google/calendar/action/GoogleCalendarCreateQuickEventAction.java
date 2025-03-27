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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CREATE_QUICK_EVENT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CREATE_QUICK_EVENT_DESCRIPTION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CREATE_QUICK_EVENT_TITLE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_OUTPUT_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TEXT;
import static com.bytechef.component.google.calendar.util.GoogleCalendarUtils.createCustomEvent;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.definition.BaseOutputDefinition.OutputSchema;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarCreateQuickEventAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        CALENDAR_ID_PROPERTY,
        string(TEXT)
            .label("Text")
            .description("The text describing the event to be created.")
            .required(true),
        SEND_UPDATES_PROPERTY
    };

    public static final OutputSchema<ObjectProperty> OUTPUT_SCHEMA = outputSchema(EVENT_OUTPUT_PROPERTY);

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_QUICK_EVENT)
        .title(CREATE_QUICK_EVENT_TITLE)
        .description(CREATE_QUICK_EVENT_DESCRIPTION)
        .properties(PROPERTIES)
        .output(OUTPUT_SCHEMA)
        .perform(GoogleCalendarCreateQuickEventAction::perform);

    private GoogleCalendarCreateQuickEventAction() {
    }

    public static CustomEvent perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException {

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        Event event = calendar.events()
            .quickAdd(inputParameters.getRequiredString(CALENDAR_ID), inputParameters.getRequiredString(TEXT))
            .setSendUpdates(inputParameters.getString(SEND_UPDATES))
            .execute();

        return createCustomEvent(event);
    }
}
