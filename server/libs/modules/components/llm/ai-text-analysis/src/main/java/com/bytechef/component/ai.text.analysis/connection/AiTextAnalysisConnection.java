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

package com.bytechef.component.ai.text.analysis.connection;

import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.ACCESS_KEY_ID;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.REGION;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.definition.Authorization.AuthorizationType.BEARER_TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ConnectionDefinition.BASE_URI;
import static com.bytechef.component.llm.constant.LLMConstants.ENDPOINT;
import static com.bytechef.component.llm.constant.LLMConstants.URL;
import static com.bytechef.component.vertex.gemini.constant.VertexGeminiConstants.LOCATION;
import static com.bytechef.component.vertex.gemini.constant.VertexGeminiConstants.PROJECT_ID;
import static com.bytechef.component.watsonx.constant.WatsonxConstants.STREAM_ENDPOINT;
import static com.bytechef.component.watsonx.constant.WatsonxConstants.TEXT_ENDPOINT;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.watsonx.constant.WatsonxConstants;

/**
 * @author Marko Kriskovic
 */
public final class AiTextAnalysisConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(Authorization.AuthorizationType.CUSTOM)
                .title("Amazon Bedrock")
                .properties(
                    string(ACCESS_KEY_ID)
                        .label("Access Key ID")
                        .required(true),
                    string(SECRET_ACCESS_KEY)
                        .label("Secret Access Key")
                        .required(true),
                    string(REGION)
                        .options(
                            option("US East (N. Virginia) [us-east-1]", "us-east-1"),
                            option("US West (Oregon) [us-west-2]", "us-west-2"),
                            option("Asia Pacific (Mumbai) [ap-south-1]", "ap-south-1"),
                            option("Asia Pacific (Singapore) [ap-southeast-1]", "ap-southeast-1"),
                            option("Asia Pacific (Sydney) [ap-southeast-2]", "ap-southeast-2"),
                            option("Asia Pacific (Tokyo) [ap-northeast-1]", "ap-northeast-1"),
                            option("Canada (Central) [ca-central-1]", "ca-central-1"),
                            option("Europe (Frankfurt) [eu-central-1]", "eu-central-1"),
                            option("Europe (Ireland) [eu-west-1]", "eu-west-1"),
                            option("Europe (London) [eu-west-2]", "eu-west-2"),
                            option("Europe (Paris) [eu-west-3]", "eu-west-3"),
                            option("South America (São Paulo) [sa-east-1]", "sa-east-1"))
                        .required(true)
                        .defaultValue("us-east-1")),
            authorization(BEARER_TOKEN)
                .title("Anthropic")
                .properties(
                    string(TOKEN)
                        .label("Token")
                        .required(true)),
            authorization(BEARER_TOKEN)
                .title("Azure Open AI")
                .properties(
                    string(ENDPOINT)
                        .label("Endpoint")
                        .required(true),
                    string(TOKEN)
                        .label("Token")
                        .required(true)),
            authorization(BEARER_TOKEN)
                .title("Groq")
                .properties(
                    string(TOKEN)
                        .label("Token")
                        .required(true)),
            authorization(BEARER_TOKEN)
                .title("Hugging Face")
                .properties(
                    string(TOKEN)
                        .label("Token")
                        .required(true)),
            authorization(BEARER_TOKEN)
                .title("Mistral")
                .properties(
                    string(TOKEN)
                        .label("Token")
                        .required(true)),
            authorization(BEARER_TOKEN)
                .title("NVIDIA")
                .properties(
                    string(TOKEN)
                        .label("Token")
                        .required(true)),
            authorization(BEARER_TOKEN)
                .title("Open AI")
                .properties(
                    string(TOKEN)
                        .label("Token")
                        .required(true)),
            authorization(BEARER_TOKEN)
                .title("Vertex Gemini")
                .properties(
                    string(PROJECT_ID)
                        .label("Project Id")
                        .description("Google Cloud Platform project ID")
                        .required(true),
                    string(LOCATION)
                        .label("Location")
                        .description("Region")
                        .options(option("Taiwan, asia-east1", "asia-east1"),
                            option("China, asia-east2", "asia-east2"),
                            option("Japan, asia-northeast1", "asia-northeast1"),
                            option("South Korea, asia-northeast3", "asia-northeast3"),
                            option("India, asia-south1", "asia-south1"),
                            option("Singapore, asia-southeast1", "asia-southeast1"),
                            option("Australia, australia-southeast1", "australia-southeast1"),
                            option("Poland, europe-central2", "europe-central2"),
                            option("Finland, europe-north1", "europe-north1"),
                            option("Spain, europe-southwest1", "europe-southwest1"),
                            option("Belgium, europe-west1", "europe-west1"),
                            option("UK, europe-west2", "europe-west2"),
                            option("Germany, europe-west3", "europe-west3"),
                            option("Netherlands, europe-west4", "europe-west4"),
                            option("Switzerland, europe-west6", "europe-west6"),
                            option("Italy, europe-west8", "europe-west8"),
                            option("France, europe-west9", "europe-west9"),
                            option("Qatar, me-central1", "me-central1"),
                            option("Kingdom of Saudi, me-central2", "me-central2"),
                            option("Israel, me-west1", "me-west1"),
                            option("Montreal, northamerica-northeast1", "northamerica-northeast1"),
                            option("Brazil, southamerica-east1", "southamerica-east1"),
                            option("Iowa, us-central1", "us-central1"),
                            option("South Carolina, us-east1", "us-east1"),
                            option("Northern Virginia, us-east4", "us-east4"),
                            option("Columbus, us-east5", "us-east5"),
                            option("Dallas, us-south1", "us-south1"),
                            option("Oregon, us-west1", "us-west1"),
                            option("Nevada, us-west4", "us-west4"))
                        .required(true)),
            authorization(BEARER_TOKEN)
                .title("Watsonx")
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
                    string(WatsonxConstants.PROJECT_ID)
                        .label("Project ID")
                        .description("The project ID.")
                        .required(true),
                    string(TOKEN)
                        .label("IAM Token")
                        .description("The IBM Cloud account IAM token.")
                        .required(true)));

    private AiTextAnalysisConnection() {
    }
}
