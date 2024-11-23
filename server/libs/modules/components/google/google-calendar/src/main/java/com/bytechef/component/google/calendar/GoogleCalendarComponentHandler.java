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

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.google.calendar.connection.GoogleCalendarConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.calendar.action.GoogleCalendarAddAttendeesToEventAction;
import com.bytechef.component.google.calendar.action.GoogleCalendarCreateEventAction;
import com.bytechef.component.google.calendar.action.GoogleCalendarCreateQuickEventAction;
import com.bytechef.component.google.calendar.action.GoogleCalendarDeleteEventAction;
import com.bytechef.component.google.calendar.action.GoogleCalendarGetEventsAction;
import com.bytechef.component.google.calendar.action.GoogleCalendarGetFreeTimeSlotsAction;
import com.bytechef.component.google.calendar.action.GoogleCalendarUpdateEventAction;
import com.bytechef.component.google.calendar.trigger.GoogleCalendarEventTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class GoogleCalendarComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleCalendar")
        .title("Google Calendar")
        .description(
            "Google Calendar is a web-based application that allows users to schedule and organize events, " +
                "appointments, and reminders, synchronizing across multiple devices.")
        .customAction(true)
        .icon("path:assets/google-calendar.svg")
        .categories(ComponentCategory.CALENDARS_AND_SCHEDULING)
        .connection(CONNECTION_DEFINITION)
        .actions(
            GoogleCalendarAddAttendeesToEventAction.ACTION_DEFINITION,
            GoogleCalendarCreateEventAction.ACTION_DEFINITION,
            GoogleCalendarCreateQuickEventAction.ACTION_DEFINITION,
            GoogleCalendarDeleteEventAction.ACTION_DEFINITION,
            GoogleCalendarGetEventsAction.ACTION_DEFINITION,
            GoogleCalendarGetFreeTimeSlotsAction.ACTION_DEFINITION,
            GoogleCalendarUpdateEventAction.ACTION_DEFINITION)
        .triggers(GoogleCalendarEventTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
