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

package com.bytechef.component.xero.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.ApplyResponse.ofHeaders;
import static com.bytechef.component.definition.Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE;
import static com.bytechef.component.definition.Authorization.BEARER;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 */
public class XeroConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.xero.com/api.xro/2.0")
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
                .apply(XeroConnection::getApplyResponse)
                .authorizationUrl((connection, context) -> "https://login.xero.com/identity/connect/authorize")
                .scopes((connection, context) -> Map.of(
                    "accounting.contacts", true, "accounting.transactions", true, "accounting.settings.read", true,
                    "offline_access", true))
                .tokenUrl((connection, context) -> "https://identity.xero.com/connect/token")
                .refreshUrl((connection, context) -> "https://identity.xero.com/connect/token"));

    private static ApplyResponse getApplyResponse(Parameters connectionParameters, Context context) {
        return ofHeaders(
            Map.of(
                AUTHORIZATION, List.of(BEARER + " " + connectionParameters.getRequiredString(ACCESS_TOKEN)),
                "Xero-tenant-id", List.of(getTenantId(connectionParameters.getRequiredString(ACCESS_TOKEN), context))));
    }

    private XeroConnection() {
    }

    private static String getTenantId(String accessToken, Context context) {
        Http.Response response = context
            .http(http -> http.get("https://api.xero.com/connections"))
            .body(
                Body.of(
                    Map.of(
                        "Authorization", BEARER + " " + accessToken,
                        "Content-Type", "application/json")))
            .header(AUTHORIZATION, BEARER + " " + accessToken)
            .configuration(
                Http.responseType(Http.ResponseType.JSON)
                    .disableAuthorization(true))
            .execute();

        Object body = response.getBody();

        if (body instanceof Map<?, ?> map) {
            return (String) map.get("tenantId");
        }

        if (body instanceof List<?>) {
            List<?> tenantList = (List<?>) response.getBody();

            if (tenantList.getFirst() instanceof Map<?, ?> map) {
                return (String) map.get("tenantId");
            }
        }
        throw new RuntimeException("Xero did not return any Tenants.");
    }
}
