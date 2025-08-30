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

package com.bytechef.component.clickup.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import javax.naming.ConfigurationException;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class ClickupConnection {
    public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.clickup.com/api/v2")
        .authorizations(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .title("OAuth2 Authorization Code")
            .properties(
                string(CLIENT_ID)
                    .label("Client Id")
                    .required(true),
                string(CLIENT_SECRET)
                    .label("Client Secret")
                    .required(true))
            .authorizationUrl((connectionParameters, context) -> "https://app.clickup.com/api")
            .tokenUrl((connectionParameters, context) -> "https://app.clickup.com/api/v2/oauth/token")
            .authorizationCallback((connectionParameters, code, redirectUri, codeVerifier, context) -> {
                String clientId = connectionParameters.getString(CLIENT_ID);
                String clientSecret = connectionParameters.getString(CLIENT_SECRET);

                Http.Response response = context
                    .http(http -> http.post("https://api.clickup.com/api/v2/oauth/token"))
                    .headers(
                        Map.of(
                            "Accept", List.of("application/json")))
                    .body(Http.Body.of(
                        Map.of(
                            "code", code,
                            "client_id", clientId,
                            "client_secret", clientSecret)))
                    .configuration(Http.responseType(Http.ResponseType.JSON))
                    .execute();

                if (response.getStatusCode() < 200 || response.getStatusCode() > 299) {
                    throw new ConfigurationException("Invalid claim");
                }

                if (response.getBody() == null) {
                    throw new ConfigurationException("Invalid claim");
                }

                return new AuthorizationCallbackResponse(response.getBody(new TypeReference<>() {}));
            }));

    private ClickupConnection() {
    }
}
