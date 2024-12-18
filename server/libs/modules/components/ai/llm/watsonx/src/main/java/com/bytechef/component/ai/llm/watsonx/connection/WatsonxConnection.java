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

package com.bytechef.component.ai.llm.watsonx.connection;

import static com.bytechef.component.ai.llm.constant.LLMConstants.URL;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.PROJECT_ID;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.STREAM_ENDPOINT;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.TEXT_ENDPOINT;
import static com.bytechef.component.definition.Authorization.AuthorizationType.BEARER_TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public final class WatsonxConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://eu-de.ml.cloud.ibm.com")
        .authorizations(
            authorization(BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(URL)
                        .label("Region")
                        .description("URL to connect to.")
                        .options(option("Dallas [us-south]", "https://us-south.ml.cloud.ibm.com"),
                            option("London [eu-gb]", "https://eu-gb.ml.cloud.ibm.com"),
                            option("Tokyo [jp-tok]", "https://jp-tok.ml.cloud.ibm.com"),
                            option("Frankfurt [eu-de]", "https://eu-de.ml.cloud.ibm.com"))
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
                        .required(true)));

    private WatsonxConnection() {
    }
}
