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

package com.bytechef.component.wolfram.alpha.full.results.connection;

import static com.bytechef.component.definition.Authorization.ADD_TO;
import static com.bytechef.component.definition.Authorization.ApiTokenLocation;
import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class WolframAlphaFullResultsConnection {
    public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.wolframalpha.com/v2")
        .authorizations(authorization(AuthorizationType.API_KEY)
            .title("API Key")
            .properties(
                string(KEY)
                    .label("Key")
                    .required(true)
                    .defaultValue("appid")
                    .hidden(true),
                string(VALUE)
                    .label("App ID")
                    .required(true),
                string(ADD_TO)
                    .label("Add to")
                    .required(true)
                    .defaultValue(ApiTokenLocation.QUERY_PARAMETERS.name())
                    .hidden(true)

            ));

    private WolframAlphaFullResultsConnection() {
    }
}
