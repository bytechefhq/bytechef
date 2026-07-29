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

package com.bytechef.component.daytona.connection;

import static com.bytechef.component.daytona.constant.DaytonaConstants.BASE_URL;
import static com.bytechef.component.daytona.constant.DaytonaConstants.DEFAULT_BASE_URL;
import static com.bytechef.component.definition.Authorization.AuthorizationType.BEARER_TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * Connection to the Daytona API. Authenticates with a personal API key sent as a Bearer token and targets the Daytona
 * Cloud API base URL by default (overridable for self-hosted deployments).
 *
 * @author Ivica Cardic
 */
public final class DaytonaConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .version(1)
        .authorizations(
            authorization(BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(TOKEN)
                        .label("API Key")
                        .description("Your Daytona API key.")
                        .required(true),
                    string(BASE_URL)
                        .label("Base URL")
                        .description("The Daytona API base URL. Change only for self-hosted deployments.")
                        .defaultValue(DEFAULT_BASE_URL)
                        .required(true)))
        .baseUri((connectionParameters, context) -> connectionParameters.getString(BASE_URL, DEFAULT_BASE_URL));

    private DaytonaConnection() {
    }
}
