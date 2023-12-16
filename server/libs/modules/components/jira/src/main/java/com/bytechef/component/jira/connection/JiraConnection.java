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

package com.bytechef.component.jira.connection;

import static com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import static com.bytechef.hermes.component.definition.ComponentDSL.authorization;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.CLIENT_ID;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.CLIENT_SECRET;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.PASSWORD;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.USERNAME;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class JiraConnection {
    public static final ComponentDSL.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://your-domain.atlassian.net")
        .authorizations(authorization(
            AuthorizationType.BASIC_AUTH.toLowerCase(), AuthorizationType.BASIC_AUTH)
                .title("Basic Auth")
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .required(true)),
            authorization(
                AuthorizationType.OAUTH2_AUTHORIZATION_CODE.toLowerCase(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                    .title("OAuth2 Authorization Code")
                    .properties(
                        string(CLIENT_ID)
                            .label("Client Id")
                            .required(true),
                        string(CLIENT_SECRET)
                            .label("Client Secret")
                            .required(true))
                    .authorizationUrl((connectionParameters, context) -> "https://auth.atlassian.com/authorize")
                    .scopes((connection, context) -> List.of("read:jira-work", "write:jira-work"))
                    .tokenUrl((connectionParameters, context) -> "https://auth.atlassian.com/oauth/token"));
}
