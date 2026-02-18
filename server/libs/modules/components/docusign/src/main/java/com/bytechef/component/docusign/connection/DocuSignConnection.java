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

package com.bytechef.component.docusign.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.docusign.constant.DocuSignConstants.ACCOUNT_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.BASE_URI;
import static com.bytechef.component.docusign.constant.DocuSignConstants.ENVIRONMENT;
import static com.bytechef.component.docusign.util.DocuSignUtils.getAuthorizationUrl;

import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TypeReference;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.naming.ConfigurationException;

/**
 * @author Nikolina Spehar
 */
public class DocuSignConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> connectionParameters.getRequiredString(BASE_URI))
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .properties(
                    string(CLIENT_ID)
                        .label("Integration Key")
                        .description("DocuSign app integration key.")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Secret Key")
                        .description("DocuSign app secret key.")
                        .required(true),
                    string(ACCOUNT_ID)
                        .label("API Account ID")
                        .description("DocuSign API account ID.")
                        .required(true),
                    string(ENVIRONMENT)
                        .label("Environment")
                        .description("Environment of the connection.")
                        .options(option("Demo / Test", "demo"),
                            option("US production", "www"),
                            option("EU production", "eu"))
                        .required(true),
                    string(BASE_URI)
                        .label("Account Base URI")
                        .description("DocuSign account base URI.")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> getAuthorizationUrl(
                    connectionParameters.getRequiredString(ENVIRONMENT)) + "auth")
                .tokenUrl((connectionParameters, context) -> getAuthorizationUrl(
                    connectionParameters.getRequiredString(ENVIRONMENT)) + TOKEN)
                .refreshUrl((connectionParameters, context) -> getAuthorizationUrl(
                    connectionParameters.getRequiredString(ENVIRONMENT)) + TOKEN)
                .authorizationCallback((connectionParameters, code, redirectUri, codeVerifier, context) -> {
                    String clientId = connectionParameters.getString(CLIENT_ID);
                    String clientSecret = connectionParameters.getString(CLIENT_SECRET);
                    String valueToEncode = clientId + ":" + clientSecret;
                    String encode = context.encoder(
                        encoder -> encoder.base64Encode(valueToEncode.getBytes(StandardCharsets.UTF_8)));

                    Http.Response httpResponse =
                        context.http(http -> http.post(getAuthorizationUrl(
                            connectionParameters.getRequiredString(ENVIRONMENT)) + "token"))
                            .headers(
                                Map.of(
                                    "Accept", List.of("application/json"),
                                    "Authorization", List.of("Basic " + encode)))
                            .body(Http.Body.of(
                                Map.of(
                                    "code", code,
                                    "grant_type", "authorization_code")))
                            .configuration(Http.responseType(Http.ResponseType.JSON))
                            .execute();

                    if (httpResponse.getStatusCode() < 200 || httpResponse.getStatusCode() > 299) {
                        throw new ConfigurationException("Invalid claim");
                    }

                    if (httpResponse.getBody() == null) {
                        throw new ConfigurationException("Invalid claim");
                    }

                    return new AuthorizationCallbackResponse(httpResponse.getBody(new TypeReference<>() {}));
                }));

    private DocuSignConnection() {
    }
}
