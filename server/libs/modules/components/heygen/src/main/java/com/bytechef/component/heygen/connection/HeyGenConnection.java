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

package com.bytechef.component.heygen.connection;

import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class HeyGenConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(AuthorizationType.API_KEY)
                .title("API Key")
                .properties(
                    string(KEY)
                        .label("API key")
                        .required(true))
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of("X-Api-Key", List.of(connectionParameters.getRequiredString(KEY))))));

    private HeyGenConnection() {
    }
}
