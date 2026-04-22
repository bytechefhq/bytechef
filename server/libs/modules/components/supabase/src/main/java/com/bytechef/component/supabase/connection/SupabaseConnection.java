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

package com.bytechef.component.supabase.connection;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.supabase.constant.SupabaseConstants.PROJECT_URL;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class SupabaseConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri(
            (connectionParameters, context) -> connectionParameters.getRequiredString(PROJECT_URL)
                .split("/rest/")[0])
        .authorizations(
            authorization(AuthorizationType.BEARER_TOKEN)
                .properties(
                    string(PROJECT_URL)
                        .label("Project URL")
                        .description("Can be found in Project Settings -> Data API.")
                        .required(true),
                    string(TOKEN)
                        .label("Project API Key")
                        .description("Can be found in Project Settings -> API Keys. Copy key under Secret keys.")
                        .required(true))
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of(
                        AUTHORIZATION, List.of("Bearer " + connectionParameters.getRequiredString(TOKEN)),
                        "apikey", List.of(connectionParameters.getRequiredString(TOKEN))))))
        .version(1)
        .help("", "https://docs.bytechef.io/reference/components/supabase_v1#connection-setup");

    private SupabaseConnection() {
    }
}
