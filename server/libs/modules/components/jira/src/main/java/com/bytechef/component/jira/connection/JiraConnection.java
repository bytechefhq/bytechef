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

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.jira.constant.JiraConstants.YOUR_DOMAIN;
import static com.bytechef.component.jira.util.JiraUtils.getBaseUrl;

import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;

/**
 * @author Monika Domiter
 */
public class JiraConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(AuthorizationType.BASIC_AUTH.toLowerCase(), AuthorizationType.BASIC_AUTH)
                .title("Basic Auth")
                .properties(
                    string(YOUR_DOMAIN)
                        .label("Your domain")
                        .description("e.g https://{yourDomain}}.atlassian.net")
                        .required(true),
                    string(USERNAME)
                        .label("Email")
                        .description("The email used to log in to Jira")
                        .required(true),
                    string(PASSWORD)
                        .label("API token")
                        .required(true)))
        .baseUri((connectionParameters, context) -> getBaseUrl(connectionParameters));

    private JiraConnection() {
    }
}
