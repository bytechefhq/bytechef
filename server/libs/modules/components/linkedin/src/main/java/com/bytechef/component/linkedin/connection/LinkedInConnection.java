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

package com.bytechef.component.linkedin.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE;
import static com.bytechef.component.definition.Authorization.BEARER;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class LinkedInConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.linkedin.com")
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
                .authorizationUrl((connection, context) -> "https://www.linkedin.com/oauth/v2/authorization")
                .scopes((connection, context) -> {
                    Map<String, Boolean> map = new HashMap<>();

                    map.put("email", true);
                    map.put("openid", true);
                    map.put("profile", true);
                    map.put("r_1st_connections_size", false);
                    map.put("r_ads", false);
                    map.put("r_ads_reporting", false);
                    map.put("r_basicprofile", false);
                    map.put("r_compliance", false);
                    map.put("r_events", false);
                    map.put("r_liteprofile", false);
                    map.put("r_organization_admin", false);
                    map.put("r_organization_followers", false);
                    map.put("r_organization_social", true);
                    map.put("r_organization_social_feed", false);
                    map.put("r_sales_nav_analytics", false);
                    map.put("r_sales_nav_display", false);
                    map.put("r_sales_nav_profiles", false);
                    map.put("r_sales_nav_validation", false);
                    map.put("rw_ads", false);
                    map.put("rw_dmp_segments", false);
                    map.put("rw_events", false);
                    map.put("rw_organization_admin", false);
                    map.put("w_compliance", false);
                    map.put("w_member_social", true);
                    map.put("w_member_social_feed", false);
                    map.put("w_organization_social", true);
                    map.put("w_organization_social_feed", false);

                    return map;
                })
                .tokenUrl((connection, context) -> "https://www.linkedin.com/oauth/v2/accessToken")
                .refreshUrl((connectionParameters, context) -> "https://www.linkedin.com/oauth/v2/accessToken")
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of(
                        "X-Restli-Protocol-Version", List.of("2.0.0"),
                        "Linkedin-Version", List.of("202504"),
                        AUTHORIZATION, List.of(BEARER + " " + connectionParameters.getRequiredString(ACCESS_TOKEN))))));

    private LinkedInConnection() {
    }
}
