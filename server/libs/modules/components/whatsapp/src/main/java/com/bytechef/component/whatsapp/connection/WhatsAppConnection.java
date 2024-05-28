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

package com.bytechef.component.whatsapp.connection;

import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.PHONE_NUMBER_ID;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.SYSTEM_USER_ACCESS_TOKEN;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDSL;

/**
 * @author Luka Ljubic
 */
public class WhatsAppConnection {

    public static final ComponentDSL.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(authorization(
            Authorization.AuthorizationType.CUSTOM.toLowerCase(), Authorization.AuthorizationType.CUSTOM)
                .title("WhatsApp Custom Authorization")
                .properties(
                    string(SYSTEM_USER_ACCESS_TOKEN)
                        .label("System user access token")
                        .required(true),
                    string(PHONE_NUMBER_ID)
                        .label("Phone number ID")
                        .required(true)));

    private WhatsAppConnection() {
    }
}
