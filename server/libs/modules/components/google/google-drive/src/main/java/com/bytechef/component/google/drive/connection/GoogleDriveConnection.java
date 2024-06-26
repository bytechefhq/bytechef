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

package com.bytechef.component.google.drive.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import java.util.List;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
public class GoogleDriveConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(authorization(
            AuthorizationType.OAUTH2_AUTHORIZATION_CODE.toLowerCase(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connection, context) -> " https://accounts.google.com/o/oauth2/v2/auth")
                .refreshUrl((connectionParameters, context) -> GoogleOAuthConstants.TOKEN_SERVER_URL)
                .refreshOn("^.*(4\\d\\d)(\\s(Unauthorized)?.*)?$")
                .scopes((connection, context) -> List.of("https://www.googleapis.com/auth/drive"))
                .tokenUrl((connection, context) -> "https://oauth2.googleapis.com/token"));

    private GoogleDriveConnection() {
    }
}
