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

package com.bytechef.component.microsoft.outlook.connection;

import static com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

import com.bytechef.microsoft.commons.MicrosoftConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Monika Kušter
 * @author Ivica Cardic
 */
public class MicrosoftOutlook365Connection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = MicrosoftConnection.createConnection(
        (connection, context) -> {
            Map<String, Boolean> map = new HashMap<>();

            map.put("Calendars.Read", false);
            map.put("Calendars.Read.Shared", false);
            map.put("Calendars.ReadBasic", false);
            map.put("Calendars.ReadWrite", true);
            map.put("Calendars.ReadWrite.Shared", true);
            map.put("Mail.Read", false);
            map.put("Mail.ReadBasic", false);
            map.put("Mail.ReadBasic.All", false);
            map.put("Mail.ReadWrite", true);
            map.put("Mail.Send", true);
            map.put("MailboxSettings.Read", true);
            map.put("MailboxSettings.ReadWrite", false);
            map.put("User.Read", false);
            map.put("User.Read.All", false);
            map.put("User.ReadBasic.All", false);
            map.put("offline_access", true);

            return map;
        });

    private MicrosoftOutlook365Connection() {
    }
}
