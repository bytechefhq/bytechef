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

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.REGION;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDSL;
import java.util.List;

/**
 * @author Luka Ljubić
 */
public class ZohoCrmConnection {

    public static final ComponentDSL.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://accounts.zoho.eu/oauth/v2/auth")
        .authorizations(authorization(
            Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE.toLowerCase(),
            Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client secret")
                        .required(true),
                    string(REGION)
                        .label("Region")
                        .options(
                            option("zoho.eu (Europe)", "zoho.eu"),
                            option("zoho.com (United States)", "zoho.com"),
                            option("zoho.com.au (Australia)", "zoho.com.au"),
                            option("zoho.jp (Japan)", "zoho.jp"),
                            option("zoho.in (India)", "zoho.in"),
                            option("zohocloud.ca (Canada)", "zohocloud.ca"))
                        .required(true))
                .authorizationUrl((connection, context) -> "https://" + connection.getString(REGION) + "/oauth/v2/auth")
                .tokenUrl((connection, context) -> "https://" + connection.getString(REGION) + "/oauth/v2/token")
                .scopes((connection, context) -> List.of(
                    "ZohoCRM.users.ALL",
                    "ZohoCRM.org.ALL",
                    "ZohoCRM.settings.ALL",
                    "ZohoCRM.modules.ALL",
                    "ZohoCRM.bulk.ALL",
                    "ZohoCRM.bulk.backup.ALL",
                    "ZohoFiles.files.ALL")));

    private ZohoCrmConnection() {
    }
}
