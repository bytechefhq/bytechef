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

package com.bytechef.component.google.chat.connection;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.google.commons.GoogleConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class GoogleChatConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = GoogleConnection.createConnection(
        "https://chat.googleapis.com/v1", (connection, context) -> {
            Map<String, Boolean> map = new HashMap<>();

            map.put("https://www.googleapis.com/auth/chat.admin.delete", false);
            map.put("https://www.googleapis.com/auth/chat.admin.memberships", false);
            map.put("https://www.googleapis.com/auth/chat.admin.memberships.readonly", false);
            map.put("https://www.googleapis.com/auth/chat.admin.spaces", false);
            map.put("https://www.googleapis.com/auth/chat.admin.spaces.readonly", false);
            map.put("https://www.googleapis.com/auth/chat.app.delete", false);
            map.put("https://www.googleapis.com/auth/chat.app.memberships", false);
            map.put("https://www.googleapis.com/auth/chat.app.messages.readonly", false);
            map.put("https://www.googleapis.com/auth/chat.app.spaces", false);
            map.put("https://www.googleapis.com/auth/chat.app.spaces.create", false);
            map.put("https://www.googleapis.com/auth/chat.customemojis", false);
            map.put("https://www.googleapis.com/auth/chat.customemojis.readonly", false);
            map.put("https://www.googleapis.com/auth/chat.delete", false);
            map.put("https://www.googleapis.com/auth/chat.import", false);
            map.put("https://www.googleapis.com/auth/chat.memberships", false);
            map.put("https://www.googleapis.com/auth/chat.memberships.app", false);
            map.put("https://www.googleapis.com/auth/chat.memberships.readonly", false);
            map.put("https://www.googleapis.com/auth/chat.messages", true);
            map.put("https://www.googleapis.com/auth/chat.messages.create", true);
            map.put("https://www.googleapis.com/auth/chat.messages.reactions", false);
            map.put("https://www.googleapis.com/auth/chat.messages.reactions.create", false);
            map.put("https://www.googleapis.com/auth/chat.messages.reactions.readonly", false);
            map.put("https://www.googleapis.com/auth/chat.messages.readonly", false);
            map.put("https://www.googleapis.com/auth/chat.spaces", true);
            map.put("https://www.googleapis.com/auth/chat.spaces.create", true);
            map.put("https://www.googleapis.com/auth/chat.spaces.readonly", false);
            map.put("https://www.googleapis.com/auth/chat.users.readstate", false);
            map.put("https://www.googleapis.com/auth/chat.users.readstate.readonly", false);
            map.put("https://www.googleapis.com/auth/chat.users.spacesettings", false);

            return map;
        });

    private GoogleChatConnection() {
    }
}
