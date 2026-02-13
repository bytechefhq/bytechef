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

package com.bytechef.component.one.simple.api.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.TOKEN;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class OneSimpleAPIConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://onesimpleapi.com/api")
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(TOKEN)
                        .label("API Token")
                        .required(true))
                .apply((connectionParameters, context) -> ApplyResponse.ofQueryParameters(
                    Map.of(
                        TOKEN, List.of(connectionParameters.getRequiredString(TOKEN)),
                        "output", List.of("json")))))
        .help("", "https://docs.bytechef.io/reference/components/one-simple-api_v1#connection-setup")
        .version(1);

    private OneSimpleAPIConnection() {
    }
}
