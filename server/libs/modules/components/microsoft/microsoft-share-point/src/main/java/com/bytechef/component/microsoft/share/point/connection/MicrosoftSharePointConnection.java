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

package com.bytechef.component.microsoft.share.point.connection;

import static com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

import com.bytechef.microsoft.commons.MicrosoftConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftSharePointConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = MicrosoftConnection.createConnection(
        (connection, context) -> {
            Map<String, Boolean> map = new HashMap<>();

            map.put("Files.Read", false);
            map.put("Files.Read.All", false);
            map.put("Files.ReadWrite", false);
            map.put("Files.ReadWrite.All", false);
            map.put("SharePointTenantSettings.Read.All", false);
            map.put("SharePointTenantSettings.ReadWrite.All", false);
            map.put("Sites.FullControl.All", false);
            map.put("Sites.Manage.All", true);
            map.put("Sites.Read.All", true);
            map.put("Sites.ReadWrite.All", true);
            map.put("Sites.Selected", false);
            map.put("offline_access", true);

            return map;
        });

    private MicrosoftSharePointConnection() {
    }
}
