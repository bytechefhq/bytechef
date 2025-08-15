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

package com.bytechef.component.jenkins.connection;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jenkins.constant.JenkinsConstants.BASE_URI;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Nikolina Spehar
 */
public class JenkinsConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> connectionParameters.getRequiredString(BASE_URI))
        .authorizations(
            authorization(AuthorizationType.BASIC_AUTH)
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .description("Jenkins username.")
                        .required(true),
                    string(PASSWORD)
                        .label("API Token")
                        .description("Jenkins API token.")
                        .required(true),
                    string(BASE_URI)
                        .label("Base URI")
                        .description("Complete base URI of your jenkins server.")
                        .required(true)));

    private JenkinsConnection() {
    }
}
