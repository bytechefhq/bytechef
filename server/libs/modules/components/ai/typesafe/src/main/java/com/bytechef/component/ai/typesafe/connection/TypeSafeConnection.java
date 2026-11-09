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

package com.bytechef.component.ai.typesafe.connection;

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.BASE_URL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.DEFAULT_BASE_URL;
import static com.bytechef.component.definition.Authorization.AuthorizationType.BEARER_TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Ivica Cardic
 */
public final class TypeSafeConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> connectionParameters.getString(BASE_URL, DEFAULT_BASE_URL))
        .version(1)
        .help("", "https://docs.bytechef.io/reference/components/typesafe_v1#connection-setup")
        .authorizations(
            authorization(BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(TOKEN)
                        .label("Token")
                        .description(
                            "Your TypeSafe AI API key. A self-hosted System One server that does not check keys " +
                                "accepts any value.")
                        .required(true),
                    string(BASE_URL)
                        .label("Base URL")
                        .description(
                            "The System One server that answers `POST /v1/systemone`. Keep the default for Jev, or " +
                                "point it at a self-hosted server of an open decision model, such as Laya " +
                                "(http://localhost:8000) or Ollama (http://localhost:11434).")
                        .defaultValue(DEFAULT_BASE_URL)
                        .required(true)));

    private TypeSafeConnection() {
    }
}
