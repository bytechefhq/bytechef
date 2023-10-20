
/*
 * Copyright 2021 <your company/name>.
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

import static com.bytechef.hermes.component.definition.Authorization.ADD_TO;
import static com.bytechef.hermes.component.definition.Authorization.ApiTokenLocation;
import static com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import static com.bytechef.hermes.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.hermes.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.hermes.component.definition.Authorization.VALUE;
import static com.bytechef.hermes.component.definition.ComponentDSL.authorization;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class PipedriveConnection {
    public static final ComponentDSL.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri(connection -> "https://api.pipedrive.com/v1")
        .authorizations(authorization(
            AuthorizationType.API_KEY.toLowerCase(), AuthorizationType.API_KEY)
                .display(
                    display("API Key"))
                .properties(

                    string(VALUE)
                        .label("Value")
                        .required(true),
                    string(ADD_TO)
                        .label("Add to")
                        .required(true)
                        .defaultValue(ApiTokenLocation.QUERY_PARAMETERS.name())
                        .hidden(true)

                ), authorization(
                    AuthorizationType.OAUTH2_AUTHORIZATION_CODE.toLowerCase(),
                    AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                        .display(
                            display("OAuth2 Authorization Code"))
                        .properties(
                            string(CLIENT_ID)
                                .label("Client Id")
                                .required(true),
                            string(CLIENT_SECRET)
                                .label("Client Secret")
                                .required(true))
                        .authorizationUrl(connection -> "https://oauth.pipedrive.com/oauth/authorize")
                        .scopes(connection -> List.of("deals:full", "contacts:full", "search:read", "leads:read",
                            "leads:full", "contacts:read", "deals:read"))
                        .tokenUrl(connection -> "https://oauth.pipedrive.com/oauth/token")
                        .refreshUrl(connection -> "https://oauth.pipedrive.com/oauth/token"));
}
