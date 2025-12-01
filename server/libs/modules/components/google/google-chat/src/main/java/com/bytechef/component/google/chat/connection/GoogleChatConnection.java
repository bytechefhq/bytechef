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
import java.util.List;

/**
 * @author Nikolina Spehar
 */
public class GoogleChatConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = GoogleConnection.createConnection(
        "https://chat.googleapis.com/v1", (connection, context) -> List.of(
            "https://www.googleapis.com/auth/chat.spaces", "https://www.googleapis.com/auth/chat.spaces.create",
            "https://www.googleapis.com/auth/chat.messages", "https://www.googleapis.com/auth/chat.messages.create"));

    private GoogleChatConnection() {
    }
}
