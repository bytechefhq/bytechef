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

package com.bytechef.component.myob.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MyobConnection {

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
                    string(KEY)
                        .label("API key")
                        .description("The API key registered in https://my.myob.com.au/au/bd/DevAppList.aspx")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> "https://secure.myob.com/oauth2/v1/authorize")
                .tokenUrl((connectionParameters, context) -> "https://secure.myob.com/oauth2/v1/token")
                .refreshUrl((connectionParameters, context) -> "https://secure.myob.com/oauth2/v1/token")
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of(
                        "x-myobapi-key", List.of(connectionParameters.getRequiredString(KEY)),
                        "x-myobapi-version", List.of("v2"),
                        "Accept-Encoding", List.of("gzip,deflate")))));

    private MyobConnection() {
    }
}
