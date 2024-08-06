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

package com.bytechef.component.copper.connection;

import static com.bytechef.component.definition.Authorization.ApplyResponse.ofHeaders;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class CopperConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.copper.com/developer_api/v1")
        .authorizations(
            authorization(AuthorizationType.API_KEY)
                .title("API Key")
                .properties(
                    string(USERNAME)
                        .label("Email address")
                        .required(true),
                    string(KEY)
                        .label("Key")
                        .required(true))
                .apply((connectionParameters, context) -> ofHeaders(
                    Map.of(
                        "X-PW-AccessToken", List.of(connectionParameters.getRequiredString(KEY)),
                        "X-PW-Application", List.of("developer_api"),
                        "X-PW-UserEmail", List.of(connectionParameters.getRequiredString(USERNAME))))));

    private CopperConnection() {
    }
}
