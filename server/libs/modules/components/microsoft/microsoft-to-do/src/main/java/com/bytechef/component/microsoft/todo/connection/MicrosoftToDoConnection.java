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

package com.bytechef.component.microsoft.todo.connection;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.microsoft.commons.MicrosoftConnection;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftToDoConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = MicrosoftConnection.createConnection(
        (connection, context) -> Map.of(
            "Tasks.Read", false,
            "Tasks.Read.All", false,
            "Tasks.ReadWrite", true,
            "offline_access", true));

    private MicrosoftToDoConnection() {
    }
}
