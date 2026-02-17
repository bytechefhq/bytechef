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

package com.bytechef.component.google.calendar.connection;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.google.commons.GoogleConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class GoogleCalendarConnection {

    private GoogleCalendarConnection() {
    }

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = GoogleConnection.createConnection(
        null,
        1,
        "https://docs.bytechef.io/reference/components/google-calendar_v1#connection-setup",
        (connection, context) -> {
            Map<String, Boolean> map = new HashMap<>();

            map.put("https://www.googleapis.com/auth/calendar", false);
            map.put("https://www.googleapis.com/auth/calendar.acls", false);
            map.put("https://www.googleapis.com/auth/calendar.acls.readonly", false);
            map.put("https://www.googleapis.com/auth/calendar.app.created", false);
            map.put("https://www.googleapis.com/auth/calendar.calendarlist", false);
            map.put("https://www.googleapis.com/auth/calendar.calendarlist.readonly", false);
            map.put("https://www.googleapis.com/auth/calendar.calendars", false);
            map.put("https://www.googleapis.com/auth/calendar.calendars.readonly", false);
            map.put("https://www.googleapis.com/auth/calendar.events", true);
            map.put("https://www.googleapis.com/auth/calendar.events.freebusy", false);
            map.put("https://www.googleapis.com/auth/calendar.events.owned", false);
            map.put("https://www.googleapis.com/auth/calendar.events.owned.readonly", false);
            map.put("https://www.googleapis.com/auth/calendar.events.public.readonly", false);
            map.put("https://www.googleapis.com/auth/calendar.events.readonly", false);
            map.put("https://www.googleapis.com/auth/calendar.freebusy", false);
            map.put("https://www.googleapis.com/auth/calendar.readonly", true);
            map.put("https://www.googleapis.com/auth/calendar.settings.readonly", false);

            return map;
        });
}
