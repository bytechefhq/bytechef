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

package com.bytechef.component.openai.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType.BEARER_TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;
import static constants.LLMConstants.ENDPOINT;

import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;

/**
 * @author Monika Domiter
 */
public final class AzureOpenAIConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
//        .baseUri((connectionParameters, context) -> "https://api.openai.com/v1")
        .authorizations(
            authorization(BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(ENDPOINT)
                        .label("Endpoint")
//                        .description("e.g https://{yourDomain}}.atlassian.net")
                        .required(true),
                    string(TOKEN)
                        .label("Token")
                        .required(true)));

    private AzureOpenAIConnection() {
    }
}