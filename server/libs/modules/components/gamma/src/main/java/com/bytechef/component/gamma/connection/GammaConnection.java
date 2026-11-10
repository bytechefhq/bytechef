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

package com.bytechef.component.gamma.connection;

import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Ivica Cardic
 */
public class GammaConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://public-api.gamma.app/v1.0")
        .help("", "https://docs.bytechef.io/reference/components/gamma_v1#connection-setup")
        .version(1)
        .authorizations(
            authorization(AuthorizationType.API_KEY)
                .title("API Key")
                .properties(
                    string(KEY)
                        .label("Key")
                        .required(true)
                        .defaultValue("X-API-KEY")
                        .hidden(true),
                    string(VALUE)
                        .label("API Key")
                        .description("To create an API key go to Settings -> API Keys in Gamma.")
                        .required(true)));

    private GammaConnection() {
    }
}
