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

package com.bytechef.component.xero.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.ApplyResponse.ofHeaders;
import static com.bytechef.component.definition.Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 */
public class XeroConnection {
    public static final ComponentDSL.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(OAUTH2_AUTHORIZATION_CODE.toLowerCase(), OAUTH2_AUTHORIZATION_CODE)
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
                .scopes((connection, context) -> List.of("accounting.contacts", "accounting.transactions"))
                .tokenUrl((connection, context) -> "https://identity.xero.com/connect/token"));

    private static ApplyResponse getApplyResponse(Parameters connectionParameters, Context context) {
        return ofHeaders(
            Map.of(
                AUTHORIZATION, List.of("Bearer " + connectionParameters.getRequiredString(ACCESS_TOKEN)),
                "Xero-tenant-id", List.of(getTenantId(connectionParameters.getRequiredString(ACCESS_TOKEN), context))));
    }

    private XeroConnection() {
    }

    public static String getTenantId(String accessToken, Context context) {
        Http.Response response = context
            .http(http -> http.get("https://api.xero.com/connections"))
            .body(
                Http.Body.of(
                    Map.of(
                        "Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json")))
            .header(AUTHORIZATION, "Bearer " + accessToken)
            .configuration(
                Http
                    .responseType(Http.ResponseType.JSON)
                    .disableAuthorization(true))
            .execute();

        Object body = response.getBody();
        if (body instanceof LinkedHashMap<?, ?>) {
            return ((LinkedHashMap<String, String>) body).get("tenantId");
        }

        if (body instanceof ArrayList<?>) {
            ArrayList<Object> tenantList = (ArrayList) response.getBody();

            if (tenantList.get(0) instanceof LinkedHashMap<?, ?>) {
                return ((LinkedHashMap<String, String>) tenantList.get(0)).get("tenantId");
            }
        }
        throw new RuntimeException("Xero did not return any Tenants.");
    }
}
