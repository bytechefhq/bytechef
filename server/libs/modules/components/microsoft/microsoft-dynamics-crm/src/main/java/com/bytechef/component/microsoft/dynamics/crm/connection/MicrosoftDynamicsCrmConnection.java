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

package com.bytechef.component.microsoft.dynamics.crm.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.dynamics.crm.constant.MicrosoftDynamicsCrmConstants.PROXY_URL;
import static com.bytechef.component.microsoft.dynamics.crm.constant.MicrosoftDynamicsCrmConstants.TENANT_ID;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftDynamicsCrmConnection {

    private static final String HOST_URL = "hostUrl";

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
                    string(TENANT_ID)
                        .label("Tenant Id")
                        .defaultValue("common")
                        .required(true),
                    string(HOST_URL)
                        .label("Host URL")
                        .description("Host URL without trailing slash. For example **https://demo.crm.dynamics.com**")
                        .required(true),
                    string(PROXY_URL)
                        .label("Proxy URL with Port")
                        .description(
                            "Only to use for establishing connections (only needed when proxying requests). For " +
                                "example **https://proxy.com:8080**.")
                        .required(false))
                .authorizationUrl(
                    (parameters, context) -> "https://login.microsoftonline.com/"
                        + parameters.getRequiredString(TENANT_ID) + "/oauth2/v2.0/authorize")
                .tokenUrl(
                    (parameters, context) -> "https://login.microsoftonline.com/" +
                        parameters.getRequiredString(TENANT_ID) + "/oauth2/v2.0/token")
                .refreshUrl(
                    (parameters, context) -> "https://login.microsoftonline.com/" +
                        parameters.getRequiredString(TENANT_ID) + "/oauth2/v2.0/token")
                .scopes((connection, context) -> Map.of(
                    connection.getRequiredString(HOST_URL) + "/.default", true,
                    "openid", true,
                    "email", true,
                    "profile", true,
                    "offline_access", true))
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of(
                        "Accept", List.of("application/json"),
                        AUTHORIZATION, List.of("Bearer " + connectionParameters.getRequiredString(ACCESS_TOKEN)),
                        "OData-MaxVersion", List.of("4.0"),
                        "OData-Version", List.of("4.0"),
                        "Content-Type", List.of("application/json")))))
        .baseUri((connectionParameters, context) -> {
            String proxyUrl = connectionParameters.getString(PROXY_URL);

            return (proxyUrl == null || proxyUrl.isEmpty() ? connectionParameters.getRequiredString(HOST_URL)
                : proxyUrl) + "/api/data/v9.2";
        })
        .help("", "https://docs.bytechef.io/reference/components/microsoft-dynamics-crm_v1#connection-setup")
        .version(1);

    private MicrosoftDynamicsCrmConnection() {
    }
}
