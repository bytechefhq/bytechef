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

package com.bytechef.component.ai.llm.router.litellm.connection;

import static com.bytechef.component.ai.llm.router.litellm.constant.LiteLLMConstants.BASE_URL;
import static com.bytechef.component.ai.llm.router.litellm.constant.LiteLLMConstants.DEFAULT_BASE_URL;
import static com.bytechef.component.definition.Authorization.AuthorizationType.BEARER_TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Aarish Yadav
 */
public final class LiteLLMConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .version(1)
        .properties(
            string(BASE_URL)
                .label("Base URL")
                .description(
                    "The base URL of your LiteLLM proxy server. Include the path prefix " +
                        "(e.g. http://localhost:4000/v1).")
                .defaultValue(DEFAULT_BASE_URL)
                .required(true))
        .baseUri((connectionParameters, context) -> connectionParameters.getString(BASE_URL, DEFAULT_BASE_URL))
        .authorizations(
            authorization(BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(TOKEN)
                        .label("API Key")
                        .description(
                            "The master key or virtual key for your LiteLLM proxy. " +
                                "Leave empty if your proxy does not require authentication.")
                        .required(false)));

    private LiteLLMConnection() {
    }
}
