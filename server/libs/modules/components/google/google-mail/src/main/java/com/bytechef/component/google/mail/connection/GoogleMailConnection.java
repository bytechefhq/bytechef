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

package com.bytechef.component.google.mail.connection;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.google.commons.GoogleConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleMailConnection {

    private GoogleMailConnection() {
    }

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = GoogleConnection.createConnection(
        null, (connection, context) -> {
            Map<String, Boolean> map = new HashMap<>();

            map.put("https://mail.google.com/", true);
            map.put("https://www.googleapis.com/auth/gmail.addons.current.action.compose", false);
            map.put("https://www.googleapis.com/auth/gmail.addons.current.message.action", false);
            map.put("https://www.googleapis.com/auth/gmail.addons.current.message.metadata", false);
            map.put("https://www.googleapis.com/auth/gmail.addons.current.message.readonly", false);
            map.put("https://www.googleapis.com/auth/gmail.compose", true);
            map.put("https://www.googleapis.com/auth/gmail.insert", false);
            map.put("https://www.googleapis.com/auth/gmail.labels", false);
            map.put("https://www.googleapis.com/auth/gmail.metadata", false);
            map.put("https://www.googleapis.com/auth/gmail.modify", false);
            map.put("https://www.googleapis.com/auth/gmail.readonly", true);
            map.put("https://www.googleapis.com/auth/gmail.send", true);
            map.put("https://www.googleapis.com/auth/gmail.settings.basic", false);
            map.put("https://www.googleapis.com/auth/gmail.settings.sharing", false);
            map.put("email", true);
            map.put("https://www.googleapis.com/auth/calendar.settings.readonly", true);

            return map;
        });
}
