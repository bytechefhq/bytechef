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

package com.bytechef.component.linkedin.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE;
import static com.bytechef.component.definition.Authorization.BEARER;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class LinkedInConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.linkedin.com")
        .authorizations(
            authorization(OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connection, context) -> "https://www.linkedin.com/oauth/v2/authorization")
                .scopes((connection, context) -> List.of(
                    "w_member_social", "openid", "email", "profile", "w_organization_social", "r_organization_social"))
                .tokenUrl((connection, context) -> "https://www.linkedin.com/oauth/v2/accessToken")
                .refreshUrl((connectionParameters, context) -> "https://www.linkedin.com/oauth/v2/accessToken")
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of(
                        "X-Restli-Protocol-Version", List.of("2.0.0"),
                        "Linkedin-Version", List.of("202504"),
                        AUTHORIZATION, List.of(BEARER + " " + connectionParameters.getRequiredString(ACCESS_TOKEN))))));

    private LinkedInConnection() {
    }
}
