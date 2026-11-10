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

package com.bytechef.component.pubmed.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.pubmed.constant.PubMedConstants.API_KEY;
import static com.bytechef.component.pubmed.constant.PubMedConstants.BASE_URL;
import static com.bytechef.component.pubmed.constant.PubMedConstants.EMAIL;
import static com.bytechef.component.pubmed.constant.PubMedConstants.TOOL;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class PubMedConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> BASE_URL)
        .authorizationRequired(false)
        .help("", "https://docs.bytechef.io/reference/components/pubmed_v1#connection-setup")
        .version(1)
        .authorizations(
            authorization(AuthorizationType.API_KEY)
                .title("API Key")
                .properties(
                    string(API_KEY)
                        .label("API Key")
                        .description(
                            "NCBI API key. Raises the E-utilities rate limit from 3 to 10 requests per second. " +
                                "Create it under Account settings -> API Key Management on NCBI.")
                        .required(true),
                    string(TOOL)
                        .label("Tool")
                        .description(
                            "Name of the application making the E-utilities calls, sent to NCBI as the tool " +
                                "parameter.")
                        .required(false),
                    string(EMAIL)
                        .label("Email")
                        .description(
                            "Email address of the developer of the application, sent to NCBI as the email " +
                                "parameter so NCBI can get in touch about problems.")
                        .required(false))
                .apply(PubMedConnection::apply));

    private PubMedConnection() {
    }

    protected static ApplyResponse apply(Parameters connectionParameters, Context context) {
        Map<String, List<String>> queryParameters = new HashMap<>();

        addQueryParameter(queryParameters, API_KEY, connectionParameters.getString(API_KEY));
        addQueryParameter(queryParameters, TOOL, connectionParameters.getString(TOOL));
        addQueryParameter(queryParameters, EMAIL, connectionParameters.getString(EMAIL));

        return ApplyResponse.ofQueryParameters(queryParameters);
    }

    private static void addQueryParameter(Map<String, List<String>> queryParameters, String name, String value) {
        if (value != null && !value.isBlank()) {
            queryParameters.put(name, List.of(value));
        }
    }
}
