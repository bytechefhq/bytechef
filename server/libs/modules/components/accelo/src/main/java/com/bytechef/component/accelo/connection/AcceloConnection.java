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

package com.bytechef.component.accelo.connection;

import static com.bytechef.component.accelo.constant.AcceloConstants.DEPLOYMENT;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class AcceloConnection {

    private AcceloConnection() {
    }

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(DEPLOYMENT)
                        .label("Deployment")
                        .description(
                            "Actual deployment identifier or name to target a specific deployment within the " +
                                "Accelo platform.")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connection, context) -> "https://" + connection.getRequiredString(DEPLOYMENT) +
                    ".api.accelo.com/oauth2/v0/authorize")
                .scopes((connection, context) -> List.of("write(all)"))
                .tokenUrl((connection, context) -> "https://" + connection.getRequiredString(DEPLOYMENT) +
                    ".api.accelo.com/oauth2/v0/token"));
}
