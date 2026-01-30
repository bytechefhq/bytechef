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

package com.bytechef.component.zoho.crm.connection;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.zoho.commons.ZohoConnection;
import java.util.Map;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class ZohoCrmConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = ZohoConnection.createConnection(
        "/crm/v7",
        Map.of(
            "ZohoCRM.users.ALL", true, "ZohoCRM.org.READ", true, "ZohoCRM.settings.roles.READ", true,
            "ZohoCRM.settings.profiles.READ", true),
        false);

    private ZohoCrmConnection() {
    }
}
