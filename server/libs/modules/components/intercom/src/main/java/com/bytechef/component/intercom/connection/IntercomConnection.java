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

package com.bytechef.component.intercom.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Luka Ljubić
 */
public class IntercomConnection {
    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(authorization(
            AuthorizationType.OAUTH2_AUTHORIZATION_CODE.toLowerCase(),
            AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client secret")
                        .required(true))
                .apply(IntercomConnection::getApplyResponse)
                .authorizationUrl((connection, context) -> "https://app.intercom.com/oauth")
                .tokenUrl((connection, context) -> "https://api.intercom.io/auth/eagle/token")
                .scopes((connection, context) -> List.of()));

    private static ApplyResponse getApplyResponse(Parameters connectionParameters, Context context) {
        return ApplyResponse.ofHeaders(
            Map.of("Accept", List.of("application/json"),
                "Authorization", List.of("Bearer " + connectionParameters.getString(ACCESS_TOKEN))));
    }

    private IntercomConnection() {
    }
}
