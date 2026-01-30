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

package com.bytechef.component.zoho.commons;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
public class ZohoConnection {

    private static final String ORGANIZATION_ID = "organization_id";
    private static final String REGION = "region";

    private static final ModifiableStringProperty CLIENT_ID_PROPERTY = string(CLIENT_ID)
        .label("Client Id")
        .required(true);

    private static final ModifiableStringProperty CLIENT_SECRET_PROPERTY = string(CLIENT_SECRET)
        .label("Client Secret")
        .required(true);

    public static final ModifiableStringProperty ORGANIZATION_PROPERTY = string(ORGANIZATION_ID)
        .label("Organization Id")
        .required(true);

    private static final ModifiableStringProperty REGION_PROPERTY = string(REGION)
        .label("Region")
        .options(
            option("zoho.eu (Europe)", "zoho.eu"),
            option("zoho.com (United States)", "zoho.com"),
            option("zoho.com.au (Australia)", "zoho.com.au"),
            option("zoho.jp (Japan)", "zoho.jp"),
            option("zoho.in (India)", "zoho.in"),
            option("zohocloud.ca (Canada)", "zohocloud.ca"))
        .required(true);

    public static ModifiableConnectionDefinition createConnection(
        String baseUrl, Map<String, Boolean> scopes, boolean addOrganizationProperty) {

        return connection()
            .baseUri((connectionParameters, context) -> connectionParameters.getString("api_domain") + baseUrl)
            .authorizations(
                authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                    .title("OAuth2 Authorization Code")
                    .properties(
                        addOrganizationProperty
                            ? List.of(REGION_PROPERTY, ORGANIZATION_PROPERTY, CLIENT_ID_PROPERTY,
                                CLIENT_SECRET_PROPERTY)
                            : List.of(REGION_PROPERTY, CLIENT_ID_PROPERTY, CLIENT_SECRET_PROPERTY))
                    .authorizationUrl(
                        (connection, context) -> "https://accounts." + connection.getString(REGION) + "/oauth/v2/auth")
                    .tokenUrl(
                        (connection, context) -> "https://accounts." + connection.getString(REGION) + "/oauth/v2/token")
                    .refreshUrl(
                        (connection, context) -> "https://accounts." + connection.getString(REGION) + "/oauth/v2/token")
                    .scopes((connection, context) -> scopes)
                    .apply((connectionParameters, context) -> {
                        Map<String, List<String>> headers = new HashMap<>();

                        headers.put(AUTHORIZATION,
                            List.of("Zoho-oauthtoken " + connectionParameters.getRequiredString(ACCESS_TOKEN)));

                        if (addOrganizationProperty) {
                            headers.put(ORGANIZATION_ID,
                                List.of(connectionParameters.getRequiredString(ORGANIZATION_ID)));
                        }

                        return ApplyResponse.ofHeaders(headers);
                    }));
    }

    private ZohoConnection() {
    }
}
