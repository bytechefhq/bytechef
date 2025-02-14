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

package com.bytechef.component.salesforce.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.ENVIRONMENT;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class SalesforceConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> getUrl(connectionParameters, "services/data/v63.0"))
        .authorizations(authorization(
            Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(ENVIRONMENT)
                        .label("Environment")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl(
                    (connectionParameters, context) -> getUrl(connectionParameters, "services/oauth2/authorize"))
                .refreshUrl((connectionParameters, context) -> getUrl(connectionParameters, "services/oauth2/token"))
                .scopes((connectionParameters, context) -> List.of("full", "refresh_token", "offline_access"))
                .tokenUrl((connectionParameters, context) -> getUrl(connectionParameters, "services/oauth2/token")));

    private SalesforceConnection() {
    }

    private static String getUrl(Parameters connectionParameters, String path) {
        String environment = connectionParameters.getRequiredString(ENVIRONMENT);

        return "https://%s/%s".formatted(environment, path);
    }
}
