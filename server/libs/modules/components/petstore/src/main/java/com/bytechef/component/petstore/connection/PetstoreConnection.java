
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

package com.bytechef.component.petstore.connection;

import static com.bytechef.hermes.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.hermes.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.hermes.component.definition.Authorization.KEY;
import static com.bytechef.hermes.component.definition.Authorization.VALUE;
import static com.bytechef.hermes.component.definition.ComponentDSL.authorization;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class PetstoreConnection {
    public static final ComponentDSL.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri(connection -> "https://petstore3.swagger.io/api/v3")
        .authorizations(authorization(
            Authorization.AuthorizationType.OAUTH2_IMPLICIT_CODE.name()
                .toLowerCase(),
            Authorization.AuthorizationType.OAUTH2_IMPLICIT_CODE)
            .display(
                display("OAuth2 Implicit"))
            .properties(
                string(CLIENT_ID)
                    .label("Client Id")
                    .required(true),
                string(CLIENT_SECRET)
                    .label("Client Secret")
                    .required(true))
            .authorizationUrl(connection -> "https://petstore3.swagger.io/oauth/authorize")
            .scopes(connection -> List.of("write:pets", "read:pets")),
            authorization(
                Authorization.AuthorizationType.API_KEY.name()
                    .toLowerCase(),
                Authorization.AuthorizationType.API_KEY)
                .display(
                    display("API Key"))
                .properties(
                    string(KEY)
                        .label("Key")
                        .required(true)
                        .defaultValue("api_key")
                        .hidden(true),
                    string(VALUE)
                        .label("Value")
                        .required(true)

                ));
}
