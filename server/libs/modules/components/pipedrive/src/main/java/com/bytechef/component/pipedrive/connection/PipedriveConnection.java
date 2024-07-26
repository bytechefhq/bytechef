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

package com.bytechef.component.pipedrive.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class PipedriveConnection {
    public static final ComponentDSL.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.pipedrive.com/v1")
        .authorizations(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .title("OAuth2 Authorization Code")
            .properties(
                string(CLIENT_ID)
                    .label("Client Id")
                    .required(true),
                string(CLIENT_SECRET)
                    .label("Client Secret")
                    .required(true))
            .authorizationUrl((connectionParameters, context) -> "https://oauth.pipedrive.com/oauth/authorize")
            .scopes((connection, context) -> List.of("deals:read", "deals:full", "goals:read", "goals:full",
                "leads:read", "leads:full", "activities:read", "activities:full", "contacts:read", "contacts:full",
                "admin", "recents:read", "search:read", "mail:read", "mail:full", "products:read", "products:full",
                "users:read", "base", "phone-integration"))
            .tokenUrl((connectionParameters, context) -> "https://oauth.pipedrive.com/oauth/token")
            .refreshUrl((connectionParameters, context) -> "https://oauth.pipedrive.com/oauth/token"));

    private PipedriveConnection() {
    }
}
