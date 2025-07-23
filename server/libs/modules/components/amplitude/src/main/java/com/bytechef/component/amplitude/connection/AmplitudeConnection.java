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

package com.bytechef.component.amplitude.connection;

import static com.bytechef.component.amplitude.constant.AmplitudeConstants.API_KEY;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.REGION;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Nikolina Spehar
 */
public class AmplitudeConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://%s.amplitude.com"
            .formatted(connectionParameters.getRequiredString(REGION)))
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(API_KEY)
                        .label("API Key")
                        .description("The API key for the Amplitude project")
                        .required(true),
                    string(REGION)
                        .label("Region")
                        .description("Environment region you wish to access the API in.")
                        .options(
                            option("Standard", "api2"),
                            option("EU", "api.eu"))
                        .required(true)));

    private AmplitudeConnection() {
    }
}
