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

package com.bytechef.component.google.calendar;

import static com.bytechef.component.google.calendar.connection.GoogleCalendarConnection.CONNECTION_DEFINITION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GOOGLE_CALENDAR;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;

import com.bytechef.component.google.calendar.action.GoogleCalendarCreateEventAction;
import com.bytechef.component.google.calendar.action.GoogleCalendarCreateQuickEventAction;
import com.bytechef.component.google.calendar.action.GoogleCalendarGetEventsAction;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class GoogleCalendarComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(GOOGLE_CALENDAR)
        .title("Google Calendar")
        .description(
            "Google Calendar is a web-based application that allows users to schedule and organize events, " +
                "appointments, and reminders, synchronizing across multiple devices.")
        .icon("path:assets/google-calendar.svg")
        .connection(CONNECTION_DEFINITION)
        .actions(GoogleCalendarCreateEventAction.ACTION_DEFINITION,
            GoogleCalendarCreateQuickEventAction.ACTION_DEFINITION,
            GoogleCalendarGetEventsAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
