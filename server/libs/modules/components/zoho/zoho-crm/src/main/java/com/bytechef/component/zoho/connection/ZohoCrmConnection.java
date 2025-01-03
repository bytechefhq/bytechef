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

package com.bytechef.component.zoho.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.REGION;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class ZohoCrmConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> connectionParameters.getString("api_domain") + "/crm/v7")
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(REGION)
                        .label("Region")
                        .options(
                            option("zoho.eu (Europe)", "zoho.eu"),
                            option("zoho.com (United States)", "zoho.com"),
                            option("zoho.com.au (Australia)", "zoho.com.au"),
                            option("zoho.jp (Japan)", "zoho.jp"),
                            option("zoho.in (India)", "zoho.in"),
                            option("zohocloud.ca (Canada)", "zohocloud.ca"))
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl(
                    (connection, context) -> "https://accounts." + connection.getString(REGION) + "/oauth/v2/auth")
                .tokenUrl(
                    (connection, context) -> "https://accounts." + connection.getString(REGION) + "/oauth/v2/token")
                .refreshUrl(
                    (connection, context) -> "https://accounts." + connection.getString(REGION) + "/oauth/v2/token")
                .scopes((connection, context) -> List.of("ZohoCRM.users.ALL", "ZohoCRM.org.READ",
                    "ZohoCRM.settings.roles.READ", "ZohoCRM.settings.profiles.READ"))
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of(AUTHORIZATION,
                        List.of("Zoho-oauthtoken " + connectionParameters.getRequiredString(ACCESS_TOKEN))))));

    private ZohoCrmConnection() {
    }
}
