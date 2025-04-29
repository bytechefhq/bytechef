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

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;

import com.bytechef.component.definition.Authorization.ApplyFunction;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.ScopesFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ConnectionDefinition.BaseUriFunction;
import com.bytechef.component.definition.Property;

/**
 * @author Marija Horvat
 */
public class ZohoConnection {

    private static final String REGION = "region";

    public static ModifiableConnectionDefinition createConnection(
        BaseUriFunction baseUri, ScopesFunction scopes, ApplyFunction apply, Property... properties) {
        return connection()
            .baseUri(baseUri)
            .authorizations(
                authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                    .title("OAuth2 Authorization Code")
                    .properties(properties)
                    .authorizationUrl(
                        (connection, context) -> "https://accounts." + connection.getString(REGION) + "/oauth/v2/auth")
                    .tokenUrl(
                        (connection, context) -> "https://accounts." + connection.getString(REGION) + "/oauth/v2/token")
                    .refreshUrl(
                        (connection, context) -> "https://accounts." + connection.getString(REGION) + "/oauth/v2/token")
                    .scopes(scopes)
                    .apply(apply));
    }

    private ZohoConnection() {
    }
}
