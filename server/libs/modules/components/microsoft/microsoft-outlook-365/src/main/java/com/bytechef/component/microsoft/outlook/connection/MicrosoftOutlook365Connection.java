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

package com.bytechef.component.microsoft.outlook.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TENANT_ID;

import java.util.List;

/**
 * @author Monika Domiter
 * @author Ivica Cardic
 */
public class MicrosoftOutlook365Connection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true),
                    string(TENANT_ID)
                        .label("Tenant Id")
                        .defaultValue("common")
                        .required(true))
                .authorizationUrl(
                    (parameters, context) -> "https://login.microsoftonline.com/" + parameters.getString(TENANT_ID) +
                        "/oauth2/v2.0/authorize")
                .tokenUrl(
                    (parameters, context) -> "https://login.microsoftonline.com/" + parameters.getString(TENANT_ID) +
                        "/oauth2/v2.0/token")
                .scopes((connection, context) -> List.of("Mail.Read", "MailboxSettings.Read", "Mail.Send")));

    private MicrosoftOutlook365Connection() {
    }
}
