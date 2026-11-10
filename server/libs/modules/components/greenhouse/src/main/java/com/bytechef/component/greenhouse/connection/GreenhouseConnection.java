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

package com.bytechef.component.greenhouse.connection;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GreenhouseConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://harvest.greenhouse.io/v1")
        .help("", "https://docs.bytechef.io/reference/components/greenhouse_v1#connection-setup")
        .version(1)
        .authorizations(
            authorization(AuthorizationType.BASIC_AUTH)
                .title("Harvest API Key")
                .properties(
                    string(USERNAME)
                        .label("Harvest API Key")
                        .description(
                            "The Harvest API key created in Greenhouse under Configure -> Dev Center -> API " +
                                "Credential Management.")
                        .required(true))
                .apply((connectionParameters, context) -> {
                    String credentials = connectionParameters.getRequiredString(USERNAME) + ":";

                    String encodedCredentials = context.encoder(
                        encoder -> encoder.base64Encode(credentials.getBytes(StandardCharsets.UTF_8)));

                    return ApplyResponse.ofHeaders(Map.of(AUTHORIZATION, List.of("Basic " + encodedCredentials)));
                }));

    private GreenhouseConnection() {
    }
}
