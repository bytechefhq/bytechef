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

package com.bytechef.component.google.contacts.connection;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.google.commons.GoogleConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class GoogleContactsConnection {

    private GoogleContactsConnection() {
    }

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = GoogleConnection.createConnection(
        null,
        (connection, context) -> {
            Map<String, Boolean> map = new HashMap<>();

            map.put("https://www.googleapis.com/auth/contacts", true);
            map.put("https://www.googleapis.com/auth/contacts.other.readonly", false);
            map.put("https://www.googleapis.com/auth/contacts.readonly", false);
            map.put("https://www.googleapis.com/auth/directory.readonly", false);
            map.put("https://www.googleapis.com/auth/user.addresses.read", false);
            map.put("https://www.googleapis.com/auth/user.birthday.read", false);
            map.put("https://www.googleapis.com/auth/user.emails.read", false);
            map.put("https://www.googleapis.com/auth/user.gender.read", false);
            map.put("https://www.googleapis.com/auth/user.organization.read", false);
            map.put("https://www.googleapis.com/auth/user.phonenumbers.read", false);
            map.put("https://www.googleapis.com/auth/userinfo.email", false);
            map.put("https://www.googleapis.com/auth/userinfo.profile", false);

            return map;
        });
}
