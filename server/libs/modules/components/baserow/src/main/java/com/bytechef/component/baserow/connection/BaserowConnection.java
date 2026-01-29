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

package com.bytechef.component.baserow.connection;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class BaserowConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .help("", "https://docs.bytechef.io/reference/components/baserow_v1#connection-setup")
        .baseUri((connectionParameters, context) -> "https://api.baserow.io/api")
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(TOKEN)
                        .label("Database Token")
                        .required(true))
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of(AUTHORIZATION, List.of("Token " + connectionParameters.getRequiredString(TOKEN))))))
        .version(1);

    private BaserowConnection() {
    }
}
