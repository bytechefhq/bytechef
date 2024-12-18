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

package com.bytechef.component.vertex.gemini.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType.BEARER_TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.vertex.gemini.constant.VertexGeminiConstants.LOCATION;
import static com.bytechef.component.vertex.gemini.constant.VertexGeminiConstants.PROJECT_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public final class VertexGeminiConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(BEARER_TOKEN)
                .title("Bearer Token")
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
                        .required(true)));

    private VertexGeminiConnection() {
    }
}
