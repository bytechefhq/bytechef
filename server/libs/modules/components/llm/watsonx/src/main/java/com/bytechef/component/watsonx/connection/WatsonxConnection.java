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

package com.bytechef.component.watsonx.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType.BEARER_TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.watsonx.constant.WatsonxConstants.PROJECT_ID;
import static com.bytechef.component.watsonx.constant.WatsonxConstants.STREAM_ENDPOINT;
import static com.bytechef.component.watsonx.constant.WatsonxConstants.TEXT_ENDPOINT;
import static constants.LLMConstants.URL;

import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;

/**
 * @author Monika Domiter
 */
public final class WatsonxConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://us-south.ml.cloud.ibm.com")
        .authorizations(
            authorization(BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(URL)
                        .label("Url")
                        .description("URL to connect to.")
                        .required(true),
                    string(STREAM_ENDPOINT)
                        .label("Stream Endpoint")
                        .description("The streaming endpoint.")
                        .defaultValue("generation/stream?version=2023-05-29")
                        .required(true),
                    string(TEXT_ENDPOINT)
                        .label("Text Endpoint")
                        .description("The text endpoint.")
                        .defaultValue("generation/text?version=2023-05-29")
                        .required(true),
                    string(PROJECT_ID)
                        .label("Project ID")
                        .description("The project ID.")
                        .required(true),
                    string(TOKEN)
                        .label("IAM Token")
                        .description("The IBM Cloud account IAM token.")
                        .required(true)
                    ));

    private WatsonxConnection() {
    }
}
